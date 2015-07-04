package com.aplos.common.backingpage;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.communication.AplosEmailEditPage;
import com.aplos.common.backingpage.communication.EmailTemplateEditPage;
import com.aplos.common.backingpage.communication.EmailTemplateListPage;
import com.aplos.common.backingpage.communication.OutgoingEmailListPage;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.WebServiceCallTypes;
import com.aplos.common.module.AplosModule;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class CreateWebsiteWizardPage extends EditPage {
	private static final long serialVersionUID = -4388047267214794445L;

	private static Logger logger = Logger.getLogger( CreateWebsiteWizardPage.class );
	private String name; //website name
	private String hostName;
	private String neaseUsername;
	private String neasePassword;
	private String neaseProjectTitle;
	private String googleAnalyticsId;
	private String packageRoot;
	private Long neaseProjectId = null;
	private boolean isNeaseProject = false;
	private boolean websiteLive = true;
	private boolean configurableCountries = false;
	private List<SelectItem> projectsCache = null;
	private Website createdWebsite = null;

	public CreateWebsiteWizardPage() {
		getEditPageConfig().setOkBtnActionListener( new OkBtnListener( this ) {

			private static final long serialVersionUID = -7000349998373801778L;

			@Override
			public void actionPerformed(boolean redirect) {
				if (setupWebsiteAndProject()) {
					ApplicationUtil.getMenuCacher().clearDynamicMenuCache();
					JSFUtil.redirect("common", SiteStructurePage.class);
				}
			}
		});
		getEditPageConfig().setCancelBtnActionListener( new CancelBtnListener( this ) {

			private static final long serialVersionUID = 8538872998114928437L;

			@Override
			public void actionPerformed(boolean redirect) {
				JSFUtil.redirect("common", SiteStructurePage.class);
			}
		});
	}
	
	@Override
	public boolean responsePageLoad() {
		JSFUtil.addToTabSession( "cForEachHack", JSFUtil.getCurrentBackingPage() );
		return super.responsePageLoad();
	}

	//TODO: can we change this to use module list with getPackageDisplayName() ?
	public List<SelectItem> getPackageRootSelectItems() {
        ArrayList<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem(null,"Please Select"));
        try {
        	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            assert classLoader != null;
            String path = "com/aplos/";
	        Enumeration<URL> resources = classLoader.getResources(path);
	        List<String> bannedPackages = new ArrayList<String>();
			bannedPackages.add("common");
			bannedPackages.add("cms");
			bannedPackages.add("ecommerce");
			List<File> dirs = new ArrayList<File>();
	        while (resources.hasMoreElements()) {
	            URL resource = resources.nextElement();
	            String fileName = resource.getFile();
	            String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
	            dirs.add(new File(fileNameDecoded));
	        }

	        ArrayList<String> packageNames = new ArrayList<String>();
	        for (File directory : dirs) {
	        	File[] files = directory.listFiles();
	        	if (files != null) {
		            for (File file : files) {
		                if (file.isDirectory()) {
		                	String fileName = file.getName();
		                    assert !fileName.contains(".");
		                    packageNames.add(fileName);
		                }
		            }
	        	} else {
	        		System.err.println("Unable to collect package names from " + directory.getPath());
	        	}
		    }
	        for (String packageName : packageNames) {
	        	 if (!bannedPackages.contains(packageName) && !packageName.contains(".")) {
	 	        	items.add(new SelectItem(packageName,CommonUtil.firstLetterToUpperCase(packageName)));
	 	        }
	        }
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return items;
    }

	public List<SelectItem> getNeaseProjectSelectItems() {
		if (projectsCache != null && projectsCache.size() > 2) {
			return projectsCache;
		}
		projectsCache = new ArrayList<SelectItem>();
		projectsCache.add(new SelectItem(null,"Not Selected"));
		projectsCache.add(new SelectItem(-1l,"-- Create New Project --"));
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL("http://app.networkingease.co.uk/webService?token=JKHDAIU23756UHV423&callType=" + WebServiceCallTypes.FETCH_PROJECT_LIST.toString()).openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(conn.getInputStream());
			//Parse the xml back into items
			NodeList nodes = doc.getElementsByTagName("error");
			if (nodes.item(0) != null) {
				JSFUtil.addMessageForError("Nease Web Service : " + nodes.item(0).getTextContent());
			} else {
				nodes = doc.getElementsByTagName("project");
				for(int i=1; i < nodes.getLength(); i++) {
					Node project = nodes.item(i);
					String title = null;
					Long id = null;
					for (int j=0; j < project.getChildNodes().getLength(); j++) {
						Node item = project.getChildNodes().item(j);
						if (item.getNodeName() != null && item.getNodeName().equals("title")) {
							title = item.getTextContent();
						} else if (item.getNodeName().equals("id")) {
							id = Long.parseLong(item.getTextContent());
						}
					}
					projectsCache.add(new SelectItem(id,title));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return projectsCache;
	}

	public boolean getIsNewNeaseProject() {
		return isNeaseProject && neaseProjectId != null && neaseProjectId.equals(-1l);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setNeaseUsername(String neaseUsername) {
		this.neaseUsername = neaseUsername;
	}

	public String getNeaseUsername() {
		return neaseUsername;
	}

	public void setIsWebsiteLive(boolean websiteLive) {
		this.websiteLive = websiteLive;
	}

	public boolean getIsWebsiteLive() {
		return websiteLive;
	}

	public void setIsNeaseProject(boolean isNeaseProject) {
		this.isNeaseProject = isNeaseProject;
	}

	public boolean getIsNeaseProject() {
		return isNeaseProject;
	}

	public boolean getIsNeaseProjectRequired() {
		return isNeaseProject && getIsValidationRequired();
	}

	public void setPackageRoot(String packageRoot) {
		this.packageRoot = packageRoot;
	}

	public String getPackageRoot() {
		return packageRoot;
	}

	public void setGoogleAnalyticsId(String googleAnalyticsId) {
		this.googleAnalyticsId = googleAnalyticsId;
	}

	public String getGoogleAnalyticsId() {
		return googleAnalyticsId;
	}

	public void setNeaseProjectTitle(String neaseProjectTitle) {
		this.neaseProjectTitle = neaseProjectTitle;
	}

	public String getNeaseProjectTitle() {
		return neaseProjectTitle;
	}

	public void setNeasePassword(String neasePassword) {
		this.neasePassword = neasePassword;
	}

	public String getNeasePassword() {
		return neasePassword;
	}

	public void setNeaseProjectId(Long neaseProjectId) {
		this.neaseProjectId = neaseProjectId;
	}

	public Long getNeaseProjectId() {
		return neaseProjectId;
	}

	protected void setCreatedWebsite(Website createdWebsite) {
		this.createdWebsite = createdWebsite;
	}

	protected Website getCreatedWebsite() {
		return createdWebsite;
	}

	public List<AplosModule> getModules() {
		return null;
	}


	public void setConfigurableCountries(boolean configurableCountries) {
		this.configurableCountries = configurableCountries;
	}


	public boolean isConfigurableCountries() {
		return configurableCountries;
	}

	public void createWebsiteObject() {
		//dont create two websites by resubmitting if we just had an error the first time
		if (createdWebsite == null) {
			createdWebsite = new Website();
			//set out variables
			createdWebsite.setName(name);
			createdWebsite.setPrimaryHostName(hostName);
			createdWebsite.setPackageName(packageRoot);
			createdWebsite.saveDetails();
		}
	}

	private boolean setupNeaseProject() {
		//if we have been successful once then this wont cause it to be recreated as id will be set
		if (getIsNewNeaseProject()) {
			try {
				HttpURLConnection conn = (HttpURLConnection) new URL("http://app.networkingease.co.uk/webService?token=JKHDAIU23756UHV423&callType=" + WebServiceCallTypes.CREATE_PROJECT.toString() + "&projectTitle=" + URLEncoder.encode(neaseProjectTitle, "UTF-8") + "&neaseUsername=" + URLEncoder.encode(neaseUsername, "UTF-8") + "&neasePassword=" + URLEncoder.encode(neasePassword, "UTF-8")).openConnection();
				if (ApplicationUtil.getAplosContextListener().isDebugMode()){
					logger.info("Nease Request : " + conn.getURL().toString());
				}
				conn.setInstanceFollowRedirects(false);
				conn.setRequestMethod("GET");
				conn.setDoOutput(true);
				conn.setDoInput(true);
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(conn.getInputStream());
				//Parse the xml back into items
				NodeList nodes = doc.getElementsByTagName("error");
				if (nodes.item(0) != null) {
					JSFUtil.addMessageForError("Nease Web Service : " + nodes.item(0).getTextContent());
					nodes = doc.getElementsByTagName("stacktrace");
					if (nodes.item(0) != null) {
						System.out.print(nodes.item(0).getTextContent());
					}
					return false;
				} else {
					Node idNode = doc.getElementsByTagName("id").item(0);
					if (idNode == null || idNode.getTextContent() == null) {
						return false;
					} else {
						neaseProjectId = Long.parseLong(idNode.getTextContent());
					}
				}
			} catch (org.xml.sax.SAXParseException e) {
				e.printStackTrace();
				JSFUtil.addMessageForError("Nease Web Service : No Response");
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				JSFUtil.addMessageForError("Nease Web Service : Error");
				return false;
			}
		}
		return true;
	}

	public boolean setupWebsiteAndProject() {

		//First make our nease project, if necessary
		if (!setupNeaseProject()) {
			return false;
		}

		createWebsiteObject();

		if (createdWebsite == null || createdWebsite.getId() == null) {
			return false; // we failed :(
		} else {
			ApplicationUtil.startNewTransaction(); // cleanup
		}
		createdWebsite.markAsCurrentWebsite();

		//let everyone see the basic menu structure by default
		List<UserLevel> viewableByList = new BeanDao(UserLevel.class).setIsReturningActiveBeans(true).getAll();

		//setup main tab panel
		TabPanel mainTp = new TabPanel(createdWebsite, name + " Main Tab Panel");
		mainTp.saveDetails();
		mainTp = setupDynamicMenu(mainTp, viewableByList);

		//cascade save the structure
		mainTp.saveDetails();

		//assign backend error pages to a tab
		mainTp.getDefaultTab().addDefaultPageBinding(TabClass.get(IssueReportedPage.class));
		mainTp.getDefaultTab().addDefaultPageBinding(TabClass.get(SessionTimeoutPage.class));

		//let the website create its own page structure (cmspagerevisions, etc)
		createdWebsite.createDefaultWebsiteObjects(ApplicationUtil.getAplosContextListener());

		//add our new site to the sites tabpanel (we used to)
//		MenuTab webTab = new MenuTab(viewableByList, name, "/" + packageRoot); // + website.getDefaultMenuUrl());
//		webTab.setWebsite(createdWebsite);
//		AplosContextListener aplosContextListener = (AplosContextListener) JSFUtil.getServletContext().getAttribute( AplosScopedBindings.CONTEXT_LISTENER );
//		SiteTabPanel sitePanel = aplosContextListener.getSiteTabPanel();
//		webTab.setTabPanel(sitePanel);
//		sitePanel.addMenuTab(webTab);
		
		markBoundBackingPagesPaidForAndAccessible();

		JSFUtil.addMessage(name + " Website Was Created.");
		JSFUtil.addMessageForError("You must restart the server BEFORE making any changes or navigating away. This will make the website available.");
		return true;
	}
	
	/**
	 * Checks the menu structure and makes sure all pages used are accessible, as, if they are bound by default, they should be
	 */
	public void markBoundBackingPagesPaidForAndAccessible() {
		ApplicationUtil.executeSql("UPDATE tabclass SET isPaidForAndAccessible=1 WHERE id IN (SELECT tabActionClass_id FROM menutab WHERE tabActionClass_id IS NOT NULL)");
		ApplicationUtil.executeSql("UPDATE tabclass SET isPaidForAndAccessible=1 WHERE id IN (SELECT defaultPageBindings_id FROM menutab_defaultpagebindings)");
	}

	public TabPanel setupDynamicMenu(TabPanel mainDtp, List<UserLevel> viewableByList) {

		//setup settings tab
		TabPanel settingsDtp = new TabPanel(createdWebsite, "Settings Tab Panel");
		settingsDtp.saveDetails();
		//add company settings to settings panel
		MenuTab companyTab = new MenuTab(viewableByList, "My Company", TabClass.get(CompanyDetailsEditPage.class));
		companyTab.setTabPanel(settingsDtp);
		companyTab.saveDetails();
		settingsDtp.addMenuTab(companyTab);

		//add configuration tab to settings panel
		MenuTab commonConfigurationTab = new MenuTab(viewableByList, "Configuration", TabClass.get(CommonConfigurationEditPage.class));
		commonConfigurationTab.setTabPanel(settingsDtp);
		commonConfigurationTab.saveDetails();
		settingsDtp.addMenuTab(commonConfigurationTab);
		settingsDtp.setDefaultTab(commonConfigurationTab);

		//add countries to settings panel
		if (configurableCountries) {
			MenuTab countriesTab = new MenuTab(viewableByList, "Countries", TabClass.get(CountryListPage.class));
			countriesTab.addDefaultPageBinding(TabClass.get(CountryEditPage.class));
			countriesTab.setTabPanel(settingsDtp);
			countriesTab.saveDetails();
			settingsDtp.addMenuTab(countriesTab);
		}

		//add configuration tab to settings panel
		MenuTab usersTab = new MenuTab(viewableByList, "Users", TabClass.get(SystemUserListPage.class));
		usersTab.addDefaultPageBinding(TabClass.get(SystemUserEditPage.class));
		usersTab.setTabPanel(settingsDtp);
		usersTab.saveDetails();
		settingsDtp.addMenuTab(usersTab);
		settingsDtp.setDefaultTab(usersTab);

		//add configuration tab to settings panel
		MenuTab emailTemplatesTab = new MenuTab(viewableByList, "Email templates", TabClass.get(EmailTemplateListPage.class));
		emailTemplatesTab.addDefaultPageBinding(TabClass.get(EmailTemplateEditPage.class));
		emailTemplatesTab.setTabPanel(settingsDtp);
		emailTemplatesTab.saveDetails();
		settingsDtp.addMenuTab(emailTemplatesTab);
		settingsDtp.setDefaultTab(emailTemplatesTab);

		//add settings panel to main panel
		MenuTab settingsTab = new MenuTab(viewableByList, "Settings", settingsDtp);
		settingsTab.setTabPanel(mainDtp);
		settingsTab.saveDetails();
		mainDtp.addMenuTab(settingsTab);
		mainDtp.setDefaultTab(settingsTab);

		//setup nease tab if selected
		if (isNeaseProject) {
			//make sure when admin user clicks the tab everything is ready for him/her
			SystemUser adminUser = CommonConfiguration.getCommonConfiguration().getDefaultAdminUser();
			if (adminUser != null) {
				adminUser.setNeasePassword(neasePassword);
				adminUser.setNeaseUsername(neaseUsername);
			}
			
			createdWebsite.setNeaseProjectId(neaseProjectId);
			
			MenuTab neaseTab = new MenuTab(viewableByList, "Networking Ease", TabClass.get(NeaseGatewayPage.class));
			neaseTab.setTabPanel(mainDtp);
			neaseTab.saveDetails();
			mainDtp.addMenuTab(neaseTab);
		}
		settingsDtp.saveDetails();
		
		//create marketing tab and apply emails to it
		TabPanel marketingStp = new TabPanel(getCreatedWebsite(), "Marketing Tab Panel");

		MenuTab emailsTab = new MenuTab(viewableByList, "Emails", TabClass.get(OutgoingEmailListPage.class));
		emailsTab.addDefaultPageBinding(TabClass.get(AplosEmailEditPage.class));
		emailsTab.setTabPanel(marketingStp);
		emailsTab.saveDetails();
		marketingStp.addMenuTab(emailsTab);
		marketingStp.setDefaultTab(emailsTab);

		MenuTab marketingTab = new MenuTab(viewableByList, "Marketing", marketingStp);
		marketingTab.setTabPanel(mainDtp);
		marketingTab.saveDetails();
		mainDtp.addMenuTab(marketingTab);
		
		marketingStp.saveDetails();

		return mainDtp;
	}

}

