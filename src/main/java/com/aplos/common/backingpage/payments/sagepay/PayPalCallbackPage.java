package com.aplos.common.backingpage.payments.sagepay;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.sagepay.directintegration.SagePayDirectPost;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class PayPalCallbackPage extends BackingPage {
	private static final long serialVersionUID = -2701592617473952978L;
	// redirect would happen before this message is displayed
	// but the message can be changed to an error message.
	private String message = "Transaction completed successfully";

	@Override
	public boolean requestPageLoad() {
		super.requestPageLoad();
		// POST for Sage Pay Paypal completion page
		String status = JSFUtil.getRequest().getParameter( "Status" );
	    String vpsTxId = JSFUtil.getRequest().getParameter( "VPSTxId" );

	    BeanDao sagePayDirectPostDao = new BeanDao( SagePayDirectPost.class ).addWhereCriteria( "vpsTxId = :vpsTxId" );
	    sagePayDirectPostDao.setNamedParameter( "vpsTxId", vpsTxId );
		List<SagePayDirectPost> sagePayDirectPostList = sagePayDirectPostDao.getAll();
		SagePayDirectPost sagePayDirectPost;

		if( sagePayDirectPostList.size() > 0 ) {
			sagePayDirectPost = sagePayDirectPostList.get( 0 );

			SagePayOrder sagePayOrder = sagePayDirectPost.getSagePayOrder();
//			sagePayOrder.hibernateInitialise( true );
			if( status.equals( "PAYPALOK" ) ) {
				sagePayDirectPost.setProcessed( true );
				sagePayDirectPost.setPaid( true );
				sagePayDirectPost.saveDetails();
			}
			SagePayThreeDCompletePage.redirectAfterRequestPost( sagePayOrder, sagePayDirectPost );
		} else {
//			JSFUtil.redirect( "checkout-failed.aplos", true );
		}
		//JSFUtil.getFacesContext().responseComplete();
		return true;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}












