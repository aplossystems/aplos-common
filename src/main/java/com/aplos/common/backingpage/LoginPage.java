package com.aplos.common.backingpage;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

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
import com.google.common.io.BaseEncoding;

@ManagedBean
@ViewScoped
@GlobalAccess
public class LoginPage extends BackingPage {
	private static final long serialVersionUID = -2631634152382964054L;
	private static Logger logger = Logger.getLogger( LoginPage.class );
	private String username;
	private String password;
	private boolean rememberMe;
	private int failedAttempts = 0;
	private boolean userDisabledByAttempts=false;
	private boolean isShowingSecondAuthentication = false;
	private Long googleAuthenticationCode;

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
		if(!isShowingSecondAuthentication) {
			setUserDisabledByAttempts(false);
			SystemUser systemUser = getSystemUser();
			if (systemUser == null) {
				handleFailedAttempt(null);
			} else if (CommonConfiguration.getCommonConfiguration().isDatabaseOnHold() && (systemUser.getUserLevel() == null || !systemUser.getUserLevel().equals( CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel()))) {
				JSFUtil.addMessage( "Sorry, only the superuser can login while database-hold is in effect", FacesMessage.SEVERITY_ERROR );
			} else {
				if(CommonConfiguration.getCommonConfiguration().isUsingGoogleAuthentication() && !CommonUtil.isNullOrEmpty(systemUser.getGoogleSecretKey())) {
					setShowingSecondAuthentication(true);
				} else {
					JSFUtil.redirect(handleSuccessfulLogin(systemUser));
				}
			}
		} else if(isShowingSecondAuthentication) {
			SystemUser systemUser = getSystemUser();
			Long t = new Date().getTime() / TimeUnit.SECONDS.toMillis(30);
			try {
				if( checkCode(systemUser.getGoogleSecretKey(), googleAuthenticationCode, t) ) {
					JSFUtil.redirect(handleSuccessfulLogin(systemUser));
					return null;
				}
			} catch( Exception ex ) {
				ApplicationUtil.handleError( ex, false );
			}
			JSFUtil.addMessageForWarning("Sorry, the google code did not match");
		}
		return null;
	}
	
	private static boolean checkCode( String secret, long code, long t)
	    throws NoSuchAlgorithmException,
	      InvalidKeyException {
	  byte[] decodedKey = BaseEncoding.base32().decode(secret);

	  // Window is used to check codes generated in the near past.
	  // You can use this value to tune how far you're willing to go. 
	  int window = 3;
	  for (int i = -window; i <= window; ++i) {
	    long hash = verify_code(decodedKey, t + i);

	    if (hash == code) {
	      return true;
	    }
	  }

	  // The validation code is invalid.
	  return false;
	}

	private static int verify_code(
	  byte[] key,
	  long t)
	  throws NoSuchAlgorithmException,
	    InvalidKeyException {
	  byte[] data = new byte[8];
	  long value = t;
	  for (int i = 8; i-- > 0; value >>>= 8) {
	    data[i] = (byte) value;
	  }

	  SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
	  Mac mac = Mac.getInstance("HmacSHA1");
	  mac.init(signKey);
	  byte[] hash = mac.doFinal(data);

	  int offset = hash[20 - 1] & 0xF;
	  
	  // We're using a long because Java hasn't got unsigned int.
	  long truncatedHash = 0;
	  for (int i = 0; i < 4; ++i) {
	    truncatedHash <<= 8;
	    // We are dealing with signed bytes:
	    // we just keep the first byte.
	    truncatedHash |= (hash[offset + i] & 0xFF);
	  }

	  truncatedHash &= 0x7FFFFFFF;
	  truncatedHash %= 1000000;

	  return (int) truncatedHash;
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

	public boolean isShowingSecondAuthentication() {
		return isShowingSecondAuthentication;
	}

	public void setShowingSecondAuthentication(boolean isShowingSecondAuthentication) {
		this.isShowingSecondAuthentication = isShowingSecondAuthentication;
	}

	public Long getGoogleAuthenticationCode() {
		return googleAuthenticationCode;
	}

	public void setGoogleAuthenticationCode(Long googleAuthenticationCode) {
		this.googleAuthenticationCode = googleAuthenticationCode;
	}

}