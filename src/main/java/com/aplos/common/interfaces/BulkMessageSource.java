package com.aplos.common.interfaces;


public interface BulkMessageSource extends SmsGenerator, EmailGenerator {
	public String getSourceUniqueDisplayName(); //used in auto-completes
	public Long getMessageSourceId();
}
