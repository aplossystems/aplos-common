package com.aplos.common.threads;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.BasicEmailFolder;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.EmailType;
import com.aplos.common.interfaces.EmailManagerJob;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;

public class IncomingEmailJob implements EmailManagerJob {
	private static Logger logger = Logger.getLogger( IncomingEmailJob.class );
	private List<MailServerSettings> mailServerSettingList;
	private Map<String,Date> deletableEmailAddressMap;
    private static final MailDateFormat mailDateFormat = new MailDateFormat();
	
	public IncomingEmailJob(List<MailServerSettings> mailServerSettingList, Map<String,Date> deletableEmailAddressMap) {
		this.mailServerSettingList = mailServerSettingList;
		this.deletableEmailAddressMap = deletableEmailAddressMap;
	}

	public void downloadPop3() {
		for( int i = 0, n = mailServerSettingList.size(); i < n; i++ ) {
			downloadPop3( mailServerSettingList.get( i ) );
		}
	}

	public void downloadPop3( MailServerSettings mailServerSettings ) {
		POP3Folder folder = null;
		Store store = null;

		try {
			// Create empty properties
			Properties props = new Properties();
			props.setProperty("mail.imaps.partialfetch", "false");
	
			// Get the session
			Session session = Session.getInstance(props, null);
	
			// Get the store
			store = session.getStore("pop3");
			store.connect(mailServerSettings.getIncomingHost(), mailServerSettings.getIncomingUsername(), mailServerSettings.getIncomingPassword());
	
			// Get folder
			folder = (POP3Folder)store.getFolder("INBOX");
			folder.open(Folder.READ_WRITE);

			// Get directory listing
			FetchProfile profile = new FetchProfile();
			profile.add(UIDFolder.FetchProfileItem.UID);
			Message[] messages = folder.getMessages();
			folder.fetch(messages,profile);
			System.err.println("Found " + messages.length + " new messages");
			if( messages.length > 0 ) {
				Map<String,Message> messageMap = new HashMap<String,Message>();
				List<String> duplicateUids = new ArrayList<String>(); // just for testing a bug

				for (int i = 0; i < messages.length; i++) {
					String messageUid = folder.getUID(messages[i]);
					if( messageMap.put( messageUid, messages[i] ) != null ) {
						duplicateUids.add( messageUid );
					}
				}
				if( duplicateUids.size() != 0 ) {
					ApplicationUtil.handleError( new Exception( StringUtils.join( duplicateUids.toArray( new String[ 0 ] ) ) ), false );
				}
	
				processEmailBatch(mailServerSettings, folder, messages);
			}
		} catch (Exception e) {
			ApplicationUtil.handleError( e, false );
		} finally {
			try {
				if( folder != null && folder.isOpen() ) {
					// Close connection
					folder.close(true); // true tells the mail server to expunge deleted
										// messages
				}
				if( store != null ) {
					store.close();
				}
			} catch( MessagingException mEx ) {
				ApplicationUtil.handleError( mEx, false );
			}
		}
	}

	private void processEmailBatch(MailServerSettings mailServerSettings, POP3Folder folder, Message[] messages) throws MessagingException {
		BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
		Map<String,Message> messageMap = new HashMap<String,Message>();
		String messageUids[] = new String[ messages.length ];
		StringBuffer parameterNamesBuf = new StringBuffer();
		for (int i = 0; i < messages.length; i++) {
			messageUids[ i ] = folder.getUID(messages[i]);
			messageMap.put( messageUids[ i ], messages[i] );
			parameterNamesBuf.append( ":param" ).append( i ).append( "," );
			aplosEmailDao.setNamedParameter( "param" + i, messageUids[ i ] );
		}

		aplosEmailDao.setIsReturningActiveBeans(null);
		aplosEmailDao.setSelectCriteria( "bean.uid, bean.plainTextBody, bean.htmlBody, bean.id, bean.incomingReadRetryCount, bean.isIncomingEmailDeleted" );
		aplosEmailDao.setWhereCriteria( "bean.emailType = " + EmailType.INCOMING.ordinal() );
		if (messageUids.length == 1) {
			aplosEmailDao.addWhereCriteria("bean.uid = " + parameterNamesBuf.substring(0, parameterNamesBuf.length() - 1));
		} else {
			aplosEmailDao.addWhereCriteria("bean.uid IN (" + parameterNamesBuf.substring(0, parameterNamesBuf.length() - 1) + ")");
		}

		List<Object[]> aplosEmailObjList;
		try {
			aplosEmailObjList = aplosEmailDao.getBeanResults();
		} catch(Exception ex) {
			System.err.println("About to print error for incoming email");
			System.err.println(aplosEmailDao.createProcessedBeanDao().getSelectSql());
			throw ex;
		}

		Map<Long,Message> missingContentMessageMap = new HashMap<Long,Message>();

		String tempMessageUid;
		for( int i = 0, n = aplosEmailObjList.size(); i < n; i++ ) {
			tempMessageUid = String.valueOf( aplosEmailObjList.get( i )[ 0 ] );
			if( CommonUtil.isNullOrEmpty( (String) aplosEmailObjList.get( i )[ 1 ] )
					&& CommonUtil.isNullOrEmpty( (String) aplosEmailObjList.get( i )[ 2 ] ) ) {
				if( ((Integer) aplosEmailObjList.get( i )[ 4 ]) < 5 && !((Boolean) aplosEmailObjList.get( i )[ 5 ]) ) {
					missingContentMessageMap.put( (Long) aplosEmailObjList.get( i )[ 3 ], messageMap.get( tempMessageUid ) );
				}
			}
			messageMap.remove( tempMessageUid );
		}

		BeanDao aplosEmailCheckDao = new BeanDao( AplosEmail.class );
		aplosEmailCheckDao.setIsReturningActiveBeans(null);
		aplosEmailCheckDao.addWhereCriteria( "bean.uid = :uid" );
		Message tempMessage;
		String tempSubject;

		System.err.println("About to process " + messageMap.keySet().size() + " new messages");
		int processedEmailCount = 0;
		for ( String tempUid : messageMap.keySet()) {
			tempMessage = messageMap.get( tempUid );
			if( !tempMessage.isSet( Flag.DELETED ) ) {
				/*
				 * This check was added in after 1000 emails were added as duplicates to Altrui
				 * , I'm not sure how this could have happened with the above code so I put in
				 * this check to make sure it doesn't happen again and to send information if it
				 * trys.
				 */
				aplosEmailCheckDao.setNamedParameter( "uid", tempUid );
				if( aplosEmailCheckDao.getAll().size() > 0 ) {
					ApplicationUtil.handleError( "Tried to download duplicate email - uid:" + tempUid , false );
					continue;
				}
				AplosEmail aplosEmail = new AplosEmail();
				try {
					fixIncorrectContentTypes(tempMessage);
					aplosEmail.setUid(tempUid);
					aplosEmail.setEmailType( EmailType.INCOMING );
					aplosEmail.setFromAddress(getFromAddress(tempMessage));
					if( deletableEmailAddressMap.get( aplosEmail.getFromAddress() ) != null ) {
						aplosEmail.setHardDeleteDate( deletableEmailAddressMap.get( aplosEmail.getFromAddress() ) );
					}

					// to list
					String[] toArray = tempMessage.getHeader( getHeaderName(Message.RecipientType.TO) );
//							Address[] toArray = tempMessage.getRecipients(Message.RecipientType.TO);
					if( toArray != null ) {
						for (String to : toArray) {
							decodeAddress(to, aplosEmail.getToAddresses());
						}
					} else {
						aplosEmail.getToAddresses().add(mailServerSettings.getEmailAddress());
					}

					// cc list
					String[] ccArray = null;
					try {
						ccArray = tempMessage.getHeader( getHeaderName(Message.RecipientType.CC) );
					} catch (Exception e) {
						ccArray = null;
					}
					if (ccArray != null) {
						for (String cc : ccArray) {
							decodeAddress(cc, aplosEmail.getCcAddresses());
						}
					}

					// subject
					tempSubject = tempMessage.getSubject();
					if( !CommonUtil.isNullOrEmpty(tempSubject) && tempSubject.length() > 190 ) {
						tempSubject = tempSubject.substring( 0, 191 );
					}
					aplosEmail.setSubject(tempSubject);

					Date receivedDate = tempMessage.getReceivedDate();
					// received date
					if (receivedDate == null && tempMessage.getHeader("Delivery-Date") != null ) {
						String s = tempMessage.getHeader("Delivery-Date")[ 0 ];
						if (s != null) {
							try {
								receivedDate = mailDateFormat.parse(s);
							} catch (ParseException pex) {
								ApplicationUtil.handleError( pex, false );
							}
						}
					}

					if( receivedDate == null ) {
						receivedDate = tempMessage.getSentDate();
					}

					aplosEmail.setEmailSentDate( receivedDate );

					// body and attachments
					aplosEmail.setHtmlBody("");

					Object content = tempMessage.getContent();
					if (content instanceof java.lang.String) {
						aplosEmail.setHtmlBody((String) content);
					} else if (content instanceof Multipart) {
						processMultipart( (Multipart) content, aplosEmail );
					} // end messages for loop

					BasicEmailFolder inboxEmailFolder = CommonConfiguration.getCommonConfiguration().getInboxEmailFolder();
					aplosEmail.getEmailFolders().clear();
					aplosEmail.addEmailFolder( inboxEmailFolder );
					aplosEmail.setMailServerSettings(mailServerSettings);

					aplosEmail.saveDetails();
					registerIncomingEmail(aplosEmail);

					StringBuffer testStrBuf = new StringBuffer();
					testStrBuf.append( aplosEmail.getId() ).append( " " );
					testStrBuf.append( FormatUtil.formatDateTimeForDB( new Date() ) );
					if( !CommonUtil.isNullOrEmpty( aplosEmail.getHtmlBody() ) ) {
						if( aplosEmail.getHtmlBody().length() > 20 ) {
							testStrBuf.append( " ; " ).append( aplosEmail.getHtmlBody().substring(0, 20).replace( "\n", " " ) );
						} else {
							testStrBuf.append( " ; " ).append( aplosEmail.getHtmlBody() );
						}
					} else {
						testStrBuf.append( " Empty Html" );
					}
					if( !CommonUtil.isNullOrEmpty( aplosEmail.getPlainTextBody() ) ) {
						if( aplosEmail.getPlainTextBody().length() > 20 ) {
							testStrBuf.append( " ; " ).append( aplosEmail.getPlainTextBody().substring(0, 20).replace( "\n", " " ) );
						} else {
							testStrBuf.append( " ; " ).append( aplosEmail.getPlainTextBody() );
						}
					} else {
						testStrBuf.append( " Empty plain text body" );
					}
					System.err.println( testStrBuf.toString() );
					logger.info( testStrBuf.toString() );

					// Finally delete the message from the server.
					if( mailServerSettings.isDeletingEmailsFromServer() ) {
						tempMessage.setFlag(Flags.Flag.DELETED, true);
					}
					processedEmailCount++;
				} catch (Exception e) {
//							HibernateUtil.getCurrentSession().clear();
					ApplicationUtil.handleError( e, false );
				}
//						HibernateUtil.startNewTransaction();
			}
		}
		System.err.println("Processed " + processedEmailCount + " new messages");

		for ( Long aplosEmailId : missingContentMessageMap.keySet()) {
			tempMessage = missingContentMessageMap.get( aplosEmailId );
			AplosEmail aplosEmail = new BeanDao( AplosEmail.class ).get( aplosEmailId ).getSaveableBean();

			try {
				if( tempMessage != null ) {
					// body and attachments
					aplosEmail.setHtmlBody("");
					Object content = tempMessage.getContent();
					if (content instanceof java.lang.String) {

						aplosEmail.setHtmlBody((String) content);

					} else if (content instanceof Multipart) {
						processMultipart( (Multipart) content, aplosEmail );
					} // end messages for loop
					registerIncomingEmail(aplosEmail);
				} else {
					aplosEmail.setIncomingEmailDeleted( true );
				}

				aplosEmail.setIncomingReadRetryCount( aplosEmail.getIncomingReadRetryCount() + 1 );
				aplosEmail.saveDetails();
			} catch (Exception e) {
//						HibernateUtil.getCurrentSession().clear();
				ApplicationUtil.handleError( e, false );
			}
//					HibernateUtil.startNewTransaction();
		}
	}
	
	public static void fixIncorrectContentTypes( Part part ) throws MessagingException {
		if( part != null && part.getHeader("Content-Type") != null ) {
			String contentTypeValue = CommonUtil.getStringOrEmpty( part.getHeader("Content-Type")[ 0 ] );
			if( contentTypeValue.contains( "cp932" ) ) {
				modifyHeader( part, "Content-Type", contentTypeValue.replace( "cp932", "windows-31j" ) );
			} else if( contentTypeValue.contains( "'UTF-8'" ) ) {
				modifyHeader( part, "Content-Type", contentTypeValue.replace( "'UTF-8'", "UTF-8" ) );
			}
		}
	}
	public static void modifyHeader( Part part, String headerName, String value ) throws MessagingException {
		if( part instanceof POP3Message ) {
			try {
				Field field = MimeMessage.class.getDeclaredField( "headers" );
				field.setAccessible(true);
				InternetHeaders internetHeaders = (InternetHeaders) field.get(part);
				internetHeaders.setHeader( headerName, value);
			} catch( Exception ex ) {
				ApplicationUtil.handleError( ex, false );
			}
		} else {
			part.setHeader( headerName, value );
		}
	}
	
	public static String getHeaderName(Message.RecipientType type) throws MessagingException {
		String headerName;
		
		if (type == Message.RecipientType.TO)
		    headerName = "To";
		else if (type == Message.RecipientType.CC)
		    headerName = "Cc";
		else if (type == Message.RecipientType.BCC)
		    headerName = "Bcc";
		else if (type == MimeMessage.RecipientType.NEWSGROUPS)
		    headerName = "Newsgroups";
		else
		    throw new MessagingException("Invalid Recipient Type");
	return headerName;
	}
	
	public static void registerIncomingEmail( AplosEmail aplosEmail ) {
		if( !(CommonUtil.isNullOrEmpty( aplosEmail.getHtmlBody() ) 
				&& CommonUtil.isNullOrEmpty( aplosEmail.getPlainTextBody() )) ) {
			ApplicationUtil.getAplosModuleFilterer().registerIncomingEmail( aplosEmail );
		}
	}
	
	public static String getFromAddress( Message tempMessage ) throws MessagingException, Exception {
		String fromAddress = "";
		try {
			fromAddress = tempMessage.getFrom()[0].toString();
		} catch( AddressException aEx ) {
			fromAddress = tempMessage.getHeader("Return-path")[ 0 ];
			
			if( CommonUtil.isNullOrEmpty( fromAddress ) ) {
				fromAddress = tempMessage.getHeader("From")[ 0 ];
			}

			if( CommonUtil.isNullOrEmpty( fromAddress ) ) {
				fromAddress = tempMessage.getHeader("Sender")[ 0 ];
			}
		}
		if( !CommonUtil.isNullOrEmpty(fromAddress) ) {
			List<String> fromAddressList = new ArrayList<String>();
			decodeAddress(fromAddress, fromAddressList);
			if( fromAddressList.size() > 0 ) {
				fromAddress = fromAddressList.get( 0 );
			}
		}
		return fromAddress;
	}
	
	public static void processMultipart( Multipart mp, AplosEmail aplosEmail ) throws MessagingException, IOException, Exception {
		try {
			
			for (int j = 0; j < mp.getCount(); j++) {
				Part part = mp.getBodyPart(j);
				String disposition = part.getDisposition();
				// Check if plain
				MimeBodyPart mbp = (MimeBodyPart) part;

				fixIncorrectContentTypes(mbp);
	
				if (mbp.isMimeType("text/plain")) {
					// Plain
					/*
					 * An email came through with two plain text parts with different Content-Type-Encoding headers
					 * one for quoted-printable and one for 7bit.  The 7bit one was empty so I'm just adding the two 
					 * together for now to make sure information isn't lost.
					 */
					if( CommonUtil.isNullOrEmpty( aplosEmail.getPlainTextBody() ) ) {
						aplosEmail.setPlainTextBody((String) mbp
								.getContent());
					} else {
						aplosEmail.setPlainTextBody( aplosEmail.getPlainTextBody() + "\n" + ((String) mbp
								.getContent()));
					}
					aplosEmail.setSendingPlainText(true);
				} else if(mbp.isMimeType("text/html")) {
					if( aplosEmail.getHtmlBody() != null ) {
						aplosEmail.setHtmlBody( aplosEmail.getHtmlBody() + (String) mbp
								.getContent());
					} else {
						aplosEmail.setHtmlBody((String) mbp
								.getContent());
					}
				} else if(mbp.isMimeType("multipart/alternative")
						|| mbp.isMimeType("multipart/related")
						|| mbp.isMimeType("multipart/mixed")) {
					processMultipart((Multipart) mbp.getContent(), aplosEmail);
				} else if ((disposition != null)
						&& (disposition.equals(Part.ATTACHMENT) || disposition
								.equals(Part.INLINE))) {
					FileDetails fileDetails = new FileDetails(
							CommonWorkingDirectory.APLOS_EMAIL_FILE_DIR
									.getAplosWorkingDirectory(),
							null);
					String originalFileName = decodeName(part.getFileName());
					if( originalFileName == null ) {
						originalFileName = "unknown";
					}
					if( originalFileName.startsWith( "<" ) ) {
						originalFileName = originalFileName.substring( 1 );
					}
					if( originalFileName.endsWith( ">" ) ) {
						originalFileName = originalFileName.substring( 0, originalFileName.length() - 1 );
					}
					fileDetails.setName( originalFileName );
					fileDetails.saveDetails();
					String fileType = null;
					if( originalFileName.lastIndexOf(".") != -1 ) {
						fileType = originalFileName.substring(originalFileName.lastIndexOf(".") + 1, originalFileName.length()).toLowerCase();
					} else {
						fileType = "unknown";
					}
					if( !CommonUtil.isAlphabetic( fileType ) ) {
						fileType = "unknown";
					}
					String fileName = fileDetails.getId() + "." + fileType;
					fileDetails.setFilename( fileName );
					fileDetails.saveDetails();
					saveFile(fileDetails.getFile(), part);
					aplosEmail.addSaveableAttachment(fileDetails);
				}
			} // end of multipart for loop
		} catch( Exception exception ) {
			aplosEmail.setHtmlBody( "Could not parse email due to error : " + exception.getMessage() );
			aplosEmail.setPlainTextBody( "Could not parse email due to error : " + exception.getMessage() );
			ApplicationUtil.handleError( exception, false );
		} 
	}
	
	/*
	 * CKEditor is breaking when there's a title tag in the content
	 */
	private static String parseBodyForCkEditor( String body ) {
		return body;
	}

	private static void decodeAddress(String name, List<String> addressList) throws Exception {
		if( name.contains( "<" ) ) {
			Pattern pattern = Pattern.compile( "<(.*)>" );
			Matcher matcher = pattern.matcher( name );
			if( matcher.find() ) {
				String address = matcher.group( 1 ).toLowerCase();
				if( address.startsWith( "'" ) ) {
					address = address.substring( 1 );
				}
				if( address.endsWith( "'" ) ) {
					address = address.substring( 0, address.length() - 1 );
				}
				name = matcher.group( 1 );
			}
		} 
		
		if( name != null ) {
			name = name.toLowerCase();
			/*
			 * Some mail servers have the addresses in one long string
			 */
			String nameParts[] = name.split( "," );
			for( int i = 0, n = nameParts.length; i < n; i++ ) {
				nameParts[ i ] = nameParts[ i ].trim();
				if( nameParts[ i ].length() < 191 ) {
					addressList.add( nameParts[ i ] );
				}
			}
		}
	}

	private static String decodeName(String name) throws Exception {
		if (name == null || name.length() == 0) {
			return null;
		}
		String ret = java.net.URLDecoder.decode(name, "UTF-8");

		// also check for a few other things in the string:
		ret = ret.replaceAll("=\\?utf-8\\?q\\?", "");
		ret = ret.replaceAll("\\?=", "");
		ret = ret.replaceAll("=20", " ");

		return ret;
	}

	private static int saveFile(File saveFile, Part part) throws Exception {
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(saveFile));

		byte[] buff = new byte[2048];
		InputStream is = part.getInputStream();
		int ret = 0, count = 0;
		while ((ret = is.read(buff)) > 0) {
			bos.write(buff, 0, ret);
			count += ret;
		}
		bos.close();
		is.close();
		return count;
	}
}
