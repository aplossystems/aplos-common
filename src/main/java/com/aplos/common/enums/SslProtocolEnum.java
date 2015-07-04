package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum SslProtocolEnum implements LabeledEnumInter {
	FORCE_SSL ( "Force SSL" ),
	FORCE_HTTP ( "Force HTTP" ),
	DONT_CHANGE ( "Dont change" ),
	SYSTEM_DEFAULT ( "System default" );
	
	String label;
	
	private SslProtocolEnum( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
