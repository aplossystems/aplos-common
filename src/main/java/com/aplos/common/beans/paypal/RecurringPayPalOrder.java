package com.aplos.common.beans.paypal;

import java.math.BigDecimal;
import java.util.Date;

import com.aplos.common.beans.paypal.directintegration.PayPalRecurringPaymentDirectPost.BillingPeriod;
import com.aplos.common.interfaces.PayPalOrder;

public interface RecurringPayPalOrder extends PayPalOrder {
	public boolean isRecurringPayment();
	public boolean hasInitialPaymentBeforeRecurring();
	public BillingPeriod getRecurringPaymentBillingPeriod();
	public Integer getRecurringPaymentBillingFrequency();
	public Integer getRecurringPaymentTotalBillingCycles();
	public Integer getRecurringPaymentMaxFailedPayments();
	public Date getRecurringPaymentStartDate();
	public BigDecimal getRecurringPaymentInitialPaymentAmount();
}
