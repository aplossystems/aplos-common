package com.aplos.common.beans;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.aplos.common.AplosUrl;
import com.aplos.common.WebsiteMemory;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.DiscriminatorType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.enums.BulkMessageFinderEnum;
import com.aplos.common.enums.CombinedResourceStatus;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.enums.VatOffset;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@DiscriminatorColumn(
    name="WEBSITE_TYPE",
    discriminatorType=DiscriminatorType.STRING
)
@Cache
public class Website extends AplosBean {
	private static final long serialVersionUID = -4643938470702372634L;

	private static Logger logger = Logger.getLogger( Website.class );
	private String name;
	private String packageName;
	private String defaultMenuUrl;
	private String primaryHostName;
	private String alternativeHostName;
	private String subdomain = "www";
	@OneToOne
	private VatType defaultVatType;
	private VatOffset vatOffset;
	private boolean isSslEnabled = false;
	private Long neaseProjectId;
	private String googleAnalyticsId;
	
	private CombinedResourceStatus combinedResourceStatus = CombinedResourceStatus.DISABLED;
	
	private SslProtocolEnum defaultSslProtocolEnum = SslProtocolEnum.FORCE_HTTP;

	private transient WebsiteMemory websiteMemory;
	@OneToOne
	@Cascade({CascadeType.ALL})
	private MailServerSettings mailServerSettings = new MailServerSettings();
	private boolean isUsingDefaultMailServerSettings = true;
	@Transient
	private boolean projectStylesExist = false;

	public Website() {
		
	}

	public void createWebsiteMemory() {
		if( getClass().getResource( "/../../resources/styles/" + packageName + ".css" ) != null ) {
			setProjectStylesExist(true);
		}
		setWebsiteMemory(ApplicationUtil.getAplosContextListener().getWebsiteMemoryMap().get( getId() ));
		if( getWebsiteMemory() == null ) {
			setWebsiteMemory(new WebsiteMemory());
			getWebsiteMemory().setMainDynamicBundle( new DynamicBundle( this ) );
			setWebsiteMemoryInMap();
		}
	}

	public void setWebsiteMemoryInMap() {
		if( !isNew() && ApplicationUtil.getAplosContextListener().getWebsiteMemoryMap().get( getId() ) == null ) {
			ApplicationUtil.getAplosContextListener().getWebsiteMemoryMap().put(getId(), getWebsiteMemory() );
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends EmailTemplate> T loadEmailTemplate( EmailTemplateEnum emailTemplateEnum ) {
		return loadEmailTemplate( emailTemplateEnum, false );
	}

	@SuppressWarnings("unchecked")
	public <T extends EmailTemplate> T loadEmailTemplate( EmailTemplateEnum emailTemplateEnum, boolean hibernateInitialise ) {
		EmailTemplate readOnlyEmailTemplate = getEmailTemplateMap().get( emailTemplateEnum );
		EmailTemplate loadedEmailTemplate = null;

		if( readOnlyEmailTemplate != null ) {
			loadedEmailTemplate = new BeanDao( EmailTemplate.class ).get( readOnlyEmailTemplate.getId() );
			loadedEmailTemplate = loadedEmailTemplate.getSaveableBean();

			/*
			 * Get the freshest content from file if it's using default content
			 */
			if( !readOnlyEmailTemplate.isValidated() && readOnlyEmailTemplate.isUsingDefaultContent() ) {
				loadedEmailTemplate.setContent(loadedEmailTemplate.getDefaultContent());
				loadedEmailTemplate.saveDetails();
				readOnlyEmailTemplate.setValidated(true);
			}
		}

		return (T) loadedEmailTemplate;
	}

	@SuppressWarnings("unchecked")
	public <T extends SmsTemplate> T loadSmsTemplate( Class<? extends SmsTemplate> smsTemplateClass ) {
		return loadSmsTemplate( smsTemplateClass, false );
	}

	@SuppressWarnings("unchecked")
	public <T extends SmsTemplate> T loadSmsTemplate( Class<? extends SmsTemplate> smsTemplateClass, boolean hibernateInitialise ) {
		SmsTemplate smsTemplate = getSmsTemplateMap().get( smsTemplateClass );

		if( smsTemplate != null ) {
			smsTemplate = new BeanDao( SmsTemplate.class ).get( smsTemplate.getId() );
		}
		//we need the getImplementation otherwise we cant cast and use what we get back
//		smsTemplate = HibernateUtil.getImplementation(smsTemplate);
//		if( hibernateInitialise ) {
//			smsTemplate.hibernateInitialise(true);
//		}
		return (T) smsTemplate;
	}

	public void checkBundleKeys( AplosContextListener aplosContextListener, List<BundleKey> availableBundleKeys ) {
		getMainDynamicBundle().checkBundleKeys( aplosContextListener, availableBundleKeys );
	}

	@Override
	public void saveBean(SystemUser currentUser) {
		boolean wasNew = isNew();
		super.saveBean(currentUser);
		if( wasNew ) {
			setWebsiteMemoryInMap();
		}
	}

	public AplosUrl determineDefaultMenuUrl() {
		if( defaultMenuUrl == null || defaultMenuUrl.equals( "" ) ) {
			return ApplicationUtil.getAplosContextListener().getMenuCacher().getMainTabPanel( this ).getDefaultTabAction();
		} else {
			AplosUrl aplosUrl = new AplosUrl( defaultMenuUrl );
			aplosUrl.addContextPath();
			return aplosUrl;
		}
	}

	public String getSessionTimeoutUrl( String requestUrl ) {
		return null;
	}

	public String getPageNotFoundUrl( String requestUrl ) {
		return null;
	}

	public URL checkFileLocations( String resourceName, String resourcesPath ) {
		return getClass().getResource( ("/com/aplos/" + getPackageName() + "/" + resourcesPath + resourceName).replace( "//", "/" ) );
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public void createDefaultWebsiteObjects( AplosContextListener aplosContextListener ) {}

	public void checkAndUpdateWebsiteState( AplosContextListener aplosContextListener ) {}

	public static Website getWebsite() {
		Website website = (Website)new BeanDao( Website.class ).getFirstBeanResult();
		// This can sometimes happen on start up with the old systems.
		if( website != null ) {
//			HibernateUtil.initialise( website, true );
			website.createWebsiteMemory();
		}
		return website;
	}

	public static void addGoogleAnalyticsCode( StringBuffer xhtmlBuffer, String googleAnalyticsId ) {
		if( !JSFUtil.isLocalHost() ) {
			xhtmlBuffer.append("<script>");
			xhtmlBuffer.append("(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){");
			xhtmlBuffer.append("(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),");
			xhtmlBuffer.append("m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)");
			xhtmlBuffer.append("})(window,document,'script','//www.google-analytics.com/analytics.js','ga');");
			xhtmlBuffer.append("ga('create', '");
			xhtmlBuffer.append(googleAnalyticsId);
			xhtmlBuffer.append("', 'auto');");
			xhtmlBuffer.append("ga('send', 'pageview');");
			xhtmlBuffer.append("</script>");
		}
	}

	public void setGoogleAnalyticsId(String googleAnalyticsId) {
		this.googleAnalyticsId = googleAnalyticsId;
	}

	public String getGoogleAnalyticsId() {
		return googleAnalyticsId;
	}

	public static Website loadWebsiteBySession() {
		return (Website) new BeanDao( Website.class ).get( getCurrentWebsiteFromTabSession().getId() );
	}

	public static Website getCurrentWebsiteFromTabSession() {
		return getCurrentWebsiteFromTabSession( JSFUtil.getSessionTemp() );
	}

	public static Website getCurrentWebsiteFromTabSession( HttpSession session ) {
		return JSFUtil.getBeanFromTabSession( AplosScopedBindings.CURRENT_WEBSITE );
	}

	public static void addCurrentWebsiteToTabSession( Website website ) {
		JSFUtil.addToTabSession( AplosScopedBindings.CURRENT_WEBSITE, website );
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		addToScope( CommonUtil.getBinding( Website.class ), this, associatedBeanScope );
	}

	public static void removeWebsiteFromTabSession() {
		JSFUtil.addToTabSession( AplosBean.getBinding( Website.class ), null );
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getDefaultMenuUrl() {
		return defaultMenuUrl;
	}

	public void setDefaultMenuUrl(String defaultMenuUrl) {
		this.defaultMenuUrl = defaultMenuUrl;
	}

	public void setPrimaryHostName(String hostName) {
		this.primaryHostName = hostName;
	}

	public String getPrimaryHostName( boolean ignoreLocal ) {
		if( !ignoreLocal && JSFUtil.isLocalHost() ) {
			return "localhost:8080";
		} else {
			return primaryHostName;
		}
	}

	public String getPrimaryHostName() {
		return getPrimaryHostName( false );
	}

	public void markAsCurrentWebsite() {
		JSFUtil.addToTabSession( AplosScopedBindings.CURRENT_WEBSITE, this );
	}
	
	@Override
	public void delete() {
		super.delete();
		ApplicationUtil.getAplosContextListener().getWebsiteList().remove( this );
	}
	
	public void reinstate() {
		super.reinstate();
		ApplicationUtil.getAplosContextListener().updateWebsite( this );
	}

	public static void refreshWebsiteInSession() {
		//make sure we havent wandered anywhere we need a different website
		if ( JSFUtil.getFacesContext() != null && JSFUtil.getFacesContext().getViewRoot() != null ) {
			String viewId = JSFUtil.getFacesContext().getViewRoot().getViewId();
			if ((Website.getCurrentWebsiteFromTabSession() == null ||
					!viewId.startsWith( "/" + Website.getCurrentWebsiteFromTabSession().getPackageName())) ) { //HibernateUtil.getCurrentSession().getTransaction().isActive()) {

	        	List<Website> websiteList = ApplicationUtil.getAplosContextListener().getWebsiteList();
	        	for ( Website tempWebsite : websiteList ) {
	        		if ( viewId.startsWith( "/" + tempWebsite.getPackageName() ) ) {
	        			tempWebsite.markAsCurrentWebsite();
						logger.info( "Log Website added to Session through view root : " + tempWebsite.getDisplayName() + " (" + tempWebsite.getId() + ")" );
//						System.out.println( "Website added to Session through viewId (" + viewId + "): " + tempWebsite.getDisplayName() + " (" + tempWebsite.getId() + ")" );
	        			return;
	        		}
	        	}
			}
		}

		//if we havent currently got a website look at the hostname and find one
		if ( Website.getCurrentWebsiteFromTabSession() == null ) {// && HibernateUtil.getCurrentSession().getTransaction().isActive() ) {
				String hostname = JSFUtil.getRequest().getServerName();
				boolean isLocalHost = JSFUtil.isLocalHost();

				List<Website> websiteList = ApplicationUtil.getAplosContextListener().getWebsiteList();
				if( !isLocalHost ) {
					for( Website tempWebsite : websiteList ) {
						if( (tempWebsite.getPrimaryHostName() != null && hostname.contains( tempWebsite.getPrimaryHostName() )) ) {
							tempWebsite.markAsCurrentWebsite();
							logger.info( "Website added to Session through primary hostname : " + tempWebsite.getPrimaryHostName() + " request hostname : " + hostname + " website id : " + tempWebsite.getId() );
//							System.out.println( "Website added to Session through primary hostname : " + tempWebsite.getPrimaryHostName() + " request hostname : " + hostname + " website id : " + tempWebsite.getId() );
							break;
						} else if( (tempWebsite.getAlternativeHostName() != null && hostname.contains( tempWebsite.getAlternativeHostName() )) ) {
							tempWebsite.markAsCurrentWebsite();
							logger.info( "Website added to Session through alternative hostname : " + tempWebsite.getAlternativeHostName() + " request hostname : " + hostname + " website id : " + tempWebsite.getId() );
//							System.out.println( "Website added to Session through alternative hostname : " + tempWebsite.getAlternativeHostName() + " request hostname : " + hostname + " website id : " + tempWebsite.getId() );
							break;
						}
					}
				}

				if ( Website.getCurrentWebsiteFromTabSession() == null ) {
					for( Website tempWebsite : websiteList ) {
						if (tempWebsite.getId() != null &&
							tempWebsite.getId().equals(1l)) {
							tempWebsite.markAsCurrentWebsite();
							logger.info( "Website added to Session through website list : " + tempWebsite.getDisplayName() + " (" + tempWebsite.getId() + ")" );
//							System.out.println( "Website added to Session through website list : " + tempWebsite.getDisplayName() + " (" + tempWebsite.getId() + ")" );
							break;
						}
					}
					if( Website.getCurrentWebsiteFromTabSession() == null ) {
						//this line was fetching website with id 1
						//of course it wont contain the atoms we have added to the list then
						//I've changed this else block so this is now a fallback only if we cant
						//find website 1 in the list
						Website website = Website.getWebsite();
						if (website != null) {
//							HibernateUtil.initialise( website, true );
							website.markAsCurrentWebsite();
							logger.info( "Website added to Session through getWebsite() : " + website.getDisplayName() + " (" + website.getId() + ")" );
//							System.out.println( "Website added to Session through getWebsite() : " + website.getDisplayName() + " (" + website.getId() + ")" );
						} else {
							logger.info( "No websites available to add to Session");
						}
					}
				}
//			} catch( MalformedURLException mUrlEx ) {
//				ApplicationUtil.getAplosContextListener().handleError( mUrlEx);
//			}

		}


	}
	
	public List<SelectItem> getBulkEmailFinderSelectItems() {
		List<BasicBulkMessageFinder> bulkEmailFinders = getBulkEmailFinders();
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		for (BasicBulkMessageFinder tempBulkMessageFinder : bulkEmailFinders) {
			SelectItem selectItem = new SelectItem( tempBulkMessageFinder, tempBulkMessageFinder.getName() ); 
			selectItems.add(selectItem);
		}
		return selectItems;
	}
	
	public List<SelectItem> getBulkSmsFinderSelectItems() {
		List<BasicBulkMessageFinder> bulkSmsFinders = getBulkSmsFinders();
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		for (BasicBulkMessageFinder tempBulkMessageFinder : bulkSmsFinders) {
			SelectItem selectItem = new SelectItem( tempBulkMessageFinder, tempBulkMessageFinder.getName() ); 
			selectItems.add(selectItem);
		}
		return selectItems;
	}

	public List<BasicBulkMessageFinder> getBulkSmsFinders() {
		List<BasicBulkMessageFinder> bulkMessageFinders = new ArrayList<BasicBulkMessageFinder>( getBulkMessageFinderMap().values() );
		for ( int i = bulkMessageFinders.size() - 1; i >= 0; i-- ) {
			if( !BulkSmsSource.class.isAssignableFrom(bulkMessageFinders.get(i).getBulkMessageFinderClass()) ) {
				bulkMessageFinders.remove( i );
			}
		}
		return bulkMessageFinders;
	}

	public List<BasicBulkMessageFinder> getBulkEmailFinders() {
		List<BasicBulkMessageFinder> bulkMessageFinders = new ArrayList<BasicBulkMessageFinder>( getBulkMessageFinderMap().values() );
		for ( int i = bulkMessageFinders.size() - 1; i >= 0; i-- ) {
			if( !BulkEmailSource.class.isAssignableFrom(bulkMessageFinders.get(i).getBulkMessageFinderClass()) ) {
				bulkMessageFinders.remove( i );
			}
		}
		return bulkMessageFinders;
	}

	public void setSslEnabled(boolean isSslEnabled) {
		this.isSslEnabled = isSslEnabled;
	}

	public boolean isSslEnabled() {
		return isSslEnabled;
	}

	public void setMainDynamicBundle(DynamicBundle mainDynamicBundle) {
		getWebsiteMemory().setMainDynamicBundle( mainDynamicBundle );
	}

	public DynamicBundle getMainDynamicBundle() {
		if (getWebsiteMemory() != null) { //site creation
			return getWebsiteMemory().getMainDynamicBundle();
		}
		return null;
	}

	public String getAlternativeHostName() {
		return alternativeHostName;
	}

	public void setAlternativeHostName(String alternativeHostName) {
		this.alternativeHostName = alternativeHostName;
	}

	public Map<Class<? extends SmsTemplate>, SmsTemplate> getSmsTemplateMap() {
		return getWebsiteMemory().getSmsTemplateMap();
	}

	public void setSmsTemplateMap(Map<Class<? extends SmsTemplate>, SmsTemplate> smsTemplateMap) {
		getWebsiteMemory().setSmsTemplateMap( smsTemplateMap );
	}

	public Map<EmailTemplateEnum, EmailTemplate> getEmailTemplateMap() {
		return getWebsiteMemory().getEmailTemplateMap();
	}

	public void setEmailTemplateMap(Map<EmailTemplateEnum, EmailTemplate> emailTemplateMap) {
		getWebsiteMemory().setEmailTemplateMap( emailTemplateMap );
	}

	public Map<BulkMessageFinderEnum, BasicBulkMessageFinder> getBulkMessageFinderMap() {
		return getWebsiteMemory().getBulkMessageFinderMap();
	}

	public void setBulkMessageFinderMap(Map<BulkMessageFinderEnum, BasicBulkMessageFinder> bulkMessageFinderMap) {
		getWebsiteMemory().setBulkMessageFinderMap( bulkMessageFinderMap );
	}

	public VatType getDefaultVatType() {
		return defaultVatType;
	}

	public void setDefaultVatType(VatType defaultVatType) {
		this.defaultVatType = defaultVatType;
	}

	public VatOffset getVatOffset() {
		return vatOffset;
	}

	public void setVatOffset(VatOffset vatOffset) {
		this.vatOffset = vatOffset;
	}
	
	public void setNeaseProjectId(Long neaseProjectId) {
		this.neaseProjectId = neaseProjectId;
	}

	public Long getNeaseProjectId() {
		return neaseProjectId;
	}

	public SslProtocolEnum getDefaultSslProtocolEnum() {
		return defaultSslProtocolEnum;
	}

	public void setDefaultSslProtocolEnum(SslProtocolEnum defaultSslProtocolEnum) {
		this.defaultSslProtocolEnum = defaultSslProtocolEnum;
	}

	public MailServerSettings getMailServerSettings() {
		return mailServerSettings;
	}

	public void setMailServerSettings(MailServerSettings mailServerSettings) {
		this.mailServerSettings = mailServerSettings;
	}

	public boolean isUsingDefaultMailServerSettings() {
		return isUsingDefaultMailServerSettings;
	}

	public void setUsingDefaultMailServerSettings(
			boolean isUsingDefaultMailServerSettings) {
		this.isUsingDefaultMailServerSettings = isUsingDefaultMailServerSettings;
	}

	public String getSubdomain() {
		return subdomain;
	}

	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}

	public boolean isProjectStylesExist() {
		return projectStylesExist;
	}

	public void setProjectStylesExist(boolean projectStylesExist) {
		this.projectStylesExist = projectStylesExist;
	}

	public WebsiteMemory getWebsiteMemory() {
		return websiteMemory;
	}

	public void setWebsiteMemory(WebsiteMemory websiteMemory) {
		this.websiteMemory = websiteMemory;
	}

	public CombinedResourceStatus getCombinedResourceStatus() {
		return combinedResourceStatus;
	}

	public void setCombinedResourceStatus(CombinedResourceStatus combinedResourceStatus) {
		this.combinedResourceStatus = combinedResourceStatus;
	}
}
