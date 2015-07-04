package com.aplos.common.backingpage.payments.cardsave;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosUrl;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.cardsave.directintegration.CardSaveDirectPost;
import com.aplos.common.interfaces.CardSaveOrder;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@WindowId(required=false)
@GlobalAccess
public class CardSaveThreeDCompletePage extends BackingPage {

	private static final long serialVersionUID = 660555745209364483L;
	// redirect would happen before this message is displayed
	// but the message can be changed to an error message.
	private String message = "Transaction completed successfully";

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		//threeDMd/MD is cross reference in cardsave but stored in the same variable as sagepay for consistency
		BeanDao cardSaveDirectPostDao = new BeanDao( CardSaveDirectPost.class ).addWhereCriteria( "crossReference = :crossReference" );
		String md = JSFUtil.getRequest().getParameter( "MD" );
		String pares = JSFUtil.getRequest().getParameter( "PARes" );
		cardSaveDirectPostDao.setNamedParameter( "crossReference", md );
		List<CardSaveDirectPost> cardSaveDirectPostList = cardSaveDirectPostDao.getAll();
		CardSaveDirectPost cardSaveDirectPost;

		if( cardSaveDirectPostList.size() > 0 ) {

			cardSaveDirectPost = cardSaveDirectPostList.get( 0 ).getSaveableBean();

			CardSaveOrder cardSaveOrder = (CardSaveOrder) cardSaveDirectPost.getCardSaveOrder().getSaveableBean();
			cardSaveDirectPost.postRequest( 
					md, 
					pares //pareq instead?
			);
			redirectAfterRequestPost( cardSaveOrder, cardSaveDirectPost );
			cardSaveDirectPost.saveDetails();

		} else {
			ApplicationUtil.handleError( new Exception("Card save direct post not found for cross reference " + md ), false );
			JSFUtil.addMessageForError( "Your payment has not be taken due to a payment processing issue.  Please contact a member of our staff to continue" );
			JSFUtil.redirect( new AplosUrl( "/", false ), true, true );
		}

		JSFUtil.getFacesContext().responseComplete();
		return true;
	}

	public static void redirectAfterRequestPost( CardSaveOrder cardSaveOrder, CardSaveDirectPost cardSaveDirectPost ) {
		if ( cardSaveDirectPost.getPageState().equals( CardSaveDirectPost.PageState.THREE_D_REDIRECT ) ) {
			cardSaveOrder.threeDRedirect();
			
		} else if ( !cardSaveDirectPost.isProcessed() ) {
			
			JSFUtil.addMessage( cardSaveDirectPost.getPageError() );
			cardSaveOrder.sendPaymentFailureEmail(cardSaveDirectPost.getPageError());
			
		} else {
			cardSaveOrder.executePaymentCompleteRoutine(true, cardSaveDirectPost);
		}
		
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}












