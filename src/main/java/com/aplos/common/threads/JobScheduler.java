package com.aplos.common.threads;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;

import com.aplos.common.ScheduledJob;
import com.aplos.common.beans.Website;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ErrorEmailSender;
import com.aplos.common.utils.FormatUtil;

@ManagedBean
public final class JobScheduler extends AplosThread {

	private static Logger logger = Logger.getLogger( JobScheduler.class );
	private List<ScheduledJob<?>> scheduledJobs = new ArrayList<ScheduledJob<?>>();
	private AplosContextListener aplosContextListener;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private Future<?> currentFuture;
	private Date lastRun;

	public synchronized void startScheduler(AplosContextListener aplosContextListenerRef) {
		this.aplosContextListener = aplosContextListenerRef;
		/*
		 * When a project is first created there isn't a website object in the db yet
		 * hence the check.
		 */
		if( ApplicationUtil.getAplosContextListener().getWebsiteList().size() > 0 ) {
			Website website = ApplicationUtil.getAplosContextListener().getWebsiteList().get(0);
			for (int i=0; i < scheduledJobs.size(); i++) {
				scheduledJobs.get( i ).setWebsite( website );
				scheduledJobs.get(i).setRunning(true);
				scheduledJobs.get(i).saveDetails();
				setJobTimer(scheduledJobs.get(i));
			}
		}
		setRunning(true);
		this.start();
	}
	
	@Override
	public int getQueueSize() {
		return scheduledJobs.size();
	}
	
	public List<ScheduledJob<?>> getScheduledJobs() {
		return scheduledJobs;
	}
	
	public Future<?> runScheduledJob( Class<? extends ScheduledJob> scheduledJobClass ) {
		return executor.submit( findJob( scheduledJobClass ) );
	}

	public void stopThread() {
		logger.info( "Job Scheduler stop requested" );
		if( currentFuture != null ) {
			currentFuture.cancel(true);
		}
		if( executor != null ) {
			executor.shutdownNow();
		}
		setRunning( false );
		this.interrupt();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

	private void setJobTimer(ScheduledJob scheduledJob ) {
		if( scheduledJob.getScheduledDate() == null ) {
			Calendar cal = scheduledJob.getFirstExecutionTime();
			if( cal == null ) {
				scheduledJob.setScheduledDate(new Date()); 
			} else {
				scheduledJob.setScheduledDate(cal.getTime());
				while(scheduledJob.getScheduledDate().before(new Date())) {
					scheduledJob.setScheduledDate(scheduledJob.getNextScheduledDate());
				}
			}
		}
	}

	public void addJob(ScheduledJob job) {
		job = job.getSaveableBean();
		scheduledJobs.add(job);
		setJobTimer(job);
		logger.info("Scheduled job (" + job.getClass().getSimpleName() + ") added for " + job.toString());
	}

	/**
	 * @return the instance of the given job if is already queued, otherwise null
	 * @param job
	 */
	public ScheduledJob findJob(Class<? extends ScheduledJob> comparisonClass) {
		for (ScheduledJob job : scheduledJobs) {
			if (job.getClass().equals(comparisonClass)) {
				return job;
			}
		}
		return null;
	}

	public void clearJobs() {
		scheduledJobs = new ArrayList<ScheduledJob<?>>();
	}

	@Override
	public void run() {
		while (isRunning()) {
			setLastRun(new Date());
			try {
				Date earliestScheduledDate = null;
				if( !aplosContextListener.isDebugMode() ) {
					for (int i=0; i < scheduledJobs.size(); i++) {
						if (scheduledJobs.get(i).isRunning() && scheduledJobs.get(i).getScheduledDate().compareTo(getLastRun()) < 1 ) {
							try {
								currentFuture = executor.submit( scheduledJobs.get( i ) );
								/*
								 * Make sure that the future completes before starting the next batch
								 */
								currentFuture.get();
							} catch ( Exception e ) {
								ErrorEmailSender.sendErrorEmail(null,aplosContextListener, e);
								e.printStackTrace();
							} finally {
								Date nextScheduledDate = scheduledJobs.get( i ).getNextScheduledDate();
								if( nextScheduledDate != null ) {
									scheduledJobs.get(i).setScheduledDate(nextScheduledDate);
									scheduledJobs.get(i).saveDetails();
								} else {
									scheduledJobs.get(i).setRunning(false);
									logger.info("Scheduled job (" + scheduledJobs.get(i).getClass().getSimpleName() + ") ended.");
								}
							}
						}
						
						if( scheduledJobs.get(i) != null && scheduledJobs.get(i).isRunning() ) {
							if( earliestScheduledDate == null || earliestScheduledDate.after( scheduledJobs.get( i ).getScheduledDate() ) ) {
								earliestScheduledDate = scheduledJobs.get( i ).getScheduledDate();
							}
						}
					}
				}
				if (earliestScheduledDate == null) {
					setRunning( false );
				} else {
					/*
					 * Make sure that it sleeps for at least one sec but no more than 5 minutes
					 */
					long sleepMillis = Math.min( 1000 * 60 * 5, Math.max( 1000, earliestScheduledDate.getTime() - new Date().getTime() ) );
					sleep( sleepMillis );
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		logger.info( "Job Scheduler exited" );
	}

	//INTERVALS

	public static long oneDayInMillis() {
		return oneHourInMillis() * 24;
	}

	public static long oneHourInMillis() {
		return oneMinuteInMillis() * 60;
	}

	public static long oneMinuteInMillis() {
		return 1000 * 60;
	}
	
	public String getLastRunStdStr() {
		return FormatUtil.formatDateTime( getLastRun(), true );
	}

	public Date getLastRun() {
		return lastRun;
	}

	private void setLastRun(Date lastRun) {
		this.lastRun = lastRun;
	}
}
