package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum VatOffset implements LabeledEnumInter {
	INCLUSIVE ("Incusive"),
	EXCLUSIVE ("Exclusive");

	private String label;

	private VatOffset( String label ) {
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}
}
