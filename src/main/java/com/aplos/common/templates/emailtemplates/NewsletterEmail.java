package com.aplos.common.templates.emailtemplates;

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
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.module.CommonConfiguration.CommonBulkMessageFinderEnum;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

@Entity
public class NewsletterEmail extends SourceGeneratedEmailTemplate<BulkSubscriberSource> {
	private static final long serialVersionUID = -2175924209284716728L;

	public NewsletterEmail() {
	}
	
	public String getDefaultName() {
		return "Newsletter";
	}

	@Override
	public String getDefaultSubject() {
		return "{COMPANY_NAME} newsletter";
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "newsletter.html" );
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
	public boolean removeUnsubscribedByDefault() {
		return true;
	}
	
	@Override
	public void addSubjectJDynamiTeValues(JDynamiTe jDynamiTe,
			BulkSubscriberSource bulkEmailRecipient) {
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
			BulkSubscriberSource subscriberSource) {
		super.addContentJDynamiTeValues(jDynamiTe, subscriberSource);
		jDynamiTe.setVariable("FIRST_NAME", CommonUtil.firstLetterToUpperCase( CommonUtil.getStringOrEmpty( subscriberSource.getFirstName() ) ) );

		if( !CommonUtil.isNullOrEmpty( subscriberSource.getFirstName() ) ) {
			jDynamiTe.setVariable("FOR_FIRST_NAME", "for " + CommonUtil.firstLetterToUpperCase( CommonUtil.getStringOrEmpty( subscriberSource.getFirstName() ) ) );
		}
		
		CompanyDetails companyDetails = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails();
		String companyName = companyDetails.getCompanyName();
		if (companyName == null) {
			companyName = ApplicationUtil.getAplosContextListener().getImplementationModule().getPackageDisplayName();
		}
		jDynamiTe.setVariable("COMPANY_NAME", CommonUtil.getStringOrEmpty(companyName) );
		if( companyDetails.getLogoDetails() != null ) {
			jDynamiTe.setVariable("COMPANY_LOGO", CommonUtil.getStringOrEmpty(companyDetails.getLogoDetails().getFilename()) );
		}
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.NEWSLETTER;
	}

}
