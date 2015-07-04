package com.aplos.common.backingpage.payments.sagepay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import com.aplos.common.AplosUrl;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails.SagePayUrlType;
import com.aplos.common.beans.sagepay.directintegration.SagePayDirectPost;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@WindowId(required=false)
@GlobalAccess
public class SagePayThreeDCompletePage extends BackingPage {
	private static final long serialVersionUID = 7825476679307421485L;
	private static Logger logger = Logger.getLogger( SagePayThreeDCompletePage.class );
	//private static final long serialVersionUID = -2701592617473952978L;
	
	// redirect would happen before this message is displayed
	// but the message can be changed to an error message.
	private String message = "Transaction completed successfully";

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		// POST for Sage Pay Direct 3D completion page
		BeanDao sagePayDirectPostDao = new BeanDao( SagePayDirectPost.class ).addWhereCriteria( "threeDMd = :threeDMd" );
		sagePayDirectPostDao.setNamedParameter( "threeDMd", JSFUtil.getRequest().getParameter( "MD" ));
		List<SagePayDirectPost> sagePayDirectPostList = sagePayDirectPostDao.getAll();
		SagePayDirectPost sagePayDirectPost;

		if( sagePayDirectPostList.size() > 0 ) {
			sagePayDirectPost = sagePayDirectPostList.get( 0 );
			sagePayDirectPost.setThreeDMd(null);

			try {
				SagePayOrder sagePayOrder = sagePayDirectPost.getSagePayOrder();
//				sagePayOrder.hibernateInitialise( true );
				String requestStr = "MD=" + JSFUtil.getRequest().getParameter( "MD" ) + "&PARes=" + URLEncoder.encode( JSFUtil.getRequest().getParameter( "PARes" ), "UTF-8");
				sagePayDirectPost.setRequestPostUrl( ((SagePayConfigurationDetails) sagePayDirectPost.getOnlinePaymentConfigurationDetails()).getSystemUrl(SagePayConfigurationDetails.IntegrationType.DIRECT, SagePayUrlType.THREE_DCALLBACK) );
				sagePayDirectPost.setRequestPost( requestStr );
				sagePayDirectPost.postRequest();
				redirectAfterRequestPost( sagePayOrder, sagePayDirectPost );
			} catch ( UnsupportedEncodingException uee ) {
				logger.warn( uee );
				//EcommerceUtil.addAbandonmentIssueToCart( CartAbandonmentIssue.PAYMENT_ISSUE );
				message = "Error occurred : " + uee.getMessage();
			}
		} else {
			JSFUtil.redirect( new AplosUrl( "/", false ), true, true );
		}
		JSFUtil.getFacesContext().responseComplete();
		return true;
	}

	public static void redirectAfterRequestPost( SagePayOrder sagePayOrder, SagePayDirectPost sagePayDirectPost ) {
		if( sagePayDirectPost.getPageState().equals( SagePayDirectPost.PageState.PAYPAL_REDIRECT ) ) {
			JSFUtil.redirect( new AplosUrl( (String) JSFUtil.getFromTabSession( "PayPalRedirectURL" ), false ), false);
		}
		if( sagePayDirectPost.getPageState().equals( SagePayDirectPost.PageState.THREE_D_REDIRECT ) ) {
			sagePayOrder.threeDRedirect();
		} else if( !sagePayDirectPost.isProcessed() ) {
			JSFUtil.addMessage( sagePayDirectPost.getPageError() );
			sagePayOrder.sendPaymentFailureEmail(sagePayDirectPost.getPageError());
		} else {
			sagePayOrder.executePaymentCompleteRoutine(true, sagePayDirectPost);
		}
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}












