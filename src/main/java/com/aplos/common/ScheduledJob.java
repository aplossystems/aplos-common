package com.aplos.common;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Website;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public abstract class ScheduledJob<T> extends AplosBean implements Callable<T> {
	private boolean isRunning;
	private Date scheduledDate;
	private Date lastScheduledDate;
	@ManyToOne
	private Website website;
	
	public ScheduledJob() {
	}
	
	@Override
	public T call() throws Exception {
		T returnValue = null;
		boolean startSession = false;
		try {
			setLastScheduledDate(new Date());
			if( JSFUtil.getSessionTemp() == null ) {
				startSession = true;
			}
			if( startSession ) {
				ApplicationUtil.getAplosContextListener().startThreadSession(getWebsite());
//				HibernateUtil.getCurrentSession().beginTransaction();
			}
			returnValue = executeCall();
			if( startSession ) {
				ApplicationUtil.getAplosContextListener().endThreadSession();
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError(ex);
			throw ex;
		} finally {
//			if( startSession ) {
//				try {
//					HibernateUtil.getCurrentSession().getTransaction().commit();
//				} catch( Exception ex ) {
//					HibernateUtil.getCurrentSession().getTransaction().rollback();
//				}
//			}
		}
		return returnValue;
	}
	
	public Date getNextScheduledDate() {
		Calendar cal = Calendar.getInstance();
		Date nextScheduledDate = null;
		cal.setTime(getScheduledDate()); //the check might occur after the set time so if we just used current time the schedule would slip
		Long interval = getIntervalQuantity(cal.getTime());
		if( interval < -1 ) {
			ApplicationUtil.handleError( new Exception( "Interval for scheduled job must be greater than one : " + getClass().getSimpleName() ));
		}
		
		if (interval != null &&	interval > 0) { //schedule the next run or cancel future execution
			/*
			 * Keep on adding the interval into the next job is in the future otherwise you
			 * might get the job being run mutliple times.
			 */
			while( cal.getTime().before( new Date() ) ) {
				cal.add(getIntervalUnit(), interval.intValue());
			}
			nextScheduledDate = cal.getTime();
		} else {
			return null;
		}
		return nextScheduledDate;
	}
	
	public abstract T executeCall() throws Exception;
	
	public Long getIntervalQuantity(Date previousExecutionDate) {
		return 1l;
	}
	
	public Integer getIntervalUnit() {
		return Calendar.DAY_OF_YEAR;
	}
	
	public Calendar getFirstExecutionTime() {
		Calendar cal = GregorianCalendar.getInstance();
		FormatUtil.resetTime(cal);
		cal.add( Calendar.DAY_OF_YEAR, 1 );
		cal.add( Calendar.HOUR, 1 );
		return cal;
	}
	
	public String getScheduledDateStdStr() {
		return FormatUtil.formatDateTime( getScheduledDate(), true );
	}
	
	public String getLastScheduledDateStdStr() {
		return FormatUtil.formatDateTime( getLastScheduledDate(), true );
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
	public Date getScheduledDate() {
		return scheduledDate;
	}
	public void setScheduledDate(Date scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	public Date getLastScheduledDate() {
		return lastScheduledDate;
	}

	public void setLastScheduledDate(Date lastScheduledDate) {
		this.lastScheduledDate = lastScheduledDate;
	}

	public Website getWebsite() {
		return website;
	}

	public void setWebsite(Website website) {
		this.website = website;
	}
	
	
}