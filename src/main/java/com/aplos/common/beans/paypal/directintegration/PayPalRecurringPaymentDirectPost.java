package com.aplos.common.beans.paypal.directintegration;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.DiscriminatorValue;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.interfaces.PayPalOrder;
import com.aplos.common.interfaces.RecurringPayPalOrder;
import com.aplos.common.utils.ApplicationUtil;
import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.services.NVPCallerServices;

@Entity
@DiscriminatorValue("PaypalRecurring")
@PluralDisplayName(name="PayPal recurring payments")
public class PayPalRecurringPaymentDirectPost extends PayPalDirectPost {

	private static final long serialVersionUID = 1676694134909132944L;
	private static Logger logger = Logger.getLogger( PayPalRecurringPaymentDirectPost.class );

	public PayPalRecurringPaymentDirectPost() {}

    public PayPalRecurringPaymentDirectPost( PayPalConfigurationDetails payPalDetails, PayPalOrder payPalOrder ) {
    	super( payPalDetails, payPalOrder );
    }

	public String getFormattedDate( Date date ) {
		SimpleDateFormat paypalFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'M'");
		return paypalFormat.format(date);
	}

	public void postRequest() {
    	try {
			NVPCallerServices caller = new NVPCallerServices();
	    	caller.setAPIProfile(createApiProfile());
	        NVPEncoder encoder = new NVPEncoder();
	
	        encoder.add("METHOD", "CreateRecurringPaymentsProfile");
	        encoder = addDirectPaymentValuePairs(encoder);
	        if( getOnlinePaymentOrder() instanceof RecurringPayPalOrder ) {
	        	RecurringPayPalOrder recurringPalOrder = (RecurringPayPalOrder) getOnlinePaymentOrder();
		        encoder.add("PROFILESTARTDATE", getFormattedDate( recurringPalOrder.getRecurringPaymentStartDate() ));
		        encoder.add("BILLINGPERIOD", recurringPalOrder.getRecurringPaymentBillingPeriod().getRequestStrValue());
		        encoder.add("BILLINGFREQUENCY", String.valueOf(recurringPalOrder.getRecurringPaymentBillingFrequency()));
		        encoder.add("TOTALBILLINGCYCLES", recurringPalOrder.getRecurringPaymentTotalBillingCycles().toString());
		        encoder.add("MAXFAILEDPAYMENTS", recurringPalOrder.getRecurringPaymentMaxFailedPayments().toString());
		        if (recurringPalOrder.hasInitialPaymentBeforeRecurring()) {
		        	//we can take this immediately and then rebill at rate in amount
		        	encoder.add("INITAMT", recurringPalOrder.getRecurringPaymentInitialPaymentAmount().toString());
		        	encoder.add("FAILEDINITAMTACTION", getPayPalConfigurationDetails().getFailedInitialAmountAction());
				}
	        }
	        encoder.add("VERSION", String.valueOf(getPayPalConfigurationDetails().getApiCallVersion()));
	
			String strNVPRequest = encoder.encode();
			//send the request to the server and return the response NVPString
			setResponseStr(caller.call( strNVPRequest));
			NVPDecoder resultValues = new NVPDecoder();
	        try {
				//decode method of NVPDecoder will parse the request and decode the name and value pair
				resultValues.decode(getResponseStr());
		    } catch(Exception e) {
				e.printStackTrace();
			}
			String strAck = resultValues.get("ACK");
			if (strAck !=null && !(strAck.equals("Success") || strAck.equals("SuccessWithWarning")))	{
				String strErr = resultValues.get("L_LONGMESSAGE0");
				setPageError(strErr);
				logger.info("paypal call failed");
			} else {
				setProcessed(true);
				setPaid(true);
			}
    	} catch( PayPalException payPalEx ) {
    		ApplicationUtil.getAplosContextListener().handleError(payPalEx);
    	}
    }

	public enum BillingPeriod {

		DAY("Day"),
		WEEK("Week"),
		SEMIMONTH("SemiMonth"),
		MONTH("Month"),
		YEAR("Year");

		private String requestStrValue;
		BillingPeriod(String requestStrValue){
			this.requestStrValue = requestStrValue;
		}

		public String getRequestStrValue() {
			return requestStrValue;
		}

	}

}
