package com.aplos.common.enums;
public enum CheckoutPageEntry {
	SIGN_IN_OR_SIGN_UP,
	SIGN_IN,
	SIGN_UP,
	SHIPPING,
	BILLING,
	PAYMENT,
	CONFIRM,
	SUCCESS;

	public String getName() {
		return name();
	}
}
