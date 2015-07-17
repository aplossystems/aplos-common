package com.aplos.common.module;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.aplos.common.annotations.FrontendBackingPage;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.InboundSms;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UserLevelUtil;

public abstract class AplosModuleImpl implements AplosModule {
	private ModuleDbConfig moduleDbConfig;
	private DatabaseLoader databaseLoader; 

	public AplosModuleImpl() {
		this.setModuleDbConfig(createModuleDbConfig());
		this.setDatabaseLoader(createDatabaseLoader());
	}

	@Override
	public void initModule() {
	}
	
	public void tabSessionCreated( HttpSession session, Map<String, Object> tabSessionMap ) {
		
	}
	
	@Override
	public void configureArchivableTables() {	
	}
	
	@Override
	public void loadupClearCache() {
		
	}
	
	@Override
	public void clearCache() {	
	}
	
	@Override
	public void registerInboundSms(InboundSms inboundSms) {}
	

	public Boolean processHardDeleteEvent( AplosAbstractBean aplosAbstractBean ) { 
		return null;
	}
	
	@Override
	public void registerIncomingEmail( AplosEmail aplosEmail ) {}
	
	@Override
	public DatabaseLoader createDatabaseLoader() {
		return null;
	}
	
	@Override
	public Boolean getFaceletEvent(String url) {
		return null;
	}
	
	@Override
	public String getJDynamiTeValue(  String variableKey, AplosEmail aplosEmail, BulkEmailSource bulkEmailSource  ) {
		return null;
	}
	
	@Override
	public void addEmailTemplateOverride() {}

	public void addGlobalAccessPages( List<String> globalAccessPages ) {}
	
	public Boolean determineIsFrontEnd( String requestUrl ) {
		String isFrontEnd = JSFUtil.getRequestParameter( AplosAppConstants.IS_FRONT_END );
		if( isFrontEnd != null ) {
			return Boolean.valueOf( isFrontEnd );
		}
		if( JSFUtil.getCurrentBackingPage() != null && JSFUtil.getCurrentBackingPage().getClass().getAnnotation( FrontendBackingPage.class ) != null ) {
			return true;
		}
		if (requestUrl != null) {
			if (requestUrl.endsWith(".xhtml") || requestUrl.endsWith( "cms/" ) ) {
				return false;
			} else if (requestUrl.endsWith(".aplos") || requestUrl.endsWith( "/" ) || requestUrl.startsWith( "/cmsGenerationViews/" ) ) {
				return true;
			}
		}

		return null;
	}

	@Override
	public void addAvailableBundleKeys(List<BundleKey> textTranslationList) {
		

	}

	@Override
	public URL getResourceFilterUrl(String resourceName, String contextPath) {
		//checks cms css/js for written files
		return null;
	}

	@Override
	public Currency updateSessionCurrency(HttpServletRequest request) {
		return null;
	}

	@Override
	public URL checkFileLocations( String resourceName, String resourcesPath ) {
		return getClass().getResource( (getPackageRoot() + resourcesPath + resourceName).replace( "//", "/") );
	}

	@Override
	//Should be overriden by Implementation Projects which have two words, ie 'Big Matt' which would return Bigmatt
	public String getPackageDisplayName()  {
		return getPackageName();
	}

	@Override
	public String getPackageName() {
		String classNameStrParts[] = getClass().getName().split( "\\." );
		return classNameStrParts[ 2 ];
	}

	@Override
	public String getPackageRoot() {
		String classNameStrParts[] = getClass().getName().split( "\\." );
		return "/" + classNameStrParts[ 0 ] + "/" + classNameStrParts[ 1 ] + "/" + classNameStrParts[ 2 ] + "/";
	}

	@Override
	public String getModuleName() {
		return CommonUtil.getModuleName( getClass().getName() );
	}

	@Override
	public String fireNewWebsiteAction() {
		return null;
	}

	@Override
	public Boolean rewriteUrl( HttpServletRequest req, HttpServletResponse resp ) throws IOException, ServletException {
		return null;
	}

	@Override
	public UserLevelUtil createUserLevelUtil() {
		return null;
	}

	@Override
	public void contextInitialisedFinishing(AplosContextListener aplosContextListener) {
		

	}

	@Override
	public void addImplicitPolymorphismEntries( AplosContextListener aplosContextListener ) {}
	@Override
	public void addViewOverrideEntries() {}
	@Override
	public List<String> getRestrictedMediaPaths() { return null; }

	public void setModuleDbConfig(ModuleDbConfig moduleDbConfig) {
		this.moduleDbConfig = moduleDbConfig;
	}

	@Override
	public ModuleDbConfig getModuleDbConfig() {
		return moduleDbConfig;
	}

	@Override
	public String getWebsiteWizardSettingsViewPath() {
		return "/" + getPackageName() + "/websiteWizardSettings.xhtml";
	}
	
	@Override
	public SelectItem[] getCountrySelectItems() {
		return null;
	}

	@Override
	public DatabaseLoader getDatabaseLoader() {
		return databaseLoader;
	}

	public void setDatabaseLoader(DatabaseLoader databaseLoader) {
		this.databaseLoader = databaseLoader;
	}

}
