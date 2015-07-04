package com.aplos.common.beans.marketing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.Address;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction.InteractionMethod;
import com.aplos.common.enums.EmailActionType;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.utils.FormatUtil;

@Entity
@BeanScope(scope=JsfScope.TAB_SESSION)
public class PotentialCompany extends AplosBean implements EmailFolder, BulkEmailSource, BulkSubscriberSource {	
	private static final long serialVersionUID = 1966960582784742706L;

	private String webAddress;
	@ManyToOne
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address address;
	private String contactPosition;
	@ManyToMany(fetch=FetchType.LAZY)
	private List<PotentialCompanyCategory> categories = new ArrayList<PotentialCompanyCategory>();
	@ManyToOne(fetch=FetchType.LAZY)
	private PotentialCompanyCategory mainCategory;
	private PotentialCompanyStatus potentialCompanyStatus = PotentialCompanyStatus.UNCONTACTED;
	private Date lastContactedDate;
	private Date reminderDate;
	private boolean isCallingAllowed = true;
	@Column(columnDefinition="LONGTEXT")
	private String notes;
	
	@Override
	public String getDisplayName() {
		return getAddress().getCompanyName();
	}
	
	@Override
	public void aplosEmailAction(EmailActionType emailActionType, AplosEmail aplosEmail ) {
		if( EmailActionType.SENT.equals( emailActionType ) ) {
			PotentialCompanyInteraction potentialCompanyInteraction = new PotentialCompanyInteraction();
			potentialCompanyInteraction.setSingleEmailRecord( aplosEmail.getSingleEmailRecord( this, true ) );
			potentialCompanyInteraction.setPotentialCompany( this );
			potentialCompanyInteraction.setMethod( InteractionMethod.EMAIL_OUT );
			potentialCompanyInteraction.setPotentialCompanyStatus( PotentialCompanyStatus.CALL_BACK );
			potentialCompanyInteraction.setContactDateTime( new Date() );
			potentialCompanyInteraction.setNotes( "Auto generated" );
			potentialCompanyInteraction.saveDetails();
		}
	}
	
	@Override
	public String getEmailFolderSearchCriteria() {
		return "CONCAT(bean.address.contactFirstName, ' ', bean.address.contactSurname) LIKE :searchStr OR bean.id LIKE :searchStr";
	}
	
	@Override
	public String getEmailAddress() {
		return getAddress().getEmailAddress();
	}
	
	@Override
	public String getFirstName() {
		return getAddress().getContactFirstName();
	}
	
	@Override
	public String getSurname() {
		return getAddress().getContactSurname();
	}
	
	@Override
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return null;
	}
	
	@Override
	public Long getMessageSourceId() {
		return getId();
	}
	
	@Override
	public String getSourceUniqueDisplayName() {
		return getAddress().getCompanyName() + "(" + getId() + ")";
	}
	
	@Override
	public SubscriberInter getSourceSubscriber() {
		return getAddress().getSubscriber();
	}
	
	public boolean isReminderDatePast() {
		return new Date().after( getReminderDate() );
	}
	
	public String getReminderDateTimeStr() {
		return FormatUtil.formatDateTime( getReminderDate(), true );
	}

	public String getWebAddress() {
		return webAddress;
	}
	public void setWebAddress(String webAddress) {
		this.webAddress = webAddress;
	}
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	public List<PotentialCompanyCategory> getCategories() {
		return categories;
	}

	public void setCategories(List<PotentialCompanyCategory> categories) {
		this.categories = categories;
	}

	public PotentialCompanyCategory getMainCategory() {
		return mainCategory;
	}

	public void setMainCategory(PotentialCompanyCategory mainCategory) {
		this.mainCategory = mainCategory;
	}

	public PotentialCompanyStatus getPotentialCompanyStatus() {
		return potentialCompanyStatus;
	}

	public void setPotentialCompanyStatus(PotentialCompanyStatus potentialCompanyStatus) {
		this.potentialCompanyStatus = potentialCompanyStatus;
	}

	public Date getLastContactedDate() {
		return lastContactedDate;
	}

	public void setLastContactedDate(Date lastContactedDate) {
		this.lastContactedDate = lastContactedDate;
	}

	public Date getReminderDate() {
		return reminderDate;
	}

	public void setReminderDate(Date reminderDate) {
		this.reminderDate = reminderDate;
	}
	public String getContactPosition() {
		return contactPosition;
	}
	public void setContactPosition(String contactPosition) {
		this.contactPosition = contactPosition;
	}

	public boolean isCallingAllowed() {
		return isCallingAllowed;
	}

	public void setCallingAllowed(boolean isCallingAllowed) {
		this.isCallingAllowed = isCallingAllowed;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
