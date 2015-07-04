package com.aplos.common.scheduledjobs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.DeletableEmailAddress;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.threads.EmailManager;
import com.aplos.common.utils.CommonUtil;

@Entity
public class IncomingAplosEmailJob extends ScheduledJob<Boolean> {
	private static final long serialVersionUID = -896524799747276985L;

	public IncomingAplosEmailJob() {
	}
	
	@Override
	public Boolean executeCall() throws Exception {
		Map<String, MailServerSettings> mailServerSettingsMap = new HashMap<String, MailServerSettings>();

		MailServerSettings mailServerSettings = CommonConfiguration.getCommonConfiguration().getMailServerSettings();
//		HibernateUtil.initialise(mailServerSettings, false);
		mailServerSettingsMap.put( mailServerSettings.getEmailAddress(), mailServerSettings);
		
		List<Website> websiteList = new BeanDao( Website.class ).getAll();
		for( int i = 0, n = websiteList.size(); i < n; i++ ) {
			mailServerSettings = websiteList.get( i ).getMailServerSettings();
			mailServerSettingsMap.put( mailServerSettings.getEmailAddress(), mailServerSettings);
		}
		
		BeanDao systemUserDao = new BeanDao( SystemUser.class );
		systemUserDao.addWhereCriteria( "bean.isUsingOwnMailServerSettings = true" );
		systemUserDao.addWhereCriteria( "bean.mailServerSettings.isUsingIncomingEmail = true" );
		List<SystemUser> systemUserList = systemUserDao.getAll();
		for( int i = 0, n = systemUserList.size(); i < n; i++ ) {
			mailServerSettings = systemUserList.get( i ).getMailServerSettings();
			mailServerSettingsMap.put( mailServerSettings.getEmailAddress(), mailServerSettings);
		}

		BeanDao deletableEmailDao = new BeanDao( DeletableEmailAddress.class );
		List<DeletableEmailAddress> deletableEmailAddresses = new BeanDao( DeletableEmailAddress.class ).getAll();
		Map<String,Date> deletableEmailAddressMap = new HashMap<String,Date>();
		Calendar cal = GregorianCalendar.getInstance();
		for( int i = 0, n = deletableEmailAddresses.size(); i < n; i++ ) {
			cal.setTime(new Date());
			cal.add(Calendar.DAY_OF_YEAR, deletableEmailAddresses.get( i ).getDaysToDeletion() );
			deletableEmailAddressMap.put( deletableEmailAddresses.get( i ).getEmailAddress(), cal.getTime());
		}

		List<MailServerSettings> activeMailServers = new ArrayList<MailServerSettings>();
		for( MailServerSettings tempMailServerSettings : mailServerSettingsMap.values() ) {
			if( tempMailServerSettings != null && tempMailServerSettings.isUsingIncomingEmail() 
					&& !(CommonUtil.isNullOrEmpty( tempMailServerSettings.getIncomingHost() ) 
					|| CommonUtil.isNullOrEmpty( tempMailServerSettings.getIncomingUsername() )
					|| CommonUtil.isNullOrEmpty( tempMailServerSettings.getIncomingPassword() )) ) {
				activeMailServers.add( tempMailServerSettings );
			}
		}
		
		
		if( activeMailServers.size() == 0 ) {
			setRunning(false);
		} else {
			EmailManager.addDownloadEmailJob( activeMailServers, deletableEmailAddressMap );
		}
		return isRunning();
	}

	@Override
	public Long getIntervalQuantity(Date previousExecutionDate) {
		return (long) 5;
	}

	@Override
	public Integer getIntervalUnit() {
		return Calendar.MINUTE;
	}

	@Override
	public Calendar getFirstExecutionTime() {
		return null;
	}

}
