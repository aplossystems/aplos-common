package com.aplos.common.templates.emailtemplates;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.ExternalBackingPageUrl;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.backingpage.ForgottenPasswordResetPage;
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
import com.aplos.common.utils.FormatUtil;

@Entity
public class ForgottenSystemPasswordEmail extends SourceGeneratedEmailTemplate<SystemUser> {

	private static final long serialVersionUID = 7318737838603092489L;

	public ForgottenSystemPasswordEmail() {
	}
	
	public String getDefaultName() {
		return "Forgotten password reset request";
	}

	@Override
	public String getDefaultSubject() {
		return "{COMPANY_NAME} password reset request";
	}
	
	@Override
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return Website.getCurrentWebsiteFromTabSession().getBulkMessageFinderMap().get( CommonBulkMessageFinderEnum.SYSTEM_USER );
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "forgottenSystemPassword.html" );
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
		jDynamiTe.setVariable("COMPANY_NAME", companyName );
		String logoFilename = ""; 
		if (companyDetails.getLogoDetails() != null && CommonUtil.isNullOrEmpty(companyDetails.getLogoDetails().getFilename())) {
			logoFilename = "";
		}
		jDynamiTe.setVariable("COMPANY_LOGO", logoFilename );
		jDynamiTe.setVariable("COMPANY_EMAIL", CommonUtil.getStringOrEmpty(companyDetails.getAddress().getEmailAddress()) );
		jDynamiTe.setVariable("COMPANY_PHONE", CommonUtil.getStringOrEmpty(companyDetails.getAddress().getPhone()) );
		ExternalBackingPageUrl externalBackingPageUrl = new ExternalBackingPageUrl( ForgottenPasswordResetPage.class );
		externalBackingPageUrl.addQueryParameter( "passwordResetCode", systemUser.getResetCode() );
		jDynamiTe.setVariable("RESET_PASSWORD_URL", externalBackingPageUrl.toString() );
		jDynamiTe.setVariable("RESET_DATE", FormatUtil.formatDate( systemUser.getResetDate() ) );
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.FORGOTTEN_SYSTEM_PASSWORD;
	}

}
