package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum VatExemption implements LabeledEnumInter {
	EXEMPT ( "Exempt"),
	EU_EXEMPT ( "EU exempt" ),
	NOT_EXEMPT ( "Not exempt" );

	private String label;

	private VatExemption( String label ) {
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}
}
