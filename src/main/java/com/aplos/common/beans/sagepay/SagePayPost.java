package com.aplos.common.beans.sagepay;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
public class SagePayPost extends PaymentGatewayPost {
	private static final long serialVersionUID = -5637209792611634743L;

    private String vendorTxCode = "";

    // OPTIONAL PARAMETERS
    private String customerEMail = "";

    @Column(columnDefinition="LONGTEXT")
    private String basket;

    @Column(columnDefinition="LONGTEXT")
    private String statusDetail;

    private String vpsTxId;
    private boolean isBackEndPayment;

    @Column(columnDefinition="LONGTEXT")
    private String requestPost;
    @Column(columnDefinition="LONGTEXT")
    private String responseStr = "";
    private TransactionType transactionType = TransactionType.PAYMENT;
    
    public enum TransactionType {
    	PAYMENT,
    	AUTHENTICATE,
    	DEFERRED,
    	AUTHORISE;
    }

    public SagePayPost() {}

    public SagePayPost( SagePayConfigurationDetails sagePayDetails, SagePayOrder sagePayOrder ) {
    	this.setOnlinePaymentConfigurationDetails(sagePayDetails);
    	this.setOnlinePaymentOrder(sagePayOrder);
    	retrieveSagePayServerItemInformation();
    }
    
    public SagePayOrder getSagePayOrder() {
    	return (SagePayOrder) getOnlinePaymentOrder();
    }
    
    @Override
    public PaymentGatewayPost recurPayment(BigDecimal partPayment) {
    	return null;
    }
    
    @Override
    public void determineOnlinePaymentConfigurationDetails() {
    	setOnlinePaymentConfigurationDetails( CommonConfiguration.getCommonConfiguration().determineSagePayCfgDetails() );
    }

    public void retrieveSagePayServerItemInformation() {
    	SagePayOrder sagePayOrder = getSagePayOrder();
		setBillingAddress( sagePayOrder.getBillingAddress().getCopy() );
		if( sagePayOrder.isDeliveryAddressTheSame() ) {
			setDeliveryAddress( sagePayOrder.getBillingAddress().getCopy() );
		} else {
			setDeliveryAddress( sagePayOrder.getDeliveryAddress().getCopy() );
		}
		setDescription( sagePayOrder.getPurchaseDescription() );
		setCurrency( sagePayOrder.getCurrency() );
    }


	public static String getUniqueVendorTxCode( String vendorName ) {
        Date todaysDate = new Date();
		return todaysDate.getTime() + Math.round(Math.random() * 100000) + vendorName;
	}

	public void generateBasketStr( List<? extends PaymentSystemCartItem> cartItems ) {
        /* Calculate the transaction total based on basket contents.  For security **
        ** we recalculate it here rather than relying on totals stored in the session or hidden fields **
        ** We'll also create the basket contents to pass to Server. See the Server Protocol for **
        ** the full valid basket format.  The code below converts from our "x of y" style into **
        ** the system basket format (using a 17.5% VAT calculation for the tax columns) **
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

	public void stripCardDetailsFromRequestPost() {
		String cardDetailNames[] = { "CardNumber", "StartDate", "ExpiryDate",
				"IssueNumber", "CV2" };

		for( int i = 0, n = cardDetailNames.length; i < n; i++ ) {
	        Pattern pattern = Pattern.compile( "(" + cardDetailNames[ i ] + "=)(.*?)&" );
			Matcher matcher = pattern.matcher( getRequestPost() );
			if( matcher.find() ) {
				setRequestPost(getRequestPost().replace( matcher.group( 1 ) + matcher.group( 2 ), matcher.group(1) + "****" ) );
			}
		}
	}


	public void addBillingAddressToRequestPost( StringBuffer requestPostBuf ) throws UnsupportedEncodingException {
    	requestPostBuf.append( "&BillingSurname=" + URLEncoder.encode(getBillingAddress().getContactSurname(), "UTF-8"));
        requestPostBuf.append( "&BillingFirstnames=" + URLEncoder.encode(getBillingAddress().getContactFirstName(), "UTF-8"));
        requestPostBuf.append( "&BillingAddress1=" + URLEncoder.encode(getBillingAddress().getLine1(), "UTF-8"));
        if( getBillingAddress().getLine2().length() > 0 ) {
        	requestPostBuf.append( "&BillingAddress2=" + URLEncoder.encode(getBillingAddress().getLine2(), "UTF-8"));
        }
        requestPostBuf.append( "&BillingCity=" + URLEncoder.encode(getBillingAddress().getCity(), "UTF-8"));
        if( CommonUtil.getStringOrEmpty( getBillingAddress().getPostcode() ).equals( "" ) ) {
            requestPostBuf.append(  "&BillingPostCode=0" );
        } else {
        	requestPostBuf.append(  "&BillingPostCode=" + URLEncoder.encode(getBillingAddress().getPostcode(), "UTF-8"));
        }
        if ( getBillingAddress().getCountry() != null ) {
    	    requestPostBuf.append( "&BillingCountry=" + URLEncoder.encode(getBillingAddress().getCountry().getIso2(), "UTF-8"));
        }
        //  This is for US customers only and requires the a 2 digit code
        if( getBillingAddress().getCountry() != null && getBillingAddress().getCountry().getId().equals( 840l ) && getBillingAddress().getState() != null && getBillingAddress().getState().length() > 0 ) {
        	requestPostBuf.append( "&BillingState=" + URLEncoder.encode(getBillingAddress().getState(), "UTF-8"));
        }
        if( getBillingAddress().getPhone() != null && getBillingAddress().getPhone().length() > 0 ) {
        	requestPostBuf.append( "&BillingPhone=" + URLEncoder.encode(getBillingAddress().getPhone(), "UTF-8"));
        }
	}

	public void addDeliveryAddressToRequestPost( StringBuffer requestPostBuf ) throws UnsupportedEncodingException {
		if (getDeliveryAddress().getContactSurname() != null) {
			requestPostBuf.append( "&DeliverySurname=" + URLEncoder.encode(getDeliveryAddress().getContactSurname(), "UTF-8"));
		}
		if (getDeliveryAddress().getContactFirstName() != null) {
			requestPostBuf.append( "&DeliveryFirstnames=" + URLEncoder.encode(getDeliveryAddress().getContactFirstName(), "UTF-8"));
		}
		if (getDeliveryAddress().getLine1() != null) {
			requestPostBuf.append( "&DeliveryAddress1=" + URLEncoder.encode(getDeliveryAddress().getLine1(), "UTF-8"));
		}
        if( getDeliveryAddress().getLine2() != null && getDeliveryAddress().getLine2().length() > 0 ) {
        	requestPostBuf.append( "&DeliveryAddress2=" + URLEncoder.encode(getDeliveryAddress().getLine2(), "UTF-8"));
        }
        if (getDeliveryAddress().getCity() != null) {
        	requestPostBuf.append( "&DeliveryCity=" + URLEncoder.encode(getDeliveryAddress().getCity(), "UTF-8"));
        }

        if( CommonUtil.getStringOrEmpty( getBillingAddress().getPostcode() ).equals( "" ) ) {
        	requestPostBuf.append( "&DeliveryPostCode=0" );
        } else {
        	requestPostBuf.append( "&DeliveryPostCode=" + URLEncoder.encode(getDeliveryAddress().getPostcode(), "UTF-8"));
        }
        if (getDeliveryAddress().getCountry() != null) {
        	requestPostBuf.append( "&DeliveryCountry=" + URLEncoder.encode(getDeliveryAddress().getCountry().getIso2(), "UTF-8"));
        }
        //  This is for US customers only and requires the a 2 digit code
        if( getDeliveryAddress().getCountry() != null && getDeliveryAddress().getCountry().getId().equals( 840l ) && getDeliveryAddress().getState() != null && getDeliveryAddress().getState().length() > 0 ) {
        	requestPostBuf.append( "&DeliveryState=" + URLEncoder.encode(getDeliveryAddress().getState(), "UTF-8"));
        }
        if( getDeliveryAddress().getPhone() != null && getDeliveryAddress().getPhone().length() > 0 ) {
        	requestPostBuf.append( "&DeliveryPhone=" + URLEncoder.encode(getDeliveryAddress().getPhone(), "UTF-8"));
        }
	}

	public void setRequestPost(String requestPost) {
		this.requestPost = requestPost;
	}

	public String getRequestPost() {
		return requestPost;
	}

	public void setResponseStr(String responseStr) {
		this.responseStr = responseStr;
	}

	public String getResponseStr() {
		return responseStr;
	}

	public void setVpsTxId(String vpsTxId) {
		this.vpsTxId = vpsTxId;
	}

	public String getVpsTxId() {
		return vpsTxId;
	}

	public void setStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
	}

	public String getStatusDetail() {
		return statusDetail;
	}

	public void setVendorTxCode(String vendorTxCode) {
		this.vendorTxCode = vendorTxCode;
	}

	public String getVendorTxCode() {
		return vendorTxCode;
	}

	public SagePayConfigurationDetails getSagePayDetails() {
		return (SagePayConfigurationDetails) getOnlinePaymentConfigurationDetails();
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
}
