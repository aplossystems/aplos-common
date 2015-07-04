package com.aplos.common.backingpage;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
@GlobalAccess
public class LoginPage extends BackingPage {
	private static final long serialVersionUID = -2631634152382964054L;
	private static Logger logger = Logger.getLogger( LoginPage.class );
	private String username;
	private String password;
	private boolean rememberMe;
	private int failedAttempts = 0;
	private boolean userDisabledByAttempts=false;

	@Override
	public boolean requestPageLoad() {
		super.requestPageLoad();
		return true;
	}

	public void handleFailedAttempt(String username) {
		if (username == null) {
			username = getUsername();
		}
		BeanDao dao = new BeanDao(SystemUser.class);
		dao.addWhereCriteria( "username=:username" );
		dao.setNamedParameter( "username", username );
		SystemUser systemUser = (SystemUser) dao.getFirstBeanResult();
		if (systemUser != null) {
			Integer limit = CommonConfiguration.getCommonConfiguration().getMaxFailedLoginAttempts();
			if (systemUser.isLockedOutByAttempts()) {
				setUserDisabledByAttempts(true);
			} else {
				SystemUser saveableSystemUser = systemUser.getSaveableBean();
				int attempts = saveableSystemUser.addAndReturnFailedLoginAttempt();
				if (limit != null && limit > 0) {
					if (attempts > limit) {
						saveableSystemUser.setLockedOutByAttempts(true);
						setUserDisabledByAttempts(true);
					} else {
						setFailedAttempts(attempts);
						setUserDisabledByAttempts(false);
					}
				} else {
					//this will handle things if we switched off a previous limit
					saveableSystemUser.resetFailedLoginAttempts();
				}
				saveableSystemUser.saveDetails();
			}
		} else {
			JSFUtil.addMessage( "Username or password is incorrect", FacesMessage.SEVERITY_ERROR );
		}
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		if( CommonUtil.isNullOrEmpty( getUsername() ) ) {
			setUsername( (String) JSFUtil.getFromFlashViewMap( AplosScopedBindings.USERNAME ) );
		}
		return true;
	}

	public void goToForgottenPassword() {
		JSFUtil.addToFlashViewMap( AplosAppConstants.USERNAME_PASSTHROUGH, username );
		JSFUtil.redirect( ForgottenPasswordPage.class );
	}

	public String login() {
		setUserDisabledByAttempts(false);
		SystemUser systemUser = getSystemUser();
		if (systemUser == null) {
			handleFailedAttempt(null);
			return null;
		} else if (CommonConfiguration.getCommonConfiguration().isDatabaseOnHold() && (systemUser.getUserLevel() == null || !systemUser.getUserLevel().equals( CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel()))) {
			JSFUtil.addMessage( "Sorry, only the superuser can login while database-hold is in effect", FacesMessage.SEVERITY_ERROR );
			return null;
		} else {
//			HibernateUtil.initialise( systemUser, true );
			JSFUtil.redirect( handleSuccessfulLogin(systemUser) );
			return null;
		}
	}

	public AplosUrl handleSuccessfulLogin(SystemUser systemUser) {
		failedAttempts=0;
		userDisabledByAttempts=false;
		return handleSuccessfulLogin(systemUser, isRememberMe());
	}
	
	public static AplosUrl handleSuccessfulLogin(SystemUser systemUser, boolean rememberMe) {
		if( systemUser.getPasswordExpiryDate() != null && systemUser.getPasswordExpiryDate().before( new Date() ) ) {
			systemUser.addToScope( JsfScope.FLASH_VIEW );
			return new BackingPageUrl(PasswordExpiryPage.class);
		} else {
			Website.removeWebsiteFromTabSession(); //clear out our website so we get it fresh
			logger.info("Website removed from session through login.");
			Website.refreshWebsiteInSession();
			SystemUser saveableUser = systemUser.getSaveableBean();
			saveableUser.saveDetails();
			/*
			 * This was removed because people sometimes get issues with state and then
			 * can't recover with another log in.  The underlying state issues need to be
			 * addressed but not being able to re-log in stops people from accessing the
			 * system in the meantime.
			 */
//			if( !systemUser.isLoggedIn() ) {
				systemUser.login();
//			}
			if (rememberMe) {
				if( systemUser != null ) {
					saveableUser.addCookies();
				}
			}
			JSFUtil.getNavigationStack().clear();
	
			//TODO EXTERNAL REDIRECTION
			String redirectUrl = (String) JSFUtil.getFromFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL);
			JSFUtil.addToFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL, null);
			if (redirectUrl != null) {
				AplosUrl redirectAplosUrl = new AplosUrl(redirectUrl);
				redirectAplosUrl.addQueryParameter( AplosScopedBindings.REDIRECTED_FROM_MEMORY, true );
				return redirectAplosUrl;
			} else {
				return systemUser.determineAfterLoginUrl();
			}
		}
	}

	public String login(SystemUser systemUser) {
		systemUser.login();
//		systemUser.hibernateInitialise( true );
//		JSFUtil.redirect(ApplicationUtil.getAplosContextListener().getAfterLoginUrl(), true);
		return null;
	}

	public static void logout() {
		if (JSFUtil.getLoggedInUser() != null) {
			JSFUtil.setAllowAutoLogin( false );
			JSFUtil.getLoggedInUser().logout();
		}
	}

	public void logoutUser() {
		logout();
	}

	public void logoutAndHome() {
		logout();
		JSFUtil.getNavigationStack().clear();
		JSFUtil.redirect( new AplosUrl( "/", false ), true);
	}

	public void switchUser() {
		logout();
		JSFUtil.getNavigationStack().clear();
		JSFUtil.redirect( new BackingPageUrl(LoginPage.class) );
	}

	public SystemUser getSystemUser() {
		return getSystemUser( getUsername(), getPassword() );
	}

	public SystemUser getSystemUser(String username, String password) {
		SystemUser systemUser = SystemUser.getSystemUserByUsername( username );
//		systemUser = HibernateUtil.getImplementation( systemUser );
		if( systemUser != null ) {
			if( systemUser.validateLogin( password ) ) {
				return systemUser;
			} else {
				// TODO, this should not print out the password but is currently needed for debugging 01 Feb 2012
				logger.warn("SystemUser password '" + password + "' was not recognised when logging in with username '" + username + "', '" + systemUser.getPassword() + "' was in the database" );
			}
		} else {
			logger.warn("SystemUser username '" + username + "' was not recognised when logging in" );
		}

		return null;
	}

	public boolean isUserLoggedIn() {
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if( currentUser != null &&
				currentUser.isLoggedIn() ) {
			return true;
		} else {
			return false;
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword( String password ) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public String getApplicationName() {
		return CommonUtil.firstLetterToUpperCase( ApplicationUtil.getAplosContextListener().getImplementationModule().getModuleName() );
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public boolean isRememberMe() {
		return rememberMe;
	}

	public void setFailedAttempts(int failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public void setUserDisabledByAttempts(boolean userDisabledByAttempts) {
		this.userDisabledByAttempts = userDisabledByAttempts;
	}

	public boolean isUserDisabledByAttempts() {
		return userDisabledByAttempts;
	}

}