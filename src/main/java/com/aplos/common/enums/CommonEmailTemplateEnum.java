package com.aplos.common.enums;

import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.templates.emailtemplates.BasicContactEmail;
import com.aplos.common.templates.emailtemplates.ForgottenSystemPasswordEmail;
import com.aplos.common.templates.emailtemplates.GeneralEmail;
import com.aplos.common.templates.emailtemplates.NewsletterEmail;
import com.aplos.common.templates.emailtemplates.SmsBundlePurchaseEmail;
import com.aplos.common.templates.emailtemplates.SystemPasswordResetEmail;

public enum CommonEmailTemplateEnum implements EmailTemplateEnum {
	FORGOTTEN_SYSTEM_PASSWORD ( ForgottenSystemPasswordEmail.class ),
	RESET_PASSWORD ( SystemPasswordResetEmail.class ),
	NEWSLETTER ( NewsletterEmail.class ),
	SMS_BUNDLE_PURCHASE ( SmsBundlePurchaseEmail.class ),
	BASIC_CONTACT ( BasicContactEmail.class ),
	GENERAL_EMAIL ( GeneralEmail.class );

	private Class<? extends EmailTemplate> emailTemplateClass;
	boolean isActive = true;

	private CommonEmailTemplateEnum( Class<? extends EmailTemplate> emailTemplateClass ) {
		this.emailTemplateClass = emailTemplateClass;
	}

	@Override
	public Class<? extends EmailTemplate> getEmailTemplateClass() {
		return emailTemplateClass;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
}