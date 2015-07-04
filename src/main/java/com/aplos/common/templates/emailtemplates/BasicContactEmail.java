package com.aplos.common.templates.emailtemplates;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.SourceGeneratedEmailTemplate;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.module.CommonConfiguration.CommonBulkMessageFinderEnum;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class BasicContactEmail extends SourceGeneratedEmailTemplate<BasicContact> {
	private static final long serialVersionUID = 3906905475783389639L;

	public BasicContactEmail() {
	}
	
	public String getDefaultName() {
		return "Basic contact email";
	}

	@Override
	public String getDefaultSubject() {
		return "";
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "basicContact.html" );
	}
	
	@Override
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return Website.getCurrentWebsiteFromTabSession().getBulkMessageFinderMap().get( CommonBulkMessageFinderEnum.BASIC_CONTACT );
	}
	
	@Override
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe,
			BasicContact basicContact) {
		super.addContentJDynamiTeValues(jDynamiTe, basicContact);
		jDynamiTe.setVariable("FIRST_NAME", basicContact.getFirstName() );
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if( currentUser == null ) {
			currentUser = CommonUtil.getAdminUser();
		}
		if( currentUser != null ) {
			jDynamiTe.setVariable("CURRENT_USER_FIRST_NAME", currentUser.getFirstName() );
		}
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.BASIC_CONTACT;
	}

}
