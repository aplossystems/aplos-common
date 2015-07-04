package com.aplos.common.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.interfaces.EmailManagerJob;
import com.aplos.common.utils.ApplicationUtil;
import com.sun.mail.pop3.POP3Folder;

public class EmailDeletionJob implements EmailManagerJob {
	private AplosEmail aplosEmail;
	
	public EmailDeletionJob( AplosEmail aplosEmail ) {
		setAplosEmail( aplosEmail );
	}
	
	public static void deleteMailFromServer( List<EmailDeletionJob> deleteEmailManagerJobs ) throws Exception {
		Map<MailServerSettings, List<String>> aplosEmailUidMap = new HashMap<MailServerSettings, List<String>>();
		List<String> tempUidList;
		AplosEmail tempAplosEmail;
		for( int i = 0, n = deleteEmailManagerJobs.size(); i < n; i++ ) {
			tempAplosEmail = deleteEmailManagerJobs.get( i ).getAplosEmail();
			if( tempAplosEmail.getMailServerSettings() != null && tempAplosEmail.getUid() != null ) {
				tempUidList = aplosEmailUidMap.get( tempAplosEmail.getMailServerSettings() );
				if( tempUidList == null ) {
					tempUidList = new ArrayList<String>();
					aplosEmailUidMap.put( tempAplosEmail.getMailServerSettings(), tempUidList );
				}
				tempUidList.add( tempAplosEmail.getUid() );
			}
		}
		
		for( MailServerSettings mailServerSettings : aplosEmailUidMap.keySet() ) {
			POP3Folder folder = null;
			Store store = null;

			try {
				// Create empty properties
				Properties props = new Properties();
		
				// Get the session
				javax.mail.Session session = javax.mail.Session.getInstance(props, null);
		
				// Get the store
				store = session.getStore("pop3");
				store.connect(mailServerSettings.getIncomingHost(), mailServerSettings.getIncomingUsername(), mailServerSettings.getIncomingPassword());
		
				// Get folder
				folder = (POP3Folder)store.getFolder("INBOX");
				folder.open(Folder.READ_WRITE);
				
				Message messages[] = folder.getMessages();
				String tempUid;
				List<String> aplosEmailUids = aplosEmailUidMap.get( mailServerSettings );
				for( int i = 0, n = messages.length; i < n; i++ ) {
					tempUid = folder.getUID(messages[i]);
					for( int j = aplosEmailUids.size() - 1; j > -1; j-- ) {
						if( aplosEmailUids.get( j ).equals( tempUid ) ) {
							messages[ i ].setFlag(Flags.Flag.DELETED, true);
							aplosEmailUids.remove( j );
							break;
						}
					}
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
	}

	public AplosEmail getAplosEmail() {
		return aplosEmail;
	}

	public void setAplosEmail(AplosEmail aplosEmail) {
		this.aplosEmail = aplosEmail;
	}
}
