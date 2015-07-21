package com.aplos.common.module;



import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.CreditCardType;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails.ConnectToType;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BasicEmailFolder;
import com.aplos.common.beans.communication.EmailFrame;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.beans.communication.SmsAccount;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails.PayPalServerType;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.enums.BulkMessageFinderEnum;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.enums.PaymentGateway;
import com.aplos.common.enums.ScheduledJobEnum;
import com.aplos.common.interfaces.BulkMessageFinderInter;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.CustomSmsTemplateInter;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.scheduledjobs.ArchiverJob;
import com.aplos.common.scheduledjobs.AutomaticAplosEmailJob;
import com.aplos.common.scheduledjobs.DeletableEmailAddressJob;
import com.aplos.common.scheduledjobs.IncomingAplosEmailJob;
import com.aplos.common.scheduledjobs.SmsBundleActivationJob;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.UserLevelUtil;

@Entity
@Cache
public class CommonConfiguration extends ModuleConfiguration {
	private static final long serialVersionUID = 8307954302058891326L;

	//Not Configurable by Settings Interface

	private Boolean databaseOnHold = false; //when true this should disallow any editing/database access for admin purposes
	private Boolean defaultRowsLoaded = false;

	@OneToOne(fetch=FetchType.LAZY)
	private BasicEmailFolder inboxEmailFolder;
	@OneToOne(fetch=FetchType.LAZY)
	private BasicEmailFolder sentEmailFolder;

	@OneToOne
	private SmsAccount defaultSmsAccount;
	//Configurable by Settings Interface for SuperUser Only

	@OneToOne(fetch=FetchType.LAZY)
	private SystemUser defaultDebugUser;
	@OneToOne(fetch=FetchType.LAZY)
	private SystemUser defaultSuperUser;
	@OneToOne(fetch=FetchType.LAZY)
	private CreditCardType visaCardType;
	private Boolean isInternationalizedApplication;
	@Column(columnDefinition="LONGTEXT")
	private String multipleTabWarning = "It is dangerous to use this application with multiple tabs, please close any additional tabs that are using this application.  You can have multiple instances of the application by opening another browser window as long as you only have one tab per window with this application.";

	//Configurable by Settings Interface for Other Users

	private String defaultLanguageStr = "en"; //compatible with Locale
	@OneToOne(fetch=FetchType.LAZY)
	private SystemUser defaultAdminUser;
	@OneToOne(fetch=FetchType.LAZY)
	private CreditCardType defaultCreditCardType;
	@OneToOne(fetch=FetchType.LAZY)
	private CompanyDetails defaultCompanyDetails;
	@OneToOne(fetch=FetchType.LAZY)
	private Currency defaultCurrency;
	@OneToOne(fetch=FetchType.LAZY)
	private VatType defaultVatType;
	@OneToOne(fetch=FetchType.LAZY)
	private VatType ukVatType;
	private Integer maxFailedLoginAttempts = 3;
	private String payPalEmail;
	private String defaultNotSelectedText = "---";
	private boolean isPasswordEncrypted = false;
	private String scannerPrefix;
	private String scannerSuffix;
	private boolean isScannerPrefixUsingCtrl = false;
	private boolean isVatInclusive = true;
	private boolean isDeliveryVatInclusive = true;
	private boolean isUsingNexemo = false;
	private boolean isAllowingAdditionalUserLevels = false;

	private boolean isShowingEditPencilColumn = false;
	private boolean isShowingIdColumn = true;
	private boolean isShowingSearch = false;
	private boolean isShowingFrontendSessionTimeout = false;
	private boolean isUsingWindowId = false;

	private boolean isUsingEmailBodyDivider = false;
	private boolean isSelectableRowsAllowed = true;
	
	@OneToOne(fetch=FetchType.LAZY)
	private UserLevelUtil userLevelUtil;
	
	@OneToOne(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private SmsAccount smsAccount = new SmsAccount();

	private boolean isShowingLoginRememberMe = true;

	private PaymentGateway defaultPaymentGateway = PaymentGateway.CARDSAVE;

	@OneToOne(fetch=FetchType.LAZY)
	private SagePayConfigurationDetails sagePayCfgDetails;
	@OneToOne(fetch=FetchType.LAZY)
	private CardSaveConfigurationDetails cardSaveCfgDetails;
	@OneToOne(fetch=FetchType.LAZY)
	private PayPalConfigurationDetails payPalCfgDetails;

	@OneToOne(fetch=FetchType.LAZY)
	private SagePayConfigurationDetails testSagePayCfgDetails;
	@OneToOne(fetch=FetchType.LAZY)
	private CardSaveConfigurationDetails testCardSaveCfgDetails;
	@OneToOne(fetch=FetchType.LAZY)
	private PayPalConfigurationDetails testPayPalCfgDetails;

	@OneToOne(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private MailServerSettings mailServerSettings;
	@OneToOne(fetch=FetchType.LAZY)
	private MailServerSettings errorMailServerSettings;

	private boolean isTestingPayPal = true;
	private boolean isTestingSagePay = true;
	private boolean isTestingCardSave = true;
	private boolean isUsingPayPalExpress = false;
	private boolean isFrontEndTabSessions = false;
	private boolean isReadReceiptsRequired = false;
	private boolean isShowingEmailFolders = false;
	
	@ManyToOne
	private EmailFrame emailFrame;
	
	@ManyToOne
	private EmailFrame outerEmailFrame;
	
	private String highlightColour = "#f37c36";
	private String mainHeaderColour = "#beae94";
	private String subHeaderColour = "#ededed";
	private String subHeaderTextColour = "#beae94";

	private String nexemoKey = null;
	private String nexemoSecretKey = null;

	@Transient
	private Map<BulkMessageFinderEnum, BasicBulkMessageFinder> bulkMessageFinderMap = new HashMap<BulkMessageFinderEnum, BasicBulkMessageFinder>();

	@Transient
	private Map<ScheduledJobEnum, ScheduledJob> scheduledJobMap = new HashMap<ScheduledJobEnum, ScheduledJob>();

	@Transient
	private Map<EmailTemplateEnum, EmailTemplate> emailTemplateMap = new HashMap<EmailTemplateEnum, EmailTemplate>();
	@Transient
	private Map<Class<? extends SmsTemplate>, SmsTemplate> smsTemplateMap = new HashMap<Class<? extends SmsTemplate>, SmsTemplate>();
	@Transient
	private Map<Class<? extends PrintTemplate>, PrintTemplate> printTemplateMap = new HashMap<Class<? extends PrintTemplate>, PrintTemplate>();
	
	public enum CommonScheduledJobEnum implements ScheduledJobEnum {
		AUTOMATIC_APLOS_EMAIL_JOB ( AutomaticAplosEmailJob.class ),
		INCOMING_APLOS_EMAIL_JOB ( IncomingAplosEmailJob.class ),
		DELETABLE_EMAIL_ADDRESS_JOB ( DeletableEmailAddressJob.class ),
		SMS_BUNDLE_ACTIVATION_JOB ( SmsBundleActivationJob.class, false ),
		ARCHIVER_JOB ( ArchiverJob.class, false );
		Class<? extends ScheduledJob<?>> scheduledJobClass;
		boolean isActive = true;
		
		private CommonScheduledJobEnum( Class<? extends ScheduledJob<?>> scheduledJobClass ) {
			this.scheduledJobClass = scheduledJobClass;
		}
		
		private CommonScheduledJobEnum( Class<? extends ScheduledJob<?>> scheduledJobClass, boolean isActive ) {
			this.scheduledJobClass = scheduledJobClass;
			setActive(isActive);
		}
		
		@Override
		public Class<? extends ScheduledJob<?>> getScheduledJobClass() {
			return scheduledJobClass;
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

	public enum CommonBulkMessageFinderEnum implements BulkMessageFinderEnum {
		SUBSCRIBER ( Subscriber.class, "Subscriber" ),
		SYSTEM_USER ( SystemUser.class, "System User" ),
		BASIC_CONTACT ( BasicContact.class, "Basic contact" );
		
		Class<? extends BulkMessageFinderInter> bulkMessageFinderClass; 
		String bulkMessageFinderName;
		boolean isActive = true;
		
		private CommonBulkMessageFinderEnum( Class<? extends BulkMessageFinderInter> bulkMessageFinderClass, String bulkMessageFinderName ) {
			this.bulkMessageFinderClass = bulkMessageFinderClass;
			this.bulkMessageFinderName = bulkMessageFinderName;
		}
		
		@Override
		public Class<? extends BulkMessageFinderInter> getBulkMessageFinderClass() {
			return bulkMessageFinderClass;
		}
		
		@Override
		public String getBulkMessageFinderName() {
			return bulkMessageFinderName;
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

	@Override
	public int getMaximumModuleVersionMajor() {
		return 1;
	}

	@Override
	public int getMaximumModuleVersionMinor() {
		return 10;
	}

	@Override
	public int getMaximumModuleVersionPatch() {
		return 0;
	}

	public DataTableState getDefaultDataTableState() {
		DataTableState dataTableState = new DataTableState();
		dataTableState.setShowingEditPencilColumn(isShowingEditPencilColumn);
		dataTableState.setShowingIdColumn(isShowingIdColumn);
		dataTableState.setShowingSearch(isShowingSearch());
		return dataTableState;
	}

	public SagePayConfigurationDetails determineSagePayCfgDetails() {
		if( ApplicationUtil.getAplosContextListener().isDebugMode() || isTestingSagePay() ) {
			return getTestSagePayCfgDetails();
		} else {
			return getSagePayCfgDetails();
		}
	}
	
	public CardSaveConfigurationDetails determineCardSaveCfgDetails() {
		if( ApplicationUtil.getAplosContextListener().isDebugMode() || isTestingCardSave() ) {
			return getTestCardSaveCfgDetails();
		} else {
			return getCardSaveCfgDetails();
		}
	}

	public PayPalConfigurationDetails determinePayPalCfgDetails() {
		if( ApplicationUtil.getAplosContextListener().isDebugMode() || isTestingPayPal() ) {
			return getTestPayPalCfgDetails();
		} else {
			return getPayPalCfgDetails();
		}
	}
	
	public static List<BulkMessageFinderEnum> getActiveBulkMessageFinderEnums( BulkMessageFinderEnum[] bulkMessageFinderEnum ) {
		List<BulkMessageFinderEnum> activeEnums = new ArrayList<BulkMessageFinderEnum>( Arrays.asList(bulkMessageFinderEnum) );
		for( int i = activeEnums.size() - 1; i > -1; i-- ) {
			if( !activeEnums.get( i ).isActive() ) {
				activeEnums.remove( i );
			}
		}
		return activeEnums;
	}
	
	public static List<ScheduledJobEnum> getActiveScheduledJobEnums( ScheduledJobEnum[] scheduledJobEnums ) {
		List<ScheduledJobEnum> activeEnums = new ArrayList<ScheduledJobEnum>( Arrays.asList(scheduledJobEnums) );
		for( int i = activeEnums.size() - 1; i > -1; i-- ) {
			if( !activeEnums.get( i ).isActive() ) {
				activeEnums.remove( i );
			}
		}
		return activeEnums;
	}
	
	public static List<EmailTemplateEnum> getActiveEmailTemplateEnums( EmailTemplateEnum[] emailTemplateEnums ) {
		List<EmailTemplateEnum> activeEnums = new ArrayList<EmailTemplateEnum>( Arrays.asList(emailTemplateEnums) );
		for( int i = activeEnums.size() - 1; i > -1; i-- ) {
			if( !activeEnums.get( i ).isActive() ) {
				activeEnums.remove( i );
			}
		}
		return activeEnums;
	}

	@Override
	public boolean recursiveModuleConfigurationInit(AplosContextListener aplosContextListener, int loopCount) {
		super.recursiveModuleConfigurationInit(aplosContextListener, loopCount);

		if( loopCount == 0 ) {
			createConfigurationObjects(aplosContextListener);
			CommonConfiguration commonConfiguration = getCommonConfiguration(); 
			if( commonConfiguration.getDefaultCompanyDetails() != null ) {
				aplosContextListener.getContext().setAttribute( CommonUtil.getBinding( CompanyDetails.class ), commonConfiguration.getDefaultCompanyDetails() );
			}
			commonConfiguration.registerEmailTemplateEnums( getActiveEmailTemplateEnums( CommonEmailTemplateEnum.values() ) );
			commonConfiguration.registerBulkMessageFinderEnums( getActiveBulkMessageFinderEnums( CommonBulkMessageFinderEnum.values() ) );
			commonConfiguration.registerScheduledJobEnums( getActiveScheduledJobEnums( CommonScheduledJobEnum.values() ) );
			return true;
		} else if ( loopCount == 1 ) {
			loadOrCreateEmailTemplates( aplosContextListener );
			CommonModule commonModule = (CommonModule) ApplicationUtil.getAplosContextListener().getAplosModuleByClass( CommonModule.class );
			if( ((CommonModuleDbConfig) commonModule.getModuleDbConfig()).isSmsUsed() ) {
				loadOrCreateSmsTemplates( aplosContextListener );
			}
			loadOrCreateBulkMessageFinders( aplosContextListener );
			loadOrCreateScheduledJobs( aplosContextListener );
		}

		return false;
	}

	public void createConfigurationObjects(AplosContextListener aplosContextListener) {
		boolean saveConfiguration = false;

		if( getCommonConfiguration().getDefaultCompanyDetails() == null ) {
			CompanyDetails companyDetails = new CompanyDetails();
			companyDetails.saveDetails();
			getCommonConfiguration().setDefaultCompanyDetails( companyDetails );
			saveConfiguration = true;
		}

		if( getCommonConfiguration().getUserLevelUtil() == null ) {
			setUserLevelUtil( ApplicationUtil.getAplosModuleFilterer(aplosContextListener).createUserLevelUtil() );
			getUserLevelUtil().saveDetails();
			saveConfiguration = true;
		}

		if( getSagePayCfgDetails() == null ) {
			setSagePayCfgDetails( new SagePayConfigurationDetails() );
			getSagePayCfgDetails().setName( "Live configuration details" );
			getSagePayCfgDetails().saveDetails();
			saveConfiguration = true;
		}

		if( getTestSagePayCfgDetails() == null ) {
			setTestSagePayCfgDetails( new SagePayConfigurationDetails() );
			getTestSagePayCfgDetails().setName( "Test configuration details" );
			getTestSagePayCfgDetails().setConnectToType( SagePayConfigurationDetails.ConnectToType.SIMULATOR );
			getTestSagePayCfgDetails().saveDetails();
			saveConfiguration = true;
		}

		if( getPayPalCfgDetails() == null ) {
			setPayPalCfgDetails( new PayPalConfigurationDetails() );
			getPayPalCfgDetails().setName( "Live configuration details" );
			getPayPalCfgDetails().setPayPalServerType( PayPalServerType.LIVE );
			getPayPalCfgDetails().saveDetails();
			saveConfiguration = true;
		}

		if( getTestPayPalCfgDetails() == null ) {
			setTestPayPalCfgDetails( new PayPalConfigurationDetails() );
			getTestPayPalCfgDetails().setName( "Test configuration details" );
			getTestPayPalCfgDetails().setPayPalServerType( PayPalServerType.TEST );
			getTestPayPalCfgDetails().saveDetails();
			saveConfiguration = true;
		}

		if( getCardSaveCfgDetails() == null ) {
			setCardSaveCfgDetails( new CardSaveConfigurationDetails() );
			getCardSaveCfgDetails().setName( "Live configuration details" );
			getCardSaveCfgDetails().setConnectToType( ConnectToType.LIVE );
			getCardSaveCfgDetails().saveDetails();
			saveConfiguration = true;
		}

		if( getTestCardSaveCfgDetails() == null ) {
			setTestCardSaveCfgDetails( new CardSaveConfigurationDetails() );
			getTestCardSaveCfgDetails().setName( "Test configuration details" );
			getTestCardSaveCfgDetails().setConnectToType( ConnectToType.TEST );
			getTestCardSaveCfgDetails().saveDetails();
			saveConfiguration = true;
		}
		
		if ( errorMailServerSettings == null ) {
			MailServerSettings mailServerSettings = new MailServerSettings();
			mailServerSettings.saveDetails();
			setErrorMailServerSettings(mailServerSettings);
		}
		
		if ( mailServerSettings == null ) {
			MailServerSettings mailServerSettings = new MailServerSettings();
			mailServerSettings.saveDetails();
			setMailServerSettings(mailServerSettings);
			saveConfiguration = true;
	
			List<Website> websiteList = new BeanDao( Website.class ).getAll();
			for( Website website : websiteList ) {
				if( website.getMailServerSettings() == null ) {
					MailServerSettings mailServerSettingsTemp = new MailServerSettings();
					mailServerSettingsTemp.saveDetails();
					website.setMailServerSettings( mailServerSettings );
					website.saveDetails();
				}
			}
		}

		if( saveConfiguration ) {
			saveDetails();
		}
	}

	public void registerSmsTemplateClass( Class<? extends SmsTemplate> smsTemplateClass ) {
		getSmsTemplateMap().put( smsTemplateClass, null );
	}

	public void registerEmailTemplateEnums( List<? extends EmailTemplateEnum> emailTemplateEnums ) {
		for( EmailTemplateEnum tempEmailTemplateEnum : emailTemplateEnums ) {
			getEmailTemplateMap().put( tempEmailTemplateEnum, null );
		}
	}

	public void registerBulkMessageFinderEnums( List<? extends BulkMessageFinderEnum> bulkMessageFinderEnums ) {
		for( BulkMessageFinderEnum tempBulkMessageFinderEnum : bulkMessageFinderEnums ) {
			getBulkMessageFinderMap().put( tempBulkMessageFinderEnum, null );
		}
	}

	public void registerScheduledJobEnums( List<? extends ScheduledJobEnum> scheduledJobEnums ) {
		for( ScheduledJobEnum scheduledJobEnum : scheduledJobEnums ) {
			getScheduledJobMap().put( scheduledJobEnum, null );
		}
	}

	public void loadOrCreateEmailTemplates( AplosContextListener aplosContextListener ) {
		BeanDao emailTemplateDao = new BeanDao( EmailTemplate.class );

		List<EmailTemplate> emailTemplateList = emailTemplateDao.setIsReturningActiveBeans(true).getAll();
		Map<Class<? extends EmailTemplate>,EmailTemplate> dbEmailTemplateMap = new HashMap<Class<? extends EmailTemplate>,EmailTemplate>();
		for ( EmailTemplate tempEmailTemplate : emailTemplateList ) {
			tempEmailTemplate = dbEmailTemplateMap.put( tempEmailTemplate.getClass(), tempEmailTemplate );
			if( tempEmailTemplate != null ) {
				tempEmailTemplate.delete();
			}
		}

		for ( EmailTemplateEnum emailTemplateEnum : getEmailTemplateMap().keySet() ) {
			EmailTemplate emailTemplate = dbEmailTemplateMap.get( emailTemplateEnum.getEmailTemplateClass() );
			if ( emailTemplate == null ) {
				emailTemplate = (EmailTemplate) CommonUtil.getNewInstance( emailTemplateEnum.getEmailTemplateClass(), null );
				emailTemplate.loadDefaultValuesAndSave();
				emailTemplate.saveDetails();
			}

			getEmailTemplateMap().put( emailTemplateEnum, emailTemplate );
		}
	}

	public void loadOrCreateSmsTemplates( AplosContextListener aplosContextListener ) {
		for( PersistentClass persistentClass : ApplicationUtil.getPersistentApplication().getPersistentClassMap().values() ) {
			if( persistentClass.isIncludeInApp() ) {
				if( SmsTemplate.class.isAssignableFrom( persistentClass.getTableClass() ) && !Modifier.isAbstract( persistentClass.getTableClass().getModifiers() ) ) {
					registerSmsTemplateClass( (Class<? extends SmsTemplate>) persistentClass.getTableClass() );
				}
			}
		}
		
		BeanDao smsTemplateDao = new BeanDao( SmsTemplate.class );

		List<SmsTemplate> smsTemplateList = smsTemplateDao.setIsReturningActiveBeans(true).getAll();
		Map<Class<? extends SmsTemplate>,SmsTemplate> dbSmsTemplateMap = new HashMap<Class<? extends SmsTemplate>,SmsTemplate>();
		for ( SmsTemplate tempSmsTemplate : smsTemplateList ) {
//			HibernateUtil.initialise( tempSmsTemplate, true );
			tempSmsTemplate.checkVersion(false);
			tempSmsTemplate = dbSmsTemplateMap.put( tempSmsTemplate.getClass(), tempSmsTemplate );
			if( tempSmsTemplate != null && !(tempSmsTemplate instanceof CustomSmsTemplateInter) ) {
				((SmsTemplate) tempSmsTemplate.getSaveableBean()).delete();
			}
		}

		for ( Class<? extends SmsTemplate> smsTemplateClass : getSmsTemplateMap().keySet() ) {
			SmsTemplate smsTemplate = dbSmsTemplateMap.get( smsTemplateClass );
			if ( smsTemplate == null && !(smsTemplateClass.isAssignableFrom( CustomSmsTemplateInter.class ) ) ) {
				smsTemplate = (SmsTemplate) CommonUtil.getNewInstance( smsTemplateClass, null );
				smsTemplate.loadDefaultValuesAndSave();
				smsTemplate.saveDetails();
			}

			getSmsTemplateMap().put( smsTemplateClass, smsTemplate );
		}
	}
	
	public void loadOrCreateBulkMessageFinders( AplosContextListener aplosContextListener ) {
		BeanDao bulkMessageFinderDao = new BeanDao( BasicBulkMessageFinder.class );

		List<BasicBulkMessageFinder> bulkMessageFinderList = bulkMessageFinderDao.setIsReturningActiveBeans(true).getAll();
		List<BulkMessageFinderEnum> bulkMessageFinderEnums = new ArrayList<BulkMessageFinderEnum>( getBulkMessageFinderMap().keySet() );
		for ( BasicBulkMessageFinder tempBulkMessageFinder : bulkMessageFinderList ) {
			for( int i = 0, n = bulkMessageFinderEnums.size(); i < n; i++ ) {
				if( tempBulkMessageFinder.getBulkMessageFinderClass().equals( bulkMessageFinderEnums.get( i ).getBulkMessageFinderClass() )) {
					getBulkMessageFinderMap().put( bulkMessageFinderEnums.get( i ), tempBulkMessageFinder );
					bulkMessageFinderEnums.remove( i );
					break;
				}
			}
		}

		for ( BulkMessageFinderEnum bulkMessageFinderEnum : getBulkMessageFinderMap().keySet() ) {
			BasicBulkMessageFinder bulkMessageFinder = getBulkMessageFinderMap().get( bulkMessageFinderEnum );
			if ( bulkMessageFinder == null ) {
				bulkMessageFinder = new BasicBulkMessageFinder( (Class<? extends BulkMessageFinderInter>) bulkMessageFinderEnum.getBulkMessageFinderClass() );
				bulkMessageFinder.setName( bulkMessageFinder.getBulkEmailFinderClassInstance().getBulkMessageFinderName() );
				bulkMessageFinder.setSourceType( (Class<? extends BulkMessageSource>) bulkMessageFinderEnum.getBulkMessageFinderClass() );
				bulkMessageFinder.saveDetails();
			}

			getBulkMessageFinderMap().put( bulkMessageFinderEnum, bulkMessageFinder );
		}
	}
	
	public void loadOrCreateScheduledJobs( AplosContextListener aplosContextListener ) {
		BeanDao scheduledJobDao = new BeanDao( ScheduledJob.class );

		List<ScheduledJob<?>> scheduledJobs = scheduledJobDao.setIsReturningActiveBeans(true).getAll();
		List<ScheduledJobEnum> scheduledJobEnums = new ArrayList<ScheduledJobEnum>( getScheduledJobMap().keySet() );
		for ( ScheduledJob<?> tempScheduledJob : scheduledJobs ) {
			for( int i = 0, n = scheduledJobEnums.size(); i < n; i++ ) {
				if( scheduledJobEnums.get( i ).getScheduledJobClass().equals( tempScheduledJob.getClass() )) {
					getScheduledJobMap().put( scheduledJobEnums.get( i ), tempScheduledJob );
					scheduledJobEnums.remove( i );
					break;
				}
			}
		}

		for ( ScheduledJobEnum scheduledJobEnum : getScheduledJobMap().keySet() ) {
			ScheduledJob<?> scheduledJob = getScheduledJobMap().get( scheduledJobEnum );
			if ( scheduledJob == null ) {
				scheduledJob = (ScheduledJob) CommonUtil.getNewInstance( scheduledJobEnum.getScheduledJobClass() );
				scheduledJob.saveDetails();
			}

			getScheduledJobMap().put( scheduledJobEnum, scheduledJob );
		}
	}

	public void setUkVatType(VatType ukVatType) {
		this.ukVatType = ukVatType;
	}

	public VatType getUkVatType() {
		return ukVatType;
	}

	public static CommonConfiguration getCommonConfiguration() {
		return (CommonConfiguration) getModuleConfiguration( CommonConfiguration.class );
	}

	@Override
	public ModuleConfiguration getModuleConfiguration() {
		return CommonConfiguration.getCommonConfiguration();
	}

	public static UserLevelUtil retrieveUserLevelUtil() {
		return getCommonConfiguration().getUserLevelUtil();
	}

	public void setDefaultAdminUser(SystemUser defaultAdminUser) {
		this.defaultAdminUser = defaultAdminUser;
	}

	public SystemUser getDefaultAdminUser() {
		return defaultAdminUser;
	}

	public void setDefaultDebugUser(SystemUser defaultDebugUser) {
		this.defaultDebugUser = defaultDebugUser;
	}

	public SystemUser getDefaultDebugUser() {
		return defaultDebugUser;
	}

	public void setDefaultSuperUser(SystemUser defaultSuperUser) {
		this.defaultSuperUser = defaultSuperUser;
	}

	public SystemUser getDefaultSuperUser() {
		return defaultSuperUser;
	}

	public void setDefaultCreditCardType(CreditCardType defaultCreditCardType) {
		this.defaultCreditCardType = defaultCreditCardType;
	}

	public CreditCardType getDefaultCreditCardType() {
		return defaultCreditCardType;
	}

	public void setDefaultCurrency(Currency defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	public Currency getDefaultCurrency() {
		return defaultCurrency;
	}

	public void setDefaultCompanyDetails(CompanyDetails defaultCompanyDetails) {
		this.defaultCompanyDetails = defaultCompanyDetails;
	}

	public CompanyDetails getDefaultCompanyDetails() {
		return defaultCompanyDetails;
	}

	public void setDefaultRowsLoaded(Boolean defaultRowsLoaded) {
		this.defaultRowsLoaded = defaultRowsLoaded;
	}

	public Boolean getDefaultRowsLoaded() {
		if( defaultRowsLoaded == null ) {
			return true;
		} else {
			return defaultRowsLoaded;
		}
	}

	public void setDefaultLanguageStr(String defaultLanguageStr) {
		if( defaultLanguageStr != null ) {
			defaultLanguageStr = defaultLanguageStr.toLowerCase();
		}
		this.defaultLanguageStr = defaultLanguageStr;
	}

	public String getDefaultLanguageStr() {
		return defaultLanguageStr;
	}

	public void setIsInternationalizedApplication(
			Boolean isInternationalizedApplication) {
		this.isInternationalizedApplication = isInternationalizedApplication;
	}

	public Boolean getIsInternationalizedApplication() {
		if( isInternationalizedApplication == null ) {
			return false;
		} else {
			return isInternationalizedApplication;
		}
	}

	public void setDefaultVatType(VatType defaultVatType) {
		this.defaultVatType = defaultVatType;
	}

	public VatType getDefaultVatType() {
		return defaultVatType;
	}

	public void setDatabaseOnHold(Boolean databaseOnHold) {
		this.databaseOnHold = databaseOnHold;
	}

	public Boolean isDatabaseOnHold() {
		if (databaseOnHold == null) {
			databaseOnHold = false;
		}
		return databaseOnHold;
	}

	public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
		this.maxFailedLoginAttempts = maxFailedLoginAttempts;
	}

	public Integer getMaxFailedLoginAttempts() {
		return maxFailedLoginAttempts;
	}

	public void setDefaultNotSelectedText(String defaultNotSelectedText) {
		this.defaultNotSelectedText = defaultNotSelectedText;
	}

	public String getDefaultNotSelectedText() {
		return defaultNotSelectedText;
	}

	public Map<EmailTemplateEnum, EmailTemplate> getEmailTemplateMap() {
		return emailTemplateMap;
	}

	public void setEmailTemplateMap(Map<EmailTemplateEnum, EmailTemplate> emailTemplateMap) {
		this.emailTemplateMap = emailTemplateMap;
	}

	public boolean isPasswordEncrypted() {
		return isPasswordEncrypted;
	}

	public void setPasswordEncrypted(boolean isPasswordEncrypted) {
		this.isPasswordEncrypted = isPasswordEncrypted;
	}

	public PaymentGateway getDefaultPaymentGateway() {
		return defaultPaymentGateway;
	}

	public void setDefaultPaymentGateway(PaymentGateway defaultPaymentGateway) {
		this.defaultPaymentGateway = defaultPaymentGateway;
	}

	public String getScannerPrefix() {
		return scannerPrefix;
	}

	public void setScannerPrefix(String scannerPrefix) {
		this.scannerPrefix = scannerPrefix;
	}

	public String getScannerSuffix() {
		return scannerSuffix;
	}

	public void setScannerSuffix(String scannerSuffix) {
		this.scannerSuffix = scannerSuffix;
	}

	public String getScannerPrefixForJavascript() {
		if( scannerPrefix != null ) {
			return "'" + scannerPrefix + "'";
		} else {
			return "null";
		}
	}

	public String getScannerSuffixForJavascript() {
		if( scannerSuffix != null ) {
			return "'" + scannerSuffix + "'";
		} else {
			return "null";
		}
	}

	public SagePayConfigurationDetails getSagePayCfgDetails() {
		return sagePayCfgDetails;
	}

	public void setSagePayCfgDetails(SagePayConfigurationDetails sagePayCfgDetails) {
		this.sagePayCfgDetails = sagePayCfgDetails;
	}

	public PayPalConfigurationDetails getPayPalCfgDetails() {
		return payPalCfgDetails;
	}

	public void setPayPalCfgDetails(PayPalConfigurationDetails payPalCfgDetails) {
		this.payPalCfgDetails = payPalCfgDetails;
	}

	public SagePayConfigurationDetails getTestSagePayCfgDetails() {
		return testSagePayCfgDetails;
	}

	public void setTestSagePayCfgDetails(SagePayConfigurationDetails testSagePayCfgDetails) {
		this.testSagePayCfgDetails = testSagePayCfgDetails;
	}

	public PayPalConfigurationDetails getTestPayPalCfgDetails() {
		return testPayPalCfgDetails;
	}

	public void setTestPayPalCfgDetails(PayPalConfigurationDetails testPayPalCfgDetails) {
		this.testPayPalCfgDetails = testPayPalCfgDetails;
	}

	public boolean isTestingPayPal() {
		return isTestingPayPal;
	}

	public void setTestingPayPal(boolean isTestingPayPal) {
		this.isTestingPayPal = isTestingPayPal;
	}

	public boolean isTestingSagePay() {
		return isTestingSagePay;
	}

	public void setTestingSagePay(boolean isTestingSagePay) {
		this.isTestingSagePay = isTestingSagePay;
	}

	public boolean isUsingPayPalExpress() {
		return isUsingPayPalExpress;
	}

	public void setUsingPayPalExpress(boolean isUsingPayPalExpress) {
		this.isUsingPayPalExpress = isUsingPayPalExpress;
	}

	// Another hibernate hack, so that this method will work from facelets.
	public UserLevelUtil getImplementedUserLevelUtil() {
//		userLevelUtil = HibernateUtil.getImplementation( userLevelUtil );
		return userLevelUtil;
	}

	public UserLevelUtil getUserLevelUtil() {
		return userLevelUtil;
	}

	public void setUserLevelUtil(UserLevelUtil userLevelUtil) {
		this.userLevelUtil = userLevelUtil;
	}

	public boolean isVatInclusive() {
		return isVatInclusive;
	}

	public void setVatInclusive(boolean isVatInclusive) {
		this.isVatInclusive = isVatInclusive;
	}

	public boolean isDeliveryVatInclusive() {
		return isDeliveryVatInclusive;
	}

	public void setDeliveryVatInclusive(boolean isDeliveryVatInclusive) {
		this.isDeliveryVatInclusive = isDeliveryVatInclusive;
	}

	public boolean isFrontEndTabSessions() {
		return isFrontEndTabSessions;
	}

	public void setFrontEndTabSessions(boolean isFrontEndTabSessions) {
		this.isFrontEndTabSessions = isFrontEndTabSessions;
	}
	
	public void addEmailTemplateOverride( EmailTemplateEnum originalEmailEnum, EmailTemplateEnum overridingEmailEnum ) {
		getEmailTemplateMap().put( originalEmailEnum, getEmailTemplateMap().get( overridingEmailEnum ) );
	}

	public MailServerSettings getMailServerSettings() {
		return mailServerSettings;
	}

	public void setMailServerSettings(MailServerSettings mailServerSettings) {
		this.mailServerSettings = mailServerSettings;
	}

	public Map<BulkMessageFinderEnum, BasicBulkMessageFinder> getBulkMessageFinderMap() {
		return bulkMessageFinderMap;
	}

	public void setBulkMessageFinderMap(Map<BulkMessageFinderEnum, BasicBulkMessageFinder> bulkMessageFinderMap) {
		this.bulkMessageFinderMap = bulkMessageFinderMap;
	}

	public boolean isScannerPrefixUsingCtrl() {
		return isScannerPrefixUsingCtrl;
	}

	public void setScannerPrefixUsingCtrl(boolean isScannerPrefixUsingCtrl) {
		this.isScannerPrefixUsingCtrl = isScannerPrefixUsingCtrl;
	}

	public CardSaveConfigurationDetails getCardSaveCfgDetails() {
		return cardSaveCfgDetails;
	}

	public void setCardSaveCfgDetails(CardSaveConfigurationDetails cardSaveCfgDetails) {
		this.cardSaveCfgDetails = cardSaveCfgDetails;
	}

	public CardSaveConfigurationDetails getTestCardSaveCfgDetails() {
		return testCardSaveCfgDetails;
	}

	public void setTestCardSaveCfgDetails(CardSaveConfigurationDetails testCardSaveCfgDetails) {
		this.testCardSaveCfgDetails = testCardSaveCfgDetails;
	}

	public boolean isTestingCardSave() {
		return isTestingCardSave;
	}

	public void setTestingCardSave(boolean isTestingCardSave) {
		this.isTestingCardSave = isTestingCardSave;
	}

	public boolean isShowingSearch() {
		return isShowingSearch;
	}

	public void setShowingSearch(boolean isShowingSearch) {
		this.isShowingSearch = isShowingSearch;
	}

	public boolean isShowingLoginRememberMe() {
		return isShowingLoginRememberMe;
	}

	public void setShowingLoginRememberMe(boolean isShowingLoginRememberMe) {
		this.isShowingLoginRememberMe = isShowingLoginRememberMe;
	}

	public boolean isShowingFrontendSessionTimeout() {
		return isShowingFrontendSessionTimeout;
	}

	public void setShowingFrontendSessionTimeout(
			boolean isShowingFrontendSessionTimeout) {
		this.isShowingFrontendSessionTimeout = isShowingFrontendSessionTimeout;
	}

	public boolean isUsingWindowId() {
		return isUsingWindowId;
	}

	public void setUsingWindowId(boolean isUsingWindowId) {
		this.isUsingWindowId = isUsingWindowId;
	}

	public Map<Class<? extends PrintTemplate>, PrintTemplate> getPrintTemplateMap() {
		return printTemplateMap;
	}

	public void setPrintTemplateMap(Map<Class<? extends PrintTemplate>, PrintTemplate> printTemplateMap) {
		this.printTemplateMap = printTemplateMap;
	}

	public EmailFrame getEmailFrame() {
		return emailFrame;
	}

	public void setEmailFrame(EmailFrame emailFrame) {
		this.emailFrame = emailFrame;
	}
	
	public String getPayPalEmail() {
		return payPalEmail;
	}

	public void setPayPalEmail(String payPalEmail) {
		this.payPalEmail = payPalEmail;
	}

	public EmailFrame getOuterEmailFrame() {
		return outerEmailFrame;
	}

	public void setOuterEmailFrame(EmailFrame outerEmailFrame) {
		this.outerEmailFrame = outerEmailFrame;
	}

	public MailServerSettings getErrorMailServerSettings() {
		return errorMailServerSettings;
	}

	public void setErrorMailServerSettings(MailServerSettings errorMailServerSettings) {
		this.errorMailServerSettings = errorMailServerSettings;
	}

	public boolean isReadReceiptsRequired() {
		return isReadReceiptsRequired;
	}

	public void setReadReceiptsRequired(boolean isReadReceiptsRequired) {
		this.isReadReceiptsRequired = isReadReceiptsRequired;
	}

	public boolean isShowingEmailFolders() {
		return isShowingEmailFolders;
	}

	public void setShowingEmailFolders(boolean isShowingEmailFolders) {
		this.isShowingEmailFolders = isShowingEmailFolders;
	}

	public BasicEmailFolder getInboxEmailFolder() {
		return inboxEmailFolder;
	}

	public void setInboxEmailFolder(BasicEmailFolder inboxEmailFolder) {
		this.inboxEmailFolder = inboxEmailFolder;
	}

	public BasicEmailFolder getSentEmailFolder() {
		return sentEmailFolder;
	}

	public void setSentEmailFolder(BasicEmailFolder sentEmailFolder) {
		this.sentEmailFolder = sentEmailFolder;
	}

	public boolean isUsingNexemo() {
		return isUsingNexemo;
	}

	public void setUsingNexemo(boolean isUsingNexemo) {
		this.isUsingNexemo = isUsingNexemo;
	}

	public boolean isAllowingAdditionalUserLevels() {
		return isAllowingAdditionalUserLevels;
	}

	public void setAllowingAdditionalUserLevels(
			boolean isAllowingAdditionalUserLevels) {
		this.isAllowingAdditionalUserLevels = isAllowingAdditionalUserLevels;
	}

	public Map<Class<? extends SmsTemplate>, SmsTemplate> getSmsTemplateMap() {
		return smsTemplateMap;
	}

	public void setSmsTemplateMap(Map<Class<? extends SmsTemplate>, SmsTemplate> smsTemplateMap) {
		this.smsTemplateMap = smsTemplateMap;
	}

	public Map<ScheduledJobEnum, ScheduledJob> getScheduledJobMap() {
		return scheduledJobMap;
	}

	public void setScheduledJobMap(Map<ScheduledJobEnum, ScheduledJob> scheduledJobMap) {
		this.scheduledJobMap = scheduledJobMap;
	}

	public CreditCardType getVisaCardType() {
		return visaCardType;
	}

	public void setVisaCardType(CreditCardType visaCardType) {
		this.visaCardType = visaCardType;
	}

	public boolean isUsingEmailBodyDivider() {
		return isUsingEmailBodyDivider;
	}

	public void setUsingEmailBodyDivider(boolean isUsingEmailBodyDivider) {
		this.isUsingEmailBodyDivider = isUsingEmailBodyDivider;
	}

	public SmsAccount getSmsAccount() {
		return smsAccount;
	}

	public void setSmsAccount(SmsAccount smsAccount) {
		this.smsAccount = smsAccount;
	}

	public SmsAccount getDefaultSmsAccount() {
		return defaultSmsAccount;
	}

	public void setDefaultSmsAccount(SmsAccount defaultSmsAccount) {
		this.defaultSmsAccount = defaultSmsAccount;
	}

	public String getMultipleTabWarning() {
		return multipleTabWarning;
	}

	public void setMultipleTabWarning(String multipleTabWarning) {
		this.multipleTabWarning = multipleTabWarning;
	}

	public String getHighlightColour() {
		return highlightColour;
	}

	public void setHighlightColour(String highlightColour) {
		this.highlightColour = highlightColour;
	}

	public String getMainHeaderColour() {
		return mainHeaderColour;
	}

	public void setMainHeaderColour(String mainHeaderColour) {
		this.mainHeaderColour = mainHeaderColour;
	}

	public String getSubHeaderColour() {
		return subHeaderColour;
	}

	public void setSubHeaderColour(String subHeaderColour) {
		this.subHeaderColour = subHeaderColour;
	}

	/**
	 * @return the isSelectableRowsAllowed
	 */
	public boolean isSelectableRowsAllowed() {
		return isSelectableRowsAllowed;
	}

	/**
	 * @param isSelectableRowsAllowed the isSelectableRowsAllowed to set
	 */
	public void setSelectableRowsAllowed(boolean isSelectableRowsAllowed) {
		this.isSelectableRowsAllowed = isSelectableRowsAllowed;
	}

	public String getSubHeaderTextColour() {
		return subHeaderTextColour;
	}

	public void setSubHeaderTextColour(String subHeaderTextColour) {
		this.subHeaderTextColour = subHeaderTextColour;
	}

	public String getNexemoKey() {
		return nexemoKey;
	}

	public void setNexemoKey(String nexemoKey) {
		this.nexemoKey = nexemoKey;
	}

	public String getNexemoSecretKey() {
		return nexemoSecretKey;
	}

	public void setNexemoSecretKey(String nexemoSecretKey) {
		this.nexemoSecretKey = nexemoSecretKey;
	}
}
