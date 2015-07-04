package com.aplos.common.backingpage.marketing;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction.InteractionMethod;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.enums.UnsubscribeType;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PotentialCompanyInteraction.class)
public class PotentialCompanyInteractionEditPage extends EditPage {
	private static final long serialVersionUID = 7653038819717820034L;
	private SingleEmailRecord singleEmailRecord;
	
	public PotentialCompanyInteractionEditPage() {
		addRequiredStateBinding( PotentialCompany.class );
	}
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		
		PotentialCompanyInteraction potentialCompanyInteraction = resolveAssociatedBean();
		if( potentialCompanyInteraction != null ) {
			if( potentialCompanyInteraction.getPotentialCompany().isReadOnly() ) {
				potentialCompanyInteraction.setPotentialCompany( (PotentialCompany) potentialCompanyInteraction.getPotentialCompany().getSaveableBean());
			}
			if( getSingleEmailRecord() == null && potentialCompanyInteraction.getAplosEmail() != null ) {
				BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
				singleEmailRecordDao.addWhereCriteria( "bean.aplosEmail.id = " + potentialCompanyInteraction.getAplosEmail().getId() );
				singleEmailRecordDao.addWhereCriteria( "bean.bulk_email_source_id = " + potentialCompanyInteraction.getPotentialCompany().getId() );
				setSingleEmailRecord( (SingleEmailRecord) singleEmailRecordDao.getFirstBeanResult() );
			}
		}
		return continueLoad;
	}

	public List<SelectItem> getMethodSelectItems() {
		return CommonUtil.getEnumSelectItems(InteractionMethod.class);
	}
	
	public List<SelectItem> getStatusSelectItems() {
		return CommonUtil.getEnumSelectItems( PotentialCompanyStatus.class );
	}
	
	public void remindInDays( int numOfDays ) {
		Date date = new Date();
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime( date );
		cal.add( Calendar.DAY_OF_YEAR, numOfDays );
		PotentialCompanyInteraction potentialCompanyInteraction = JSFUtil.getBeanFromScope( PotentialCompanyInteraction.class );
		potentialCompanyInteraction.getPotentialCompany().setReminderDate( cal.getTime() );
		potentialCompanyInteraction.getPotentialCompany().saveDetails();
	}

	public SingleEmailRecord getSingleEmailRecord() {
		return singleEmailRecord;
	}

	public void setSingleEmailRecord(SingleEmailRecord singleEmailRecord) {
		this.singleEmailRecord = singleEmailRecord;
	}
	
}
