package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.cardsave.directintegration.CardSaveDirectPost;
import com.aplos.common.beans.paypal.directintegration.PayPalDirectPost;
import com.aplos.common.beans.sagepay.directintegration.SagePayDirectPost;

public enum PaymentGateway implements LabeledEnumInter {
	SAGEPAY ( "SagePay", "/common/payments/sagepay/sagePayThreeDRedirect.jsf" ),
	PAYPAL ( "PayPal", "" ),
	CARDSAVE ( "CardSave", "/common/payments/cardsave/cardSaveThreeDRedirect.jsf" );

	private String label;
	private String threeDRedirectUrl;

	private PaymentGateway( String label, String threeDRedirectUrl ) {
		this.label = label;
		this.setThreeDRedirectUrl(threeDRedirectUrl);
	}
	
	public PaymentGatewayPost getDirectPost() {
		if( this.equals( PaymentGateway.SAGEPAY ) ) {
			return new SagePayDirectPost();
		} else if( this.equals( PaymentGateway.CARDSAVE ) ) {
			return new CardSaveDirectPost();
		} else if( this.equals( PaymentGateway.PAYPAL ) ) {
			return new PayPalDirectPost();
		}
		return null;
	}

	@Override
	public String getLabel() {
		return label;
	}

	public String getThreeDRedirectUrl() {
		return threeDRedirectUrl;
	}

	public void setThreeDRedirectUrl(String threeDRedirectUrl) {
		this.threeDRedirectUrl = threeDRedirectUrl;
	}
}