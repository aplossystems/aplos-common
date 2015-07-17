package com.aplos.common.listeners;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.el.util.ReferenceCache;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.ImplicitPolymorphismEntry;
import com.aplos.common.ImplicitPolymorphismVariable;
import com.aplos.common.MenuCacher;
import com.aplos.common.ScheduledJob;
import com.aplos.common.WebsiteMemory;
import com.aplos.common.annotations.BackingPageOverride;
import com.aplos.common.annotations.PrintTemplateOverride;
import com.aplos.common.annotations.SslProtocol;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.CreateWebsiteWizardPage;
import com.aplos.common.backingpage.IssueReportedPage;
import com.aplos.common.backingpage.LoginPage;
import com.aplos.common.backingpage.PageNotFoundPage;
import com.aplos.common.backingpage.SessionTimeoutPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.AplosSiteBean;
import com.aplos.common.beans.AplosWorkingDirectory;
import com.aplos.common.beans.BackingPageMetaData;
import com.aplos.common.beans.DynamicBundleEntry;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.facelets.AplosFaceletCache;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.module.AplosModule;
import com.aplos.common.module.AplosModuleFilterer;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.module.DatabaseLoader;
import com.aplos.common.module.ModuleConfiguration;
import com.aplos.common.persistence.PersistentApplication;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.tabpanels.SiteTabPanel;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.threads.AplosThread;
import com.aplos.common.threads.EmailManager;
import com.aplos.common.threads.EmailSender;
import com.aplos.common.threads.JobScheduler;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ConversionUtil;
import com.aplos.common.utils.ErrorEmailSender;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.lowagie.text.DocumentException;

public abstract class AplosContextListener implements ServletContextListener {
	private static Logger logger = Logger.getLogger( AplosContextListener.class );
	private boolean debugMode = true;
	private ServletContext context;
	private boolean isAutoLogin = false;
	private boolean isPageAccessChecked = true;
	private boolean isPageAccessCached = true;
	private String autoLoginUsername;
	private String autoLoginPassword;
	private String autoLoginFirstPage;
	private EmailSender emailSender;
	private EmailManager emailManager;
	private List<AplosModule> aplosModuleList = new ArrayList<AplosModule>();
	private static HashMap<ImplicitPolymorphismVariable,List<ImplicitPolymorphismEntry>> implicitPolymorphisimMap = new HashMap<ImplicitPolymorphismVariable,List<ImplicitPolymorphismEntry>>();
	private AplosModule implementationModule;
	private AplosModuleFilterer aplosModuleFilterer;
	private List<Website> websiteList = new ArrayList<Website>();
	private List<Class<? extends AplosSiteBean>> disabledAplosSiteBeans = new ArrayList<Class<? extends AplosSiteBean>>();
	private static AplosContextListener aplosContextListener;
	private MenuCacher menuCacher = new MenuCacher();
	private final Map<String, Class<? extends BackingPage>> listPageClasses = new HashMap<String, Class<? extends BackingPage>>();
	private final Map<String, Class<? extends BackingPage>> editPageClasses = new HashMap<String, Class<? extends BackingPage>>();
	private final Map<Class<? extends BackingPage>, BackingPageMetaData> backingPageMetaDataMap = new HashMap<Class<? extends BackingPage>, BackingPageMetaData>();
	private String backendIeConditionalStatement;
	private String frontendIeConditionalStatement;
	private Map<Long, WebsiteMemory> websiteMemoryMap = new HashMap<Long,WebsiteMemory>();
	private Map<Long,List<String>> userLevelPageAccess = new HashMap<Long,List<String>>();
	private Map<String, AplosWorkingDirectoryInter> aplosWorkingDirectoryEnumMap = new HashMap<String,AplosWorkingDirectoryInter>();
	private String serverUrl;
    private PersistentApplication persistentApplication;
    private JobScheduler jobScheduler = new JobScheduler();
    private AplosFaceletCache aplosFaceletCache;
    private Set<AplosThread> aplosThreads = new HashSet<AplosThread>();
    private boolean isErrorEmailActivated = true;
	
	private Map<Thread,Map<String,Object>> threadSessionMap = new HashMap<Thread,Map<String,Object>>();
	

	public AplosContextListener() {
	}

	public void addAplosModule( AplosModule aplosModule ) {
		getAplosModuleList().add( aplosModule );
	}
	
	public void systemUserLoggedIn( SystemUser systemUser ) {
		
	}
	
	public void systemUserLoggedOut( SystemUser systemUser ) {}
	
	public void sessionDestroyed(HttpSessionEvent event) {}
	
	public void addFonts( ITextRenderer renderer ) throws IOException, DocumentException {
	}
	
	public void toggleDebugMode() {
		setDebugMode(!isDebugMode());
	}
	
	public long getFaceletRefreshPeriod() {
		/*
		 *      Changing this value has a serious effect on the performance of the server.  Having it set as -1 
		 *		means that facelets pages will never be recreated which stops modifications made through the CMS
		 *		from showing.  Therefore it needs to be a positive number.
		 */
		if( JSFUtil.isLocalHost() ) {
			return 2;
		} else {
			/*
			 *  disables the clearing of the cache.  The AplosFaceletCache can still manually expire URL's which is used
			 *  in the CMS.
			 */
			return -1;
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		logger.error( "Context Destroyed" );
		if( getEmailSender() != null ) {
			getEmailSender().stopThread();
			getEmailSender().interrupt();
			try {
				getEmailSender().join();
			} catch( InterruptedException iEx ) {
				iEx.printStackTrace();
			}
		}
		if( ApplicationUtil.getJobScheduler() != null ) {
			ApplicationUtil.getJobScheduler().stopThread();
			try {
				ApplicationUtil.getJobScheduler().join();
			} catch( InterruptedException iEx ) {
				iEx.printStackTrace();
			}
		}
		
		jBossElFix();
		ApplicationUtil.getPersistentApplication().closeCpds();
		logger.info( "Shutdown completed successfully for " + serverUrl );
	}
	
	public void jBossElFix() {
		try {
			Field methodCacheField = org.jboss.el.util.ReflectionUtil.class.getDeclaredField("methodCache");
			methodCacheField.setAccessible(true);
			Object methodCache = methodCacheField.get(null);
			Field cacheField = ReferenceCache.class.getDeclaredField("cache");
			cacheField.setAccessible(true);
			ConcurrentHashMap concurrentHashMap = (ConcurrentHashMap) cacheField.get( methodCache );
			for( Object mapKey : concurrentHashMap.keySet() ) {
				if( mapKey.getClass().getClassLoader().equals( Thread.currentThread().getContextClassLoader() ) ) {
					concurrentHashMap.remove( mapKey );
				}
			}
		} catch( IllegalAccessException iaEx ) {
			iaEx.printStackTrace();
		} catch( NoSuchFieldException nsfEx ) {
			nsfEx.printStackTrace();
		}
	}
	
	public boolean isRedirectingOnIllegalSet() {
		if( JSFUtil.isLocalHost() ) {
			return true;
		} else {
			return false;
		}
	}

	// for overriding
	public boolean writeScriptsAndStylesToFrontEnd() {
		return true;
	}
	
	public Website getWebsite( Long websiteId ) {
		for( int i = 0, n = getWebsiteList().size(); i < n; i++ ) {
			if( getWebsiteList().get( i ).getId().equals( websiteId ) ) {
				return getWebsiteList().get( i );
			}
		}
		return null;
	}
	
	public void startThreadSession( Website website ) {
		startThreadSession();
		Website.addCurrentWebsiteToTabSession(website);
	}
	
	public void startThreadSession() {
		getThreadSessionMap().put( Thread.currentThread(), new HashMap<String,Object>() );
	}
	
	public void endThreadSession() {
		getThreadSessionMap().remove( Thread.currentThread() );
	}
	
	public void refreshUserLevelPageAccess() {
		List<Long> userLevelIds = new ArrayList<Long>( userLevelPageAccess.keySet() );
		for( int i = 0, n = userLevelIds.size(); i < n; i++ ) {
			UserLevel userLevel = new BeanDao( UserLevel.class ).get( userLevelIds.get( i ) );
			userLevel.updateAccessPages(this);
		}
	}
	
	public int getMaxInactiveInterval() {
		return 60 * 60 * 4;
	}

	public void changeWebsite() {
		Website website = (Website) JSFUtil.getRequest().getAttribute( "curWebsite" );
		website.addToScope( JsfScope.TAB_SESSION );
		website.markAsCurrentWebsite();
		JSFUtil.redirect( new AplosUrl("/"), true );
	}

	public void goToSite() {
		JSFUtil.redirect( new AplosUrl("/"), true );
	}

	public boolean isAdminLoggedIn() {
		SystemUser systemUser = JSFUtil.getLoggedInUser();
		if( systemUser != null ) {
			if( systemUser.isLoggedIn() ) {
				if( systemUser.getUserLevel().equals( CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAdminOrHigherLoggedIn() {
		SystemUser systemUser = JSFUtil.getLoggedInUser();
		if( systemUser != null ) {
			if( systemUser.isLoggedIn() ) {
				if( systemUser.getUserLevel().getClearanceLevelDoesntExceedMaximum( CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel().getClearanceLevel() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAplosSiteBeanDisabled(Class<? extends AplosSiteBean> aplosSiteBeanClass) {
		return getDisabledAplosSiteBeans().contains(aplosSiteBeanClass);
	}

	public String getFixedSalt() {
		return "aS7*&yHiju£$ech54";
	}

	public String getAfterLoginUrlString() {
		if( Website.getCurrentWebsiteFromTabSession() == null ) {
			Website.refreshWebsiteInSession();
		}
		JSFUtil.redirect( getAfterLoginUrl( Website.getCurrentWebsiteFromTabSession() ) );
		/*
		 *  We need to return a valid url  for pretty faces, we might as well use the same page
		 *  , it shouldn't affect load time as the above redirect will set responseComplete(). 
		 */
		return getAfterLoginUrl( Website.getCurrentWebsiteFromTabSession() ).toString() + "?faces-redirect=true";
	}

	public AplosUrl getAfterLoginUrl( Website website ) {
		if( website != null ) {
			return website.determineDefaultMenuUrl();
		} else {
			return new BackingPageUrl( "common", CreateWebsiteWizardPage.class, true );
		}
	}

	public void updateWebsite( Website website ) {
		website.createWebsiteMemory();
		for( int i = 0, n = getWebsiteList().size(); i < n; i++ ) {
			if( getWebsiteList().get( i ).getId().equals( website.getId() ) ) {
				getWebsiteList().set( i , website );
				return;
			}
		}
		if( website.isActive() ) {
			getWebsiteList().add( website );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.setProperty("org.apache.el.parser.COERCE_TO_ZERO", "false");
//        WebConfiguration.getInstance(servletContextEvent.getServletContext()).overrideContextInitParameter(WebConfiguration.WebContextInitParameter.FaceletsDefaultRefreshPeriod, "-1");
        
		try {
			logger.debug("Context Initialised");
			
			aplosContextListener = this;
			setContext(servletContextEvent.getServletContext());
			createAndStartEmailSender();
			setEmailManager(new EmailManager());
			getAplosThreads().add( getEmailManager() );
			getEmailManager().startThread();
			aplosModuleFilterer = new AplosModuleFilterer( this );
			createFrontendIeConditionalStatement();
			createBackendIeConditionalStatement();
	
			getContext().setAttribute(AplosScopedBindings.ACTIVE_USERS, new HashMap<String,HttpSession>() );
			getContext().setAttribute(CommonUtil.getBinding( AplosModuleFilterer.class ), aplosModuleFilterer );
			String serverUrl = getInitParameter("serverUrl" );
			getContext().setAttribute(AplosScopedBindings.SERVER_URL, serverUrl );
			setServerUrl( serverUrl );
			getContext().setAttribute(AplosScopedBindings.CONTEXT_LISTENER, this);
			if (getInitParameter("debugMode") != null && getInitParameter("debugMode").equals("1")) {
				setDebugMode( true );
			} else {
				setDebugMode( false );
			}
	
			ArrayList<BundleKey> availableBundleKeys = new ArrayList<BundleKey>();
	
			setAplosWorkingDirectoryEnumMap( new HashMap<String,AplosWorkingDirectoryInter>() );
			for( int i = getAplosModuleList().size() - 1; i >= 0; i-- ) {
				AplosWorkingDirectoryInter[] aplosWorkingDirectoryEnums = getAplosModuleList().get( i ).createWorkingDirectoryEnums();
				if( aplosWorkingDirectoryEnums != null ) {
					for( AplosWorkingDirectoryInter tempAplosWorkingDirectoryEnum : aplosWorkingDirectoryEnums ) {
						getAplosWorkingDirectoryEnumMap().put( tempAplosWorkingDirectoryEnum.name(), tempAplosWorkingDirectoryEnum );
					}
				}
				getAplosModuleList().get( i ).addImplicitPolymorphismEntries( this );
				getAplosModuleList().get( i ).addAvailableBundleKeys( availableBundleKeys );
			}
	 
	    	persistentApplication = createPersistentApplication();
	    	for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
	    		aplosModuleList.get( i ).configureArchivableTables();
	        	aplosModuleList.get( i ).getModuleDbConfig().addAnnotatedClass(persistentApplication);
	    	}
	    	persistentApplication.prepareForPersistence();
	    	
//			if( getClass().getResource( "/hibernateTransfer.cfg.xml") != null ) {
//				HibernateTransferUtil.setAplosContextListener(this);
//			}
			
//			HibernateUtil.setAplosContextListener(this);
//			HibernateUtil.getSessionFactory().getCurrentSession();
//			HibernateUtil.getCurrentSession().beginTransaction();
	
			List<AplosWorkingDirectory> aplosWorkingDirectoryList = new BeanDao( AplosWorkingDirectory.class ).getAll();
			AplosWorkingDirectory tempAplosWorkingDirectory;
			List<AplosWorkingDirectory> newAplosWorkingDirectoryList = new ArrayList<AplosWorkingDirectory>();
			for( AplosWorkingDirectoryInter tempAplosWorkingDirectoryEnum : getAplosWorkingDirectoryEnumMap().values() ) {
				tempAplosWorkingDirectory = null;
				for( int j = 0, p = aplosWorkingDirectoryList.size(); j < p; j++ ) {
					if( aplosWorkingDirectoryList.get( j ).getEnumName().equals( tempAplosWorkingDirectoryEnum.name() ) ) {
						tempAplosWorkingDirectory = aplosWorkingDirectoryList.get( j );
						// remove for efficency in the next search
						aplosWorkingDirectoryList.remove( tempAplosWorkingDirectory );
						break;
					}
				}
				if( tempAplosWorkingDirectory == null ) {
					tempAplosWorkingDirectory = new AplosWorkingDirectory();
					tempAplosWorkingDirectory.setEnumName( tempAplosWorkingDirectoryEnum.name() );
					newAplosWorkingDirectoryList.add( tempAplosWorkingDirectory );
				}
				tempAplosWorkingDirectory.setAplosWorkingDirectoryEnum(tempAplosWorkingDirectoryEnum);
				tempAplosWorkingDirectoryEnum.setAplosWorkingDirectory( tempAplosWorkingDirectory );
			}
			
			Connection conn = null;
			try {
				conn = ApplicationUtil.getConnection();
				for( PersistentClass persistentClass : ApplicationUtil.getPersistentApplication().getNewlyAddedTables() ) {
					ApplicationUtil.getAplosModuleFilterer().newTableAdded( persistentClass.getTableClass() );
					PersistentApplication.createPersistenceHelperTable( persistentClass.determineSqlTableName(), conn );
				}
			} catch( SQLException sqlEx ) {
				ApplicationUtil.handleError( sqlEx, false );
			} finally {
				ApplicationUtil.closeConnection(conn);
			}
			ApplicationUtil.getPersistentApplication().getNewlyAddedTables().clear();
//			HibernateUtil.getCurrentSession().flush();
	
			/* Note: if this starts causing problems it may be because we have
			 * changed the order/direction of the loop. This was because we need
			 * common to be upgraded first in circumstances where systemusers or
			 * userlevels change.
			 */
			for( int i = getAplosModuleList().size()-1; i >=0 ; i-- ) {
				getAplosModuleList().get( i ).createModuleUpgrader().manageUpgradeModule();
			}
			
			/* 
			 * Save after the modules have been updated incase there's a database conflict
			 */
			for( AplosWorkingDirectory tempNewAplosWorkingDirectory : newAplosWorkingDirectoryList ) {
				tempNewAplosWorkingDirectory.saveDetails();
			}
	
			/* Assemble a list of BackingPage classes available to the Dynamic Menu
			 * Note: Backing Page rename's should be handled before this loop is called
			 * using the module upgrader
			 */
			BeanDao packageDao = new BeanDao(Website.class);
			packageDao.setSelectCriteria("bean.packageName");
			Set<String> packages = new HashSet();
			packageDao.setIsReturningActiveBeans( true );
			packages.addAll(packageDao.getResultFields());
			for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
				packages.add( aplosModuleList.get( i ).getPackageName() );
			}
	
			processBackingPageClasses( packages );
			processPrintTemplateClasses( packages );

			if( ApplicationUtil.getPersistentApplication().isArchivingData() ) {
				CommonConfiguration.CommonScheduledJobEnum.ARCHIVER_JOB.setActive(true);
			}
			
			boolean continueModuleConfigurationInit = true;
			int loopCount = 0;
			
			while( continueModuleConfigurationInit ) {
				continueModuleConfigurationInit = false;
				for( int i = getAplosModuleList().size()-1; i >=0 ; i-- ) {
					if( getAplosModuleList().get( i ).getModuleConfiguration() != null ) {
						ModuleConfiguration saveableConfiguration = getAplosModuleList().get( i ).getModuleConfiguration();
						continueModuleConfigurationInit = saveableConfiguration.recursiveModuleConfigurationInit(this, loopCount) || continueModuleConfigurationInit;
					}
				}
				loopCount++;
			}
	
			List<Website> loadedWebsiteList = new BeanDao( Website.class ).getAll();
			for( Website tempWebsite : loadedWebsiteList ) {
				if( tempWebsite.isActive() ) {
					getWebsiteList().add( tempWebsite );
				}
				tempWebsite.createWebsiteMemory();
				tempWebsite.getMainDynamicBundle().refreshDynamicBundleEntryMap(aplosContextListener);
				tempWebsite.checkBundleKeys( this, availableBundleKeys );
				tempWebsite.getMainDynamicBundle().checkPersistentApplication(this);
			}
	
			for( int i = getAplosModuleList().size()-1; i >=0 ; i-- ) {
				getAplosModuleList().get( i ).initModule();
				getAplosModuleList().get( i ).addEmailTemplateOverride();
			}
			
			for( ScheduledJob<?> scheduledJob : CommonConfiguration.getCommonConfiguration().getScheduledJobMap().values() ) {
				ApplicationUtil.getJobScheduler().addJob( scheduledJob );
			}
			ApplicationUtil.getJobScheduler().startScheduler(this);
			getAplosThreads().add( ApplicationUtil.getJobScheduler() );
			
			ApplicationUtil.getAplosModuleFilterer().loadupClearCache();
			
			if( getInitParameter("autoLogin") != null && getInitParameter("autoLogin").equals("1") ) {
				setAutoLogin(true);
				if( getInitParameter("autoLoginUsername") != null ) {
					autoLoginUsername = servletContextEvent.getServletContext().getInitParameter("autoLoginUsername");
				} else {
					autoLoginUsername = "";
				}
				if( getInitParameter("autoLoginPassword") != null ) {
					autoLoginPassword = servletContextEvent.getServletContext().getInitParameter("autoLoginPassword");
				} else {
					autoLoginPassword = "";
				}
				if( getInitParameter("autoLoginFirstPage") != null ) {
					autoLoginFirstPage = servletContextEvent.getServletContext().getInitParameter("autoLoginFirstPage");
				} else {
					autoLoginFirstPage = "";
				}
			}
	
			if( getInitParameter("pageAccessChecked") != null && getInitParameter("pageAccessChecked").equals("1") ) {
				setPageAccessChecked(true);
			}
	
			if( getInitParameter("pageAccessCached") != null && getInitParameter("pageAccessCached").equals("0") ) {
				setPageAccessCached(false);
			}
	
			//if its null we cant have just created it
			if ( CommonConfiguration.getCommonConfiguration().getDefaultRowsLoaded() != null && !CommonConfiguration.getCommonConfiguration().getDefaultRowsLoaded() ) {
				insertInitialUserLevelsAndUsers();
	//			Website website = getAplosModuleFilterer().createDefaultWebsite();
	//			website.aqlSaveDetails();
	
				DatabaseLoader tempDatabaseLoader;
				for( int i = getAplosModuleList().size() - 1; i >= 0; i-- ) {
					tempDatabaseLoader = getAplosModuleList().get( i ).getDatabaseLoader();
					if( tempDatabaseLoader != null ) {
						tempDatabaseLoader.loadTables();
					}
				}
				CommonConfiguration.getCommonConfiguration().setDefaultRowsLoaded(true);
				CommonConfiguration.getCommonConfiguration().saveDetails();
			}
	
			for( Website tempWebsite : loadedWebsiteList ) {
				tempWebsite.checkAndUpdateWebsiteState(this);
			}
	
			contextInitialized();
			
			for( int i = getAplosModuleList().size() - 1; i >= 0; i-- ) {
				getAplosModuleList().get( i ).contextInitialisedFinishing( this );
			}
			//applyMappingsToProductInfo();
//			HibernateUtil.getCurrentSession().getTransaction().commit();
		} catch( Throwable t ) {
			logger.error( "Exception in ContextListener startup", t );
			t.printStackTrace();
		}

	}
	
	public String getPrimefacesTheme() {
		return "aristo";
	}
	
	public PersistentApplication createPersistentApplication() {
		return new PersistentApplication();
	}
	
	public void createAndStartEmailSender() {
		if( getEmailSender() != null ) {
			getEmailSender().setRunning( false );
			ApplicationUtil.handleError(new Exception( "Email sender restarted" ) );
		}
		setEmailSender(new EmailSender());
		getAplosThreads().add( getEmailSender() );
		getEmailSender().startThread();
	}

	public void processBackingPageClasses( Set<String> packages ) {
		List<Class<? extends BackingPage>> backingPageClasses = new ArrayList<Class<? extends BackingPage>>();
		for( String packageName : packages ) {
			try {
				List<Class<?>> uncastClassList = CommonUtil.getClasses("com/aplos/" + packageName + "/backingpage", BackingPage.class, false);
				for( int i = 0, n = uncastClassList.size(); i < n; i++ ) {
					backingPageClasses.add( (Class<? extends BackingPage>) uncastClassList.get( i ) );
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		TabClass.propogateClassChangesToDb(backingPageClasses);
		String backingPageClassName;
		for( Class<? extends BackingPage> backingPageClass : backingPageClasses ) {
			backingPageClassName = backingPageClass.getSimpleName();
			if ( backingPageClassName.endsWith( "ListPage" ) ) {
				getListPageClasses().put( backingPageClassName.replace( "ListPage", "" ), backingPageClass );
			} else if ( backingPageClassName.endsWith( "EditPage" ) ) {
				getEditPageClasses().put( backingPageClassName.replace( "EditPage", "" ), backingPageClass );
			}

			BackingPageMetaData backingPageMetaData = new BackingPageMetaData();
			if( backingPageClass.getAnnotation( SslProtocol.class ) != null ) {
				backingPageMetaData.setSsLProtocol( backingPageClass.getAnnotation( SslProtocol.class ).sslProtocolEnum() );
			}

			if( backingPageClass.getAnnotation( WindowId.class ) != null ) {
				backingPageMetaData.setRequiresWindowId( backingPageClass.getAnnotation( WindowId.class ).required() );
			}
			
			if( backingPageClass.getAnnotation( BackingPageOverride.class ) != null ) {
				PageBindingPhaseListener.addBindingOverride( backingPageClass.getAnnotation( BackingPageOverride.class ).backingPageClass(), backingPageClass ); 
			}
			backingPageMetaDataMap.put( backingPageClass, backingPageMetaData );
		}
	}

	public void processPrintTemplateClasses( Set<String> packages ) {
		for( String packageName : packages ) {
			try {
				List<Class<?>> uncastClassList = CommonUtil.getClasses("com/aplos/" + packageName + "/templates/printtemplates", PrintTemplate.class, false);
				for( int i = 0, n = uncastClassList.size(); i < n; i++ ) {
					PrintTemplateOverride printTemplateOverride = uncastClassList.get( i ).getAnnotation(PrintTemplateOverride.class);
					PrintTemplate tempPrintTemplate = (PrintTemplate) CommonUtil.getNewInstance( uncastClassList.get( i ).getName(), null );
					if( printTemplateOverride != null ) {
						CommonConfiguration.getCommonConfiguration().getPrintTemplateMap().put( (Class<? extends PrintTemplate>) printTemplateOverride.templateClass(), tempPrintTemplate );
					} else {
						CommonConfiguration.getCommonConfiguration().getPrintTemplateMap().put( (Class<? extends PrintTemplate>) uncastClassList.get( i ), tempPrintTemplate );
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Renamed from translate because it is hiding {@link this#translate(String)}
	 * from EL/views since the swtich to myfaces
	 * @param bundleKey
	 * @return
	 */
	public String translateByKey(BundleKey bundleKey) {
		return translate(bundleKey.getName());
	}
	
	public AplosWorkingDirectoryInter getAplosWorkingDirectoryEnum( String name ) {
		return getAplosWorkingDirectoryEnumMap().get( name );
	}

	public String translate(String bundleKeyName, Locale locale ) {
		Website website = Website.getCurrentWebsiteFromTabSession();
		if (website != null && website.getMainDynamicBundle() != null) { //site creation
			return website.getMainDynamicBundle().translate( bundleKeyName, locale );
		} else {
			return bundleKeyName;
		}
	}

	public String translate(String bundleKeyName) {
		String translation = translate( bundleKeyName, CommonUtil.getContextLocale() );
		if( translation == null ) {
			return FormatUtil.untoken(bundleKeyName);
		} else {
			return translation;
		}
	}

	public DynamicBundleEntry getDynamicBundleEntry(String bundleKeyName) {
		return getDynamicBundleEntry(bundleKeyName, Website.getCurrentWebsiteFromTabSession());
	}

	public DynamicBundleEntry getDynamicBundleEntry(String bundleKeyName, Website website) {
		DynamicBundleEntry ret = null;
		if( website != null && website.getMainDynamicBundle() != null) {
			ret = website.getMainDynamicBundle().getDynamicBundleEntryMap().get(bundleKeyName);
		}
		return ret;
	}

	public void handleSessionTimeout( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse ) {
		//  We need to get the original URL from pretty faces otherwise it returns
		//  a random url name, this may break other areas but if so make sure that if
		//  the system is using pretty faces then this line will be called
		String requestUrl = JSFUtil.getAplosContextOriginalUrl( httpServletRequest );
		String redirectUrl;
		
		if( !shouldRedirectToSessionTimeout(requestUrl)  ) {
			redirectUrl = requestUrl;
		} else {
			Website website = Website.getWebsite();
			if (website == null) {
				//this happens for new projects (there are no sites)
				redirectUrl = new BackingPageUrl( "common", LoginPage.class, true ).toString();
			} else {
				redirectUrl = website.getSessionTimeoutUrl( requestUrl );
	
				if( redirectUrl == null ) {
					redirectUrl = new BackingPageUrl( "common", SessionTimeoutPage.class, true ).toString();
				}
			}
		}
		if( !redirectUrl.startsWith( "/" ) ) {
			redirectUrl = "/" + redirectUrl;
		}
		if( !redirectUrl.startsWith( httpServletRequest.getContextPath() ) ) {
			redirectUrl = httpServletRequest.getContextPath() + redirectUrl;
		}
		
		try {
			httpServletResponse.sendRedirect(redirectUrl);
		} catch( IOException ioex ) {
			ioex.printStackTrace();
		}
	}
	
	public boolean shouldRedirectToSessionTimeout( String requestUrl ) {
		return !(ApplicationUtil.getAplosModuleFilterer().determineIsFrontEnd(requestUrl,false) && !CommonConfiguration.getCommonConfiguration().isShowingFrontendSessionTimeout());
	}

	public List<ImplicitPolymorphismEntry> getImplicitPolymorphismMapEntryList( ImplicitPolymorphismVariable impPolVariable ) {
		for( ImplicitPolymorphismVariable tempImpPolVariable : implicitPolymorphisimMap.keySet() ) {
			if( tempImpPolVariable.getFullClassName().equals( impPolVariable.getFullClassName() ) &&
					tempImpPolVariable.getVariableName().equals( impPolVariable.getVariableName() )) {
				return implicitPolymorphisimMap.get( tempImpPolVariable );
			}
		}
		return null;
	}

	public void addImplicitPolymorphismEntry( ImplicitPolymorphismVariable implicitPolymorphismVariable, ImplicitPolymorphismEntry implicitPolymorphismEntry ) {
		List<ImplicitPolymorphismEntry> entryList = null;
		for( ImplicitPolymorphismVariable impPolVariable : implicitPolymorphisimMap.keySet() ) {
			if( impPolVariable.getFullClassName().equals( implicitPolymorphismVariable.getFullClassName() ) &&
					impPolVariable.getVariableName().equals( implicitPolymorphismVariable.getVariableName() )) {
				implicitPolymorphismVariable = impPolVariable;
				entryList = implicitPolymorphisimMap.get( impPolVariable );
				break;
			}

		}
		if( entryList == null ) {
			entryList = new ArrayList<ImplicitPolymorphismEntry>();
			implicitPolymorphisimMap.put( implicitPolymorphismVariable, entryList );
		}
		if( implicitPolymorphismEntry != null ) {
			entryList.add( implicitPolymorphismEntry );
		}
	}

	public HashMap<ImplicitPolymorphismVariable,List<ImplicitPolymorphismEntry>> getImplicitPolymorphismMap() {
		return implicitPolymorphisimMap;
	}

	public String getCurrentUrlsHtml() {
		if( FacesContext.getCurrentInstance() != null ) {
			return getCurrentUrlsHtml( JSFUtil.getRequest() );
		} else {
			return "";
		}
	}

	public String getCurrentUrlsHtml( HttpServletRequest httpRequest ) {
		StringBuffer urls = new StringBuffer();
		if( httpRequest != null ) {
			try {
				urls.append( "Host Name: " + new URL(httpRequest.getRequestURL().toString()).getHost() + "<br/>" );
			} catch ( MalformedURLException mUrlEx ) {
				mUrlEx.printStackTrace();
			}
			urls.append("Request URI: " + httpRequest.getRequestURI() + "<br/>");
			String originalPrettyUrl = JSFUtil.getAplosContextOriginalUrl(httpRequest);
			if( originalPrettyUrl != null ) {
				urls.append("Underlying URL: " + originalPrettyUrl + "<br/>");
			}
		}
		return urls.toString();
	}

	public Class<? extends BackingPage> getBackendIssueReportPageClass() {
		return IssueReportedPage.class;
	}

	public void handlePageNotFound(Throwable throwable) {
		handlePageNotFound( JSFUtil.getRequest(), JSFUtil.getResponse(), throwable, getCurrentUrlsHtml() );
	}

	public void handlePageNotFound( HttpServletRequest httpRequest, HttpServletResponse httpResponse, Throwable throwable, String errorUrls ) {
		logger.error( "Page not found", throwable );
		String packageName;
		HttpSession session = httpRequest.getSession();
		
		AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext();
		if( aplosRequestContext != null && aplosRequestContext.getPageRequest() != null ) {
			aplosRequestContext.getPageRequest().setStatus404( true );
		}
		Website website = Website.getCurrentWebsiteFromTabSession(session);
		if ( website != null && website.getPackageName() != null) {
			packageName = website.getPackageName();
		} else {
			packageName = null; //"Website_Object_Unavailable";
		}

		String originalUrl = JSFUtil.getAplosContextOriginalUrl();
		AplosUrl aplosUrl;
		JSFUtil.addMessage( originalUrl + " could not be found" );
		if( originalUrl != null && (originalUrl.contains( "/content/") || originalUrl.endsWith( ".aplos" ) && packageName != null) ) {
			if( website != null ) {
				aplosUrl = new AplosUrl( website.getPageNotFoundUrl( originalUrl ) );
			} else {
				/*
				 *  TODO I'm not sure if this situation could occur I just added
				 *  this to please the compiler as I don't currently have
				 *  the time to check.
				 */
				aplosUrl = new AplosUrl( "/" );
			}
		} else {
			if (packageName == null) {
				packageName = "common";
			}
			JSFUtil.addToFlashViewMap( AplosScopedBindings.PAGE_NOT_FOUND, originalUrl );
			aplosUrl = new BackingPageUrl( packageName, PageNotFoundPage.class, true );
		}
		aplosUrl.addWindowId(JSFUtil.getWindowId( httpRequest ));

		if( httpResponse.isCommitted() ) {
			httpResponse.setStatus( 404 );
			JSFUtil.redirect( aplosUrl, true, httpResponse, this );
		} else {
			JSFUtil.redirect( aplosUrl, true, JSFUtil.getExternalContext(), this );
		}


	}

	public void handleError(Throwable throwable) {
		handleError( JSFUtil.getRequest(), JSFUtil.getResponse(), throwable, getCurrentUrlsHtml(),true );
	}

	public void handleError(Throwable throwable, boolean redirectToIssueReported) {
		handleError( JSFUtil.getRequest(), JSFUtil.getResponse(), throwable, getCurrentUrlsHtml(), redirectToIssueReported );
	}

	public void handleError( HttpServletRequest httpRequest, HttpServletResponse httpResponse, Throwable throwable, String errorUrls, boolean redirectToIssueReported ) {
		logger.error( "Exception caught in request filter", throwable );

		boolean isExternalContextRedirect = false;
		if( redirectToIssueReported && httpRequest != null && JSFUtil.getExternalContext() != null && !httpResponse.isCommitted() ) {
			isExternalContextRedirect = true;
		}
		if( errorUrls != null ) {
			errorUrls += "<br/>External context redirect : " + isExternalContextRedirect;
		}
		ErrorEmailSender.sendErrorEmail(httpRequest,this,throwable,errorUrls);

		throwable.printStackTrace();

		/*
		 * HttpRequest may be null if within another thread so you don't want to redirect.
		 */
		if (redirectToIssueReported && httpRequest != null) {
			// Create error message for the user
			if( httpRequest.getSession() != null ) {
				httpRequest.getSession().setAttribute( AplosScopedBindings.TECHNICAL_DETAILS, CommonUtil.createStackTraceString(throwable) );
			}
			String packageName;
			HttpSession session = httpRequest.getSession();
			Website sessionWebsite = Website.getCurrentWebsiteFromTabSession(session);
			if ( sessionWebsite != null && sessionWebsite.getPackageName() != null) {
				packageName = sessionWebsite.getPackageName();
			} else {
				packageName = null; //"Website_Object_Unavailable";
			}

			AplosUrl aplosUrl;
			String originalUrl = JSFUtil.getAplosContextOriginalUrl(httpRequest); 
			if( originalUrl != null && (originalUrl.contains( "/content/") || originalUrl.endsWith( ".aplos" ) && packageName != null) ) {
				/**
				 * TODO: Common knows nothing about .aplos, this needs fixing
				 * discussed solution was to move this to module filterer so common could have .jsf by default
				 */
				aplosUrl = new AplosUrl( "/issue-reported.aplos" );
			} else {
				if (packageName == null) { //the changes to this section should keep our sessiontimeout (etc.) pages working (they have been broken)
					packageName = "common";
				}
				aplosUrl = new BackingPageUrl( packageName, getBackendIssueReportPageClass(), true );
			}

			aplosUrl.addWindowId( JSFUtil.getWindowId( httpRequest ) );

			if( JSFUtil.getExternalContext() != null && !httpResponse.isCommitted() ) {
				JSFUtil.redirect( aplosUrl, true, JSFUtil.getExternalContext(), this );
			} else {
				JSFUtil.redirect( aplosUrl, true, httpResponse, this, JSFUtil.getWindowId( httpRequest ) );
			}
		}
	}

	public abstract String getDefaultTheme();

	public String getInitParameter( String paramKey ) {
		return getContext().getInitParameter( paramKey );
	}

	public String getLoginPageUrl() {
		return getLoginPageUrl( true );
	}

	public String getLoginPageUrl(boolean includeContext) {
		return getLoginPageUrl(JSFUtil.getRequest().getRequestURL().toString(), JSFUtil.getRequest(), false, includeContext );
	}

	public String getLoginPageUrl( String requestUrl, HttpServletRequest httpRequest, boolean addExtension ) {
		return getLoginPageUrl( requestUrl, httpRequest, addExtension, true );
	}

	public String getLoginPageUrl( String requestUrl, HttpServletRequest httpRequest, boolean addExtension, boolean includeContext ) {
		String loginPageUrl;
		if (includeContext){
			loginPageUrl = httpRequest.getContextPath() + "/common/login";
		} else {
			loginPageUrl = "/common/login";
		}
		if( addExtension ) {
			loginPageUrl += ".jsf";
		}
		return loginPageUrl;
	}

	public void autoLogin( javax.faces.context.FacesContext facesContext ) {
		SystemUser systemUser = SystemUser.getSystemUserByUsername( autoLoginUsername, true );

		if( systemUser != null ) {
			boolean isPasswordValid = systemUser.validateLogin( autoLoginPassword );

			if( isPasswordValid ) {
				systemUser.login();

	    		String contextPath = ((HttpServletRequest) facesContext.getExternalContext().getRequest()).getContextPath();
	    		 
				Long websiteCount = ConversionUtil.convertToLong( ApplicationUtil.getFirstResult("SELECT COUNT(id) FROM " + AplosBean.getTableName(Website.class))[0]);
				String redirectUrl;
				if (websiteCount == null || websiteCount.intValue() < 1) {
					redirectUrl = new BackingPageUrl( CreateWebsiteWizardPage.class, true ).toString();
				} else {
		    		redirectUrl = autoLoginFirstPage;
		    		if( redirectUrl.equals( "" ) ) {
		    			return;
		    		}
				}
	    		if( !redirectUrl.startsWith( "/" ) ) {
	    			redirectUrl = "/" + redirectUrl;
	    		}
	    		if( !redirectUrl.endsWith( ".jsf" ) ) {
	    			redirectUrl += ".jsf";
	    		}
	    		redirectUrl = contextPath + redirectUrl;
	    		JSFUtil.redirect( new AplosUrl(redirectUrl), false );
			}
		}
	}

//	public boolean handlePageAccessFailed( FacesContext facesContext, HttpServletResponse response ) {
//		final String message = "You do not have the required permissions to access this page";
//		FacesContext.getCurrentInstance().addMessage( null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message) );
//		return false;
//	}

	public void contextInitialized() {}

	public SystemUser getNewSystemUser() {
		return new SystemUser();
	}

	public SelectItem[] getYesNoSelectItems() {
		SelectItem selectItems[] = new SelectItem[ 2 ];
		selectItems[ 0 ] = new SelectItem( true, "Yes" );
		selectItems[ 1 ] = new SelectItem( false, "No" );
		return selectItems;
	}

	public String getErrorEmailAddress() {
		//TODO: change this to receive error emails for TBP, don't sync
		return "error@aplossystems.co.uk";
	}

	public void insertInitialUserLevelsAndUsers() {
		if (CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() == null) {
			UserLevel superUserLevel = new UserLevel();
			superUserLevel.setName( "Super User" );
			superUserLevel.setClearanceLevel(100);
			superUserLevel.saveDetails();
			CommonConfiguration.retrieveUserLevelUtil().setSuperUserLevel( superUserLevel );
			UserLevel adminUserLevel = new UserLevel();
			adminUserLevel.setName( "Admin" );
			adminUserLevel.setClearanceLevel(200);
			adminUserLevel.saveDetails();
			CommonConfiguration.retrieveUserLevelUtil().setAdminUserLevel( adminUserLevel );
			UserLevel debugUserLevel = new UserLevel();
			debugUserLevel.setName( "Debug" );
			debugUserLevel.setClearanceLevel(200);
			debugUserLevel.saveDetails();
			CommonConfiguration.retrieveUserLevelUtil().setDebugUserLevel( debugUserLevel );
		}
		try {
			if( CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() != null ) {
				SystemUser superUser = getNewSystemUser();
				superUser.setUsername( "super" );
				superUser.updatePassword( "superReplaceThis" );
				superUser.setEmail( "info@aplossystems.co.uk" );
				superUser.setUserLevel( CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel() );
				superUser.saveDetails();
				superUser.setOwner( superUser );
				superUser.saveDetails();
				CommonConfiguration.getCommonConfiguration().setDefaultSuperUser( superUser );
			} else {
				logger.info("Admin users not inserted due to missing user level.");
			}
			if( CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() != null ) {
				SystemUser adminUser = getNewSystemUser();
				adminUser.setUsername( "admin" );
				adminUser.updatePassword( "adminReplaceThis" );
				adminUser.setEmail( "customer@aplossystems.co.uk" );
				adminUser.setUserLevel( CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() );
				adminUser.saveDetails();
				adminUser.setOwner( adminUser );
				adminUser.saveDetails();
				CommonConfiguration.getCommonConfiguration().setDefaultAdminUser( adminUser );
			} else {
				logger.info("Admin users not inserted due to missing user level.");
			}
			CommonConfiguration.retrieveUserLevelUtil().saveDetails();
		} catch (Exception e) { logger.info("Admin users not inserted due to exception. (may already exist)"); }
	}

	public void createFrontendIeConditionalStatement() {
		frontendIeConditionalStatement = "";
	}

	public void createBackendIeConditionalStatement() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( "<!--[if lte IE 7]>" );
		strBuf.append( "<style>" );
		strBuf.append( "#aploraNavigationBar {" );
		strBuf.append( "padding: 10px;" );
		strBuf.append( "}</style><![endif]-->" );
		setBackendIeConditionalStatement( strBuf.toString() );
	}

	public String getBackendIeConditionalStatement() {
		return backendIeConditionalStatement;
	}

	public void setBackendIeConditionalStatement( String backendIeConditionalStatement ) {
		this.backendIeConditionalStatement = backendIeConditionalStatement;
	}

	public String getFrontendIeConditionalStatement() {
		return frontendIeConditionalStatement;
	}

	public void setFrontendIeConditionalStatement( String frontendIeConditionalStatement ) {
		this.frontendIeConditionalStatement = frontendIeConditionalStatement;
	}

	public String getServletRealPath() {
		return getContext().getRealPath("/");
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setAutoLogin(boolean isAutoLogin) {
		this.isAutoLogin = isAutoLogin;
	}

	public boolean isAutoLogin() {
		return isAutoLogin;
	}

	public void setPageAccessChecked(boolean isPageAccessChecked) {
		this.isPageAccessChecked = isPageAccessChecked;
	}

	public boolean isPageAccessChecked() {
		return isPageAccessChecked;
	}

	public void setPageAccessCached(boolean isPageAccessCached) {
		this.isPageAccessCached = isPageAccessCached;
	}

	public boolean isPageAccessCached() {
		return isPageAccessCached;
	}

	public void setContext(ServletContext context) {
		this.context = context;
	}

	public ServletContext getContext() {
		return context;
	}

	public void setAplosModuleList(List<AplosModule> aplosModuleList) {
		this.aplosModuleList = aplosModuleList;
	}

	public List<AplosModule> getAplosModuleList() {
		return aplosModuleList;
	}
	
	public List<String> getIgnoreStrings() {
		List<String> ignoreStrings = new ArrayList<String>();
		ignoreStrings.add( "ignore" );
		return ignoreStrings;
	}

	@SuppressWarnings("rawtypes")
	public AplosModule getAplosModuleByClass(Class moduleClass) {
		for (AplosModule module : aplosModuleList) {
			if (module.getClass().equals(moduleClass)) {
				return module;
			}
		}
		return null;
	}

	public AplosModule getAplosModuleByName(String moduleName) {
		for (AplosModule module : aplosModuleList) {
			if (module.getModuleName().toLowerCase().equals(moduleName.toLowerCase())) {
				return module;
			}
		}
		return null;
	}

	public void setImplementationModule(AplosModule implementationModule) {
		this.implementationModule = implementationModule;
	}

	public AplosModule getImplementationModule() {
		return implementationModule;
	}

	public SiteTabPanel getSiteTabPanel() {
		SiteTabPanel siteTabPanel = (SiteTabPanel) JSFUtil.getServletContext().getAttribute( CommonUtil.getBinding( SiteTabPanel.class ) );
		if( siteTabPanel != null) {
			siteTabPanel.setSelectedTab( Website.getCurrentWebsiteFromTabSession() );
		}
		return siteTabPanel;
	}

	public void setAplosModuleFilterer(AplosModuleFilterer aplosModuleFilterer) {
		this.aplosModuleFilterer = aplosModuleFilterer;
	}

	public AplosModuleFilterer getAplosModuleFilterer() {
		return aplosModuleFilterer;
	}

	public void setWebsiteList(List<Website> websiteList) {
		this.websiteList = websiteList;
	}

	public List<Website> getWebsiteList() {
		return websiteList;
	}

	public void setDisabledAplosSiteBeans(List<Class<? extends AplosSiteBean>> disabledAplosSiteBeans) {
		this.disabledAplosSiteBeans = disabledAplosSiteBeans;
	}

	public List<Class<? extends AplosSiteBean>> getDisabledAplosSiteBeans() {
		return disabledAplosSiteBeans;
	}

	public static AplosContextListener getAplosContextListener() {
		return aplosContextListener;
	}

	public MenuCacher getMenuCacher() {
		return menuCacher;
	}

	public void setMenuCacher(MenuCacher menuCacher) {
		this.menuCacher = menuCacher;
	}

	public Map<String, Class<? extends BackingPage>> getListPageClasses() {
		return listPageClasses;
	}

	public Map<String, Class<? extends BackingPage>> getEditPageClasses() {
		return editPageClasses;
	}

	public BackingPageMetaData getBackingPageMetaData( Class<? extends BackingPage> backingPageClass ) {
		return getBackingPageMetaDataMap().get( backingPageClass );
	}

	public Map<Class<? extends BackingPage>, BackingPageMetaData> getBackingPageMetaData() {
		return getBackingPageMetaDataMap();
	}

	public Map<Class<? extends BackingPage>, BackingPageMetaData> getBackingPageMetaDataMap() {
		return backingPageMetaDataMap;
	}

	public Map<Long, WebsiteMemory> getWebsiteMemoryMap() {
		return websiteMemoryMap;
	}

	public void setWebsiteMemoryMap(Map<Long, WebsiteMemory> websiteMemoryMap) {
		this.websiteMemoryMap = websiteMemoryMap;
	}

	public Map<Long,List<String>> getUserLevelPageAccess() {
		return userLevelPageAccess;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public Map<String, AplosWorkingDirectoryInter> getAplosWorkingDirectoryEnumMap() {
		return aplosWorkingDirectoryEnumMap;
	}

	public void setAplosWorkingDirectoryEnumMap(
			Map<String, AplosWorkingDirectoryInter> aplosWorkingDirectoryEnumMap) {
		this.aplosWorkingDirectoryEnumMap = aplosWorkingDirectoryEnumMap;
	}

	public PersistentApplication getPersistentApplication() {
		return persistentApplication;
	}

	public void setPersistentApplication(PersistentApplication persistentApplication) {
		this.persistentApplication = persistentApplication;
	}

	public Map<Thread,Map<String,Object>> getThreadSessionMap() {
		return threadSessionMap;
	}

	public void setThreadSessionMap(Map<Thread,Map<String,Object>> threadSessionMap) {
		this.threadSessionMap = threadSessionMap;
	}

	public JobScheduler getJobScheduler() {
		return jobScheduler;
	}

	public void setJobScheduler(JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
	}

	public EmailSender getEmailSender() {
		return emailSender;
	}

	public void setEmailSender(EmailSender emailSender) {
		this.emailSender = emailSender;
	}

	public AplosFaceletCache getAplosFaceletCache() {
		return aplosFaceletCache;
	}

	public void setAplosFaceletCache(AplosFaceletCache aplosFaceletCache) {
		this.aplosFaceletCache = aplosFaceletCache;
	}

	public EmailManager getEmailManager() {
		return emailManager;
	}

	public void setEmailManager(EmailManager emailManager) {
		this.emailManager = emailManager;
	}

	public Set<AplosThread> getAplosThreads() {
		return aplosThreads;
	}

	public void setAplosThreadList(Set<AplosThread> aplosThreads) {
		this.aplosThreads = aplosThreads;
	}

	public boolean isErrorEmailActivated() {
		return isErrorEmailActivated;
	}

	public void setErrorEmailActivated(boolean isErrorEmailActivated) {
		this.isErrorEmailActivated = isErrorEmailActivated;
	}
}











