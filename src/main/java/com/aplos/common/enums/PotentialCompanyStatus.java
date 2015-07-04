package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum PotentialCompanyStatus implements LabeledEnumInter {
	UNCONTACTED ("Uncontacted"),
	AWAITING_RESPONSE ("Awaiting response"),
	NOT_INTERESTED ("Not interested"),
	INTERESTED ("Interested"),
	NOT_APPLICABLE ("Not applicable"),
	NOT_CURRENTLY_APPLICABLE ("Not currently applicable"),
	ON_SYSTEM ("On system"),
	CALL_BACK ("Call back"),
	COMPANY_CLOSED ("Company Closed"),
	PHONE_NUMBER_NOT_WORKING ("Phone number not working"),
	NOT_RESPONDING ("Not responding to multiple calls"),
	CONVERTED ("Converted");
	
	private String label;
	
	private PotentialCompanyStatus( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
