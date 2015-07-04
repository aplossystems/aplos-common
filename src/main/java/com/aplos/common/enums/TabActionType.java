package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum TabActionType implements LabeledEnumInter {
	BACKINGPAGE ( "Backing Page (Select a page below)" ),
	OUTPUT_LINK ( "External Page" ),
	COMMAND_LINK ( "Action based redirection" );
	
	private String label;
	
	private TabActionType( String label ) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
