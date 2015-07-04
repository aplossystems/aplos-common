package com.aplos.common.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.enums.SingleEmailRecordStatus;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class BulkEmailSender extends Thread {
    private AplosEmail aplosEmail;
    private Set<SingleEmailRecord> singleEmailRecordSet;
	private Website website;

	public BulkEmailSender( AplosEmail aplosEmail, Set<SingleEmailRecord> singleEmailRecordSet ) {
		this.aplosEmail = aplosEmail;
		this.singleEmailRecordSet = singleEmailRecordSet;
		this.website = Website.getCurrentWebsiteFromTabSession();
	}
	
	public MimeMessage startSendingEmails( boolean saveEmail ) throws IOException, MessagingException {
		MimeMessage mimeMessage = null;
		if( MessageGenerationType.MESSAGE_GROUPS.equals( aplosEmail.getEmailGenerationType() ) || 
				MessageGenerationType.MESSAGE_FINDERS.equals( aplosEmail.getEmailGenerationType() ) ) {
			if( singleEmailRecordSet.size() > 0 ) {
				start();
				mimeMessage = new MimeMessage(JSFUtil.determineMailServerSettings().getMailSession());
			} else {
				JSFUtil.addMessage( "No recipients have been set" );
				return null;
			}
		} else {
			if( aplosEmail.getToAddresses().size() < 1 ) {
				JSFUtil.addMessage( "No recipients have been set" );
				return null;
			}
			
			if( MessageGenerationType.NONE.equals( aplosEmail.getEmailGenerationType() ) ) {
				List<String> processedToAddresses = new ArrayList<String>( aplosEmail.getToAddresses() );
				if( aplosEmail.isRemoveDuplicateToAddresses() ) {
					Set<String> toAddressesSet = new HashSet<String>( processedToAddresses );
					processedToAddresses = new ArrayList<String>( toAddressesSet );
				}
				mimeMessage = aplosEmail.sendEmail( new SingleEmailRecord( aplosEmail, processedToAddresses ) );
			} else if( MessageGenerationType.SINGLE_SOURCE.equals( aplosEmail.getEmailGenerationType() ) ) {
				int sentCount = 0;
				Iterator<SingleEmailRecord> emailSourceIter = singleEmailRecordSet.iterator();
				/*
				 * Currently using an iterator but there should only be one.  I've put this check in to receive
				 * an error if the set size isn't one, if this isn't thrown within a few months
				 * I'll remove the iterator 14/6/2015
				 */
				if( singleEmailRecordSet.size() != 1 ) {
					ApplicationUtil.handleError( new Exception( "Single Source type set but source set size = " + singleEmailRecordSet.size() ), false );
				}
				if( emailSourceIter.hasNext() ) {
					if( emailSourceIter instanceof EmailFolder ) {
						aplosEmail.addEmailFolder( (EmailFolder) emailSourceIter ); 
					}
					sentCount++;
					SingleEmailRecord singleEmailRecord = emailSourceIter.next(); 
					singleEmailRecord.getToAddresses().clear();
					singleEmailRecord.getToAddresses().addAll( aplosEmail.getToAddresses() );
					mimeMessage = aplosEmail.sendEmail(singleEmailRecord);
				}
				if( sentCount > 1 ) {
					aplosEmail.setEmailSentCount( sentCount );
				}
			}
			if( mimeMessage != null ) {
		        aplosEmail.setEmailSentDate(new Date());
			}
	        aplosEmail.updateEmailFolders( singleEmailRecordSet );
		}
        
        if( saveEmail ) {
        	aplosEmail.saveDetails();
        }
		
		return mimeMessage;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		int emailSentCount = 0;
		List<SingleEmailRecord> sortedEmailSources = new ArrayList<SingleEmailRecord>();
		/*
		 * This is to get over the issue of sending from scheduled jobs.  The website
		 * attaches to the thread session and this is a different thread and therefore
		 * a different thread session.
		 */
		boolean isStartThreadSession = false;
		Iterator<SingleEmailRecord> emailSourceIter = singleEmailRecordSet.iterator();
		if( JSFUtil.getSessionTemp() == null ) {
			isStartThreadSession = true;
			AplosContextListener.getAplosContextListener().startThreadSession(website);
		}
		while( emailSourceIter.hasNext() ) {
			sortedEmailSources.add( emailSourceIter.next() );
		}
		Collections.sort( sortedEmailSources, new Comparator<SingleEmailRecord>() {
			@Override
			public int compare(SingleEmailRecord o1, SingleEmailRecord o2) {
				if( o1.getFirstEmailAddress() == null ) {
					return -1;
				} else if( o2.getFirstEmailAddress() == null ) {
					return 1;
				} else {
					return o1.getFirstEmailAddress().compareTo( o2.getFirstEmailAddress() );
				}
			}
		});
		Integer maxSendQuantity = aplosEmail.getMaxSendQuantity();
		if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			maxSendQuantity = 5;
			JSFUtil.addMessage( "Max quantity of " + maxSendQuantity + " set for debug mode" );
		}
		
		SingleEmailRecord singleEmailRecord;
		for( int i = 0, n = sortedEmailSources.size(); i < n; i++ ) {
			singleEmailRecord = sortedEmailSources.get( i );
			try {
				if( maxSendQuantity != null && emailSentCount >= maxSendQuantity ) {
					break;
				}
				if( aplosEmail.getSendStartIdx() == null || i >= aplosEmail.getSendStartIdx() ) {
					if( aplosEmail.sendEmail(singleEmailRecord ) != null ) {
						emailSentCount++;
					} 
				}
			} catch( IOException ioex ) {
				ApplicationUtil.getAplosContextListener().handleError( ioex, false );
			} catch( MessagingException mex ) {
				ApplicationUtil.getAplosContextListener().handleError( mex, false );
			} catch( Exception ex ) {
				ApplicationUtil.getAplosContextListener().handleError( ex, false );
			} finally {
				if( singleEmailRecord != null && singleEmailRecord.isNew() ) {
					singleEmailRecord.setStatus(SingleEmailRecordStatus.SENDING_ERROR);
					singleEmailRecord.saveDetails();
				}
			}
		}
		
        aplosEmail.updateEmailFolders( singleEmailRecordSet );
		
		try {
			aplosEmail.setEmailSentCount( emailSentCount );
			aplosEmail.setEmailSentDate( new Date() );
			aplosEmail.saveDetails();
			ApplicationUtil.executeSql( "UPDATE " + CommonUtil.getBinding( AplosEmail.class ) + " SET emailSentCount = " + emailSentCount + " WHERE id = " + aplosEmail.getId() );
		} catch( Exception ex ) {
			ApplicationUtil.getAplosContextListener().handleError( ex, false );
		} finally {
			if( isStartThreadSession ) {
				ApplicationUtil.getAplosContextListener().endThreadSession();
			}
//			HibernateUtil.getCurrentSession().getTransaction().commit();
//			HibernateUtil.getCurrentSession().close();
		}
	}
}
