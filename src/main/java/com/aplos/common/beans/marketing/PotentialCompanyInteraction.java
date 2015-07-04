package com.aplos.common.beans.marketing;


import java.util.Date;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.utils.FormatUtil;

@Entity
@BeanScope(scope=JsfScope.TAB_SESSION)
public class PotentialCompanyInteraction extends AplosBean {
	private static final long serialVersionUID = -5427127130642722751L;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private PotentialCompany potentialCompany;

	private InteractionMethod method = InteractionMethod.EMAIL_OUT;
	@Column(columnDefinition="LONGTEXT")
	private String notes;
	@Column(columnDefinition="LONGTEXT")
	private String nextStep;
	private Date contactDateTime;
	private PotentialCompanyStatus potentialCompanyStatus = PotentialCompanyStatus.UNCONTACTED;
	@ManyToOne(fetch=FetchType.LAZY)
	private AplosEmail aplosEmail;
	@ManyToOne(fetch=FetchType.LAZY)
	private SingleEmailRecord singleEmailRecord;
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialise( getPotentialCompany(), fullInitialisation );
//	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		boolean wasNew = isNew();
		super.saveBean(currentUser);
		if( getPotentialCompany() != null && wasNew && getPotentialCompanyStatus() != null ) {
			setPotentialCompany( (PotentialCompany) getPotentialCompany().getSaveableBean() );
			if( getContactDateTime() != null ) {
				getPotentialCompany().setLastContactedDate( getContactDateTime() );
			} else {
				getPotentialCompany().setLastContactedDate( new Date() );	
			}
			
			getPotentialCompany().setPotentialCompanyStatus( getPotentialCompanyStatus() );
			getPotentialCompany().saveDetails( currentUser );
		}
	}

	public void copyFieldsIntoNewInteraction( PotentialCompanyInteraction potentialCompanyInteraction ) {
		potentialCompanyInteraction.setMethod( getMethod() );
		potentialCompanyInteraction.setNotes( getNotes() );
		potentialCompanyInteraction.setNextStep( getNextStep() );
		if( potentialCompanyInteraction.getPotentialCompany().getReminderDate() == null ) {
			potentialCompanyInteraction.getPotentialCompany().setReminderDate( getPotentialCompany().getReminderDate() );
		}
		potentialCompanyInteraction.setPotentialCompanyStatus( getPotentialCompanyStatus() );
	}

	public String getNextStep() {
		return nextStep;
	}

	public void setNextStep(String nextStep) {
		this.nextStep = nextStep;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Date getContactDateTime() {
		return contactDateTime;
	}
	
	public String getContactDateTimeStr() {
		return FormatUtil.formatDateTime(contactDateTime, true);
	}

	public void setContactDateTime(Date contactDateTime) {
		this.contactDateTime = contactDateTime;
	}
	
	public PotentialCompany getPotentialCompany() {
		return potentialCompany;
	}

	public void setPotentialCompany(PotentialCompany potentialCompany) {
		this.potentialCompany = potentialCompany;
	}

	public InteractionMethod getMethod() {
		return method;
	}

	public void setMethod(InteractionMethod method) {
		this.method = method;
	}

	public PotentialCompanyStatus getPotentialCompanyStatus() {
		return potentialCompanyStatus;
	}

	public void setPotentialCompanyStatus(PotentialCompanyStatus potentialCompanyStatus) {
		this.potentialCompanyStatus = potentialCompanyStatus;
	}

	public AplosEmail getAplosEmail() {
		return aplosEmail;
	}

	public SingleEmailRecord getSingleEmailRecord() {
		return singleEmailRecord;
	}

	public void setSingleEmailRecord(SingleEmailRecord singleEmailRecord) {
		this.singleEmailRecord = singleEmailRecord;
	}

	public static enum InteractionMethod implements LabeledEnumInter {
		EMAIL_IN ( "Email in" ),
		EMAIL_OUT ( "Email out" ),
		CALL_IN ( "Call in" ),
		CALL_OUT ( "Call out" ),
		TEXT_IN ( "Text in" ),
		TEXT_OUT ( "Text out" ),
		POST ( "Post" ),
		RESEARCH ( "Research" ),
		SYSTEM ( "System" );

		private String label;

		private InteractionMethod( String label ) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}
}
