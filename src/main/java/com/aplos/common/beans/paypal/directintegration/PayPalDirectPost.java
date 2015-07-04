package com.aplos.common.beans.paypal.directintegration;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aplos.common.AplosUrl;
import com.aplos.common.ExternalBackingPageUrl;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.backingpage.payments.paypal.PayPalValidatePage;
import com.aplos.common.beans.Address;
import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.beans.paypal.PayPalPost;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.interfaces.PayPalOrder;
import com.aplos.common.interfaces.PaymentGatewayDirectPost;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.services.NVPCallerServices;

@Entity
@GlobalAccess
public class PayPalDirectPost extends PayPalPost implements PaymentGatewayDirectPost {
	private static final long serialVersionUID = -8974237682901104166L;
	private String token;
	private String confirmRequestStr;
	private String confirmResponseStr;
	private boolean isUsingPayPalExpress = false;

	public PayPalDirectPost() {
		super();
	}

    public PayPalDirectPost( PayPalConfigurationDetails payPalDetails, PayPalOrder payPalOrder ) {
    	super( payPalDetails, payPalOrder );
		setDecTotal( getOnlinePaymentOrder().getGrandTotal( true ) );
    }
    
    @Override
    public void createRequestMessage(String userAgent,
    		List<? extends PaymentSystemCartItem> cartItems,
    		CreditCardDetails creditCardDetails, Currency currency ) {
    }
    
    @Override
    public void setOnlinePaymentConfigurationDetails(
    		OnlinePaymentConfigurationDetails onlinePaymentConfigurationDetails) {	
    }
    

    public void postRequest() {
    	try {
	    	NVPCallerServices caller = new NVPCallerServices();
			NVPEncoder encoder = new NVPEncoder();
	        caller.setAPIProfile(createApiProfile());
	        PayPalConfigurationDetails payPalDetails = getPayPalConfigurationDetails();
	        encoder.add("VERSION", String.valueOf(payPalDetails.getApiCallVersion()));
			encoder.add("PAYMENTACTION", "Sale");
			encoder.add("AMT", FormatUtil.formatTwoDigit( getDecTotal(), false ) );
			encoder.add("CURRENCYCODE", "GBP");
	
			if( isUsingPayPalExpress ) {
				payPalPaymentOption( encoder, getDeliveryAddress() );
			} else {
				creditCardPaymentOption( encoder );
			}
	
			setRequestPost( encoder.encode() );
			String nvpRequest = encoder.encode();
			String nvpResponse =caller.call(nvpRequest);
			stripCardDetailsFromRequestPost();
	
			NVPDecoder resultValues = new NVPDecoder();
			resultValues.decode(nvpResponse);
			String strAck = resultValues.get("ACK");
			setResponseStr( nvpResponse );
			if (strAck !=null && !(strAck.equals("Success") || strAck.equals("SuccessWithWarning")))	{
				String strErr = resultValues.get("L_LONGMESSAGE0");
				setPageError(strErr);
			} else {
				if( isUsingPayPalExpress ) {
					setToken( resultValues.get("TOKEN").toString() );
					AplosUrl aplosUrl = new AplosUrl( payPalDetails.getPayPalServerType().getExpressCheckoutUrl() );
					aplosUrl.addQueryParameter("cmd", "_express-checkout" );
					aplosUrl.addQueryParameter("token",resultValues.get("TOKEN").toString());
	    			JSFUtil.redirect( aplosUrl, false, true );
				} else {
					setProcessed(true);
					setPaid(true);
				}
			}
    	} catch( PayPalException payPalEx ) {
    		ApplicationUtil.getAplosContextListener().handleError(payPalEx);
    	}
	}

    public void creditCardPaymentOption( NVPEncoder encoder ) {
		encoder.add("METHOD", "DoDirectPayment");
		//request-specific
		encoder = addDirectPaymentValuePairs(encoder);
		encoder.add("STREET", getBillingAddress().getLine1());
		encoder.add("CITY", getBillingAddress().getCity());
		encoder.add("STATE", getBillingAddress().getState());
		encoder.add("ZIP", getBillingAddress().getPostcode());
		encoder.add("COUNTRYCODE", getBillingAddress().getCountry().getIso2());
		//Execute the API operation and obtain the response.
    }

    public void payPalPaymentOption( NVPEncoder encoder, Address shippingAddress ) {
		encoder.add("METHOD", "SetExpressCheckout");
		encoder.add("ADDROVERRIDE", "1");
		encoder.add("RETURNURL", new ExternalBackingPageUrl( PayPalValidatePage.class ).toString() );
		encoder.add("CANCELURL", new ExternalBackingPageUrl( PayPalValidatePage.class ).toString() );
		encoder.add("SHIPTONAME", shippingAddress.getContactFullName() );
		encoder.add("SHIPTOSTREET", shippingAddress.getLine1() );
		encoder.add("SHIPTOSTREET2", shippingAddress.getLine2() );
		encoder.add("SHIPTOCITY", shippingAddress.getCity() );
		encoder.add("SHIPTOSTATE", shippingAddress.getState() );
		encoder.add("SHIPTOCOUNTRYCODE", shippingAddress.getCountry().getIso2() );
		encoder.add("SHIPTOZIP", shippingAddress.getPostcode() );
		encoder.add("PHONENUM", shippingAddress.getPhone() );
    }

    public NVPEncoder addDirectPaymentValuePairs(NVPEncoder encoder) {
    	//these are all common to direct payments and recurring payments
        CreditCardDetails creditCardDetails = ((PayPalOrder) getOnlinePaymentOrder()).determineCreditCardDetails();
		encoder.add("CREDITCARDTYPE", creditCardDetails.getCardType().getPayPalTag());
		encoder.add("ACCT", creditCardDetails.getCardNumber()); //credit card number
		if (creditCardDetails.getCardType().getPayPalTag()!= null &&
			(creditCardDetails.getCardType().getPayPalTag().equals("Maestro") ||
					creditCardDetails.getCardType().getPayPalTag().equals("Solo"))) {
			encoder.add("STARTDATE", creditCardDetails.getStartMonth()+""+creditCardDetails.getStartYear());
			encoder.add("ISSUENUMBER", String.valueOf(creditCardDetails.getIssueNo()));
		} else {
			String expiryMonthStr = new DecimalFormat( "00" ).format( creditCardDetails.getExpiryMonth() );
			String expiryYearStr;
			Calendar cal = new GregorianCalendar();
			cal.setTime( new Date() );
			if( creditCardDetails.getExpiryYear() < 20 && (cal.get(Calendar.YEAR) % 100) > 80 ) {
				expiryYearStr = new DecimalFormat( "00" ).format( (cal.get(Calendar.YEAR) / 100) + 1 );
			} else {
				expiryYearStr = new DecimalFormat( "00" ).format( cal.get(Calendar.YEAR) / 100 );
			}

			expiryYearStr += new DecimalFormat( "00" ).format( creditCardDetails.getExpiryYear() );
			encoder.add("EXPDATE", expiryMonthStr + expiryYearStr );
		}
		encoder.add("CVV2", creditCardDetails.getCvv());
		encoder.add("FIRSTNAME", getBillingAddress().getContactFirstName());
		encoder.add("LASTNAME", getBillingAddress().getContactSurname());
		return encoder;
    }

	public void stripCardDetailsFromRequestPost() {
		String cardDetailNames[] = { "ACCT", "STARTDATE", "EXPDATE",
				"ISSUENUMBER", "CVV2" };

		for( int i = 0, n = cardDetailNames.length; i < n; i++ ) {
	        Pattern pattern = Pattern.compile( "(" + cardDetailNames[ i ] + "=)(.*?)&" );
			Matcher matcher = pattern.matcher( getRequestPost() );
			if( matcher.find() ) {
				setRequestPost(getRequestPost().replace( matcher.group( 1 ) + matcher.group( 2 ), matcher.group(1) + "****" ) );
			}
		}
	}
	
	@Override
	public void setApply3dSecure(boolean isApply3dSecure) {
		/*
		 * This is disabled by default, you have to sign up to Cardinal Centinel to get it working.
		 */
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getConfirmRequestStr() {
		return confirmRequestStr;
	}

	public void setConfirmRequestStr(String confirmRequestStr) {
		this.confirmRequestStr = confirmRequestStr;
	}

	public String getConfirmResponseStr() {
		return confirmResponseStr;
	}

	public void setConfirmResponseStr(String confirmResponseStr) {
		this.confirmResponseStr = confirmResponseStr;
	}

	public boolean isUsingPayPalExpress() {
		return isUsingPayPalExpress;
	}

	public void setUsingPayPalExpress(boolean isUsingPayPalExpress) {
		this.isUsingPayPalExpress = isUsingPayPalExpress;
	}

//    @Override
//	public void createRequestStr( List<PayPalCartItem> cartItems, CreditCardDetails creditCardDetails ) {
//		StringBuffer nvpairs = new StringBuffer(super.getApiSignatureString());
//		//dont change the names of these name value pairs, they are PayPal's terminology
//		nvpairs.append("CREDITCARDTYPE=");
//		nvpairs.append(  ); //What is expected here?
//		nvpairs.append("&ACCT="); //credit card number
//		nvpairs.append(creditCardDetails.getCardNumber());
//		nvpairs.append("&EXPDATE="); //month and year of expiry
//		nvpairs.append(  ); //What format is expected here?
//		nvpairs.append("&FIRSTNAME=");
//		nvpairs.append(URLEncoder.encode( getBillingAddress().getContactFirstName(), "UTF-8" ));
//		nvpairs.append("&LASTNAME=");
//		nvpairs.append(URLEncoder.encode( getBillingAddress().getContactSurname(), "UTF-8" ));
//		nvpairs.append("&AMT="); //amount to bill [per cycle when overridden by PayPalRecurringPaymentDirectPost]
//		calculateDecTotal( cartItems );
//		nvpairs.append(getDecTotal());
//		setRequestPost( nvpairs.toString() );
//	}
}
