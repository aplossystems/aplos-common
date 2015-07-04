package com.aplos.common.backingpage;

import java.io.Serializable;

import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabPanel;

public class BackingPageConfig implements Serializable {
	private static final long serialVersionUID = 9075485539522322911L;
	private TabPanel mainTabPanel;
	private TabPanel subTabPanel;
	private MenuTab selectedMainTab;
	private MenuTab selectedSubTab;

	public BackingPageConfig() {}

	public BackingPageConfig getCopy() {
		BackingPageConfig newBackingPageConfig = new BackingPageConfig();
		newBackingPageConfig.copy( this );
		return newBackingPageConfig;
	}

	public void copy( BackingPageConfig sourceBackingPageConfig ) {
		setMainTabPanel( sourceBackingPageConfig.mainTabPanel );
		setSubTabPanel( sourceBackingPageConfig.subTabPanel );
		setSelectedMainTab( sourceBackingPageConfig.getSelectedMainTab() );
		setSelectedSubTab( sourceBackingPageConfig.getSelectedSubTab() );
	}

	public void setMainTabPanel(TabPanel mainTabPanel) {
		this.mainTabPanel = mainTabPanel;
	}
	public TabPanel getMainTabPanel() {
		return mainTabPanel;
	}

	public void setSubTabPanel(TabPanel subTabPanel) {
		this.subTabPanel = subTabPanel;
	}
	public TabPanel getSubTabPanel() {
		return subTabPanel;
	}

	public void setSelectedMainTab(MenuTab selectedMainTab) {
		this.selectedMainTab = selectedMainTab;
	}

	public MenuTab getSelectedMainTab() {
		return selectedMainTab;
	}

	public void setSelectedSubTab(MenuTab selectedSubTab) {
		this.selectedSubTab = selectedSubTab;
	}

	public MenuTab getSelectedSubTab() {
		return selectedSubTab;
	}
}
