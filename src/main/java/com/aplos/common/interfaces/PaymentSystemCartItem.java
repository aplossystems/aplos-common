package com.aplos.common.interfaces;

import java.math.BigDecimal;

public interface PaymentSystemCartItem {
	public int getQuantity();
	public BigDecimal getSingleItemBasePrice();
	public BigDecimal getSingleItemFinalPrice( boolean includingVat );
	public BigDecimal getPaymentSystemGrossLinePrice();
	public BigDecimal getSingleItemVatAmount();
	public String getItemCode();
	public String getItemName();
	public BigDecimal getSingleItemDiscountAmount();
}
