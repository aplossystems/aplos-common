package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum CombinedResourceStatus implements LabeledEnumInter {
	ENABLED ("Enabled"),
	DISABLED ("Disabled"),
	FRONT_END_ONLY ("Fronted only"),
	BACK_END_ONLY ("Backend only");
	
	private String label;
	
	private CombinedResourceStatus( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
