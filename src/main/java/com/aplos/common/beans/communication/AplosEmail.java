package com.aplos.common.beans.communication;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.AplosUrl;
import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.Index;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.JoinTable;
import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosSiteBean;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.InputStreamDataSource;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.EmailActionType;
import com.aplos.common.enums.EmailStatus;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.enums.EmailType;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.enums.SingleEmailRecordStatus;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.interfaces.EmailValidatorPage;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.threads.BulkEmailSender;
import com.aplos.common.threads.EmailManager;
import com.aplos.common.threads.EmailSender;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UrlEncrypter;

@Entity
@PluralDisplayName(name="sent emails")
@Cache
@BeanScope(scope=JsfScope.TAB_SESSION)  // Set to tab session so it's visible in iframe in aplos email edit page
public class AplosEmail extends AplosSiteBean {
	private static final long serialVersionUID = 3067338302741650954L;

	private static Logger logger = Logger.getLogger( AplosEmail.class );
    @ManyToAny( metaColumn = @Column( name = "message_source_type" ), fetch=FetchType.LAZY )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinTable( inverseJoinColumns = @JoinColumn( name = "messageSources_id" ) )
    @DynamicMetaValues
	private List<BulkMessageSource> messageSourceList = new ArrayList<BulkMessageSource>();
    @ManyToAny( metaColumn = @Column( name = "removed_source_type" ), fetch=FetchType.LAZY )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinTable( inverseJoinColumns = @JoinColumn( name = "removedSources_id" ) )
    @DynamicMetaValues
	private Set<BulkEmailSource> removedSourceSet = new HashSet<BulkEmailSource>();
    @ManyToAny( metaColumn = @Column( name = "email_folder_type" ), fetch=FetchType.LAZY )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinTable( inverseJoinColumns = @JoinColumn( name = "email_folder_id" ) )
    @DynamicMetaValues
	private Set<EmailFolder> emailFolders = new HashSet<EmailFolder>();
    @Any( metaColumn = @Column( name = "email_generator_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinColumn( name = "email_generator_id" )
    @DynamicMetaValues
    private EmailGenerator emailGenerator;
	@Column(columnDefinition="LONGTEXT")
	private String headContent;
	private String subject;
	@Column(columnDefinition="LONGTEXT")
	private String htmlBody;
	@Column(columnDefinition="LONGTEXT")
	private String plainTextBody;
	@CollectionOfElements
	private List<String> toAddresses = new ArrayList<String>();
	@CollectionOfElements
	private List<String> ccAddresses = new ArrayList<String>();
	@CollectionOfElements
	private List<String> bccAddresses = new ArrayList<String>();
	private String fromAddress;
	@ManyToMany(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private List<FileDetails> saveableAttachments = new ArrayList<FileDetails>();
	@ManyToOne(fetch=FetchType.LAZY)
	private EmailTemplate emailTemplate; 
	@ManyToOne(fetch=FetchType.LAZY)
	private EmailFrame emailFrame; 
	@ManyToOne(fetch=FetchType.LAZY)
	private EmailFrame outerEmailFrame;
	@ManyToOne(fetch=FetchType.LAZY)
	private AplosEmail originalEmail;
	@ManyToOne(fetch=FetchType.LAZY)
	private AplosEmail repliedEmail;
	@ManyToOne(fetch=FetchType.LAZY)
	private AplosEmail forwardedEmail;
	private Integer sendStartIdx;
	private Integer maxSendQuantity;
	@Index(name="uidIndex")
	private String uid;
	private int incomingReadRetryCount = 0;
	
	// This is just for testing
	private boolean isIncomingEmailDeleted = false;
	/*****************/
	
	private boolean isAskingForReceipt = false;
	@Index(name="emailStatusIndex")
	private EmailStatus emailStatus = EmailStatus.UNFLAGGED;

	private boolean removeDuplicateToAddresses = true;
	private boolean isSendingPlainText = false;
	private Date emailSentDate;
	@Index(name="autoSendDateIndex")
	private Date automaticSendDate;
	private MessageGenerationType emailGenerationType = MessageGenerationType.NONE;
	private String encryptionSalt;
	private boolean isUsingEmailSourceAsOwner = false;
	private int emailSentCount;
	@Index(name="emailTypeIndex")
	private EmailType emailType = EmailType.OUTGOING;
	@Index(name="emailReadDateIndex")
	private Date emailReadDate;
	private Date hardDeleteDate;

	@ManyToOne(fetch=FetchType.LAZY)
	private MailServerSettings mailServerSettings;
	/*
	 * This has been choosen as this format because outlook alters the HTML in the email before sending.
	 * This format will stay the same after alterations have been made to the rest of the email.
	 */
	public static final String EMAIL_BODY_DIVIDER = "<div class=\"emailbodydivider2\">&nbsp;</div>";
	public static final String KEY = "key";

	@Transient
	private boolean isDivertingEmailsInDebug = true;
	@Transient
	private boolean isSendingViaQueue = true;
	@Transient
	private UrlEncrypter urlEncrypter;
	public static final String REPLY_HEADER = "<div style='border:none;border-top:solid #B5C4DF 1.0pt;padding:3.0pt 0cm 0cm 0cm'><b>From: </b>{FROM}<br/><b>Sent: </b>{SENT}<br/>{TO}{CC}{BCC}<b>Subject: </b>{SUBJECT}</div>";
	@Transient
	private Map<BulkEmailSource,SingleEmailRecord> singleEmailRecordMap = new HashMap<BulkEmailSource,SingleEmailRecord>();
	@Transient
	private boolean isSingleEmailRecordMapInitialised = true;

	public AplosEmail() {
		init();
		CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
		if( commonConfiguration != null ) {
			includeDefaultHeaderAndFooter();
		}
	}

	public AplosEmail( String subject, String body ) {
		this( subject, body, true );
	}

	public AplosEmail( String subject, String body, boolean appendFooterAndHeader ) {
		init();
		this.subject = subject;
		this.htmlBody = body;
		if( appendFooterAndHeader ) {
			includeDefaultHeaderAndFooter();
		}
	}

	public AplosEmail( EmailTemplateEnum emailTemplateEnum ) {
		init();
		updateEmailTemplate(emailTemplateEnum);
		setSaveableAttachments( emailTemplate.getAttachments(null) );
	}

	public AplosEmail( EmailTemplateEnum emailTemplateEnum, BulkEmailSource bulkEmailSource, EmailGenerator emailGenerator ) {
		init();
		updateEmailTemplate(emailTemplateEnum);
		setSingleMessageSource(bulkEmailSource, emailGenerator);
	}

	public AplosEmail( EmailTemplate emailTemplate, BulkEmailSource bulkEmailSource, EmailGenerator emailGenerator ) {
		init();
		updateEmailTemplate(emailTemplate);
		setSingleMessageSource(bulkEmailSource, emailGenerator);
	}

	public AplosEmail( EmailTemplateEnum emailTemplateEnum, List<BulkEmailSource> emailSourceList ) {
		init();
		updateEmailTemplate(emailTemplateEnum);
		setSaveableAttachments( emailTemplate.getAttachments(null) );
		getMessageSourceList().clear();
		getMessageSourceList().addAll( emailSourceList );
		setEmailGenerationType(MessageGenerationType.MESSAGE_GROUPS);
	}

	public AplosEmail( EmailTemplateEnum emailTemplateEnum, BulkMessageSourceGroup bulkMessageSourceGroup ) {
		init();
		updateEmailTemplate(emailTemplateEnum);
		setSaveableAttachments( emailTemplate.getAttachments(null) );
		getMessageSourceList().clear();
		getMessageSourceList().add( bulkMessageSourceGroup );
		setEmailGenerationType(MessageGenerationType.MESSAGE_GROUPS);
	}
	
	public void init() {
		setMailServerSettings( JSFUtil.determineMailServerSettings() );
		CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
		if( commonConfiguration != null ) {
			setAskingForReceipt( commonConfiguration.isReadReceiptsRequired() );
			BasicEmailFolder sentEmailFolder = CommonConfiguration.getCommonConfiguration().getSentEmailFolder();
			addEmailFolder( sentEmailFolder );
		}
	}
	
	@Override
	public void persistenceBeanCreated() {
		if( isInDatabase() ) {
			setSingleEmailRecordMapInitialised( false );
		}
	}
	
	@Override
	public void copy(AplosAbstractBean aplosAbstractBean) {
		AplosEmail sourceEmail = (AplosEmail) aplosAbstractBean;
		setMessageSourceList( new ArrayList<BulkMessageSource>( sourceEmail.getMessageSourceList() ) );
		setSubject( sourceEmail.getSubject() );
		setHtmlBody( sourceEmail.getHtmlBody() );
		setPlainTextBody( sourceEmail.getPlainTextBody() );
		setToAddresses( new ArrayList<String>( sourceEmail.getToAddresses() ) );
		setCcAddresses( new ArrayList<String>( sourceEmail.getCcAddresses() ) );
		setFromAddress( sourceEmail.getFromAddress() );
		setEmailFolders( new HashSet<EmailFolder>( sourceEmail.getEmailFolders() ) );
		setSaveableAttachments( new ArrayList<FileDetails>( sourceEmail.getSaveableAttachments() ) );
		setEmailTemplate( sourceEmail.getEmailTemplate() );
		setEmailFrame( sourceEmail.getEmailFrame() );
		setOuterEmailFrame( sourceEmail.getOuterEmailFrame() );
		setOriginalEmail( sourceEmail.getOriginalEmail() );
		setRemoveDuplicateToAddresses( sourceEmail.isRemoveDuplicateToAddresses() );
		setSendingPlainText( sourceEmail.isSendingPlainText() );
		setEmailSentDate( sourceEmail.getEmailSentDate() );
		setEmailGenerationType( sourceEmail.getEmailGenerationType() );
		setEmailGenerator( sourceEmail.getEmailGenerator() );
		setMailServerSettings( sourceEmail.getMailServerSettings() );
		setMaxSendQuantity( sourceEmail.getMaxSendQuantity() );
		setSendStartIdx( sourceEmail.getSendStartIdx() );
		setAskingForReceipt( sourceEmail.isAskingForReceipt() );
		setEmailType( sourceEmail.getEmailType() );
		setEmailReadDate( sourceEmail.getEmailReadDate() );
		setHeadContent( sourceEmail.getHeadContent() );
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( !getClass().equals( AplosEmail.class ) ) {
			addToScope( CommonUtil.getBinding( AplosEmail.class ), this, associatedBeanScope );
		}
	}
	
	public static void addEmailFolderToExistingEmails( BulkEmailSource bulkEmailSource ) {
		if( bulkEmailSource instanceof EmailFolder ) {
			BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
			aplosEmailDao.addQueryTable( "toAddress", "bean.toAddresses" );
			aplosEmailDao.addWhereCriteria( "toAddress LIKE :emailAddress" );
			aplosEmailDao.setNamedParameter( "emailAddress", "%" + bulkEmailSource.getEmailAddress() + "%" );
			List<AplosEmail> aplosEmailList = aplosEmailDao.getAll();
			for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
				addEmailToFolder( aplosEmailList.get( i ), (EmailFolder) bulkEmailSource );
			}
			
			aplosEmailDao.addQueryTable( "ccAddress", "bean.ccAddresses" );
			aplosEmailDao.setWhereCriteria( "ccAddress LIKE :emailAddress" );
			aplosEmailDao.setNamedParameter( "emailAddress", "%" + bulkEmailSource.getEmailAddress() + "%" );
			aplosEmailList = aplosEmailDao.getAll();
			for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
				addEmailToFolder( aplosEmailList.get( i ), (EmailFolder) bulkEmailSource );
			}
			
			aplosEmailDao.addQueryTable( "bccAddress", "bean.bccAddresses" );
			aplosEmailDao.setWhereCriteria( "bccAddress LIKE :emailAddress" );
			aplosEmailDao.setNamedParameter( "emailAddress", "%" + bulkEmailSource.getEmailAddress() + "%" );
			aplosEmailList = aplosEmailDao.getAll();
			for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
				addEmailToFolder( aplosEmailList.get( i ), (EmailFolder) bulkEmailSource );
			}
			
			aplosEmailDao.setWhereCriteria( "bean.fromAddress LIKE :emailAddress" );
			aplosEmailDao.setNamedParameter( "emailAddress", "%" + bulkEmailSource.getEmailAddress() + "%" );
			aplosEmailList = aplosEmailDao.getAll();
			for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
				addEmailToFolder( aplosEmailList.get( i ), (EmailFolder) bulkEmailSource );
			}
		}
	}
	
	public static void addEmailToFolder( AplosEmail aplosEmail, EmailFolder emailFolder ) {
		AplosEmail saveableAplosEmail = aplosEmail.getSaveableBean();
		if( saveableAplosEmail.isArchived() ) {
			saveableAplosEmail.unarchive();
		}
		saveableAplosEmail.addEmailFolder( emailFolder );
		saveableAplosEmail.saveDetails();
		
	}
	
	public static AplosEmail verifyParameters( EmailValidatorPage emailValidatorPage ) {
		String aplosEmailId = JSFUtil.getRequest().getParameter( "aplosEmailId" );
		String emailSourceId = JSFUtil.getRequest().getParameter( "emailSourceId" );
		String emailSourceClassStr = JSFUtil.getRequest().getParameter( "emailSourceClass" );
		
		if( !CommonUtil.isNullOrEmpty( aplosEmailId ) ) {
			AplosEmail tempAplosEmail = (AplosEmail) new BeanDao( AplosEmail.class ).get( Long.parseLong( aplosEmailId ) );

			BulkEmailSource tempEmailSource = null;
			Class<? extends BulkEmailSource> bulkEmailSourceClass = null;
			if( !CommonUtil.isNullOrEmpty( emailSourceId ) && !CommonUtil.isNullOrEmpty( emailSourceClassStr ) ) {
				try {
					bulkEmailSourceClass = (Class<? extends BulkEmailSource>) Class.forName( emailSourceClassStr );
					tempEmailSource = (BulkEmailSource) new BeanDao( (Class<? extends AplosAbstractBean>) bulkEmailSourceClass ).get( Long.parseLong( emailSourceId ) );
				} catch (ClassNotFoundException e) {
					ApplicationUtil.getAplosContextListener().handleError( e );
				}
			}

			try {
				String key = JSFUtil.getRequest().getParameter( AplosEmail.KEY );
				key = new String( Base64.decodeBase64(key), "UTF8" );
				if( CommonUtil.checkStdEncrypt( emailSourceId + aplosEmailId, tempAplosEmail.getEncryptionSalt(), true, key ) ) {
					emailValidatorPage.setAplosEmail(tempAplosEmail);
					emailValidatorPage.setEmailSource(tempEmailSource);
				} else {
					ApplicationUtil.handleError( new Exception( "The key could not be decrypted correctly" ));
				}
			} catch( UnsupportedEncodingException useEx ) {
				ApplicationUtil.handleError( useEx );
			}
		}
		return null;
	}
	
	public String getContentSample() {
		StringBuffer strBuf;
		if( isSendingPlainText() && !CommonUtil.isNullOrEmpty( getPlainTextBody() ) ) {
			strBuf = new StringBuffer(getPlainTextBody());
		} else {
			strBuf = new StringBuffer(CommonUtil.getStringOrEmpty(CommonUtil.stripHtml(getHtmlBody())));
		}
		
		return StringEscapeUtils.unescapeHtml( strBuf.substring( 0, Math.min( strBuf.length(), 250) ) );
	}
	
	public String getFirstToAddress() {
		if( getToAddresses().size() > 0 ) {
			return getToAddresses().get( 0 );	
		} else {
			return null;
		}
	}
	
	public AplosUrl addEncryptionParameters( AplosUrl aplosUrl, BulkEmailSource bulkEmailSource ) {
		aplosUrl.addQueryParameter( "aplosEmailId", getId() );
		if( bulkEmailSource != null ) {
			aplosUrl.addQueryParameter( "emailSourceId", String.valueOf( bulkEmailSource.getMessageSourceId() ) );
			aplosUrl.addQueryParameter( "emailSourceClass", ApplicationUtil.getClass( (AplosAbstractBean) bulkEmailSource ).getName() );
			String key = CommonUtil.stdEncrypt( String.valueOf( bulkEmailSource.getMessageSourceId() ) + String.valueOf( getId() ), getEncryptionSalt(), true );
			key = Base64.encodeBase64URLSafeString( key.getBytes() );
			aplosUrl.addQueryParameter( AplosEmail.KEY, key );
		}
		return aplosUrl;
	}
	
	public void addAdminEmailToBccAddresses() {
		addBccAddress( CommonConfiguration.getCommonConfiguration().getDefaultAdminUser().getEmail() );	
	}
	
	public BulkEmailSource getFirstEmailSource() {
		for( int i = 0, n = getMessageSourceList().size(); i < n; i++ ) {
			if( getMessageSourceList().get( 0 ) instanceof BulkEmailSource ) {
				return (BulkEmailSource) getMessageSourceList().get( 0 );
			}
		} 
		return null; 
	}
	
	public void setSingleMessageSource( BulkEmailSource bulkEmailSource, EmailGenerator emailGenerator ) {
		getMessageSourceList().clear();
		getMessageSourceList().add( bulkEmailSource );
		setEmailGenerationType(MessageGenerationType.SINGLE_SOURCE);
		
		if( getEmailTemplate() != null ) {
			String emailAddress = getEmailTemplate().getEmailAddress( bulkEmailSource );
			if( !CommonUtil.isNullOrEmpty( emailAddress ) ) {
				getToAddresses().add( getEmailTemplate().getEmailAddress( bulkEmailSource ) );
			}
			if( getEmailTemplate().isSendToAdminAlsoByDefault(bulkEmailSource) ) {
				addAdminEmailToBccAddresses();
			}
			setSaveableAttachments( getEmailTemplate().getAttachments(emailGenerator) );
			getEmailTemplate().singleMessageSourceSet( this, bulkEmailSource );
		}
		
		try {
			setHtmlBody( processHtmlBodyDynamicValues( getSingleEmailRecord( bulkEmailSource, true ), emailGenerator ) );
			setSubject( processSubjectDynamicValues( bulkEmailSource, emailGenerator ) );
		} catch( IOException ioex ) {
			ApplicationUtil.handleError( ioex );
		}
	}
	
	public void updateEmailTemplate( EmailTemplateEnum emailTemplateEnum ) {
		EmailTemplate emailTemplate = Website.getCurrentWebsiteFromTabSession().loadEmailTemplate(emailTemplateEnum);
		updateEmailTemplate( emailTemplate );
	}
	
	public void updateEmailTemplate( EmailTemplate emailTemplate ) {
		setSubject(emailTemplate.getSubject());
		setHtmlBody(emailTemplate.getContent());
		setPlainTextBody(emailTemplate.getPlainText());
		if( !CommonUtil.isNullOrEmpty( getPlainTextBody() ) ) {
			setSendingPlainText(true);
		}
		setEmailTemplate( emailTemplate );
		setEmailFrame( emailTemplate.getEmailFrame() );
		setOuterEmailFrame( emailTemplate.getOuterEmailFrame() );
		setFromAddress(emailTemplate.getDefaultFromAddress());
	}
	
	public String determineDefaultFromAddress() {
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if( currentUser != null && !CommonUtil.isNullOrEmpty(currentUser.getEmailAddress())) {
			return currentUser.getEmailAddress();
		} else {
			return CommonConfiguration.getCommonConfiguration().getDefaultAdminUser().getEmailAddress();
		}
	}
	
	public String getFirstEmailSourceFullName() {
		BulkEmailSource firstEmailSource = getFirstEmailSource(); 
		if( firstEmailSource != null ) {
			return FormatUtil.getFullName( firstEmailSource.getFirstName(), firstEmailSource.getSurname() );	
		} else {
			return null;
		}
	}
	
	public void removeSaveableAttachment( FileDetails fileDetails ) {
		getSaveableAttachments().remove( fileDetails );
		if( fileDetails instanceof CreatedPrintTemplate ) {
			CreatedPrintTemplate tempCreatedPrintTemplate;
			for( SingleEmailRecord tempSingleEmailRecord : getSingleEmailRecordMap().values() ) {
				for( FileDetails tempFileDetails : new ArrayList<FileDetails>( tempSingleEmailRecord.getSaveableAttachments() ) ) {
					if( tempFileDetails instanceof CreatedPrintTemplate ) {
						if( ((CreatedPrintTemplate) tempFileDetails).getPrintTemplate().equals( ((CreatedPrintTemplate) fileDetails).getPrintTemplate() ) ) {
							tempSingleEmailRecord.getSaveableAttachments().remove( tempFileDetails );
						}
					}
				}
			}
		} else {
			for( SingleEmailRecord tempSingleEmailRecord : getSingleEmailRecordMap().values() ) {
				tempSingleEmailRecord.getSaveableAttachments().remove( fileDetails );
			}
		}
	}
	
	public List<EmailFolder> createEmailFolderListFromSet() {
		return new ArrayList<EmailFolder>( getEmailFolders() );
	}
	
	public void includeDefaultHeaderAndFooter() {
		EmailFrame emailFrame = CommonConfiguration.getCommonConfiguration().getEmailFrame();
		if( emailFrame != null ) {
			this.setEmailFrame( emailFrame );
		}
	}
	
	public Set<BulkEmailSource> filterMessageSourceList() {
		Set<BulkEmailSource> bulkEmailSourceSet = new HashSet<BulkEmailSource>();
		boolean removeUnsubscribed = false;
		if( getEmailTemplate() != null && !MessageGenerationType.SINGLE_SOURCE.equals( getEmailGenerationType() ) ) {
			removeUnsubscribed = getEmailTemplate().removeUnsubscribedByDefault();
		}
		boolean validateEmailAddress = true;
		if( MessageGenerationType.SINGLE_SOURCE.equals( getEmailGenerationType() ) && getToAddresses().size() > 0 ) {
			validateEmailAddress = false;
		}
		BulkMessageSourceGroup.filterBulkEmailSources( this, getMessageSourceList(), bulkEmailSourceSet, removeUnsubscribed, validateEmailAddress, new HashSet<String>() );

		for( BulkEmailSource tempBulkEmailSource : getRemovedSourceSet() ) {
			bulkEmailSourceSet.remove( tempBulkEmailSource );
		}
		return bulkEmailSourceSet;
	}

	public MimeMessage sendAplosEmailToQueue() {
		AplosEmail saveabledAplosEmail = getSaveableBean();
		return saveabledAplosEmail.sendAplosEmailToQueue(true);
	}

	public MimeMessage sendAplosEmailToQueue( boolean saveEmail ) {
		Set<BulkEmailSource> bulkEmailSourceSet = filterMessageSourceList();

		Set<SingleEmailRecord> singleEmailRecordSet = new HashSet<SingleEmailRecord>();
		if( !isSingleEmailRecordMapInitialised() ) {
			/*
			 * This is just done for efficiency so each one is being pulled out one by one from the 
			 * DB.
			 */
			BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
			singleEmailRecordDao.addWhereCriteria( "bean.aplosEmail.id = " + getId() );
			List<SingleEmailRecord> existingSingleEmailRecordList = singleEmailRecordDao.getAll();
			for( int i = 0, n = existingSingleEmailRecordList.size(); i < n; i++ ) {
				getSingleEmailRecordMap().put( existingSingleEmailRecordList.get( i ).getBulkEmailSource(), existingSingleEmailRecordList.get( i ));
			}
			setSingleEmailRecordMapInitialised(true);
		}
		for( BulkEmailSource bulkEmailSource : bulkEmailSourceSet ) {
			singleEmailRecordSet.add( getSingleEmailRecord(bulkEmailSource, true) );
		}
		
		try {
			MimeMessage mimeMessage = null;
			// Save the details now so that there is an id available for the email viewer
	        if( saveEmail ) {
	        	saveDetails();
	        }
	        
		   BulkEmailSender bulkEmailSender = new BulkEmailSender( this, singleEmailRecordSet );
		   mimeMessage = bulkEmailSender.startSendingEmails( saveEmail );
	        
			if( mimeMessage != null ) {
		        setEmailSentDate(new Date());
			}
	        if( saveEmail ) {
				saveDetails();
	        }
	        return mimeMessage;
		} catch (javax.mail.MessagingException mEx) {
			ApplicationUtil.getAplosContextListener().handleError( mEx );
			return null;
		} catch (IOException ioEx) {
			ApplicationUtil.getAplosContextListener().handleError( ioEx );
			return null;
		}
	}
	
	public String processHtmlBodyDynamicValues(SingleEmailRecord singleEmailRecord, EmailGenerator determinedEmailGenerator ) throws IOException {
		if( emailTemplate != null && singleEmailRecord.getBulkEmailSource() != null ) {
			return emailTemplate.compileContent( singleEmailRecord.getBulkEmailSource(), determinedEmailGenerator, getHtmlBody(), singleEmailRecord );
		} else {
			JDynamiTe dynamiTe = new JDynamiTe();
			CommonUtil.addUtf8StringToJDynamiTe( dynamiTe, getHtmlBody() );
			EmailTemplate.addCommonJDynamiTeValues( dynamiTe, singleEmailRecord ); 
			dynamiTe.parse();
			return dynamiTe.toString();
		}
	}
	
	public String generateHtmlBody(SingleEmailRecord singleEmailRecord, EmailGenerator determinedEmailGenerator, String processedSubject ) throws IOException {
		StringBuffer htmlBodyStrBuf  = null;
		if( MessageGenerationType.SINGLE_SOURCE.equals( getEmailGenerationType() ) ) {
			htmlBodyStrBuf = new StringBuffer( getHtmlBody() );
		} else {
			htmlBodyStrBuf = new StringBuffer( processHtmlBodyDynamicValues(singleEmailRecord, determinedEmailGenerator ) );
		}
		
        appendFooterAndHeader( htmlBodyStrBuf, singleEmailRecord );
        if( CommonConfiguration.getCommonConfiguration().isUsingEmailBodyDivider() ) {
        	htmlBodyStrBuf.insert( 0, EMAIL_BODY_DIVIDER );
        }
        if( !htmlBodyStrBuf.toString().contains( "<html>" ) ) {
        	StringBuffer headStrBuf = new StringBuffer();
        	/*
        	 * The extra microsoft garbage helps prevent it from being Junk mailed.  This can be tested
        	 * by setting your Junk mail settings to high in Outlook and sending emails to that account 
        	 * to see if they get put into Junk mail or not.
        	 */
        	headStrBuf.append( "<!doctype html>\n" );
        	headStrBuf.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\"  xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n" );
        	headStrBuf.append( "<head>\n" );
        	headStrBuf.append( "<meta name=Generator content=\"Microsoft Word 12 (filtered medium)\">" );
        	headStrBuf.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" );
        	if( !CommonUtil.isNullOrEmpty( processedSubject ) ) {
        		headStrBuf.append( "<title>" ).append( processedSubject ).append( "</title>\n" );
        	}
			if( !CommonUtil.isNullOrEmpty( getHeadContent() ) ) {
				headStrBuf.append( getHeadContent() );
			}
			if( getEmailFrame() != null && !CommonUtil.isNullOrEmpty( getEmailFrame().getHtmlHead() ) ) {
				headStrBuf.append( getEmailFrame().getHtmlHead() );
			}
			headStrBuf.append( "\n</head>\n" );
			headStrBuf.append( "<body leftmargin=\"0\" topmargin=\"0\" marginwidth=\"0\" marginheight=\"0\" yahoo=\"fix\">\n" );
			htmlBodyStrBuf.insert( 0, headStrBuf.toString() );
			htmlBodyStrBuf.append( "</body></html>" );
        }
        
        return htmlBodyStrBuf.toString();
	}
	
	public String processSubjectDynamicValues(BulkEmailSource bulkEmailSource, EmailGenerator determinedEmailGenerator) throws IOException {
		String processedSubject  = getSubject();
		if( emailTemplate != null && bulkEmailSource != null ) {
			processedSubject = emailTemplate.compileSubject( bulkEmailSource, determinedEmailGenerator, processedSubject );
		} else {
			JDynamiTe dynamiTe = new JDynamiTe();
			CommonUtil.addUtf8StringToJDynamiTe( dynamiTe, processedSubject );
			EmailTemplate.addCommonJDynamiTeValues( dynamiTe, null ); 
			dynamiTe.parse();
			processedSubject = dynamiTe.toString();
		}
		return processedSubject;
	}
	
	public EmailGenerator determineEmailGenerator( SingleEmailRecord singleEmailRecord ) {
		EmailGenerator determinedEmailGenerator = getEmailGenerator();
		if( determinedEmailGenerator == null ) {
			determinedEmailGenerator = singleEmailRecord.getBulkEmailSource();
		}
		return determinedEmailGenerator;
	}
	
	public MimeMessage sendEmail( SingleEmailRecord singleEmailRecord ) throws MessagingException, IOException {
		MimeMessage mimeMessage = new AplosMimeMessage( getMailServerSettings().getMailSession() );

		EmailGenerator determinedEmailGenerator = determineEmailGenerator(singleEmailRecord);
		
		String processedSubject = null;
		if( MessageGenerationType.SINGLE_SOURCE.equals( getEmailGenerationType() ) ) {
			processedSubject = getSubject();
		} else {
			processedSubject = processSubjectDynamicValues( singleEmailRecord.getBulkEmailSource(), determinedEmailGenerator );
		}
		
		mimeMessage.setSubject(processedSubject,"UTF-8");
		String htmlBody = generateHtmlBody( singleEmailRecord, determinedEmailGenerator, processedSubject );
		
		String processedPlainTextBody = getPlainTextBody();
		if( isSendingPlainText() && !CommonUtil.isNullOrEmpty(getPlainTextBody()) ) {
			processedPlainTextBody = emailTemplate.compileContent( singleEmailRecord.getBulkEmailSource(), determinedEmailGenerator, getPlainTextBody(), singleEmailRecord );
		}
		
		String divertEmailAddress = null;
		if ( ApplicationUtil.getAplosContextListener().isDebugMode() && isDivertingEmailsInDebug() ) {
			// To stop unwanted external email sending during testing
			divertEmailAddress = getDivertEmailAddress();

			mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(divertEmailAddress));
			if( ccAddresses.size() > 0 ) {
				ccAddresses.clear();
				ccAddresses.add( getDivertEmailAddress() );
			}
			if( bccAddresses.size() > 0 ) {
				bccAddresses.clear();
				bccAddresses.add( getDivertEmailAddress() );
			}
		} else {
			for( String toAddress : singleEmailRecord.getToAddresses() ) {
				try {
					mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
				} catch( AddressException aex ) {
					JSFUtil.addMessage( "The recipient address '" + toAddress + "' is invalid please update it." );
					logger.debug( "The recipient address '" + toAddress + "' is invalid please update it." );
					return null;
				}
			}
		}
		for( String ccAddress : ccAddresses ) {
			try {
				mimeMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddress)); 
			}
			catch( AddressException aex ) {
				JSFUtil.addMessage( "The cc address '" + ccAddress + "' is invalid please update it." );
				logger.debug( "The cc address '" + ccAddress + "' is invalid please update it." );
				return null;
			}	
		}
		for( String bccAddress : bccAddresses ) {
			try {
				mimeMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccAddress)); 
			}
			catch( AddressException aex ) {
				JSFUtil.addMessage( "The bcc address '" + bccAddress + "' is invalid please update it." );
				logger.debug( "The bcc address '" + bccAddress + "' is invalid please update it." );
				return null;
			}	
		}

		if ( ApplicationUtil.getAplosContextListener().isDebugMode() && isDivertingEmailsInDebug() ) {
			/*
			 * Add the message after the recipients have been added in case they fail
			 */
			JSFUtil.addMessage( "Emails have been diverted to " + divertEmailAddress );
			logger.debug( "Emails have been diverted to " + divertEmailAddress );
			htmlBody = htmlBody + "<br/><br/><br/><table><tr><td class='divert' style='padding:5px;background-color:#555555;color:#FFFFFF !important;'>Emails have been diverted to "  + divertEmailAddress + " from " + StringUtils.join( singleEmailRecord.getToAddresses(), ",") + "</td></tr></table>";
		}
		if( CommonUtil.isNullOrEmpty( getFromAddress() ) ) {
			setFromAddress( CommonUtil.getAdminUser().getEmailAddress() );
		}
		mimeMessage.setFrom(new InternetAddress(getFromAddress()));
		mimeMessage.setSentDate( new Date() );
		
		/*
		 * Follow this structure for wide compliance
		 * 
		 * mixed
			alternative
				text
				related
					html
					inline image
					inline image
			attachment
			attachment
		 */

		Multipart mainMultipart = new MimeMultipart("mixed");

		Multipart alternativeMultipart = newChild(mainMultipart, "alternative");
		

        if( isSendingPlainText() ) {
	        MimeBodyPart plainTextBodyPart = new MimeBodyPart();
	        plainTextBodyPart.setContent(processedPlainTextBody, "text/plain; charset=UTF-8");
	        alternativeMultipart.addBodyPart(plainTextBodyPart);
		}

        
        final Multipart mpRelated = newChild(alternativeMultipart,"related");

        MimeBodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent( htmlBody, "text/html; charset=UTF-8");
        mpRelated.addBodyPart( htmlBodyPart );
        
		for (FileDetails tempFileDetails : singleEmailRecord.getSaveableAttachments()) {
			MimeBodyPart attachmentBodyPart = new MimeBodyPart();

			String filename = tempFileDetails.getFilename();

			if( tempFileDetails instanceof CreatedPrintTemplate && CommonUtil.isNullOrEmpty(filename) ) {
				((CreatedPrintTemplate) tempFileDetails).generateAndSavePDFFile();
				filename = tempFileDetails.getFilename();
			}
			InputStream inputStream = new FileInputStream( tempFileDetails.getFile() );
			/*
			 * TODO 
			 * You shouldn't have to append the extension to the filename below it 
			 * should already be there.  After checking the database there's quite a few file 
			 * names that do not have the extension on that will need to be updated.  Some of them 
			 * are even generated by the Aplos Architecture.
			 */
			String userFriendlyFileName = tempFileDetails.getName();
			if( !userFriendlyFileName.endsWith( "." + tempFileDetails.getExtension() ) ) {
				userFriendlyFileName += "." + tempFileDetails.getExtension();
			}
			
			String mimeType = JSFUtil.getServletContext().getMimeType(tempFileDetails.getFile().getAbsolutePath().toLowerCase());
			if( mimeType == null ) {
				mimeType = "application/unknown"; 
			}
			DataSource dataSource = new InputStreamDataSource( inputStream, userFriendlyFileName, mimeType );
	        attachmentBodyPart.setDataHandler(new DataHandler(dataSource));
	        attachmentBodyPart.setFileName(dataSource.getName());

	        mainMultipart.addBodyPart(attachmentBodyPart);
		}  
		
		if( isAskingForReceipt() ) {
			mimeMessage.addHeader( "Disposition-Notification-To", getFromAddress() );
		}
		
        mimeMessage.setContent(mainMultipart);
        if( isSendingViaQueue() ) {
        	EmailSender.addEmailToQueue(mimeMessage);
        } else {
        	EmailSender.sendMessage(mimeMessage);
        }
        
        if( mimeMessage != null ) {
			singleEmailRecord.setEmailSentDate(new Date());
			singleEmailRecord.setStatus(SingleEmailRecordStatus.SENT);
        }
		singleEmailRecord.saveDetails();
		
        return mimeMessage;
	}
	
    private Multipart newChild(Multipart parent, String alternative) throws MessagingException {
        MimeMultipart child =  new MimeMultipart(alternative);
        final MimeBodyPart mbp = new MimeBodyPart();
        parent.addBodyPart(mbp);
        mbp.setContent(child);
        return child;
    }
	
	public String getJoinedFoldersStr() {
		String folderNames[] = new String[ createEmailFolderListFromSet().size() ];
		if( folderNames.length > 0 ) {
			List<EmailFolder> emailFolderList = createEmailFolderListFromSet();
			for( int i = 0, n = folderNames.length; i < n; i++ ) {
				folderNames[ i ] = emailFolderList.get( i ).getDisplayName();
			}
			return StringUtils.join( folderNames, "," );
		}
		return null;
	}
	
	public String getRowStyle() {
		if( EmailStatus.FLAGGED.equals( getEmailStatus() ) ) {
			return "background-color:#FFBBBB";
		} else if( !EmailStatus.COMPLETE.equals( getEmailStatus() ) ) {
			if( getEmailReadDate() != null ) {
				return "background-color:#CCFFAA";
			} else {
				return "background-color:#88FFDD";
			}
		}
		return "";
	}
	
	public void openSaveableAttachment(FileDetails fileDetails) {
		if( fileDetails instanceof CreatedPrintTemplate && CommonUtil.isNullOrEmpty(fileDetails.getFilename()) ) {
			((CreatedPrintTemplate) fileDetails).generateAndSavePDFFile();
		}
		fileDetails.redirectToAplosUrl();
	}
	
	public void downloadSaveableAttachment(FileDetails fileDetails) {
		if( fileDetails instanceof CreatedPrintTemplate && CommonUtil.isNullOrEmpty(fileDetails.getFilename()) ) {
			((CreatedPrintTemplate) fileDetails).generateAndSavePDFFile();
		}
		fileDetails.redirectToAplosUrl(true);
	}
	
	public List<CreatedPrintTemplate> getCreatedPrintTemplates( boolean isOnlyDocumentGenerated ) {
		List<CreatedPrintTemplate> createdPrintTemplates = new ArrayList<CreatedPrintTemplate>();
		for( FileDetails fileDetails : getSaveableAttachments() ) {
			if( fileDetails instanceof CreatedPrintTemplate ) {
				if( !isOnlyDocumentGenerated || fileDetails.getFilename() != null ) {
					createdPrintTemplates.add( (CreatedPrintTemplate) fileDetails );
				}
			}
		}
		return createdPrintTemplates;
	}
	
	public String getDateCreatedDateTimeStr() {
		return FormatUtil.formatDateTime( getDateCreated(), true );
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		if( CommonUtil.isNullOrEmpty( getEncryptionSalt() ) ) {
			setEncryptionSalt(CommonUtil.getRandomSalt());
		}
		boolean wasNew = isNew();
		super.saveBean(currentUser);
		if( wasNew ) {
			if( getOriginalEmail() != null ) {
				if( EmailType.FORWARD.equals( getEmailType() ) || EmailType.REPLY.equals( getEmailType() )) {
					AplosEmail originalEmail = getOriginalEmail().getSaveableBean();
					if( EmailType.FORWARD.equals( getEmailType() ) ) {
						originalEmail.setForwardedEmail( this );
					} else if( EmailType.REPLY.equals( getEmailType() )) {
						originalEmail.setRepliedEmail( this );
					}
					originalEmail.saveDetails();
				}
			}
		}
		for( EmailFolder emailFolder : createEmailFolderListFromSet() ) {
			emailFolder.aplosEmailAction( EmailActionType.SAVED, this );
		}
	}
	
	public UrlEncrypter getOrCreateUrlEncrypter() {
		if( getUrlEncrypter() == null ) {
			setUrlEncrypter( createUrlEncrypter() );
		}
		
		return getUrlEncrypter();
	}
	
	public UrlEncrypter createUrlEncrypter() {
		return new UrlEncrypter( "aplosPassPhrase", getEncryptionSalt(), 5 );
	}
	
	public void updateEmailFolders( Set<SingleEmailRecord> singleEmailRecordSet ) {
		List<EmailFolder> emailFolderList = createEmailFolderListFromSet();
    	for( int i = 0, n = emailFolderList.size(); i < n; i++ ) {
        	updateEmailFolder( emailFolderList.get( i ) );	
    	} 
        
        if( isUsingEmailSourceAsOwner() ) {
        	for( SingleEmailRecord singleEmailRecord : singleEmailRecordSet ) {
        		if( singleEmailRecord.getBulkEmailSource() instanceof EmailFolder && !getEmailFolders().contains( singleEmailRecord.getBulkEmailSource() ) ) {
        			updateEmailFolder( (EmailFolder) singleEmailRecord.getBulkEmailSource() );
        		}
        	}
        }
	}
	
	public void updateEmailFolder( EmailFolder emailFolder ) {
        if( emailFolder != null ) {
    		emailFolder.aplosEmailAction(EmailActionType.SENT, this);
        }
	}
	
	public void setToAddress( String toAddress ) {
		toAddresses.clear();
		toAddresses.add( toAddress );
	}

	public static String getDivertEmailAddress() {
		try {
			if( InetAddress.getLocalHost().getHostName().equalsIgnoreCase( "targaryan-PC" ) ) {
				return "nick@aplossystems.co.uk";
			} else {
				return "info@aplossystems.co.uk";
			}
		} catch( UnknownHostException unEx ) {
			return "info@aplossystems.co.uk";
		}
	}
	
	public String getJoinedToAddresses() {
		return StringUtils.join( getToAddresses(), ", " );
	}

	public void addToAddress( String toAddress ) {
		toAddresses.add( toAddress );
	}

	public void addCcAddress( String ccAddress ) {
		ccAddresses.add( ccAddress );
	}

	public void addBccAddress( String bccAddress ) {
		bccAddresses.add( bccAddress );
	}

	public void addBulkEmailSource( BulkEmailSource bulkEmailSource ) {
		getMessageSourceList().add( bulkEmailSource );
	}

	public void removeBulkEmailSource( BulkEmailSource bulkEmailSource ) {
		getMessageSourceList().remove( bulkEmailSource );
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void appendFooterAndHeader( StringBuffer htmlBodyStrBuf, SingleEmailRecord singleEmailRecord ) {
		if ( getEmailFrame() != null && !CommonUtil.isEmptyEditorContent(getEmailFrame().getHeader()) ) {
			htmlBodyStrBuf.insert( 0, getEmailFrame().parseHeader(this, singleEmailRecord) );
		}
		if ( getOuterEmailFrame() != null && !CommonUtil.isEmptyEditorContent(getOuterEmailFrame().getHeader()) ) {
			htmlBodyStrBuf.insert( 0, getOuterEmailFrame().parseHeader(this, singleEmailRecord) );
		}
		if ( getEmailFrame() != null && !CommonUtil.isEmptyEditorContent(getEmailFrame().getFooter()) ) {
			htmlBodyStrBuf.append( getEmailFrame().parseFooter(this, singleEmailRecord) );
		}
		if ( getOuterEmailFrame() != null && !CommonUtil.isEmptyEditorContent(getOuterEmailFrame().getFooter()) ) {
			htmlBodyStrBuf.append( getOuterEmailFrame().parseFooter(this, singleEmailRecord) );
		}
	}

	public void setToAddresses(String[] toAddresses) {
		this.toAddresses = new ArrayList<String>();
		for( int i = 0, n = toAddresses.length; i < n; i++ ) {
			this.toAddresses.add( toAddresses[ i ] );
		}
	}

	public void setCcAddresses(String[] ccAddresses) {
		this.ccAddresses = new ArrayList<String>();
		for( int i = 0, n = ccAddresses.length; i < n; i++ ) {
			this.ccAddresses.add( ccAddresses[ i ] );
		}
	}

	public void setBccAddresses(String[] bccAddresses) {
		this.bccAddresses = new ArrayList<String>();
		for( int i = 0, n = bccAddresses.length; i < n; i++ ) {
			this.bccAddresses.add( bccAddresses[ i ] );
		}
	}

	public void setToAddresses(List<String> toAddresses) {
		this.toAddresses = toAddresses;
	}

	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setRemoveDuplicateToAddresses(boolean removeDuplicateToAddresses) {
		this.removeDuplicateToAddresses = removeDuplicateToAddresses;
	}

	public boolean isRemoveDuplicateToAddresses() {
		return removeDuplicateToAddresses;
	}

	public void setCcAddresses(List<String> ccAddresses) {
		this.ccAddresses = ccAddresses;
	}

	public List<String> getCcAddresses() {
		return ccAddresses;
	}

	public void setSaveableAttachments(List<FileDetails> saveableAttachments) {
		this.saveableAttachments = saveableAttachments;
	}

	public List<FileDetails> getSaveableAttachments() {
		return saveableAttachments;
	}

	public String getPlainTextBody() {
		return plainTextBody;
	}

	public void setPlainTextBody(String plainTextBody) {
		this.plainTextBody = plainTextBody;
	}
	
	public String getEmailSentDateStr() {
		return FormatUtil.formatDate( getEmailSentDate() );
	}
	
	public String getEmailSentDateTimeStr() {
		return FormatUtil.formatDateTime( getEmailSentDate(), true );
	}

	public Date getEmailSentDate() {
		return emailSentDate;
	}

	public void setEmailSentDate(Date emailSentDate) {
		this.emailSentDate = emailSentDate;
	}

	public boolean isSendingPlainText() {
		return isSendingPlainText;
	}

	public void setSendingPlainText(boolean isSendingPlainText) {
		this.isSendingPlainText = isSendingPlainText;
	}

	public EmailTemplate getEmailTemplate() {
		return emailTemplate;
	}

	public void setEmailTemplate(EmailTemplate emailTemplate) {
		this.emailTemplate = emailTemplate;
	}

	public List<BulkMessageSource> getMessageSourceList() {
		return messageSourceList;
	}

	public void setMessageSourceList(List<BulkMessageSource> emailSourceList) {
		this.messageSourceList = emailSourceList;
	}

	public EmailFrame getEmailFrame() {
		return emailFrame;
	}

	public void setEmailFrame(EmailFrame emailFrame) {
		this.emailFrame = emailFrame;
	}

	public EmailFrame getOuterEmailFrame() {
		return outerEmailFrame;
	}

	public void setOuterEmailFrame(EmailFrame outerEmailFrame) {
		this.outerEmailFrame = outerEmailFrame;
	}

	public AplosEmail getOriginalEmail() {
		return originalEmail;
	}

	public void setOriginalEmail(AplosEmail originalEmail) {
		this.originalEmail = originalEmail;
	}

	public MessageGenerationType getEmailGenerationType() {
		return emailGenerationType;
	}

	public void setEmailGenerationType(MessageGenerationType emailGenerationType) {
		this.emailGenerationType = emailGenerationType;
	}
	
	public String getEncryptionSalt() {
		return encryptionSalt;
	}

	public void setEncryptionSalt(String encryptionSalt) {
		this.encryptionSalt = encryptionSalt;
	}

	public EmailGenerator getEmailGenerator() {
		return emailGenerator;
	}

	public void setEmailGenerator(EmailGenerator emailGenerator) {
		this.emailGenerator = emailGenerator;
	}
	
	public static AplosEmail sendEmail( EmailTemplateEnum emailTemplateEnum, BulkEmailSource bulkEmailSource ) {
		AplosEmail aplosEmail = new AplosEmail( emailTemplateEnum, bulkEmailSource, bulkEmailSource );
		aplosEmail.sendAplosEmailToQueue();
		return aplosEmail;
	}
	
	public void addEmailFolder( EmailFolder emailFolder ) {
		if( emailFolder != null ) {
			getEmailFolders().add( emailFolder );
		}
	}

	public Date getAutomaticSendDate() {
		return automaticSendDate;
	}

	public void setAutomaticSendDate(Date automaticSendDate) {
		this.automaticSendDate = automaticSendDate;
	}

	public boolean isUsingEmailSourceAsOwner() {
		return isUsingEmailSourceAsOwner;
	}

	public void setUsingEmailSourceAsOwner(boolean isUsingEmailSourceAsOwner) {
		this.isUsingEmailSourceAsOwner = isUsingEmailSourceAsOwner;
	}

	public List<String> getBccAddresses() {
		return bccAddresses;
	}

	public void setBccAddresses(List<String> bccAddresses) {
		this.bccAddresses = bccAddresses;
	}

	public int getEmailSentCount() {
		return emailSentCount;
	}

	public void setEmailSentCount(int emailSentCount) {
		this.emailSentCount = emailSentCount;
	}

	public MailServerSettings getMailServerSettings() {
		return mailServerSettings;
	}

	public void setMailServerSettings(MailServerSettings mailServerSettings) {
		this.mailServerSettings = mailServerSettings;
	}

	public boolean isAskingForReceipt() {
		return isAskingForReceipt;
	}

	public void setAskingForReceipt(boolean isAskingForReceipt) {
		this.isAskingForReceipt = isAskingForReceipt;
	}

	public Integer getSendStartIdx() {
		return sendStartIdx;
	}

	public void setSendStartIdx(Integer sendStartIdx) {
		this.sendStartIdx = sendStartIdx;
	}

	public Integer getMaxSendQuantity() {
		return maxSendQuantity;
	}

	public void setMaxSendQuantity(Integer maxSendQuantity) {
		this.maxSendQuantity = maxSendQuantity;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Date getEmailReadDate() {
		return emailReadDate;
	}

	public void setEmailReadDate(Date emailReadDate) {
		this.emailReadDate = emailReadDate;
	}

	public Set<EmailFolder> getEmailFolders() {
		return emailFolders;
	}

	public void setEmailFolders(Set<EmailFolder> emailFolders) {
		this.emailFolders = emailFolders;
	}

	public int getIncomingReadRetryCount() {
		return incomingReadRetryCount;
	}

	public void setIncomingReadRetryCount(int incomingReadRetryCount) {
		this.incomingReadRetryCount = incomingReadRetryCount;
	}

	public boolean isIncomingEmailDeleted() {
		return isIncomingEmailDeleted;
	}

	public void setIncomingEmailDeleted(boolean isIncomingEmailDeleted) {
		this.isIncomingEmailDeleted = isIncomingEmailDeleted;
	}

	public AplosEmail getForwardedEmail() {
		return forwardedEmail;
	}

	public void setForwardedEmail(AplosEmail forwardedEmail) {
		this.forwardedEmail = forwardedEmail;
	}

	public AplosEmail getRepliedEmail() {
		return repliedEmail;
	}

	public void setRepliedEmail(AplosEmail repliedEmail) {
		this.repliedEmail = repliedEmail;
	}

	public EmailType getEmailType() {
		return emailType;
	}

	public void setEmailType(EmailType emailType) {
		this.emailType = emailType;
	}
	
	@Override
	public void hardDelete() {
		if( EmailType.INCOMING.equals( getEmailType() ) ) {
			for( int i = 0, n = saveableAttachments.size(); i < n; i++ ) {
				saveableAttachments.get( i ).hardDelete();
			}
			EmailManager.addEmailForDeletion( this );
		}
		ApplicationUtil.executeSql( "UPDATE AplosEmail SET originalEmail_id = null WHERE originalEmail_id = " + getId() );
		ApplicationUtil.executeSql( "UPDATE AplosEmail SET originalEmail_id = null WHERE repliedEmail_id = " + getId() );
		ApplicationUtil.executeSql( "UPDATE AplosEmail SET originalEmail_id = null WHERE forwardedEmail_id = " + getId() );
		super.hardDelete();
	}

	public Date getHardDeleteDate() {
		return hardDeleteDate;
	}

	public void setHardDeleteDate(Date hardDeleteDate) {
		this.hardDeleteDate = hardDeleteDate;
	}

	public EmailStatus getEmailStatus() {
		return emailStatus;
	}

	public void setEmailStatus(EmailStatus emailStatus) {
		this.emailStatus = emailStatus;
	}

	public String getHeadContent() {
		return headContent;
	}

	public void setHeadContent(String headContent) {
		this.headContent = headContent;
	}

	public boolean isDivertingEmailsInDebug() {
		return isDivertingEmailsInDebug;
	}

	public void setDivertingEmailsInDebug(boolean isDivertingEmailsInDebug) {
		this.isDivertingEmailsInDebug = isDivertingEmailsInDebug;
	}

	public UrlEncrypter getUrlEncrypter() {
		return urlEncrypter;
	}

	public void setUrlEncrypter(UrlEncrypter urlEncrypter) {
		this.urlEncrypter = urlEncrypter;
	}
	
	public boolean isSendingViaQueue() {
		return isSendingViaQueue;
	}

	public void setSendingViaQueue(boolean isSendingViaQueue) {
		this.isSendingViaQueue = isSendingViaQueue;
	}
	
	public SingleEmailRecord getSingleEmailRecord( BulkEmailSource bulkEmailSource, boolean createIfMissing ) {
		if( getSingleEmailRecordMap() != null && getSingleEmailRecordMap().get( bulkEmailSource ) != null ) {
			return getSingleEmailRecordMap().get( bulkEmailSource );
		}
		
		SingleEmailRecord singleEmailRecord = null;
		if( !isSingleEmailRecordMapInitialised() ) {
			PersistentClass persistentClass = ApplicationUtil.getPersistentClass( bulkEmailSource.getClass() ).getDbPersistentClass();
			BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
			singleEmailRecordDao.addWhereCriteria( "bean.bulkEmailSource.id = " + bulkEmailSource.getMessageSourceId() );
			singleEmailRecordDao.addWhereCriteria( "bean.bulk_email_source_type = '" + persistentClass.getDiscriminatorValue() + "'" );
			singleEmailRecord = singleEmailRecordDao.getFirstBeanResult();
		}
		
		if( singleEmailRecord == null && createIfMissing ) {
			singleEmailRecord = new SingleEmailRecord(this, bulkEmailSource);
			getSingleEmailRecordMap().put( bulkEmailSource, singleEmailRecord );
		}
		return singleEmailRecord;
	}

	public Map<BulkEmailSource,SingleEmailRecord> getSingleEmailRecordMap() {
		return singleEmailRecordMap;
	}

	public boolean isSingleEmailRecordMapInitialised() {
		return isSingleEmailRecordMapInitialised;
	}

	public void setSingleEmailRecordMapInitialised(
			boolean isSingleEmailRecordMapInitialised) {
		this.isSingleEmailRecordMapInitialised = isSingleEmailRecordMapInitialised;
	}

	public Set<BulkEmailSource> getRemovedSourceSet() {
		return removedSourceSet;
	}

	public void setRemovedSourceList(Set<BulkEmailSource> removedSourceSet) {
		this.removedSourceSet = removedSourceSet;
	}

	public class AplosMimeMessage extends MimeMessage {
		
		public AplosMimeMessage( Session session ) {
			super( session );
		}
		
		@Override
		protected void updateMessageID() throws MessagingException {
			super.updateMessageID();
			String[] messageId = getHeader( "Message-ID" );
			if( messageId[ 0 ] != null ) {
				messageId[ 0 ] = messageId[ 0 ].replace( "$", "" );
			}
			setHeader( "Message-ID", messageId[ 0 ] );
		}
	}
	
	/*
	   delete FROM aplosemail_bccaddresses where aplosemail_id = id_here;
	   delete FROM aplosemail_ccaddresses where aplosemail_id = id_here;
	   delete FROM aplosemail_emailfolders where aplosemail_id = id_here;
	   delete FROM aplosemail_filedetails where aplosemail_id = id_here;
	   delete FROM aplosemail_messagesourcelist where aplosemail_id = id_here;
	   delete FROM aplosemail_toaddresses where aplosemail_id = id_here;
	   delete FROM aplosemail_website where aplosemail_id = id_here;
       update aplosemail set originalEmail_id = null where originalEmail_id = id_here;
	   delete FROM aplosemail where id = id_here;
	 */
}
