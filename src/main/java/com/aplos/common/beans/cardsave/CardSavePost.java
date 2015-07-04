package com.aplos.common.beans.cardsave;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.thepaymentgateway.common.NullableInt;
import net.thepaymentgateway.paymentsystem.CrossReferenceTransaction;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails.CardSaveUrlType;
import com.aplos.common.beans.cardsave.directintegration.CardSaveDirectPost;
import com.aplos.common.interfaces.CardSaveOrder;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class CardSavePost extends PaymentGatewayPost {
	private static final long serialVersionUID = -4769231260964367014L;

    // OPTIONAL PARAMETERS
    private String customerEMail = "";

    @Column(columnDefinition="LONGTEXT")
    private String basket;

    @Column(columnDefinition="LONGTEXT")
    private String statusDetail;

    private boolean isBackEndPayment;

    @Column(columnDefinition="LONGTEXT")
    private String requestPost;
    @Column(columnDefinition="LONGTEXT")
    private String responseStr = "";
    private TransactionType transactionType = TransactionType.SALE;
    
    public enum TransactionType {
    	SALE,
    	PREAUTH,
    	REFUND,
    	COLLECTION;
    }

    public enum DeviceCategory { 
    	COMPUTER_BROWSER (0),
    	MOBILE_DEVICE (1);
    	
    	private int val;
    	
    	private DeviceCategory(int val) {
			this.val = val;
		}
    	
    	public NullableInt value() {
    		return new NullableInt( val );
    	}
    }

    public CardSavePost() {}

    public CardSavePost( CardSaveConfigurationDetails cardSaveDetails, CardSaveOrder cardSaveOrder ) {
    	init( cardSaveDetails, cardSaveOrder );
    }
    
    public void init( CardSaveConfigurationDetails cardSaveDetails, CardSaveOrder cardSaveOrder ) {
    	this.setOnlinePaymentConfigurationDetails(cardSaveDetails);
    	this.setOnlinePaymentOrder(cardSaveOrder);
		setBillingAddress( cardSaveOrder.getBillingAddress().getCopy() );
    }
    
    public CardSaveOrder getCardSaveOrder() {
    	return (CardSaveOrder) getOnlinePaymentOrder();
    }

	public void setStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
	}

	public String getStatusDetail() {
		return statusDetail;
	}

	public void setCustomerEMail(String customerEMail) {
		this.customerEMail = customerEMail;
	}

	public String getCustomerEMail() {
		return customerEMail;
	}

	public void setBasket(String basket) {
		this.basket = basket;
	}

	public String getBasket() {
		return basket;
	}

	public void setBackEndPayment(boolean isBackEndPayment) {
		this.isBackEndPayment = isBackEndPayment;
	}

	public boolean isBackEndPayment() {
		return isBackEndPayment;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}
	
	/**
     * basket isnt specifically required for / used by cardsave but it seemed useful to save on the objects as with sagepay
     * it is given to the transaction details as transaction description
     * @param cartItems
     */
	public void generateBasketStr( List<PaymentSystemCartItem> cartItems ) {
        /* Calculate the transaction total based on basket contents.  For security **
        ** we recalculate it here rather than relying on totals stored in the session or hidden fields **
        ** We'll also create the basket contents to pass to Server. See the Server Protocol for **
        ** the full valid basket format.  The code below converts from our "x of y" style into **
        ** the system basket format**
        */

        PaymentSystemCartItem tempCartItem;

        StringBuffer basketBuf = new StringBuffer();
        if( getDeliveryCost() != null ) {
	        basketBuf.append(cartItems.size() + 1);
        } else {
        	basketBuf.append( cartItems.size() );
        }
        for( int i = 0, n = cartItems.size(); i < n; i++ ) {
        	tempCartItem = cartItems.get( i );
            // Add another item to our Server basket **
            basketBuf.append( ":" + tempCartItem.getItemName().replace( ":", "-" ) + ":" + tempCartItem.getQuantity());
            basketBuf.append( ":" + FormatUtil.formatTwoDigit( cartItems.get( i ).getSingleItemFinalPrice(false).doubleValue() ) ); //'** Price ex-Vat **
            basketBuf.append( ":" + FormatUtil.formatTwoDigit( cartItems.get( i ).getSingleItemVatAmount().doubleValue() ) ); // '** VAT component **
            basketBuf.append( ":" + FormatUtil.formatTwoDigit( cartItems.get( i ).getSingleItemFinalPrice(true).doubleValue() ) ); // '** Item price **
            basketBuf.append( ":" + FormatUtil.formatTwoDigit( cartItems.get( i ).getPaymentSystemGrossLinePrice().doubleValue() ) ); // '** Line total **
        }

        //** We've been right through the cart, so delivery to the total and the basket **
        if( getDeliveryCost() != null ) {
	        String deliveryCostStr = FormatUtil.formatTwoDigit( getDeliveryCost().doubleValue() );
	        basketBuf.append(":Delivery:1:" + deliveryCostStr + ":---:" + deliveryCostStr + ":" + deliveryCostStr);
        }

        setBasket( basketBuf.toString() );
	}
	
	@Override
	public PaymentGatewayPost recurPayment(BigDecimal partPayment) {
		return null;
	}
    
    @Override
    public void determineOnlinePaymentConfigurationDetails() {
    	setOnlinePaymentConfigurationDetails( CommonConfiguration.getCommonConfiguration().determineCardSaveCfgDetails() );
    }

	public void stripCardDetailsFromRequestPost() {
		Pattern pattern = Pattern.compile( "<CardDetails>(.*)</CardDetails>" );
		if( getRequestPost() != null ) {
			Matcher matcher = pattern.matcher( getRequestPost() );
			while( matcher.find() ) {
				setRequestPost(getRequestPost().replace( matcher.group( 1 ), "****" ) );
			}
		}
	}

	public String getRequestPost() {
		return requestPost;
	}

	public void setRequestPost(String requestPost) {
		this.requestPost = requestPost;
	}

	public String getResponseStr() {
		return responseStr;
	}

	public void setResponseStr(String responseStr) {
		this.responseStr = responseStr;
	}

}




















