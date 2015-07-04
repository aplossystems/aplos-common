package com.aplos.common.beans.communication;

import java.util.Date;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.enums.SmsBundleState;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.utils.FormatUtil;

@Entity
public class SmsBundlePurchase extends AplosBean implements EmailGenerator {
	private static final long serialVersionUID = 6735455546737431297L;
	
	private int creditsPurchased;
	private int creditsRemaining;
	private Date validFromDate;
	private Date validUntilDate;
	private SmsBundleState smsBundleState = SmsBundleState.LIVE;
	private boolean paidDate;
	@ManyToOne
	private SmsAccount smsAccount;
	
	public SmsBundlePurchase() {
	}
	
	public SmsBundlePurchase( SmsAccount smsAccount ) {
		setSmsAccount( smsAccount );
	}
	
	public SmsBundlePurchase( SmsAccount smsAccount, int creditsPurchased ) {
		setSmsAccount( smsAccount );
		setCreditsPurchased(creditsPurchased);
		setCreditsRemaining(creditsPurchased);
		setValidFromDate(new Date());
	}
	
	public String getValidFromStdStr() {
		return FormatUtil.formatDate( getValidFromDate() );
	}
	
	public String getValidUntilStdStr() {
		return FormatUtil.formatDate( getValidUntilDate() );
	}
	
	public int getCreditsPurchased() {
		return creditsPurchased;
	}
	public void setCreditsPurchased(int creditsPurchased) {
		this.creditsPurchased = creditsPurchased;
	}
	public boolean isPaidDate() {
		return paidDate;
	}
	public void setPaidDate(boolean paidDate) {
		this.paidDate = paidDate;
	}
	public SmsAccount getSmsAccount() {
		return smsAccount;
	}
	public void setSmsAccount(SmsAccount smsAccount) {
		this.smsAccount = smsAccount;
	}
	public int getCreditsRemaining() {
		return creditsRemaining;
	}
	public void setCreditsRemaining(int creditsRemaining) {
		this.creditsRemaining = creditsRemaining;
	}
	public Date getValidFromDate() {
		return validFromDate;
	}
	public void setValidFromDate(Date validFromDate) {
		this.validFromDate = validFromDate;
	}
	public SmsBundleState getSmsBundleState() {
		return smsBundleState;
	}
	public void setSmsBundleState(SmsBundleState smsBundleState) {
		this.smsBundleState = smsBundleState;
	}

	public Date getValidUntilDate() {
		return validUntilDate;
	}

	public void setValidUntilDate(Date validUntilDate) {
		this.validUntilDate = validUntilDate;
	}
}
