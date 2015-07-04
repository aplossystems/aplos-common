package com.aplos.common.scheduledjobs;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.SmsBundlePurchase;
import com.aplos.common.enums.SmsBundleState;
import com.aplos.common.utils.FormatUtil;

@Entity
public class SmsBundleActivationJob extends ScheduledJob<Boolean> {
	private static final long serialVersionUID = 2321145260661336605L;

	public SmsBundleActivationJob() {}
	
	@Override
	public Boolean executeCall() throws Exception {
		BeanDao smsBundlePurchaseDao = new BeanDao( SmsBundlePurchase.class );
		smsBundlePurchaseDao.addWhereCriteria( "bean.smsBundleState = " + SmsBundleState.UNSTARTED.ordinal() );
		smsBundlePurchaseDao.addWhereCriteria( "bean.validFromDate < NOW()" );
		List<SmsBundlePurchase> smsBundlePurchases = smsBundlePurchaseDao.getAll();
		
		for( int i = 0, n = smsBundlePurchases.size(); i < n; i++ ) {
			SmsBundlePurchase smsBundlePurchase = smsBundlePurchases.get( i ).getSaveableBean();
			smsBundlePurchase.setSmsBundleState( SmsBundleState.LIVE );
			smsBundlePurchase.saveDetails();
			smsBundlePurchase.getSmsAccount().addActiveBundlePurchase( smsBundlePurchase );
		}
		
		smsBundlePurchaseDao = new BeanDao( SmsBundlePurchase.class );
		smsBundlePurchaseDao.addWhereCriteria( "bean.smsBundleState = " + SmsBundleState.LIVE.ordinal() );
		smsBundlePurchaseDao.addWhereCriteria( "bean.validUntilDate < DATE_ADD(NOW(),INTERVAL -1 DAY)" );
		smsBundlePurchases = smsBundlePurchaseDao.getAll();
		
		for( int i = 0, n = smsBundlePurchases.size(); i < n; i++ ) {
			SmsBundlePurchase smsBundlePurchase = smsBundlePurchases.get( i ).getSaveableBean();
			smsBundlePurchase.setSmsBundleState( SmsBundleState.EXPIRED );
			smsBundlePurchase.saveDetails();
			smsBundlePurchase.getSmsAccount().removeActiveBundlePurchase( smsBundlePurchase );
		}
		
		
		return true;
	}

}
