package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.enums.SmsBundleState;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class SmsAccount extends AplosBean {
	private static final long serialVersionUID = -8116188671075017559L;

	private int cachedSmsCredits = 0; //might be good to have a default value for testing or letting users try it
	private String smsSourceNumber;
	private boolean autoRebuySmsBundle = false;
	private int autoRebuyBundleSize = 1000;
	private int autoRebuyThreshold = 50;
	@Any( metaColumn = @Column( name = "account_owner_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
	@JoinColumn( name = "account_owner_id" )
    @DynamicMetaValues
	private BulkEmailSource accountOwner;
	private boolean isDeterminingSourceNumberFromUser = true;
	@OneToMany
	private List<SmsBundlePurchase> activeBundlePurchases = new ArrayList<SmsBundlePurchase>();
	
	public SmsAccount() {
	}
	
	public SmsAccount( String smsSourceNumber ) {
		setSmsSourceNumber( smsSourceNumber );
		setDeterminingSourceNumberFromUser( false );
	}
	
	public void addActiveBundlePurchase( SmsBundlePurchase smsBundlePurchase ) {
		if( !getActiveBundlePurchases().contains( smsBundlePurchase ) ) {
			SmsAccount saveableThis = getSaveableBean();
			saveableThis.getActiveBundlePurchases().add( smsBundlePurchase );
			saveableThis.updateCachedSmsCredits();
			saveableThis.saveDetails();
		}
	}
	
	public void removeActiveBundlePurchase( SmsBundlePurchase smsBundlePurchase ) {
		if( getActiveBundlePurchases().contains( smsBundlePurchase ) ) {
			SmsAccount saveableThis = getSaveableBean();
			saveableThis.getActiveBundlePurchases().remove( smsBundlePurchase );
			saveableThis.updateCachedSmsCredits();
			saveableThis.saveDetails();
		}
	}
	
	public void removeSmsCredits( int creditsToRemove ) {
		SmsBundlePurchase firstExpiringBundle = getActiveBundlePurchases().get( 0 );
		if( getActiveBundlePurchases().size() > 1 ) {
			for( int i = 1, n = getActiveBundlePurchases().size(); i < n; i++ ) {
				if( getActiveBundlePurchases().get( i ).getValidUntilDate() != null ) {
					if( firstExpiringBundle.getValidUntilDate() == null ) {
						firstExpiringBundle = getActiveBundlePurchases().get( i );
					} else {
						if( firstExpiringBundle.getValidUntilDate().after( getActiveBundlePurchases().get( i ).getValidUntilDate() ) ) {
							firstExpiringBundle = getActiveBundlePurchases().get( i );
						}
					}
				}
			}
		}
		
		SmsBundlePurchase saveableBundle = firstExpiringBundle.getSaveableBean();
		SmsAccount saveableThis = getSaveableBean();
		if( creditsToRemove < firstExpiringBundle.getCreditsRemaining() ) {
			saveableBundle.setCreditsRemaining( firstExpiringBundle.getCreditsRemaining() - creditsToRemove );
			saveableBundle.saveDetails();
		} else {
			int additionalCredits = creditsToRemove - saveableBundle.getCreditsRemaining();
			saveableBundle.setCreditsRemaining( 0 );
			saveableBundle.setSmsBundleState(SmsBundleState.EXHAUSTED);
			saveableBundle.saveDetails();
			saveableThis.getActiveBundlePurchases().remove( firstExpiringBundle );
			saveableThis.saveDetails();
			if( additionalCredits > 0 ) {
				removeSmsCredits( additionalCredits );
			}
		}
		saveableThis.updateCachedSmsCredits();
		saveableThis.saveDetails();
	}
	
	public void updateCachedSmsCredits() {
		int smsCreditTotal = 0;
		for( int i = 0, n = getActiveBundlePurchases().size(); i < n; i++ ) {
			smsCreditTotal += getActiveBundlePurchases().get( i ).getCreditsRemaining();
		}
		setCachedSmsCredits(smsCreditTotal);
	}
	
	public String determineSourceNumber() {
		String sourcePhone = null;
		if( isDeterminingSourceNumberFromUser() && JSFUtil.getLoggedInUser() != null ) {
			sourcePhone = JSFUtil.getLoggedInUser().getMobile();
		} else {
			sourcePhone	= getSmsSourceNumber();
		}
		
		if( CommonUtil.isNullOrEmpty( sourcePhone ) ) {
			sourcePhone = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getAddress().getMobile();
		}
		return sourcePhone;
	}


	public String getSmsSourceNumber() {
		return smsSourceNumber;
	}
	public void setSmsSourceNumber(String smsSourceNumber) {
		this.smsSourceNumber = smsSourceNumber;
	}
	public boolean isAutoRebuySmsBundle() {
		return autoRebuySmsBundle;
	}
	public void setAutoRebuySmsBundle(boolean autoRebuySmsBundle) {
		this.autoRebuySmsBundle = autoRebuySmsBundle;
	}
	public int getAutoRebuyBundleSize() {
		return autoRebuyBundleSize;
	}
	public void setAutoRebuyBundleSize(int autoRebuyBundleSize) {
		this.autoRebuyBundleSize = autoRebuyBundleSize;
	}
	public int getAutoRebuyThreshold() {
		return autoRebuyThreshold;
	}
	public void setAutoRebuyThreshold(int autoRebuyThreshold) {
		this.autoRebuyThreshold = autoRebuyThreshold;
	}

	public BulkEmailSource getAccountOwner() {
		return accountOwner;
	}

	public void setAccountOwner(BulkEmailSource accountOwner) {
		this.accountOwner = accountOwner;
	}
	public boolean isDeterminingSourceNumberFromUser() {
		return isDeterminingSourceNumberFromUser;
	}
	public void setDeterminingSourceNumberFromUser(
			boolean isDeterminingSourceNumberFromUser) {
		this.isDeterminingSourceNumberFromUser = isDeterminingSourceNumberFromUser;
	}

	public List<SmsBundlePurchase> getActiveBundlePurchases() {
		return activeBundlePurchases;
	}

	public void setActiveBundlePurchases(List<SmsBundlePurchase> activeBundlePurchases) {
		this.activeBundlePurchases = activeBundlePurchases;
	}

	public int getCachedSmsCredits() {
		return cachedSmsCredits;
	}

	public void setCachedSmsCredits(int cachedSmsCredits) {
		this.cachedSmsCredits = cachedSmsCredits;
	}
}
