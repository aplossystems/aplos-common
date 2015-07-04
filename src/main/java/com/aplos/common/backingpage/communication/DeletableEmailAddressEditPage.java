package com.aplos.common.backingpage.communication;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.DeletableEmailAddress;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=DeletableEmailAddress.class)
public class DeletableEmailAddressEditPage extends EditPage {
	private static final long serialVersionUID = 1007843241691296427L;
	
	private boolean isRunningOnExistingEmails = false;
	
	public DeletableEmailAddressEditPage() {
		DeletableEmailAddress deletableEmailAddress = resolveAssociatedBean();
		if( deletableEmailAddress != null ) {
			if( deletableEmailAddress.isNew() ) {
				setRunningOnExistingEmails(true);
			}
		}
	}
	
	@Override
	public boolean saveBean() {
		boolean savedSuccessfully = super.saveBean();
		if( isRunningOnExistingEmails() ) {
			DeletableEmailAddress deletableEmailAddress = resolveAssociatedBean();
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime( new Date() ); 
			cal.add(Calendar.DAY_OF_YEAR, deletableEmailAddress.getDaysToDeletion());
			Date hardDeleteDate = cal.getTime();
			
			BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
			aplosEmailDao.addWhereCriteria( "bean.fromAddress LIKE :emailAddress" );
			aplosEmailDao.addWhereCriteria( "bean.hardDeleteDate = null" );
			aplosEmailDao.setIsReturningActiveBeans(null);
			aplosEmailDao.setNamedParameter( "emailAddress", deletableEmailAddress.getEmailAddress() );
			List<AplosEmail> aplosEmailList = aplosEmailDao.setIsReturningActiveBeans(null).getAll();
			for( int i = 0, n = aplosEmailList.size(); i < n; i++ ) {
				aplosEmailList.get(i).setHardDeleteDate(hardDeleteDate);
				aplosEmailList.get( i ).saveDetails();
			}
			return true;
		}
		return savedSuccessfully;
	}

	public boolean isRunningOnExistingEmails() {
		return isRunningOnExistingEmails;
	}

	public void setRunningOnExistingEmails(boolean isRunningOnExistingEmails) {
		this.isRunningOnExistingEmails = isRunningOnExistingEmails;
	}
	
}