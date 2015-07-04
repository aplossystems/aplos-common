package com.aplos.common.backingpage.payments.paypal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpServletRequest;

import com.aplos.common.annotations.FrontendBackingPage;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@RequestScoped
@GlobalAccess
@FrontendBackingPage
public class PayPalIpnListenerPage extends BackingPage {
	private static final long serialVersionUID = 4846930231327944228L;

	public PayPalIpnListenerPage() {
	}
	
	@Override
	public boolean requestPageLoad() {
		boolean continueLoad = super.requestPageLoad();
		
		HttpServletRequest request = JSFUtil.getRequest();
		Enumeration en = request.getParameterNames();
		String str = "cmd=_notify-validate";
		try {
			while(en.hasMoreElements()){
				String paramName = (String)en.nextElement();
				String paramValue = request.getParameter(paramName);
				str = str + "&" + paramName + "=" + URLEncoder.encode(paramValue, "UTF-8");
			}
		} catch( UnsupportedEncodingException ueex ) {
			ApplicationUtil.getAplosContextListener().handleError( ueex );
		}

		/*
		 *  Payment may already have been taken as IPN is called when payments are made
		 *  through an auction website also
		 */
		if( !"true".equals( request.getParameter("for_auction") ) && !"Refunded".equals( request.getParameter("payment_status" ) ) ) {
			// post back to PayPal system to validate
			// NOTE: change http: to https: in the following URL to verify using SSL (for increased security).
			// using HTTPS requires either Java 1.4 or greater, or Java Secure Socket Extension (JSSE)
			// and configured for older versions.
			try {
				URL u;
				u = new URL("https://www.paypal.com/cgi-bin/webscr");
				URLConnection uc = u.openConnection();
				uc.setDoOutput(true);
				uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				PrintWriter pw = new PrintWriter(uc.getOutputStream());
				pw.println(str);
				pw.close();
		
				BufferedReader in = new BufferedReader(
				new InputStreamReader(uc.getInputStream()));
				String res = in.readLine();
				in.close();
	
	//			// assign posted variables to local variables
	//			String itemName = request.getParameter("item_name");
	//			String itemNumber = request.getParameter("item_number");
	//			String paymentStatus = request.getParameter("payment_status");
	//			String paymentAmount = request.getParameter("mc_gross");
	//			String paymentCurrency = request.getParameter("mc_currency");
	//			String txnId = request.getParameter("txn_id");
	//			String receiverEmail = request.getParameter("receiver_email");
	//			String payerEmail = request.getParameter("payer_email");
				String orderId = request.getParameter("invoice");
	
				if(res.equals("VERIFIED")) {
					if( !CommonUtil.isNullOrEmpty( orderId ) ) {
						verify( orderId );
					} else {
						ApplicationUtil.getAplosContextListener().handleError( new Exception( "Order id is null : " + str ), false );
					}
				}
				else if(res.equals("INVALID")) {
					ApplicationUtil.getAplosContextListener().handleError( new Exception( "PayPal payment invalid : " + str ) );
				}
				else {
					ApplicationUtil.getAplosContextListener().handleError( new Exception( "PayPal payment failed with res : " + res + ", " + str ) );
				}
			} catch (Exception e) {
				ApplicationUtil.getAplosContextListener().handleError( e );
			}
		}
		return continueLoad;
	}
	
	public void verify( String txnId ) {
		
	}
}
