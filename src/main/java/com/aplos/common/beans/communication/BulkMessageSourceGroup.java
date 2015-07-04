package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.JoinTable;
import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkGroupedMessageSource;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.utils.CommonUtil;

@Entity
@DynamicMetaValueKey(oldKey="BULK_SOURCE_GROUP")
public class BulkMessageSourceGroup extends AplosBean implements BulkGroupedMessageSource {
	private static final long serialVersionUID = -7527301091503107469L;

	private Class<? extends BulkMessageSource> sourceType = Subscriber.class;
	private String name;
    @ManyToAny( metaColumn = @Column( name = "bulk_source_type" ), fetch=FetchType.EAGER )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinTable( inverseJoinColumns = @JoinColumn( name = "bulkMessageSources_id" ) )
    @DynamicMetaValues
	private List<BulkMessageSource> bulkMessageSources = new ArrayList<BulkMessageSource>();

    private boolean isEmailRequired = true;
    private boolean isSmsRequired = false;
    private boolean isVisibleInSearches = false;

//    @Override
//    public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//    	super.hibernateInitialiseAfterCheck(fullInitialisation);
//    	if( fullInitialisation ) {
//    		HibernateUtil.initialiseList(bulkMessageSources, false);
//    	}
//    }

	@Override
	public String getDisplayName() {
		int validForSmsCount = 0;
		for (BulkMessageSource bms : bulkMessageSources) {
			if (bms instanceof BulkSmsSource && ((BulkSmsSource)bms).getInternationalNumber() != null && CommonUtil.validateTelephone(((BulkSmsSource)bms).getInternationalNumber().getSafeFullNumber())) {
				validForSmsCount++;
			}
		}
		return name + " (" + bulkMessageSources.size() + " addresses, " + validForSmsCount + " valid for SMS)";
	}
	
	@Override
	public Long getMessageSourceId() {
		return getId();
	}
	
	@Override
	public String getTrailDisplayName() {
		//stop it returning display name which causes lazy init exceptions
		return "Bulk Message Source Group";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<BulkMessageSource> getBulkMessageSources() {
		return bulkMessageSources;
	}

	public void setBulkMessageSources(List<BulkMessageSource> bulkEmailSources) {
		this.bulkMessageSources = bulkEmailSources;
	}
	
	public static void filterBulkEmailSources( AplosEmail aplosEmail, List<BulkMessageSource> bulkMessageSourceList, Set<BulkEmailSource> bulkEmailSourceSet, boolean removeUnsubscribed, boolean validateEmailAddress, Set<String> emailAddresses ) {
		for( BulkMessageSource tempBulkMessageSource : bulkMessageSourceList ) {
			if (tempBulkMessageSource instanceof BulkMessageSourceGroup) {
				if( tempBulkMessageSource.getMessageSourceId() != null ) {
					filterBulkEmailSources( aplosEmail, ((BulkMessageSourceGroup)tempBulkMessageSource).getBulkMessageSources(), bulkEmailSourceSet, removeUnsubscribed, validateEmailAddress, emailAddresses );
				} else {
					filterBulkEmailSources( aplosEmail, ((BulkMessageSourceGroup) tempBulkMessageSource).getBulkMessageSources(), bulkEmailSourceSet, removeUnsubscribed, validateEmailAddress, emailAddresses );
				}
			} else if( tempBulkMessageSource instanceof BulkEmailSource ) {
				if( removeUnsubscribed ) {
					if( tempBulkMessageSource instanceof BulkSubscriberSource ) {
				    	SubscriberInter subscriber = ((BulkSubscriberSource) tempBulkMessageSource).getSourceSubscriber();
					    if ( subscriber.getEmailAddress() != null && subscriber.determineIsSubscribed( aplosEmail )) {
							 addBulkEmailSourceToSet( bulkEmailSourceSet, (BulkEmailSource)tempBulkMessageSource, validateEmailAddress, emailAddresses);
					    }
			    	}
				 } else {
					 addBulkEmailSourceToSet( bulkEmailSourceSet, (BulkEmailSource)tempBulkMessageSource, validateEmailAddress, emailAddresses);
				 }
			}
		}
	}
	
	public static void addBulkEmailSourceToSet( Set<BulkEmailSource> bulkEmailSourceSet, BulkEmailSource bulkEmailSource, boolean validateEmailAddress, Set<String> emailAddresses ) {
		if( (!validateEmailAddress || CommonUtil.validateEmailAddressFormat(bulkEmailSource.getEmailAddress()))
				&& !emailAddresses.contains( bulkEmailSource.getEmailAddress() ) ) {
			bulkEmailSourceSet.add( bulkEmailSource );
			emailAddresses.add( bulkEmailSource.getEmailAddress() );
		}
	}
	
	public static void filterBulkSmsSources( SmsMessage smsMessage, List<BulkMessageSource> bulkMessageSourceList, Set<BulkSmsSource> bulkSmsSourceSet, boolean removeUnsubscribed ) {
		for( BulkMessageSource tempBulkMessageSource : bulkMessageSourceList ) {
			if (tempBulkMessageSource instanceof BulkMessageSourceGroup) {
				if( tempBulkMessageSource.getMessageSourceId() != null ) {
					BulkMessageSourceGroup loadedGroup = new BeanDao(BulkMessageSourceGroup.class).get( ((BulkMessageSourceGroup) tempBulkMessageSource).getId() );
//					loadedGroup.hibernateInitialise(true);
					filterBulkSmsSources( smsMessage, loadedGroup.getBulkMessageSources(), bulkSmsSourceSet, removeUnsubscribed );
				} else {
					filterBulkSmsSources( smsMessage, ((BulkMessageSourceGroup) tempBulkMessageSource).getBulkMessageSources(), bulkSmsSourceSet, removeUnsubscribed );
				}
			} else if( tempBulkMessageSource instanceof BulkSmsSource ) {
				BulkSmsSource bulkSmsSource = (BulkSmsSource) tempBulkMessageSource; 
				if( bulkSmsSource.getInternationalNumber() != null ) {
					if( !removeUnsubscribed || bulkSmsSource.determineIsSmsSubscribed( smsMessage ) ) {
						bulkSmsSourceSet.add( bulkSmsSource );
					 }
				}
			}
		}
	}
	
	public Set<BulkEmailSource> getBulkEmailSources( AplosEmail aplosEmail, boolean removeUnsubscribed, boolean validateEmailAddress ) {
		Set<BulkEmailSource> bulkEmailSourceSet = new HashSet<BulkEmailSource>();
		filterBulkEmailSources(aplosEmail, getBulkMessageSources(), bulkEmailSourceSet, removeUnsubscribed, validateEmailAddress, new HashSet<String>());
		return bulkEmailSourceSet;
	}

	public boolean containsRequiredClass(Class<? extends BulkMessageSource> bulkSourceClass) {
		for (BulkMessageSource source : bulkMessageSources) {
			if (bulkSourceClass.isAssignableFrom(source.getClass())) {
				return true;
			}
		}
		return false;
	}

	public Class<? extends BulkMessageSource> getSourceType() {
		return sourceType;
	}

	public void setSourceType(Class<? extends BulkMessageSource> sourceType) {
		this.sourceType = sourceType;
	}

	@Override
	public String getSourceUniqueDisplayName() {
		return name;
	}

	public boolean isEmailRequired() {
		return isEmailRequired;
	}

	public void setEmailRequired(boolean isEmailRequired) {
		this.isEmailRequired = isEmailRequired;
	}

	public boolean isSmsRequired() {
		return isSmsRequired;
	}

	public void setSmsRequired(boolean isSmsRequired) {
		this.isSmsRequired = isSmsRequired;
	}

	public boolean isVisibleInSearches() {
		return isVisibleInSearches;
	}

	public void setVisibleInSearches(boolean isVisibleInSearches) {
		this.isVisibleInSearches = isVisibleInSearches;
	}

}
