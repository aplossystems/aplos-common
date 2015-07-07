package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum EmailFromAddressType implements LabeledEnumInter {
	SYSTEM_DEFAULT ("System default"),
	CURRENT_USER ("Current user"),
	COMPANY_DETAILS ("Company details"),
	OTHER ("Other");
	
	private String label;
	
	private EmailFromAddressType( String label ) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	};
}
