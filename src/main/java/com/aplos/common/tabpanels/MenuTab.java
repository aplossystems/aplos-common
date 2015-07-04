package com.aplos.common.tabpanels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.EnumType;
import com.aplos.common.annotations.persistence.Enumerated;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.backingpage.PaidFeaturePage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.TabActionType;
import com.aplos.common.interfaces.DynamicMenuNode;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.CascadeMemory;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Cache
public class MenuTab extends AplosBean implements Serializable, DynamicMenuNode {

	private static final long serialVersionUID = 314097686813293020L;

	@Enumerated(EnumType.STRING)
	private TabActionType tabActionType = TabActionType.BACKINGPAGE;
	
	@Transient //these three attributes are only for site tabs (generated dynamically)
	private Website website;
	@Transient
	private String packageName;

	private String displayName;
	@Transient
	private String effectiveTabAction; //don't take from tabActionClass each time - can be overridden (to replace newBeanTabPanel)
	@Transient
	private Boolean hasEffectiveEl; //relates to above field, says whether we are using an EL expression not a redirect
	@ManyToOne
	private TabClass tabActionClass;
	private String tabWidth;
	@Transient
	private boolean isSelected;
	private boolean showWhenNew = true;
	private Boolean defaultTabForBackingPage=false;

	//private String beanBinding; //filling in the gap left by newBeanTabPanel - get this automatically from backing page
	private String linkValue;
	private String linkAction;
	private String target;

	@OneToOne(mappedBy = "parentMenuTab")
	private TabPanel subTabPanel;

	@ManyToOne
	@JoinColumn(name="tabPanel_id")
	private TabPanel tabPanel; //parent - (mapped by this)
	@ManyToMany(fetch=FetchType.LAZY) //these are the userlevels this tab renders for
	@Cache
	private List<UserLevel> viewableByList = new ArrayList<UserLevel>();
	//this decides whether to render a tab if there's no currentUser but is ignored when there is a user
	private boolean isRenderedWhenNoUser = false;
	private boolean isHiddenFromSuperuser = false;

	@ManyToMany(fetch=FetchType.LAZY) //these are pages without their own tab that we tie to this tab
	@Cache
	private List<TabClass> defaultPageBindings = new ArrayList<TabClass>();

	public MenuTab() {}

	public MenuTab( List<UserLevel> viewableByList, String displayName, TabPanel subTabPanel ) {
		this.displayName = displayName;
		setSubTabPanel( subTabPanel );
		subTabPanel.setParentMenuTab( this );
		this.setViewableByList(viewableByList);
	}

	public MenuTab( List<UserLevel> viewableByList, String displayName, TabClass tabAction ) {
		this.displayName = displayName;
		this.setTabActionClass( tabAction );
		this.setViewableByList(viewableByList);
	}

	/** this should only be used for website tabs **/
	public MenuTab( List<UserLevel> viewableByList, String displayName, String tabAction ) {
		this.displayName = displayName;
		this.setEffectiveTabAction( tabAction );
		this.setViewableByList(viewableByList);
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialiseList(defaultPageBindings, fullInitialisation);
//		HibernateUtil.initialiseList(getViewableByList(), fullInitialisation);
//		HibernateUtil.initialise( getTabPanel(), fullInitialisation );
//	}
//
//	@Override
//	public void hibernateInitialiseChildren( boolean isRecursive ) {
//		if (getSubTabPanel() != null) {
//			getSubTabPanel().hibernateInitialise( true );
//			if( isRecursive ) {
//				getSubTabPanel().hibernateInitialiseChildren( true );
//			} else {
//				getSubTabPanel().hibernateInitialiseChildren( false );
//			}
//		}
//		if( getTabPanel() != null ) {
//			getTabPanel().hibernateInitialiseChildren(false);
//		}
//	}
	
	public String getInternationalisedText() {
		if (CommonConfiguration.getCommonConfiguration().getIsInternationalizedApplication()) {
			//internationalised - the tab panel 'names' should actually be resource bundle keys
			//start with a _ for safety so we can mix/match... and not break anything legacy
			if (getDisplayName().startsWith("_")) {
				return CommonUtil.translate(getDisplayName());
			} else {
				return getDisplayName();
			}
		} else {
			return getDisplayName();
		}
	}

	public void addDefaultPageBinding(TabClass tabClass) {
		if (!defaultPageBindings.contains(tabClass)) {
			defaultPageBindings.add(tabClass);
		}
	}

	public void setDisplayName( String displayName ) {
		this.displayName = displayName;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public String getDisplayNameRaw() {
		return displayName;
	}

	public void setTabWidth( String tabWidth ) {
		this.tabWidth = tabWidth;
	}

	public String getTabWidth() {
		return tabWidth;
	}

	public void setTabActionClass( TabClass tabActionClass ) {
		this.tabActionClass = tabActionClass;
	}

	public TabClass getTabActionClass() {
		return tabActionClass;
	}

	@Override
	public Website determineWebsite() {
		if (website != null) {
			return website;
		} else if (tabPanel == null) {
			return null;
		}
		return tabPanel.getWebsite();
	}

	/**
	 * @see determineTabAction. getTabAction is public only for use by PageAccessListener
	 * @return can be null
	 */
	public AplosUrl getTabAction() {
		if (effectiveTabAction != null) {
			return new AplosUrl( effectiveTabAction );
		}
		if (TabActionType.OUTPUT_LINK.equals( getTabActionType() ) ) {
			return new AplosUrl( getLinkValue() );
		}
		if (tabActionClass == null) {
			return null;
		} else if (!tabActionClass.isPaidForAndAccessible() && !JSFUtil.getLoggedInUser().getUserLevel().equals(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel())) {
			return new BackingPageUrl(determineWebsite(), PaidFeaturePage.class, true);
		}
		Website website = determineWebsite();
		if (website == null) { //should only be for site tab panel, superuser tabs, might need a better way to determine these
			return new BackingPageUrl(packageName, tabActionClass.getBackingPageClass(), true); //, true);
		} else {
			return new BackingPageUrl(website, tabActionClass.getBackingPageClass(), true); //, true);
		}
	}

	public AplosUrl determineTabAction() {
		if ( effectiveTabAction == null && getLinkValue() == null && (tabActionClass == null || tabActionClass.getBackingPageClass()==null) && getSubTabPanel() != null ) {
			return getSubTabPanel().getDefaultTabAction();
		} else {
			return getTabAction();
		}
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
		ApplicationUtil.getMenuCacher().putMenuTab(this);
	}
	
	

	public void setSelected( boolean isSelected ) {
		this.isSelected = isSelected;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setShowWhenNew(boolean showWhenNew) {
		this.showWhenNew = showWhenNew;
	}

	public boolean isShowWhenNew() {
		return showWhenNew;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getTarget() {
		return target;
	}

	public void setSubTabPanel(TabPanel subTabPanel) {
		this.subTabPanel = subTabPanel;
	}

	public TabPanel getSubTabPanel() {
		return subTabPanel;
	}

	public void setTabPanel(TabPanel tabPanel) {
		this.tabPanel = tabPanel;
	}

	public TabPanel getTabPanel() {
		return tabPanel;
	}

	public void setViewableByList(List<UserLevel> viewableByList) {
		this.viewableByList = viewableByList;
	}

	public List<UserLevel> getViewableByList() {
		return viewableByList;
	}

	public void setWebsite(Website website) {
		this.website = website;
	}

	public Website getWebsite() {
		return website;
	}

	protected void setEffectiveTabAction(String effectiveTabAction) {
		this.effectiveTabAction = effectiveTabAction;
	}

	public void setDefaultPageBindings(List<TabClass> defaultPageBindings) {
		this.defaultPageBindings = defaultPageBindings;
	}

	public List<TabClass> getDefaultPageBindings() {
		return defaultPageBindings;
	}

	public void setLinkValue(String linkValue) {
		this.linkValue = linkValue;
	}

	public String getLinkValue() {
		return linkValue;
	}

	@Override
	public boolean getIsOutputLink() {
		return TabActionType.OUTPUT_LINK.equals( getTabActionType() );
	}

	public boolean isOutputLink() {
		return getIsOutputLink();
	}

	@Override
	public DynamicMenuNode getParent() {
		return getTabPanel();
	}

	@Override
	public List<DynamicMenuNode> getMenuNodeChildren() {
		List<DynamicMenuNode> children = new ArrayList<DynamicMenuNode>();
		if (isSiteRoot()) {
			if (website != null) {
				//pretend the maintabpanel for this website is a child
				TabPanel panel = ApplicationUtil.getMenuCacher().getMainTabPanel(website);
				if (panel != null) {
					children.add(panel);
				}
			}
		} else {
			if (getSubTabPanel() != null) {
				children.add(getSubTabPanel());
			}
		}
		return children;
	}

	@Override /** add child here works as replace, position is ignored **/
	public void addChild(Integer position, DynamicMenuNode newChild) {
		setSubTabPanel((TabPanel)newChild);
		newChild.setParent(this);
	}
	
	@Override
	public void replaceChildWithSaveableBean(DynamicMenuNode dynamicMenuNode) {
		setSubTabPanel((TabPanel)dynamicMenuNode);
	}

	@Override
	public void removeChild(DynamicMenuNode child) {
		if (((TabPanel)child).equals(getSubTabPanel())) {
			setSubTabPanel(null);
		}
	}

	@Override
	public boolean isSiteRoot() {
		return tabPanel instanceof SiteTabPanel;
	}

	@Override
	public void setParent(DynamicMenuNode newParent) {
		setTabPanel((TabPanel) newParent);
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setIsRenderedWhenNoUser(Boolean isRenderedWhenNoUser) {
		this.isRenderedWhenNoUser = isRenderedWhenNoUser;
	}

	public Boolean getIsRenderedWhenNoUser() {
		return isRenderedWhenNoUser;
	}

	@Override
	public DynamicMenuNode getCopy() {
		MenuTab node = new MenuTab();
		node.setActive(this.isActive());
		node.setDefaultPageBindings(defaultPageBindings);
		node.setDisplayName(displayName);
		node.setEffectiveTabAction(effectiveTabAction);
		node.setIsRenderedWhenNoUser(isRenderedWhenNoUser);
		node.setIsHiddenFromSuperuser(isHiddenFromSuperuser);
		node.setLinkValue(linkValue);
		node.setPackageName(packageName);
		node.setShowWhenNew(showWhenNew);
		node.setSubTabPanel(getSubTabPanel());
		node.setTabActionClass(tabActionClass);
		node.setTabPanel(getSubTabPanel());
		node.setTabWidth(tabWidth);
		node.setTarget(target);
		node.setViewableByList(viewableByList);
		node.setWebsite(website);
		return node;
	}

	public void setHasEffectiveEl(Boolean hasEffectiveEl) {
		this.hasEffectiveEl = hasEffectiveEl;
	}

	public Boolean getHasEffectiveEl() {
		if (hasEffectiveEl==null){
			hasEffectiveEl=false;
		}
		return hasEffectiveEl;
	}

	public String getEffectiveTabAction() {
		return effectiveTabAction;
	}

	public void setDefaultTabForBackingPage(Boolean defaultTabForBackingPage) {
		this.defaultTabForBackingPage = defaultTabForBackingPage;
	}

	public Boolean getDefaultTabForBackingPage() {
		if (defaultTabForBackingPage==null) {
			defaultTabForBackingPage=false;
		}
		return defaultTabForBackingPage;
	}

	public Boolean getIsHiddenFromSuperuser() {
		return isHiddenFromSuperuser;
	}

	public void setIsHiddenFromSuperuser(Boolean isHiddenFromSuperuser) {
		this.isHiddenFromSuperuser = isHiddenFromSuperuser;
	}

	public TabActionType getTabActionType() {
		return tabActionType;
	}

	public void setTabActionType(TabActionType tabActionType) {
		this.tabActionType = tabActionType;
	}

	public String getLinkAction() {
		return linkAction;
	}

	public void setLinkAction(String linkAction) {
		this.linkAction = linkAction;
	}


}

