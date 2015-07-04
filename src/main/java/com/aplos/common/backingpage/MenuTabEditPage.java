package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DynamicBundleEntry;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.TabActionType;
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
@AssociatedBean(beanClass=MenuTab.class)
public class MenuTabEditPage extends EditPage {
	private static final long serialVersionUID = -9340172972505884L;

	private TabActionType tabActionType = null;
	private AplosBean actionBinding = null; //this could be a subtabpanel or a dynamictabclass
	private String beanDisplayName = "";
	private String tabWidth = "";
	private String beanBinding = null;
	private Boolean showWhenNew = true;
	private Boolean linkedToBean = false; //decides whether to render showwhennew field
	private Boolean rendersWhenNoUser = false;
	private Boolean hiddenFromSuperuser = false;
	private String target = "";
	private String linkValue = "";
	private String linkAction = "";
	private Website website = null;
	private TabPanel parentPanel = null;
	private boolean defaultTabForBackingPage=true;
	private boolean currentDefaultTabForBackingPage=false;

	private List<TabClass> assignedClasses = new ArrayList<TabClass>();
	private List<TabClass> selectedAssignedClasses = new ArrayList<TabClass>();
	private List<TabClass> selectedAvailableClasses = new ArrayList<TabClass>();

	private List<UserLevel> restrictedUserTypes = new ArrayList<UserLevel>();
	private List<UserLevel> allowedUserTypes = new ArrayList<UserLevel>();
	private List<UserLevel> selectedRestrictedUserTypes = new ArrayList<UserLevel>();
	private List<UserLevel> selectedAllowedUserTypes = new ArrayList<UserLevel>();

	//caching
	private List<TabClass> unassignedTabClasses = null;
	private List<TabClass> unassignedExtraBindingTabClasses = null;
	private SelectItem[] websiteCache = null;
	private SelectItem[] panelCache = null;
	private SelectItem[] actionCache = null;

	public MenuTabEditPage() {
		getBeanDao().setListPageClass( SiteStructurePage.class );
		BeanDao userDao = new BeanDao(UserLevel.class);
		userDao.setSelectCriteria("bean.id,bean.name");
		restrictedUserTypes = userDao.getAll();
		loadInitialTabState(); //setup state from existing tab data
		UserLevel superUserLevel = CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel();
		if (superUserLevel != null) {
			restrictedUserTypes.remove(superUserLevel);
			allowedUserTypes.remove(superUserLevel);
		}
	}

	public SelectItem[] getRestrictedUserTypeSelectItems() {
		return AplosBean.getSelectItemBeans(restrictedUserTypes);
	}

	public SelectItem[] getAllowedUserTypeSelectItems() {
		return AplosBean.getSelectItemBeans(allowedUserTypes);
	}

	public String addUserAccess() {
		if (selectedRestrictedUserTypes.size() > 0) {
			for (UserLevel level : selectedRestrictedUserTypes) {
				allowedUserTypes.add(level);
				restrictedUserTypes.remove(level);
			}
		} else {
			JSFUtil.addMessageForError("No user types are selected to be allowed");
		}
		return null;
	}

	public String removeUserAccess() {
		if (selectedAllowedUserTypes.size() > 0) {
			for (UserLevel level : selectedAllowedUserTypes) {
				allowedUserTypes.remove(level);
				restrictedUserTypes.add(level);
			}
		} else {
			JSFUtil.addMessageForError("No user types are selected to be restricted");
		}
		return null;
	}

	@Override
	public boolean saveBean() {
		MenuTab dtab = resolveAssociatedBean();
		boolean wasNew = dtab.isNew();

		if (dtab != null) {
			dtab.setDisplayName(beanDisplayName);
			dtab.setTabWidth(tabWidth);
			dtab.setShowWhenNew(showWhenNew);
			dtab.setIsRenderedWhenNoUser(rendersWhenNoUser);
			dtab.setIsHiddenFromSuperuser(hiddenFromSuperuser);

			if (tabActionType != null) {
				//we set the correct fields and unset the others as we should only ever have a single action
				dtab.setTabActionType(getTabActionType());
				if (tabActionType.equals(TabActionType.OUTPUT_LINK)) {
					dtab.setLinkValue(linkValue);
					dtab.setTarget(target);
					dtab.setDefaultTabForBackingPage(false);
					setCurrentDefaultTabForBackingPage(false);
				} else if (tabActionType.equals(TabActionType.COMMAND_LINK)) {
					dtab.setLinkAction(linkAction);
					dtab.setDefaultTabForBackingPage(false);
					setCurrentDefaultTabForBackingPage(false);
//				} else if (tabActionType.equals(TabActionType.SUBMENU) ) {
//					AqlBeanDao dao = new AqlBeanDao(TabPanel.class);
//					TabPanel loadedSubPanel = dao.get(((TabPanel)actionBinding).getId());
//					dtab.setSubTabPanel(loadedSubPanel);
//					if (dtab.isNew()) {
//						dtab.aqlSaveDetails();
//					}
//					loadedSubPanel.setParentMenuTab(dtab);
//					dtab.setDefaultTabForBackingPage(false);
//					setCurrentDefaultTabForBackingPage(false);
				} else if (actionBinding != null) {
					AplosBean saveableActionBinding = actionBinding.getSaveableBean();
					((TabClass)saveableActionBinding).setPaidForAndAccessible(true);
					saveableActionBinding.saveDetails();
					dtab.setTabActionClass((TabClass)actionBinding);
					dtab.setDefaultTabForBackingPage(defaultTabForBackingPage);
					setCurrentDefaultTabForBackingPage(defaultTabForBackingPage);
				} else {
					dtab.setTabActionClass( null );
					dtab.setDefaultTabForBackingPage( null );
				}
			}
			dtab.setViewableByList(allowedUserTypes);
			for (TabClass dtc : assignedClasses) {
				TabClass saveableDtc = dtc.getSaveableBean();
				saveableDtc.setPaidForAndAccessible(true);
				saveableDtc.saveDetails();
			}
			dtab.setDefaultPageBindings(assignedClasses);

			//make sure we have only one default when saving a new default tab with an action class
			if (actionBinding != null && actionBinding instanceof TabClass && defaultTabForBackingPage) {
				ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName(MenuTab.class) + " SET defaultTabForBackingPage=0 WHERE tabActionClass_id=" + ((TabClass)actionBinding).getId());
			}
			dtab.saveDetails();

			if( wasNew ) {
				ApplicationUtil.getMenuCacher().putMenuTab( dtab );
			} else {
				ApplicationUtil.getMenuCacher().replaceMenuTab( dtab );
			}
			if( ApplicationUtil.getAplosContextListener().getDynamicBundleEntry( dtab.getDisplayName() ) == null  ) {
				DynamicBundleEntry dynamicBundleEntry = new DynamicBundleEntry();
				dynamicBundleEntry.setEntryKey( dtab.getDisplayName() );
				dynamicBundleEntry.setEntryValue( dtab.getDisplayName() );
				dynamicBundleEntry.saveDetails();
			}
			
			ApplicationUtil.getAplosContextListener().refreshUserLevelPageAccess();
		}

		return true;
	}

	public boolean isMultiUseBackingPageClass() {
		if (actionBinding == null || tabActionType == null || !tabActionType.equals(TabActionType.OUTPUT_LINK)) {
			return false;
		}
		BeanDao dao = new BeanDao(MenuTab.class);
		dao.setSelectCriteria("COUNT(bean.id)");
		dao.setWhereCriteria("bean.tabActionClass.id=" + ((TabClass)actionBinding).getId());
		Long count = (Long) dao.setIsReturningActiveBeans(true).getAll().get( 0 );
		return count != null && count > 1;
	}

	//method to take the current tab settings
	private void loadInitialTabState() {
		MenuTab dtab = resolveAssociatedBean();
		if (dtab != null) {
			setBeanDisplayName(dtab.getDisplayNameRaw());
			setTabWidth(dtab.getTabWidth());
			setShowWhenNew(dtab.isShowWhenNew());
			if (dtab.getTabPanel() != null) {
				setLinkedToBean(dtab.getTabPanel().isLinkedToBean());
			}
			setTarget(dtab.getTarget());
			setLinkValue(dtab.getLinkValue());
			setParentPanel(dtab.getTabPanel());
			setTabActionType(dtab.getTabActionType());
			if (dtab.getWebsite() != null) {
				setWebsite(dtab.getWebsite());
			} else if (getParentPanel() != null) {
				setWebsite(getParentPanel().getWebsite());
			}
			assignedClasses = dtab.getDefaultPageBindings();
			allowedUserTypes = dtab.getViewableByList();
			for (UserLevel userLev : allowedUserTypes) {
				restrictedUserTypes.remove(userLev);
			}
			rendersWhenNoUser = dtab.getIsRenderedWhenNoUser();
			hiddenFromSuperuser = dtab.getIsHiddenFromSuperuser();
			website = dtab.determineWebsite();
			defaultTabForBackingPage = dtab.getDefaultTabForBackingPage();
			//once its in the db that this is the default we cant undefault it from this edit page, only from another tab
			setCurrentDefaultTabForBackingPage(defaultTabForBackingPage);
		}
		if (dtab != null && dtab.getSubTabPanel() != null) {
			setActionBinding(dtab.getSubTabPanel());
			if (dtab.getTabActionClass() != null) {
				setActionBinding(dtab.getTabActionClass()); //subtabs still have an action
			}
		} else if (dtab != null && dtab.getTabActionClass() != null) {
			setActionBinding(dtab.getTabActionClass());
		} else {
			setActionBinding(null);
		}
	}

	public boolean getHasHome() { //is the tab assigned to a website so we can correctly get the available panels?
		return (website != null && parentPanel != null);
	}

	public SelectItem[] getWebsiteSelectItemBeans() {
		if (websiteCache != null) {
			return websiteCache;
		}
		BeanDao websiteDao = new BeanDao(Website.class);
		websiteDao.setSelectCriteria("bean.id,bean.name");
		websiteCache = AplosBean.getSelectItemBeans(websiteDao.getAll());
		return websiteCache;
	}

	public SelectItem[] getParentPanelSelectItemBeans() {
		if (panelCache != null) {
			return panelCache;
		}
		BeanDao panelDao = new BeanDao(TabPanel.class);
		panelDao.setSelectCriteria("bean.id,bean.name");
		if (getWebsite() == null) {
			panelDao.addWhereCriteria("bean.website=null");
		} else {
			panelDao.addWhereCriteria("bean.website.id=" + getWebsite().getId());
		}
		panelCache = AplosBean.getSelectItemBeansWithNotSelected(panelDao.getAll(),"Not Assigned");
		return panelCache;
	}

	public SelectItem[] getAvailableActionBindingSelectItemBeans() {

		if (actionCache != null) {
			return actionCache;
		}

		MenuTab dtab = resolveAssociatedBean();
		if (unassignedTabClasses == null) {
			BeanDao classDao = new BeanDao(TabClass.class);
			classDao.setSelectCriteria("bean.id,bean.backingPageClass");
			classDao.setWhereCriteria("bean.isPaidForAndAccessible=true");
			//limit to one use of a class per website
			//classDao.addWhereCriteria("bean.id NOT IN(" + MenuTab.getAssignedTabActionClassIdString(website,dtab,null,new ArrayList<TabClass>()) + ")");
			//we can only see the 3 common modules or our implementation project
			classDao.addWhereCriteria("(bean.backingPageClass LIKE 'com.aplos.common%' OR bean.backingPageClass LIKE 'com.aplos.cms%' OR bean.backingPageClass LIKE 'com.aplos.ecommerce%' OR bean.backingPageClass LIKE 'com.aplos." + website.getPackageName() + "%')");
			unassignedTabClasses = classDao.getAll(); //get all classes that are available, consider the ones assigned to thsi tab avaialble
		}
		List<TabClass> avail = unassignedTabClasses;
		//this is a dropdown menu so we need to be able to see ourself / selected value, so dont remove it
		for (TabClass taken : assignedClasses) {
			if (!taken.equals(actionBinding)) {
				avail.remove(taken);
			}
		}
		actionCache = AplosBean.getSelectItemBeansWithNotSelected(avail, "Not Selected");

		return actionCache;
	}

	//this is for extra bindings, not for the action
	public SelectItem[] getAvailableClassSelectItems() {
		MenuTab dtab = resolveAssociatedBean();
		if (unassignedExtraBindingTabClasses == null) {
			BeanDao classDao = new BeanDao(TabClass.class);
			classDao.setSelectCriteria("bean.id,bean.backingPageClass");
			classDao.setWhereCriteria("bean.isPaidForAndAccessible=true");
			//TODO: limits us to using a class only once per website - we may still need this one as our enum key wont help us with these extra bindings
//			String idString = MenuTab.getAssignedTabActionClassIdString(website,dtab,null,new ArrayList<TabClass>());
//			if (idString != null && !idString.equals("")) {
//				classDao.addWhereCriteria("bean.id NOT IN(" + idString + ")");
//			}
			//we can only see the 3 common modules or our implementation project
			classDao.addWhereCriteria("(bean.backingPageClass LIKE 'com.aplos.common%' OR bean.backingPageClass LIKE 'com.aplos.cms%' OR bean.backingPageClass LIKE 'com.aplos.ecommerce%' OR bean.backingPageClass LIKE 'com.aplos." + website.getPackageName() + "%')");
			unassignedExtraBindingTabClasses = classDao.getAll(); //get all classes that are available, consider the ones assigned to thsi tab avaialble
		}
		List<TabClass> avail = unassignedExtraBindingTabClasses;
		if (actionBinding instanceof TabClass) {
			avail.remove(actionBinding);
		}
		for (TabClass taken : assignedClasses) {
			avail.remove(taken);
		}
		return AplosBean.getSelectItemBeans(avail);
	}

	public List<SelectItem> getTabWidthSelectItems() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		//myro:48px
		//networking ease: 121px
		items.add(new SelectItem(null,"Automatic"));
		for (int i=35; i <= 200; i=i+5) {
			items.add(new SelectItem(i,i +"px"));
		}
		return items;
	}
	
	public void defaultTabActionChanged(AjaxBehaviorEvent abe) {
		if (tabActionType.equals(TabActionType.BACKINGPAGE) && actionBinding != null) {
			List<Class<? extends BackingPage>> defaultBindings = null;
			Class<? extends BackingPage> backingPageClass = ((TabClass)actionBinding).getBackingPageClass();
			
			try {
				
				BackingPage backingPage = backingPageClass.newInstance();
				defaultBindings = backingPage.getTabDefaultPageBindings();
				if (defaultBindings == null) {
					defaultBindings = new ArrayList<Class<? extends BackingPage>>();
				}
				
				if (backingPage instanceof ListPage) {
					
					Class<? extends BackingPage> editPage = ApplicationUtil.getAplosContextListener().getEditPageClasses().get(backingPageClass.getSimpleName().replace("ListPage", ""));
					if (editPage != null && !defaultBindings.contains(editPage)) {
						defaultBindings.add(editPage);
					}
				}
				
				for (Class<? extends BackingPage> defaultBinding : defaultBindings) {

					TabClass tabClass = TabClass.get(defaultBinding).getSaveableBean();
					tabClass.setPaidForAndAccessible(true);
					tabClass.saveDetails();
					if (!assignedClasses.contains(tabClass)) {
						assignedClasses.add(tabClass);
					}
					
				}
				
				//can happen if we select the list then switch to edit etc.
				if (assignedClasses.contains(actionBinding)) {
					assignedClasses.remove(actionBinding);
				}
				
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public boolean getIsBackingPageLink() {
		return tabActionType.equals(TabActionType.OUTPUT_LINK);
	}

	public boolean getIsOutputLink() {
		return tabActionType != null && tabActionType.equals(TabActionType.OUTPUT_LINK);
	}

	public boolean getIsBackingPageAction() {
		return tabActionType != null && tabActionType.equals(TabActionType.BACKINGPAGE);
	}

	public boolean getIsCommandLink() {
		return tabActionType != null && tabActionType.equals(TabActionType.COMMAND_LINK);
	}

	public List<SelectItem> getTabActionTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( TabActionType.class );
	}

	public boolean getHasSubmenu() {
		MenuTab dtab = resolveAssociatedBean();
		return dtab.getSubTabPanel() != null;
	}

	public void setTabActionType(TabActionType tabActionType) {
		actionCache = null;
		this.tabActionType = tabActionType;
	}

	public TabActionType getTabActionType() {
		return tabActionType;
	}

	public void setActionBinding(AplosBean actionBinding) {
		this.actionBinding = actionBinding;
	}

	public AplosBean getActionBinding() {
		return actionBinding;
	}

	public void setParentPanel(TabPanel parentPanel) {
		this.parentPanel = parentPanel;
	}

	public TabPanel getParentPanel() {
		return parentPanel;
	}

	public void setWebsite(Website website) {
		if ((website != null && !website.equals(this.website)) || (website == null && this.website != null)) {
			panelCache = null; // cache becomes useless when changing sites
		}
		this.website = website;
	}

	public Website getWebsite() {
		SelectItem[] sites = getWebsiteSelectItemBeans();
		for (SelectItem site : sites) { //try and match up, it wont do it itself as they are optimised / not equal
			if (website != null && ((Website)site.getValue()).getId() != null && ((Website)site.getValue()).getId().equals(website.getId())) {
				return (Website)site.getValue();
			}
		}
		return website;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getTarget() {
		return target;
	}

	public void setShowWhenNew(Boolean showWhenNew) {
		this.showWhenNew = showWhenNew;
	}

	public Boolean getShowWhenNew() {
		return showWhenNew;
	}

	public void setTabWidth(String tabWidth) {
		this.tabWidth = tabWidth;
	}

	public String getTabWidth() {
		return tabWidth;
	}

	public void setBeanDisplayName(String beanDisplayName) {
		this.beanDisplayName = beanDisplayName;
	}

	public String getBeanDisplayName() {
		return beanDisplayName;
	}

	public void setSelectedAvailableClasses(List<TabClass> selectedAvailableClasses) {
		this.selectedAvailableClasses = selectedAvailableClasses;
	}

	public List<TabClass> getSelectedAvailableClasses() {
		return selectedAvailableClasses;
	}

	public void setSelectedAssignedClasses(List<TabClass> selectedAssignedClasses) {
		this.selectedAssignedClasses = selectedAssignedClasses;
	}

	public List<TabClass> getSelectedAssignedClasses() {
		return selectedAssignedClasses;
	}

	public SelectItem[] getAssignedClassSelectItems() {
		return AplosBean.getSelectItemBeans(assignedClasses);
	}

	public void setAssignedClasses(List<TabClass> assignedClasses) {
		this.assignedClasses = assignedClasses;
	}

	public List<TabClass> getAssignedClasses() {
		return assignedClasses;
	}

	public String addAssignments() {
		if (selectedAvailableClasses.size() > 0) {
			for (TabClass backingClass : selectedAvailableClasses) {
				assignedClasses.add(backingClass);
			}
		} else {
			JSFUtil.addMessageForError("No classes are selected to be assigned");
		}
		return null;
	}

	public String removeAssignments() {
		if (selectedAssignedClasses.size() > 0) {
			for (TabClass backingClass : selectedAssignedClasses) {
				assignedClasses.remove(backingClass);
			}
		} else {
			JSFUtil.addMessageForError("No classes are selected to be unassigned");
		}
		return null;
	}

	@Override
	public String getBeanClassDisplayName() {
		return beanBinding;
	}

	public void setLinkedToBean(Boolean linkedToBean) {
		this.linkedToBean = linkedToBean;
	}

	public Boolean getLinkedToBean() {
		return linkedToBean;
	}

	public void setSelectedRestrictedUserTypes(
			List<UserLevel> selectedRestrictedUserTypes) {
		this.selectedRestrictedUserTypes = selectedRestrictedUserTypes;
	}

	public List<UserLevel> getSelectedRestrictedUserTypes() {
		return selectedRestrictedUserTypes;
	}

	public void setSelectedAllowedUserTypes(List<UserLevel> selectedAllowedUserTypes) {
		this.selectedAllowedUserTypes = selectedAllowedUserTypes;
	}

	public List<UserLevel> getSelectedAllowedUserTypes() {
		return selectedAllowedUserTypes;
	}

	public void setRendersWhenNoUser(Boolean rendersWhenNoUser) {
		this.rendersWhenNoUser = rendersWhenNoUser;
	}

	public Boolean getRendersWhenNoUser() {
		return rendersWhenNoUser;
	}

	public void setDefaultTabForBackingPage(boolean defaultTabForBackingPage) {
		this.defaultTabForBackingPage = defaultTabForBackingPage;
	}

	public boolean isDefaultTabForBackingPage() {
		return defaultTabForBackingPage;
	}

	public void setCurrentDefaultTabForBackingPage(
			boolean currentDefaultTabForBackingPage) {
		this.currentDefaultTabForBackingPage = currentDefaultTabForBackingPage;
	}

	public boolean isCurrentDefaultTabForBackingPage() {
		return currentDefaultTabForBackingPage;
	}

	public Boolean getHiddenFromSuperuser() {
		return hiddenFromSuperuser;
	}

	public void setHiddenFromSuperuser(Boolean hiddenFromSuperuser) {
		this.hiddenFromSuperuser = hiddenFromSuperuser;
	}

	public String getLinkValue() {
		return linkValue;
	}

	public void setLinkValue(String linkValue) {
		this.linkValue = linkValue;
	}

	public String getLinkAction() {
		return linkAction;
	}

	public void setLinkAction(String linkAction) {
		this.linkAction = linkAction;
	}

}
