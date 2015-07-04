package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.enums.EmailActionType;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSmsFinder;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.SmsMessageOwner;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.utils.CommonUtil;

@Entity
public class BasicContact extends AplosBean implements BulkEmailSource, BulkSmsSource, BulkSmsFinder, BulkEmailFinder, EmailFolder, SmsMessageOwner, BulkSubscriberSource {
	private static final long serialVersionUID = -6934259986618967661L;
	@OneToOne(fetch=FetchType.LAZY)
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address address;
	@ManyToMany
	private Set<BasicContactTag> basicContactTags = new HashSet<BasicContactTag>();
	
	@Override
	public void aplosEmailAction(EmailActionType emailActionType,
			AplosEmail aplosEmail) {	
	}
	
	@Override
	public String getDisplayName() {
		return address.getContactFullName();
	}
	
	@Override
	public boolean determineIsSmsSubscribed(SmsMessage smsMessage) {
		return getAddress().isSmsSubscribed();
	}
	
	@Override
	public void setSmsSubscribed(boolean isSmsSubscribed) {
		getAddress().setSmsSubscribed(isSmsSubscribed);;
	}
	
	@Override
	public String getEmailFolderSearchCriteria() {
		return "bean.address.subscriber.firstName LIKE :searchStr OR bean.address.subscriber.surname LIKE :searchStr OR bean.id LIKE :searchStr";
	}
	
	@Override
	public void smsMessageSent(SmsMessage smsMessage) {	
	}
	
	public List<BasicContactTag> getBasicContactTagList() {
		return new ArrayList<BasicContactTag>( getBasicContactTags() );
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( !BasicContact.class.equals( getClass() ) ) {
			addToScope( CommonUtil.getBinding( BasicContact.class ), this, associatedBeanScope );
		}
	}
	
	@Override
	public SubscriberInter getSourceSubscriber() {
		return getAddress().getSubscriber();
	}
	
	public String getEmailAddress() {
		return getAddress().getEmailAddress();
	};
	
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return null;
	};
	
	@Override
	public Long getMessageSourceId() {
		return getId();
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
	public String getAlphabeticalSortByCriteria() {
		return "bean.address.subscriber.firstName ASC, bean.address.contactSurname ASC";
	}
	
	@Override
	public String getBulkMessageFinderName() {
		return "Basic contacts";
	}
	
	@Override
	public String getFinderSearchCriteria() {
		return "CONCAT(bean.address.subscriber.firstName,' ',bean.address.contactSurname)";
	}
	
	@Override
	public InternationalNumber getInternationalNumber() {
		return InternationalNumber.parseMobileNumberStr(getAddress().getMobile());
	}
	
	@Override
	public List<BulkSmsSource> getSmsAutoCompleteSuggestions(
			String searchString, Integer limit) {
		BeanDao aqlBeanDao = new BeanDao(  BasicContact.class );
		aqlBeanDao.setIsReturningActiveBeans( true );
		aqlBeanDao.setMaxResults(limit);
		List<BulkSmsSource> foundList = null;
		if( searchString != null ) {
			aqlBeanDao.addWhereCriteria( "(CONCAT(bean.address.subscriber.firstName,' ',bean.address.contactSurname) LIKE :similarSearchText OR bean.address.mobile LIKE :similarSearchText)" );
			aqlBeanDao.setNamedParameter("similarSearchText", "%" + searchString + "%");
			foundList = (List<BulkSmsSource>) aqlBeanDao.getAll();
		} else {
			foundList = aqlBeanDao.getAll();
		}
		return foundList;
	}

	@Override
	public String getSourceUniqueDisplayName() {
		if (getAddress().getEmailAddress() != null && 
				(getAddress().getEmailAddress().equals(getFirstName())
						|| CommonUtil.isNullOrEmpty( getAddress().getContactFullName() )) ) { //this can be the case when we create a 'bare' contact from the autocomplete etc
			return getAddress().getEmailAddress();
		} else {
			return getAddress().getContactFullName() + " (" + getAddress().getEmailAddress() + ")";
		}
	}

	@Override
	public List<BulkEmailSource> getEmailAutoCompleteSuggestions(String searchString, Integer limit) {
		BeanDao subscriberDao = new BeanDao( BasicContact.class );
		subscriberDao.setIsReturningActiveBeans( true );
		List<BulkEmailSource> subscribers = null;
		if( searchString != null ) {
			subscriberDao.addWhereCriteria( "CONCAT(bean.address.subscriber.firstName,' ',bean.address.contactSurname) like :similarSearchText OR bean.address.subscriber.emailAddress like :similarSearchText" );
			if( limit != null ) {
				subscriberDao.setMaxResults(limit);
			}
			subscriberDao.setNamedParameter("similarSearchText", "%" + searchString + "%");
			subscribers = (List<BulkEmailSource>) subscriberDao.getAll();
		} else {
			subscribers = subscriberDao.getAll();
		}
		return subscribers;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Set<BasicContactTag> getBasicContactTags() {
		return basicContactTags;
	}

	public void setBasicContactTags(Set<BasicContactTag> basicContactTags) {
		this.basicContactTags = basicContactTags;
	}
}
