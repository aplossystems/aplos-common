package com.aplos.common.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.utils.ApplicationUtil;

public class BulkSmsSender extends Thread {
    private SmsMessage smsMessage;
    private Set<BulkSmsSource> smsSourceSet;
	private Iterator<BulkSmsSource> smsSourceIter;
	private Website website;

	public BulkSmsSender( SmsMessage smsMessage, Set<BulkSmsSource> smsSourceSet ) {
		this.smsMessage = smsMessage;
		this.smsSourceSet = smsSourceSet;
		this.website = Website.getCurrentWebsiteFromTabSession();
	}
	
	public void startSendingSmsMessages() throws IOException, MessagingException {
		smsSourceIter = smsSourceSet.iterator();

		BulkSmsSource bulkSmsSource;
		if( MessageGenerationType.MESSAGE_GROUPS.equals( smsMessage.getSmsGenerationType() ) || 
				MessageGenerationType.MESSAGE_FINDERS.equals( smsMessage.getSmsGenerationType() ) ) {
			start();
		} else {
			int creditsUsed = 0;
			List<String> toAddresses = new ArrayList<String>();
			Set<String> sentNumbers = new HashSet<String>();
			String tempNumber;
			if( smsSourceIter.hasNext() ) {
				bulkSmsSource = smsSourceIter.next();
				tempNumber = bulkSmsSource.getInternationalNumber().getSafeFullNumber();
				
				if( !smsMessage.isRemovingDuplicateNumbers() || sentNumbers.add( tempNumber ) ) {
					toAddresses.clear();
					toAddresses.add( tempNumber );
					creditsUsed += smsMessage.sendSms( toAddresses, bulkSmsSource);
				}
			}

			smsMessage.setCreditsUsed(creditsUsed);
			smsMessage.setRecipientCount(smsSourceSet.size());
			smsMessage.updateSmsQuota(creditsUsed);
			if( creditsUsed == 0 ) {
				smsMessage.setSmsSentDate(null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		if( Website.getCurrentWebsiteFromTabSession() == null ) {
			Website.addCurrentWebsiteToTabSession(website);
		}
//		HibernateUtil.getCurrentSession().beginTransaction();
		List<String> toAddresses = new ArrayList<String>();
		BulkSmsSource bulkSmsSource;
		int creditsUsed = 0;
		int creditsUsedThisMessage;
		int recipientCount = 0;
		List<BulkSmsSource> sortedSmsSources = new ArrayList<BulkSmsSource>();
		while( smsSourceIter.hasNext() ) {
			sortedSmsSources.add( smsSourceIter.next() );
		}
		Collections.sort( sortedSmsSources, new Comparator<BulkSmsSource>() {
			@Override
			public int compare(BulkSmsSource o1, BulkSmsSource o2) {
				if( o1.getInternationalNumber() == null ) {
					return -1;
				} else if( o2.getInternationalNumber() == null ) {
					return 1;
				} else {
					return o1.getInternationalNumber().getSafeFullNumber().compareTo( o2.getInternationalNumber().getSafeFullNumber() );
				}
			}
		});

		Set<String> sentNumbers = new HashSet<String>();
		String tempNumber;
		try {
			for( int i = 0, n = sortedSmsSources.size(); i < n; i++ ) {
				bulkSmsSource = sortedSmsSources.get( i );
				tempNumber = bulkSmsSource.getInternationalNumber().getSafeFullNumber();
				if( !smsMessage.isRemovingDuplicateNumbers() || sentNumbers.add( tempNumber ) ) {
					toAddresses.clear();
					toAddresses.add( tempNumber );
					creditsUsedThisMessage = smsMessage.sendSms(toAddresses, bulkSmsSource );
					if( creditsUsedThisMessage > 0) {
						recipientCount++;
						creditsUsed += creditsUsedThisMessage;
					}
					/*
					 * Nexmo has throttling of 5 per second, you may not need this for others though
					 * and could have this as a variable passed in
					 */
					sleep(250);
				}
			}
		} catch( InterruptedException irEx ) {
			ApplicationUtil.handleError( irEx );
		}
		
		try {
			smsMessage.setCreditsUsed(creditsUsed);
			smsMessage.setRecipientCount(recipientCount);
			smsMessage.updateSmsQuota(creditsUsed);
			if( creditsUsed == 0 ) {
				smsMessage.setSmsSentDate(null);
			}
			smsMessage.saveDetails();
		} catch( Exception ex ) {
			ApplicationUtil.getAplosContextListener().handleError( ex, false );
		}
//		HibernateUtil.getCurrentSession().getTransaction().commit();
//		HibernateUtil.getCurrentSession().close();
	}
}
