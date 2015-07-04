package com.aplos.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPageState;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.interfaces.DynamicMenuNode;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.SiteTabPanel;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;


public class MenuCacher {
	private HashMap<Website,TabPanel> websitePanelMap = new HashMap<Website,TabPanel>();
	private Map<String,Set<MenuTab>> cachedMenuTabByUrlMap = new HashMap<String,Set<MenuTab>>();

	public void clearDynamicMenuCache() {
		//this will allow the menu to be re-rendered when we update an object
//		backingPageMap = new HashMap<String,BackingPageConfig>();
		websitePanelMap = null;
		ApplicationUtil.getAplosContextListener().setWebsiteList( new ArrayList<Website>() );
		List<Website> loadedWebsiteList = new BeanDao( Website.class ).setIsReturningActiveBeans(true).getAll();
		for( Website tempWebsite : loadedWebsiteList ) {
//			HibernateUtil.initialise( tempWebsite, true );
			ApplicationUtil.getAplosContextListener().getWebsiteList().add( tempWebsite );
		}
		if (((SiteTabPanel)JSFUtil.getServletContext().getAttribute( CommonUtil.getBinding( SiteTabPanel.class ) )) != null) {
			((SiteTabPanel)JSFUtil.getServletContext().getAttribute( CommonUtil.getBinding( SiteTabPanel.class ) )).resetTabs(ApplicationUtil.getAplosContextListener());
		}
	}

	public void loadValuesIntoCache() {
		BeanDao dynamicTabPanelDao = new BeanDao( TabPanel.class );
		List<TabPanel> dynamicTabPanels = dynamicTabPanelDao.getAll();
		for( TabPanel dynamicTabPanel : dynamicTabPanels ) {
//			HibernateUtil.initialise( dynamicTabPanel, true );
			if( dynamicTabPanel.getParent() == null ) {
				websitePanelMap.put( dynamicTabPanel.getWebsite(), dynamicTabPanel );
//				dynamicTabPanel.hibernateInitialiseChildren( true );
			}
		}

		BeanDao menuTabDao = new BeanDao( MenuTab.class );
		List<MenuTab> menuTabs = menuTabDao.getAll();
		for( MenuTab menuTab : menuTabs ) {
			putMenuTab( menuTab );
		}
	}
	
	public void replaceMenuTab( MenuTab menuTab ) {
		Set<MenuTab> tempMenuTabSet;
		for( String tempKey : getCachedMenuTabByUrlMap().keySet() ) {
			tempMenuTabSet = getCachedMenuTabByUrlMap().get( tempKey );
			tempMenuTabSet.remove( menuTab );
		}
		putMenuTab( menuTab );
	}
	
	public void putMenuTab( MenuTab menuTab ) {
		Website tempWebsite = findWebsite( menuTab );
		String tempPath;
		Set<MenuTab> tempMenuTabSet;
//		HibernateUtil.initialise( menuTab, true );
		if( menuTab.getTabActionClass() != null ) {
			tempPath = new BackingPageUrl( tempWebsite, menuTab.getTabActionClass().getBackingPageClass(), false).getPath();
			tempMenuTabSet = getCachedMenuTabByUrlMap().get( tempPath );
			if( tempMenuTabSet == null ) {
				tempMenuTabSet = new HashSet<MenuTab>();
				getCachedMenuTabByUrlMap().put( tempPath, tempMenuTabSet );
			}
			tempMenuTabSet.add( menuTab );
		}

		for (TabClass bindingClass : menuTab.getDefaultPageBindings()) {
			tempPath = new BackingPageUrl( tempWebsite, bindingClass.getBackingPageClass(), false).getPath();
			tempMenuTabSet = getCachedMenuTabByUrlMap().get( tempPath );
			if( tempMenuTabSet == null ) {
				tempMenuTabSet = new HashSet<MenuTab>();
				getCachedMenuTabByUrlMap().put( tempPath, tempMenuTabSet );
			}
			tempMenuTabSet.add( menuTab );
		}
	}

	public Website findWebsite( MenuTab menuTab ) {
		DynamicMenuNode menuNode = menuTab;
		while( menuNode.getParent() != null ) {
			menuNode = menuNode.getParent();
			if( menuNode instanceof TabPanel && ((TabPanel)menuNode).getWebsite() != null ) {
				return ((TabPanel)menuNode).getWebsite();
			}
		}
		return null;
	}
	
	public Set<MenuTab> getMenuTabsByUrl( String menuTabUrl ) {
		return getCachedMenuTabByUrlMap().get( menuTabUrl );
	}

	public void setWebsitePanelMap(HashMap<Website,TabPanel> websitePanelMap) {
		this.websitePanelMap = websitePanelMap;
	}

	public HashMap<Website,TabPanel> getWebsitePanelMap() {
		return websitePanelMap;
	}

	public TabPanel getMainTabPanel( Website website ) {
		TabPanel mainPanel = null;

		if (website != null && getWebsitePanelMap() != null) {
			if (getWebsitePanelMap().get(website) == null) {
				loadValuesIntoCache();
			}
			SystemUser systemUser = JSFUtil.getLoggedInUser();
			if( systemUser != null 
					&& systemUser.getUserLevel() != null
					&& systemUser.getUserLevel().getMainTabPanel() != null ) {
				mainPanel = systemUser.getUserLevel().getMainTabPanel();
			} else {
				mainPanel = getWebsitePanelMap().get(website);
			}
		}
		return mainPanel;
	}

	public TabPanel getCurrentMainTabPanel( boolean setSelectedTab ) {
		TabPanel dynamicTabPanel = getMainTabPanel( Website.getCurrentWebsiteFromTabSession() );
		if ( setSelectedTab ) {
			MenuTab childMenuTab = getCurrentMenuTab();
			if ( childMenuTab != null ) {
				DynamicMenuNode menuNode = childMenuTab.getParent();
				while( menuNode != null  ) {
					if ( menuNode.getParent() == null && menuNode instanceof TabPanel ) {
						((TabPanel)menuNode).setSelectedTab( childMenuTab );
						break;
					} else if ( menuNode instanceof MenuTab ) {
						childMenuTab = (MenuTab) menuNode;
					}
					menuNode = menuNode.getParent();
				}
			}
		}
		return dynamicTabPanel;
	}
	public MenuTab getMenuTab( String requestUrl ) {
		return getMenuTab( requestUrl, 1 );
	}

	public MenuTab getMenuTab( String requestUrl, int historyListOffset ) {
		Set<MenuTab> menuTabSet = getMenuTabList( requestUrl );
		MenuTab menuTab = null;
		/*
		 * This fixes the issue of when two or more menutabs have the same URL.  This often happens
		 * when list pages share the same edit page.  The edit page is on two different defaultMenuBinding
		 * lists and therefore connected to more than one menu tab.
		 */
		if( menuTabSet != null ) {
			if( menuTabSet.size() == 1 ) {
				menuTab = menuTabSet.iterator().next();
			} else if( menuTabSet.size() > 1 ) {
				String requestMenuTabId = JSFUtil.getRequest().getParameter( AplosScopedBindings.MENU_TAB_ID );
				if( requestMenuTabId != null ) {
					for( MenuTab tempMenuTab : menuTabSet ) {
						if( String.valueOf( tempMenuTab.getId() ).equals( requestMenuTabId ) ) {
							menuTab = tempMenuTab;
							break;
						}
					}
				}
				if( menuTab == null ) {
					List<BackingPageState> historyList = JSFUtil.getHistoryList();
					if( historyList.size() > historyListOffset ) {
						String redirectUrl = historyList.get( historyList.size() - ( 1 + historyListOffset ) ).getRedirectUrl(); 
						MenuTab previousMenuTab = getMenuTab( redirectUrl.substring(0, redirectUrl.indexOf(".")), historyListOffset + 1 );
						if( previousMenuTab != null ) {
							// check default bindings
							for( MenuTab tempMenuTab : menuTabSet ) {
								if( tempMenuTab.equals( previousMenuTab ) ) {
									menuTab = previousMenuTab;
									break;
								}
							}
							// check actual bindings
							if( menuTab == null ) {
								for( MenuTab tempMenuTab : menuTabSet ) {
									if( tempMenuTab.getParent() != null && previousMenuTab.equals( tempMenuTab.getParent().getParent() ) ) {
										menuTab = tempMenuTab;
										break;
									}
								}
							}
						}
					}
				}
				
				if( menuTab == null ) {
					menuTab = menuTabSet.iterator().next();
				}
			}
		}
		return menuTab;
	}

	public MenuTab getCurrentMenuTab() {
		return getMenuTab(JSFUtil.getCurrentPath( true ));
	}

	public Set<MenuTab> getCurrentMenuTabList() {
		return getMenuTabList( JSFUtil.getCurrentPath( true ) );
	}

	public Set<MenuTab> getMenuTabList( String requestUrl ) {
		return getCachedMenuTabByUrlMap().get( requestUrl );
	}

	public TabPanel getLowestTabPanel( boolean setSelectedTab ) {
		MenuTab menuTab = getCurrentMenuTab();
		if (menuTab != null && menuTab.getParent() instanceof TabPanel ) {
			if( setSelectedTab ) {
				((TabPanel) menuTab.getParent()).setSelectedTab( menuTab );
			}
			return (TabPanel) menuTab.getParent();
		}
		return null;
	}

	private Map<String,Set<MenuTab>> getCachedMenuTabByUrlMap() {
		return cachedMenuTabByUrlMap;
	}
}
