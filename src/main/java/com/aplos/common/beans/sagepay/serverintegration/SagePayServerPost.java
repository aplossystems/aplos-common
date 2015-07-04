package com.aplos.common.beans.sagepay.serverintegration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.beans.sagepay.SagePayPost;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class SagePayServerPost extends SagePayPost {
	private static final long serialVersionUID = -8333414357096938709L;
	private static Logger logger = Logger.getLogger( SagePayServerPost.class );
    /*
    * Description
    * ===========
    *
    * This page performs 3 main functions:
    *	(1) Registers the transaction details with Server
    *	(2) Stores the order details, transaction IDs and security keys for this transaction in the database
    *	(3) Redirects the user to the payment pages, or handles errors if the registration fails
    * If the kit is in SIMULATOR mode, everything is shown on the screen and the user asked to Proceed
    * to the payment pages.  In Test and Live mode, nothing is echoed to the screen and the browser
    * is automatically redirected to the payment pages.
    */

	//** Check we have a cart in the session.  If not, go back to the buildOrder page to get one **

    private String securityKey;

    public SagePayServerPost() {}

    public SagePayServerPost( SagePayConfigurationDetails sagePayDetails, SagePayOrder sagePayOrder ) {
    	super( sagePayDetails, sagePayOrder );
    }

    public void setDeliveryAddressFromBillingAddress() {
    	setDeliveryAddress(this.getBillingAddress().getCopy());
    }

//    public String objConn As MySqlConnection = oMySQLConnection()

    public boolean sendToPaymentScreen( List<PaymentSystemCartItem> cartItems ) {
        String strNextURL;


        /* Format the POST to Server **
        ** First we need to generate a unique VendorTxCode for this transaction **
        ** We're using VendorName, time stamp and a random element.  You can use different mehtods if you wish **
        ** but the VendorTxCode MUST be unique for each transaction you send to Server **
        */
        setVendorTxCode( getUniqueVendorTxCode( getSagePayDetails().getVendorName() ) );
        generateBasketStr( cartItems );

        /** Now create the Server POST **
        ** Now to build the Server POST.  For more details see the Server Protocol 2.23 **
        ** NB: Fields potentially containing non ASCII characters are URLEncoded when included in the POST **
        */
        StringBuffer requestPostBuf = new StringBuffer();
        requestPostBuf.append("VPSProtocol=" + getSagePayDetails().getProtocol());
        requestPostBuf.append( "&TxType=" + getTransactionType().name());  //** PAYMENT by default.  You can change this in the includes file **
        requestPostBuf.append( "&Vendor=" + getSagePayDetails().getVendorName());
        requestPostBuf.append( "&VendorTxCode=" + getVendorTxCode()); //** As generated above **

        requestPostBuf.append( "&Amount=" + FormatUtil.formatTwoDigit( getDecTotal().doubleValue(), false )); //** Formatted to 2 decimal places with leading digit but no commas or currency symbols **
        if( getSagePayOrder() == null || getSagePayOrder().getCurrency() == null ) {
        	requestPostBuf.append( "&Currency=GBP" );
        } else {
        	requestPostBuf.append( "&Currency=" + getSagePayOrder().getCurrency().getSymbol() );
        }
        requestPostBuf.append( "&Description=" + getDescription());

        //** Optional: If you are a Sage Pay Partner and wish to flag the transactions with your unique partner id, it should be passed here
//        If Len(strPartnerID) > 0 Then
//            strPost = strPost & "&ReferrerID=" & URLEncode(strPartnerID)  //** You can change this in the includes file **
//        End If

        //** Up to 100 chars of free format description **

        //** The Notification URL is the page to which Server calls back when a transaction completes **
        //** You can change this for each transaction, perhaps passing a session ID or state flag if you wish **

        String siteFQDN = getSagePayDetails().getConnectToType() == SagePayConfigurationDetails.ConnectToType.LIVE ? getSagePayDetails().getYourSiteFQDN() : getSagePayDetails().getYourSiteInternalFQDN();
        requestPostBuf.append( "&NotificationURL=" + siteFQDN + getSagePayDetails().getVirtualDir() + getSagePayDetails().getNotificationPageName() );

        //** Billing Details **

        try {
            addBillingAddressToRequestPost( requestPostBuf );
            addDeliveryAddressToRequestPost( requestPostBuf );
	        //** other optionals **
	        requestPostBuf.append( "&CustomerEMail=" + URLEncoder.encode(getCustomerEMail(), "UTF-8"));
	        requestPostBuf.append( "&Basket=" + URLEncoder.encode(getBasket(), "UTF-8")); //** As created above **
        } catch ( UnsupportedEncodingException uee ) {
        	logger.warn( uee );
        	return false;
        }

        //** For charities registered for Gift Aid, set to 1 to display the Gift Aid check box on the payment pages **
        requestPostBuf.append( "&AllowGiftAid=0");

        //** Allow fine control over AVS/CV2 checks and rules by changing this value. 0 is Default **
        //** It can be changed dynamically, per transaction, if you wish.  See the Server Protocol document **
        if( !getTransactionType().equals( TransactionType.AUTHENTICATE ) ) {
        	requestPostBuf.append( "&ApplyAVSCV2=0");
        }

        //** Allow fine control over 3D-Secure checks and rules by changing this value. 0 is Default **
        //** It can be changed dynamically, per transaction, if you wish.  See the Server Protocol document **
        requestPostBuf.append( "&Apply3DSecure=0");

        //** Optional setting for Profile can be used to set a simpler payment page. See protocol guide for more info. **
        requestPostBuf.append( "&Profile=NORMAL"); //'NORMAL is default setting. Can also be set to LOW for the simpler payment page version.
        setRequestPost( requestPostBuf.toString() );

        //** The full transaction registration POST has now been built **

	    HashMap<String, String> responseAttrMap = new HashMap<String, String>();
        try {
        	URL url =  new URL(getSagePayDetails().getSystemUrl(SagePayConfigurationDetails.IntegrationType.SERVER, SagePayConfigurationDetails.SagePayUrlType.PURCHASE));
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			OutputStream ostream = conn.getOutputStream();
			ostream.write(getRequestPost().getBytes());
			ostream.flush();
			ostream.close();

	        //Get response
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;

	    	int equalsIdx = -1;
	    	String key;
	    	String value;
			while ((line = reader.readLine()) != null) {
				setResponseStr(getResponseStr() + line);
				equalsIdx = line.indexOf( "=" );
	    		key = line.substring( 0, equalsIdx );
	    		value = line.substring( equalsIdx + 1, line.length() );
	    		responseAttrMap.put( key , value);
			}
        } catch( IOException ioex ) {
        	logger.warn( ioex );
        	return false;
        }

        //** No transport level errors, so the message got the Sage Pay **
       	//** Analyse the response from Server to check that everything is okay **
        //** Registration results come back in the Status and StatusDetail fields **
        setStatus(responseAttrMap.get( "Status" ));
        setStatusDetail( responseAttrMap.get( "StatusDetail" ) );

        //** Caters for both OK and OK REPEATED if the same transaction is registered twice **
        if( getStatus().startsWith( "OK" ) ) {
            /** An OK status mean that the transaction has been successfully registered **
            ** Your code needs to extract the VPSTxId (Sage Pay's unique reference for this transaction) **
            ** and the SecurityKey (used to validate the call back from Sage Pay later) and the NextURL **
            ** (the URL to which the customer's browser must be redirected to enable them to pay) **
            */
            setVpsTxId(responseAttrMap.get( "VPSTxId" ));
            setSecurityKey(responseAttrMap.get( "SecurityKey" ));
            strNextURL = responseAttrMap.get( "NextURL" );

            try {
	            if( !getSagePayDetails().getConnectToType().equals( SagePayConfigurationDetails.ConnectToType.SIMULATOR ) ) {
	                JSFUtil.getResponse().sendRedirect( strNextURL );
	                JSFUtil.getFacesContext().responseComplete();
	            }
            } catch( IOException ioex ) {
            	logger.warn( ioex );
            	return false;
            }

        } else if( getStatus().equals( "MALFORMED" ) ) {
            /** A MALFORMED status occurs when the POST sent above is not correctly formatted **
            ** or is missing compulsory fields.  You will normally only see these during **
            ** development and early testing **
            */
            setPageError( "Sage Pay returned an MALFORMED status. " +
            "The POST was Malformed because \"" + responseAttrMap.get( "StatusDetail" ) + "\"" );
            return false;
        } else if ( getStatus().equals( "INVALID" ) ) {
            /** An INVALID status occurs when the structure of the POST was correct, but **
            ** one of the fields contains incorrect or invalid data.  These may happen when live **
            ** but you should modify your code to format all data correctly before sending **
            ** the POST to Server **
            */
        	setPageError( "Sage Pay returned an INVALID status. " +
            "The data sent was Invalid because \"" + responseAttrMap.get( "StatusDetail" ) + "\"" );
            return false;
        } else {
            /** The only remaining status is ERROR **
            ** This occurs extremely rarely when there is a system level error at Sage Pay **
            ** If you receive this status the payment systems may be unavailable **<br>
            ** You could redirect your customer to a page offering alternative methods of payment here **
            */
        	setPageError( "Sage Pay returned an ERROR status. " +
            "The description of the error was \"" + responseAttrMap.get( "StatusDetail" ) + "\"" );
            return false;
        }

        return true;
    }

	public String getDecTotalStr() {
		return FormatUtil.formatTwoDigit( getDecTotal().doubleValue(), false );
	}

	public void setSecurityKey(String securityKey) {
		this.securityKey = securityKey;
	}

	public String getSecurityKey() {
		return securityKey;
	}


}
