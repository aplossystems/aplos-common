package com.aplos.common.templates.smstemplates;

import java.io.IOException;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.communication.SourceGeneratedSmsTemplate;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.CustomSmsTemplateInter;

@Entity
public class CustomSmsTemplate extends SourceGeneratedSmsTemplate<BulkSmsSource> implements CustomSmsTemplateInter {
	private static final long serialVersionUID = 5130294032943792305L;
	private Class<? extends BulkSmsSource> sourceType;
	
	public CustomSmsTemplate() {
		super();
		setUsingDefaultContent(false);
		setDeletable(true);
	}
	
	@Override
	public String compileContent(BulkSmsSource smsRecipient, String content)
			throws ClassCastException, IOException {
		return content;
	}

	public Class<? extends BulkSmsSource> getSourceType() {
		return sourceType;
	}

	public void setSourceType(Class<? extends BulkSmsSource> sourceType) {
		this.sourceType = sourceType;
	}
}
