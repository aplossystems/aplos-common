package com.aplos.common.enums;

import com.aplos.common.beans.communication.EmailTemplate;

public interface EmailTemplateEnum {
	public Class<? extends EmailTemplate> getEmailTemplateClass();
	public void setActive(boolean isActive);
	public boolean isActive();
}
