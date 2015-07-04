package com.aplos.common.beans.cardsave;

import java.util.ArrayList;
import java.util.List;

import net.thepaymentgateway.paymentsystem.MerchantDetails;
import net.thepaymentgateway.paymentsystem.RequestGatewayEntryPointList;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.utils.CommonUtil;

@Entity
@PluralDisplayName(name="card save configurations")
public class CardSaveConfigurationDetails extends AplosBean implements OnlinePaymentConfigurationDetails {

	private static final long serialVersionUID = -4986711121202241194L;
	
	//configuration name
	private String name;
	
	private ConnectToType connectToType;     //** Set to SIMULATOR for the Simulator expert system, TEST for the Test Server and LIVE in the live environment **
	private IntegrationType integrationType;
	
	private String merchantId;
	private String gatewayPassword;
	
	private String recurringMerchantId;
	private String recurringGatewayPassword;
	
	// Will need to put a valid path here for where the payment pages reside 
	// e.g. https://www.yoursitename.com/Pages/ 
	// NOTE: This path MUST include the trailing "/" 
	private String secureSiteBaseUrl;
	
	// This is the domain and port number (minus any host header for your payment processor
	// e.g. for "https://gwX.cardsaveonlinepayments.com:4430/", this should just be "cardsaveonlinepayments.com:4430"
	//NOTE: the gateway (subdomain) changes, but this doesnt seem to
	private String paymentProcessorFullDomain = "cardsaveonlinepayments.com:4430";
	
	//these aren't part of the CS API but will probably be necessary for our system
	private String frontEndSuccessfulRedirectionUrl;
	private String frontEndFailedRedirectionUrl;
	private String backEndSuccessfulRedirectionUrl;
	private String backEndFailedRedirectionUrl;
	
	@Column(columnDefinition="LONGTEXT")
	private String notes;
	
	@OneToMany
	@Cascade(value=CascadeType.ALL)
	private List<CardSaveGateway> gateways = new ArrayList<CardSaveGateway>();
	
	public CardSaveConfigurationDetails() {
		// TODO Auto-generated constructor stub
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialiseList(getGateways(), fullInitialisation);
//	}
	
	public enum ConnectToType implements LabeledEnumInter {
		LIVE,
		TEST,
		SIMULATOR;
		
		@Override
		public String getLabel() {
			return CommonUtil.firstLetterToUpperCase( name().toLowerCase() );
		};
	}
	
	public enum IntegrationType implements LabeledEnumInter {
		DIRECT,
		SERVER;
		
		@Override
		public String getLabel() {
			return CommonUtil.firstLetterToUpperCase( name().toLowerCase() );
		};
	}
	
	public enum CardSaveUrlType {
		ABORT,
		AUTHORISE,
		CANCEL,
		PURCHASE,
		REFUND,
		RELEASE,
		REPEAT,
		VOID,
		THREE_DCALLBACK,
		SHOWPOST;
	}

    public String getSystemUrl( IntegrationType integrationType, CardSaveUrlType cardSaveUrlType ) {
    	//dont add the gateways (subdomain), as we need to be able to try them all this happens in the send method instead
    	//cardsave works differently to sag pay - sagepay uses an identical message to different urls, cardsave uses different messages to the same url
	    return "cardsaveonlinepayments.com:4430";
	    
	}
    
    /** alias, to be compatible with sagepay config */
	public String getYourSiteFQDN() {
		return secureSiteBaseUrl;
	}

	public ConnectToType getConnectToType() {
		return connectToType;
	}

	public String getFrontEndSuccessfulRedirectionUrl() {
		return frontEndSuccessfulRedirectionUrl;
	}

	public String getFrontEndFailedRedirectionUrl() {
		return frontEndFailedRedirectionUrl;
	}

	public String getBackEndSuccessfulRedirectionUrl() {
		return backEndSuccessfulRedirectionUrl;
	}

	public String getBackEndFailedRedirectionUrl() {
		return backEndFailedRedirectionUrl;
	}

	public String getSuccessfulRedirectionUrl( CardSavePost cardSavePost ) {
		if( cardSavePost.isBackEndPayment() ) {
			return getBackEndSuccessfulRedirectionUrl();
		} else {
			return getFrontEndSuccessfulRedirectionUrl();
		}
	}

	public String getFailedRedirectionUrl( CardSavePost cardSavePost ) {
		if( cardSavePost.isBackEndPayment() ) {
			return getBackEndFailedRedirectionUrl();
		} else {
			return getFrontEndFailedRedirectionUrl();
		}
	}

	public void setConnectToType(ConnectToType connectToType) {
		this.connectToType = connectToType;
	}

	/** alias, to be compatible with sagepay config */
	public void setYourSiteFQDN(String yourSiteFQDN) {
		this.secureSiteBaseUrl = yourSiteFQDN;
	}

	public void setFrontEndSuccessfulRedirectionUrl(
			String frontEndSuccessfulRedirectionUrl) {
		this.frontEndSuccessfulRedirectionUrl = frontEndSuccessfulRedirectionUrl;
	}

	public void setFrontEndFailedRedirectionUrl(
			String frontEndFailedRedirectionUrl) {
		this.frontEndFailedRedirectionUrl = frontEndFailedRedirectionUrl;
	}

	public void setBackEndSuccessfulRedirectionUrl(
			String backEndSuccessfulRedirectionUrl) {
		this.backEndSuccessfulRedirectionUrl = backEndSuccessfulRedirectionUrl;
	}

	public void setBackEndFailedRedirectionUrl(
			String backEndFailedRedirectionUrl) {
		this.backEndFailedRedirectionUrl = backEndFailedRedirectionUrl;
	}

	public String getSecureSiteBaseUrl() {
		return secureSiteBaseUrl;
	}

	public void setSecureSiteBaseUrl(String secureSiteBaseUrl) {
		this.secureSiteBaseUrl = secureSiteBaseUrl;
	}

	public String getPaymentProcessorFullDomain() {
		return paymentProcessorFullDomain;
	}

	public void setPaymentProcessorFullDomain(String paymentProcessorFullDomain) {
		this.paymentProcessorFullDomain = paymentProcessorFullDomain;
	}

	public IntegrationType getIntegrationType() {
		return integrationType;
	}

	public void setIntegrationType(IntegrationType integrationType) {
		this.integrationType = integrationType;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getGatewayPassword() {
		return gatewayPassword;
	}

	public void setGatewayPassword(String gatewayPassword) {
		this.gatewayPassword = gatewayPassword;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CardSaveGateway> getGateways() {
		return gateways;
	}

	public void setGateways(List<CardSaveGateway> gateways) {
		this.gateways = gateways;
	}

	//not sure if these methods below should actually go somewhere else
	
	public RequestGatewayEntryPointList getRequestGatewayEntryPointList() {
        RequestGatewayEntryPointList rgeplRequestGatewayEntryPointList = new RequestGatewayEntryPointList();
        // The lower the metric (2nd parameter) means that entry point will be attempted first,
        // EXCEPT if it is -1 - in this case that entry point will be skipped
        // The 3rd parameter is a retry attempt, so it is possible to try that entry point that number of times before failing over onto the next entry point in the list
        for (CardSaveGateway gateway : getGateways()) {
        	rgeplRequestGatewayEntryPointList.add("https://" + gateway.getSubdomain() + "." + getPaymentProcessorFullDomain(), gateway.getPriority(), 2);
        }
        return rgeplRequestGatewayEntryPointList;
	}
	
	public MerchantDetails getMerchantDetails() {
		MerchantDetails mdMerchantDetails = new MerchantDetails(getMerchantId(), getGatewayPassword());
		return mdMerchantDetails;
	}
	
	public MerchantDetails getRecurringMerchantDetails() {
		MerchantDetails recurringMerchantDetails = new MerchantDetails(getRecurringMerchantId(), getRecurringGatewayPassword());
		return recurringMerchantDetails;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getRecurringMerchantId() {
		return recurringMerchantId;
	}

	public void setRecurringMerchantId(String recurringMerchantId) {
		this.recurringMerchantId = recurringMerchantId;
	}

	public String getRecurringGatewayPassword() {
		return recurringGatewayPassword;
	}

	public void setRecurringGatewayPassword(String recurringGatewayPassword) {
		this.recurringGatewayPassword = recurringGatewayPassword;
	}
	
}
