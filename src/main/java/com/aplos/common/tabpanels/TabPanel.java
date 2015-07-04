package com.aplos.common.tabpanels;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.IndexColumn;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.BackingPageConfig;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.DynamicMenuNode;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.listeners.PageBindingPhaseListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Cache
public class TabPanel extends AplosBean implements Serializable, DynamicMenuNode {

	private static final long serialVersionUID = -1601598879003354411L;
	private String name;
	private boolean isVertical = false;
	private String verticalWidth = "145px";
	private String verticalTabHeight = "60px";
	@ManyToOne(fetch=FetchType.LAZY)
	private Website website;
	@ManyToOne (fetch=FetchType.LAZY)
	private MenuTab defaultTab;
	private boolean linkedToBean = false;
	@OneToOne(fetch=FetchType.LAZY)
	private MenuTab parentMenuTab;

//	@OneToMany(mappedBy = "tabPanel")

	@OneToMany(fetch=FetchType.LAZY)
	@IndexColumn(name="position")
	@JoinColumn(name="tabPanel_id")
	@Cache
	private List<MenuTab> fullTabList = new ArrayList<MenuTab>();

	public TabPanel() { /* for hibernate */ }

	public TabPanel( Website website, String name ) {
		this.setWebsite(website);
		this.setName(name);
	}

	public MenuTab getDefaultTab() {
		if (defaultTab == null && getFullTabList().size() > 0) {
			return getFullTabList().get(0);
		}
		return defaultTab;
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialise( getWebsite(), fullInitialisation );
//		HibernateUtil.initialise( getDefaultTab(), fullInitialisation );
//		HibernateUtil.initialise( getParentMenuTab(), fullInitialisation );
//		HibernateUtil.initialiseList(fullTabList, fullInitialisation);
//	}

//	@Override
//	public void hibernateInitialiseChildren( boolean isRecursive ) {
////		HibernateUtil.initialiseList(fullTabList, true);
//		if( isRecursive ) {
//			for( MenuTab menuTab : fullTabList ) {
//				menuTab.hibernateInitialiseChildren( true );
//			}
//		}
//	}

	public void setSelectedTab( MenuTab menuTab ) {
		List<MenuTab> selectedTabList = getFullTabList();
		for( int i = 0, n = selectedTabList.size(); i < n; i++ ) {
			selectedTabList.get( i ).setSelected(false); 
			if ( selectedTabList.get( i ).equals(menuTab) ) {
				selectedTabList.get( i ).setSelected(true);
			}
		}
	}

	public MenuTab getSelectedTab() {
		List<MenuTab> selectedTabList = getFullTabList();
		for( int i = 0, n = selectedTabList.size(); i < n; i++ ) {
			if( selectedTabList.get( i ).isSelected() ) {
				return selectedTabList.get( i );
			}
		}
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addMenuTab( MenuTab menuTab ) {
		fullTabList.add( menuTab );
	}

//	private String getPageUrl( Website website, Class<? extends BackingPage> backingPageClass ) {
//		return getPageUrl( new BackingPageUrl(website, backingPageClass) );
//	}

	public AplosUrl getDefaultTabAction() {
		AplosUrl tabAction = null;
		if(isLinkedToBean()) {
			if( getAssociatedBackingPageClass() != null ) {
				return new BackingPageUrl(website, getAssociatedBackingPageClass(), true);
			} else {
				return null;
			}
		}
		MenuTab defaultTab = getDefaultTab();
		if (defaultTab != null && getProcessedTabList().contains( defaultTab )) {
			List<MenuTab> tabList = JSFUtil.getAplosRequestContext().getProcessedMenuTabs(this);
			if( tabList.contains(defaultTab) ) {
				tabAction = defaultTab.determineTabAction();
			} else if (tabList.size() > 0) {
				tabAction = tabList.get(0).determineTabAction();
			}
		} else if (getFullTabList().size() > 0) {
			MenuTab menuTab = getFullTabList().get(0);
			tabAction = menuTab.determineTabAction();
		}
		if( tabAction != null && tabAction.getPath().endsWith( "Edit" ) && showListPageForDefaultEditTab() ) {
			tabAction.setPath( tabAction.getPath().substring( 0, tabAction.getPath().lastIndexOf( "Edit" ) ) + "List" );
		}
		return tabAction;
	}

	public boolean showListPageForDefaultEditTab() {
		return true;
	}

	public void setParentMenuTab(MenuTab parentMenuTab) {
		this.parentMenuTab = parentMenuTab;
	}

	public MenuTab getParentMenuTab() {
		return parentMenuTab;
	}

	public void bindDefaultPages( String url, BackingPageConfig backingPageConfig, HashMap<String,BackingPageConfig> backingPageMap ) {
		if( getWebsite() != null && url != null && !url.startsWith( "/" + getWebsite().getPackageName() + "/" )  ) {
			url = "/" + getWebsite().getPackageName() + url;
		}
		if( url != null && url.endsWith( "List.jsf" ) ) {
			backingPageMap.put( url, backingPageConfig.getCopy() );
			backingPageMap.put( url.replace( "List.jsf", "" ) + "Edit.jsf", backingPageConfig.getCopy() );
		} else if( url != null && url.endsWith( "Edit.jsf" ) ) {
			backingPageMap.put( url, backingPageConfig.getCopy() );
		} else if (url != null) {
			backingPageMap.put( url, backingPageConfig.getCopy() );
		}
	}

	public void setDefaultTab(MenuTab defaultTab) {
		this.defaultTab = defaultTab;
	}

	public void bindMenuTabs( BackingPageConfig backingPageConfig, HashMap<String,BackingPageConfig> backingPageMap ) {

		if (!isMainPanel()) {
			backingPageConfig.setSubTabPanel( this );
		}

		//set the selected tab for each child tab's backing page config to itself
		for ( MenuTab menuTab : getFullTabList() ) {

			BackingPageConfig tabsBackingPageConfig = backingPageConfig.getCopy();
			menuTab.setTabPanel( this );

			if (isMainPanel()) {
				tabsBackingPageConfig.setSelectedMainTab( menuTab );
			} else {
				tabsBackingPageConfig.setSelectedSubTab( menuTab );
			}

			if (menuTab.getTabPanel() != null &&
				menuTab.getTabPanel().isLinkedToBean()) { //if we are a newbean tab panel
				tabsBackingPageConfig.setSubTabPanel(menuTab.getTabPanel());
			}
			bindDefaultPages( menuTab.determineTabAction().getPath(), tabsBackingPageConfig, backingPageMap );
			for (TabClass bindingClass : menuTab.getDefaultPageBindings()) {
				bindDefaultPages( new BackingPageUrl(bindingClass.getBackingPageClass()).toString(), tabsBackingPageConfig.getCopy(), backingPageMap );
			}

			if ( menuTab.getSubTabPanel() != null ) {
				menuTab.getSubTabPanel().bindMenuTabs( tabsBackingPageConfig, backingPageMap);
			}

		}
	}
	
	public List<MenuTab> getProcessedTabList() {
		List<MenuTab> unprocessedTabList = getFullTabList();
		List<MenuTab> tabList = new ArrayList<MenuTab>();
		boolean beanIsNew=false;
		boolean haveCheckedBeanIsNew=false;
		boolean isSuperUser=false;
		UserLevel currentUserLevel = null;
		if (JSFUtil.getLoggedInUser() != null) {
			currentUserLevel = JSFUtil.getLoggedInUser().determineUserLevel();
			isSuperUser=currentUserLevel.equals(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel());
		}
		for (int i=0; i < unprocessedTabList.size(); i++) {
			MenuTab tab = unprocessedTabList.get( i );
			if (!haveCheckedBeanIsNew && isLinkedToBean()) {
				if (!tab.isShowWhenNew()) { //if we show either way then no point in loading this bean
					haveCheckedBeanIsNew = true;
					beanIsNew=true; //in case we have no bean at all, we can't access pages reliant on it
					@SuppressWarnings("unchecked")
					AplosBean bean;
					BeanDao beanDao = getAssociatedBackingPage().getBeanDao();
					if (beanDao != null) {
						bean = JSFUtil.getBeanFromScope( beanDao.getBeanClass() );
					} else {
						bean = null;
					}
					if (bean != null) {
						beanIsNew=bean.getIsNew();
					}
				}
			}
			boolean isAddingTab = false;
			boolean isShowingToSuperUser = isSuperUser && !tab.getIsHiddenFromSuperuser();
			boolean isShowingToNoUserUserLevel = currentUserLevel == null && tab.getIsRenderedWhenNoUser();
			boolean isShowingToUserLevel = tab.getViewableByList().contains(currentUserLevel);
			
			if( isShowingToSuperUser || isShowingToNoUserUserLevel || isShowingToUserLevel ) {
				isAddingTab = true;
			} else {
				if(JSFUtil.getLoggedInUser() != null) {
					for( UserLevel additionalUserLevel : JSFUtil.getLoggedInUser().getAdditionalUserLevels() ) {
						if( tab.getViewableByList().contains(additionalUserLevel) ) {
							isAddingTab = true;
							break;
						}
					}
				}
			}
			if( isAddingTab ) {
				if (!beanIsNew || tab.isShowWhenNew()) {
					//can render
					tabList.add(tab);
				}
			}
		}
		return tabList;
	}
	
	public BackingPage getAssociatedBackingPage() {
		Class<? extends BackingPage> backingPageClass = getAssociatedBackingPageClass();
		if( backingPageClass != null ) {
			return PageBindingPhaseListener.resolveBackingPage(backingPageClass);
		} else {
			return null;
		}
	}
	
	public Class<? extends BackingPage> getAssociatedBackingPageClass() {
		if( getParent() != null && ((MenuTab)getParent()).getTabActionClass() != null ) {
			return ((MenuTab)getParent()).getTabActionClass().getBackingPageClass();
		} else {
			return null;
		}
	}

	public void setWebsite(Website website) {
		this.website = website;
	}

	public Website getWebsite() {
		return website;
	}

	public void setFullTabListFromMenuNodes(List<DynamicMenuNode> fullTabList) {
		this.fullTabList = new ArrayList<MenuTab>();
		for (int i=0; i < fullTabList.size(); i++) {
			this.fullTabList.add((MenuTab)fullTabList.get(i));
		}
	}

	public void setFullTabList(List<MenuTab> fullTabList) {
		this.fullTabList = fullTabList;
	}

	public List<MenuTab> getFullTabList() {
		return fullTabList;
	}

	public boolean isMainPanel() {
		if( parentMenuTab == null ) {
			return true;
		}
		return false;
	}

	public void setVertical(boolean isVertical) {
		this.isVertical = isVertical;
	}

	public boolean isVertical() {
		return isVertical;
	}

	@Override
	public DynamicMenuNode getParent() {
		return getParentMenuTab();
	}

	@Override
	/** This method casts children to be DynamicMenuNode's in a new list
	 * the result being that add or remove do not affect this objects actual children
	 * please use addChild(int position, node) and removeChild(node) instead
	 */
	public List getMenuNodeChildren() {
		return getFullTabList();
	}

	@Override
	public void addChild(Integer position, DynamicMenuNode newChild) {
		//check for end of list
		if (fullTabList == null) {
			fullTabList = new ArrayList<MenuTab>();
		}
		if (newChild != null) {
			if (position == null || (position != null && (position-1) >= fullTabList.size())) {
				fullTabList.add( (MenuTab)newChild );
			} else {
				fullTabList.add( position, (MenuTab)newChild );
			}
			newChild.setParent(this);
		}
	}
	
	@Override
	public void replaceChildWithSaveableBean(DynamicMenuNode dynamicMenuNode) {
		if (fullTabList == null) {
			fullTabList = new ArrayList<MenuTab>();
		}
		
		int listIndex = fullTabList.indexOf( dynamicMenuNode.getReadOnlyBean() ); 
		if( listIndex > -1 ) {
			fullTabList.set( listIndex, (MenuTab)dynamicMenuNode );
		}
	}

	@Override
	public void removeChild(DynamicMenuNode child) {
		fullTabList.remove(child);
	}

	@Override
	public boolean isSiteRoot() {
		return false;
	}

	@Override
	public boolean getIsOutputLink() {
		return false;
	}

	@Override
	public String getDisplayName() {
		return name; //FormatUtil.breakCamelCase(name);
	}

	@Override
	/**
	 * @alias for setParentMenuTab, for DynamicMenuNodeInterface
	 */
	public void setParent(DynamicMenuNode newParent) {
		parentMenuTab = (MenuTab) newParent;
	}

	public void setVerticalTabHeight(String verticalTabHeight) {
		this.verticalTabHeight = verticalTabHeight;
	}

	public String getVerticalTabHeight() {
		return verticalTabHeight;
	}

	public void setVerticalWidth(String verticalWidth) {
		this.verticalWidth = verticalWidth;
	}

	public String getVerticalWidth() {
		return verticalWidth;
	}

	public void setLinkedToBean(boolean linkedToBean) {
		this.linkedToBean = linkedToBean;
	}

	public boolean isLinkedToBean() {
		return linkedToBean;
	}

	private static List<UserLevel> getViewableByList(AplosContextListener contextListener, Website site, TabClass tabClass, Map<UserLevel,List<String>> userUrlMap) {
		if (userUrlMap == null) {
			userUrlMap = getUserUrlMap(contextListener);
		}
		List<UserLevel> viewableByList = new ArrayList<UserLevel>();
		if (tabClass != null) {
			String url = new BackingPageUrl(site, tabClass.getBackingPageClass(), true).toString();
			for (Map.Entry<UserLevel, List<String>> entry : userUrlMap.entrySet()) {
				if (entry.getValue() == null) {
					//the null means we have full access
					viewableByList.add(entry.getKey());
				} else {
					for (String pageUrlRegex : entry.getValue()) {
						if (url.matches(pageUrlRegex)) {
							viewableByList.add(entry.getKey());
						}
					}
				}
			}
		}
		return viewableByList;
	}

	//returns a map containing a list of url strings mapped to a user id
	private static Map<UserLevel,List<String>> getUserUrlMap(AplosContextListener contextListener) {
		Map<UserLevel,List<String>> userUrlMap = new HashMap<UserLevel,List<String>>();
		BeanDao userDao = new BeanDao(UserLevel.class);
		userDao.setSelectCriteria("name,id");
		List<UserLevel> allUserLevels = userDao.getAll(); //we MUST account for inactive (null active) users too
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			URL accessFileUrl = JSFUtil.checkFileLocations("userAccess.xml", "resources/access/", true );
			if( accessFileUrl != null ) {
				Document doc = docBuilder.parse ( accessFileUrl.openStream() );
				// normalize text representation
				doc.getDocumentElement().normalize();
				NodeList listOfUserLevelNodes = doc.getElementsByTagName("userLevel");
				for (UserLevel userLevel : allUserLevels) {
					List<String> allowedUrls = new ArrayList<String>();
					for( int i = 0, n = listOfUserLevelNodes.getLength(); i < n; i++ ) {
						Long userLvId = Long.valueOf(listOfUserLevelNodes.item(i).getAttributes().getNamedItem("userLevelId").getNodeValue());
						if (userLevel.getId().equals(userLvId)) {
							Element originalLevelElem = (Element) listOfUserLevelNodes.item( i );
							Element userLevelElem = originalLevelElem;
							if( userLevelElem.getAttribute( "fullAccess" ).equals( "true" ) ) {
								allowedUrls = null;
								//userUrlMap.put(userLevel, null); //null will represent full access
								continue;
							} else {
								/* Note:
								 * the inheritable attribute is optional and specifies whether
								 * all/any levels inheriting this level will acknowledge this rule
								 * In contrast the denyAccess tag tells one sub-level not to allow the rule
								 * it would inherit without globally disabling inheritance of the rule
								 * (although its children would then in turn inherit the deny rule)
								 */
								while( userLevelElem != null ) {
									NodeList allowAccessElements = userLevelElem.getElementsByTagName( "allowAccess" );
									for( int j = 0, p = allowAccessElements.getLength(); j < p; j++ ) {
										//only worry about the inheritable attribute if we are inheriting
										if (!userLevelElem.equals(originalLevelElem) && allowAccessElements.item( j ).getAttributes().getNamedItem( "inheritable" ) != null) {
											if (!allowAccessElements.item( j ).getAttributes().getNamedItem( "inheritable" ).getNodeValue().equals("false")) {
												allowedUrls.add( allowAccessElements.item( j ).getAttributes().getNamedItem( "page" ).getNodeValue() );
											}
										} else {
										//otherwise we are at this users root
											allowedUrls.add( allowAccessElements.item( j ).getAttributes().getNamedItem( "page" ).getNodeValue() );
										}
									}
									//inherits says we take the same rules as another user level
									if( userLevelElem.getAttribute( "inherits" ) != null && userLevelElem.getAttribute( "inherits" ) != "" ) {
									//loop around as this new user type and get our extended rules
										String inheritsStr = userLevelElem.getAttribute( "inherits" );
										userLevelElem = null;
										for( int j = 0, p = listOfUserLevelNodes.getLength(); j < p; j++ ) {
											if( listOfUserLevelNodes.item( j ).getAttributes().getNamedItem( "userLevelId" ).getNodeValue().equals( inheritsStr  ) ) {
												userLevelElem = (Element) listOfUserLevelNodes.item( j );
												break;
											}
										}
									} else {
									//otherwise break our loop
										userLevelElem = null;
									}
								}
							}
						}
					}
					userUrlMap.put(userLevel, allowedUrls);
				}
			}

			//need to account for global access file too
			accessFileUrl = JSFUtil.checkFileLocations("globalAccess.xml", "resources/access/", true);
			if( accessFileUrl != null ) {
				Document doc = docBuilder.parse ( accessFileUrl.openStream() );
				// normalize text representation
				doc.getDocumentElement().normalize();
				List<String> globalPageUrls = new ArrayList<String>();
				NodeList allowAccessElements = doc.getElementsByTagName( "allowAccess" );
				for( int j = 0, p = allowAccessElements.getLength(); j < p; j++ ) {
					globalPageUrls.add( allowAccessElements.item( j ).getAttributes().getNamedItem( "page" ).getNodeValue() );
				}
				userUrlMap.put(null, globalPageUrls);
				for (UserLevel userLevel : allUserLevels) {
					List<String> userLevelUrlList = userUrlMap.get(userLevel);
					//if its null we want to keep it null as it represents global access
					if (userLevelUrlList != null) {
						userLevelUrlList.addAll(globalPageUrls);
					}
				}
			}
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace ();
		} catch (Throwable t) {
			t.printStackTrace ();
		}

		return userUrlMap;
	}

	@Override
	public DynamicMenuNode getCopy() {
		TabPanel node = new TabPanel();
		node.setActive(this.isActive());
		node.setDefaultTab(defaultTab);
		node.setFullTabList(fullTabList);
		node.setLinkedToBean(linkedToBean);
		node.setName(name);
		node.setParentMenuTab(parentMenuTab);
		node.setVertical(isVertical);
		node.setVerticalTabHeight(verticalTabHeight);
		node.setVerticalWidth(verticalWidth);
		node.setWebsite(website);
		return node;
	}

	@Override
	public Website determineWebsite() {
		return website;
	}



}
