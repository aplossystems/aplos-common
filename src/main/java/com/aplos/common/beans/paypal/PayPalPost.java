package com.aplos.common.beans.paypal;

import java.math.BigDecimal;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.interfaces.PayPalOrder;
import com.aplos.common.module.CommonConfiguration;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;

@Entity
public class PayPalPost extends PaymentGatewayPost {

	private static final long serialVersionUID = -1273753898664563294L;

    @Column(columnDefinition="LONGTEXT")
    private String requestPost;
    @Column(columnDefinition="LONGTEXT")
    private String responseStr = "";
    @Column(columnDefinition="LONGTEXT")
    private String statusDetail;

    public PayPalPost() { }

    public PayPalPost( PayPalConfigurationDetails payPalDetails, PayPalOrder payPalOrder) {
    	this.setOnlinePaymentConfigurationDetails(payPalDetails);
    	this.setOnlinePaymentOrder(payPalOrder);
    	retrievePayPalServerItemInformation();
    }

    public void retrievePayPalServerItemInformation() {
		setBillingAddress( getOnlinePaymentOrder().getBillingAddress().getCopy() );
    }
    
    @Override
    public void determineOnlinePaymentConfigurationDetails() {
    	setOnlinePaymentConfigurationDetails( CommonConfiguration.getCommonConfiguration().determinePayPalCfgDetails() );
    }
    
    @Override
    public PaymentGatewayPost recurPayment(
    		BigDecimal partPayment) {
    	return null;
    }

	public APIProfile createApiProfile() throws PayPalException {
		APIProfile profile = ProfileFactory.createSignatureAPIProfile();
		PayPalConfigurationDetails palConfigurationDetails = getPayPalConfigurationDetails();
		profile.setAPIUsername(palConfigurationDetails.getApiUsername());
		profile.setAPIPassword(palConfigurationDetails.getApiPassword());
		profile.setSignature(palConfigurationDetails.getApiSignature());
		profile.setEnvironment(palConfigurationDetails.getPayPalServerType().getEnvironment()); //TODO: check this is right, production maybe?
    	return profile;
	}
	
	public PayPalConfigurationDetails getPayPalConfigurationDetails() {
		return (PayPalConfigurationDetails) getOnlinePaymentConfigurationDetails();
	}

	public void setStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
	}

	public String getStatusDetail() {
		return statusDetail;
	}

	public void setRequestPost(String requestPost) {
		this.requestPost = requestPost;
	}

	public String getRequestPost() {
		return requestPost;
	}

	public void setResponseStr(String responseStr) {
		this.responseStr = responseStr;
	}

	public String getResponseStr() {
		return responseStr;
	}
}
