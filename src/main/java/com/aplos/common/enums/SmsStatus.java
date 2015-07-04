package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum SmsStatus implements LabeledEnumInter {
	UNSENT ("Unsent"),
	RETRYING ("Retrying"),
	SENT_SUCCESFULLY ("Sent successfully"),
	CANCELLED ("Cancelled"),
	SENDING ("Sending");
	
	private String label;
	
	private SmsStatus( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
