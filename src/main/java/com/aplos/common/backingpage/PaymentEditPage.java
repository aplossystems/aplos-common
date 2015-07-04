package com.aplos.common.backingpage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.ShoppingCartItem;
import com.aplos.common.beans.Website;
import com.aplos.common.interfaces.PaymentGatewayDirectPost;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PaymentGatewayPost.class)
public class PaymentEditPage extends EditPage {
	private static final long serialVersionUID = -2907144692112139862L;
	private CreditCardDetails creditCardDetails; 

	public PaymentEditPage() {
		creditCardDetails = new CreditCardDetails();
		creditCardDetails.setCardType(CommonConfiguration.getCommonConfiguration().getDefaultCreditCardType());
		PaymentGatewayPost paymentGatewayPost = resolveAssociatedBean();
		
		if( JSFUtil.isLocalHost() && ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			paymentGatewayPost.insertTestCardDetails( paymentGatewayPost.getBillingAddress(), creditCardDetails, false );
			JSFUtil.addMessage( "Test card details added for debugging" );
		}
	}

	public void validateCreditCardNumber(FacesContext context, UIComponent toValidate, Object value) {
		String cardNumber = (String) value;

		int numberLength = cardNumber.replace( " ", "" ).trim().length();
		if ( numberLength > 12 && numberLength < 17 ) {
			try {
				Long.parseLong( cardNumber.replace( " ", "" ).trim() );
			} catch( NumberFormatException nfex ) {
				FacesMessage message = new FacesMessage("Please enter your 13-16 digit card number exactly as shown on the front of your card.");
				message.setSeverity(FacesMessage.SEVERITY_ERROR);
				context.addMessage(toValidate.getClientId(context), message);
				((UIInput) toValidate).setValid(false);
			}
		} else {
			FacesMessage message = new FacesMessage("Please enter your 13-16 digit card number exactly as shown on the front of your card.");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			context.addMessage(toValidate.getClientId(context), message);
			((UIInput) toValidate).setValid(false);
		}
	}

	public void makePayment() {
		makePayment( (PaymentGatewayPost) JSFUtil.getBeanFromScope( PaymentGatewayPost.class ), getCreditCardDetails() );
	}

	public static void makePayment( PaymentGatewayPost directPost, CreditCardDetails creditCardDetails ) {
//		directPost.setOnlinePaymentConfigurationDetails( CommonConfiguration.getCommonConfiguration().getCardSaveCfgDetails() );
		directPost.determineOnlinePaymentConfigurationDetails();
		directPost.saveDetails();
		
		ShoppingCartItem shoppingCartItem = new ShoppingCartItem();
		
		BigDecimal vatPercentage;
		if( CommonConfiguration.getCommonConfiguration().getDefaultVatType() != null ) {
			vatPercentage = CommonConfiguration.getCommonConfiguration().getDefaultVatType().getPercentage();
		} else {
			vatPercentage = new BigDecimal( 0 );
		}
		BigDecimal vatAmount = directPost.getDecTotal().divide( new BigDecimal(100).add( vatPercentage ).divide(vatPercentage, RoundingMode.HALF_UP), RoundingMode.HALF_UP );
		BigDecimal baseAmount = directPost.getDecTotal().subtract( vatAmount );
		shoppingCartItem.setItemCode( directPost.toString() );
		shoppingCartItem.setQuantity( 1 );
		shoppingCartItem.setSingleItemVatAmount( vatAmount );
		shoppingCartItem.setItemName( Website.getCurrentWebsiteFromTabSession().getName() );
		shoppingCartItem.setSingleItemBasePrice( baseAmount );
		shoppingCartItem.updateSingleItemNetPrice();
		shoppingCartItem.updateCachedValues(false);

		List<PaymentSystemCartItem> cartItems = new ArrayList<PaymentSystemCartItem>();
		cartItems.add( shoppingCartItem );
		//this setDeliveryAddress code was taken from existing code, might need to do a safety check on address-null, but it wasnt there before so i dont think its needed
		directPost.setDeliveryAddress(directPost.getBillingAddress().getCopy());
		((PaymentGatewayDirectPost) directPost).createRequestMessage( CommonUtil.getStringOrEmpty(JSFUtil.getRequest().getHeader("user-agent")), cartItems, creditCardDetails, CommonConfiguration.getCommonConfiguration().getDefaultCurrency() );
		((PaymentGatewayDirectPost) directPost).setApply3dSecure(false);
		((PaymentGatewayDirectPost) directPost).postRequest();
		
		if( !directPost.isProcessed() ) {
			if( !directPost.getPageError().equals("") ) {
				JSFUtil.addMessage( directPost.getPageError() );
			} else {
				JSFUtil.addMessage( "Payment was not processed" );
			}
		} else {
			JSFUtil.addMessage( "Payment made successfully" );
		}
		
		directPost.saveDetails();
	}
	
	@Override
	public boolean isValidationRequired() {
		return validationRequired("makePaymentBtn") || super.isValidationRequired();
	}

	public SelectItem[] getCountrySelectItems() {
		return AplosAbstractBean.getSelectItemBeans(Country.class);
	}

	public CreditCardDetails getCreditCardDetails() {
		return creditCardDetails;
	}

	public void setCreditCardDetails(CreditCardDetails creditCardDetails) {
		this.creditCardDetails = creditCardDetails;
	}

}
