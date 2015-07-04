package com.aplos.common.interfaces;

import java.util.List;

import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.Currency;

public interface PaymentGatewayDirectPost {
	public void createRequestMessage( String userAgent,List<? extends PaymentSystemCartItem> cartItems, CreditCardDetails creditCardDetails, Currency currency );
	public void postRequest();
	public void setApply3dSecure(boolean isApply3dSecure);
}
