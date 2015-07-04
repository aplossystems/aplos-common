package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum ContactTitle implements LabeledEnumInter {
	MR ("Mr"),
	MRS ("Mrs"),
	MISS ("Miss"),
	MS ("Ms"),
	DR ("Dr");
	
	private String label;
	
	private ContactTitle( String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
