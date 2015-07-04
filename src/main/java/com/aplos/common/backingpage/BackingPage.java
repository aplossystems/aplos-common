package com.aplos.common.backingpage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.TrailDisplayName;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.DynamicBundleEntry;
import com.aplos.common.beans.ScannerHook;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.interfaces.DataTableStateCreator;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.PersistentContextTransaction;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

public abstract class BackingPage implements Serializable, DataTableStateCreator {
	
	private static final long serialVersionUID = 8885397464697854975L;

	private static Logger logger = Logger.getLogger( BackingPage.class );
	private BeanDao aqlBeanDao;
	private String returnPage;
	private boolean prependMainFormId = false;
	private String pageDisplayName = null;
	private String scannedStr;
	private boolean disableFormSubmitOnEnter = true;
	private Set<String> requiredStateBindings = new HashSet<String>();
	private JsfScope associatedBeanScope;
	private Long associatedBeanId;
	private Long alternateAssociatedBeanId;

	public BackingPage() {
		AssociatedBean associatedBeanAnnotation = null;
		Class<BackingPage> backingPageClass = (Class<BackingPage>) getClass();
		while( associatedBeanAnnotation == null && BackingPage.class.isAssignableFrom( backingPageClass ) ) {
			associatedBeanAnnotation = backingPageClass.getAnnotation(AssociatedBean.class);
			backingPageClass = (Class<BackingPage>) backingPageClass.getSuperclass();
		}
		
		if( associatedBeanAnnotation != null ) {
			if( associatedBeanAnnotation.beanClass() != null ) {
				setBeanDao( createBeanDao( associatedBeanAnnotation.beanClass() ) );
			}
			if( associatedBeanAnnotation.scope() != null ) {
				Class<? extends AplosAbstractBean> beanClass = null;
				if( getBeanDao() != null ) {
					beanClass = getBeanDao().getBeanClass();
				}
				setAssociatedBeanScope( determineScope( getClass(), getBeanDao().getBeanClass() ) );
			} else if( getBeanDao() != null ) {
				setAssociatedBeanScope( AplosAbstractBean.determineScope( getBeanDao().getBeanClass() ) );
			}
			
		}
	}
	
	public boolean isTabHighlighted() {
		return false;
	}
	
	public static JsfScope determineScope( Class<? extends BackingPage> backingPageClass, Class<? extends AplosAbstractBean> beanClass ) {
		if( backingPageClass != null ) {
			AssociatedBean associatedBeanAnnotation = backingPageClass.getAnnotation(AssociatedBean.class);
			if( associatedBeanAnnotation != null && !associatedBeanAnnotation.scope().equals( JsfScope.NOT_SELECTED ) ) {
				return associatedBeanAnnotation.scope();
			}
		} 
		if( beanClass != null ) {
			return AplosAbstractBean.determineScope(beanClass);
		} else {
			return JsfScope.FLASH_VIEW;
		}
	}
	
	public boolean isShowingNoFollow() {
		return true;
	}
	 
	public BeanDao createBeanDao( Class<? extends AplosAbstractBean> beanClass ) {
		return new BeanDao( beanClass );
	}
	
	public boolean isAdminUser() {
		return JSFUtil.getLoggedInUser().isAdmin();
	}
	
	public boolean isSuperUser() {
		return JSFUtil.getLoggedInUser().isSuperuser();
	}
	
	public boolean isShowingMobileHeadCode() {
		return false;
	}
	
	public boolean requestPageLoad() {
		return true;
	}
	
	public void addRequiredStateBinding( Class<?> bindingClass ) {
		getRequiredStateBindings().add( CommonUtil.getBinding( bindingClass ) );
	}
	
	public boolean requiredStateCheck( boolean isCallFromResponsePhase ) {
		return requiredStateCheck(getRequiredStateBindings());
	}
	
	public boolean requiredStateCheck(Set<String> bindingsToCheck) {
		FacesContext context = JSFUtil.getFacesContext();
		boolean isRequiredStateCheckPassed = true;
		if( bindingsToCheck != null ) {
			for( String binding : bindingsToCheck ) {
				if( context.getApplication().evaluateExpressionGet(context, "#{" + binding + "}", Object.class ) == null ) {
					isRequiredStateCheckPassed = false;
					break;
				}
			}
		}
		
		if( !isRequiredStateCheckPassed ) {
			JSFUtil.addMessage( "You cannot access " + JSFUtil.getRequest().getRequestURI() + " directly.  This may be a result of using the back and forward buttons in your browser, please refrain from using these and use the navigation provided in the website instead, or you may have set up a bookmark to this page in which case please remove it and bookmark a different page." );
			AplosUrl aplosUrl;
			if( "true".equals( JSFUtil.getRequestParameter( AplosScopedBindings.REQUIRED_STATE_REDIRECT ) ) ) {
				aplosUrl = new AplosUrl( AplosContextListener.getAplosContextListener().getLoginPageUrl( JSFUtil.getRequest().getRequestURI(), JSFUtil.getRequest(), true, false) );
			} else if( this instanceof EditPage && ((EditPage) this).getBeanDao() != null ) {
				Class<? extends BackingPage> listPageClass = ApplicationUtil.getAplosContextListener().getListPageClasses().get( ((EditPage) this).getBeanDao().getBeanClass().getSimpleName() );
				if( listPageClass != null ) {
					aplosUrl = new BackingPageUrl( listPageClass );
				} else {
					aplosUrl = ApplicationUtil.getAplosContextListener().getAfterLoginUrl(Website.getCurrentWebsiteFromTabSession());
				}
			} else {
				aplosUrl = ApplicationUtil.getAplosContextListener().getAfterLoginUrl(Website.getCurrentWebsiteFromTabSession());
			}
			
			aplosUrl.addQueryParameter( AplosScopedBindings.REQUIRED_STATE_REDIRECT, "true" );
			JSFUtil.redirect(aplosUrl);
			
			ApplicationUtil.handleError( new Exception( "Required state failed for " + this.getClass().getSimpleName() ), false );
		}
		return isRequiredStateCheckPassed;
	}
	
	public String getScannerHooksForJavascript() {
		List<ScannerHook> scannerHooks = getScannerHooks();
		StringBuffer scannerHooksBuf = new StringBuffer( "[" );
		for( int i = 0, n = scannerHooks.size(); i < n; i++ ) {
			scannerHooksBuf.append( "[" );
			scannerHooksBuf.append( scannerHooks.get( i ).getJavascriptString() );
			scannerHooksBuf.append( "]" );
			if( i < (scannerHooks.size() - 1) ) {
				scannerHooksBuf.append( "," );
			}
		}
		scannerHooksBuf.append( "]" );
		return scannerHooksBuf.toString();
	}
	
	public boolean isShowingEditActionButtons() {
		return false;
	}
	
	public List<ScannerHook> getScannerHooks() {
		List<ScannerHook> scannerHooks = new ArrayList<ScannerHook>();
		if( !CommonUtil.isNullOrEmpty( CommonConfiguration.getCommonConfiguration().getScannerPrefix() ) && 
				!CommonUtil.isNullOrEmpty( CommonConfiguration.getCommonConfiguration().getScannerSuffix() ) ) {
			String scannerPrefix = CommonConfiguration.getCommonConfiguration().getScannerPrefix();
			String scannerSuffix = CommonConfiguration.getCommonConfiguration().getScannerSuffix();
			boolean isScannerPrefixUsingCtrl = CommonConfiguration.getCommonConfiguration().isScannerPrefixUsingCtrl();
			scannerHooks.add( new ScannerHook( scannerPrefix, scannerSuffix, isScannerPrefixUsingCtrl ) );
		}
		return scannerHooks;
	}
	
	public SslProtocolEnum getSslProtocolEnum() {
		return ApplicationUtil.getAplosContextListener().getBackingPageMetaData(getClass()).getSsLProtocol();
	}

	public boolean responsePageLoad() {
		String param = JSFUtil.getRequestParameter( AplosAppConstants.LANG );
		if (param != null && !param.equals("")) {
			if (!CommonUtil.getContextLocale().getLanguage().equals(param)) {
				CommonUtil.setContextLocale( new Locale( param ) );
			}
		}
		return true;
	}
	
	public boolean saveBeanWithWrap() {
		boolean beanSaved = false;
		PersistentContextTransaction persistentContextTransaction = new PersistentContextTransaction();
		try {
			persistentContextTransaction.createConnection();
			beanSaved = saveBean();
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
		} finally {
			persistentContextTransaction.commitAndCloseOrRollback();
		}
		return beanSaved;
	}
	
	public boolean isAjaxApplyButton() {
		/*
		 * Validation wasn't working when the save button was set to be ajax.  Although
		 * I tried changing the execute command it still wouldn't work.
		 */
		return false;
	}
	
	public boolean saveBean() {
		return resolveAssociatedBean().saveDetails();
	}

	public void genericPageLoad() {

	}

//	//when this is true it ignores both database-on-hold and paid-feature checks
	public final boolean isGlobalAccess(){
		GlobalAccess globalAccess = getClass().getAnnotation(GlobalAccess.class);
		if( globalAccess != null ) {
			return globalAccess.allowAccess();
		} else {
			return false;
		}
	}
	
	public String getFormOnKeyPress() {
		if( isDisableFormSubmitOnEnter() ) {
			return "return monitorFormKeyPress(event);";
		}
		return "";
	}

	public boolean ignoresPaidFeatureCheck() {
		return isGlobalAccess() || false;
	}

	public String getPageTitle() {
		return getPageHeading();
	}

	public String getPageHeading() {
		return "";
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosBean> T resolveAssociatedBean() {
		if (aqlBeanDao != null) {
			return (T) JSFUtil.getBeanFromScope( aqlBeanDao.getBeanClass() );
		} else {
			return null;
		}
	}


	/*
	 * Called from the backend.xhtml and overridden in sub classes to give access
	 * to values scanned into the page, in conjunction with the scannedStr variable.
	 */
	public void registerScan() {
		logger.info("BackingPage registered scan of '" + scannedStr + "'");
	}

	public TrailDisplayName determineTrailDisplayName() {
		return determineTrailDisplayName(JSFUtil.getCurrentPath( true ));
	}

	public TrailDisplayName determineTrailDisplayName(String requestUrl) {
		TrailDisplayName trailDisplayName = getTrailDisplayNameFromMenuTab( requestUrl );

		if( trailDisplayName == null ) {
			trailDisplayName = getTrailDisplayNameFromClassName();
		}

		return trailDisplayName;
	}

	public TrailDisplayName getTrailDisplayNameFromClassName() {
		return getTrailDisplayNameFromClassName(getClass());
	}
	
	public static TrailDisplayName getTrailDisplayNameFromClassName(Class clazz) {
		TrailDisplayName trailDisplayName = new TrailDisplayName();
		String bundleKey = clazz.getName().replace( "com.aplos.", "" );
		DynamicBundleEntry dyammicBundleEntry = ApplicationUtil.getAplosContextListener().getDynamicBundleEntry( bundleKey );
		if( dyammicBundleEntry == null ) {
			trailDisplayName.setPlainText( FormatUtil.breakCamelCase( clazz.getSimpleName() ) );
		} else {
			trailDisplayName.setDynamicBundleKey( bundleKey );
		}
		return trailDisplayName;
	}

	public TrailDisplayName getTrailDisplayNameFromMenuTab( String requestUrl ) {
		//Naming Priority: Menu Tab Name
		MenuTab menuTab =  ApplicationUtil.getMenuCacher().getMenuTab( requestUrl );
		if (menuTab != null) {
			TrailDisplayName trailDisplayName = new TrailDisplayName();
			trailDisplayName.setDynamicBundleKey( menuTab.getDisplayNameRaw() );
			return trailDisplayName;
		}
		return null;
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel( DataTableState dataTableState, BeanDao beanDao ) {
		return new AplosLazyDataModel( dataTableState, beanDao );
	}

	@Override
	public DataTableState getDefaultDataTableState( Class<?> parentClass ) {
		DataTableState dataTableState = CommonConfiguration.getCommonConfiguration().getDefaultDataTableState();
		dataTableState.setParentClass( parentClass );
		return dataTableState;
	}
	
	public String getGoogleAnalyticsCode() {
		Website currentWebsite = Website.getCurrentWebsiteFromTabSession();
		if( currentWebsite != null ) {
			String googleAnalyticsId = currentWebsite.getGoogleAnalyticsId();
			if( !CommonUtil.isNullOrEmpty( googleAnalyticsId ) ) {
				StringBuffer strBuf = new StringBuffer();
				Website.addGoogleAnalyticsCode( strBuf, googleAnalyticsId );
				return strBuf.toString();
			}
		}
		return null;
	}

	public void setBeanDao(BeanDao aqlBeanDao) {
		this.aqlBeanDao = aqlBeanDao;
	}

	public BeanDao getBeanDao() {
		return aqlBeanDao;
	}

	public String getReturnPage() {
		return returnPage;
	}

	public void setPrependMainFormId(boolean prependMainFormId) {
		this.prependMainFormId = prependMainFormId;
	}

	public boolean isPrependMainFormId() {
		return prependMainFormId;
	}

	public void setPageDisplayName(String pageDisplayName) {
		this.pageDisplayName = pageDisplayName;
	}

	public String getPageDisplayName() {
		return pageDisplayName;
	}

	public static boolean validationRequired( String triggerButtonName ) {
		Iterator iter = JSFUtil.getRequest().getParameterMap().keySet().iterator();
		while( iter.hasNext() ) {
			String key = ((String) iter.next());
			//System.out.println("Validate Against : " + item);
			if( key.contains( triggerButtonName ) ) {
				return true;
			} else if( key.equals( "javax.faces.source" ) && ((String[]) JSFUtil.getRequest().getParameterMap().get( key ))[0].contains( triggerButtonName ) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean validationRequired() {
		return validationRequired("aplosEditOkBtn") || validationRequired("aplosEditSaveBtn") || validationRequired("okButton") || validationRequired("applyButton");
	}

	public boolean isValidationRequired() {
		return BackingPage.validationRequired();
	}

	/**
	 * @deprecated
	 * Please use isValidationRequired() instead
	 * @return
	 */
	public boolean getIsValidationRequired() {
		return BackingPage.validationRequired();
	}

	public boolean shouldStackOnNavigation() {
		return shouldStackOnNavigation(null);
	}

	public String getDefaultPreviousPageUrl() {
		return null;
	}

	public void navigateToPreviousPage() {
		if (JSFUtil.getNavigationStack().size() >= 2) {
			JSFUtil.getNavigationStack().navigateBack(); // this will do a redirect
		} else {
			if( getDefaultPreviousPageClass() != null ) {
				JSFUtil.redirect( getDefaultPreviousPageClass() );
			}
		}
	}
	
	public boolean isIncludingAnalytics() {
		return false;
	}

	public Class<? extends BackingPage> getDefaultPreviousPageClass() {
		return null;
	}

	/**
	 * This can be overridden to stop a reference to the page being added to the stack and breadcrumbs
	 * when loading this page
	 * @param argument - an object to be used as part of the determination logic
	 * @return
	 */
	public boolean shouldStackOnNavigation(Object argument) {
		return true;
	}

	public String getScannedStr() {
		return scannedStr;
	}

	public void setScannedStr(String scannedStr) {
		this.scannedStr = scannedStr;
	}

	public boolean isDisableFormSubmitOnEnter() {
		return disableFormSubmitOnEnter;
	}

	public void setDisableFormSubmitOnEnter(boolean disableFormSubmitOnEnter) {
		this.disableFormSubmitOnEnter = disableFormSubmitOnEnter;
	}

	/**
	 * @return a list of BackingPage classes which should be associated with the same tab as this class
	 */
	public List<Class<? extends BackingPage>> getTabDefaultPageBindings() {
		return null;
	}

	public Set<String> getRequiredStateBindings() {
		return requiredStateBindings;
	}

	public void setRequiredStateBindings(Set<String> requiredStateBindings) {
		this.requiredStateBindings = requiredStateBindings;
	}

	public JsfScope determineAssociatedBeanScope() {
		if( associatedBeanScope == null ) {
			if( getBeanDao() != null ) {
				setAssociatedBeanScope( AplosAbstractBean.determineScope( getBeanDao().getBeanClass() ) );
			} else {
				setAssociatedBeanScope( JsfScope.FLASH_VIEW );
			}
		}
		return associatedBeanScope;
	}

	public JsfScope getAssociatedBeanScope() {
		return associatedBeanScope;
	}

	public void setAssociatedBeanScope(JsfScope associatedBeanScope) {
		this.associatedBeanScope = associatedBeanScope;
	}

	public void setAssociatedBeanId(Long associatedBeanId) {
		this.associatedBeanId = associatedBeanId;
	}

	public Long getAssociatedBeanId() {
		return associatedBeanId;
	}

	public Long getAlternateAssociatedBeanId() {
		return alternateAssociatedBeanId;
	}

	public void setAlternateAssociatedBeanId(Long alternateAssociatedBeanId) {
		this.alternateAssociatedBeanId = alternateAssociatedBeanId;
	}
}





