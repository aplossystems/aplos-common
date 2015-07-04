package com.aplos.common.beans.lookups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ManagedBean;

import com.aplos.common.BackingPageUrl;
import com.aplos.common.MenuCacher;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.Website;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;

@Entity
@ManagedBean
@Cache
public class UserLevel extends LookupBean {
	private static final long serialVersionUID = 5423698822362997025L;
	private int clearanceLevel;
	@ManyToOne
	private TabPanel mainTabPanel;

	public UserLevel() {
	}

	public boolean hasClearance(int clearanceThreshold) {
		return getClearanceLevelDoesntExceedMaximum(clearanceThreshold);
	}

	public boolean getClearanceExceedsMinimum(int clearanceThreshold) {
		return this.clearanceLevel >= clearanceThreshold;
	}

	public boolean getClearanceLevelDoesntExceedMaximum(int clearanceThreshold) {
		return this.clearanceLevel <= clearanceThreshold;
	}
	
	public boolean checkAccess( Class<? extends BackingPage> backingPageClass ) {
		return checkAccess( new BackingPageUrl( backingPageClass ).toString() );
	}

	public boolean checkAccess( String url ) {
		List<String> accessPages = ApplicationUtil.getAplosContextListener().getUserLevelPageAccess().get( this.getId() );
		if ( accessPages == null ) {
			return true;
		}
		for( int i = 0, n = accessPages.size(); i < n; i++ ) {
			if ( url.matches( accessPages.get(  i  ) ) ) {
				return true;
			}
		}
		return false;
	}

	public void updateAccessPages( AplosContextListener aplosContextListener ) {
		MenuCacher menuCacher = AplosContextListener.getAplosContextListener().getMenuCacher();
		//this handles backend/cms pages using dynamic tabs access lists
		List<String> accessPages;
		if (this.equals(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel())) {
			//we dont bother wasting the effort to get the url's if we are a superuser
			accessPages=null;
		} else {
			accessPages = new ArrayList<String>();
			//loop through the available tab panels. get bindings for each action and defaultPageBinding
			HashMap<Website, TabPanel> websitePanelMap = ApplicationUtil.getMenuCacher().getWebsitePanelMap();
			if (websitePanelMap != null) {
				for ( TabPanel tempTabPanel : websitePanelMap.values()) {
					addAccessUrlsFromTabPanel( (TabPanel) new BeanDao( TabPanel.class ).get( tempTabPanel.getId() ),accessPages);
				}
			}
		}

		ApplicationUtil.getAplosContextListener().getUserLevelPageAccess().put( this.getId(), accessPages );
	}

	private void addAccessUrlsFromTabPanel(TabPanel panel, List<String> accessPages) {
		for (MenuTab tab : panel.getFullTabList()) {
			if (tab.getViewableByList().contains(this)) {
				if (!tab.getIsOutputLink()) {
					//new bean tab panels do have an action, but still need to be explored by this method
					if (tab.getSubTabPanel() != null && ( tab.getTabAction() == null || tab.getSubTabPanel().isLinkedToBean() ) ) {
						if( tab.getSubTabPanel().getAssociatedBackingPage() != null ) {
							accessPages.add(new BackingPageUrl(tab.determineWebsite(), tab.getSubTabPanel().getAssociatedBackingPageClass(), true ).getPath());
						}
						//newbean tabs have a list page which we need access to
						if ( tab.getTabAction() != null && tab.getSubTabPanel().isLinkedToBean() ) {
							accessPages.add(tab.getTabAction().getPath());
						}
					} else if (tab.getTabAction() != null) {
						accessPages.add(tab.getTabAction().getPath());
					}
				}
				List<TabClass> otherBindings = tab.getDefaultPageBindings();
				if (otherBindings != null) {
					Website site = tab.determineWebsite();
					for (TabClass bindingClass : otherBindings) {
						accessPages.add(new BackingPageUrl(site, bindingClass.getBackingPageClass(), true).getPath());
					}
				}
			}
			// Even if the tab isn't visible may want to allow access from other routes than the menu system
			if (tab.getSubTabPanel() != null && ( tab.getTabAction() == null || tab.getSubTabPanel().isLinkedToBean() ) ) {
				addAccessUrlsFromTabPanel(tab.getSubTabPanel(), accessPages);
			}
		}
	}

	public void setClearanceLevel(int clearanceLevel) {
		this.clearanceLevel = clearanceLevel;
	}

	public int getClearanceLevel() {
		return clearanceLevel;
	}

	public TabPanel getMainTabPanel() {
		return mainTabPanel;
	}

	public void setMainTabPanel(TabPanel mainTabPanel) {
		this.mainTabPanel = mainTabPanel;
	}

}



