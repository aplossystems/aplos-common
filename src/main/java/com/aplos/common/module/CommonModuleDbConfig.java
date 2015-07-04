package com.aplos.common.module;

import com.aplos.common.FacebookPage;
import com.aplos.common.beans.Address;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.AplosTranslation;
import com.aplos.common.beans.AplosWorkingDirectory;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.BasicContactTag;
import com.aplos.common.beans.ColumnState;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.CountryArea;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.CreditCardType;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.DateRange;
import com.aplos.common.beans.DynamicBundleEntry;
import com.aplos.common.beans.DynamicBundleLanguage;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.InternationalNumber;
import com.aplos.common.beans.MenuWizard;
import com.aplos.common.beans.PageRequest;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.PostalZone;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.beans.ShoppingCartItem;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.beans.SubscriberReferrer;
import com.aplos.common.beans.SubscriptionChannel;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.cardsave.CardSaveGateway;
import com.aplos.common.beans.cardsave.directintegration.CardSaveDirectPost;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BasicEmailFolder;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.DeletableEmailAddress;
import com.aplos.common.beans.communication.EmailFrame;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.HqlMessageSourceFinder;
import com.aplos.common.beans.communication.InboundSms;
import com.aplos.common.beans.communication.KeyedBulkEmailSource;
import com.aplos.common.beans.communication.KeyedBulkSmsSource;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.beans.communication.MailShotType;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.beans.communication.SingleSmsRecord;
import com.aplos.common.beans.communication.SmsAccount;
import com.aplos.common.beans.communication.SmsBundlePurchase;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.beans.communication.SmsMessagePart;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.beans.communication.SourceGeneratedSmsTemplate;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.PotentialCompanyCategory;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.beans.marketing.SalesCallTask;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.beans.paypal.directintegration.PayPalDirectPost;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.beans.sagepay.directintegration.SagePayDirectPost;
import com.aplos.common.beans.sagepay.serverintegration.SagePayServerPost;
import com.aplos.common.beans.translations.CountryTranslation;
import com.aplos.common.beans.translations.CurrencyTranslation;
import com.aplos.common.beans.translations.DynamicBundleEntryTranslation;
import com.aplos.common.beans.translations.SystemUserTranslation;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.CardSaveOrder;
import com.aplos.common.interfaces.CustomSmsTemplateInter;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.interfaces.KeyedBulkEmailSourceInter;
import com.aplos.common.interfaces.KeyedBulkSmsSourceInter;
import com.aplos.common.interfaces.OnlinePaymentConfigurationDetails;
import com.aplos.common.interfaces.OnlinePaymentOrder;
import com.aplos.common.interfaces.PayPalOrder;
import com.aplos.common.interfaces.SagePayOrder;
import com.aplos.common.interfaces.SmsGenerator;
import com.aplos.common.interfaces.SmsMessageOwner;
import com.aplos.common.persistence.PersistentApplication;
import com.aplos.common.scheduledjobs.ArchiverJob;
import com.aplos.common.scheduledjobs.AutomaticAplosEmailJob;
import com.aplos.common.scheduledjobs.DeletableEmailAddressJob;
import com.aplos.common.scheduledjobs.IncomingAplosEmailJob;
import com.aplos.common.scheduledjobs.SmsBundleActivationJob;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.templates.emailtemplates.BasicContactEmail;
import com.aplos.common.templates.emailtemplates.ForgottenSystemPasswordEmail;
import com.aplos.common.templates.emailtemplates.GeneralEmail;
import com.aplos.common.templates.emailtemplates.NewsletterEmail;
import com.aplos.common.templates.emailtemplates.SmsBundlePurchaseEmail;
import com.aplos.common.templates.emailtemplates.SystemPasswordResetEmail;
import com.aplos.common.templates.smstemplates.CustomSmsTemplate;
import com.aplos.common.utils.UserLevelUtil;

public class CommonModuleDbConfig extends ModuleDbConfig {
	private boolean isSagePayServerUsed = false;
	private boolean isSagePayDirectUsed = false;
	private boolean isCardSaveServerUsed = false;
	private boolean isCardSaveDirectUsed = false;
	private boolean isPayPalDirectUsed = false;
	private boolean isCountryAreaUsed = false;
	private boolean isPaymentSystemUsed = false;
	private boolean isCurrencyUsed = false;
	private boolean isSmsUsed = false;

	public CommonModuleDbConfig( AplosModuleImpl aplosModule ) {
		super(aplosModule);
	}

	@Override
	public void addAnnotatedClass(PersistentApplication persistentApplication) {
		persistentApplication.addPersistentClass( AplosBean.class, true );
		persistentApplication.addPersistentClass( TabClass.class, true );
		persistentApplication.addPersistentClass( TabPanel.class, true );
		persistentApplication.addPersistentClass( MenuTab.class, true );

    	persistentApplication.addPersistentClass( CommonConfiguration.class, true );
    	persistentApplication.addPersistentClass( SystemUser.class, true );
    	persistentApplication.addPersistentClass( DateRange.class, true );
    	persistentApplication.addPersistentClass( UserLevel.class, true );
    	persistentApplication.addPersistentClass( Address.class, true );
    	persistentApplication.addPersistentClass( Country.class, true );
    	persistentApplication.addPersistentClass( CompanyDetails.class, true );
    	persistentApplication.addPersistentClass( Subscriber.class, true );
		persistentApplication.addPersistentClass( AplosTranslation.class, true );
		persistentApplication.addPersistentClass( Website.class, true );
    	persistentApplication.addPersistentClass( EmailTemplate.class, true );
		persistentApplication.addPersistentClass( EmailFrame.class, true );
		persistentApplication.addPersistentClass( BasicContact.class, true );
		persistentApplication.addPersistentClass( BasicContactTag.class, true );
		persistentApplication.addPersistentClass( InboundSms.class, true );
		persistentApplication.addPersistentClass( FacebookPage.class, true );
		persistentApplication.addPersistentClass( PotentialCompany.class, true );
		persistentApplication.addPersistentClass( PotentialCompanyCategory.class, true );
		persistentApplication.addPersistentClass( PotentialCompanyInteraction.class, true );
		persistentApplication.addPersistentClass( SingleEmailRecord.class, true );
		persistentApplication.addPersistentClass( SingleSmsRecord.class, true );
		persistentApplication.addPersistentClass( SalesCallTask.class, true );

		persistentApplication.addPersistentClass( AplosEmail.class, true );
		persistentApplication.addPersistentClass( BasicEmailFolder.class, true );
    	persistentApplication.addPersistentClass( CreatedPrintTemplate.class, true );
    	//persistentApplication.addPersistentClass( DynamicBundle.class, true );
    	persistentApplication.addPersistentClass( DynamicBundleEntry.class, true );
    	persistentApplication.addPersistentClass( DynamicBundleLanguage.class, true );
    	persistentApplication.addPersistentClass( DynamicBundleEntryTranslation.class, true );
    	persistentApplication.addPersistentClass( CreatedPrintTemplate.class, true );
    	persistentApplication.addPersistentClass( SubscriptionChannel.class, true );

    	persistentApplication.addPersistentClass( ForgottenSystemPasswordEmail.class, true );
    	persistentApplication.addPersistentClass( SystemPasswordResetEmail.class, true );
    	persistentApplication.addPersistentClass( NewsletterEmail.class, true );
    	persistentApplication.addPersistentClass( GeneralEmail.class, true );

    	persistentApplication.addPersistentClass( BulkMessageSourceGroup.class, true );
    	persistentApplication.addPersistentClass( HqlMessageSourceFinder.class, true );
    	persistentApplication.addPersistentClass( UserLevelUtil.class, true );

    	persistentApplication.addPersistentClass( DataTableState.class, true );
    	persistentApplication.addPersistentClass( ColumnState.class, true );
    	persistentApplication.addPersistentClass( MailServerSettings.class, true );
    	persistentApplication.addPersistentClass( BasicBulkMessageFinder.class, true );
    	
    	persistentApplication.addPersistentClass( KeyedBulkEmailSource.class, true );
    	persistentApplication.addPersistentClass( AplosWorkingDirectory.class, true );
    	persistentApplication.addPersistentClass( FileDetails.class, true );
		persistentApplication.addPersistentClass( InternationalNumber.class, true );

    	persistentApplication.addPersistentClass( SmsBundlePurchaseEmail.class, true );
    	persistentApplication.addPersistentClass( BasicContactEmail.class, true );
    	persistentApplication.addPersistentClass( PageRequest.class, true );
    	persistentApplication.addPersistentClass( DeletableEmailAddress.class, true );

    	persistentApplication.addPersistentClass( IncomingAplosEmailJob.class, true );
    	persistentApplication.addPersistentClass( DeletableEmailAddressJob.class, true );
    	persistentApplication.addPersistentClass( AutomaticAplosEmailJob.class, true );
    	persistentApplication.addPersistentClass( SmsBundleActivationJob.class, true );
    	persistentApplication.addPersistentClass( ArchiverJob.class, true );
    	
    	// interfaces

    	persistentApplication.addPersistentClass( BulkMessageSource.class );
    	persistentApplication.addPersistentClass( PrintTemplate.class );
    	persistentApplication.addPersistentClass( KeyedBulkEmailSourceInter.class );
    	persistentApplication.addPersistentClass( CardSaveOrder.class );
    	persistentApplication.addPersistentClass( PayPalOrder.class );
    	persistentApplication.addPersistentClass( SagePayOrder.class );
    	persistentApplication.addPersistentClass( BulkEmailSource.class );
    	persistentApplication.addPersistentClass( BulkSmsSource.class );
    	persistentApplication.addPersistentClass( FileDetailsOwnerInter.class );
    	persistentApplication.addPersistentClass( EmailFolder.class );

		// The classes below will only be added to the application if requested
    	
    	persistentApplication.addPersistentClass( CardSaveConfigurationDetails.class );
    	persistentApplication.addPersistentClass( OnlinePaymentConfigurationDetails.class );
    	persistentApplication.addPersistentClass( PaymentGatewayPost.class );
    	persistentApplication.addPersistentClass( CardSaveGateway.class );
    	persistentApplication.addPersistentClass( CardSaveDirectPost.class );

    	persistentApplication.addPersistentClass( SagePayConfigurationDetails.class );
    	persistentApplication.addPersistentClass( SagePayServerPost.class );
    	persistentApplication.addPersistentClass( SagePayDirectPost.class );
    	persistentApplication.addPersistentClass( PayPalConfigurationDetails.class );
    	persistentApplication.addPersistentClass( PayPalDirectPost.class );
    	persistentApplication.addPersistentClass( CountryArea.class );
    	persistentApplication.addPersistentClass( CreditCardDetails.class );
    	persistentApplication.addPersistentClass( CreditCardType.class );
    	persistentApplication.addPersistentClass( ShoppingCartItem.class );
    	persistentApplication.addPersistentClass( ShoppingCart.class );
    	persistentApplication.addPersistentClass( Currency.class );
    	persistentApplication.addPersistentClass( CurrencyTranslation.class );
    	persistentApplication.addPersistentClass( CountryTranslation.class );
    	persistentApplication.addPersistentClass( SystemUserTranslation.class );
    	persistentApplication.addPersistentClass( PostalZone.class );
    	persistentApplication.addPersistentClass( VatType.class );
    	persistentApplication.addPersistentClass( SubscriberReferrer.class );
    	persistentApplication.addPersistentClass( MenuWizard.class );
    	persistentApplication.addPersistentClass( MailShotType.class );
    	

    	persistentApplication.addPersistentClass( SmsAccount.class );
    	persistentApplication.addPersistentClass( SmsMessage.class );
    	persistentApplication.addPersistentClass( SmsMessagePart.class );
    	persistentApplication.addPersistentClass( SmsBundlePurchase.class );
    	persistentApplication.addPersistentClass( SmsTemplate.class );
    	persistentApplication.addPersistentClass( SourceGeneratedSmsTemplate.class );
    	persistentApplication.addPersistentClass( KeyedBulkSmsSource.class );
    	persistentApplication.addPersistentClass( CustomSmsTemplate.class );

    	if( isSagePayDirectUsed() || isSagePayServerUsed() ) {
	    	persistentApplication.includePersistentClass( SagePayConfigurationDetails.class );

			if( isSagePayServerUsed() ) {
		    	persistentApplication.includePersistentClass( SagePayServerPost.class );
			}
			if( isSagePayDirectUsed() ) {
				persistentApplication.includePersistentClass( SagePayDirectPost.class );
			}
			setPaymentSystemUsed( true );
		}

    	if( isCardSaveDirectUsed() || isCardSaveServerUsed() ) {
    		
    		persistentApplication.includePersistentClass( CardSaveConfigurationDetails.class );

			if( isCardSaveServerUsed() ) {
		    	persistentApplication.includePersistentClass( CardSaveGateway.class );
			}
			if( isCardSaveDirectUsed() ) {
				persistentApplication.includePersistentClass( CardSaveDirectPost.class );
				persistentApplication.includePersistentClass( CardSaveGateway.class );
			}
			
			setPaymentSystemUsed( true );
		}
    	
    	if( isPayPalDirectUsed() ) {
	    	persistentApplication.includePersistentClass( PayPalConfigurationDetails.class );
	    	persistentApplication.includePersistentClass( PayPalDirectPost.class );
	    	//persistentApplication.includePersistentClass( PayPalCartItem.class );
			setPaymentSystemUsed( true );
		}

		if( isCountryAreaUsed() ) {
	    	persistentApplication.includePersistentClass( CountryArea.class );
		}

		if( isPaymentSystemUsed() ) {
	    	persistentApplication.includePersistentClass( CreditCardDetails.class );
	    	persistentApplication.includePersistentClass( CreditCardType.class );
	    	persistentApplication.includePersistentClass( ShoppingCartItem.class );
	    	persistentApplication.includePersistentClass( ShoppingCart.class );
	    	setCurrencyUsed( true );
		}

		if( isCurrencyUsed() ) {
			persistentApplication.includePersistentClass( Currency.class );
		}

		if( ((CommonModule) getAplosModule()).isInternationalizedApplication() ) {
			persistentApplication.includePersistentClass( CurrencyTranslation.class );
	    	persistentApplication.includePersistentClass( CountryTranslation.class );
			persistentApplication.includePersistentClass( SystemUserTranslation.class );
		}
		
		if( isSmsUsed() ) {
	    	persistentApplication.includePersistentClass( SmsAccount.class );
	    	persistentApplication.includePersistentClass( SmsMessage.class );
	    	persistentApplication.includePersistentClass( SmsMessagePart.class );
	    	persistentApplication.includePersistentClass( SmsBundlePurchase.class );
	    	persistentApplication.includePersistentClass( SmsTemplate.class );
	    	persistentApplication.includePersistentClass( SourceGeneratedSmsTemplate.class );
	    	persistentApplication.includePersistentClass( KeyedBulkSmsSource.class );
	    	persistentApplication.includePersistentClass( CustomSmsTemplate.class );
		}
		
    	// interfaces

    	persistentApplication.addPersistentClass( OnlinePaymentOrder.class );
    	persistentApplication.addPersistentClass( EmailGenerator.class );
    	persistentApplication.addPersistentClass( SmsGenerator.class );
    	persistentApplication.addPersistentClass( SmsMessageOwner.class );
    	persistentApplication.addPersistentClass( KeyedBulkSmsSourceInter.class );
    	persistentApplication.addPersistentClass( CustomSmsTemplateInter.class );
	}

	public void setSagePayServerUsed(boolean isSagePayServerUsed) {
		this.isSagePayServerUsed = isSagePayServerUsed;
	}

	public boolean isSagePayServerUsed() {
		return isSagePayServerUsed;
	}

	public void setSagePayDirectUsed(boolean isSagePayDirectUsed) {
		this.isSagePayDirectUsed = isSagePayDirectUsed;
	}

	public boolean isSagePayDirectUsed() {
		return isSagePayDirectUsed;
	}
	
	
	public void setCardSaveServerUsed(boolean isCardSaveServerUsed) {
		this.isCardSaveServerUsed = isCardSaveServerUsed;
	}

	public boolean isCardSaveServerUsed() {
		return isCardSaveServerUsed;
	}

	public void setCardSaveDirectUsed(boolean isCardSaveDirectUsed) {
		this.isCardSaveDirectUsed = isCardSaveDirectUsed;
	}

	public boolean isCardSaveDirectUsed() {
		return isCardSaveDirectUsed;
	}
	

	public void setPayPalDirectUsed(boolean isPayPalDirectUsed) {
		this.isPayPalDirectUsed = isPayPalDirectUsed;
	}

	public boolean isPayPalDirectUsed() {
		return isPayPalDirectUsed;
	}

	public void setPaymentSystemUsed(boolean isPaymentSystemUsed) {
		this.isPaymentSystemUsed = isPaymentSystemUsed;
	}

	public boolean isPaymentSystemUsed() {
		return isPaymentSystemUsed;
	}

	public void setCurrencyUsed(boolean isCurrencyUsed) {
		this.isCurrencyUsed = isCurrencyUsed;
	}

	public boolean isCurrencyUsed() {
		return isCurrencyUsed;
	}

	public void setCountryAreaUsed(boolean isCountryAreaUsed) {
		this.isCountryAreaUsed = isCountryAreaUsed;
	}

	public boolean isCountryAreaUsed() {
		return isCountryAreaUsed;
	}

	public boolean isSmsUsed() {
		return isSmsUsed;
	}

	public void setSmsUsed(boolean isSmsUsed) {
		this.isSmsUsed = isSmsUsed;
		if( isSmsUsed ) {
			CommonConfiguration.CommonScheduledJobEnum.SMS_BUNDLE_ACTIVATION_JOB.setActive(true);
		}
	}
}
