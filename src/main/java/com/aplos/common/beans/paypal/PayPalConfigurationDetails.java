package com.aplos.common.beans.paypal;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.utils.CommonUtil;

@Entity
@PluralDisplayName(name="PayPal configuration details")
public class PayPalConfigurationDetails extends AplosBean implements OnlinePaymentConfigurationDetails {

	private static final long serialVersionUID = -6726337961041763673L;

	private String name;

	private String accountEmailAddress;
	private String apiAccount; //testtest
	private String apiUsername;
	private String apiPassword;
	private String apiSignature;
	private String frontEndSuccessfulRedirectionUrl;
	private String frontEndFailedRedirectionUrl;
	private String backEndSuccessfulRedirectionUrl;
	private String backEndFailedRedirectionUrl;
	private PayPalServerType payPalServerType;
	@Column(columnDefinition="LONGTEXT")
	private String notes;

	// If the initial payment amount fails, PayPal should add the failed payment amount to the outstanding
	// balance due on this recurring payment profile
	//private static final String FAILEDINITAMTACTION = "ContinueOnFailure";

	// If payment fails PayPal will create the recurring payment profile, but will place it into
	// a pending status until the initial payment is completed. If the initial payment clears,
	// PayPal will notify you by IPN that the pending profile has been activated. If the payment fails,
	// PayPal will notify you by IPN that the pending profile has been cancelled
	private static final String FAILEDINITAMTACTION = "CancelOnFailure";
	private static final Double APICALLVERSION = 54.0d; //PayPal maintain various version 54 is the minimum version to use for some features

	private static IntegrationType integrationType;

	public enum PayPalServerType implements LabeledEnumInter {
		LIVE ( "https://api.paypal.com/nvp", "https://api-3t.paypal.com/nvp", "live", "https://www.paypal.com/cgi-bin/webscr", "https://www.paypal.com/webscr"),
		TEST ( "https://api.sandbox.paypal.com/nvp", "https://api-3t.sandbox.paypal.com/nvp", "sandbox", "https://www.sandbox.paypal.com/cgi-bin/webscr", "https://www.sandbox.paypal.com/webscr");

		private String nvpEndPoint;
		private String nvp3tEndPoint;
		private String environment;
		private String directPostUrl;
		private String expressCheckoutUrl;

		private PayPalServerType( String nvpEndPoint, String nvp3tEndPoint, String environment, String directPostUrl, String expressCheckoutUrl ) {
			this.nvpEndPoint = nvpEndPoint;
			this.nvp3tEndPoint = nvp3tEndPoint;
			this.environment = environment;
			this.directPostUrl = directPostUrl;
			this.expressCheckoutUrl = expressCheckoutUrl;
		}

		public String getNvpEndPoint() {
			return nvpEndPoint;
		}

		public String getNvp3tEndPoint() {
			return nvp3tEndPoint;
		}

		public String getEnvironment() {
			return environment;
		}

		public String getDirectPostUrl() {
			return directPostUrl;
		}

		public String getExpressCheckoutUrl() {
			return expressCheckoutUrl;
		}

		@Override
		public String getLabel() {
			return CommonUtil.firstLetterToUpperCase( name().toLowerCase() );
		}
	}

	public enum IntegrationType {
		DIRECT,
		SERVER;
	}

	public static void setIntegrationType(IntegrationType integrationType) {
		PayPalConfigurationDetails.integrationType = integrationType;
	}

	public static IntegrationType getIntegrationType() {
		return integrationType;
	}

	public static String getFailedInitialAmountAction() {
		return FAILEDINITAMTACTION;
	}

	public static Double getApiCallVersion() {
		return APICALLVERSION;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApiAccount() {
		return apiAccount;
	}

	public String getApiUsername() {
		return apiUsername;
	}

	public String getApiSignature() {
		return apiSignature;
	}

	public void setApiAccount(String apiAccount) {
		this.apiAccount = apiAccount;
	}

	public void setApiUsername(String apiUsername) {
		this.apiUsername = apiUsername;
	}

	public void setApiSignature(String apiSignature) {
		this.apiSignature = apiSignature;
	}

	public String getApiPassword() {
		return apiPassword;
	}

	public void setApiPassword(String apiPassword) {
		this.apiPassword = apiPassword;
	}

	public PayPalServerType getPayPalServerType() {
		return payPalServerType;
	}

	public void setPayPalServerType(PayPalServerType payPalServerType) {
		this.payPalServerType = payPalServerType;
	}

	public String getFrontEndSuccessfulRedirectionUrl() {
		return frontEndSuccessfulRedirectionUrl;
	}

	public void setFrontEndSuccessfulRedirectionUrl(
			String frontEndSuccessfulRedirectionUrl) {
		this.frontEndSuccessfulRedirectionUrl = frontEndSuccessfulRedirectionUrl;
	}

	public String getFrontEndFailedRedirectionUrl() {
		return frontEndFailedRedirectionUrl;
	}

	public void setFrontEndFailedRedirectionUrl(
			String frontEndFailedRedirectionUrl) {
		this.frontEndFailedRedirectionUrl = frontEndFailedRedirectionUrl;
	}

	public String getBackEndSuccessfulRedirectionUrl() {
		return backEndSuccessfulRedirectionUrl;
	}

	public void setBackEndSuccessfulRedirectionUrl(
			String backEndSuccessfulRedirectionUrl) {
		this.backEndSuccessfulRedirectionUrl = backEndSuccessfulRedirectionUrl;
	}

	public String getBackEndFailedRedirectionUrl() {
		return backEndFailedRedirectionUrl;
	}

	public void setBackEndFailedRedirectionUrl(
			String backEndFailedRedirectionUrl) {
		this.backEndFailedRedirectionUrl = backEndFailedRedirectionUrl;
	}

	public String getAccountEmailAddress() {
		return accountEmailAddress;
	}

	public void setAccountEmailAddress(String accountEmailAddress) {
		this.accountEmailAddress = accountEmailAddress;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
