package com.aplos.common.interfaces;

import java.util.List;

import com.aplos.common.beans.CreditCardDetails;

public interface PayPalOrder extends OnlinePaymentOrder {
	public CreditCardDetails determineCreditCardDetails();
	public List<? extends PaymentSystemCartItem> getCartItems();
}
