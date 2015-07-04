package com.aplos.common.beans.sagepay.directintegration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.ExternalBackingPageUrl;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.backingpage.payments.sagepay.PayPalCallbackPage;
import com.aplos.common.backingpage.payments.sagepay.SagePayThreeDCallbackPage;
import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.beans.sagepay.SagePayPost;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.PaymentGatewayDirectPost;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@DynamicMetaValueKey(oldKey="CARDSAVE")
public class SagePayDirectPost extends SagePayPost implements PaymentGatewayDirectPost {
	private static final long serialVersionUID = -8280586711226593822L;
	private boolean isPayPalExpress;

	@Transient
	private PageState pageState=PageState.NORMAL;

	private String threeDMd;
	private String securityKey;
	private boolean isUsingPayPal = false;
	private String authoriseRequestPost;
	private String requestPostUrl;
    private Apply3DSecure apply3dSecure = Apply3DSecure.NORMAL;

    public enum Apply3DSecure {
    	NORMAL,
    	FORCE,
    	OFF,
    	ASK_BUT_AUTHORISE;
    }


	public enum PageState {
		NORMAL,
		THREE_D_REDIRECT,
		PAYPAL_REDIRECT;
	}

	public SagePayDirectPost() {}

    public SagePayDirectPost( SagePayConfigurationDetails sagePayDetails, SagePayOrder sagePayOrder ) {
    	super( sagePayDetails, sagePayOrder );

		setUsingPayPal( getSagePayOrder().isUsingPayPal() );
    }

	public void createRequestStr( List<? extends PaymentSystemCartItem> cartItems, CreditCardDetails creditCardDetails ) {
		/* All required fields are present, so first store the order in the database then format the POST to Sage Pay Direct
		** First we need to generate a unique VendorTxCode for this transaction
		** We're using VendorName, time stamp and a random element.  You can use different methods if you wish
		** but the VendorTxCode MUST be unique for each transaction you send to Sage Pay Direct */
		setVendorTxCode( getUniqueVendorTxCode( getSagePayDetails().getVendorName() ) );
		/* Calculate the transaction total based on basket contents.  For security
		** we recalculate it here rather than relying on totals stored in the session or hidden fields
		** We'll also create the basket contents to pass to Sage Pay Direct. See the Sage Pay Direct Protocol for
		** the full valid basket format.  The code below converts from our "x of y" style into
		** the Sage Pay system basket format (using a 17.5% VAT calculation for the tax columns) */
		generateBasketStr( cartItems );

		createSagePayDirectPostRequestStr( creditCardDetails ); 
		
	}
	
	public final void authorise( BigDecimal partPayment ) {
		setTransactionType(TransactionType.AUTHORISE);
		StringBuffer requestPostBuf = new StringBuffer();
        requestPostBuf.append( "VPSProtocol=" + getSagePayDetails().getProtocol());
        requestPostBuf.append( "&TxType=" + getTransactionType().name());  //** PAYMENT by default.  You can change this in the includes file **
        requestPostBuf.append( "&Vendor=" + getSagePayDetails().getVendorName());
        requestPostBuf.append( "&VendorTxCode=" ).append( getVendorTxCode() ).append( "auth" ); //** As generated above **
        if( partPayment != null ) {
        	requestPostBuf.append( "&Amount=" + FormatUtil.formatTwoDigit( partPayment.doubleValue(), false ) ); //** Formatted to 2 decimal places with leading digit but no commas or currency symbols **
        } else {
        	requestPostBuf.append( "&Amount=" + FormatUtil.formatTwoDigit( getDecTotal().doubleValue(), false ) ); //** Formatted to 2 decimal places with leading digit but no commas or currency symbols **
        }
        
        try {
    		requestPostBuf.append( "&Description=" + URLEncoder.encode( getDescription(), "UTF-8" ) );
        } catch ( UnsupportedEncodingException uee ) {
        	ApplicationUtil.getAplosContextListener().handleError( uee );
        	setProcessed( false );
        }

		requestPostBuf.append( "&RelatedVPSTxId=" ).append( getVpsTxId() );
		requestPostBuf.append( "&RelatedVendorTxCode=" ).append( getVendorTxCode() );
		requestPostBuf.append( "&RelatedSecurityKey=" ).append( getSecurityKey() );
		setAuthoriseRequestPost( requestPostBuf.toString() );
	}
	
	public void createSagePayDirectPostRequestStr( CreditCardDetails creditCardDetails ) {
		// Now create the Sage Pay Direct POST

		/* Now to build the Sage Pay Direct POST.  For more details see the Sage Pay Direct Protocol 2.23
		** NB: Fields potentially containing non ASCII characters are URLEncoded when included in the POST */

        StringBuffer requestPostBuf = new StringBuffer();
        requestPostBuf.append( "VPSProtocol=" + getSagePayDetails().getProtocol());
        requestPostBuf.append( "&TxType=" + getTransactionType().name());  //** PAYMENT by default.  You can change this in the includes file **
        requestPostBuf.append( "&Vendor=" + getSagePayDetails().getVendorName());
        requestPostBuf.append( "&VendorTxCode=" + getVendorTxCode()); //** As generated above **
        requestPostBuf.append( "&Apply3DSecure=" + getApply3dSecure().ordinal() );

        requestPostBuf.append( "&Amount=" + FormatUtil.formatTwoDigit( getDecTotal().doubleValue(), false ) ); //** Formatted to 2 decimal places with leading digit but no commas or currency symbols **
        if( getSagePayOrder() != null && getSagePayOrder().getCurrency() != null ) {
        	requestPostBuf.append( "&Currency=" + getSagePayOrder().getCurrency() );
        } else {
        	if( getCurrency() != null ) {
        		requestPostBuf.append( "&Currency=" + getCurrency() );
        	} else {
        		requestPostBuf.append( "&Currency=" + CommonConfiguration.getCommonConfiguration().getDefaultCurrency() );
        	}
        }

		// Optional: If you are a Sage Pay Partner and wish to flag the transactions with your unique partner id, it should be passed here
//					    if (strlen($strPartnerID) > 0)
//					            $strPost=$strPost . "&ReferrerID=" . URLEncode($strPartnerID);  //You can change this in the includes file

		// Up to 100 chars of free format description

		// Card details. Not required if CardType = "PAYPAL"
		if ( creditCardDetails != null ) {
			if ( !isUsingPayPal ) {
				requestPostBuf.append( "&CardHolder=" + creditCardDetails.getCardholderName() );
				requestPostBuf.append( "&CardNumber=" + creditCardDetails.getCardNumber() );
				DecimalFormat twoDigFormat = new DecimalFormat( "00" );
				if (creditCardDetails.getStartYear() != null && creditCardDetails.getStartYear() != 0 ) {
					requestPostBuf.append( "&StartDate=" + twoDigFormat.format( creditCardDetails.getStartMonth() ) + twoDigFormat.format( creditCardDetails.getStartYear() ) );
				}

				requestPostBuf.append( "&ExpiryDate=" + twoDigFormat.format( creditCardDetails.getExpiryMonth() ) + twoDigFormat.format( creditCardDetails.getExpiryYear() ) );
				if (creditCardDetails.getIssueNo() != null && creditCardDetails.getIssueNo() != 0 ) {
					requestPostBuf.append( "&IssueNumber=" + creditCardDetails.getIssueNo() );
				}

				requestPostBuf.append( "&CV2=" + creditCardDetails.getCvv() );
				requestPostBuf.append( "&CardType=" + creditCardDetails.getCardType().getSagePayTag() );
			} else {
				requestPostBuf.append( "&CardType=PAYPAL" );
			}

		}

        // If this is a PaypalExpress checkout method then NO billing and delivery details are supplied
        try {

            if (getSagePayOrder() != null) {
            	String description = getSagePayOrder().getPurchaseDescription();
            	if( description == null ) {
            		description = ((SagePayConfigurationDetails) getOnlinePaymentConfigurationDetails()).getDefaultOrderDescription();
            	}
    			requestPostBuf.append( "&Description=" + URLEncoder.encode( CommonUtil.getStringOrEmpty( description ), "UTF-8" ) );
            } else {
    			requestPostBuf.append( "&Description=" + URLEncoder.encode( getDescription(), "UTF-8" ) );
            }

	        if (isPayPalExpress == false)
	        {
	            addBillingAddressToRequestPost( requestPostBuf );
	            addDeliveryAddressToRequestPost( requestPostBuf );
	        }

	        if ( isUsingPayPal )
	        {
	        	requestPostBuf.append( "&PayPalCallbackURL=" + URLEncoder.encode(getSagePayDetails().getYourSiteFQDN() + getSagePayDetails().getVirtualDir() + new BackingPageUrl(PayPalCallbackPage.class), "UTF-8") );
	        }

			// Set other optionals
	        requestPostBuf.append( "&CustomerEMail=" + URLEncoder.encode(getCustomerEMail(), "UTF-8"));
	        requestPostBuf.append( "&Basket=" + URLEncoder.encode(getBasket(), "UTF-8")); //** As created above **

        } catch ( UnsupportedEncodingException uee ) {
        	ApplicationUtil.getAplosContextListener().handleError(uee);
        	setProcessed( false );
        }

		/* For PAYPAL cardtype only: Fully qualified domain name of the URL to which customers are redirected upon
        ** completion of a PAYPAL transaction. Here we are getting strYourSiteFQDN & strVirtualDir from
        ** the includes file. Must begin with http:// or https:// */


		// For charities registered for Gift Aid, set to 1 to makr this as a Gift Aid transaction
        requestPostBuf.append( "&GiftAidPayment=0" );

		/* Allow fine control over AVS/CV2 checks and rules by changing this value. 0 is Default
		** It can be changed dynamically, per transaction, if you wish.  See the Sage Pay Direct Protocol document */
//				if ( !getTransactionType().equals( TransactionType.AUTHENTICATE ) ) {
//					requestPostBuf.append( "&ApplyAVSCV2=0" );
//				}

		// Send the IP address of the person entering the card details
		if( getSagePayDetails().getConnectToType().equals( SagePayConfigurationDetails.ConnectToType.LIVE ) ) {
			requestPostBuf.append( "&ClientIPAddress=" + JSFUtil.getRequest().getRemoteAddr() );
		} else {
			requestPostBuf.append( "&ClientIPAddress=10.10.10.10" );
		}

		/* Send the account type to be used for this transaction.  Web sites should us E for e-commerce **
		** If you are developing back-office applications for Mail Order/Telephone order, use M **
		** If your back office application is a subscription system with recurring transactions, use C **
		** Your Sage Pay account MUST be set up for the account type you choose.  If in doubt, use E **/
		requestPostBuf.append( "&AccountType=E" );
		setRequestPost( requestPostBuf.toString() );
	}
	
	@Override
	public void setApply3dSecure(boolean isApply3dSecure) {
		if( isApply3dSecure ) {
			setApply3dSecure(Apply3DSecure.NORMAL);
		} else {
			setApply3dSecure(Apply3DSecure.OFF);
		}
	}

	public void postRequest() {
		postRequest( getRequestPost(), getRequestPostUrl() );
	}

	public void postRequest( String postUrl ) {
		postRequest( getRequestPost(), postUrl );
	}

	public void postRequest( String postRequestStr, String postUrl ) {
		/* The full transaction registration POST has now been built **
		** Send the post to the target URL
		** if anything goes wrong with the connection process:
		** - $arrResponse["Status"] will be 'FAIL';
		** - $arrResponse["StatusDetail"] will be set to describe the problem
		** Data is posted to strPurchaseURL which is set depending on whether you are using SIMULATOR, TEST or LIVE */

	    HashMap<String, String> responseAttrMap = new HashMap<String, String>();
        try {
        	URL url = new URL( postUrl ); 
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			OutputStream ostream = conn.getOutputStream();
			ostream.write(postRequestStr.getBytes());
			ostream.flush();
			ostream.close();

	        //Get response
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;

	    	int equalsIdx = -1;
	    	String key;
	    	String value;
			while ((line = reader.readLine()) != null) {
				equalsIdx = line.indexOf( "=" );
	    		key = line.substring( 0, equalsIdx );
	    		value = line.substring( equalsIdx + 1, line.length() );
	    		if( !(key.equals( "ACSURL" ) || key.equals( "PAReq" )) ) {
					setResponseStr(getResponseStr() + line);
	    		} else {
					setResponseStr(getResponseStr() + key + "=****" );
	    		}
	    		responseAttrMap.put( key , value);
			}
        } catch( IOException ioex ) {
        	ApplicationUtil.getAplosContextListener().handleError(ioex);
        	return;
        }

		/* Analyse the response from Sage Pay Direct to check that everything is okay
		** Registration results come back in the Status and StatusDetail fields */

        stripCardDetailsFromRequestPost();
        setStatus(responseAttrMap.get( "Status" ));
        setStatusDetail( responseAttrMap.get( "StatusDetail" ) );
//        if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
//        	setStatus("INVALID");
//        }
		if (getStatus().startsWith( "3DAUTH" ) ) {
			/* This is a 3D-Secure transaction, so we need to redirect the customer to their bank
			** for authentication.  First get the pertinent information from the response */
			threeDMd = responseAttrMap.get( "MD" );
			JSFUtil.getSessionTemp().setAttribute( "sagepayMD", threeDMd );
			JSFUtil.getSessionTemp().setAttribute( "sagepayACSURL", responseAttrMap.get( "ACSURL" ));
			JSFUtil.getSessionTemp().setAttribute( "sagepayPAReq", responseAttrMap.get( "PAReq" ));

			AplosUrl threeDCallBackAplosUrl = new ExternalBackingPageUrl( SagePayThreeDCallbackPage.class );
			threeDCallBackAplosUrl.addQueryParameter( "jsessionid", JSFUtil.getRequest().getSession().getId() );
			JSFUtil.getSessionTemp().setAttribute( "sagepayThreeDCallback", threeDCallBackAplosUrl.toString()  );
//			if (JSFUtil.getExternalContext().getRequestServerName().equals("localhost")) {
//				JSFUtil.addToTabSession( "sagepayThreeDCallback", JSFUtil.getServerUrl() + JSFUtil.getContextPath() + "/common/payments/sagepay/threeDCallback.jsf" );
//			} else {
//				JSFUtil.addToTabSession( "sagepayThreeDCallback", JSFUtil.getServerUrl().replace( "http:", "https:") + JSFUtil.getContextPath() + "/common/payments/sagepay/threeDCallback.jsf" );
//			}

			setPageState(PageState.THREE_D_REDIRECT);
		} else if(getStatus().startsWith( "PPREDIRECT" )) {
            /* The customer needs to be redirected to a PayPal URL as PayPal was chosen as a card type or
	            ** payment method and PayPal is active for your account. A VPSTxId and a PayPalRedirectURL are
	            ** returned in this response so store the VPSTxId in your database now to match to the response
	            ** after the customer is redirected to the PayPalRedirectURL to go through PayPal authentication */
				JSFUtil.addToTabSession( "PayPalRedirectURL", responseAttrMap.get( "PayPalRedirectURL" ) );
				setPageState(PageState.PAYPAL_REDIRECT);
	            setVpsTxId( responseAttrMap.get( "VPSTxId" ) );
				return;
		} else {
			/* If this isn't 3D-Auth, then this is an authorisation result (either successful or otherwise) **
			** Get the results form the POST if they are there */

			// Update the database and redirect the user appropriately
			if ( getStatus().equals("OK")) {
				setPageError( "AUTHORISED - The transaction was successfully authorised with the bank." );
			} else if(getStatus().equals("MALFORMED")) {
				setPageError( "MALFORMED - The StatusDetail was: " + getStatusDetail());
			} else if(getStatus().equals("INVALID")) {
				setPageError( "INVALID - The StatusDetail was: " + getStatusDetail());
			} else if(getStatus().equals("NOTAUTHED")) {
				setPageError( "DECLINED - The transaction was not authorised by the bank.");
				if( getSagePayDetails().getConnectToType().equals( SagePayConfigurationDetails.ConnectToType.TEST ) ) {
					JSFUtil.addMessage( "The server is in test mode and requires test card details, this are available at the SagePay website" );
				}
			} else if(getStatus().equals("REJECTED")) {
				setPageError( "REJECTED - The transaction was failed by your 3D-Secure or AVS/CV2 rule-bases.");
			} else if(getStatus().equals("AUTHENTICATED")) {
				setPageError( "AUTHENTICATED - The transaction was successfully 3D-Secure Authenticated and can now be Authorised.");
			} else if(getStatus().equals("REGISTERED")) {
	            setVpsTxId( responseAttrMap.get( "VPSTxId" ) );
	            setSecurityKey( responseAttrMap.get( "SecurityKey" ) );
				setPageError( "REGISTERED - The transaction was could not be 3D-Secure Authenticated, but has been registered to be Authorised.");
			} else if(getStatus().equals("ERROR")) {
				setPageError( "ERROR - There was an error during the payment process.  The error details are: " + getStatusDetail());
			} else {
				setPageError( "UNKNOWN - An unknown status was returned from Sage Pay.  The Status was: " + getStatus() + ", with StatusDetail:" + getStatusDetail());
			}
			
			if(getStatus().equals("OK")) {
				getStatus().equals("OK");
				setPaid( true );
			}

			if (getStatus().equals("OK")||getStatus().equals("AUTHENTICATED")||getStatus().equals("REGISTERED")) {
				setProcessed( true );
				return;
			}
		}
	}

	public void setPageState(PageState pageState) {
		this.pageState = pageState;
	}

	public PageState getPageState() {
		return pageState;
	}

	public void setThreeDMd(String threeDMd) {
		this.threeDMd = threeDMd;
	}

	public String getThreeDMd() {
		return threeDMd;
	}

	public void setUsingPayPal(boolean isUsingPayPal) {
		this.isUsingPayPal = isUsingPayPal;
	}

	public boolean isUsingPayPal() {
		return isUsingPayPal;
	}

	public Apply3DSecure getApply3dSecure() {
		return apply3dSecure;
	}

	public void setApply3dSecure(Apply3DSecure apply3dSecure) {
		this.apply3dSecure = apply3dSecure;
	}

	public String getSecurityKey() {
		return securityKey;
	}

	public void setSecurityKey(String securityKey) {
		this.securityKey = securityKey;
	}

	public String getAuthoriseRequestPost() {
		return authoriseRequestPost;
	}

	public void setAuthoriseRequestPost(String authoriseRequestPost) {
		this.authoriseRequestPost = authoriseRequestPost;
	}

	@Override
	public void createRequestMessage(String userAgent,List<? extends PaymentSystemCartItem> cartItems, CreditCardDetails creditCardDetails, Currency currency) {
		createRequestStr(cartItems, creditCardDetails);
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		addToScope(CommonUtil.getBinding(PaymentGatewayPost.class), this, associatedBeanScope);
	}

	public String getRequestPostUrl() {
		return requestPostUrl;
	}

	public void setRequestPostUrl(String requestPostUrl) {
		this.requestPostUrl = requestPostUrl;
	}

}
