package com.aplos.common.utils;

import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.aplos.common.AplosUrl;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.Currency;
import com.aplos.common.module.AplosModuleFilterer;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.NavigationStack;

/* This utility class is to contain those methods or helper methods we need
 * to call directly from a view which are usually in CommonUtil
 * (as the view cannot refer to a static method)
 */

@ManagedBean
@SessionScoped //needed for historyDropdownSelectedIndex
public class ViewUtil {

	private Integer historyDropdownSelectedIndex = null;

	public ViewUtil() {	}

	/** returns Yes/No, select/radio options for boolean values, automatically translated **/
	public SelectItem[] getBooleanSelectItems() {
		SelectItem[] items = new SelectItem[2];
		if (CommonConfiguration.getCommonConfiguration().getIsInternationalizedApplication()) {
			items[0] = new SelectItem (false,CommonUtil.translate("NO"));
			items[1] = new SelectItem (true,CommonUtil.translate("YES"));
		} else {
			items[0] = new SelectItem (false,"No");
			items[1] = new SelectItem (true,"Yes");
		}
		return items;
	}

	/** non-static implementation of {@link BackingPage#validationRequired() } **/
	/**
	 * @deprecated
	 * Please use backingPage.isValidationRequired() instead
	 **/
	@Deprecated
	public boolean getIsValidationRequired() {
		return BackingPage.validationRequired();
	}

	/** non-static implementation of {@link AplosModuleFilterer#getCountrySelectItems() } **/
	public SelectItem[] getCountrySelectItems() {
		return ApplicationUtil.getAplosModuleFilterer().getCountrySelectItems();
	}

	public SelectItem[] getCountrySelectItemsWithNotSelected() {
		SelectItem[] items = ApplicationUtil.getAplosModuleFilterer().getCountrySelectItems();
		SelectItem[] newItems = new SelectItem[items.length+1];
		newItems[0] = new SelectItem(null,CommonUtil.translate("ANY"));
		for (int i=1; i <= items.length; i++) {
			newItems[i] = items[i-1];
		}
		return newItems;
	}

	public String getContextLangaugeString() {
		return CommonUtil.getContextLocale().getLanguage();
	}

	/** uses {@link AplosModuleFilterer#getCountrySelectItems() } to generate a list of associated country codes **/
	public SelectItem[] getCountryCodeSelectItems() {

		SelectItem[] items = getCountrySelectItemsWithPleaseSelect();
		for (SelectItem item : items) {
			if (item.getValue() == null) {
				continue;
			}
			String display = ((Country)item.getValue()).getName() + " (+" + ((Country)item.getValue()).getIddCode() + ")";
			item.setLabel(display);
			item.setValue(((Country)item.getValue()).getIddCode()); // we needs strings not objects - no converter
		}

		return items;

	}

	/** non-static implementation of {@link AplosModuleFilterer#getCountrySelectItems() } **/
	public SelectItem[] getCountrySelectItemsWithPleaseSelect() {
		SelectItem[] items = ApplicationUtil.getAplosModuleFilterer().getCountrySelectItems();
		SelectItem[] newItems = new SelectItem[items.length+1];
		newItems[0] = new SelectItem(null,"Please Select");
		for (int i=1; i <= items.length; i++) {
			newItems[i] = items[i-1];
		}
		return newItems;
	}

	public String menuRedirect(String whereToNavigateTo, boolean clearDown, boolean addContext) {
		return menuRedirect(whereToNavigateTo, clearDown, addContext, null);
	}

	/** trigger action to keep breadcrumbs synchronised when clicking a menu-tab **/
	public String menuRedirect(String whereToNavigateTo, boolean clearDown, boolean addContext, Long menuTabId) {
		AplosUrl aplosUrl = new AplosUrl(whereToNavigateTo);
		if( menuTabId != null ) {
			Set<MenuTab> menuTabSet = ApplicationUtil.getMenuCacher().getMenuTabsByUrl( FormatUtil.removeExtension( aplosUrl.toString() ) );
			if( menuTabSet != null && menuTabSet.size() > 1 ) {
				aplosUrl.addQueryParameter( AplosScopedBindings.MENU_TAB_ID, menuTabId);
			}
		}
		JSFUtil.redirect(aplosUrl,addContext);
		return null;
	}

	/** trigger action when clicking a crumb in the breadcrumbs **/
	public String breadcrumbRedirect(int index) {
		JSFUtil.getNavigationStack().navigateBackTo(index); //breadcrumb style redirect
		return null;
	}
	
	public String historyRedirect(int index) {
		if (JSFUtil.getHistoryList().size() >= index) {
			NavigationStack.navigateTo(JSFUtil.getHistoryList().get(index)); //history style redirect
		}
		return null;
	}

	public void historyRedirect(ValueChangeEvent event) {
		if( event.getPhaseId().equals( PhaseId.INVOKE_APPLICATION ) ) {
			historyRedirect();
		} else {
			event.setPhaseId( PhaseId.INVOKE_APPLICATION );
			event.queue();
			return;
		}
	}

	/** trigger action when changing the value of the breadcrumbs history dropdown **/
	public String historyRedirect() {
		//NavigationStack.navigateTo((BackingPageState) JSFUtil.getSessionAttribute(BreadCrumbs.HISTORY_VARIABLE_BINDING));
		//String historyVariable = (String) JSFUtil.getSession().getAttribute(BreadCrumbs.HISTORY_VARIABLE_BINDING);
		if (historyDropdownSelectedIndex != null) {
			return historyRedirect(historyDropdownSelectedIndex - 1);
		} //else {
			//JSFUtil.getSession().getAttributeNames();
			//JSFUtil.addMessageForError("An error occured. We could not access your history.");
		//}
		historyDropdownSelectedIndex = null;
		return null;
	}

	/** unused, relates to commented feature in breadcrumbs which allows us to remove a specific crumb **/
	public String breadcrumbDelete(int index) {
		JSFUtil.getNavigationStack().remove(index);
		JSFUtil.redirect(new AplosUrl(JSFUtil.getRequest().getRequestURI()), false);
		return null;
	}

	/** superuser function to disallow logins backend, triggers a holding-page **/
	public String toggleDatabaseHold() {
		CommonConfiguration.getCommonConfiguration().setDatabaseOnHold(!CommonConfiguration.getCommonConfiguration().isDatabaseOnHold());
		CommonConfiguration.getCommonConfiguration().saveDetails();
		if (CommonConfiguration.getCommonConfiguration().isDatabaseOnHold()) {
			JSFUtil.addMessage("The database is now locked, other users will not be able to access the system.");
		} else {
			JSFUtil.addMessage("The database is now unlocked, normal system access has resumed.");
		}
		ApplicationUtil.getAplosContextListener().getSiteTabPanel().resetTabs(ApplicationUtil.getAplosContextListener());
		JSFUtil.redirect(new AplosUrl(JSFUtil.getRequest().getRequestURI()), false); //so we rerender our site tab panel and disable any edit fields
		return null;
	}

	/** non-static wrapper for {@link CommonUtil#getCurrency()}
	 *  possibly required by custom component {@link CurrencyChanger }
	 * */
	public Currency getCurrency() {
		return CommonUtil.getCurrency();
	}

	/** non-static wrapper for {@link CommonUtil#setCurrency()}
	 *  required by custom component {@link CurrencyChanger }
	 * */
	public void setCurrency(Currency currency) {
		CommonUtil.setCurrency(currency);
	}

	/** required by custom component {@link CurrencyChanger }
	 * */
	public String switchCurrency() {
		JSFUtil.redirect(new AplosUrl(JSFUtil.getRequest().getRequestURI()), false);
		return null;
	}

	public void setHistoryDropdownSelectedIndex(
			Integer historyDropdownSelectedIndex) {
		this.historyDropdownSelectedIndex = historyDropdownSelectedIndex;
	}

	public Integer getHistoryDropdownSelectedIndex() {
		return historyDropdownSelectedIndex;
	}

}
