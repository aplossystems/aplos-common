package com.aplos.common.beans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class ShoppingCartItem extends AplosBean implements PaymentSystemCartItem {
	private static final long serialVersionUID = 4452686559431866315L;

	/*
	 *  This may be different to the net price depending on whether VAT
	 *  is inclusive or exclusive
	 */
	private BigDecimal singleItemBasePrice = new BigDecimal( 0 );
	private BigDecimal singleItemNetPrice = new BigDecimal( 0 );
	private String itemCode;
	private String itemName;
	private String commodityCode;
	private int quantity;
	//these are all stored against base rate / default currency
	private BigDecimal vatPercentage = new BigDecimal( 0 );
	private BigDecimal singleItemVatAmount = new BigDecimal( 0 );
	private BigDecimal singleItemDiscountPercentage = new BigDecimal( 0 );
	private BigDecimal singleItemDiscountAmount = new BigDecimal( 0 );
	private BigDecimal cachedNetLinePrice = new BigDecimal( 0 );
	private BigDecimal cachedLineVatAmount = new BigDecimal( 0 );

	@ManyToOne
	@Cascade({CascadeType.ALL})
	@JoinColumn(name="shoppingCart_id", insertable=false, updatable=false)
	private ShoppingCart shoppingCart;

	private Boolean isUpdatable;
	private boolean isCustomisable = false;
	
	public ShoppingCartItem() {
		setEditable(false);
	}

	@Override
	public BigDecimal getSingleItemFinalPrice( boolean includingVat ) {
		return getSingleItemFinalPrice( includingVat, false );
	}

	public BigDecimal getSingleItemFinalPrice( boolean includingVat, boolean convert ) {
		BigDecimal singleItemFinalPrice;
		if( CommonConfiguration.getCommonConfiguration().isVatInclusive() ) {
			singleItemFinalPrice = getSingleItemBasePrice().subtract( getSingleItemDiscountAmount() );

			if( !includingVat ) {
				singleItemFinalPrice = singleItemFinalPrice.subtract( getSingleItemVatAmount() );
			}
		} else {
			singleItemFinalPrice = getSingleItemNetPrice().subtract( getSingleItemDiscountAmount() );

			if( includingVat ) {
				singleItemFinalPrice = singleItemFinalPrice.add( getSingleItemVatAmount() );
			}
		}
		if (convert) {
			return Currency.convert(singleItemFinalPrice, shoppingCart.getCurrencyBaseRate());
		} else {
			return singleItemFinalPrice;
		}
	}
	
	// overidden in other classes
	public void itemPurchased(PaymentGatewayPost paymentGatewayDirectPost ) {
	}

	public void updateVatPercentage() {
		if( shoppingCart.isVatExempt() ) {
			setVatPercentage( new BigDecimal( 0 ) );
		}
	}

	public void updateAllValues() {
		updateSingleItemNetPrice();
		calculateDiscountAmount( false, false );
		// This calls updateCachedValues
		calculateVatAmount( true, true );
	}
	
	public boolean isShoppingCartChangesDisabled() {
		return false;
	}

	public void updateVatCachedValue() {
		calculateVatAmount( true, false );
	}

	public void updateCachedValues(boolean updateCartValues) {
		setCachedNetLinePrice( getLinePrice( false ) );
		BigDecimal vatAmount;
		vatAmount = singleItemVatAmount.multiply( new BigDecimal( getQuantity() ) );
		setCachedLineVatAmount( vatAmount );

		if( updateCartValues ) {
			if( shoppingCart != null ) {
				shoppingCart.updateCachedValues( false );
			}
		}
	}

	public BigDecimal getCachedGrossLinePrice() {
		return getCachedGrossLinePrice(false);
	}

	public BigDecimal getCachedGrossLinePrice(boolean convert) {
		return getCachedNetLinePrice(convert).add( getCachedLineVatAmount(convert) );
	}

	public String getCachedGrossLinePriceString() {
		return getCachedGrossLinePriceString(false);
	}

	public String getCachedGrossLinePriceString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedGrossLinePrice(convert).doubleValue() );
	}

	public String getCachedNetLinePriceString() {
		return getCachedNetLinePriceString(false);
	}

	public String getCachedNetLinePriceString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedNetLinePrice(convert).doubleValue() );
	}

	public String getCachedLinePriceString() {
		return getCachedLinePriceString(false);
	}

	public String getCachedLinePriceString(boolean convert) {
		return FormatUtil.formatTwoDigit( getCachedNetLinePrice(convert).doubleValue() );
	}

	public String getSingleItemFinalPriceString() {
		return getSingleItemFinalPriceString( true );
	}

	public String getSingleItemFinalPriceString( boolean includingVat ) {
		return getSingleItemFinalPriceString( includingVat, false );
	}

	public String getSingleItemFinalPriceString( boolean includingVat, boolean convert ) {
		return FormatUtil.formatTwoDigit( getSingleItemFinalPrice(includingVat, convert).doubleValue() );
	}

	public void calculateVatAmount( boolean updateCachedValues, boolean updateCartCachedValues ) {
		if( CommonConfiguration.getCommonConfiguration().isVatInclusive() ) {
			setSingleItemVatAmount( CommonUtil.getInclusivePercentageAmountAndRound( getSingleItemBasePrice().subtract( getSingleItemDiscountAmount() ), getVatPercentage() ).setScale( 2, RoundingMode.HALF_DOWN ) );
		} else {
			setSingleItemVatAmount( CommonUtil.getPercentageAmountAndRound( getSingleItemNetPrice().subtract( getSingleItemDiscountAmount() ), getVatPercentage() ).setScale( 2, RoundingMode.HALF_DOWN ) );
		}

		if( updateCachedValues ) {
			updateCachedValues( updateCartCachedValues );
		}
	}

	public void calculateDiscountAmount( boolean updateCachedValues, boolean updateCartCachedValues ) {
		setSingleItemDiscountAmount( CommonUtil.getPercentageAmountAndRound( getSingleItemBasePrice(), getSingleItemDiscountPercentage() ).setScale( 2, RoundingMode.HALF_DOWN ) );
		if( updateCachedValues ) {
			updateCachedValues( updateCartCachedValues );
		}
	}

	public void setSingleItemDiscountAndCalculate( BigDecimal singleItemDiscountPercentage ) {
		setSingleItemDiscountPercentage(singleItemDiscountPercentage);
		calculateDiscountAmount( false, false );
	}

	@Override
	public BigDecimal getPaymentSystemGrossLinePrice() {
		return getPaymentSystemGrossLinePrice(false);
	}

	public BigDecimal getPaymentSystemGrossLinePrice(boolean convert) {
		return getCachedGrossLinePrice(convert);
	}

	protected BigDecimal getLinePrice( boolean includingVat ) {
		return getLinePrice( includingVat, false );
	}

	private BigDecimal getLinePrice( boolean includingVat, boolean convert ) {
		return getSingleItemFinalPrice( includingVat, convert ).multiply( new BigDecimal( getQuantity() ) );
	}

	public String getLinePriceString() {
		return getLinePriceString( true );
	}

	public String getLinePriceString( boolean includingVat ) {
		return getLinePriceString( includingVat, false );
	}

	public String getLinePriceString( boolean includingVat, boolean convert ) {
		return FormatUtil.formatTwoDigit( getLinePrice( includingVat, convert ).doubleValue() );
	}

	public void setIsUpdatable(Boolean isUpdatable) {
		this.isUpdatable = isUpdatable;
	}

	public Boolean getIsUpdatable() {
		if( isUpdatable == null ) {
			return true;
		} else {
			return isUpdatable;
		}
	}

	public void setShoppingCart(ShoppingCart shoppingCart) {
		this.shoppingCart = shoppingCart;
	}

	public ShoppingCart getShoppingCart() {
		return shoppingCart;
	}

	public String getSingleItemBasePriceString() {
		return getSingleItemBasePriceString(false);
	}

	public String getSingleItemBasePriceString(boolean convert) {
		return FormatUtil.formatTwoDigit( getSingleItemBasePrice(convert).doubleValue() );
	}

	public void setSingleItemVatAmount(BigDecimal singleItemVatAmount) {
		this.singleItemVatAmount = singleItemVatAmount;
	}

	@Override
	public BigDecimal getSingleItemVatAmount() {
		return getSingleItemVatAmount(false);
	}

	public BigDecimal getSingleItemVatAmount(boolean convert) {
		if (convert) {
			return Currency.convert(singleItemVatAmount, shoppingCart.getCurrencyBaseRate());
		} else {
			return singleItemVatAmount;
		}
	}

	public String getSingleItemVatAmountString() {
		return getSingleItemVatAmountString(false);
	}

	public String getSingleItemVatAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getSingleItemVatAmount(convert).doubleValue() );
	}

	public void setSingleItemDiscountPercentage( BigDecimal singleItemDiscountPercentage) {
		this.singleItemDiscountPercentage = singleItemDiscountPercentage;
	}

	public BigDecimal getSingleItemDiscountPercentage() {
		return getSingleItemDiscountPercentage(false);
	}

	public BigDecimal getSingleItemDiscountPercentage(boolean convert) {
		if (convert) {
			return Currency.convert(singleItemDiscountPercentage, shoppingCart.getCurrencyBaseRate());
		} else {
			return singleItemDiscountPercentage;
		}
	}

	public void setSingleItemDiscountAmount(BigDecimal singleItemDiscountAmount) {
		this.singleItemDiscountAmount = singleItemDiscountAmount;
	}

	@Override
	public BigDecimal getSingleItemDiscountAmount() {
		return getSingleItemDiscountAmount(false);
	}

	public BigDecimal getSingleItemDiscountAmount(boolean convert) {
		if (convert) {
			return Currency.convert(singleItemDiscountAmount, shoppingCart.getCurrencyBaseRate());
		} else {
			return singleItemDiscountAmount;
		}
	}

	public String getSingleItemDiscountAmountString() {
		return getSingleItemDiscountAmountString(false);
	}

	public String getSingleItemDiscountAmountString(boolean convert) {
		return FormatUtil.formatTwoDigit( getSingleItemDiscountAmount(convert).doubleValue() );
	}

	public String getSingleItemNetPriceString() {
		return FormatUtil.formatTwoDigit( getSingleItemNetPrice().doubleValue() );
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	@Override
	public String getItemCode() {
		return itemCode;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	@Override
	public String getItemName() {
		return itemName;
	}

	public void setCachedNetLinePrice(BigDecimal cachedNetLinePrice) {
		this.cachedNetLinePrice = cachedNetLinePrice;
	}

	public BigDecimal getCachedNetLinePrice() {
		return getCachedNetLinePrice(false);
	}

	public BigDecimal getCachedNetLinePrice(boolean convert) {
		if (convert) {
			return Currency.convert(cachedNetLinePrice, shoppingCart.getCurrencyBaseRate());
		} else {
			return cachedNetLinePrice;
		}
	}

	public void setCachedLineVatAmount(BigDecimal cachedLineVatAmount) {
		this.cachedLineVatAmount = cachedLineVatAmount;
	}

	public BigDecimal getCachedLineVatAmount() {
		return getCachedLineVatAmount(false);
	}

	public BigDecimal getCachedLineVatAmount(boolean convert) {
		if (convert) {
			return Currency.convert(cachedLineVatAmount, shoppingCart.getCurrencyBaseRate());
		} else {
			return cachedLineVatAmount;
		}
	}

	public void setSingleItemBasePrice(BigDecimal singleItemBasePrice) {
		this.singleItemBasePrice = singleItemBasePrice;
	}

	@Override
	public BigDecimal getSingleItemBasePrice() {
		return getSingleItemBasePrice(false);
	}

	public BigDecimal getSingleItemBasePrice(boolean convert) {
		if (convert) {
			return Currency.convert(singleItemBasePrice, shoppingCart.getCurrencyBaseRate());
		} else {
			return singleItemBasePrice;
		}
	}

	public void updateSingleItemNetPrice() {
		if( CommonConfiguration.getCommonConfiguration().isVatInclusive() ) {
			setSingleItemNetPrice( getSingleItemBasePrice().subtract( CommonUtil.getInclusivePercentageAmountAndRound( getSingleItemBasePrice(), getVatPercentage() )) );
		} else {
			setSingleItemNetPrice( getSingleItemBasePrice() );
		}
	}

	public void setCustomisable(boolean isCustomisable) {
		this.isCustomisable = isCustomisable;
	}

	public boolean isCustomisable() {
		return isCustomisable;
	}

	public void setCommodityCode(String commodityCode) {
		this.commodityCode = commodityCode;
	}

	public String getCommodityCode() {
		return commodityCode;
	}

	@Override
	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getVatPercentage() {
		return vatPercentage;
	}

	public void setVatPercentage(BigDecimal vatPercentage) {
		this.vatPercentage = vatPercentage;
	}

	public BigDecimal getSingleItemNetPrice() {
		return singleItemNetPrice;
	}

	public void setSingleItemNetPrice(BigDecimal singleItemNetPrice) {
		this.singleItemNetPrice = singleItemNetPrice;
	}

}
