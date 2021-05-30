package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.MailShotType;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.enums.SubscriptionHook;
import com.aplos.common.enums.UnsubscribeType;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.utils.CommonUtil;


@Entity
@ManagedBean(name="subscriberSignup")
@SessionScoped
@DynamicMetaValueKey(oldKey="SUBSCRIBER")
public class Subscriber extends AplosBean implements BulkEmailFinder, BulkSubscriberSource, SubscriberInter {
	private static final long serialVersionUID = -3887960323607272728L;

	@Column(columnDefinition="LONGTEXT")
	private String firstName;
	@Column(columnDefinition="LONGTEXT")
	private String surname;
	private String emailAddress;
	@Column(columnDefinition="LONGTEXT")
	private String unsubscribeReason;
	private UnsubscribeType unsubscribeType;
	private boolean isSubscribed = true;
	private SubscriptionHook subscriptionHook;
	@ManyToMany
	private List<SubscriptionChannel> subscriptions = new ArrayList<SubscriptionChannel>();

	@ManyToOne
	private SubscriberReferrer subscriberReferrer;

	@ManyToOne
	private MailShotType mailShotType;

	private String referrerOther;

	public Subscriber() {}

	public Subscriber(String firstName, String emailAddress) {
		this.firstName = firstName;
		this.emailAddress = emailAddress;
	}
	
	@Override
	public boolean isEmptyBean() {
		if( !CommonUtil.isNullOrEmpty( getEmailAddress() ) ) {
			return false;
		}
		if( !CommonUtil.isNullOrEmpty( getFirstName() ) ) {
			return false;
		}
		if( !CommonUtil.isNullOrEmpty( getSurname() ) ) {
			return false;
		}
		if( !isSubscribed() ) {
			return false;
		}
		return true;
	}
	
	@Override
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return null;
	}

	@Override
	public String getDisplayName() {
		return getFullName();
	}
	
	@Override
	public Long getMessageSourceId() {
		return getId();
	}
	
	@Override
	public String getBulkMessageFinderName() {
		return "All subscribers";
	}

	public static Subscriber getSubscriberByEmailAddress( String emailAddress ) {
		BeanDao subscriberBeanDao = new BeanDao(Subscriber.class);
		subscriberBeanDao.addWhereCriteria("bean.emailAddress = '" + emailAddress + "'");
		return (Subscriber) subscriberBeanDao.getFirstBeanResult();
	}

	public static Subscriber getOrCreateSubscriber( String emailAddress ) {
		BeanDao subscriberBeanDao = new BeanDao(Subscriber.class);
		subscriberBeanDao.addWhereCriteria("bean.emailAddress = '" + emailAddress + "'");
		Subscriber subscriber = (Subscriber) subscriberBeanDao.getFirstBeanResult();

		if( subscriber == null ) {
			subscriber = new Subscriber();
			subscriber.setEmailAddress( emailAddress );
		}
		
		return subscriber;
	}

	public String getFullName() {
		return (getFirstName() == null && getSurname() == null ? super.getDisplayName() : getFirstName() + " " + CommonUtil.getStringOrEmpty(getSurname()) );
	}

	public void setSubscribed(boolean newIsSubscribed) {
		this.isSubscribed = newIsSubscribed;
	}

	public boolean isSubscribed() {
		return isSubscribed;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getSurname() {
		return surname;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setSubscriberReferrer(SubscriberReferrer subscriberReferrer) {
		this.subscriberReferrer = subscriberReferrer;
	}

	public SubscriberReferrer getSubscriberReferrer() {
		return subscriberReferrer;
	}

	public void setReferrerOther(String referrerOther) {
		this.referrerOther = referrerOther;
	}

	public String getReferrerOther() {
		return referrerOther;
	}

	public void setMailShotType(MailShotType mailShotType) {
		this.mailShotType = mailShotType;
	}

	public MailShotType getMailShotType() {
		return mailShotType;
	}

	public void setSubscriptionHook(SubscriptionHook subscriptionHook) {
		this.subscriptionHook = subscriptionHook;
	}

	public SubscriptionHook getSubscriptionHook() {
		return subscriptionHook;
	}

	public void setSubscriptions(List<SubscriptionChannel> subscriptions) {
		this.subscriptions = subscriptions;
	}

	public List<SubscriptionChannel> getSubscriptions() {
		if (subscriptions == null) {
			subscriptions = new ArrayList<SubscriptionChannel>();
		}
		return subscriptions;
	}

	public String addToChannel(SubscriptionChannel channel) {
		if (!getSubscriptions().contains(channel)) {
			getSubscriptions().add(channel);
		}
		return null;
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		setFirstName( CommonUtil.firstLetterToUpperCase(getFirstName()) );
		setSurname( CommonUtil.firstLetterToUpperCase(getSurname()) );
		super.saveBean(currentUser);
	}

	public String removeFromChannel(SubscriptionChannel channel) {
		getSubscriptions().remove(channel);
		return null;
	}
	
	public boolean determineIsSubscribed( AplosEmail aplosEmail ) {
		return isSubscribed();
	}
	
	public boolean determineIsSmsSubscribed( SmsMessage smsMessage ) {
		return isSubscribed();
	}

	@Override
	public Subscriber getSourceSubscriber() {
		return this;
	}

	@Override
	public String getSourceUniqueDisplayName() {
		if (getEmailAddress() != null && getEmailAddress().equals(getFirstName())) { //this can be the case when we create a 'bare' contact form the autocomplete etc
			return getEmailAddress();
		} else {
			return getFullName() + " (" + getEmailAddress() + ")";
		}
	}

	@Override
	public List<BulkEmailSource> getEmailAutoCompleteSuggestions(String searchString, Integer limit) {
		BeanDao subscriberDao = new BeanDao( Subscriber.class );
		subscriberDao.setIsReturningActiveBeans( true );
		List<BulkEmailSource> subscribers = null;
		if( searchString != null ) {
			subscriberDao.addWhereCriteria( "CONCAT(firstName,' ',surname) like :similarSearchText OR emailAddress like :similarSearchText" );
			
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

	@Override
	public String getFinderSearchCriteria() {
		return "(CONCAT(bean.firstName,' ',bean.surname) LIKE :similarSearchText OR bean.emailAddress LIKE :similarSearchText)";
	}

	@Override
	public String getAlphabeticalSortByCriteria() {
		return "bean.firstName ASC, bean.surname ASC";
	}

	public String getUnsubscribeReason() {
		return unsubscribeReason;
	}

	public void setUnsubscribeReason(String unsubscribeReason) {
		this.unsubscribeReason = unsubscribeReason;
	}

	public UnsubscribeType getUnsubscribeType() {
		return unsubscribeType;
	}

	public void setUnsubscribeType(UnsubscribeType unsubscribeType) {
		this.unsubscribeType = unsubscribeType;
	}

	public boolean validateXss() {
		if (!CommonUtil.validateXss(firstName)) {
			return false;
		}
		if (!CommonUtil.validateXss(surname)) {
			return false;
		}
		if (!CommonUtil.validateXss(emailAddress)) {
			return false;
		}
		if (!CommonUtil.validateXss(unsubscribeReason)) {
			return false;
		}
		if (!CommonUtil.validateXss(referrerOther)) {
			return false;
		}
		return true;
	}

	public boolean encodeAgainstXss() {
		CommonUtil.encodeAgainstXss(firstName);
		CommonUtil.encodeAgainstXss(surname);
		CommonUtil.encodeAgainstXss(emailAddress);
		CommonUtil.encodeAgainstXss(unsubscribeReason);
		CommonUtil.encodeAgainstXss(referrerOther);
		return true;
	}
}
