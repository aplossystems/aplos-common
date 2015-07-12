package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabClass;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
public class ClassAccessPage extends BackingPage {

	private static final long serialVersionUID = 5159757601183238019L;
	private List<TabClass> enabledClasses;
	private List<TabClass> availableClasses = new ArrayList<TabClass>();
	private List<TabClass> selectedEnabledClasses = new ArrayList<TabClass>();
	private List<TabClass> selectedAvailableClasses = new ArrayList<TabClass>();
	private boolean isShowingConfirmationScreen = false;
	private StringBuffer attachedClassesText=null;;

	public ClassAccessPage() {

	}
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();

		BeanDao classDao = new BeanDao(TabClass.class);

		if( enabledClasses == null ) {
			classDao.setWhereCriteria("bean.isPaidForAndAccessible=true");
			classDao.addWhereCriteria("bean.backingPageClass!=null");
			enabledClasses = classDao.setIsReturningActiveBeans(true).getAll();
		
			classDao.setWhereCriteria("bean.isPaidForAndAccessible!=true");
			classDao.addWhereCriteria("bean.backingPageClass!=null");
			availableClasses = classDao.setIsReturningActiveBeans(true).getAll();
			for( int i = availableClasses.size() - 1; i >= 0; i-- ) {
				if( availableClasses.get( i ).getBackingPageClass().getAnnotation( GlobalAccess.class ) != null ) {
					availableClasses.remove( i );
				}
			}
		}
		
		return continueLoad;
	}

	public String getAttachedClassesText() {
		if (attachedClassesText != null) {
			return attachedClassesText.toString();
		}
		return "";
	}

	public String enableClasses() {
		int enabledCount=0;
		if (selectedAvailableClasses.size() > 0) {
			for (TabClass backingClass : selectedAvailableClasses) {
				TabClass saveableBackingPageClass = backingClass.getSaveableBean();
				saveableBackingPageClass.setPaidForAndAccessible(true);
				availableClasses.remove(backingClass);
				enabledClasses.add(backingClass);
				saveableBackingPageClass.saveDetails();
				enabledCount++;
			}
			JSFUtil.addMessage(enabledCount + " classes enabled");
		} else {
			JSFUtil.addMessageForError("No classes are selected to be enabled");
		}
		return null;
	}

	public String disableClasses() {
		int disabledCount=0;
		attachedClassesText = new StringBuffer();
		if (selectedEnabledClasses.size() > 0) {
			for (int i=selectedEnabledClasses.size()-1; i >= 0; i--) {
				TabClass backingClass = selectedEnabledClasses.get(i);
				if (canDisableBackingPageClass(backingClass)) {
					backingClass.setPaidForAndAccessible(false);
					enabledClasses.remove(backingClass);
					availableClasses.add(backingClass);
					backingClass.saveDetails();
					disabledCount++;
					selectedEnabledClasses.remove(backingClass);
				}
			}
			JSFUtil.addMessageForError(disabledCount + " classes disabled.");
			if (attachedClassesText.length() > 0) {
				showConfirmationScreen();
			}
		} else {
			JSFUtil.addMessageForError("No classes are selected to be disabled");
		}
		return null;
	}

	public void showConfirmationScreen() {
		setShowingConfirmationScreen(true);
	}

	public String hideConfirmationScreen() {
		setShowingConfirmationScreen(false);
		return null;
	}

	public String removeBoundClasses() {
		int disabledCount=0;
		attachedClassesText = new StringBuffer();
		if (selectedEnabledClasses.size() > 0) {
			for (int i=selectedEnabledClasses.size()-1; i >= 0; i--) {
				TabClass backingClass = selectedEnabledClasses.get(i);
				removeBackingPageBindings(backingClass);
				backingClass.setPaidForAndAccessible(false);
				enabledClasses.remove(backingClass);
				availableClasses.add(backingClass);
				backingClass.saveDetails();
				disabledCount++;
			}
			JSFUtil.addMessageForError(disabledCount + " classes disabled.");
		} else {
			JSFUtil.addMessageForError("No classes are selected to be disabled");
		}
		hideConfirmationScreen();
		return null;
	}

	private void removeBackingPageBindings(TabClass backingClass) {
		//remove panel bindings
		String sql = "UPDATE " + AplosBean.getTableName(TabPanel.class) + " SET associatedBackingPage_id = NULL WHERE associatedBackingPage_id=" + backingClass.getId();
		ApplicationUtil.executeSql(sql);
		//remove main tab action bindings
		sql = "UPDATE " + AplosBean.getTableName(MenuTab.class) + " SET tabActionClass_id = NULL WHERE tabActionClass_id=" + backingClass.getId();
		ApplicationUtil.executeSql(sql);
		//remove additional bindings
		sql = "DELETE intermediateTable FROM menutab_defaultpagebindings intermediateTable";
		sql += " LEFT JOIN menutab ON menutab.id = intermediateTable.MenuTab_id";
		sql += " LEFT JOIN tabpanel ON tabpanel.id = menutab.tabPanel_id";
		sql += " WHERE intermediateTable.defaultPageBindings_id=" + backingClass.getId();
		ApplicationUtil.executeSql(sql);
	}

	private boolean canDisableBackingPageClass(TabClass backingClass) {

		boolean attached=false;
		//check if a binding exists (if the class is used somewhere)
		BeanDao tabDao = new BeanDao(MenuTab.class);
		tabDao.setWhereCriteria("bean.tabActionClass.id=" + backingClass.getId());
		List<AplosBean> tabs = tabDao.getAll();
		if (tabs != null && tabs.size() > 0) {
			attached = true;
		}
		for (AplosBean tab : tabs) {
			attachedClassesText.append("<li>");
			attachedClassesText.append(backingClass.getDisplayName());
			attachedClassesText.append(" is bound to ");
			attachedClassesText.append(tab.getDisplayName());
			attachedClassesText.append(" in " + ((MenuTab)tab).determineWebsite().getDisplayName());
			attachedClassesText.append("</li>");
		}
		BeanDao panelDao = new BeanDao(TabPanel.class);
		panelDao.setWhereCriteria("bean.associatedBackingPage.id=" + backingClass.getId());
		List<TabPanel> panels = panelDao.getAll();
		if (panels != null && panels.size() > 0) {
			attached = true;
		}
		for (TabPanel panel : panels) {
			attachedClassesText.append("<li>");
			attachedClassesText.append(backingClass.getDisplayName());
			attachedClassesText.append(" is bound to ");
			attachedClassesText.append(panel.getDisplayName());
			attachedClassesText.append(" in " + panel.getWebsite().getDisplayName());
			attachedClassesText.append("</li>");
		}
		//ensure the class isnt in use as an additional page binding anywhere
		tabs = null;
		tabs = ApplicationUtil.getParentsByChildInList(MenuTab.class, backingClass.getId(), "defaultPageBindings");
		if (tabs != null && tabs.size() > 0) {
			attached = true;
		}
		for (AplosBean tab : tabs) {
			attachedClassesText.append("<li>");
			attachedClassesText.append(backingClass.getDisplayName());
			attachedClassesText.append(" is bound to ");
			attachedClassesText.append(tab.getDisplayName());
			attachedClassesText.append(" in " + ((MenuTab)tab).determineWebsite().getDisplayName());
			attachedClassesText.append("</li>");
		}
		return !attached;
	}

	public void setEnabledClasses(List<TabClass> enabledClasses) {
		this.enabledClasses = enabledClasses;
	}

	public List<TabClass> getEnabledClasses() {
		return enabledClasses;
	}

	public void setAvailableClasses(List<TabClass> availableClasses) {
		this.availableClasses = availableClasses;
	}

	public List<TabClass> getAvailableClasses() {
		return availableClasses;
	}

	public void setSelectedEnabledClasses(List<TabClass> selectedEnabledClasses) {
		this.selectedEnabledClasses = selectedEnabledClasses;
	}

	public List<TabClass> getSelectedEnabledClasses() {
		return selectedEnabledClasses;
	}

	public void setSelectedAvailableClasses(List<TabClass> selectedAvailableClasses) {
		this.selectedAvailableClasses = selectedAvailableClasses;
	}

	public List<TabClass> getSelectedAvailableClasses() {
		return selectedAvailableClasses;
	}

	public SelectItem[] getEnabledClassSelectItems() {
		return AplosBean.getSelectItemBeans(enabledClasses);
	}

	public SelectItem[] getAvailableClassSelectItems() {
		return AplosBean.getSelectItemBeans(availableClasses);
	}

	public void setShowingConfirmationScreen(boolean isShowingConfirmationScreen) {
		this.isShowingConfirmationScreen = isShowingConfirmationScreen;
	}

	public boolean isShowingConfirmationScreen() {
		return isShowingConfirmationScreen;
	}

}
