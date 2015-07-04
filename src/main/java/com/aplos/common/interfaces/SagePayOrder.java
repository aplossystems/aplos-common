package com.aplos.common.interfaces;

import com.aplos.common.beans.Address;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.PaymentGatewayPost;

public interface SagePayOrder extends OnlinePaymentOrder {
	public boolean isDeliveryAddressTheSame();
	public boolean isUsingPayPal();
	public Address getDeliveryAddress();
	public String getPurchaseDescription();
	public Currency getCurrency();
	public void sendPaymentFailureEmail(String error);
	public void threeDRedirect();
	public void executePaymentCompleteRoutine( boolean redirectRequested, PaymentGatewayPost paymentGatewayDirectPost );
}
