package com.aplos.common.enums;


public enum CartAbandonmentIssue {

	UNKNOWN("No issues recorded","No issues were recorded."),
	@Deprecated
	SIGN_IN_ISSUE("Sign-in issues","The customer did not proceed beyond the sign in screen. It may be they have forgotten their sign in details or tried to register a new account when they have registered already."),
	INCORRECT_PASSWORD("Incorrect password","The customer entered an incorrect password."),
	INVALID_EMAIL_ADDRESS("Invalid email address","The customer entered an invalid email address."),
	NO_EMAIL_ADDRESS("Email address not entered","The customer did not enter an email address."),
	NO_PASSWORD("Password not entered","The customer did not enter an password."),
	PASSWORD_MISMATCH("Passwords don't match","When asked to confirm their password the user entered two different passwords."),
	LOG_IN_NEEDED("Log in needed","The customer was not logged in."),
	PRODUCT_OUT_OF_STOCK("Product out of stock","The customer tried to add an item to their cart but it was out of stock."),
	EMAIL_ADDRESS_NOT_ENTERED("Email address not entered", "An email address wasn't entered on sign in" ),
	EMAIL_ADDRESS_TAKEN("Email address taken", "The customer was trying to create a new account with an existing email address" ),
	@Deprecated
	ACCOUNT_DETAILS_ISSUE("Issue registering new account or updating details","This may have been a user error such as with form validation. The user did not proceed beyond the shipping and billing entry screens."),
	@Deprecated
	PAYMENT_DETAILS_ISSUE("Issue with payment details entry","This may have been a user error such as with form validation, the user may not have held any applicable payment methods. The user did not proceed beyond the payment details screen."),
	CREDIT_CARD_NUMBER_INCORRECT("Credit card number incorrect","The user entered a credit card number that was incorrect"),
	CREDIT_CARD_NUMBER_INCORRECT_LENGTH("Credit card number incorrect length","The user entered a credit card number that was an incorrect length"),
	CVV_INCORRECT("Cvv incorrect","The user entered a cvv number that was incorrect"),
	CVV_INCORRECT_LENGTH("Cvv incorrect length","The user entered a cvv number that was an incorrect length"),
	CONFIRMATION_ISSUE("Issue with order confirmation","The user may not have been happy with the displayed details."),
	ITEMS_MOVED_TO_WISHLIST("Items moved to wishlist","The creation of an order may have been accidental or the customer may have decided to wait until another date to place the order as the items were moved onto their wishlist."),
	@Deprecated
	PAYMENT_ISSUE("Issues occured during payment","This may mean payment was declined."),
	PAYMENT_ALREADY_MADE("Payment already made","A payment had already been made on this order."),
	INSUFFICIENT_STOCK_WARNING("Insufficient stock at confirmation","By the time the user reached confirmation, some of the items in their basket were unavailable."),
	SHIPPING_METHOD("No shipping selected","The user tried to proceed to checkout without selecting a shipping method."),
	BASKET_EMPTY("Basket was empty","The user tried to proceed to checkout with an empty basket."),
	NO_CART_IN_SESSION("The cart was not in session","The cart was not in session."),
	PROMOTION_NOT_RECOGNISED("Promotion not recognised","The promotional voucher the user attempted to use was not recognised by the system"),
	POSSIBLE_SYSTEM_ERROR("Possible System Error","The basket was in an unexpected state, the system or client browser may have crashed or the user may have tried going directly to a later stage in the checkout process."),
	UNDERAGE("The user was underage", "The signup procedure includes a check to ensure the customer was at least 16 years old and/or had their parents permission to make purchases. The customer indicated they were underage and did not have permission to proceed."),
	CUSTOMER_ACCOUNT_DEACTIVATED("The customers account was deactivated","The customer attempted to sign in with an email address for an existing account, but the linked account was marked as deleted, either by you or another process.");

	String displayName = "";
	String description = "";
	private CartAbandonmentIssue(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}
}
