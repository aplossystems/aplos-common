package com.aplos.common.threads;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.interfaces.EmailManagerJob;
import com.aplos.common.utils.ApplicationUtil;

public class EmailManager extends AplosThread {
	private static PriorityBlockingQueue<EmailManagerJob> jobQueue = new PriorityBlockingQueue<EmailManagerJob>( 10, new EmailManagerJobComparator() );
	private static boolean isDownloadJobInQueue = false;

	public static void addEmailForDeletion( AplosEmail aplosEmail ) {
		if( aplosEmail != null ) {
			EmailManagerJob emailManagerJob = new EmailDeletionJob( aplosEmail );
			jobQueue.add( emailManagerJob );
		}
	}
	
	@Override
	public int getQueueSize() {
		return jobQueue.size();
	}
	
	public static void addDownloadEmailJob( List<MailServerSettings> mailServerSettingList, Map<String,Date> deletableEmailAddressMap ) {
		if( !isDownloadJobInQueue && mailServerSettingList != null && deletableEmailAddressMap != null ) {
			EmailManagerJob emailManagerJob = new IncomingEmailJob(mailServerSettingList, deletableEmailAddressMap);
			isDownloadJobInQueue = true;
			jobQueue.add( emailManagerJob );
		}
	}

	public void stopThread() {
		setRunning( false );
	}

	@Override
	public void run() {
		while( isRunning() ) {
			try {
				if( !isRunning() ) {
					break;
				}
				EmailManagerJob emailManagerJob = jobQueue.take();
//				HibernateUtil.getCurrentSession().beginTransaction();
				List<EmailDeletionJob> deleteEmailJobList = new ArrayList<EmailDeletionJob>();
				boolean isCheckingForDownloadEmailJobs = true;
				while( isCheckingForDownloadEmailJobs ) {
					if( emailManagerJob instanceof EmailDeletionJob ) {
						deleteEmailJobList.add( (EmailDeletionJob) emailManagerJob );
						while( jobQueue.peek() instanceof EmailDeletionJob ) {
							emailManagerJob = jobQueue.take();
							deleteEmailJobList.add( (EmailDeletionJob) emailManagerJob );
						}
						sleep( 1000 );
						if( jobQueue.peek() instanceof EmailDeletionJob ) {
							emailManagerJob = jobQueue.take();
						} else {
							isCheckingForDownloadEmailJobs = false;
						}
					} else {
						isCheckingForDownloadEmailJobs = false;
					}
				}
				
				if( deleteEmailJobList.size() > 0 ) {
					EmailDeletionJob.deleteMailFromServer(deleteEmailJobList);
				} else if( emailManagerJob instanceof IncomingEmailJob ) {
					((IncomingEmailJob) emailManagerJob).downloadPop3();
					isDownloadJobInQueue = false;
				}
//				HibernateUtil.getCurrentSession().getTransaction().commit();
//				HibernateUtil.getCurrentSession().close();
				
				
			} catch( InterruptedException iEx ) {
				ApplicationUtil.handleError( iEx );
				break;
			} catch( Exception ex ) {
				ApplicationUtil.handleError( ex );
				break;
			} 
		}
	}
	
	private static class EmailManagerJobComparator implements Comparator<EmailManagerJob> {
		@Override
		public int compare(EmailManagerJob o1, EmailManagerJob o2) {
			if( o1 instanceof IncomingEmailJob && o2 instanceof EmailDeletionJob ) {
				return 1;
			} else if( o1 instanceof EmailDeletionJob && o2 instanceof IncomingEmailJob ) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
