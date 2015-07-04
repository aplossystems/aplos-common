package com.aplos.common.beans.sagepay;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@PluralDisplayName(name="sage pay configurations")
public class SagePayConfigurationDetails extends AplosBean implements OnlinePaymentConfigurationDetails {
	private static final long serialVersionUID = 7308307682805631316L;

	public enum SagePayUrlType {
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

	private String name;

	private ConnectToType connectToType;     //** Set to SIMULATOR for the Simulator expert system, TEST for the Test Server **
	    //** and LIVE in the live environment **

	private String virtualDir; //'** Change if you've created a Virtual Directory in IIS with a different name **

    /* IMPORTANT.  Set the strYourSiteFQDN value to the Fully Qualified Domain Name of your server. **
    ** This should start http:// or https:// and should be the name by which our servers can call back to yours **
    ** i.e. it MUST be resolvable externally, and have access granted to the Sage Pay servers **
    ** examples would be https://www.mysite.com or http://212.111.32.22/ **
    ** NOTE: You should leave the final / in place. **
    */
	private String yourSiteFQDN;

    /* At the end of a Server transaction, the customer is redirected back to the completion page **
    ** on your site using a client-side browser redirect. On live systems, this page will always be **
    ** referenced using the strYourSiteFQDN value above.  During development and testing, however, it **
    ** is often the case that the development machine sits behind the same firewall as the server **
    ** hosting the kit, so your browser might not be able resolve external IPs or dns names. **
    ** e.g. Externally your server might have the IP 212.111.32.22, but behind the firewall it **
    ** may have the IP 192.168.0.99.  If your test machine is also on the 192.168.0.n network **
    ** it may not be able to resolve 212.111.32.22. **
    ** Set the strYourSiteInternalFQDN to the internal Fully Qualified Domain Name by which **
    ** your test machine can reach the server (in the example above you'd use http://192.168.0.99/) **
    ** If you are not on the same network as the test server, set this value to the same value **
    ** as strYourSiteFQDN above. **
    ** NOTE: You should leave the final / in place. **
    */
	private String yourSiteInternalFQDN;

	private String notificationPageName;
	private String frontEndSuccessfulRedirectionUrl;
	private String frontEndFailedRedirectionUrl;
	private String backEndSuccessfulRedirectionUrl;
	private String backEndFailedRedirectionUrl;
	private String defaultOrderDescription;

	private String vendorName; //'** Set this value to the Vendor Name assigned to you by Sage Pay or chosen when you applied **

	// Currently unused but in the API

	private String partnerID;
	@Column(columnDefinition="LONGTEXT")
	private String notes;

	public enum IntegrationType {
		DIRECT,
		SERVER;
	}

	public enum ConnectToType implements LabeledEnumInter {
		LIVE,
		TEST,
		SIMULATOR;

		@Override
		public String getLabel() {
			return CommonUtil.firstLetterToUpperCase( name().toLowerCase() );
		};
	}

    /*
    * Global Definitions for this site
	*/
    private String strProtocol = "2.23";

    public String getSystemUrl( IntegrationType integrationType, SagePayUrlType sagePayUrlType ) {
    	String strSystemUrl = "";
	    if( getConnectToType().equals( ConnectToType.LIVE ) ) {
	        switch( sagePayUrlType ) {
	            case ABORT :strSystemUrl = "https://live.sagepay.com/gateway/service/abort.vsp";
	            	break;
	            case AUTHORISE:strSystemUrl = "https://live.sagepay.com/gateway/service/authorise.vsp";
	            	break;
	            case CANCEL : strSystemUrl = "https://live.sagepay.com/gateway/service/cancel.vsp";
	            	break;
	            case PURCHASE :
	            	if( integrationType == IntegrationType.SERVER ) {
	            		strSystemUrl = "https://live.sagepay.com/gateway/service/vspserver-register.vsp";
	            	} else if( integrationType == IntegrationType.DIRECT ){
	            		strSystemUrl = "https://live.sagepay.com/gateway/service/vspdirect-register.vsp";
	            	}
	            	break;
	            case REFUND : strSystemUrl = "https://live.sagepay.com/gateway/service/refund.vsp";
            		break;
	            case RELEASE : strSystemUrl = "https://live.sagepay.com/gateway/service/release.vsp";
	                break;
	            case REPEAT : strSystemUrl = "https://live.sagepay.com/gateway/service/repeat.vsp";
		            break;
	            case VOID : strSystemUrl = "https://live.sagepay.com/gateway/service/void.vsp";
	            	break;
	            case THREE_DCALLBACK : strSystemUrl = "https://live.sagepay.com/gateway/service/direct3dcallback.vsp";
	            	break;
	            case SHOWPOST : strSystemUrl = "https://test.sagepay.com/showpost/showpost.asp";
	            	break;
	        }
	    } else if( getConnectToType().equals( ConnectToType.TEST ) ) {
	        switch( sagePayUrlType ) {
	            case ABORT:strSystemUrl = "https://test.sagepay.com/gateway/service/abort.vsp";
	            	break;
	            case AUTHORISE:strSystemUrl = "https://test.sagepay.com/gateway/service/authorise.vsp";
	            	break;
	            case CANCEL: strSystemUrl = "https://test.sagepay.com/gateway/service/cancel.vsp";
	            	break;
	            case PURCHASE:
	            	if( integrationType == IntegrationType.SERVER ) {
	            		strSystemUrl = "https://test.sagepay.com/gateway/service/vspserver-register.vsp";
	            	} else if( integrationType == IntegrationType.DIRECT ) {
	            		strSystemUrl = "https://test.sagepay.com/gateway/service/vspdirect-register.vsp";
	            	}
	            	break;
	            case REFUND: strSystemUrl = "https://test.sagepay.com/gateway/service/refund.vsp";
	        		break;
	            case RELEASE: strSystemUrl = "https://test.sagepay.com/gateway/service/release.vsp";
	                break;
	            case REPEAT: strSystemUrl = "https://test.sagepay.com/gateway/service/repeat.vsp";
		            break;
	            case VOID: strSystemUrl = "https://test.sagepay.com/gateway/service/void.vsp";
	            	break;
	            case THREE_DCALLBACK: strSystemUrl = "https://test.sagepay.com/gateway/service/direct3dcallback.vsp";
	            	break;
	            case SHOWPOST: strSystemUrl = "https://test.sagepay.com/showpost/showpost.asp";
	            	break;
	        }
	    } else {
	    	switch( sagePayUrlType ) {
	            case ABORT:strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorAbortTx";
		        	break;
		        case AUTHORISE:strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorAuthoriseTx";
		        	break;
		        case CANCEL: strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorCancelTx";
		        	break;
		        case PURCHASE:
	            	if( integrationType == IntegrationType.SERVER ) {
	            		strSystemUrl = "https://test.sagepay.com/simulator/VSPServerGateway.asp?Service=VendorRegisterTx";
	            	} else if( integrationType == IntegrationType.DIRECT ) {
	            		strSystemUrl = "https://test.sagepay.com/simulator/VSPDirectGateway.asp";
	            	}
		        	break;
		        case REFUND: strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorRefundTx";
		    		break;
		        case RELEASE: strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorReleaseTx";
		            break;
		        case REPEAT: strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorRepeatTx";
		            break;
		        case VOID: strSystemUrl = "https://test.sagepay.com/simulator/vspserverGateway.asp?Service=VendorVoidTx";
		        	break;
		        case THREE_DCALLBACK: strSystemUrl = "https://test.sagepay.com/simulator/VSPDirectCallback.asp";
		        	break;
		        case SHOWPOST: strSystemUrl = "https://test.sagepay.com/showpost/showpost.asp";
		        	break;
	    	}
	     }

	    return strSystemUrl;
	}

	public String getYourSiteFQDN() {
		return yourSiteFQDN;
	}
	public String getYourSiteInternalFQDN() {
		return yourSiteInternalFQDN;
	}
	public String getVendorName() {
		return vendorName;
	}
	public String getPartnerID() {
		return partnerID;
	}
	public String getProtocol() {
		return strProtocol;
	}
	public ConnectToType getConnectToType() {
		return connectToType;
	}

	public String getNotificationPageName() {
		return notificationPageName;
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

	public String getSuccessfulRedirectionUrl( SagePayPost sagePayPost ) {
		if( sagePayPost.isBackEndPayment() ) {
			return getBackEndSuccessfulRedirectionUrl();
		} else {
			return getFrontEndSuccessfulRedirectionUrl();
		}
	}

	public String getFailedRedirectionUrl( SagePayPost sagePayPost ) {
		if( sagePayPost.isBackEndPayment() ) {
			return getBackEndFailedRedirectionUrl();
		} else {
			return getFrontEndFailedRedirectionUrl();
		}
	}

	public void setDefaultOrderDescription(String defaultOrderDescription) {
		this.defaultOrderDescription = defaultOrderDescription;
	}

	public String getDefaultOrderDescription() {
		return defaultOrderDescription;
	}

	public String getVirtualDir() {
		return virtualDir;
	}

	public void setVirtualDir(String virtualDir) {
		this.virtualDir = virtualDir;
	}

	public void setConnectToType(ConnectToType connectToType) {
		this.connectToType = connectToType;
	}

	public void setYourSiteInternalFQDN(String yourSiteInternalFQDN) {
		this.yourSiteInternalFQDN = yourSiteInternalFQDN;
	}

	public void setYourSiteFQDN(String yourSiteFQDN) {
		this.yourSiteFQDN = yourSiteFQDN;
	}

	public void setNotificationPageName(String notificationPageName) {
		this.notificationPageName = notificationPageName;
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

	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}

	public void setPartnerID(String partnerID) {
		this.partnerID = partnerID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
