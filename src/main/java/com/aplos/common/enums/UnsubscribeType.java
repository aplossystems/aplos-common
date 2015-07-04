package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum UnsubscribeType implements LabeledEnumInter {
	USER_REQUESTED ("User requested"),
	USER_NOT_FOUND ("User not found"),
	MAILBOX_FULL ("Mailbox full"),
	MARKED_AS_SPAM ("Marked as spam"),
	RELAY_NOT_PERMITTED ("Relay not permitted"),
	SERVER_FAILED_TO_RESPOND ("Server failed to respond");
	
	private String label;
	
	private UnsubscribeType( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
