package com.aplos.common.interfaces;

import com.aplos.common.beans.Currency;
import com.aplos.common.beans.PaymentGatewayPost;

public interface CardSaveOrder extends OnlinePaymentOrder {
	public String getPurchaseDescription();
	public Currency getCurrency();
	public void sendPaymentFailureEmail(String error);
	public void threeDRedirect();
	public void executePaymentCompleteRoutine( boolean redirectRequested, PaymentGatewayPost paymentGatewayDirectPost );
}
