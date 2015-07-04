package com.aplos.common.scheduledjobs;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.EmailType;
import com.aplos.common.utils.FormatUtil;

@Entity
public class DeletableEmailAddressJob extends ScheduledJob<Boolean> {

	public DeletableEmailAddressJob() {	}
	
	@Override
	public Boolean executeCall() throws Exception {
		BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
		aplosEmailDao.addWhereCriteria( "bean.hardDeleteDate < NOW()" );
		aplosEmailDao.addWhereCriteria( "bean.emailType = " + EmailType.INCOMING.ordinal() );
		List<AplosEmail> aplosEmailList = aplosEmailDao.setIsReturningActiveBeans(null).getAll();
		for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
//			HibernateUtil.initialise(aplosEmailList.get( i ), true);
			aplosEmailList.get(i).hardDelete();
		}
		return true;
	}

}
