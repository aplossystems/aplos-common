package com.aplos.common.enums;

import com.aplos.common.LabeledEnumInter;

public enum EmailFilterStatus implements LabeledEnumInter {
	UNREAD ( "Unread", "bean.emailReadDate = null AND bean.emailStatus != " + EmailStatus.COMPLETE.ordinal() ),
	READ ( "Read", "bean.emailReadDate != null AND bean.emailStatus != " + EmailStatus.COMPLETE.ordinal() ),
	FLAGGED ( "Flagged", "bean.emailStatus = " + EmailStatus.FLAGGED.ordinal() ),
	UNFLAGGED ( "Unflagged", "bean.emailStatus = " + EmailStatus.UNFLAGGED.ordinal() ),
	COMPLETE ( "Complete", "bean.emailStatus = " + EmailStatus.COMPLETE.ordinal() ),
	NOT_COMPLETE ( "Not Complete", "bean.emailStatus != " + EmailStatus.COMPLETE.ordinal() );
	
	private String whereCriteria;
	private String label;
	
	private EmailFilterStatus( String label, String whereCriteria ) {
		this.label = label;
		this.whereCriteria = whereCriteria;
	}
	
	public String getWhereCritieria() {
		return whereCriteria;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
}
