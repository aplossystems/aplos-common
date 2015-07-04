package com.aplos.common.interfaces;

import java.math.BigDecimal;

import com.aplos.common.beans.Address;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.PaymentGatewayPost;

public interface OnlinePaymentOrder {
	public Long getId();
	public void paymentComplete( boolean isRedirectRequested, PaymentGatewayPost paymentGatewayDirectPost );
	public void sendConfirmationEmail();
	public void reevaluateOrderObjectsSession();
	public Address getBillingAddress();
	public <T extends AplosAbstractBean> T getSaveableBean();
	public BigDecimal getGrandTotal( boolean includeVat );
}
