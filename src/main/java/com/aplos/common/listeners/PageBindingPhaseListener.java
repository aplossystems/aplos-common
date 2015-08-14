package com.aplos.common.listeners;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.BackingPageOverride;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.DatabaseLockedPage;
import com.aplos.common.backingpage.LoginPage;
import com.aplos.common.backingpage.PaidFeaturePage;
import com.aplos.common.beans.PageRequest;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FacesMessagesUtils;
import com.aplos.common.utils.FlashViewValue;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

public class PageBindingPhaseListener implements PhaseListener {
	private static final long serialVersionUID = -4507684080052102142L;
	private static HashMap<String, Class<? extends BackingPage>> bindingOverrideMap = new HashMap<String, Class<? extends BackingPage>>();
	private static HashMap<String, Class<? extends BackingPage>> reverseBindingOverrideMap = new HashMap<String, Class<? extends BackingPage>>();
	private static HashMap<String, String> viewOverrideMap = new HashMap<String, String>();
	private static HashMap<String, String> printTemplateOverrideMap = new HashMap<String, String>();
	private static PageAccessListener pageAccessListener = new PageAccessListener();
	private Logger logger = Logger.getLogger(PageBindingPhaseListener.class);

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	public static HashMap<String, Class<? extends BackingPage>> getBindingOverrideMap() {
		return bindingOverrideMap;
	}

	public static void addBindingOverride(
			Class<? extends BackingPage> originalBackingPageClass,
			Class<? extends BackingPage> overridingBackingPageClass) {
		String originalBinding = CommonUtil
				.firstLetterToLowerCase(originalBackingPageClass
						.getSimpleName());
		String overrideBinding = CommonUtil
				.firstLetterToLowerCase(overridingBackingPageClass
						.getSimpleName());
		bindingOverrideMap.put(originalBinding, overridingBackingPageClass);
		reverseBindingOverrideMap
				.put(overrideBinding, originalBackingPageClass);
	}

	public static void addViewOverride(String originalPackageName,
			Class<? extends BackingPage> originalBackingPageClass,
			String overridingPackageName,
			Class<? extends BackingPage> overridingBackingPageClass) {
		String originalUrl = new BackingPageUrl(originalPackageName,
				originalBackingPageClass, false).toString();
		String overridingUrl = new BackingPageUrl(overridingPackageName,
				originalBackingPageClass, false).toString();
		viewOverrideMap.put(originalUrl, overridingUrl);
	}

	public static BackingPage resolveBackingPage(
			Class<? extends BackingPage> backingPageClass) {
		return resolveBackingPage(CommonUtil.getBinding(backingPageClass));
	}

	public static BackingPage resolveBackingPage(String binding) {
		if (bindingOverrideMap.containsKey(binding)) {
			binding = CommonUtil.firstLetterToLowerCase(bindingOverrideMap.get(
					binding).getSimpleName());
		}

		FacesContext context = FacesContext.getCurrentInstance();
		try {
			return context.getApplication().evaluateExpressionGet(context,
					"#{" + binding + "}", BackingPage.class);
		} catch( IllegalArgumentException iaex ) {
			return null;
		}

	}

	public static Class<? extends BackingPage> resolveBackingPageClass(
			Class<? extends BackingPage> backingPageClass) {
		String binding = CommonUtil.getBinding(backingPageClass);
		if (bindingOverrideMap.containsKey(binding)) {
			return bindingOverrideMap.get(binding);
		} else {
			return backingPageClass;
		}
	}

	public static Class<? extends BackingPage> reverseResolveBackingPageClass(
			Class<? extends BackingPage> backingPageClass) {
		String binding = CommonUtil.getBinding(backingPageClass);
		if (reverseBindingOverrideMap.containsKey(binding)) {
			return reverseBindingOverrideMap.get(binding);
		} else {
			return backingPageClass;
		}
	}

	public static BackingPage reverseResolveBackingPage(String binding) {
		if (reverseBindingOverrideMap.containsKey(binding)) {
			binding = CommonUtil
					.firstLetterToLowerCase(reverseBindingOverrideMap.get(
							binding).getSimpleName());
		}

		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().evaluateExpressionGet(context,
				"#{" + binding + "}", BackingPage.class);

	}

	public static void addPrintTemplateOverride(
			Class<? extends PrintTemplate> originalPrintTemplateClass,
			Class<? extends PrintTemplate> overridingPrintTemplateClass) {
		getPrintTemplateOverrideMap().put(originalPrintTemplateClass.getName(),
				overridingPrintTemplateClass.getName());
	}

	@Override
	public void beforePhase(PhaseEvent event) {
//		 CommonUtil.timeTrial( "before " + " " + event.getPhaseId() );
		logger.debug( "before phase " + " " + event.getPhaseId() );
		try {
			/*
			 * \ |*| Bind the backing page before the view is rendered so that
			 * the binding |*\ is available for components to use. \
			 */
	        FacesContext facesContext = event.getFacesContext();
	        
	        FacesMessagesUtils.saveMessages(facesContext, facesContext.getExternalContext().getSessionMap());
	        
			if (event.getPhaseId() == PhaseId.RESTORE_VIEW) {
				AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext();
				if( aplosRequestContext != null ) {
					if( aplosRequestContext.getDynamicViewEl() != null 
							&& !aplosRequestContext.isDynamicViewProcessed() ) {
						aplosRequestContext.redirectToDynamicView();
					}
				}
				

				if( JSFUtil.getRequest() != null ) {
					Enumeration<String> enumeration = JSFUtil.getRequest().getParameterNames();
					String tempRequestKey;

					List<String> requestCommandParameters;
					if( JSFUtil.getFromTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST  ) != null ) {
						requestCommandParameters = (List<String>) JSFUtil.getFromTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST  );
					} else {
						requestCommandParameters = new ArrayList<String>();	
						JSFUtil.addToTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST, requestCommandParameters );
					}
					StringBuffer commandBuf = new StringBuffer( "[");
					while( enumeration.hasMoreElements() ) {
						tempRequestKey = enumeration.nextElement();
						if( !CommonUtil.isNullOrEmpty( JSFUtil.getRequestParameter( tempRequestKey ) )
								&& JSFUtil.getRequestParameter( tempRequestKey ).equals( tempRequestKey ) ) {
							if( JSFUtil.isAjaxRequest() ) {
								tempRequestKey = "(" + JSFUtil.getRequest().getParameter( "javax.faces.source" ) + ") " + tempRequestKey; 
							}
							if( commandBuf.length() > 1 ) {
								commandBuf.append( "," );
							}
							commandBuf.append( tempRequestKey );
						}
					}
					if( commandBuf.length() == 1 ) {
						commandBuf.append( JSFUtil.getRequest().getRequestURI() );
					}
					commandBuf.append("] ").append( FormatUtil.formatTime( new Date() ) );
					requestCommandParameters.add( commandBuf.toString() );
				}

//				String sendRedirect = JSFUtil.getRequest().getParameter(
//						AplosAppConstants.SEND_REDIRECT);
//				if (!"true".equals(sendRedirect)) {
//					FacesContext.getCurrentInstance().renderResponse();
//				}
			} else if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {

				/*
				 * This needs to be set because otherwise it breaks in Safari
				 * browser as JSF tries to set the charset. However it's been
				 * removed as it was then breaking JSF when we moved to
				 * integrate Facelets.
				 */
				// response.setHeader("Content-Type", "text/html");
				PageRequest pageRequest;
				if( JSFUtil.getAplosRequestContext() != null ) {
					pageRequest = JSFUtil.getAplosRequestContext().getPageRequest();
				} else {
					pageRequest = new PageRequest();
				}
				pageRequest.updateTimeMarker();
				pageRequest.setRenderResponseBeforeBindTime(System.nanoTime() - pageRequest.getTimeMarker());
				if( pageAccessListener.checkAccess(event ) ) {
					clearOldFlashViewMapEntries();
	
					String uri = JSFUtil.getCurrentPath( false );
					JSFUtil.getAplosRequestContext().determineTabPanelState();
					bindBackingPage(uri, event.getPhaseId());
				}
				pageRequest.setRenderResponseAfterBindTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();

	            /*
	             * Check to see if we are "naturally" in the RENDER_RESPONSE phase.
	             * If we have arrived here and the response is already complete,
	             * then the page is not going to show up: don't display messages
	             * yet.
	             */
	            if (!facesContext.getResponseComplete())
	            {
	            	FacesMessagesUtils.restoreMessages(facesContext, facesContext.getExternalContext().getSessionMap());
	            }
			}

		} catch (Exception e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
	}

	public void clearOldFlashViewMapEntries() {
		Map<String, FlashViewValue> flashViewMap = JSFUtil
				.getFlashViewMap(JSFUtil.getSessionTemp());
		String viewId = JSFUtil.getViewId();
		List<String> keyList = new ArrayList<String>(flashViewMap.keySet());
		FlashViewValue tempFlashViewValue;
		for ( int i = keyList.size() - 1; i > -1; i-- ) {
			tempFlashViewValue = flashViewMap.get( keyList.get( i ) );
			if( tempFlashViewValue.getCurrentViewId() == null ) {
				tempFlashViewValue.setCurrentViewId( JSFUtil.getViewId() );
			}
			if (!tempFlashViewValue.getCurrentViewId().equals(viewId)) {
				JSFUtil.addToView( keyList.get( i ), tempFlashViewValue.getValue());
				flashViewMap.remove(keyList.get( i ));
			}
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
//		 CommonUtil.timeTrial( "after " + " " + event.getPhaseId() + " " + JSFUtil.getAplosContextOriginalUrl() );
		logger.debug( "after phase " + " " + event.getPhaseId() );
		try {
			/*
			 * Bind the backing page again after restoring the view so that when
			 * the user has navigated to the page via the browsers back button,
			 * the binding is correct.
			 */
			PageRequest pageRequest;
			if( JSFUtil.getAplosRequestContext() != null ) {
				pageRequest = JSFUtil.getAplosRequestContext().getPageRequest();
			} else {
				pageRequest = new PageRequest();
			}
			if (event.getPhaseId() == PhaseId.RESTORE_VIEW) {
				pageRequest.setRestoreViewBeforeBindTime(System.nanoTime() - pageRequest.getPageRequestedTime());
				pageRequest.updateTimeMarker();
				if( pageAccessListener.checkAccess(event ) ) {
					if( event.getFacesContext().getViewRoot() != null ) {
						String uri = event.getFacesContext().getViewRoot().getViewId();
		
						bindBackingPage(uri, event.getPhaseId());
					}
				}
				// JSFUtil.addToTabSession("responsePageLoaded",false);
				pageRequest.setRestoreViewAfterBindTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
				
				if( JSFUtil.getLoggedInUser() != null ) {
					SystemUser saveableSystemUser = JSFUtil.getLoggedInUser().getSaveableBean();
					saveableSystemUser.setLastPageAccessDate( new Date() );
					saveableSystemUser.setLastPageAccessed( pageRequest.getPageUrl() );
					saveableSystemUser.saveDetails();
				}
				
			} else if (event.getPhaseId() == PhaseId.APPLY_REQUEST_VALUES) {
				pageRequest.setApplyRequestValuesTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
			} else if (event.getPhaseId() == PhaseId.PROCESS_VALIDATIONS) {
				pageRequest.setProcessValidationsTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
			} else if (event.getPhaseId() == PhaseId.UPDATE_MODEL_VALUES) {
				pageRequest.setUpdateModelValuesTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
			} else if (event.getPhaseId() == PhaseId.INVOKE_APPLICATION) {
				pageRequest.setInvokeApplicationTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
			} else if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
				pageRequest.setRenderResponseCompleteTime(System.nanoTime() - pageRequest.getTimeMarker());
				pageRequest.updateTimeMarker();
//				if( JSFUtil.getResponse().getHeaders( "Set-Cookie" ).size() != 0 ) {
//					if( !CommonUtil.getStringOrEmpty( JSFUtil.getRequest().getHeader( "user-agent" ) ).contains( "uptimerobot") ) {
//						StringBuffer cookieBuffer = new StringBuffer();
//						for( String header : JSFUtil.getResponse().getHeaders( "Set-Cookie" ) ) {
//							cookieBuffer.append( header ).append( ";" );
//						}
//						ApplicationUtil.handleError( new Exception( "SetCookie " + cookieBuffer.toString() ), false ); 
//					}
//				}

//				BackingPage backingPage = JSFUtil.getCurrentBackingPage();
//				if( backingPage instanceof EditPage ) {
//					((EditPage) backingPage).responsePageLoaded();
//				}
			}

			FacesContext facesContext = event.getFacesContext();
			if( !PhaseId.RENDER_RESPONSE.equals(event.getPhaseId()) ) {
	            FacesMessagesUtils.saveMessages(facesContext, facesContext.getExternalContext().getSessionMap());
			}
		} catch (Exception e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
	}

	private void bindBackingPage(String uri, PhaseId phaseId) {
		try {
			// Check if it's a web page
			if (uri.endsWith(".jsp") || uri.endsWith(".jsf")
					|| uri.endsWith(".xhtml")) {
				/*
				 * The response may be complete if page access was refused.
				 */
				if (!JSFUtil.getFacesContext().getResponseComplete()
						&& !uri.contains("loginScreen")
						&& !uri.contains("usermodule")) {
					setNoCacheHeaders();

					/*
					 * This is needed because the first page that loads will
					 * come into the RESTORE_VIEW phase before the website has
					 * been initiated by the PageAccessListener in
					 * RENDER_RESPONSE.
					 */
					if (Website.getCurrentWebsiteFromTabSession() == null) {
						Website.refreshWebsiteInSession();
					}
					// Assume the bean is managed and get it from the faces
					// context
					// because it may not be in the session yet.

					String simpleName;
					if (uri.startsWith("/cmsGenerationViews/cmsPageRevisions")) {
						simpleName = "content";
					} else {
						simpleName = JSFUtil.getCurrentSimplePath(true);
					}

					// if it IS numeric then we are looking at a cms page
					// revision (file), we would
					// then get an invalid binding like 2Page or 175Page, for
					// now we dont have to bind
					// anything though I'm leaving this TODO in case we find we
					// need to bind ContentPage
					if (!CommonUtil.validatePositiveInteger(simpleName)) {
						String binding = simpleName + "Page";
						BackingPage backingPage = resolveBackingPage(binding);
						if (backingPage == null) {
							JSFUtil.addToTabSession(
									AplosScopedBindings.BACKING_PAGE, null);
							return;
						} else {
							JSFUtil.setCurrentBackingPage(backingPage);
							if (phaseId.equals(PhaseId.RENDER_RESPONSE)) {
								if ( Website.getCurrentWebsiteFromTabSession() != null && Website.getCurrentWebsiteFromTabSession().isSslEnabled() ) {
									BackingPageUrl backingPageUrl = new BackingPageUrl( reverseResolveBackingPageClass(backingPage.getClass()));
									backingPageUrl.addCurrentQueryString();
									backingPageUrl.addContextPath( true );
									if( JSFUtil.schemeRedirectIfRequired(  backingPageUrl , backingPage.getSslProtocolEnum() ) ) {
										return;
									}
								}
								/*
								 * You don't want to save the backingPageState for the frontend as it has a large impact on the RAM
								 * required for the system.
								 */
								if (backingPage.shouldStackOnNavigation() && !JSFUtil.isAjaxRequest() && !JSFUtil.determineIsFrontEnd() ) {
									JSFUtil.saveBackingPageState(backingPage);
								}

								// check access rights, ignore the check for
								// pagefeaturepage and login pages
								// this does not check access rights of specific
								// users, this is handled separately by
								// pageaccesslistener
								// TODO - caching?
								if (!backingPage.isGlobalAccess() && !backingPage.ignoresPaidFeatureCheck()
										&& (JSFUtil.getLoggedInUser() == null || !JSFUtil
												.getLoggedInUser()
												.getUserLevel()
												.equals(CommonConfiguration
														.retrieveUserLevelUtil()
														.getSuperUserLevel()))) {
									if (!TabClass.get(backingPage.getClass()).isPaidForAndAccessible()) {
										BackingPageOverride backingPageOverride = backingPage.getClass().getAnnotation( BackingPageOverride.class ); 
										if( !(backingPageOverride != null 
												&& TabClass.get(backingPageOverride.backingPageClass()).isPaidForAndAccessible()) ) {
											JSFUtil.addMessageForError("Sorry, the "
													+ FormatUtil
															.breakCamelCase(backingPage
																	.getClass()
																	.getSimpleName())
													+ " you have requested is not available.");
											JSFUtil.redirect(PaidFeaturePage.class);
											return;	
										}
									}
								}

								if( backingPage.requiredStateCheck(true) ) {
									backingPage.responsePageLoad();
								}

							} else { 

								AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext();
								if( aplosRequestContext != null 
										&& aplosRequestContext.isSessionInvalid() 
										&& !backingPage.isGlobalAccess() ) {
									aplosRequestContext.handleSessionTimeout();
								} else {
									// we do this check in request not response as
									// we don't want any saving to happen once a
									// database is locked
									// because superuser ignores this we can still
									// switch db lock back off
									if (CommonConfiguration
											.getCommonConfiguration()
											.isDatabaseOnHold()) {
										// superuser ignores this constraint, if its
										// global access you dont need to be logged
										// in
										if (!backingPage.isGlobalAccess()
												&& !(backingPage instanceof LoginPage)
												&& !(backingPage instanceof DatabaseLockedPage)
												&& (JSFUtil.getLoggedInUser() == null || !JSFUtil
														.getLoggedInUser()
														.getUserLevel()
														.equals(CommonConfiguration
																.retrieveUserLevelUtil()
																.getSuperUserLevel()))) {
											JSFUtil.redirect(DatabaseLockedPage.class);
											return;
										}
									}

									String sendRedirect = JSFUtil
											.getRequest()
											.getParameter(
													AplosAppConstants.SEND_REDIRECT);
									if (!"true".equals(sendRedirect)) {
										if( backingPage.requiredStateCheck(false) ) {
											backingPage.requestPageLoad();
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		}
	}

	public static void setNoCacheHeaders() {
		HttpServletResponse response = JSFUtil.getResponse();
		response.addHeader("Pragma", "no-cache");
		response.addHeader("Cache-Control", "no-cache");
		// Stronger according to blog comment below that references HTTP spec
		response.addHeader("Cache-Control", "no-store");
		response.addHeader("Cache-Control", "must-revalidate");
		// some date in the past
		response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");
	}

	public static HashMap<String, String> getPrintTemplateOverrideMap() {
		return printTemplateOverrideMap;
	}
}
