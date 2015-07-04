package com.aplos.common.backingpage.marketing;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.PotentialCompanyCategory;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction.InteractionMethod;
import com.aplos.common.beans.marketing.SalesCallTask;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.enums.UnsubscribeType;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PotentialCompany.class)
public class PotentialCompanyEditPage extends EditPage {
	private static final long serialVersionUID = 921055458936496407L;
	private DataTableState interactionDataTableState;
	private BeanDao interactionBeanDao;
	private static final String INTERACTION_DTS = "interactionDts";
	private PotentialCompanyInteraction newInteraction;

	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		if( continueLoad ) {
			if( getInteractionBeanDao() == null ) {
				setInteractionBeanDao( new BeanDao( PotentialCompanyInteraction.class ) );
				PotentialCompany potentialCompany = resolveAssociatedBean();
				getInteractionBeanDao().addWhereCriteria( "bean.potentialCompany.id = " + potentialCompany.getId() );
			}
			if( getNewInteraction() == null ) {
				setNewInteraction( createNewInteraction() );
			}
			getOrCreateDataTableState();
		}
		return continueLoad;
	}
	
	public String getReminderDateStyle() {
		PotentialCompany potentialCompany = resolveAssociatedBean();
		if( potentialCompany.isReminderDatePast() ) {
			return "color:red;";
		} else {
			return "color:green;";
		}
	}

	public DataTableState getOrCreateDataTableState() {
		if ( getInteractionDataTableState() == null && getInteractionBeanDao() != null ) {
			setInteractionDataTableState( CommonUtil.createDataTableState( this, getClass(), INTERACTION_DTS, getInteractionBeanDao() ) );
		}
		return getInteractionDataTableState();
	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel( DataTableState dataTableState, BeanDao aqlBeanDao) {
		if( INTERACTION_DTS.equals( dataTableState.getTableIdentifier() ) ) {
			return new InteractionLdm(dataTableState, aqlBeanDao);
		}
		return null;
	}

	public List<SelectItem> getMethodSelectItems() {
		return CommonUtil.getEnumSelectItems(InteractionMethod.class);
	}
	
	public void remindInDays( int numOfDays ) {
		Date date = new Date();
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime( date );
		cal.add( Calendar.DAY_OF_YEAR, numOfDays );
		PotentialCompany potentialCompany = resolveAssociatedBean();
		potentialCompany.setReminderDate( cal.getTime() );
		potentialCompany.saveDetails();
	}
	
	public void createNoAnswerInteraction() {
		quickInteraction( "No answer" );
	}
	
	public void createAnswerMachineInteraction() {
		quickInteraction( "Answer machine" );
	}
	
	public void createNumberNotWorkingInteraction() {
		quickInteraction( "Number not working" );
	}
	
	public void quickInteraction( String notes ) {
		PotentialCompanyInteraction createNoAnswerInteraction = createNewInteraction();
		createNoAnswerInteraction.setPotentialCompanyStatus(PotentialCompanyStatus.CALL_BACK);
		createNoAnswerInteraction.setNotes(notes);
		createNoAnswerInteraction.saveDetails();
		SalesCallTask salesCallTask = JSFUtil.getBeanFromScope( SalesCallTask.class );
		if( salesCallTask != null ) {
			salesCallTask.nextTask();
		}
	}
	
	public void applyInteraction() {
		getNewInteraction().saveDetails();
		getNewInteraction().addToScope();
	}
	
	public void copyLastInteraction() {
		PotentialCompanyInteraction oldInteraction = JSFUtil.getBeanFromScope( PotentialCompanyInteraction.class );
		setNewInteraction( new PotentialCompanyInteraction() );
		PotentialCompany potentialCompany = resolveAssociatedBean();
		getNewInteraction().setPotentialCompany( potentialCompany );
		if( oldInteraction != null ) {
			oldInteraction.copyFieldsIntoNewInteraction( getNewInteraction() );
		} else {
			getNewInteraction().setPotentialCompanyStatus( potentialCompany.getPotentialCompanyStatus() );
		}
	}
	
	public void newInteraction() {
		if( !newInteraction.isNew() || !CommonUtil.isNullOrEmpty( newInteraction.getNotes() ) ) {
			applyInteraction();
		}
		setNewInteraction( createNewInteraction() );
	}
	
	public PotentialCompanyInteraction createNewInteraction() {
		PotentialCompany potentialCompany = resolveAssociatedBean();
		PotentialCompanyInteraction potentialCompanyInteraction = new PotentialCompanyInteraction();
		SalesCallTask salesCallTask = JSFUtil.getBeanFromScope( SalesCallTask.class );
		if( salesCallTask != null ) {
			potentialCompanyInteraction.setMethod( InteractionMethod.CALL_OUT );
			potentialCompanyInteraction.setPotentialCompanyStatus(PotentialCompanyStatus.CALL_BACK);
		}
		potentialCompanyInteraction.setPotentialCompany(potentialCompany);
		return potentialCompanyInteraction;
	}
	
	public boolean isInteractionValidationRequired() {
		return validationRequired( "applyInteractionBtn" );
	}
	
	public SelectItem[] getCategorySelectItems() {
		return AplosBean.getSelectItemBeans( PotentialCompanyCategory.class );
	}
	
	public List<SelectItem> getStatusSelectItems() {
		return CommonUtil.getEnumSelectItems( PotentialCompanyStatus.class );
	}
	
	public List<SelectItem> getUnsubscribeTypeSelectItems() {
		return CommonUtil.getEnumSelectItemsWithNotSelected( UnsubscribeType.class );
	}

	public DataTableState getInteractionDataTableState() {
		return interactionDataTableState;
	}

	public void setInteractionDataTableState(DataTableState interactionDataTableState) {
		this.interactionDataTableState = interactionDataTableState;
	}

	public BeanDao getInteractionBeanDao() {
		return interactionBeanDao;
	}

	public void setInteractionBeanDao(BeanDao interactionBeanDao) {
		this.interactionBeanDao = interactionBeanDao;
	}
	
	public PotentialCompanyInteraction getNewInteraction() {
		return newInteraction;
	}

	public void setNewInteraction(PotentialCompanyInteraction newInteraction) {
		this.newInteraction = newInteraction;
	}

	public static class InteractionLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = -6645487614756095122L;

		public InteractionLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super( dataTableState, aqlBeanDao );
			dataTableState.setShowingFilterBy(false);
			dataTableState.setShowingSortBy(false);
			dataTableState.setShowingHeader(false);
			dataTableState.setShowingFooter(false);
			dataTableState.setShowingNewBtn(false);
			dataTableState.setShowingShowDeletedBtn(false);
			dataTableState.setMaxRowsOnPage(500);
			dataTableState.getLazyDataModel().getBeanDao().setOrderBy( "bean.contactDateTime DESC" );
		}
	}
}
