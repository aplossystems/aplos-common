package com.aplos.common.templates.emailtemplates;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.SourceGeneratedEmailTemplate;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.module.CommonConfiguration.CommonBulkMessageFinderEnum;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

@Entity
//having the word forgotten in the class name makes the discriminator too long
public class SystemPasswordResetEmail extends SourceGeneratedEmailTemplate<SystemUser> {
	private static final long serialVersionUID = 2494307734788615306L;

	public SystemPasswordResetEmail() {
	}
	
	public String getDefaultName() {
		return "System password reset";
	}

	@Override
	public String getDefaultSubject() {
		return "Your New {COMPANY_NAME} Password";
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "systemPasswordReset.html" );
	}
	
	@Override
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return Website.getCurrentWebsiteFromTabSession().getBulkMessageFinderMap().get( CommonBulkMessageFinderEnum.SYSTEM_USER );
	}

	@Override
	public BulkEmailSource getTestSource(SystemUser adminUser) {
		return CommonUtil.getAdminUser();
	}
	
	@Override
	public void addSubjectJDynamiTeValues(JDynamiTe jDynamiTe,
			SystemUser bulkEmailRecipient) {
		super.addSubjectJDynamiTeValues(jDynamiTe, bulkEmailRecipient);
		CompanyDetails companyDetails = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails();
		String companyName = companyDetails.getCompanyName();
		if (companyName == null) {
			companyName = ApplicationUtil.getAplosContextListener().getImplementationModule().getPackageDisplayName();
		}
		jDynamiTe.setVariable( "COMPANY_NAME", companyName );
	}
	
	@Override
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe,
			SystemUser systemUser) {
		super.addContentJDynamiTeValues(jDynamiTe, systemUser);
		jDynamiTe.setVariable("FIRST_NAME", systemUser.getFullName() );
		CompanyDetails companyDetails = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails();
		String companyName = companyDetails.getCompanyName();
		if (companyName == null) {
			companyName = ApplicationUtil.getAplosContextListener().getImplementationModule().getPackageDisplayName();
		}
		jDynamiTe.setVariable("COMPANY_NAME", CommonUtil.getStringOrEmpty(companyName) );
		if( companyDetails.getLogoDetails() != null ) {
			jDynamiTe.setVariable("COMPANY_LOGO", CommonUtil.getStringOrEmpty(companyDetails.getLogoDetails().getFilename()) );
		}
		jDynamiTe.setVariable("COMPANY_WEBSITE", CommonUtil.getStringOrEmpty(companyDetails.getWeb()) );
		jDynamiTe.setVariable("PASSWORD", systemUser.getPassword() );
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.RESET_PASSWORD;
	}

}
