package com.aplos.common.enums;

import com.aplos.common.interfaces.BundleKey;

//PLEASE KEEP KEYS IN ALPHABETICAL ORDER
public enum CommonBundleKey implements BundleKey {
	ADDRESS ("address"),
	BREADCRUMBS_HISTORY ("History"),
	BREADCRUMBS_NO_HISTORY ("No History"),
	CITY ("city"),
	COMPANY ("company"),
	CONTACT_FIRST_NAME ("contact first name"),
	CONTACT_SURNAME ("contact surname"),
	CONTACT_TITLE ("contact title"),
	COUNTY_OR_STATE ("county/state"),
	COUNTRY ("country"),
	EMAIL ("email"),
	FAX ("fax"),
	FIRST_NAME ("first name"),
	GOOD ("good"),
	MOBILE ("mobile"),
	NAME ("name"),
	NOW_SIGNED_IN ("You are now signed in"),
	PAYMENT ("payment"),
	PHONE ("phone"),
	PHONE_1 ("phone 1"),
	PHONE_2 ("phone 2"),
	POSTCODE ("postcode"),
	TOWN_OR_CITY ("town/city"),
	REGISTER ("register"),
	SAVED_SUCCESSFULLY ( "Saved successfully" ),
	SECOND_PHONE ("second phone"),
	SIGN_IN ("sign in"),
	STATE ("state"),
	STRONG ("strong"),
	SURNAME ("surname"),
	TBA ("TBA") /* to be arranged, used in checkout */,
	WEAK ("weak");

	private String defaultValue;

	private CommonBundleKey( String defaultValue ) {
		this.defaultValue = defaultValue;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}
}
