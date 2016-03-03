package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.ThemeManager;
import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.DiscriminatorType;
import com.aplos.common.annotations.persistence.DiscriminatorValue;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.CreateWebsiteWizardPage;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.beans.translations.SystemUserTranslation;
import com.aplos.common.enums.EmailActionType;
import com.aplos.common.enums.EmailStatus;
import com.aplos.common.enums.EmailType;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.UnsubscribeType;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSmsFinder;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.ForumUser;
import com.aplos.common.interfaces.SaltHolder;
import com.aplos.common.interfaces.SubscriberInter;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@DiscriminatorColumn(
    name="USER_TYPE",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue("SystemUser")
@Cache
@DynamicMetaValueKey(oldKey={"SYSTEM_USER","SYSTEM_USER_FU"})
@BeanScope(scope=JsfScope.TAB_SESSION)
public class SystemUser extends AplosBean implements SaltHolder<SystemUser>, ForumUser, BulkEmailSource, BulkSmsSource, BulkEmailFinder, BulkSmsFinder, EmailFolder, SubscriberInter, BulkSubscriberSource {
	private static final long serialVersionUID = 713600702673037543L;

	@Column(unique=true)
	private String username;
	private String password;
	private String passwordSalt;
	private String cookieSalt;
	
	private String googleSecretKey;

	private String title;
	private String surname;
	private String firstName;

	private boolean isUsingNetEase;
	private String neaseUsername;
	private String neasePassword;

	@Column(columnDefinition="LONGTEXT")
	private String unsubscribeReason;
	//private Long neaseProjectId; //TODO: currently not configurable through Nease Gateway, only through website wizard

	@Column(columnDefinition="LONGTEXT")
	private String emailSignature;

	@OneToOne(fetch=FetchType.LAZY)
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address address;
	private String phone;
	private String mobile;
	private String email;

	// For password reset
	@Column(unique=true)
	private String resetCode;
	private Date resetDate;
	
	private Date passwordExpiryDate;
	private String passwordExpiryCode;

	private String theme;
	@ManyToOne(fetch=FetchType.LAZY)
	private UserLevel userLevel;
	@OneToMany
	private Set<UserLevel> additionalUserLevels = new HashSet<UserLevel>();
	private int failedLoginAttempts = 0;
	private boolean lockedOutByAttempts = false;
	private String defaultAfterLoginUrl;
	@ManyToOne(fetch=FetchType.LAZY)
	private Website defaultWebsite;

	//Language here is an Aplos enum (in common), each entry contains a Locale
	//@Enumerated(EnumType.ORDINAL)
	private String defaultLanguage = "en";
	private boolean isWebsiteVisibilityRestricted;

	@OneToMany(fetch=FetchType.LAZY)
	private List<Website> visibleWebsites = new ArrayList<Website>();

	@CollectionOfElements
	private List<String> allowedIpAddresses = new ArrayList<String>();
	private boolean isRestrictLoginByIpAddress = false;
	private boolean isShownUnreadMessageNotification = true;
	private boolean isUsingOwnMailServerSettings = false;
	@ManyToOne(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private MailServerSettings mailServerSettings;
	private boolean isSubscribed = true;
	private boolean isSmsSubscribed = true;
	private UnsubscribeType unsubscribeType;
	
	private String encryptionSalt;
	
	private Date lastLoggedInDate;
	private Date previousLastLoggedInDate;
	private Date lastPageAccessDate;
	private String lastPageAccessed;
	
	private String assignedColour;

	public SystemUser() {}
	
	@Override
	public String getBulkMessageFinderName() {
		return "All system users";
	}
	
	@Override
	public String getEmailFolderSearchCriteria() {
		return "CONCAT(bean.firstName,' ',bean.surname) LIKE :searchStr";
	}
	
	
	public boolean validateLogin( String plainPassword ) {
		return validateLogin(plainPassword, getPasswordSalt(), getPassword());
	}
	
	@Override
	public SubscriberInter getSourceSubscriber() {
		return this;
	}
	
	@Override
	public void aplosEmailAction(EmailActionType emailActionType, AplosEmail aplosEmail) {	
	}
	
	@Override
	public boolean determineIsSubscribed(AplosEmail aplosEmail) {
		return isSubscribed();
	}
	
	@Override
	public boolean determineIsSmsSubscribed(SmsMessage smsMessage) {
		return isSmsSubscribed();
	}
	
	@Override
	public String getUnsubscribeReason() {
		return null;
	}
	
	public boolean validateLogin( String plainPassword, String passwordSalt, String encryptedPassword ) {
		if( isRestrictLoginByIpAddress() ) {
			boolean isIpAddressRestricted = true;
			for( int i = 0, n = getAllowedIpAddresses().size(); i < n; i++ ) {
				String remoteAddress = FormatUtil.padIpAddress(JSFUtil.getRequest().getRemoteAddr());
				if( getAllowedIpAddresses().get( i ).equals( remoteAddress ) ) {
					isIpAddressRestricted = false;
				}
			}
			
			if( isIpAddressRestricted ) {
				JSFUtil.addMessage( "Login is restricted at IP address " + JSFUtil.getRequest().getRemoteAddr() );
				return false;
			}
		}
		if( CommonConfiguration.getCommonConfiguration().isPasswordEncrypted() ) {
			if( CommonUtil.checkStdEncrypt( plainPassword, passwordSalt, true, encryptedPassword ) ) {
				return true;
			}
		} else {
			if( plainPassword.equals( getPassword() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void saveBean(SystemUser currentUser) {
		if ( getPasswordSalt() == null && CommonConfiguration.getCommonConfiguration().isPasswordEncrypted() ) {
			setPasswordSalt( CommonUtil.getRandomSalt() );
			setPassword( CommonUtil.stdEncrypt( getPassword(), getPasswordSalt(), true ) );
		}
		boolean wasLoggedIn = isLoggedIn();
		super.saveBean(currentUser);

		if (JSFUtil.getFacesContext() != null && JSFUtil.getLoggedInUser() != null && JSFUtil.getLoggedInUser().getId() != null && JSFUtil.getLoggedInUser().getId().equals(getId())) {
			SystemUser systemUser = new BeanDao( SystemUser.class ).get( getId() );
//			systemUser = HibernateUtil.getImplementation( systemUser, true, true );
			if( wasLoggedIn && !isLoggedIn() ) {
				systemUser.login();
			} else {
				JSFUtil.setLoggedInUser( systemUser );
			}
		}
	}
	
	public String getUnreadMessagesNotification() {
		if( isShownUnreadMessageNotification() ) {
			BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
			aplosEmailDao.addWhereCriteria( "bean.emailType = " + EmailType.INCOMING.ordinal() );
			aplosEmailDao.addWhereCriteria( "bean.emailReadDate = null" );
			aplosEmailDao.addWhereCriteria( "bean.emailStatus != " + EmailStatus.COMPLETE.ordinal() );
			aplosEmailDao.addWhereCriteria( "bean.mailServerSettings.id = " + JSFUtil.determineMailServerSettings().getId() );
			int messageCount = aplosEmailDao.getCountAll();
			if( messageCount > 0 ) {
				return " " + messageCount + " unread messages"; 
			}
		}
		return null;
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialiseList( visibleWebsites, fullInitialisation );
//		HibernateUtil.initialise( getAddress(), fullInitialisation );
//		HibernateUtil.initialise( getUserLevel(), fullInitialisation );
//		HibernateUtil.initialise( getDefaultWebsite(), fullInitialisation );
//		HibernateUtil.initialise( getMailServerSettings(), fullInitialisation );
//		HibernateUtil.initialiseSet( getAdditionalUserLevels(), fullInitialisation );
////		Hibernate.initialize(getAllowedIpAddresses());
//	}
	
	@Override
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return null;
	}
	
	@Override
	public Long getMessageSourceId() {
		return getId();
	}

	@Override
	public String getDisplayName() {
		if (getFirstName() != null || getSurname() != null) {
			if (!getFullName().equals(" ")) {
				return getFullName();
			}
		}
		return getUsername();
	}

	public boolean hasClearance( UserLevel userLevel ) {
		return getUserLevel().hasClearance( userLevel.getClearanceLevel() );
	}

	public void setDefaultLanguage(String defaultLanguageCode) {
		this.defaultLanguage = defaultLanguageCode;
	}

	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	@Override
	public String getFullName() {
		return getFullName(CommonUtil.getContextLocale().getLanguage());
	}
	
	public String getFullName(String language) {
		return FormatUtil.getFullName( getFirstName(language), getSurname(language) );
	}

	public static List<SystemUser> getSystemUserList() {
		return new BeanDao( SystemUser.class ).setIsReturningActiveBeans(true).getAll();
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( !getClass().equals( SystemUser.class ) ) {
		// Make sure that it gets added on to this binding as classes may be overriding it
			addToScope(CommonUtil.getBinding( SystemUser.class ), this, associatedBeanScope);
		}
	}
	
	public static SystemUser getSystemUserByUsername( String username ) {
		return getSystemUserByUsername(username, null);
	}

	public static SystemUser getSystemUserByUsername( String username, Boolean isReturningActiveBeans ) {
		BeanDao systemUserDao = new BeanDao(SystemUser.class);
		systemUserDao.addWhereCriteria( "username=:username" ).setIsReturningActiveBeans(isReturningActiveBeans);
		systemUserDao.setNamedParameter( "username", username );
		return systemUserDao.getFirstBeanResult();
	}

	public void addCookies() {
		setCookieSalt( CommonUtil.getRandomSalt() );
		saveDetails();
		Cookie usernameCookie = new Cookie("username", getUsername());
		usernameCookie.setPath("/");
		usernameCookie.setMaxAge( 3600 * 24 * 360 ); //  Set to one year

		Cookie passwordCookie = new Cookie("password", CommonUtil.stdEncrypt( getPassword(), getCookieSalt(), true ));
		passwordCookie.setPath("/");
		passwordCookie.setMaxAge( 3600 * 24 * 360 ); //  Set to one year
		JSFUtil.getResponse().addCookie(usernameCookie);
		JSFUtil.getResponse().addCookie(passwordCookie);
	}

	public String resetPassword() {
		Random r = new Random();
		resetCode = Long.toString( Math.abs( r.nextLong() ), 36 );

		Calendar calendar = Calendar.getInstance();
		calendar.setTime( new Date() );
		calendar.add( Calendar.DATE, 1 );
		resetDate = calendar.getTime();

		return resetCode;
	}

	public boolean isAdmin() {
		//if( userLevel != null && userLevel.getName().equals("Admin")) {
		if (userLevel != null && (userLevel.equals(CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel()) || userLevel.equals(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel()))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean canUpgrade() {
		return false;
	}

	public void login() {
		SystemUser saveableSystemUser = getSaveableBean();
		if( getFailedLoginAttempts() != 0 ) {
			saveableSystemUser.resetFailedLoginAttempts();
		}
		saveableSystemUser.updateLastLoggedInDate( new Date() );
		saveableSystemUser.saveDetails();
		JSFUtil.getSessionTemp().setMaxInactiveInterval( ApplicationUtil.getAplosContextListener().getMaxInactiveInterval() );
		JSFUtil.setLoggedInUser( this );
		// Initialise the menu system so that user level access will work correctly
		ApplicationUtil.getAplosContextListener().getMenuCacher().getMainTabPanel( Website.getCurrentWebsiteFromTabSession() );
		getUserLevel().updateAccessPages( ApplicationUtil.getAplosContextListener() );
		ApplicationUtil.getAplosContextListener().systemUserLoggedIn(this);
		JSFUtil.getAplosRequestContext().setMenuProcessed(false);
		JSFUtil.getAplosRequestContext().determineTabPanelState();
		if( theme != null ) {
			ThemeManager.getThemeManager().setTheme(theme);
		}
	}

	public void logout() {
		ApplicationUtil.getAplosContextListener().systemUserLoggedOut(this);
		JSFUtil.setLoggedInUser( null );
	}

	public void setUserLevelId(Long userLevelId) {
//		if( HibernateUtil.isSessionFactoryInitialised() ) {
			userLevel = new BeanDao( UserLevel.class ).get(userLevelId);
//		}
	}
	
	// Overridable
	public UserLevel determineUserLevel() {
		return getUserLevel();
	}

	public static int getDefaultClearance() {
		return 0;
	}

	public String getDetailsHeader() {
		if (isNew()) {
			return "New System User";
		} else {
			return "Edit System User (" + getId() + ")";
		}
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
	
	public void updatePassword( String password ) {
		setPassword( password );
		setPasswordSalt( null );
	}

	public void setPassword(String password) {
		if (password != "") {
			this.password = password;
		}
	}

	public String getPassword() {
		return password;
	}

	public void setFirstName(String firstName) {
		SystemUserTranslation trans = getAplosTranslation();
		if (trans != null) {
			trans.setFirstName(firstName);
		} else {
			this.firstName = firstName;
		}
	}

	public String getFirstName(String language) {
		SystemUserTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			if (trans.getFirstName() != null && !trans.getFirstName().equals("")) {
				return trans.getFirstName();
			}
		}
		return firstName;
	}

	
	@Override
	public String getFirstName() {
		return getFirstName(CommonUtil.getContextLocale().getLanguage());
	}

	public void setSurname(String surname) {
		SystemUserTranslation trans = getAplosTranslation();
		if (trans != null) {
			trans.setSurname(surname);
		} else {
			this.surname = surname;
		}
	}

	public String getSurname(String language) {
		SystemUserTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			return trans.getSurname();
		}
		return surname;
	}

	public String getSurname() {
		return getSurname(CommonUtil.getContextLocale().getLanguage());
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhone() {
		return phone;
	}

	public AplosUrl determineAfterLoginUrl() {
		if( getDefaultAfterLoginUrl() != null ) {
			return new AplosUrl( getDefaultAfterLoginUrl() );
		} else {
			Long websiteCount = (Long) ApplicationUtil.getFirstResult("SELECT COUNT(id) FROM " + AplosBean.getTableName(Website.class))[ 0 ];
			if (websiteCount == null || websiteCount.intValue() < 1) {
				return new BackingPageUrl( "common", CreateWebsiteWizardPage.class, true );
			}
			Website website = Website.getCurrentWebsiteFromTabSession();
			if( isWebsiteVisibilityRestricted() ) {
				if( !getVisibleWebsites().contains( website ) ) {
					if( getDefaultWebsite() != null ) {
						website = getDefaultWebsite();
					} else {
						if( getVisibleWebsites().size() > 0 ) {
							website = getVisibleWebsites().get( 0 );
						}
					}
				}
			}
			return ApplicationUtil.getAplosContextListener().getAfterLoginUrl( website );
		}
	}

	public void setThemeAndSetInThemeManager(String theme) {
		setTheme( theme );
		saveDetails();
		ThemeManager.getThemeManager().setTheme(theme);
	}

	@Override
	public boolean isLoggedIn() {
		return equals( JSFUtil.getLoggedInUser() );
	}


	public boolean isSuperuser() {
		return getUserLevel() != null && getUserLevel().equals(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel());
	}

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public void setUserLevel( UserLevel userLevel ) {
		this.userLevel = userLevel;
	}

	public UserLevel getUserLevel() {
		return userLevel;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress( Address address ) {
		this.address = address;
	}

	public String getTitle(String language) {
		SystemUserTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			return trans.getTitle();
		}
		return title; //it cannot be null unless language is default
	}

	public String getTitle() {
		return getTitle(CommonUtil.getContextLocale().getLanguage());
	}

	public void setTitle( String title ) {
		SystemUserTranslation trans = getAplosTranslation();
		if (trans != null) {
			trans.setTitle(title);
		} else {
			this.title = title;
		}
	}

	public String getAssignedColour() {
		return assignedColour;
	}

	public void setAssignedColour(String assignedColour) {
		this.assignedColour = assignedColour;
	}

	public String getResetCode() {
		return resetCode;
	}

	public void setResetCode( String resetCode ) {
		this.resetCode = resetCode;
	}

	public Date getResetDate() {
		return resetDate;
	}

	public void setResetDate( Date resetDate ) {
		this.resetDate = resetDate;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getMobile() {
		return mobile;
	}

	public void setNeaseUsername(String neaseUsername) {
		this.neaseUsername = neaseUsername;
	}

	public String getNeaseUsername() {
		return neaseUsername;
	}

	public void setNeasePassword(String neasePassword) {
		this.neasePassword = neasePassword;
	}

	public String getNeasePassword() {
		return neasePassword;
	}

	public boolean isSubscribed() {
		return isSubscribed;
	}

	public void setSubscribed(boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}

//	public void setFailedLoginAttempts(int failedLoginAttempts) {
//		this.failedLoginAttempts = failedLoginAttempts;
//	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public int addAndReturnFailedLoginAttempt() {
		//if (getUserLevel() == null || !getUserLevel().equals(CommonConfiguration.getCommonConfiguration().getSuperUserLevel())) {
			failedLoginAttempts++;
		//}
		return failedLoginAttempts;
	}

	public void resetFailedLoginAttempts() {
		failedLoginAttempts=0;
	}

	public void setLockedOutByAttempts(boolean isLockedOutByAttempts) {
		if (!this.lockedOutByAttempts && !isLockedOutByAttempts) {
			//looks like we are unlocking them so reset their attempts
			resetFailedLoginAttempts();
		}
		this.lockedOutByAttempts = isLockedOutByAttempts;
	}

	public boolean isLockedOutByAttempts() {
		return lockedOutByAttempts;
	}

	public void setVisibleWebsites(List<Website> visibleWebsites) {
		this.visibleWebsites = visibleWebsites;
	}

	public List<Website> getVisibleWebsites() {
		return visibleWebsites;
	}

	public void setWebsiteVisibilityRestricted(boolean isWebsiteVisibilityRestricted) {
		this.isWebsiteVisibilityRestricted = isWebsiteVisibilityRestricted;
	}

	public boolean isWebsiteVisibilityRestricted() {
		return isWebsiteVisibilityRestricted;
	}

	public void setDefaultAfterLoginUrl(String defaultAfterLoginUrl) {
		this.defaultAfterLoginUrl = defaultAfterLoginUrl;
	}

	public String getDefaultAfterLoginUrl() {
		return defaultAfterLoginUrl;
	}

	public void setDefaultWebsite(Website defaultWebsite) {
		this.defaultWebsite = defaultWebsite;
	}

	public Website getDefaultWebsite() {
		return defaultWebsite;
	}

	public String getPasswordSalt() {
		return passwordSalt;
	}

	public void setPasswordSalt(String passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	public String getCookieSalt() {
		return cookieSalt;
	}

	public void setCookieSalt(String cookieSalt) {
		this.cookieSalt = cookieSalt;
	}

	@Override
	public String getSourceUniqueDisplayName() {
		if ( !CommonUtil.isNullOrEmpty( getFullName() ) ) {
			return username + " / " + getFullName() + " (" + email + ")";
		} else {
			return username + " (" + email + ")";
		}
	}

	@Override
	public List<BulkEmailSource> getEmailAutoCompleteSuggestions(String searchString, Integer limit) {
		BeanDao systemUserDao = new BeanDao( SystemUser.class );
		systemUserDao.setIsReturningActiveBeans( true );
		List<BulkEmailSource> systemUsers = null;
		if( searchString != null ) {
			systemUserDao.addWhereCriteria( "CONCAT(firstName,' ',surname) like :similarSearchText OR username like :similarSearchText OR email like :similarSearchText" );
			if( limit != null ) {
				systemUserDao.setMaxResults(limit);
			}
			systemUserDao.setNamedParameter("similarSearchText", "%" + searchString + "%");
			systemUsers = (List<BulkEmailSource>) systemUserDao.getAll();
		} else {
			systemUsers = systemUserDao.getAll();
		}
		return systemUsers;
	}

	@Override
	public List<BulkSmsSource> getSmsAutoCompleteSuggestions(String searchString, Integer limit) {
		BeanDao systemUserDao = new BeanDao( SystemUser.class );
		systemUserDao.setIsReturningActiveBeans( true );
		List<BulkSmsSource> systemUsers = null;
		if( searchString != null ) {
			systemUserDao.addWhereCriteria( "CONCAT(firstName,' ',surname) like :similarSearchText OR username like :similarSearchText OR mobile like :similarSearchText" );
			if( limit != null && limit > 0 ) {
				systemUserDao.setMaxResults( limit );
			}
			systemUserDao.setNamedParameter("similarSearchText", "%" + searchString + "%");
			systemUsers = (List<BulkSmsSource>) systemUserDao.getAll();
		} else {
			systemUsers = systemUserDao.getAll();
		}
		return systemUsers;
	}

	@Override
	public InternationalNumber getInternationalNumber() {
		return InternationalNumber.parseMobileNumberStr(getMobile());
	}

	@Override
	public String getEmailAddress() {
		return getEmail();
	}

	@Override
	public String getFinderSearchCriteria() {
		return "(CONCAT(bean.firstName,' ',bean.surname) LIKE :similarSearchText OR bean.email LIKE :similarSearchText)";
	}

	@Override
	public String getAlphabeticalSortByCriteria() {
		return "bean.firstName ASC, bean.surname ASC";
	}

	public boolean isUsingNetEase() {
		return isUsingNetEase;
	}

	public void setUsingNetEase(boolean isUsingNetEase) {
		this.isUsingNetEase = isUsingNetEase;
	}

	public List<String> getAllowedIpAddresses() {
		return allowedIpAddresses;
	}

	public void setAllowedIpAddresses(List<String> allowedIpAddresses) {
		this.allowedIpAddresses = allowedIpAddresses;
	}

	public boolean isRestrictLoginByIpAddress() {
		return isRestrictLoginByIpAddress;
	}

	public void setRestrictLoginByIpAddress(boolean isRestrictLoginByIpAddress) {
		this.isRestrictLoginByIpAddress = isRestrictLoginByIpAddress;
	}

	public Set<UserLevel> getAdditionalUserLevels() {
		return additionalUserLevels;
	}

	public void setAdditionalUserLevels(Set<UserLevel> additionalUserLevels) {
		this.additionalUserLevels = additionalUserLevels;
	}

	public MailServerSettings getMailServerSettings() {
		return mailServerSettings;
	}

	public void setMailServerSettings(MailServerSettings mailServerSettings) {
		this.mailServerSettings = mailServerSettings;
	}

	public boolean isUsingOwnMailServerSettings() {
		return isUsingOwnMailServerSettings;
	}

	public void setUsingOwnMailServerSettings(boolean isUsingOwnMailServerSettings) {
		this.isUsingOwnMailServerSettings = isUsingOwnMailServerSettings;
	}

	public void setUnsubscribeReason(String unsubscribeReason) {
		this.unsubscribeReason = unsubscribeReason;
	}

	public String getEmailSignature() {
		return emailSignature;
	}

	public void setEmailSignature(String emailSignature) {
		this.emailSignature = emailSignature;
	}

	public boolean isShownUnreadMessageNotification() {
		return isShownUnreadMessageNotification;
	}

	public void setShownUnreadMessageNotification(
			boolean isShownUnreadMessageNotification) {
		this.isShownUnreadMessageNotification = isShownUnreadMessageNotification;
	}

	public Date getPasswordExpiryDate() {
		return passwordExpiryDate;
	}

	public void setPasswordExpiryDate(Date passwordExpiryDate) {
		this.passwordExpiryDate = passwordExpiryDate;
	}

	public String getPasswordExpiryCode() {
		return passwordExpiryCode;
	}

	public void setPasswordExpiryCode(String passwordExpiryCode) {
		this.passwordExpiryCode = passwordExpiryCode;
	}

	public boolean isSmsSubscribed() {
		return isSmsSubscribed;
	}

	@Override
	public void setSmsSubscribed(boolean isSmsSubscribed) {
		this.isSmsSubscribed = isSmsSubscribed;
	}

	@Override
	public UnsubscribeType getUnsubscribeType() {
		return unsubscribeType;
	}

	@Override
	public void setUnsubscribeType(UnsubscribeType unsubscribeType) {
		this.unsubscribeType = unsubscribeType;
	}

	/**
	 * @return the encryptionSalt
	 */
	public String getEncryptionSalt() {
		return encryptionSalt;
	}

	/**
	 * @param encryptionSalt the encryptionSalt to set
	 */
	public void setEncryptionSalt(String encryptionSalt) {
		this.encryptionSalt = encryptionSalt;
	}

	public Date getLastLoggedInDate() {
		return lastLoggedInDate;
	}

	public void setLastLoggedInDate(Date lastLoggedInDate) {
		this.lastLoggedInDate = lastLoggedInDate;
	}

	public void updateLastLoggedInDate(Date lastLoggedInDate) {
		setPreviousLastLoggedInDate( getLastLoggedInDate() );
		setLastLoggedInDate( lastLoggedInDate );
	}

	public Date getLastPageAccessDate() {
		return lastPageAccessDate;
	}

	public void setLastPageAccessDate(Date lastPageAccessDate) {
		this.lastPageAccessDate = lastPageAccessDate;
	}

	public String getLastPageAccessed() {
		return lastPageAccessed;
	}

	public void setLastPageAccessed(String lastPageAccessed) {
		this.lastPageAccessed = lastPageAccessed;
	}

	public Date getPreviousLastLoggedInDate() {
		return previousLastLoggedInDate;
	}

	public void setPreviousLastLoggedInDate(Date previousLastLoggedInDate) {
		this.previousLastLoggedInDate = previousLastLoggedInDate;
	}

	public String getGoogleSecretKey() {
		return googleSecretKey;
	}

	public void setGoogleSecretKey(String googleSecretKey) {
		this.googleSecretKey = googleSecretKey;
	}
}
