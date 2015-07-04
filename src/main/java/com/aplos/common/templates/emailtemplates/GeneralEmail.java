package com.aplos.common.templates.emailtemplates;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.SourceGeneratedEmailTemplate;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.module.CommonConfiguration.CommonBulkMessageFinderEnum;

@Entity
public class GeneralEmail extends SourceGeneratedEmailTemplate<BasicContact> {
	private static final long serialVersionUID = 3906905475783389639L;

	public GeneralEmail() {
	}
	
	public String getDefaultName() {
		return "General email";
	}

	@Override
	public String getDefaultSubject() {
		return "";
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "generalEmail.html" );
	}
	
	@Override
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return Website.getCurrentWebsiteFromTabSession().getBulkMessageFinderMap().get( CommonBulkMessageFinderEnum.BASIC_CONTACT );
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.GENERAL_EMAIL;
	}

}
