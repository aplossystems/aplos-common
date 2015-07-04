package com.aplos.common.beans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.EnumType;
import com.aplos.common.annotations.persistence.Enumerated;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.enums.CartAbandonmentIssue;
import com.aplos.common.enums.CheckoutPageEntry;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Cache
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@BeanScope(scope=JsfScope.TAB_SESSION)
public class ShoppingCart extends AplosBean {

	private static final long serialVersionUID = 290946459876270700L;

	@ManyToOne
	private Currency currency;
	//currencyBaseRate is the rate the stored currency had at the time of the order
	//(all values are cached at default currency base rate, not at the rate of this applied currency)
	private BigDecimal currencyBaseRate = new BigDecimal( 1 );

	@OneToMany
	@Cascade({CascadeType.ALL})
	@Cache
	@JoinColumn(name="shoppingCart_id")
	private List<ShoppingCartItem> items = new ArrayList<ShoppingCartItem>();

	//Unknown is set until we record any specific issue
	@Enumerated(EnumType.STRING)
	private CartAbandonmentIssue abandonmentIssue = CartAbandonmentIssue.UNKNOWN;

	@Enumerated(EnumType.STRING)
	private CheckoutPageEntry lastRecordedPageEntry;
	@Enumerated(EnumType.STRING)
	private CheckoutPageEntry furthestRecordedPageEntry;

	private BigDecimal adminCharge = new BigDecimal( 0 );

	private boolean isVatExempt = false;
	private BigDecimal cachedNetTotalAmount = new BigDecimal( 0 );
	private BigDecimal cachedTotalVatAmount = new BigDecimal( 0 );

	private boolean isPayPalExpressRequested = false;
	
	//@ManyToOne
	@Any(metaColumn = @Column(name = "GATEWAY_POST_TYPE"), fetch=FetchType.LAZY)
    @AnyMetaDef(idType = "long", metaType = "string",
            metaValues = { /* Meta Values added in at run-time */ })
    @JoinColumn(name="GATEWAY_POST_ID")
	@DynamicMetaValues
	private PaymentGatewayPost recurringPaymentGatewayPost;

	public ShoppingCart() {
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialiseList(items, fullInitialisation);
//	}

	@Override
	public <T> T initialiseNewBean() {
		ShoppingCart shoppingCart = super.initialiseNewBean();

		if( CommonConfiguration.getCommonConfiguration() != null ) {
//			currency = HibernateUtil.getImplementation( CommonConfiguration.getCommonConfiguration().getDefaultCurrency(), true, true );
			if( currency != null ) {
				setCurrencyBaseRate( currency.getBaseRate() );
			}
		}

		updateVatCachedValues();
		return (T) shoppingCart;
	}
	
	public void addToFrontEndScope() {
		JSFUtil.addToTabSession( CommonUtil.getBinding( ShoppingCart.class ), this, JSFUtil.getSessionTemp(), true );
	}
	
	public boolean isShoppingCartChangesDisabled() {
		return false;
	}
	
	public PaymentGatewayPost recurPayment( BigDecimal total ) {
		if( getRecurringPaymentGatewayPost() != null ) {
			return getRecurringPaymentGatewayPost().recurPayment( total );
		}
		return null;
	}

	public void applyDiscountToAllItems( BigDecimal discount ) {
		for( ShoppingCartItem tempItem : items ) {
			tempItem.setSingleItemDiscountAndCalculate(discount);
		}
	}
	
	public void removeCartItem( ShoppingCartItem shoppingCartItem ) {
		getItems().remove( shoppingCartItem );
		updateCachedValues(false);
	}
	
	public void removeAllItems() {
		getItems().clear();
		updateCachedValues(false);
	}

	public String getAdminChargeString() {
		return getAdminChargeString(false);
	}

	public String getAdminChargeString(boolean convert) {
		return FormatUtil.formatTwoDigit( getAdminCharge(convert).doubleValue() );
	}
	
	public void notifyCartItemsOfPurchase(PaymentGatewayPost paymentGatewayDirectPost) {
		for (ShoppingCartItem tempCartItem : getItems()) {
			for (int i=0; i < tempCartItem.getQuantity(); i++) {
				tempCartItem.itemPurchased(paymentGatewayDirectPost);
			}
		}
	}

	public void updateAdminCharge(BigDecimal newAdminCharge) {
		if( getAdminCharge().compareTo( new BigDecimal( 0 ) ) <= 0 &&
				getAdminCharge().compareTo( newAdminCharge ) != 0 ) {
			JSFUtil.addMessage( "Admin charge has been added" );
		} else if( newAdminCharge.equals( new BigDecimal( 0 ) ) &&
				getAdminCharge().compareTo( newAdminCharge ) != 0 ) {
			JSFUtil.addMessage( "Admin charge has been removed" );
		} else if( getAdminCharge().compareTo( newAdminCharge ) != 0 ) {
			JSFUtil.addMessage( "Admin charge has been updated" );
		}
		setAdminCharge( newAdminCharge );
	}

	public List<ShoppingCartItem> getItems() {
		return items;
	}

	public void setItems(List<ShoppingCartItem> newItemsList) {
		this.items = newItemsList;
	}

	// We need to call this as hibernate screws up the bidirectional relationship between
	// the items in the cart and the cart.  It replaces the objects with different instances
	// which need to be replaced with the right instance.
	public void refreshShoppingCartItemReferences() {
		for( ShoppingCartItem item : getItems() ) {
			item.setShoppingCart( this );
		}
	}

	public void updateCachedValues( boolean updateChildren ) {
		if( updateChildren ) {
			updateItemCachedValues();
		}

		setCachedNetTotalAmount( getTotal( false ) );
		setCachedTotalVatAmount( getTotalVatAmount() );

	}

	public void updateItemCachedValues() {
		for (int i=0, n = items.size(); i < n; i++) {
			items.get(i).updateCachedValues(false);
		}
	}

	public void updateVatCachedValues() {
		for (int i=0, n = items.size(); i < n; i++) {
			items.get(i).updateVatCachedValue();
		}
		setCachedTotalVatAmount( getTotalVatAmount() );
	}

	protected BigDecimal getTotal( boolean includingVat ) {
		return getTotal( includingVat, false );
	}

	protected BigDecimal getTotal( boolean includingVat, boolean convert ) {
		BigDecimal total = new BigDecimal( 0 );
		for (int i=0, n = items.size(); i < n; i++) {
			if ( includingVat ) {
				BigDecimal grossLinePrice = items.get(i).getCachedGrossLinePrice();
				if (convert) {
					grossLinePrice = Currency.convert(grossLinePrice, currencyBaseRate);
				}
				total = total.add( grossLinePrice );
			} else {
				BigDecimal netLinePrice = items.get(i).getCachedNetLinePrice();
				if (convert) {
					netLinePrice = Currency.convert(netLinePrice, currencyBaseRate);
				}
				total = total.add( netLinePrice  );
			}
		}
		return total;
	}

	protected BigDecimal getTotalVatAmount() {
		return getTotalVatAmount(false);
	}

	protected BigDecimal getTotalVatAmount(boolean convert) {
		BigDecimal total = new BigDecimal( 0 );
		for (int i=0, n = items.size(); i < n; i++) {
			BigDecimal lineVat = items.get(i).getCachedLineVatAmount();
			if (convert) {
				lineVat = Currency.convert(lineVat, currencyBaseRate);
			}
			total = total.add( lineVat );
		}
		return total;
	}

	public BigDecimal getCachedGrossTotalAmount() {
		return getCachedGrossTotalAmount(false);
	}

	public BigDecimal getCachedGrossTotalAmount(boolean convert) {
		return getCachedNetTotalAmount(convert).add( getCachedTotalVatAmount(convert) );
	}

	public String getCachedGrossTotalAmountString() {
		return getCachedGrossTotalAmountString(false);
	}

	public String getCachedGrossTotalAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedGrossTotalAmount(convert).doubleValue() );
	}

	public String getCachedNetTotalAmountString() {
		return getCachedNetTotalAmountString(false);
	}

	public String getCachedNetTotalAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedNetTotalAmount(convert).doubleValue() );
	}

	public String getCachedTotalAmountString() {
		return getCachedTotalAmountString(false);
	}

	public String getCachedTotalAmountString(boolean convert) {
		if (CommonConfiguration.getCommonConfiguration().isVatInclusive()) {
			return FormatUtil.formatTwoDigit( getCachedGrossTotalAmount(convert).doubleValue() );
		} else {
			return FormatUtil.formatTwoDigit( getCachedNetTotalAmount(convert).doubleValue() );
		}
	}

	public boolean isBasketEmpty() {
		if (items.size() > 0) {
			return false;
		}
		return true;
	}

	public boolean isBasketReadyForCheckout() {
		if (isBasketEmpty()) {
			return false;
		}
		return true;
	}

	public BigDecimal getTotalDiscount() {
		return getTotalDiscount(false);
	}

	public BigDecimal getTotalDiscount(boolean convert) {
		BigDecimal discount = new BigDecimal( 0 );
		for (int i=0; i < items.size(); i++) {
			BigDecimal discountAmount = items.get(i).getSingleItemDiscountAmount();
			if (convert) {
				discountAmount = Currency.convert(discountAmount, currencyBaseRate);
			}
			discount = discount.add( discountAmount.multiply( new BigDecimal( items.get(i).getQuantity() ) ) );
		}
		if (convert) {
			return Currency.convert(discount, currencyBaseRate);
		} else {
			return discount;
		}
	}

	public String getTotalDiscountString() {
		return getTotalDiscountString(false);
	}

	public String getTotalDiscountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getTotalDiscount(convert).doubleValue() );
	}

	public void updateVatPercentageOnItems() {
		for( ShoppingCartItem shoppingCartItem : items ) {
			shoppingCartItem.updateVatPercentage();
			shoppingCartItem.updateSingleItemNetPrice();
		}
	}

	public int getNumberOfItems() {
		int count = 0;
		for (int i=0; i < items.size(); i++) {
			count = count + items.get(i).getQuantity();
		}
		return count;
	}

	public void setAbandonmentIssue(CartAbandonmentIssue abandonmentIssue) {
		this.abandonmentIssue = abandonmentIssue;
	}

	public CartAbandonmentIssue getAbandonmentIssue() {
		return abandonmentIssue;
	}

	//TODO: does this need to trigger an update of cached values
	public void setCurrency(Currency currency) {
		this.currency = currency;
		this.currencyBaseRate = currency.getBaseRate();
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setLastRecordedPageEntry(CheckoutPageEntry lastRecordedPageEntry) {
		this.lastRecordedPageEntry = lastRecordedPageEntry;
	}

	public CheckoutPageEntry getLastRecordedPageEntry() {
		return lastRecordedPageEntry;
	}

	public void setFurthestRecordedPageEntry(CheckoutPageEntry furthestRecordedPageEntry) {
		this.furthestRecordedPageEntry = furthestRecordedPageEntry;
	}

	public CheckoutPageEntry getFurthestRecordedPageEntry() {
		return furthestRecordedPageEntry;
	}

	public void setCurrencyBaseRate(BigDecimal currencyBaseRate) {
		this.currencyBaseRate = currencyBaseRate;
	}

	public BigDecimal getCurrencyBaseRate() {
		return currencyBaseRate;
	}

	public void setCachedNetTotalAmount(BigDecimal cachedNetTotalAmount) {
		this.cachedNetTotalAmount = cachedNetTotalAmount;
	}

	public BigDecimal getCachedNetTotalAmount() {
		return getCachedNetTotalAmount(false);
	}

	public BigDecimal getCachedNetTotalAmount(boolean convert) {
		if (convert) {
			return Currency.convert(cachedNetTotalAmount, currencyBaseRate);
		} else {
			return cachedNetTotalAmount;
		}
	}

	public void setCachedTotalVatAmount(BigDecimal cachedTotalVatAmount) {
		this.cachedTotalVatAmount = cachedTotalVatAmount;
	}

	public BigDecimal getCachedTotalVatAmount() {
		return getCachedTotalVatAmount(false);
	}

	public BigDecimal getCachedTotalVatAmount(boolean convert) {
		if (convert) {
			return Currency.convert(cachedTotalVatAmount, currencyBaseRate);
		} else {
			return cachedTotalVatAmount;
		}
	}

	public BigDecimal getGrandTotalVatAmount(boolean convert) {
		return getCachedTotalVatAmount( convert );
	}

	public String getGrandTotalVatAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getGrandTotalVatAmount( convert ) );
	}

	public String getCachedTotalVatAmountString() {
		return getCachedTotalVatAmountString(false);
	}

	public String getCachedTotalVatAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedTotalVatAmount(convert).doubleValue() );
	}

	public void setAdminCharge(BigDecimal adminCharge) {
		if( adminCharge == null ) {
			adminCharge = new BigDecimal( 0 );
		}
		this.adminCharge = adminCharge;
	}

	public BigDecimal getAdminCharge() {
		return getAdminCharge(false);
	}

	public BigDecimal getAdminCharge(boolean convert) {
		if (convert) {
			return Currency.convert(adminCharge, currencyBaseRate);
		} else {
			return adminCharge;
		}
	}

	public boolean isPayPalExpressRequested() {
		return isPayPalExpressRequested;
	}

	public void setPayPalExpressRequested(boolean isPayPalExpressRequested) {
		this.isPayPalExpressRequested = isPayPalExpressRequested;
	}

	public boolean isVatExempt() {
		return isVatExempt;
	}

	public void setVatExempt(boolean isVatExempt) {
		this.isVatExempt = isVatExempt;
	}

	public PaymentGatewayPost getRecurringPaymentGatewayPost() {
		return recurringPaymentGatewayPost;
	}

	public void setRecurringPaymentGatewayPost(
			PaymentGatewayPost recurringPaymentGatewayPost) {
		this.recurringPaymentGatewayPost = recurringPaymentGatewayPost;
	}

}

