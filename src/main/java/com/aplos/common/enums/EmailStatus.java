package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum EmailStatus implements LabeledEnumInter {
	UNFLAGGED ("Unflagged"),
	FLAGGED ("Flagged"),
	COMPLETE ("Complete");
	
	private String label;
	
	private EmailStatus( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
