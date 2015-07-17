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

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.InboundSms;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.UserLevelUtil;

public interface AplosModule {
	public ModuleDbConfig createModuleDbConfig();
	public ModuleDbConfig getModuleDbConfig();
	public DatabaseLoader createDatabaseLoader();
	public DatabaseLoader getDatabaseLoader();
	public String getPackageName();
	public String getPackageDisplayName();
	public String getPackageRoot();
	public void initModule();
	public URL checkFileLocations( String resourceName, String resourcesPath );
	public void addImplicitPolymorphismEntries( AplosContextListener aplosContextListener );
	public Boolean rewriteUrl( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException;
	public String getModuleName();
	public Boolean getFaceletEvent(String url);
	public String fireNewWebsiteAction();
	public void contextInitialisedFinishing( AplosContextListener aplosContextListener );
	public void addGlobalAccessPages( List<String> globalAccessPages );
	public void addViewOverrideEntries();
	public ModuleUpgrader createModuleUpgrader();
	public ModuleConfiguration getModuleConfiguration();
	public AplosWorkingDirectoryInter[] createWorkingDirectoryEnums();
	public void tabSessionCreated(HttpSession session, Map<String, Object> tabSessionMap);
	public void addEmailTemplateOverride();
	public void registerIncomingEmail( AplosEmail aplosEmail );
	public Boolean processHardDeleteEvent( AplosAbstractBean aplosAbstractBean );
	public void addAvailableBundleKeys( List<BundleKey> textTranslationList );
	public String getJDynamiTeValue( String variableKey, AplosEmail aplosEmail, BulkEmailSource bulkEmailSource );
	public void registerInboundSms( InboundSms inboundSms );
	public void configureArchivableTables();
	public void clearCache();

	//  This is to retrieve project specific objects from non-implementation projects
	public SelectItem[] getCountrySelectItems();
	public Currency updateSessionCurrency( HttpServletRequest request );
	public String getWebsiteWizardSettingsViewPath();
	public URL getResourceFilterUrl(String resourceName, String contextPath);
	public UserLevelUtil createUserLevelUtil();
	public Boolean determineIsFrontEnd(String requestUrl);
	public List<String> getRestrictedMediaPaths();
}
