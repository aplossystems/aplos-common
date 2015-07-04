package com.aplos.common.beans;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.http.HttpSession;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.interfaces.OnlinePaymentOrder;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class PaymentGatewayPost extends AplosBean {
	private static final long serialVersionUID = -2516646501621372558L;
    @Any(metaColumn = @Column(name = "CONFIGURATION_DETAILS_TYPE"))
    @AnyMetaDef(idType = "long", metaType = "string",
            metaValues = { /* Meta Values added in at run-time */ })
    @JoinColumn(name="CONFIGURATION_DETAILS_ID")
    @DynamicMetaValues
	private OnlinePaymentConfigurationDetails onlinePaymentConfigurationDetails;
    private String description = "";

	@ManyToOne
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address billingAddress;
	@ManyToOne
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address deliveryAddress;
    private boolean isProcessed = false;
    private boolean isPaid= false;
    @Transient
    private String pageError = "";
    private String status = "";
    private BigDecimal deliveryCost;
	
	//  REQUIRED PARAMETERS
    private BigDecimal decTotal = new BigDecimal( 0 );
    @ManyToOne(fetch=FetchType.LAZY)
    private Currency currency;
    
    @Any(metaColumn = @Column(name = "PAYMENT_ORDER_TYPE"))
    @AnyMetaDef(idType = "long", metaType = "string",
            metaValues = { /* Meta Values added in at run-time */ })
    @JoinColumn(name="PAYMENT_ORDER_ID")
    @DynamicMetaValues
    private OnlinePaymentOrder onlinePaymentOrder;
    
//    @Override
//    public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//    	super.hibernateInitialiseAfterCheck(fullInitialisation);
//    	HibernateUtil.initialise( billingAddress, fullInitialisation);
//    	HibernateUtil.initialise( deliveryAddress, fullInitialisation);
//    	HibernateUtil.initialise( getCurrency(), fullInitialisation);
//    	HibernateUtil.initialise( getOnlinePaymentOrder(), fullInitialisation);
//    	HibernateUtil.initialise( getOnlinePaymentConfigurationDetails(), fullInitialisation);
//    }
    
    public abstract void determineOnlinePaymentConfigurationDetails();
    
    public abstract PaymentGatewayPost recurPayment( BigDecimal partPayment);
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( !getClass().equals( PaymentGatewayPost.class ) ) {
			addToScope( CommonUtil.getBinding( PaymentGatewayPost.class ), this, associatedBeanScope );
		}
	}
	
	public void insertTestCardDetails( Address billingAddress, CreditCardDetails creditCardDetails, boolean isForThreeDAuth ) {}

	public OnlinePaymentConfigurationDetails getOnlinePaymentConfigurationDetails() {
		return onlinePaymentConfigurationDetails;
	}
	public void setOnlinePaymentConfigurationDetails(
			OnlinePaymentConfigurationDetails onlinePaymentConfigurationDetails) {
		this.onlinePaymentConfigurationDetails = onlinePaymentConfigurationDetails;
	}
	public void setBillingAddress(Address billingAddress) {
		this.billingAddress = billingAddress;
	}
	public Address getDeliveryAddress() {
		return deliveryAddress;
	}
	public Address getBillingAddress() {
		return billingAddress;
	}
	public void setDeliveryAddress(Address deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}

	public void setProcessed(boolean isProcessed) {
		this.isProcessed = isProcessed;
	}

	public boolean isProcessed() {
		return isProcessed;
	}

	public String getPageError() {
		return pageError;
	}

	public void setPageError(String pageError) {
		this.pageError = pageError;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getDecTotalStr() {
		return decTotal.setScale( 2, RoundingMode.HALF_DOWN ).toString();
	}

	public BigDecimal getDecTotal() {
		return decTotal;
	}

	public void setDecTotal(BigDecimal decTotal) {
		this.decTotal = decTotal;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public OnlinePaymentOrder getOnlinePaymentOrder() {
		return onlinePaymentOrder;
	}

	public void setOnlinePaymentOrder(OnlinePaymentOrder onlinePaymentOrder) {
		this.onlinePaymentOrder = onlinePaymentOrder;
	}

	public BigDecimal getDeliveryCost() {
		return deliveryCost;
	}

	public void setDeliveryCost(BigDecimal deliveryCost) {
		this.deliveryCost = deliveryCost;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPaid() {
		return isPaid;
	}

	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}
	
}
