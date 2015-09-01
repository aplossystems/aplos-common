package com.aplos.common.backingpage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Website;
import com.aplos.common.interfaces.DynamicMenuNode;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
@AssociatedBean(beanClass=TabPanel.class)
public class TabPanelEditPage extends EditPage {

	private static final long serialVersionUID = 8969229993942283906L;
	private String beanDisplayName = "";
	private String verticalWidth = "";
	private String verticalTabHeight = "";
	private Boolean isVertical = false;
	private boolean linkedToBean = false;
	private Website website = null;
	private MenuTab parentMenuTab = null;
	private MenuTab defaultTab = null;

	public TabPanelEditPage() {
		getBeanDao().setListPageClass( SiteStructurePage.class );
		updatePanelData(); //get existing data, so we dont update the tree and cause lasting damage with each change
	}

	private void updatePanelData() {
		TabPanel dtab = resolveAssociatedBean();
		if (dtab != null) {
			setBeanDisplayName(dtab.getDisplayName());
			setIsVertical(dtab.isVertical());
			setVerticalWidth(dtab.getVerticalWidth());
			setVerticalTabHeight(dtab.getVerticalTabHeight());
			setWebsite(dtab.getWebsite());
			setLinkedToBean(dtab.isLinkedToBean());
			setParentMenuTab(dtab.getParentMenuTab());
			setDefaultTab(dtab.getDefaultTab());
		}
	}

	@Override
	public void okBtnAction() {
		if( saveBtnAction() ) {
			cancelBtnAction();
		}
	}

	@Override
	public void applyBtnAction() {
		saveBtnAction();
	}

	public boolean saveBtnAction() {

		//the commented code is here because you coudl originally change location of node in tree from this view

		//we may have to change not only the object we are working on, but many of its children
		//too when we save - for instance we may have changed website, in which case each
		//object needs to know which website it is now nested under

		TabPanel dyanmicTabPanel = resolveAssociatedBean();
//		DynamicMenuNode unchangedNode = dtab.getCopy();

		//Unique Name Check
		BeanDao nameDao = new BeanDao(TabPanel.class);
		nameDao.setWhereCriteria("bean.name='" + beanDisplayName + "'");

		nameDao.addWhereCriteria("bean.id != " + dyanmicTabPanel.getId());
		nameDao.setSelectCriteria("bean.id");
		List<BigInteger> existing = nameDao.getResultFields();
		if (existing.size() > 0) {
			JSFUtil.addMessageForError("The name you have chosen is already in use, please select another");
			return false;
		} else {

				//Update Website bindings and save
				dyanmicTabPanel.setDefaultTab(defaultTab);
				dyanmicTabPanel.setName(beanDisplayName);
				dyanmicTabPanel.setParentMenuTab(parentMenuTab);
				dyanmicTabPanel.setVertical(isVertical);
				dyanmicTabPanel.setLinkedToBean(isLinkedToBean());
				dyanmicTabPanel.setVerticalTabHeight(verticalTabHeight);
				dyanmicTabPanel.setVerticalWidth(verticalWidth);

				//save is already set to cascade
				dyanmicTabPanel.saveDetails();
//				dyanmicTabPanel.hibernateInitialise( true );
//				dyanmicTabPanel.hibernateInitialiseChildren(false);
				if( getParentMenuTab() != null ) {
					ApplicationUtil.getMenuCacher().putMenuTab( getParentMenuTab() );
				}
				if( dyanmicTabPanel.isMainPanel() ) {
					ApplicationUtil.getMenuCacher().getWebsitePanelMap().put( dyanmicTabPanel.getWebsite(), dyanmicTabPanel );
				}
				return true;
		}
	}

	public SelectItem[] getParentTabSelectItemBeans() {
		if (getWebsite() != null) {
			//get tabs belonging to selected website (has it set or its parent has it set)
			//which dont belong to any descendant of ours
			TabPanel dtab = resolveAssociatedBean();
			StringBuffer descendentBuffer = new StringBuffer();
			boolean first = true;
			collectDescendentIds(descendentBuffer, dtab, first);

			BeanDao parentDao = new BeanDao(MenuTab.class);
			parentDao.addQueryTable( "tp", "bean.tabPanel" );
			//get orphan tab panels or panels for this website only
			parentDao.setWhereCriteria("(tp.website = null OR tp.website.id=" + getWebsite().getId() + ")");
			if (!descendentBuffer.toString().equals("")) {
				parentDao.addWhereCriteria("bean.id NOT IN (" + descendentBuffer.toString() + ")");
			}
			//this stops us selecting an occupied tab
			//parentDao.addWhereCriteria("(bean.subTabPanel=null OR bean.subTabPanel.id=" + dtab.getId() + ")");

			List<MenuTab> available = parentDao.setIsReturningActiveBeans(true).getAll(); //= AplosAbstractBean.getSelectItemBeans(parentDao.setIsReturningActiveBeans(true).getAll());
			if (available.size() < 1) {
				return new SelectItem[] { new SelectItem(null,"None - This will be the Main Panel for this website") };
			} else {
				SelectItem[] availableSelectItems = new SelectItem[available.size()];
				for (int i=0; i < available.size(); i++) {
					MenuTab tab = available.get(i);
					String displayName = getDisplayPath(tab.getDisplayName(), tab);
					availableSelectItems[i] = new SelectItem(tab,displayName);
				}
				return availableSelectItems;
			}
		} else {
			return new SelectItem[0];
		}
	}

	private String getDisplayPath(String displayName, DynamicMenuNode node) {
		if (node.getParent() == null) {
			return displayName;
		} else {
			return getDisplayPath(node.getParent().getDisplayName() + " > " + displayName, node.getParent());
		}
	}

	private void collectDescendentIds(StringBuffer buff, TabPanel panel, boolean first) {
		TabPanel loadedPanel;
		if (panel == null || panel.getId() == null || panel.getFullTabList() == null) {
			return;
		} else {
			loadedPanel = (TabPanel) new BeanDao(TabPanel.class).get(panel.getId());
//			loadedPanel.hibernateInitialise( true );
		}
		for (MenuTab childTab : loadedPanel.getFullTabList()) {
			if (!first) {
				buff.append(",");
			}
			buff.append(childTab.getId());
			first = false;
			if (childTab.getSubTabPanel() != null) {
				collectDescendentIds(buff, childTab.getSubTabPanel(), first);
			}
		}
	}

	public SelectItem[] getAvailableBackingPageSelectItemBeans() {
		TabPanel panel = resolveAssociatedBean();
		BeanDao classDao = new BeanDao(TabClass.class);
		classDao.setWhereCriteria("bean.isPaidForAndAccessible=true");
		//stops us reusing tab classes
		//classDao.addWhereCriteria("bean.id NOT IN(" + MenuTab.getAssignedTabActionClassIdString(website,null,panel, new ArrayList<TabClass>()) + ")");
		//we can only see the 3 common modules or our implementation project
		classDao.addWhereCriteria("(bean.backingPageClass LIKE 'com.aplos.common%' OR bean.backingPageClass LIKE 'com.aplos.cms%' OR bean.backingPageClass LIKE 'com.aplos.ecommerce%' OR bean.backingPageClass LIKE 'com.aplos." + website.getPackageName() + "%')");
		SelectItem[] items = AplosBean.getSelectItemBeans(classDao.setIsReturningActiveBeans(true).getAll());
		if (items.length > 0) {
			return items;
		} else {
			items = new SelectItem[1];
			items[0] = new SelectItem(null, "There are no available, unbound backing pages");
		}
		return items;
	}

	public SelectItem[] getFullTabListSelectItemBeans() {
		TabPanel dtab = resolveAssociatedBean();
		if (dtab != null && dtab.getFullTabList().size() > 0) {
			return AplosAbstractBean.getSelectItemBeans(new ArrayList<MenuTab>(dtab.getFullTabList()));
		} else {
			return new SelectItem[] { new SelectItem(null,"No Tabs Available") };
		}
	}

	public List<SelectItem> getVerticalWidthSelectItems() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		//myro:48px
		//networking ease: 121px
		items.add(new SelectItem(null,"Automatic"));
		for (int i=35; i <= 250; i=i+5) {
			items.add(new SelectItem(i,i +"px"));
		}
		return items;
	}

	public List<SelectItem> getVerticalHeightSelectItems() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		items.add(new SelectItem(null,"Automatic"));
		for (int i=25; i <= 100; i=i+5) {
			items.add(new SelectItem(i,i +"px"));
		}
		return items;
	}

	public SelectItem[] getWebsiteSelectItemBeans() {
		BeanDao websiteDao = new BeanDao(Website.class);
		return AplosBean.getSelectItemBeans(websiteDao.setIsReturningActiveBeans(true).getAll());
	}

	public void setBeanDisplayName(String beanDisplayName) {
		this.beanDisplayName = beanDisplayName;
	}

	public String getBeanDisplayName() {
		return beanDisplayName;
	}

	public void setVerticalWidth(String verticalWidth) {
		this.verticalWidth = verticalWidth;
	}

	public String getVerticalWidth() {
		return verticalWidth;
	}

	public void setVerticalTabHeight(String verticalTabHeight) {
		this.verticalTabHeight = verticalTabHeight;
	}

	public String getVerticalTabHeight() {
		return verticalTabHeight;
	}

	public void setIsVertical(Boolean isVertical) {
		this.isVertical = isVertical;
	}

	public Boolean getIsVertical() {
		return isVertical;
	}

	public void setWebsite(Website website) {
		this.website = website;
	}

	public Website getWebsite() {
		return website;
	}

	public void setParentMenuTab(MenuTab parentMenuTab) {
		this.parentMenuTab = parentMenuTab;
	}

	public MenuTab getParentMenuTab() {
		return parentMenuTab;
	}

	public void setDefaultTab(MenuTab defaultTab) {
		this.defaultTab = defaultTab;
	}

	public MenuTab getDefaultTab() {
		return defaultTab;
	}

	public void setLinkedToBean(boolean linkedToBean) {
		this.linkedToBean = linkedToBean;
	}

	public boolean isLinkedToBean() {
		return linkedToBean;
	}

}
