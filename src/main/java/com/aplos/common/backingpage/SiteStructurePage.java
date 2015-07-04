package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang.StringEscapeUtils;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.CommonBundleKey;
import com.aplos.common.enums.TabActionType;
import com.aplos.common.interfaces.DynamicMenuNode;
import com.aplos.common.module.AplosModule;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.SiteTabPanel;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
//@SessionScoped // for tree memory - new tabs redirect
public class SiteStructurePage extends BackingPage {

	private static final long serialVersionUID = 291771046873294539L;
	/* For saving movement in the tree */
	private String dragNode;
	private String dropNode;
	private String dragNodeType;
	private String dropNodeType;
	private String dropType;
	private Object currentState;
	private Long nodeId;
	private List<DynamicMenuNode> menuRoots = null;
	private List<Long> openIds;
	private static final String OPEN_IDS = "ssOpenIds";

	public SiteStructurePage() {
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		if (menuRoots == null) {
			createMenuRoots();
		}
		if( getOpenIds() == null ) {
			setOpenIds( (List<Long>) JSFUtil.getFromTabSession( OPEN_IDS ) );
			if( getOpenIds() == null ) {
				setOpenIds( new ArrayList<Long>() );
				JSFUtil.addToTabSession( OPEN_IDS, getOpenIds() );
			}
		}
		return true;
	}
	
	public void createMenuRoots() {
		menuRoots = new ArrayList<DynamicMenuNode>();

		SiteTabPanel siteTabPanel = (SiteTabPanel) JSFUtil.getServletContext().getAttribute( CommonUtil.getBinding( SiteTabPanel.class ) );

		ArrayList<UserLevel> superUserPseudoList = new ArrayList<UserLevel>();
		superUserPseudoList.add(CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel());
		BeanDao websiteDao = new BeanDao(Website.class);
		List<Website> websites = websiteDao.setIsReturningActiveBeans(true).getAll();
		for (int i=websites.size()-1; i >= 0; i--){
			Website website = websites.get(i);
			MenuTab webTab = new MenuTab(superUserPseudoList, website.getName(), "/" + website.getPackageName() + website.getDefaultMenuUrl());
			webTab.setWebsite(website);
			webTab.setTabPanel(siteTabPanel);
			menuRoots.add(webTab);
		}

		MenuTab orphanPanelNode = new MenuTab();
		orphanPanelNode.setDisplayName("Unused / Ophaned Panels");
		orphanPanelNode.setId(-1l);
		orphanPanelNode.setTabPanel(siteTabPanel);
		menuRoots.add(orphanPanelNode);
		MenuTab orphanTabNode = new MenuTab();
		orphanTabNode.setDisplayName("Unused / Ophaned Tabs");
		orphanTabNode.setId(-2l);
		orphanTabNode.setTabPanel(siteTabPanel);
		menuRoots.add(orphanTabNode);
		MenuTab destroyNode = new MenuTab();
		destroyNode.setDisplayName("Remove");
		destroyNode.setId(-3l);
		destroyNode.setTabPanel(siteTabPanel);
		menuRoots.add(destroyNode);
	}

	public String getMenuJson() {
		StringBuffer jsonBuff = new StringBuffer();
		if (menuRoots != null) {
			for( int i=0; i < menuRoots.size(); i++ ) {
				getDynamicMenuNodeJson( jsonBuff, menuRoots.get( i ) );
				jsonBuff.append( "," );
			}
		}
		return jsonBuff.toString();
	}

	public String getDisplayNameHtml( DynamicMenuNode dynamicMenuNode ) {
		String displayName;
		if (dynamicMenuNode == null) {
			return "<i>error - null</i>";
		} else if (dynamicMenuNode.getDisplayName() != null) {
			displayName = StringEscapeUtils.escapeHtml(dynamicMenuNode.getDisplayName());
		} else {
			displayName = "<i>No Display Name</i>";
		}
		StringBuffer strBuf = new StringBuffer( displayName );
		if( dynamicMenuNode instanceof MenuTab){
			MenuTab menuTab = (MenuTab)dynamicMenuNode;
			if (menuTab.getId() != null && menuTab.getId().equals(-3l)) {
				strBuf.append( "<i> (Drag items here to remove them)</i>" );
			} else {
				if (!dynamicMenuNode.isSiteRoot() && menuTab.determineTabAction()==null && !TabActionType.COMMAND_LINK.equals( menuTab.getTabActionType() ) ) {
					strBuf.append( "<i> [Unbound]</i>" );
				}
				if ( menuTab.getTabPanel() != null &&
						menuTab.getTabPanel().getFullTabList() != null &&
						menuTab.getTabPanel().getFullTabList().size() > 1 &&
						menuTab.getTabPanel().getDefaultTab() != null &&
						menuTab.getTabPanel().getDefaultTab().equals(dynamicMenuNode)) {
					strBuf.append( "<i> [Default]</i>" );
				}
			}
		}
		return strBuf.toString();
	}

	protected String getRootType(MenuTab menuTabNode) {
		if ( menuTabNode.getWebsite() != null ) {
				return "simple_root";
			} else {
				return "root";
			}
	}

	public void getDynamicMenuNodeJson( StringBuffer jsonBuff, DynamicMenuNode dynamicMenuNode ) {
		String type = "root";
		if (dynamicMenuNode.isSiteRoot()) {
			type = getRootType((MenuTab)dynamicMenuNode);
		} else {
			if( dynamicMenuNode instanceof TabPanel ) {
				if (((TabPanel)dynamicMenuNode).isLinkedToBean()) {
					type = "newbean_tabpanel";
				} else {
					type = "tabpanel";
				}
			} else if( dynamicMenuNode.getIsOutputLink() ) {
				type = "output";
			} else {
				if (((MenuTab)dynamicMenuNode).getTabPanel() != null &&
					((MenuTab)dynamicMenuNode).getTabPanel().isLinkedToBean() &&
					!((MenuTab)dynamicMenuNode).isShowWhenNew()) {
					type = "menutab_hidden";
				} else {
					type = "menutab";
				}
			}
		}

		jsonBuff.append( "{ " );
		jsonBuff.append( "\"attributes\" : { " );
		if (type.contains("root")) {
			if (((MenuTab)dynamicMenuNode).getWebsite() == null) {
				//orphan nodes
				jsonBuff.append( "\"id\" : \"" + dynamicMenuNode.getId() + "\", " );
				if (dynamicMenuNode.getId().equals(-1l)) {
					type = "orphan-panels";
				} else if (dynamicMenuNode.getId().equals(-2l)) {
					type = "orphan-tabs";
				} else {
					type = "destroy-node";
				}
			} else {
				//valid website
				jsonBuff.append( "\"id\" : \"" + ((MenuTab)dynamicMenuNode).getWebsite().getId() + "\", " );
			}

		} else {
			jsonBuff.append( "\"id\" : \"" + dynamicMenuNode.getId() + "\", " );
		}
		jsonBuff.append( "\"rel\" : \"" + type + "\"" );
		jsonBuff.append( " }, " );
		jsonBuff.append( "\"data\": { " );
		jsonBuff.append( "\"title\" : \"" + getDisplayNameHtml( dynamicMenuNode ) + "\", " );
		jsonBuff.append( "\"attributes\" : {" );
		//nothing...at the moment
		jsonBuff.append( "} " );
		jsonBuff.append( "}" );

		if (type.equals("orphan-panels")) {
			jsonBuff.append( ", \"state\" : \"open\"" );
			jsonBuff.append( ", \"children\" : [ " );
			boolean first = true;
			BeanDao orphanDao = new BeanDao(TabPanel.class);
			orphanDao.setWhereCriteria("bean.website=null");
			orphanDao.addWhereCriteria("bean.parentMenuTab=null");
			List<TabPanel> orphanTabPanels = orphanDao.setIsReturningActiveBeans(true).getAll();

			for ( TabPanel orphanTabPanel : orphanTabPanels ) {
				if (!first) { jsonBuff.append( ", " ); }
				if (first) { first = false; }
				getDynamicMenuNodeJson( jsonBuff, orphanTabPanel );
			}

			jsonBuff.append( " ] " );
		} else if (type.equals("orphan-tabs")) {
			jsonBuff.append( ", \"state\" : \"" );
			jsonBuff.append( "open" );
			jsonBuff.append( "\", \"children\" : [ " );
			boolean first = true;
			BeanDao orphanDao = new BeanDao(MenuTab.class);
			orphanDao.setWhereCriteria("bean.tabPanel=null");
			List<MenuTab> orphanMenuTabs = orphanDao.setIsReturningActiveBeans(true).getAll();
			for (MenuTab orphanMenuTab : orphanMenuTabs) {
				if (!first) { jsonBuff.append( ", " ); }
				if (first) { first = false; }
				getDynamicMenuNodeJson( jsonBuff, orphanMenuTab );
			}

			jsonBuff.append( " ] " );
		} else if (!dynamicMenuNode.getMenuNodeChildren().isEmpty()) {
			jsonBuff.append( ", \"state\" : \"" );
			if( (dynamicMenuNode.getId() != null &&	dynamicMenuNode.getId() == 1l) || openIds.contains(dynamicMenuNode.getId()) ) {
				jsonBuff.append( "open" );
			} else if (dynamicMenuNode.getId() == null &&  dynamicMenuNode instanceof MenuTab && openIds.contains(((MenuTab)dynamicMenuNode).getWebsite().getId()) ) {
				jsonBuff.append( "open" );
			} else {
				jsonBuff.append( "closed" );
			}
			jsonBuff.append( "\", \"children\" : [ " );

			boolean first = true;
			List<DynamicMenuNode> menuNodeChildren = dynamicMenuNode.getMenuNodeChildren();
			for ( int i = 0, n = menuNodeChildren.size(); i < n; i++ ) {
				DynamicMenuNode nodeObj = menuNodeChildren.get( i );
				if (!first) {
					jsonBuff.append( ", " );
				}
				if (first) {
					first = false;
				}

				getDynamicMenuNodeJson( jsonBuff, nodeObj );
			}

			jsonBuff.append( " ] " );
		}
		jsonBuff.append( "}" );
	}

	public void goToNewTab() {
		MenuTab newMenuTab = new MenuTab();
		Date newDate = new Date();
		newMenuTab.setDisplayName("New Tab Orphan - " + FormatUtil.formatDate(FormatUtil.getEngSlashSimpleDateFormat(), newDate) + " " + FormatUtil.formatTime(newDate));
		newMenuTab.getViewableByList().add(CommonConfiguration.getCommonConfiguration().getUserLevelUtil().getAdminUserLevel());
		newMenuTab.saveDetails();
		JSFUtil.redirect(SiteStructurePage.class);
	}

	public void goToNewPanel() {
		TabPanel newPanel = new TabPanel();
		Date newDate = new Date();
		newPanel.setName("New Panel Orphan - " + FormatUtil.formatDate(FormatUtil.getEngSlashSimpleDateFormat(), newDate) + " " + FormatUtil.formatTime(newDate));
		newPanel.saveDetails();
		JSFUtil.redirect(SiteStructurePage.class);
	}

	public void updateTree() {
//		System.out.println("Site Structure : Move " + dragNodeType + " " + dropType + " " + dropNodeType);
		if (dropNodeType.equals("top") || (dropNodeType.contains("root") && ("before".equals( dropType ) || "after".equals( dropType )))) {
			JSFUtil.addMessageForError("You cannot drop a node into root");
			//if just got an error, reload the tree so we haven't changed anything
			JSFUtil.redirect("cms", SiteStructurePage.class);
			return;
		}


		DynamicMenuNode dbDragNode;

		if (dragNodeType.contains("tabpanel")) {
			dbDragNode = (DynamicMenuNode)new BeanDao(TabPanel.class).get(Long.parseLong( dragNode ));
		} else {
			dbDragNode = (DynamicMenuNode)new BeanDao(MenuTab.class).get(Long.parseLong( dragNode ));
		}
		
		if( dbDragNode != null ) {
			dbDragNode = (DynamicMenuNode) dbDragNode.getSaveableBean();
		}
		//DynamicMenuNode preMoveDragNode = dbDragNode.getCopy();
		if (dropNodeType.equals("destroy-node") && "inside".equals(dropType)) {
			deleteMenuNode( dbDragNode );
			//deal with menu cache
			createMenuRoots();
			JSFUtil.redirect(SiteStructurePage.class); //reload tree so node is removed
			return;
		}

		DynamicMenuNode dbDropNode = null;
		if (dropNodeType.contains("root")) {
			//dropNode in this case holds the id of a website not a node
		} else {
			if (dropNodeType.contains("tabpanel")) {
				dbDropNode = (DynamicMenuNode)new BeanDao(TabPanel.class).get(Long.parseLong( dropNode ));
			} else {
				dbDropNode = (DynamicMenuNode)new BeanDao(MenuTab.class).get(Long.parseLong( dropNode ));
			}
		}
		if( dbDropNode != null ) {
			dbDropNode = (DynamicMenuNode) dbDropNode.getSaveableBean();
		}

		Website dragSite = null;
		Website dropSite = null;
		if (dbDragNode instanceof TabPanel) {
			dragSite = ((TabPanel)dbDragNode).getWebsite();
		} else {
			if ((TabPanel)(dbDragNode.getParent()) != null) {
				dragSite = ((TabPanel)(dbDragNode.getParent())).getWebsite();
			}
		}
		if( dragSite != null ) {
			dragSite = dragSite.getSaveableBean();
		}
		if (dbDropNode instanceof TabPanel) {
			dropSite = ((TabPanel)dbDropNode).getWebsite();
		} else if (dbDropNode != null ) {
			if ((TabPanel)(dbDropNode.getParent()) != null) {
				dropSite = ((TabPanel)(dbDropNode.getParent())).getWebsite();
			}
		}
		if( dropSite != null ) {
			dropSite = dropSite.getSaveableBean();
		}
		
		
		if (dragSite != null && !dragSite.equals(dropSite)) {
			if (!assessWebsiteSuitability(dbDragNode,dropSite)) {
				JSFUtil.addMessageForError(dbDragNode.getDisplayName() + " cannot be moved because some of its bindings or its children's bindings conflict with bindings already in use for " + dropSite.getDisplayName());
				JSFUtil.redirect(SiteStructurePage.class); //reload tree so the node is not moved
				return;
			}
		}
		
		DynamicMenuNode dbDropNodeParent = dbDropNode.getParent();
		if( dbDropNodeParent != null ) {
			dbDropNodeParent = (DynamicMenuNode) dbDropNodeParent.getSaveableBean();
		}
		
		DynamicMenuNode dbDragNodeParent = dbDragNode.getParent();
		if( dbDragNodeParent != null ) {
			dbDragNodeParent = (DynamicMenuNode) dbDragNodeParent.getSaveableBean();
		}

//		System.out.println( "Drop Nodes: " + dropNode.toString() + " " + cachedDropNode.toString() );
//		System.out.println( "Drag Nodes: " + dragNode.toString() + " " + cachedDragNode.toString() );
//		System.out.println( "Drop type: " + dropType );
//		System.out.println( "Drop node type: " + dropNodeType );
		if ("before".equals( dropType ) || "after".equals( dropType )) {
			if (dbDropNodeParent instanceof MenuTab) {
				JSFUtil.addMessageForError("Only one tab panel may be assigned to a given menu tab at any one time");
				//if just got an error, to reload  the tree so we havent changed anything
				JSFUtil.redirect(SiteStructurePage.class);
				return;
			} else {

				if ("before".equals( dropType )) {
					if (!dbDropNodeParent.equals(dbDragNodeParent)) {
						if (dbDragNodeParent != null) { //null when coming from an orphan
							dbDragNodeParent.removeChild(dbDragNode);
							dbDragNodeParent.saveDetails();
						}
						dbDropNodeParent.addChild( dbDropNodeParent.getMenuNodeChildren().indexOf( dbDropNode ), dbDragNode );
					} else if (dbDragNodeParent instanceof TabPanel){
						int dragIdx = dbDragNodeParent.getMenuNodeChildren().indexOf( dbDragNode );
						int dropIdx = dbDropNodeParent.getMenuNodeChildren().indexOf( dbDropNode );
						List<DynamicMenuNode> children = dbDropNodeParent.getMenuNodeChildren();
						if (dragIdx > dropIdx) {
							//drop -> drag-1 = +1
							for (int i=dragIdx-1; i >= dropIdx; i--) {
								children.set(i+1, children.get(i));
							}
							children.set(dropIdx, dbDragNode);
						} else {
							//drag+1 -> drop-1 = -1
							for (int i=dragIdx; i < dropIdx-1; i++) {
								children.set(i, children.get(i+1));
							}
							children.set(dropIdx-1, dbDragNode);
						}
					}
				} else if ("after".equals(dropType)) {
					if (dbDropNodeParent == null) {
						dbDragNode.setParent( null );
						dbDragNode.saveDetails();
					} else if (!dbDropNodeParent.equals(dbDragNodeParent)) {
						if (dbDragNodeParent != null) { //null when coming from an orphan
							dbDragNodeParent.removeChild(dbDragNode);
							dbDragNodeParent.saveDetails();
						}
						dbDragNode.setParent( null ); //this way because of orphan nodes
						//if (drop.getParent() != null) {
							dbDropNodeParent.addChild( dbDropNodeParent.getMenuNodeChildren().indexOf( dbDropNode )+1, dbDragNode );
							dbDragNode.saveDetails();

						//}
					} else if (dbDragNodeParent instanceof TabPanel){
						int dragIdx = dbDropNodeParent.getMenuNodeChildren().indexOf( dbDragNode );
						int dropIdx = dbDropNodeParent.getMenuNodeChildren().indexOf( dbDropNode );
						List<DynamicMenuNode> children = dbDropNodeParent.getMenuNodeChildren();
						if (dragIdx > dropIdx) {
							//drop+1 -> drag-1 = +1
							for (int i=dragIdx-1; i > dropIdx; i--) {
								children.set(i+1, children.get(i));
							}
						} else {
							//drag+1 -> drop = -1
							for (int i=dragIdx; i < dropIdx; i++) {
								children.set(i, children.get(i+1));
							}
						}
						children.set(dropIdx, dbDragNode);
						
//						((TabPanel)dbDragNodeParent).setFullTabListFromMenuNodes(children);
					}
				}

			}
		} else if ("inside".equals(dropType)) {
			//for a menu tab, any children at all means we cant have any more
			if (dbDropNode != null && dbDropNode instanceof MenuTab && !(dropNodeType.equals("orphan-tabs") || dropNodeType.equals("orphan-panels")) && dbDropNode.getMenuNodeChildren().size() > 0) {
				JSFUtil.addMessageForError("Only one tab panel may be assigned to a given menu tab at any one time");
				//if just got an error, to reload the tree so we havent changed anything
				JSFUtil.redirect(SiteStructurePage.class);
				return;
			} else if (dropNodeType.equals("orphan-tabs") || dropNodeType.equals("orphan-panels")) {
				dbDragNodeParent.removeChild(dbDragNode);
				if (dbDragNodeParent.getId() >= 0) {
					dbDragNodeParent.saveDetails();
				}
				dbDragNode.setParent(null);
			} else if (dbDropNode == null) {
				if (dbDragNodeParent != null) { //when coming from an orphan
					dbDragNodeParent.removeChild(dbDragNode);
					if (dbDragNodeParent.getId() >= 0) {
						dbDragNodeParent.saveDetails(); //do this to update position index etc
					}
					dbDragNode.setParent(null);
				}
			} else {
				if( dbDragNodeParent != null ) {
					dbDragNodeParent.removeChild(dbDragNode);
				}
				/*
				 * Make sure that the drag child is removed before adding it to the drop node.  As adding
				 * the child also updates the parent. 
				 */
				dbDropNode.addChild( null, dbDragNode );
			}
		}

		//orphan the node if we dragged it to an orphan location
		if (dbDragNode instanceof TabPanel) {
			if (dropNodeType.equals("orphan-panels")) {
				dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode,null);
			} else if (dropNodeType.contains("root")) {
				Website website = (Website) new BeanDao(Website.class).get(Long.parseLong(dropNode));
				dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode,website);
			} else {
				//reset website when returned to tree
				if ("before".equals( dropType ) || "after".equals( dropType )) {
					if (dbDropNodeParent instanceof TabPanel) {
						dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode, ((TabPanel)dbDropNodeParent).getWebsite());
					} else {
						dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode, ((TabPanel)dbDropNodeParent.getParent()).getWebsite());
					}
				} else {
					if (dbDropNode instanceof TabPanel) {
						dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode, ((TabPanel)dbDropNode).getWebsite());
					} else {
						dbDragNode = updateWebsiteReferences((TabPanel)dbDragNode, ((TabPanel)dbDropNodeParent).getWebsite());
					}
				}
			}
		} else if (dbDragNode instanceof MenuTab && dropNodeType.equals("orphan-tabs")) {
			//((MenuTab)drag).setWebsite(null); //website transient anyway and parent has been handled
		}

		if (dbDragNodeParent != null) {
			dbDragNodeParent.saveDetails(); //cascades, saving from here so we save position safely
		} else {
			dbDragNode.saveDetails();
		}

		if ( !dropNodeType.contains("root") && !dropNodeType.equals("orphan-panels") 
				&& !dropNodeType.equals("orphan-tabs") && !dropNodeType.equals("destroy-node") ) {
			if (dbDropNodeParent != null) {
				dbDropNodeParent.saveDetails(); 
			} 
			dbDropNode.saveDetails();
		}
		return;
//		ApplicationUtil.getAplosContextListener().updateMenuCache(drag,preMoveDragNode);
	}
	
	public void deleteMenuNode( DynamicMenuNode dynamicMenuNode ) {
		//remove our parent's reference to us
		dynamicMenuNode = (DynamicMenuNode) dynamicMenuNode.getSaveableBean();
		if (dynamicMenuNode.getParent() != null) {
			DynamicMenuNode parent = (DynamicMenuNode) dynamicMenuNode.getParent().getSaveableBean(); 
			parent.removeChild(dynamicMenuNode);
			if( parent instanceof TabPanel && ((TabPanel)parent).getDefaultTab().equals( dynamicMenuNode ) ) {
				((TabPanel)parent).setDefaultTab(null);
			}
			parent.saveDetails();
		}
		//remove our reference to our parent
		if (dynamicMenuNode instanceof MenuTab) {
			((MenuTab)dynamicMenuNode).setTabPanel(null);
			dynamicMenuNode.saveDetails(); //otherwise that change will be lost
		} else {
			((TabPanel)dynamicMenuNode).setParentMenuTab(null);
			dynamicMenuNode.saveDetails(); //otherwise that change will be lost
		}
		dynamicMenuNode.delete();
	}

	/** This method checks if it is safe to move the node into a new website
	 * @return false if any binding on then ode or its descendants conflict
	 * with the bindings already in use for the new website
	 * */
	@SuppressWarnings("unchecked")
	private boolean assessWebsiteSuitability(DynamicMenuNode node, Website targetWebsite) {
		if (targetWebsite == null) {
			return true; //for dragging to orphan nodes, remove node
		}
		StringBuffer usedClassIds = new StringBuffer("-1"); //the -1 saves checking for empty strings when we add a commma or use the output in an IN()
		addUsedTabActionClassIdsForNode(node, usedClassIds);
		String sql = "SELECT bean.id FROM " + AplosBean.getTableName(TabPanel.class) + " bean "; //for efficiency
		sql += "WHERE associatedBackingPage_id IS NOT NULL AND associatedBackingPage_id IN (" + usedClassIds.toString() + ")";
		List<Long> conflictIds = new ArrayList<Long>();
		addFirstResultToList( conflictIds, ApplicationUtil.getResults(sql) );
		if (conflictIds.size()==0) {
			sql = "SELECT bean.id FROM " + AplosBean.getTableName(MenuTab.class) + " bean ";
			sql += "WHERE tabActionClass_id IS NOT NULL AND tabActionClass_id IN (" + usedClassIds.toString() + ")";
			addFirstResultToList( conflictIds, ApplicationUtil.getResults(sql));
			if (conflictIds.size()==0) {
				sql = "SELECT bean.MenuTab_id FROM menutab_defaultpagebindings bean ";
				sql += "WHERE defaultPageBindings_id IN (" + usedClassIds.toString() + ")";
				addFirstResultToList( conflictIds, ApplicationUtil.getResults(sql) );
			}
		}
		return conflictIds.size()==0;
	}
	
	private void addFirstResultToList( List<Long> list, List<Object[]> results ) {
		for( int i = 0, n = results.size(); i < n; i++ ) {
			list.add( (Long) results.get( i )[ 0 ] );
		}
	}

	private void addUsedTabActionClassIdsForNode(DynamicMenuNode node, StringBuffer usedClassIds) {
		if (node instanceof TabPanel) {
//			if (((TabPanel)node).getAssociatedBackingPage() != null ) {
//				usedClassIds.append(",");
//				usedClassIds.append(((TabPanel)node).getAssociatedBackingPage().getId());
//			}
			for (MenuTab tab : ((TabPanel)node).getFullTabList()) {
				addUsedTabActionClassIdsForNode(tab, usedClassIds);
			}
		} else { //tab
			if (((MenuTab)node).getTabActionClass() != null) {
				usedClassIds.append(",");
				usedClassIds.append(((MenuTab)node).getTabActionClass().getId());
			}
			for (TabClass tabClass : ((MenuTab)node).getDefaultPageBindings()) {
				usedClassIds.append(",");
				usedClassIds.append(tabClass.getId());
			}
			if (((MenuTab)node).getSubTabPanel() != null) {
				addUsedTabActionClassIdsForNode(((MenuTab)node).getSubTabPanel(), usedClassIds);
			}
		}
	}

	private TabPanel updateWebsiteReferences(TabPanel panel, Website website) {
		panel.setWebsite(website);
		for (MenuTab childTab : panel.getFullTabList()) {
			if (childTab.getSubTabPanel() != null) {
				updateWebsiteReferences(childTab.getSubTabPanel(), website);
			}
		}
		return panel;
	}

	public void selectNode() {
		AplosBean loadedDrop;

		if (dropNodeType.contains("tabpanel")) {
			loadedDrop = (TabPanel)new BeanDao(TabPanel.class).get(Long.parseLong( dropNode ));
		} else if (dropNodeType.contains("root")) {
			loadedDrop = (Website)new BeanDao(Website.class).get(Long.parseLong( dropNode ));
		} else {
			loadedDrop = (MenuTab)new BeanDao(MenuTab.class).get(Long.parseLong( dropNode ));
		}
		loadedDrop.redirectToEditPage();
	}

	public void startWebsiteCreationWizard() {
		JSFUtil.redirect( CreateWebsiteWizardPage.class );
	}

	public String createSimpleWebsite() {
		AplosAbstractBean newBean = new BeanDao( Website.class ).getNew();
		newBean.addToScope();
		return "websiteEdit";
	}

	public String createCmsWebsite() {
		AplosModule aplosModule = ApplicationUtil.getAplosContextListener().getAplosModuleByName( "cms" );
		if( aplosModule != null ) {
			//this just creates a new CmsWebsite object and redirects us to the edit page
			return aplosModule.fireNewWebsiteAction();
		} else {
			return null;
		}
	}

	public void save() {
		for( int i = 0, n = menuRoots.size(); i < n; i++ ) {
			menuRoots.get( i ).saveDetails();
		}
		JSFUtil.addMessage(ApplicationUtil.getAplosContextListener().translateByKey( CommonBundleKey.SAVED_SUCCESSFULLY ));
	}

	public String getDragNode() {
		return dragNode;
	}

	public void setDragNode( String dragNode ) {
		this.dragNode = dragNode;
	}

	public String getDropNode() {
		return dropNode;
	}

	public void setDropNode( String dropNode ) {
		this.dropNode = dropNode;
	}

	public String getDragNodeType() {
		return dragNodeType;
	}

	public void setDragNodeType( String dragNodeType ) {
		this.dragNodeType = dragNodeType;
	}

	public String getDropNodeType() {
		return dropNodeType;
	}

	public void setDropNodeType( String dropNodeType ) {
		this.dropNodeType = dropNodeType;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setDropType(String dropType) {
		this.dropType = dropType;
	}

	public String getDropType() {
		return dropType;
	}

	public void setCurrentState(Object currentState) {
		this.currentState = currentState;
	}

	public Object getCurrentState() {
		return currentState;
	}

	public void openNode() {
		try {
			openIds.add( Long.parseLong(dropNode) );
		} catch (NumberFormatException nfe) {}
	}
	
	public void closeNode() {
		try {
			openIds.remove( Long.parseLong(dropNode) );
		} catch (NumberFormatException nfe) {}
	}

	public List<Long> getOpenIds() {
		return openIds;
	}

	public void setOpenIds(List<Long> openIds) {
		this.openIds = openIds;
	}

}
