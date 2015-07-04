package com.aplos.common.backingpage.payments.paypal;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import com.aplos.common.annotations.FrontendBackingPage;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.beans.paypal.directintegration.PayPalDirectPost;
import com.aplos.common.interfaces.PayPalOrder;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.services.NVPCallerServices;

@ManagedBean
@RequestScoped
@GlobalAccess
@FrontendBackingPage
public class PayPalValidatePage extends BackingPage {

	private static final long serialVersionUID = 1959043888187081773L;

	public PayPalValidatePage() {
//		AqlBeanDao aqlBeanDao = new AqlBeanDao( PayPalDirectPost.class );
//		AqlBeanDao.addWhereCriteria( "bean.id = 95" );
//		PayPalDirectPost payPalDirectPost2 = (PayPalDirectPost) AqlBeanDao.getAllQuery().uniqueResult();
//		payPalDirectPost2.getPayPalOrder().reevaluateOrderObjectsSession();
//		redirectToPaymentSuccessful();
		
		String token = JSFUtil.getRequestParameter( "token" );
		String payerId = JSFUtil.getRequestParameter( "PayerID" );
		if( CommonUtil.getStringOrEmpty( token ).equals( "" ) || CommonUtil.getStringOrEmpty( payerId ).equals( "" ) ) {
			JSFUtil.addMessage( "Url is not in the correct format" );
			redirectToPaymentFailed();
		} else {
			BeanDao payPalDirectPostDao = new BeanDao( PayPalDirectPost.class );
			payPalDirectPostDao.addWhereCriteria( "bean.token = :token" );
			payPalDirectPostDao.setNamedParameter( "token", token );
			PayPalDirectPost payPalDirectPost = (PayPalDirectPost) payPalDirectPostDao.getFirstBeanResult();
			if( payPalDirectPost != null ) {
				if( payPalDirectPost.isProcessed() ) {
					redirectToPaymentAlreadyMade();
				} else {
					if( !validateOrder( payPalDirectPost, token, payerId ) ) {
						redirectToPaymentFailed();
					}
				}
			} else {
				JSFUtil.addMessage( "Paypal token does not match any orders in the database" );
				redirectToPaymentFailed();
			}
		}
	}

	public boolean validateOrder( PayPalDirectPost payPalDirectPost, String token, String payerId ) {
        try {
	    	NVPCallerServices caller = new NVPCallerServices();
			NVPEncoder encoder = new NVPEncoder();
			caller.setAPIProfile(payPalDirectPost.createApiProfile());
	        PayPalConfigurationDetails payPalDetails = payPalDirectPost.getPayPalConfigurationDetails();
	        encoder.add( "VERSION", String.valueOf(payPalDetails.getApiCallVersion()));
			encoder.add( "METHOD", "DoExpressCheckoutPayment" );
			encoder.add( "TOKEN", token );
			encoder.add( "PAYERID", payerId );
			encoder.add( "PAYMENTACTION", "Sale" );
			encoder.add( "AMT", FormatUtil.formatTwoDigit( payPalDirectPost.getDecTotal(), false ) );
			encoder.add( "CURRENCYCODE", "GBP" );
			encoder.add( "IPADDRESS", JSFUtil.getRequest().getServerName() );

		    String nvpRequest = encoder.encode();
			String nvpResponse =caller.call(nvpRequest);

			NVPDecoder resultValues = new NVPDecoder();
			resultValues.decode(nvpResponse);
			String strAck = resultValues.get("ACK");
		    if(strAck !=null && !(strAck.equalsIgnoreCase("Success") || strAck.equalsIgnoreCase("SuccessWithWarning")))
		    {
		    	JSFUtil.addMessage( resultValues.get("L_LONGMESSAGE0") );
		    } else {
				PayPalOrder payPalOrder = (PayPalOrder) payPalDirectPost.getOnlinePaymentOrder();

//				payPalOrder.hibernateInitialise( true );
				payPalOrder.paymentComplete( true, null );
				payPalOrder.sendConfirmationEmail();
				payPalOrder.reevaluateOrderObjectsSession();
				redirectToPaymentSuccessful();
				return true;
		    }
		    payPalDirectPost.setConfirmRequestStr( nvpRequest );
		    payPalDirectPost.setConfirmResponseStr( nvpResponse );
			payPalDirectPost.saveDetails();
		} catch (PayPalException e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}

        return false;
	}

	public void redirectToPaymentFailed() {

	}

	public void redirectToPaymentAlreadyMade() {

	}

	public void redirectToPaymentSuccessful() {

	}
}
