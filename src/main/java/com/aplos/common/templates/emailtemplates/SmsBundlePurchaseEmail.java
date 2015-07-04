package com.aplos.common.templates.emailtemplates;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.beans.communication.SmsBundlePurchase;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.interfaces.BulkEmailSource;

@Entity
public class SmsBundlePurchaseEmail extends EmailTemplate<BulkEmailSource,SmsBundlePurchase> {
	private static final long serialVersionUID = -2175924209284716728L;

	public SmsBundlePurchaseEmail() {
	}
	
	public String getDefaultName() {
		return "SMS bundle purchase";
	}

	@Override
	public String getDefaultSubject() {
		return "SMS bundle purchase";
	}

	@Override
	public String getDefaultContent() {
		return loadBodyFromFile( "smsBundlePurchase.html" );
	}
	
	@Override
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe,
			BulkEmailSource bulkEmailRecipient, SmsBundlePurchase smsBundlePurchase, SingleEmailRecord singleEmailRecord) {
		super.addContentJDynamiTeValues(jDynamiTe, bulkEmailRecipient, smsBundlePurchase, singleEmailRecord);
		jDynamiTe.setVariable( "FIRST_NAME", bulkEmailRecipient.getFirstName() );
		jDynamiTe.setVariable( "SMS_BUNDLE_SIZE", String.valueOf( smsBundlePurchase.getSmsAccount().getAutoRebuyBundleSize() ) );
		jDynamiTe.setVariable( "COMPANY_NAME", "Aplos Systems" );
	}

	@Override
	public EmailTemplateEnum getEmailTemplateEnum() {
		return CommonEmailTemplateEnum.SMS_BUNDLE_PURCHASE;
	}

}
