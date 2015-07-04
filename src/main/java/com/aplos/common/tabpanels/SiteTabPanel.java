package com.aplos.common.tabpanels;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.CacheViewPage;
import com.aplos.common.backingpage.ClassAccessPage;
import com.aplos.common.backingpage.MenuTabEditPage;
import com.aplos.common.backingpage.SiteStructurePage;
import com.aplos.common.backingpage.TabPanelEditPage;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
public class SiteTabPanel extends TabPanel {

	private static final long serialVersionUID = -6576201637924912750L;
	private Website currentSite;

	public SiteTabPanel() {
		//this constructor is for jsf for ManagedBean->getSiteSelectItemBeans
		//super();
		//this.setName("Site Selection Tab Panel");
		//resetTabs(ApplicationUtil.getAplosContextListener());
	}

	public SiteTabPanel(AplosContextListener contextLstn) {
		super();
		this.setName("Site Selection Tab Panel");
		resetTabs(contextLstn);
	}

	@Override
	public MenuTab getDefaultTab() {
		if (getFullTabList().size() > 0) {
			return getFullTabList().get(0);
		}
		return null;
	}

	public void setSelectedTab( Website website ) {
		for( int i = 0, n = getFullTabList().size(); i < n; i++ ) {
			if( getFullTabList().get( i ).getWebsite() != null &&
				getFullTabList().get( i ).getWebsite().equals( website ) ) {
				super.setSelectedTab( getFullTabList().get( i ) );
				break;
			}
		}
	}
	
	public void addDefaultTabs(ArrayList<UserLevel> superUserPseudoList) {
		
		MenuTab featuresMenuTab = new MenuTab(superUserPseudoList, "Page Availability", new TabClass(ClassAccessPage.class) );
		featuresMenuTab.setTabPanel(this);
		featuresMenuTab.setPackageName("common");
		featuresMenuTab.setIsHiddenFromSuperuser(false);
		addMenuTab(featuresMenuTab);
		
		MenuTab menusMenuTab = new MenuTab(superUserPseudoList, "Site Structure", new TabClass(SiteStructurePage.class) );
		menusMenuTab.addDefaultPageBinding(new TabClass(MenuTabEditPage.class));
		menusMenuTab.addDefaultPageBinding(new TabClass(TabPanelEditPage.class));
		menusMenuTab.setTabPanel(this);
		menusMenuTab.setPackageName("common");
		menusMenuTab.setIsHiddenFromSuperuser(false);
		addMenuTab(menusMenuTab);

		MenuTab cacheViewMenuTab = new MenuTab(superUserPseudoList, "Cache View", new TabClass(CacheViewPage.class) );
		cacheViewMenuTab.setTabPanel(this);
		cacheViewMenuTab.setPackageName("common");
		cacheViewMenuTab.setIsHiddenFromSuperuser(false);
		addMenuTab(cacheViewMenuTab);
	}

	public void resetTabs(AplosContextListener contextListener) {

		this.setFullTabList(new ArrayList<MenuTab>());

		ArrayList<UserLevel> superUserPseudoList = new ArrayList<UserLevel>();
		UserLevel superUserLevel = CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel();
//		HibernateUtil.initialise(superUserLevel, true);
		superUserPseudoList.add(superUserLevel);

		addDefaultTabs(superUserPseudoList);

		String tabName = "Database Hold: <strong id='dbholdstatus' style='color:";
		if (CommonConfiguration.getCommonConfiguration().isDatabaseOnHold()) {
			tabName += "darkred;text-shadow: firebrick 1px 1px 4px";
		} else {
			tabName += "#448044";
		}
		tabName += ";' title='When switched on other users will not be able to use the system to affect the database'>";
		if (CommonConfiguration.getCommonConfiguration().isDatabaseOnHold()) {
			tabName += "ON";
		} else {
			tabName += "OFF";
		}
		tabName += "</strong>";
		MenuTab databaseHoldTab = new MenuTab(superUserPseudoList, tabName, new TabClass(SiteStructurePage.class) );
		databaseHoldTab.setEffectiveTabAction("#{viewUtil.toggleDatabaseHold()}");
		databaseHoldTab.setHasEffectiveEl(true);
		databaseHoldTab.setTabPanel(this);
		databaseHoldTab.setPackageName("common");
		databaseHoldTab.setIsHiddenFromSuperuser(false);
		addMenuTab(databaseHoldTab);

//		HibernateUtil.getCurrentSession().beginTransaction();

		if (contextListener.getMenuCacher().getWebsitePanelMap() == null) {
			pullTreeToMap(contextListener);
		}
		//HibernateUtil.getCurrentSession().getTransaction().commit();
		//contextLstn.setWebsiteList(websites);
	}

	private void pullTreeToMap(AplosContextListener contextListener) {
		BeanDao panelDao = new BeanDao(TabPanel.class);
//		panelDao.setSelectCriteria("distinct bean");
//		panelDao.setJoinFetch( " join fetch bean.fullTabList ftl");

		List<TabPanel> allPanels = panelDao.setIsReturningActiveBeans(true).getAll();

		//List<TabPanel> mainPanels = new ArrayList<TabPanel>();
//		for (MenuTab tab : getFullTabList()) {
//			Website site = tab.getWebsite();
//			if (site != null) {
//				for (TabPanel panel : allPanels) {
//					if (panel.getParentMenuTab() == null && panel.getWebsite() != null && panel.getWebsite().equals(site)) {
////						panel.hibernateInitialise( true );
////						panel.hibernateInitialiseChildren(true);
////						contextListener.getMenuCacher().addTabPanelToWebsitePanelMap( site, panel, true );
//					}
//				}
//			}
//		}

	}

	public void setCurrentSite(Website currentSite) {
		this.currentSite = currentSite;
	}

	public Website getCurrentSite() {
		if (Website.getCurrentWebsiteFromTabSession() != null) {
			currentSite = Website.getCurrentWebsiteFromTabSession();
		}
		return currentSite;
	}

	public SelectItem[] getSiteSelectItemBeans() {
		SystemUser systemUser = JSFUtil.getLoggedInUser();
		if( systemUser.isWebsiteVisibilityRestricted() ) {
			return Website.getSelectItemBeans(systemUser.getVisibleWebsites());
		} else {
			return Website.getSelectItemBeans(ApplicationUtil.getAplosContextListener().getWebsiteList());
		}
	}

	public int getSiteCount() {
		SystemUser systemUser = JSFUtil.getLoggedInUser();
		if ( systemUser.isWebsiteVisibilityRestricted() ) {
			return systemUser.getVisibleWebsites().size();
		} else {
			return ApplicationUtil.getAplosContextListener().getWebsiteList().size();
		}
	}

	public void switchSite(ValueChangeEvent event) {
		if( event.getPhaseId().equals( PhaseId.UPDATE_MODEL_VALUES ) ) {
			switchSite((Website)event.getNewValue());
		} else {
			event.setPhaseId( PhaseId.UPDATE_MODEL_VALUES );
			event.queue();
			return;
		}
	}

	public void switchSite(Website website) {
		website.addToScope( JsfScope.TAB_SESSION );
		website.markAsCurrentWebsite();
		TabPanel mainDtp = JSFUtil.getAplosRequestContext().getMainTabPanel();
		if (mainDtp == null) {
			JSFUtil.getAplosRequestContext().determineTabPanelState();
			mainDtp = JSFUtil.getAplosRequestContext().getMainTabPanel();
		}
		
		JSFUtil.redirect( mainDtp.getDefaultTabAction(), true );
	}

	public boolean isSiteHasFrontend() {
		if (getCurrentSite() != null) {
			return  ApplicationUtil.getAplosContextListener().getAplosModuleByName("cms") != null;
		}
		return false;
	}
	
}













