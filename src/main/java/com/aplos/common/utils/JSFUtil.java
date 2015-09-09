package com.aplos.common.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.application.NavigationHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.AplosRequestContext;
import com.aplos.common.AplosUrl;
import com.aplos.common.AplosUrl.Protocol;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.BackingPageState;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.BackingPageMetaData;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.TabSessionMap;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.AplosModule;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.NavigationStack;
import com.sun.faces.util.DebugUtil;
import com.sun.faces.util.MessageUtils;

public class JSFUtil {
	private static Logger logger = Logger.getLogger( JSFUtil.class );
	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	
	public static MailServerSettings determineMailServerSettings() {
		if( JSFUtil.isLocalHost() ) {
			CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
			if( commonConfiguration != null ) {
				MailServerSettings mailServerSettings = commonConfiguration.getErrorMailServerSettings();
//				HibernateUtil.initialise(mailServerSettings,false);
				return mailServerSettings;
			} else {
				return null;
			}
		} else {
			SystemUser currentUser = JSFUtil.getLoggedInUser();
			if( currentUser != null && currentUser.isUsingOwnMailServerSettings() && currentUser.getMailServerSettings() != null ) {
				return currentUser.getMailServerSettings();
			}
			Website website = Website.getCurrentWebsiteFromTabSession();
			if( website == null || website.isUsingDefaultMailServerSettings() ) {
				CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
				if( commonConfiguration != null ) {
					MailServerSettings mailServerSettings = commonConfiguration.getMailServerSettings();
//					HibernateUtil.initialise(mailServerSettings, false);
					return mailServerSettings;
				} else {
					return null;
				}
			} else {
				return website.getMailServerSettings();
			}
		}
	}
	
	public static String getMimeType(FacesContext context, String name) {
		String mimeType = context.getExternalContext().getMimeType(name);

		if (mimeType == null) {
			mimeType = DEFAULT_MIME_TYPE;
		}

		return mimeType;
	}
	
	public static boolean isPostBack() {
    	FacesContext facesContext = FacesContext.getCurrentInstance();
    	return facesContext.getRenderKit().getResponseStateManager().isPostback(facesContext);
	}

	public static void printViewRootTree() {
		DebugUtil.printTree(FacesContext.getCurrentInstance().getViewRoot(),System.out);
	}
	
	public static AplosRequestContext getAplosRequestContext( HttpServletRequest request ) {
		return getAplosRequestContext( request, false );
	}
	
	public static AplosRequestContext getAplosRequestContext( HttpServletRequest request, boolean createIfNotExists ) {
		if( request != null ) {
			if( request.getAttribute( AplosScopedBindings.APLOS_REQUEST_CONTEXT ) != null ) {
				return (AplosRequestContext) request.getAttribute( AplosScopedBindings.APLOS_REQUEST_CONTEXT );
			} else {
				if( createIfNotExists ) {
					String requestUri = request.getRequestURI();
					String contextPath = request.getServletContext().getContextPath();
					if( requestUri.startsWith( contextPath ) ) {
						requestUri = requestUri.replaceFirst( contextPath, "" );
					}
					AplosRequestContext aplosRequestContext = new AplosRequestContext( requestUri, request );
					setAplosRequestContext(request, aplosRequestContext);
					return aplosRequestContext;
				}
			}
		}
		return null;
	}
	
	public static AplosRequestContext getAplosRequestContext() {
		return getAplosRequestContext( JSFUtil.getRequest() );
	}
	
	public static void setAplosRequestContext( HttpServletRequest request, AplosRequestContext aplosRequestContext ) {
		request.setAttribute(AplosScopedBindings.APLOS_REQUEST_CONTEXT, aplosRequestContext);
	}
	
	public static void setAplosRequestContext( AplosRequestContext aplosRequestContext ) {
		setAplosRequestContext(JSFUtil.getRequest(), aplosRequestContext);
	}
	
	public static void dispatch( String viewId ) { 
		try {
			FacesContext facesContext = JSFUtil.getFacesContext(); 
	        facesContext.getExternalContext().dispatch( viewId );
	        facesContext.responseComplete();
		} catch( IOException ioex ) {
			ApplicationUtil.getAplosContextListener().handleError( ioex );
		}
	}
	
	public static String getAplosContextOriginalUrl() {
		return getAplosContextOriginalUrl(JSFUtil.getRequest());
	}
	
	public static String getAplosContextOriginalUrl( HttpServletRequest request ) {
		AplosRequestContext aplosRequestContext = getAplosRequestContext( request );
		if( aplosRequestContext != null ) {
			return aplosRequestContext.getOriginalUrl();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends BackingPage> getEditPageClass(Class<? extends AplosAbstractBean> beanClass) {
		Map<String, Class<? extends BackingPage>> editPageClasses = ApplicationUtil.getAplosContextListener().getEditPageClasses();
		String simpleName = beanClass.getSimpleName();
		Class<? extends BackingPage> backingPageClass = editPageClasses.get( simpleName );
		while( backingPageClass == null && beanClass.getSuperclass() != null ) {
			beanClass = (Class<? extends AplosAbstractBean>) beanClass.getSuperclass();
			if( beanClass == AplosAbstractBean.class || beanClass == AplosBean.class ) {
				break;
			}
			if( AplosAbstractBean.class.isAssignableFrom( beanClass.getSuperclass() ) ) {
				backingPageClass = ApplicationUtil.getAplosContextListener().getEditPageClasses().get( beanClass.getSimpleName() );
			} else {
				break;
			}
		}
		return backingPageClass;
	}
	
	public static String determineViewId() {
		FacesContext facesContext = JSFUtil.getFacesContext();
		Map requestMap = facesContext.getExternalContext().getRequestMap();
		String viewId = (String) requestMap.get("javax.servlet.include.path_info");
        if (viewId == null) {
            viewId = facesContext.getExternalContext().getRequestPathInfo();
        }

        // It could be that this request was mapped using
        // a prefix mapping in which case there would be no
        // path_info.  Query the servlet path.
        if (viewId == null) {
            viewId = (String)
              requestMap.get("javax.servlet.include.servlet_path");
        }

        if (viewId == null) {
            viewId = facesContext.getExternalContext().getRequestServletPath();
        }

        if (viewId == null) {
            throw new FacesException(MessageUtils.getExceptionMessageString(
              MessageUtils.NULL_REQUEST_VIEW_ERROR_MESSAGE_ID));
        }
        
        return viewId;
	}

	public static void ajaxRedirect(Class<? extends BackingPage> backingPageClass ) {
		ajaxRedirect( backingPageClass, null );
	}

	public static void ajaxRedirect(Class<? extends BackingPage> backingPageClass, String queryString ) {
		FacesContext context = JSFUtil.getFacesContext();
        Application application = context.getApplication();
		NavigationHandler navHandler = application.getNavigationHandler();

		String backingPageSimpleName = backingPageClass.getSimpleName();

		if( backingPageSimpleName.endsWith( "Page" ) ) {
			backingPageSimpleName = backingPageSimpleName.substring( 0, backingPageSimpleName.indexOf( "Page" ) );
		}
		//we need to figure the whole address otherwise it looks for the view relative to our current location
		//com.aplos.nease.backingpage.projectmain.projectsub.ProjectItemEdit becomes .nease.projectmain.projectsub. and then /nease/projectmain/projectsub/
		//String address = backingPageClass.getName().replace("com.aplos.", "").replace("backingpage.","").replace(backingPageClass.getSimpleName(), ""); 
		String address = backingPageClass.getName().replaceAll("com.aplos.([a-zA-Z]*).backingpage(.*)?." + backingPageClass.getSimpleName(), ".$1$2.");
		StringBuffer urlBuf = new StringBuffer( address.replace(".", "/") );
		urlBuf.append( CommonUtil.firstLetterToLowerCase( backingPageSimpleName ) );
		if ( CommonUtil.isNullOrEmpty( queryString ) ) {
			urlBuf.append( "?jsf-redirect=true" );
		} else {
			urlBuf.append( "?" ).append( queryString ).append( "&jsf-redirect=true" );
		}
        navHandler.handleNavigation(context, null, urlBuf.toString() );

        context.renderResponse();
	}

	public static boolean isAjaxRequest() {
		return JSFUtil.getFacesContext().getPartialViewContext().isPartialRequest();
	}

	public static void setCurrentBackingPage( BackingPage backingPage ) {
		JSFUtil.addToRequest( AplosScopedBindings.BACKING_PAGE, backingPage );
	}

	public static BackingPage getCurrentBackingPage() {
		HttpServletRequest request = JSFUtil.getRequest(); 
		if( request != null ) {
			return (BackingPage) request.getAttribute( AplosScopedBindings.BACKING_PAGE );
		} else {
			return null;
		}
	}

	public static AplosBean getTableBean() {
		return getBeanFromRequest("tableBean");
	}

	public static AplosBean getTableBean(AplosLazyDataModel AqlAplosLazyDataModel) {
		return getBeanFromRequest(CommonUtil.getBinding(AqlAplosLazyDataModel
				.getBeanDao().getBeanClass()));
	}

	public static String getViewId() {
		if (JSFUtil.getFacesContext() == null) {
			return null;
		}
		UIViewRoot viewRoot = JSFUtil.getFacesContext().getViewRoot();
		if (viewRoot == null) {
			return null;
		} else {
			return JSFUtil.getFacesContext().getViewRoot().getViewId();
		}
	}

	public static Map<String, Object> getApplicationMap() {
		if( getExternalContext() != null ) {
			return getExternalContext().getApplicationMap();
		} else {
			return null;
		}
	}

	public static BackingPageState saveBackingPageState(BackingPage backingPage) {
		BackingPageState backingPageState = JSFUtil.getNavigationStack()
				.saveState(backingPage);
		JSFUtil.addToHistoryList(backingPageState);
		return backingPageState;
	}

	public static void redirectToLoginPage() {
		String originalRequestUrl = JSFUtil.getAplosContextOriginalUrl();
		String loginPageUrl = ApplicationUtil.getAplosContextListener()
				.getLoginPageUrl(originalRequestUrl, JSFUtil.getRequest(), true);
		JSFUtil.redirect(new AplosUrl( loginPageUrl, false ), false, true);
	}

	/**
	 * @deprecated
	 * Special case so that we could use it in the view file as it needed a
	 * unique method name, this will be removed once the abstractTagLibraries
	 * are fixed.
	 */
	@Deprecated
	public static boolean isCurrentViewUsingTabSessionsForView() {
		return isCurrentViewUsingTabSessions( true );
	}


	public static boolean isCurrentViewUsingTabSessions() {
		return isCurrentViewUsingTabSessions( determineIsFrontEnd() );
	}

	public static boolean isCurrentViewUsingTabSessions( boolean isFrontEnd ) {
		BackingPage backingPage = JSFUtil.getCurrentBackingPage();
		if( backingPage != null ) {
			BackingPageMetaData backingPageMetaData = ApplicationUtil.getAplosContextListener().getBackingPageMetaData( backingPage.getClass() );
			if ( CommonConfiguration.getCommonConfiguration().isFrontEndTabSessions()
					|| (backingPageMetaData.isRequiresWindowId() && !isFrontEnd) ) {
				return true;
			} else {
				return false;
			}
		} else {
			if( isFrontEnd && !CommonConfiguration.getCommonConfiguration().isFrontEndTabSessions() ) {
				return false;
			} else {
				return true;
			}
		}
	}

	public static URL checkFileLocations(String resourceName, String resourcePath, boolean allowOverride) {
		URL url = null;

		// Check the implementation websites first
		List<Website> websiteList = ApplicationUtil.getAplosContextListener().getWebsiteList();
		for (int i = 0, n = websiteList.size(); i < n && url == null; i++) {
			if (allowOverride || resourceName.startsWith("/" + websiteList.get(i).getPackageName())) {
				url = websiteList.get(i).checkFileLocations(resourceName,	resourcePath);
				if (url != null) {
					break;
				}
			}
		}

		// Check to see has the site extension beforehand and remove
		// This needs to be done after the implementation project has been checked
		// The site extension is sometimes added before to help identify the site and 
		// load the correct site into session. This is primarily used in
		// CommonUtil.getBackingPageUrl.
		List<Website> siteList = ApplicationUtil.getAplosContextListener().getWebsiteList();
		for (Website tempSite : siteList) {
			if (resourceName.startsWith("/" + tempSite.getPackageName())) {
				resourceName = resourceName.replaceFirst("/" + tempSite.getPackageName(), "");
				break;
			}
		}

		// Check the Web Content directory
		if (url == null ) {
			url = JSFUtil.class.getResource("/../../" + resourceName);
		}

		// Then check the aplos modules
		if (url == null) {
			List<AplosModule> aplosModuleList = ApplicationUtil.getAplosContextListener().getAplosModuleList();
			for (int i = 0, n = aplosModuleList.size(); i < n && url == null; i++) {
				if (resourceName.startsWith("/quickview")) {
					//find /quickview/<beanType>.xhtml
					url = aplosModuleList.get(i).checkFileLocations( resourceName.toLowerCase(), resourcePath );
					
					//ignores implementation module...
					//doesnt ignore implementation when website list empty (because that means it is startup and we are loading email bodies)
		        } else if ((websiteList.size() == 0 && siteList.size()==0) || !aplosModuleList.get(i).equals(ApplicationUtil.getAplosContextListener().getImplementationModule())) {
					
					if (allowOverride || resourceName.startsWith("/" + aplosModuleList.get(i).getPackageName())) {
						/*
						 * This if statement makes sure that only the module that
						 * first creates the file is called to check the url, if
						 * allow override is false.
						 */
						url = aplosModuleList.get(i).checkFileLocations(resourceName, resourcePath);
					}
					
					if (url != null) {
						break;
					}
				}
			}
			
			if (url == null && resourceName.startsWith("/quickview")) {
				//loop separately to find default because if we put this check in the above loop
				// /myro/quickView/default would be a higher precedence than /cms/quickView/realizedProduct
				for (int i = 0, n = aplosModuleList.size(); i < n && url == null; i++) {
					url = aplosModuleList.get(i).checkFileLocations( "/default.xhtml", resourcePath + "/quickview" );
					if (url != null) {
						break;
					}
				}
			}

			/*
			 * These seems to be for paths that come in for javax.faces.resource, although
			 * I'm not sure why this folder name is being used instead of the standard scripts folder.
			 */
			if (url == null	&& (allowOverride || resourceName.startsWith("/core"))) {
				url = aplosModuleList
						.get(0)
						.getClass()
						.getResource(
								("/com/aplos/core/" + resourcePath + resourceName)
										.replace("//", "/"));
			}
		}
		
		return url;
	}

	public static void addToTabSession(Object object) {
		addToTabSession( CommonUtil.getBinding(object.getClass()), object );
	}

	public static void addToTabSession(Object object,HttpSession session,boolean isFrontEnd) {
		addToTabSession( CommonUtil.getBinding(object.getClass()), object, session, isFrontEnd );
	}

	public static void addToTabSession(String key, Object value) {
		addToTabSession(key, value, JSFUtil.getSessionTemp());
	}

	public static void addToTabSession(String key, Object value,
			HttpSession session) {
		addToTabSession(key, value, session, determineIsFrontEnd() );
	}

	public static void addToTabSession(String key, Object value, HttpSession session,boolean isFrontEnd) {
		if( session != null ) {
			Map<String, Object> tabSessionMap = getTabSession(session,isFrontEnd);
			if (tabSessionMap != null) {
				tabSessionMap.put(key, value);
			}
		} else {
			Map<String,Object> threadSession =  ApplicationUtil.getThreadSessionMap().get( Thread.currentThread() );
			if( threadSession != null ) {
				threadSession.put( key, value );
			}
		}
	}


	public static void addToTabSession(String key, Object value, HttpSession session,boolean isFrontEnd, String windowId ) {
		Map<String, Object> tabSessionMap = getTabSession(session,isFrontEnd,windowId);
		if (tabSessionMap != null) {
			tabSessionMap.put(key, value);
		}
	}

	public static Object getFromTabSession(String key) {
		return getFromTabSession( key, JSFUtil.getSessionTemp(), determineIsFrontEnd() );
	}

	public static Object getFromTabSession(String key, HttpSession session, boolean isFrontEnd ) {
		if( session != null ) { 
			Map<String, Object> tabSessionMap = getTabSession(session, isFrontEnd);
			if (tabSessionMap != null) {
				return tabSessionMap.get(key);
			}
		} else {
			Map<String,Object> threadSession =  ApplicationUtil.getThreadSessionMap().get( Thread.currentThread() );
			if( threadSession != null ) {
				return threadSession.get( key );
			}
		}

		return null;
	}
	
	public static boolean determineIsFrontEnd() {
		return determineIsFrontEnd(JSFUtil.getRequest());
	}

	public static boolean determineIsFrontEnd(HttpServletRequest request) {
		return ApplicationUtil.getAplosModuleFilterer().determineIsFrontEnd(request, false);
	}

	public static void redirect(Class<? extends BackingPage> backingPageClass) {
		if( JSFUtil.getFacesContext().getPartialViewContext().isPartialRequest() ) {
			redirect(new BackingPageUrl(Website
					.getCurrentWebsiteFromTabSession().getPackageName(), backingPageClass,
					true), true, false);
		} else {
			redirect(new BackingPageUrl(Website
					.getCurrentWebsiteFromTabSession().getPackageName(), backingPageClass,
					true), true, true);
		}
	}

	public static void redirect(String sitePackageName,
			Class<? extends BackingPage> backingPageClass) {
		redirect(new BackingPageUrl(sitePackageName,
				backingPageClass, true), true, true);
	}

	public static void insertContextPath( StringBuffer urlStrBuf ) {
		if( !(getContextPath().endsWith( "/" ) || (urlStrBuf.charAt( 0 ) == '\\') || urlStrBuf.charAt( 0 ) == '/') ) {
			urlStrBuf.insert( 0, "/" );
		}
		urlStrBuf.insert( 0, getContextPath() );
	}

	public static String appendContextPath( String url ) {
		StringBuffer strBuf = new StringBuffer( url );
		if( !(getContextPath().endsWith( "/" ) || ( url.startsWith( "\\" ) || url.startsWith( "/" ) )) ) {
			strBuf.insert( 0, "/" );
		}
		strBuf.insert( 0, getContextPath() );
		return strBuf.toString();
	}

	public static String getWindowId() {
		return getWindowId(JSFUtil.getRequest());
	}

	public static String getWindowId(HttpServletRequest request) {
		String windowId;
		CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
		if( commonConfiguration != null && commonConfiguration.isUsingWindowId() ) {
			windowId = JSFUtil.getRequestParameter(AplosAppConstants.WINDOW_ID);
			if (windowId == null && request != null) {
				if (request.getAttribute(AplosAppConstants.WINDOW_ID) != null) {
					windowId = String.valueOf(request.getAttribute(
							AplosAppConstants.WINDOW_ID));
				}
				if (request.getParameter(AplosAppConstants.WINDOW_ID) != null) {
					windowId = String.valueOf(request.getParameter(
							AplosAppConstants.WINDOW_ID));
				}
			}
	
			if( CommonUtil.isNullOrEmpty( windowId ) || windowId.equals( "null" ) ) {
				return null;
			}
	
			if( windowId.contains( "<" ) ) { 
				windowId = "stripped";
			}
		} else {
			windowId = TabSessionMap.TABLESS_SESSION;
		}
		return windowId;
	}

	public static void redirect(AplosUrl aplosUrl) {
		redirect(aplosUrl, true, true);
	}

	public static void redirect(AplosUrl aplosUrl, boolean addContextPath) {
		redirect(aplosUrl, addContextPath, true);
	}

	public static void redirect( AplosUrl aplosUrl, boolean addContextPath,
			HttpServletResponse response,
			AplosContextListener aplosContextListener ) {
		redirect( aplosUrl, addContextPath, response, aplosContextListener, JSFUtil.getWindowId() );
	}

	public static void redirect( AplosUrl aplosUrl, boolean addContextPath,
			HttpServletResponse response,
			AplosContextListener aplosContextListener, String windowId) {
		try {
			if( !response.isCommitted() ) {
				aplosUrl.addWindowId( windowId );
				aplosUrl.addContextPath( addContextPath );
				aplosUrl.addQueryParameter( AplosAppConstants.SEND_REDIRECT, "true");
				response.sendRedirect( aplosUrl.toString() );
				if( JSFUtil.getFacesContext() != null && aplosUrl.isCompletingResponseAfterRedirect() ) {
					FacesMessagesUtils.saveMessages( JSFUtil.getFacesContext(), JSFUtil.getFacesContext().getExternalContext().getSessionMap() );
					JSFUtil.getFacesContext().responseComplete();
				}
			}
		} catch (IOException ioex) {
			if (aplosContextListener != null) {
				aplosContextListener.handleError(ioex);
			}
		}
	}

	public static void redirect(AplosUrl aplosUrl, boolean addContextPath,
			ExternalContext externalContext,
			AplosContextListener aplosContextListener) {
		try {
			aplosUrl.addWindowId( getWindowId() );
			aplosUrl.addContextPath( addContextPath );
			externalContext.redirect( aplosUrl.toString() );
		} catch (IOException ioex) {
			if (aplosContextListener != null) {
				aplosContextListener.handleError(ioex);
			}
		}
	}

	public static void redirect( String url, boolean addContextPath ) {
		redirect( new AplosUrl( url ), addContextPath );
	}

	public static void redirect( AplosUrl aplosUrl, boolean addContextPath,
			boolean sendRedirect) {
		if( sendRedirect ) {
			redirect(aplosUrl, addContextPath, JSFUtil.getResponse(),
					ApplicationUtil.getAplosContextListener());
		} else {
			redirect(aplosUrl, addContextPath,
					JSFUtil.getExternalContext(), ApplicationUtil.getAplosContextListener());
		}
	}

	public static boolean isLocalHost() {
		return CommonUtil.isLocalHost(JSFUtil.getServletContext());
	}

	public static void httpRedirect( Class<? extends BackingPage> backingPageClass, boolean includeQueryString ) {
		AplosUrl aplosUrl = new BackingPageUrl( backingPageClass);
		if( includeQueryString ) {
			aplosUrl.addCurrentQueryString();
		}
		httpRedirect( aplosUrl, true );
	}

	public static void httpRedirect( AplosUrl aplosUrl ) {
		protocolRedirect( aplosUrl, AplosUrl.Protocol.HTTP );
	}

	public static void httpRedirect( AplosUrl aplosUrl, boolean addContextPath ) {
		aplosUrl.addContextPath( addContextPath );
		httpRedirect(aplosUrl);
	}

	public static void secureRedirect( AplosUrl aplosUrl ) {
		protocolRedirect( aplosUrl, AplosUrl.Protocol.HTTPS );
	}

	public static void secureRedirect( AplosUrl aplosUrl, boolean addContextPath ) {
		aplosUrl.addContextPath( addContextPath );
		secureRedirect(aplosUrl);
	}

	public static void protocolRedirect( AplosUrl aplosUrl, Protocol protocol ) {
		String serverName = JSFUtil.getRequest().getServerName();
		Website currentWebsite = Website.getCurrentWebsiteFromTabSession();
		if( currentWebsite != null && currentWebsite.isSslEnabled() ) {
			aplosUrl.setScheme( protocol.name().toLowerCase() );
			
			if( serverName.equals( "localhost" ) ) {
				if( Protocol.HTTP.equals( protocol ) ) {
					aplosUrl.setHost( "localhost:8080" );
				} else {
					aplosUrl.setHost( "localhost:8443" );
				}
			} else {
				aplosUrl.setHost( serverName );
			}
		}

		HttpServletResponse response = JSFUtil.getResponse();
//		ApplicationUtil.handleError( new Exception( "redirecting" ), false );
//		ApplicationUtil.handleError( new Exception( aplosUrl.toString() ), false );
		if( !response.isCommitted() ) {
			aplosUrl.addWindowId( JSFUtil.getWindowId() );
//				response.setAppCommitted(true);
	        response.resetBuffer();
			response.setContentType("text/html");
			response.setDateHeader("Expires", 0);
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", aplosUrl.toString() );
//			ApplicationUtil.handleError( new Exception( aplosUrl.toString() ), false );
//			response.setSuspended(true);
			
			if( JSFUtil.getFacesContext() != null && aplosUrl.isCompletingResponseAfterRedirect() ) {
				FacesMessagesUtils.saveMessages( JSFUtil.getFacesContext(), JSFUtil.getFacesContext().getExternalContext().getSessionMap() );
				JSFUtil.getFacesContext().responseComplete();
			}
		}
	}
	
	public static boolean schemeRedirectIfRequired( AplosUrl aplosUrl , SslProtocolEnum sslProtocolEnum ) {
		boolean sslEnabled = Website.getCurrentWebsiteFromTabSession().isSslEnabled();
		SslProtocolEnum defaultSslProtocolEnum = Website.getCurrentWebsiteFromTabSession().getDefaultSslProtocolEnum();
		if ( (SslProtocolEnum.FORCE_SSL.equals(sslProtocolEnum) || 
				(SslProtocolEnum.SYSTEM_DEFAULT.equals(sslProtocolEnum) && SslProtocolEnum.FORCE_SSL.equals(defaultSslProtocolEnum))) && 
				!JSFUtil.getRequest().getScheme().contains("https") && 
				sslEnabled) {
			JSFUtil.secureRedirect(aplosUrl);
			return true;
		} else if ( (SslProtocolEnum.FORCE_HTTP.equals(sslProtocolEnum) || 
				(SslProtocolEnum.SYSTEM_DEFAULT.equals(sslProtocolEnum) && SslProtocolEnum.FORCE_HTTP.equals(defaultSslProtocolEnum))) && JSFUtil.getRequest().getScheme().contains("https")) {
			JSFUtil.httpRedirect(aplosUrl);
			return true;
		}
		
		return false;
	}

	public static void secureRedirect(Class<? extends BackingPage> backingPageClass, boolean includeQueryString ) {
		AplosUrl aplosUrl = new BackingPageUrl(backingPageClass);
		if( includeQueryString == true ) {
			aplosUrl.addCurrentQueryString();
		}
		secureRedirect(aplosUrl, true);
	}

	public static void secureRedirect(Class<? extends BackingPage> backingPageClass) {
		secureRedirect(new BackingPageUrl(backingPageClass), true);
	}

	public static void addToView( String binding, Object value ) {
		getViewMap().put( binding, value );
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromView(
			Class<? extends AplosAbstractBean> beanClass) {
		return (T) getBeanFromView(CommonUtil.getBinding(beanClass));
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromView(
			String binding ) {
		return (T) getViewMap().get(binding);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromFlash(
			Class<? extends AplosAbstractBean> beanClass) {
		return (T) getBeanFromFlash(CommonUtil.getBinding(beanClass));
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromFlash( String binding ) {
		return (T) getFromFlashViewMap(binding);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromFlashCurrent( String binding ) {
		return (T) getFromFlashViewMap(binding, true );
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromTabSession(String binding) {
		return (T) getFromTabSession( binding );
	}

	public static Object getFromView(String binding) {
		if (getFacesContext() != null) {
			Map<String, Object> viewMap = getViewMap();
			if( viewMap != null ) {
				return viewMap.get(binding);
			}
		} 
		
		return null;
	}

	public static Object getFromView(Class<?> bindingClass) {
		return getFromView(CommonUtil.getBinding(bindingClass));
	}

	public static Object getFromTabSession(Class<?> bindingClass) {
		return getFromTabSession(CommonUtil.getBinding(bindingClass));
	}
	
	public static Object getFromTabSession(Class<?> bindingClass, HttpServletRequest request) {
		return getFromTabSession(CommonUtil.getBinding(bindingClass), request.getSession(), determineIsFrontEnd());
	}

	public static Object getFromRequest(Class<?> bindingClass) {
		return getFromRequest( CommonUtil.getBinding( bindingClass ) );
	}

	public static Object getFromRequest( String binding ) {
		if (getFacesContext() != null) {
			return getRequest().getAttribute(binding);
		} else {
			return null;
		}
	}

	public static Object getFromApplicationMap(Class<?> bindingClass) {
		if (getFacesContext() != null) {
			return getApplicationMap().get(CommonUtil.getBinding(bindingClass));
		} else {
			return null;
		}
	}

//	@SuppressWarnings("unchecked")
//	public static <T extends AplosBean> T getBeanFromTabSession(
//			Class<? extends AplosBean> beanClass) {
//		return (T) getBeanFromTabSession(CommonUtil.getBinding(beanClass));
//	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromScope(
			Class<? extends AplosAbstractBean> beanClass) {
		return (T) getBeanFromScope(CommonUtil.getBinding(beanClass), AplosAbstractBean.determineScope(beanClass) );
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromScope(
			Class<? extends AplosAbstractBean> beanClass, JsfScope jsfScope ) {
		return (T) getBeanFromScope(CommonUtil.getBinding(beanClass), jsfScope );
	}

	@SuppressWarnings("unchecked")
	public static void removeFromScope(
			Class<? extends AplosAbstractBean> beanClass ) {
		String binding = CommonUtil.getBinding(beanClass);
		JsfScope jsfScope = AplosAbstractBean.determineScope(beanClass);
		if( JsfScope.TAB_SESSION.equals( jsfScope ) ) {
			JSFUtil.addToTabSession( binding, null, JSFUtil.getSessionTemp(), JSFUtil.determineIsFrontEnd() ); 
		} else if( JsfScope.SESSION.equals( jsfScope ) ) {
			JSFUtil.getSessionTemp().removeAttribute( binding ); 
		} else if( JsfScope.VIEW.equals( jsfScope ) ) {
			JSFUtil.addToView( binding, null );
		} else if( JsfScope.FLASH_VIEW.equals( jsfScope ) ) {
			JSFUtil.addToFlashViewMap( binding, null );
		} else if( JsfScope.REQUEST.equals( jsfScope ) ) {
			JSFUtil.addToRequest( binding, null );
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromScope(
			String binding, JsfScope jsfScope ) {
		if( JsfScope.TAB_SESSION.equals( jsfScope ) ) {
			return JSFUtil.getBeanFromTabSession( binding, JSFUtil.getSessionTemp(), JSFUtil.determineIsFrontEnd() ); 
		} else if( JsfScope.SESSION.equals( jsfScope ) ) {
			return (T) JSFUtil.getSessionTemp().getAttribute( binding ); 
		} else if( JsfScope.VIEW.equals( jsfScope ) ) {
			return JSFUtil.getBeanFromView( binding );
		} else if( JsfScope.FLASH_VIEW.equals( jsfScope ) ) {
			return JSFUtil.getBeanFromFlash( binding );
		} else if( JsfScope.FLASH_VIEW_CURRENT.equals( jsfScope ) ) {
			return JSFUtil.getBeanFromFlash( binding );
		} else if( JsfScope.REQUEST.equals( jsfScope ) ) {
			return JSFUtil.getBeanFromRequest( binding );
		}
		return null;
	}
	
	public static final void addToScope( String binding, Object value, JsfScope associatedBeanScope ) {
		if( JsfScope.TAB_SESSION.equals( associatedBeanScope ) ) {
			JSFUtil.addToTabSession( binding, value, JSFUtil.getSessionTemp(), JSFUtil.determineIsFrontEnd() ); 
		} else if( JsfScope.SESSION.equals( associatedBeanScope ) ) {
			JSFUtil.getSessionTemp().setAttribute(binding, value); 
		} else if( JsfScope.VIEW.equals( associatedBeanScope ) ) {
			JSFUtil.getViewMap().put( binding, value );
		} else if( JsfScope.FLASH_VIEW.equals( associatedBeanScope ) ) {
			JSFUtil.addToFlashViewMap( binding, value );
		} else if( JsfScope.FLASH_VIEW_CURRENT.equals( associatedBeanScope ) ) {
			JSFUtil.addToFlashViewMap( binding, value, true );
		} else if( JsfScope.REQUEST.equals( associatedBeanScope ) ) {
			JSFUtil.getRequest().setAttribute( binding, value );
		}
	}


	@SuppressWarnings("unchecked")
	public static <T extends AplosBean> T getBeanFromTabSession( String binding, HttpSession session) {
		return getBeanFromTabSession( binding, session, determineIsFrontEnd() );
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromTabSession( String binding, HttpSession session, boolean isFrontEnd ) {
		if (getFacesContext() != null && session != null) {
			return (T) getTabSession(session,isFrontEnd).get( binding );
		} else {
			Map<String,Object> threadSession =  ApplicationUtil.getThreadSessionMap().get( Thread.currentThread() );
			if( threadSession != null ) {
				return (T) threadSession.get( binding );
			} else {
				return null;
			}
		}
	}

	public static <T extends AplosBean> T getBeanFromTabSession(
			Class<? extends AplosBean> beanClass, HttpSession session) {
		return getBeanFromTabSession( CommonUtil.getBinding(beanClass));
	}

	public static void handlePermissionsFailure() {
		JSFUtil.addMessage(
				"You do not have the correct permissions to access that page",
				FacesMessage.SEVERITY_ERROR);
		FacesContext
				.getCurrentInstance()
				.getApplication()
				.getNavigationHandler()
				.handleNavigation(
						FacesContext.getCurrentInstance(),
						null,
						ApplicationUtil.getAplosContextListener().getLoginPageUrl("",
								getRequest(), true));
	}

	public static void addMessage(String message, Severity severity) {
		if (!CommonUtil.isNullOrEmpty(message) && !message.equals(" ") && JSFUtil.getFacesContext() != null) {
			boolean unique = true;
			for (FacesMessage listedMessage : JSFUtil.getFacesContext().getMessageList()) {
				if (listedMessage.getDetail().equals(message)) {
					unique = false;
					logger.info("Skipped JSF message: already in list. (" + message + ")");
					break;
				}
			}
			if (unique) {
				JSFUtil.getFacesContext().addMessage(null, new FacesMessage(severity, message, message));
				if( JSFUtil.getRequest() != null ) {
					JSFUtil.getRequest().setAttribute("userMessageSize",	JSFUtil.getFacesContext().getMessageList().size());
				}
			}
		}
	}

	public static String getCurrentSimplePath(boolean removeExtension) {
		String currentPath = getCurrentPath( removeExtension );
		String lastBit = currentPath.substring(currentPath.lastIndexOf("/") + 1);
		return lastBit;
	}

	public static String getCurrentPath(boolean removeExtension) {
		return getCurrentPath( removeExtension, false );
	}

	public static String getCurrentPath(boolean removeExtension, boolean replaceXhtmlExtension ) {
//		String viewId = JSFUtil.getExternalContext().getRequestServletPath();
		/*
		 * I was testing some code that meant the view wasn't available
		 * after debugging I found the above code is what populates the 
		 * viewId so I used that directly.  27 Mar 2012
		 * 
		 * However this was not working in the PageBindingPhaseListener
		 * as the viewId is calculated on a number of different conditions
		 * not just the one above.  This is shown in the class RestoreViewPhase in 
		 * the execute method.  In this case the viewRoot actually gets changed
		 * at a later lifecycle or point in the lifecycle which is why the viewId 
		 * and the servlet path will be different.
		 * 
		 * Also the viewId gives a .xhtml instead of the .jsf which is sent in.
		 */
		String viewId;
		
		if( getFacesContext().getViewRoot() == null ) {
			viewId = JSFUtil.getExternalContext().getRequestServletPath();
		} else {
			viewId = getFacesContext().getViewRoot().getViewId();
		}
//		String viewId = JSFUtil.getPrettyFacesOriginalUri().replaceFirst( JSFUtil.getContextPath(), "");

		if (removeExtension) {
			return viewId.substring(0, viewId.indexOf("."));
		} else {
			if( replaceXhtmlExtension ) {
				return viewId.replace( ".xhtml", ".jsf");
			} else {
				return viewId;
			}
		}
	}

	public static void addMessage(String message) {
		addMessage(message, FacesMessage.SEVERITY_INFO);
	}

	public static void addMessageForError(String message) {
		addMessage(message, FacesMessage.SEVERITY_ERROR);
	}

	public static void addMessageForWarning(String message) {
		addMessage(message, FacesMessage.SEVERITY_WARN);
	}

	public static void setLoggedInUser(SystemUser user) {
		JSFUtil.addToTabSession(AplosScopedBindings.CURRENT_USER, user, JSFUtil.getSessionTemp(), true, TabSessionMap.TABLESS_SESSION);
		JSFUtil.addToTabSession(AplosScopedBindings.CURRENT_USER, user, JSFUtil.getSessionTemp(), false, getWindowId());
	}

	public static void setAllowAutoLogin( boolean isAllowAutoLogin ) {
		// Make sure that it's not frontend as this is a backend parameter
		JSFUtil.addToTabSession( AplosScopedBindings.ALLOW_AUTO_LOGIN, isAllowAutoLogin, getSessionTemp(), false );
	}

	public static boolean isAllowAutoLogin() {
		// Make sure that it's not frontend as this is a backend parameter
		Boolean allowAutoLogin = (Boolean) JSFUtil.getFromTabSession( AplosScopedBindings.ALLOW_AUTO_LOGIN, getSessionTemp(), false );
		if ( allowAutoLogin == null || allowAutoLogin ) {
			return true;
		} else {
			return false;
		}
	}

	public static String getBackingPageBinding(String className) {
		return className.substring(0, 1).toLowerCase() + className.substring(1);
	}

	public static String getBinding(Class<?> classObj) {
		return classObj.getSimpleName().substring(0, 1).toLowerCase()
				+ classObj.getSimpleName().substring(1);
	}

	@SuppressWarnings("rawtypes")
	public static Object getFromTabSessionOrCreate(Class classToCreate,
			String sessionBinding) {
		Object tabPanelObj = JSFUtil.getFromTabSession(sessionBinding);
		if (tabPanelObj != null) {
			return tabPanelObj;
		} else {
			tabPanelObj = CommonUtil.getNewInstance(classToCreate, null);
			JSFUtil.addToTabSession(sessionBinding, tabPanelObj);
			return tabPanelObj;
		}
	}

	public static void addToRequest(Object object) {
		addToRequest(CommonUtil.getBinding(object.getClass()), object);
	}

	public static void addToRequest(String binding, Object object) {
		HttpServletRequest request = getRequest();
		if (request != null) {
			getRequest().setAttribute(binding, object);
		}
	}

	public static void addToApplicationMap(Object object) {
		getApplicationMap().put(CommonUtil.getBinding(object.getClass()),
				object);
	}

	public static SystemUser getLoggedInUser() {
		/*
		 * At the moment I've left the system so that only one user can be logged in
		 * at a time, ignoring the tab the user is in, this is so that links from
		 * emails don't ask the user to log in again.  However all logged in users
		 * could be stored in a map and checked to see if they are logged in and then
		 * the windowId could be set to the one associated to the user.
		 */
//		return (SystemUser) JSFUtil.getFromTabSession(AplosScopedBindings.CURRENT_USER, JSFUtil.getSessionTemp(), true);
		return (SystemUser) JSFUtil.getFromTabSession(AplosScopedBindings.CURRENT_USER );
	}

	public static SystemUser getLoggedInUser( HttpSession session ) {
		return (SystemUser) JSFUtil.getFromTabSession(AplosScopedBindings.CURRENT_USER, session, determineIsFrontEnd() );
	}


	public static String getContextPath(boolean removePrecedingSlash) {
		String contextPath = getContextPath();
		if (removePrecedingSlash && contextPath.startsWith("/")) {
			contextPath = contextPath.replaceFirst("/", "");
		}

		return contextPath;
	}

	public static String getContextPath() {
		return ApplicationUtil.getAplosContextListener().getContext().getContextPath();
	}

	public static String getServerUrl() {
		return (String) ApplicationUtil.getAplosContextListener().getServerUrl();
	}

	public static void setVariable(FacesContext facesContext, Object object,
			String scopedBinding) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		ctx.getExternalContext().getSessionMap().put(scopedBinding, object);

		ELContext el = ctx.getELContext();
		ValueExpression ve = createVE(scopedBinding, Object.class);
		ve.setValue(el, object);
	}

	public static Object resolveVariable(Class<?> bindingClass) {
		return resolveVariable(CommonUtil.getBinding(bindingClass));
	}

	public static Object resolveVariable(String scopedBinding) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (ctx != null) {
			ELContext el = ctx.getELContext();
			ValueExpression ve = createVE(scopedBinding, Object.class);
			Object o = ve.getValue(el);
			return o;
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	public static ValueExpression createVE(String varName, Class objClass) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		ExpressionFactory expf = ctx.getApplication().getExpressionFactory();
		ValueExpression ve = expf.createValueExpression(ctx.getELContext(),
				"#{" + varName + "}", objClass);
		return ve;
	}

	public static <T extends AplosAbstractBean> T getBeanFromRequest(
			Class<?> beanClass) {
		return getBeanFromRequest(getBinding(beanClass));
	}

	@SuppressWarnings("unchecked")
	public static <T extends AplosAbstractBean> T getBeanFromRequest(
			String requestBinding) {
		return (T) getRequest().getAttribute(requestBinding);
	}

	public static NavigationStack getNavigationStack() {
		NavigationStack stack = (NavigationStack) getFromTabSession(CommonUtil
				.getBinding(NavigationStack.class));
		if (stack == null) {
			stack = new NavigationStack();
			addToTabSession(CommonUtil.getBinding(NavigationStack.class), stack);
		}
		return stack;
	}

	public static void addToHistoryList(BackingPageState backingPageState) {
		List<BackingPageState> historyList = getHistoryList();
		historyList.add(backingPageState);
		if (historyList.size() > 25) {
			historyList.remove(0);
		}
	}
	
	public static BackingPageState getCurrentStateFromNavigationStack() {
		NavigationStack navigationStack = getNavigationStack();
		if( navigationStack.size() > 0 ) {
			return navigationStack.peek();
		}
		return null;
	}

	public static List<BackingPageState> getHistoryList() {
		return getHistoryList( getSessionTemp() );
	}

	public static List<BackingPageState> getHistoryList( HttpSession session ) {
		@SuppressWarnings("unchecked")
		List<BackingPageState> historyList = (List<BackingPageState>) getFromTabSession(AplosScopedBindings.HISTORY_QUEUE, session, determineIsFrontEnd());
		if (historyList == null) {
			historyList = new ArrayList<BackingPageState>();
			addToTabSession(AplosScopedBindings.HISTORY_QUEUE, historyList);
		}
		return historyList;
	}

	public static void moveFromRequestToSession(Class<?> beanClass) {
		moveFromRequestToSession(getBinding(beanClass));
	}

	public static void moveFromRequestToSession(String configBinding) {
		ExternalContext context = JSFUtil.getExternalContext();
		context.getSessionMap().put(configBinding,
				context.getRequestMap().get(configBinding));
	}

	public static PhaseId getCurrentPhaseId() {
		return FacesContext.getCurrentInstance().getCurrentPhaseId();
	}

	public static ExternalContext getExternalContext() {
		if (FacesContext.getCurrentInstance() != null) {
			return FacesContext.getCurrentInstance().getExternalContext();
		} else {
			return null;
		}
	}

	public static void addToFlashViewMap(String binding, Object value) {
		addToFlashViewMap(binding, value, false);
	}

	public static void addToFlashViewMap(String binding, Object value, boolean isFinalView ) {
		String viewId = JSFUtil.getViewId();
		FlashViewValue flashViewValue = new FlashViewValue(viewId, value);
		flashViewValue.setFinalView( isFinalView );
		getFlashViewMap(JSFUtil.getSessionTemp()).put(binding, flashViewValue);
	}

	public static Object getFromFlashViewMap(String binding) {
		return getFromFlashViewMap(binding,false);
	}

	public static Object getFromFlashViewMap(String binding, boolean onlyIfCurrent) {
		FlashViewValue flashViewValue = getFlashViewMap(JSFUtil.getSessionTemp()).get(binding);
		if (flashViewValue != null && (!onlyIfCurrent || flashViewValue.isFinalView()) ) {
			return flashViewValue.getValue();
		} else {
			return getFromView( binding );
		}
	}

	public static Map<String, Object> getTabSession() {
		return getTabSession(JSFUtil.getSessionTemp(), true,
				determineIsFrontEnd());
	}

	public static Map<String, Object> getTabSession(HttpSession session) {
		return getTabSession(session, true, determineIsFrontEnd());
	}

	public static Map<String, Object> getTabSession(HttpSession session,
			boolean isFrontEnd) {
		return getTabSession(session, true, isFrontEnd);
	}

	public static Map<String, Object> getTabSession(HttpSession session,
			boolean isFrontEnd, String windowId ) {
		return getTabSession(session, true, isFrontEnd, windowId );
	}

	public static Map<String, Object> getTabSession(boolean create,
			boolean isFrontEnd) {
		return getTabSession(JSFUtil.getSessionTemp(), create, isFrontEnd);
	}
	

	

	public static Map<String, Object> getTabSession(HttpSession session, boolean create, boolean isFrontEnd ) {
		String windowId;
		if( JSFUtil.isCurrentViewUsingTabSessions( isFrontEnd ) ) {
			windowId = JSFUtil.getWindowId();
		} else {
			windowId = TabSessionMap.TABLESS_SESSION;
		}
		return getTabSession(session, create, isFrontEnd, windowId);
	}

	public static Map<String, Object> getTabSession(HttpSession session,
			boolean create, boolean isFrontEnd, String windowId ) {
		if( session == null ) {
			return null;
		}

		TabSessionMap tabSession = ((TabSessionMap) session
				.getAttribute(AplosScopedBindings.TAB_SESSION));
		if( tabSession != null ) {
			return tabSession.getTabSession( session, create, isFrontEnd, windowId );
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, FlashViewValue> getFlashViewMap(
			HttpSession session) {
		Map<String, FlashViewValue> flashViewMap = null;
		if (session != null) {
			flashViewMap = (Map<String, FlashViewValue>) session
					.getAttribute(AplosScopedBindings.FLASH_VIEW_MAP);
			if (flashViewMap == null) {
				flashViewMap = new HashMap<String, FlashViewValue>();
				session.setAttribute(AplosScopedBindings.FLASH_VIEW_MAP,
						flashViewMap);
			}
		}

		return flashViewMap;
	}

	/**
	 * Get http session
	 *
	 * @param create
	 *            if true the session will be created if it doesn't exist
	 * @return HttpSession
	 */
	public static HttpSession getSession(boolean create) {
		return (HttpSession) FacesContext.getCurrentInstance()
				.getExternalContext().getSession(create);

	}

	/**
	 * Get or create http session
	 *
	 * @return HttpSession
	 */

  public static ServletContext getServletContext() {
      return ApplicationUtil.getAplosContextListener().getContext();
  }

	/**
	 * Get or create http session
	 *
	 * @return HttpSession
	 */
	public static HttpSession getSessionTemp() {
		return getSessionTemp(true);
	}

	/**
	 * Get or create http session
	 *
	 * @return HttpSession
	 */
	public static HttpSession getSessionTemp(boolean createSession) {
		if (FacesContext.getCurrentInstance() == null) {
			return null;
		}
		return (HttpSession) FacesContext.getCurrentInstance()
				.getExternalContext().getSession(createSession);
	}

	public static HttpServletRequest getRequest() {
		if (FacesContext.getCurrentInstance() == null) {
			return null;
		} else {
			return (HttpServletRequest) FacesContext.getCurrentInstance()
					.getExternalContext().getRequest();
		}
	}

	public static HttpServletResponse getResponse() {
		if (FacesContext.getCurrentInstance() == null) {
			return null;
		} else {
			return (HttpServletResponse) FacesContext.getCurrentInstance()
					.getExternalContext().getResponse();
		}
	}

	/**
	 * Get the faces context
	 *
	 * @return FacesContext
	 */
	public static FacesContext getFacesContext() {
		return FacesContext.getCurrentInstance();
	}

	/**
	 * Get mapped bean determined by its name
	 *
	 * @param name
	 *            the name of the mapped bean
	 * @return Object
	 */
	public static Object getBean(String name) {
		FacesContext fc = FacesContext.getCurrentInstance();
		Object bean = fc.getApplication().getVariableResolver()
				.resolveVariable(fc, name);
		return bean;
	}

	/**
	 * Get a parameter from the http request
	 *
	 * @param name
	 *            the name of the parameter
	 * @return String parameter value
	 */
	public static String getRequestParameter(String name) {
		if (FacesContext.getCurrentInstance() != null) {
			return FacesContext.getCurrentInstance().getExternalContext()
					.getRequestParameterMap().get(name);
		}

		return null;
	}

	/**
	 * Get session attribute
	 *
	 * @param name
	 *            the name of the attribute
	 * @return Object attribute value
	 */
	public static Object getTabSessionAttribute(String name) {
		return getFromTabSession(name);
	}

	/**
	 * Get view map
	 *
	 * @return Map<String,Object> UIViewMap object
	 */
	public static Map<String, Object> getViewMap() {
		if( FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null ) {
			return FacesContext.getCurrentInstance().getViewRoot().getViewMap();
		} else {
			return null;
		}
	}
}
