package com.aplos.common.scheduledjobs;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.utils.FormatUtil;

@Entity
public class AutomaticAplosEmailJob extends ScheduledJob<Integer> {
	private static final long serialVersionUID = 6224803353500535299L;

	public AutomaticAplosEmailJob() {	}
	
	@Override
	public Integer executeCall() throws Exception {
		BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
		aplosEmailDao.addWhereCriteria( "bean.emailSentDate IS NULL" );
		/*
		 * The is not null check adds a huge DB performance improvement
		 */
		aplosEmailDao.addWhereCriteria( "bean.automaticSendDate IS NOT NULL AND bean.automaticSendDate < NOW()" );
		List<AplosEmail> aplosEmailList = aplosEmailDao.getAll();
		for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
			aplosEmailList.get( i ).sendAplosEmailToQueue();
		}
		return aplosEmailList.size();
	}

	@Override
	public Long getIntervalQuantity(Date previousExecutionDate) {
		return (long) 1;
	}

	@Override
	public Integer getIntervalUnit() {
		return Calendar.MINUTE;
	}

	@Override
	public Calendar getFirstExecutionTime() {
		Calendar cal = Calendar.getInstance();
		FormatUtil.resetTime(cal);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		return cal;
	}

}
