package com.aplos.common.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.event.PhaseEvent;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.CreateWebsiteWizardPage;
import com.aplos.common.backingpage.LoginPage;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.TabSessionMap;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.module.AplosModule;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class PageAccessListener {
	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

	private enum LogInRestriction {
		NO_CURRENT_USER ( "No current user" ),
		WEBSITE_NOT_VISIBLE ( "Website not visible to current user" ),
		NO_USER_LEVEL ( "User does not have a user level" ),
		NO_USER_LEVEL_ACCESS ( "Users level does not allow access" ),
		USER_NOT_LOGGED_IN ( "User is not logged in");

		private String reason;

		private LogInRestriction( String reason ) {
			this.reason = reason;
		}

		public String getReason() {
			return reason;
		}
	}

	public PageAccessListener() {
	}
	
	public void shareSessionwideLogin(HttpServletRequest request) {
		if (JSFUtil.getLoggedInUser() == null || !JSFUtil.getLoggedInUser().isLoggedIn()) {
			if (request.getParameter("sessionLogin") != null && Boolean.parseBoolean(request.getParameter("sessionLogin"))) {
				TabSessionMap tabSessionMap = ((TabSessionMap) request.getSession().getAttribute(AplosScopedBindings.TAB_SESSION));
				Iterator iterator = tabSessionMap.keySet().iterator();
				while (iterator.hasNext()) {
					Map<String, Object> tabSession = (Map<String, Object>) iterator.next();
					SystemUser systemUser = (SystemUser) tabSession.get(AplosScopedBindings.CURRENT_USER);
					if (systemUser != null && systemUser.isLoggedIn()) {
						//in some cases we may only want a specific user (for security)
						Object specificUserId = request.getParameter("suid"); //system-user-id
						if (specificUserId == null || specificUserId.equals(systemUser.getId())) {
							systemUser.addToScope( JsfScope.TAB_SESSION ); //add to our new/current tab session	
						}
					}
				}
			}
		}
	}
	
	public boolean checkAccess(PhaseEvent event) {
		AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext();
		if( aplosRequestContext != null ) {
			LogInRestriction logInRestriction = null;
			AplosContextListener aplosContextListener = ApplicationUtil.getAplosContextListener();
			
			HttpServletResponse response = (HttpServletResponse) event.getFacesContext().getExternalContext().getResponse();
			HttpServletRequest request = (HttpServletRequest) event.getFacesContext().getExternalContext().getRequest();
			request.getSession();
			Website.refreshWebsiteInSession();
			String url = JSFUtil.getCurrentPath( false, true );
			
			if( !aplosRequestContext.isAccessAllowed() ) {
				
				if( CommonConfiguration.getCommonConfiguration().isUsingWindowId() ) {
					checkWindowId( request );
				}
				
				shareSessionwideLogin(request);
		
				boolean allowCookieLogin = true;
				if( "false".equals( JSFUtil.getRequestParameter( AplosAppConstants.ALLOW_COOKIE_LOGIN ) ) ) {
					allowCookieLogin = false;
				}
				if (JSFUtil.getLoggedInUser() == null || !JSFUtil.getLoggedInUser().isLoggedIn()) {
					if ( allowCookieLogin && cookieLogin() ) {
						if( (JSFUtil.getContextPath() + url).equals( aplosContextListener.getLoginPageUrl(request.getRequestURI(), request,true))) {
							//TODO EXTERNAL REDIRECTION
							String redirectUrl = (String) JSFUtil.getFromFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL);
							JSFUtil.addToFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL, null);
							if (redirectUrl != null) {
								JSFUtil.redirect( new AplosUrl(redirectUrl), true, response, aplosContextListener );
							} else {
								JSFUtil.redirect( JSFUtil.getLoggedInUser().determineAfterLoginUrl(), true, response, aplosContextListener );
							}
							aplosRequestContext.setAccessAllowed(true);
							return true;
						}
					}
				}
		
				List<String> globalAccessPages = new ArrayList<String>();
				updateGlobalAccessPages(globalAccessPages,aplosContextListener);
		
				if (accessRestricted(url,globalAccessPages)) {
					SystemUser sysUser = JSFUtil.getLoggedInUser();
					if ( (sysUser == null || !sysUser.isLoggedIn()) && JSFUtil.isAllowAutoLogin() && aplosContextListener.isAutoLogin()) {
						aplosContextListener.autoLogin(event.getFacesContext());
						sysUser = JSFUtil.getLoggedInUser();
					}
					if (sysUser != null) {
						Website website = Website.getCurrentWebsiteFromTabSession();
						if( sysUser.isWebsiteVisibilityRestricted() ) {
							if( !sysUser.getVisibleWebsites().contains( website ) ) {
								logInRestriction = LogInRestriction.WEBSITE_NOT_VISIBLE;
							}
						} else if (sysUser.isLoggedIn()) {
							if (aplosContextListener.isPageAccessChecked()) {
								if (sysUser.getUserLevel() == null) {
									logInRestriction = LogInRestriction.NO_USER_LEVEL;
								} else {
									if( "true".equals( JSFUtil.getRequestParameter(AplosScopedBindings.ADD_WEBSITE_FOR_ACCESS) ) ) {
										if( Website.getCurrentWebsiteFromTabSession() != null ) {
											url = "/" + Website.getCurrentWebsiteFromTabSession().getPackageName() + url;
										}
									}
									boolean isAccessAllowed = sysUser.getUserLevel().checkAccess(url);
									if( !isAccessAllowed && sysUser.getAdditionalUserLevels().size() > 0 ) {
										for( UserLevel additionalUserLevel : sysUser.getAdditionalUserLevels() ) {
											isAccessAllowed = additionalUserLevel.checkAccess(url);
											if( isAccessAllowed ) {
												break;
											}
										}
									}
									if (!isAccessAllowed) {
										logInRestriction = LogInRestriction.NO_USER_LEVEL_ACCESS;
									}
								}
							}
						} else {
							logInRestriction = LogInRestriction.USER_NOT_LOGGED_IN;
						}
					} else {
						logInRestriction = LogInRestriction.NO_CURRENT_USER;
					}
				}
			}
	
			if (logInRestriction != null) {
				if (JSFUtil.getFromFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL) == null) {
					if( JSFUtil.getRequestParameter(AplosScopedBindings.REMEMBER_REDIRECT ) == null || !JSFUtil.getRequestParameter(AplosScopedBindings.REMEMBER_REDIRECT ).equals( "false" ) ) {
						if( CommonUtil.isNullOrEmpty( AplosScopedBindings.REDIRECTED_FROM_MEMORY ) || !logInRestriction.equals( LogInRestriction.NO_USER_LEVEL_ACCESS ) ) {
							JSFUtil.addToFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL, url + "?" + request.getQueryString());	
						}
					}
					if( JSFUtil.getRequestParameter(AplosScopedBindings.USERNAME ) != null ) {
						JSFUtil.addToFlashViewMap(AplosScopedBindings.USERNAME, JSFUtil.getRequestParameter(AplosScopedBindings.USERNAME ));
					}
				}
				JSFUtil.redirect(new AplosUrl(aplosContextListener.getLoginPageUrl(url,JSFUtil.getRequest(),true)),false);
				JSFUtil.addMessage( "Access is restricted to " + url + " please login : " + logInRestriction.getReason() );
//				ApplicationUtil.handleError( new Exception( logInRestriction.getReason() ), false );
			} else {
				aplosRequestContext.setAccessAllowed(true);
				return true;
			}
			aplosRequestContext.setAccessAllowed(false);
		}
		return false;
	}
	
	public void checkWindowId( HttpServletRequest request ) {
		AplosContextListener aplosContextListener = ApplicationUtil.getAplosContextListener();
		String windowId = JSFUtil.getWindowId();
		if( JSFUtil.getAplosContextOriginalUrl() != null && JSFUtil.getAplosContextOriginalUrl().contains( "/common/windowIdUpdater.jsf" ) ) {
			return;
		} else if( windowId == null ) {
			if ( JSFUtil.isCurrentViewUsingTabSessions() ) {
				Boolean isWindowIdSet = (Boolean) JSFUtil.getSessionTemp().getAttribute( "isWindowIdSet" );
				String referrerUrl = JSFUtil.getExternalContext().getRequestHeaderMap().get( "referrer" );
				if( referrerUrl != null && referrerUrl.contains( "windowId=" ) ) {
					referrerUrl.substring( referrerUrl.indexOf( "windowId=" ) + 9 );
					windowId = referrerUrl;
					if( referrerUrl.contains( "&" ) ) {
						windowId = referrerUrl.substring( 0, referrerUrl.indexOf( "&" ) );
					}
					JSFUtil.getRequest().setAttribute( AplosAppConstants.WINDOW_ID, new String[] { windowId } );
				} else if( isWindowIdSet == null && windowId == null ) {
					windowId = "0";
					JSFUtil.getRequest().setAttribute( AplosAppConstants.WINDOW_ID, windowId );
					JSFUtil.getSessionTemp().setAttribute( "isWindowIdSet", true );
				} else {
					if (JSFUtil.getFromFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL) != null) {
						JSFUtil.addToFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL, JSFUtil.getFromFlashViewMap(AplosScopedBindings.EXTERNAL_LOGIN_REDIRECTION_URL));
					}
					//requestContext was null and causing us to loop on the 3d secure page because of set attribute (below)
					//ive assumed going to windowidupdater is pointless without a return url
					AplosRequestContext requestContext = JSFUtil.getAplosRequestContext( request );
					if (requestContext != null) {
						JSFUtil.redirect(new AplosUrl( JSFUtil.getContextPath() + "/common/windowIdUpdater.jsf" ), false );
						JSFUtil.getSessionTemp().setAttribute( 
								AplosScopedBindings.WINDOW_ID_REDIRECTION_URL, 
								aplosContextListener.getContext().getContextPath() +
								requestContext.getCurrentUrlWithQueryString() 
						);
					}
					return;
				}
			}
		}
	}

	/** This is used frontend only, cms/backend pages are handled by the dynamic tabs **/
	public void updateGlobalAccessPages(List<String> globalAccessPages, AplosContextListener aplosContextListener) {
		List<AplosModule> aplosModuleList = ApplicationUtil.getAplosContextListener().getAplosModuleList();
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			aplosModuleList.get( i ).addGlobalAccessPages( globalAccessPages );
		}
	}

	public boolean accessRestricted(String url,List<String> globalAccessPages) {
		//dont check access for cms page revisions
		//Note: if in the future we need to start checking this we need to do it without the
		// 'binding' method below as 2Page, 175Page, etc is invalid
		//if (JSFUtil.getFacesContext().getViewRoot().getViewId().contains(CmsWorkingDirectory.CMS_PAGE_REVISION_VIEW_FILES.getDirectoryPath().replace(AplosWorkingDirectory.SERVER_WORK_DIR.getDirectoryPath(), ""))) {
		if (CommonUtil.validatePositiveInteger(JSFUtil.getCurrentSimplePath( true ))) {
			return false;
		}
		String binding = JSFUtil.getCurrentSimplePath( true ) + "Page";
		BackingPage backingPage = PageBindingPhaseListener.resolveBackingPage( binding );
		if( backingPage != null && backingPage.isGlobalAccess() ) {
			return false;
		}

		if (url.indexOf("?") > -1) {
			url = url.substring(0,url.indexOf("?")-1);
		}

		if (url.endsWith(".jsf") || url.endsWith(".jsp")) {
			for (int i = 0, n = globalAccessPages.size(); i < n; i++) {
				if (url.matches(globalAccessPages.get(i))) {
					return false;
				}
			}
			
			Website currentWebsite = Website.getCurrentWebsiteFromTabSession();
			if( currentWebsite != null && !CommonUtil.isNullOrEmpty( currentWebsite.getPackageName() ) ) {
				if( url.startsWith( "/" + currentWebsite.getPackageName() ) ) {
					url = url.substring( currentWebsite.getPackageName().length() + 1 );
				}
			}
			for (int i = 0, n = globalAccessPages.size(); i < n; i++) {
				if (url.matches(globalAccessPages.get(i))) {
					return false;
				}
			}

			return true;
		}
		return false;
	}

	public boolean cookieLogin() {
		Cookie cookies[] = ((HttpServletRequest)JSFUtil.getFacesContext().getExternalContext().getRequest()).getCookies();
		if (cookies != null && cookies.length > 1  ) {
			if ( JSFUtil.isAllowAutoLogin() ) {
				String cookieUsername = "";
				String cookiePassword = "";
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("username")) {
						cookieUsername = cookie.getValue();
					} else if (cookie.getName().equals("password")) {
						cookiePassword = cookie.getValue();
					}
				}
				// This is to save the db request if the cookies aren't set
				if( cookieUsername.equals( "" ) || cookiePassword.equals( "" ) ) {
					return false;
				}
				BeanDao systemUserBeanDao = new BeanDao(SystemUser.class);
				systemUserBeanDao.addWhereCriteria("bean.username = '" + cookieUsername + "'");
				SystemUser systemUser = (SystemUser) systemUserBeanDao.getFirstBeanResult();


				if ( systemUser != null && systemUser.validateLogin( systemUser.getPassword(), systemUser.getCookieSalt(), cookiePassword )) {
					((LoginPage)JSFUtil.resolveVariable(CommonUtil.getBinding(LoginPage.class))).login(systemUser);

					//copied from ln146 in loginpage (otherwise when cookie loggin in to teletest we select binorig)
					Website.removeWebsiteFromTabSession(); //clear out ourwebsite so we get it fresh
					//Logger.info("Website removed from session through login.");
					Website.refreshWebsiteInSession();

						
					String currentUrl = JSFUtil.getCurrentPath( false, true );
					if ( !(currentUrl.endsWith( "sessionTimeout.jsf" ) || currentUrl.endsWith( "loginScreen.jsf" ) 
							|| currentUrl.endsWith( "login.jsf" ) || currentUrl.startsWith( "/cmsGenerationViews/" )) ) {
						/*
						 *  This used to redirect to the URL, but it was removed as it doesn't seem to make sense
						 *  to redirect the user to a different page from the one they are trying to access, just 
						 *  becuase the app has logged them in. 
						 */
						LoginPage.handleSuccessfulLogin(systemUser, false);

						if( Website.getCurrentWebsiteFromTabSession() == null ) {
							JSFUtil.redirect( new BackingPageUrl( "common", CreateWebsiteWizardPage.class, true ) );
						}
					}
					
					return true;
				}
			}
		}
		return false;
	}

}
