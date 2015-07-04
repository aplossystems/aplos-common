package com.aplos.common.enums;


public enum WebServiceCallTypes {

	FETCH_PROJECT_LIST,
	CREATE_PROJECT,
	AUTHENTICATE_LOGIN,
	SEND_SMS_MESSAGE;

	String directoryPath;

	private WebServiceCallTypes() {}

}
