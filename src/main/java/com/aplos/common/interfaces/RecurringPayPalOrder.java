package com.aplos.common.interfaces;

import java.util.Date;

import com.aplos.common.beans.paypal.directintegration.PayPalRecurringPaymentDirectPost.BillingPeriod;

public interface RecurringPayPalOrder extends PayPalOrder {
	public boolean isRecurringPayment();
	public boolean hasInitialPaymentBeforeRecurring();
	public BillingPeriod getRecurringPaymentBillingPeriod();
	public Integer getRecurringPaymentBillingFrequency();
	public Integer getRecurringPaymentTotalBillingCycles();
	public Integer getRecurringPaymentMaxFailedPayments();
	public Date getRecurringPaymentStartDate();
	public Double getRecurringPaymentInitialPaymentAmount();
}
