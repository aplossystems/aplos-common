package com.aplos.common.backingpage;

import java.util.List;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.primefaces.event.SelectEvent;

import com.aplos.common.SiteBeanDao;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.DynamicBundleEntry;
import com.aplos.common.beans.DynamicBundleLanguage;
import com.aplos.common.beans.Website;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=DynamicBundleEntry.class)
public class DynamicBundleEntryEditPage extends EditPage {

	private static final long serialVersionUID = 7421286459907793333L;
	
	private static final int BASIC_VALUE_TYPE = 800001;
	private static final int HTML_VALUE_TYPE = 800002;
	private static final int KEY_REDIRECT_VALUE_TYPE = 800003;
	
	private DynamicBundleEntry selectedDynamicBundleEntry;
	private int valueType = BASIC_VALUE_TYPE;

	public DynamicBundleEntryEditPage() {

		getEditPageConfig().setOkBtnActionListener( new OkBtnListener(this) {
			private static final long serialVersionUID = -4592474148962169358L;
			@Override
			public void actionPerformed( boolean redirect ) {
				super.actionPerformed( redirect );
				Website.getCurrentWebsiteFromTabSession().getMainDynamicBundle().refreshDynamicBundleEntryMap();
			}
		});

		getEditPageConfig().setApplyBtnActionListener( new SaveBtnListener(this) {
			private static final long serialVersionUID = -2342380732752094546L;
			@Override
			public void actionPerformed( boolean redirect ) {
				super.actionPerformed( redirect );
				Website.getCurrentWebsiteFromTabSession().getMainDynamicBundle().refreshDynamicBundleEntryMap();
			}
		});
	}

	public SelectItem[] getValueTypeSelectItems() {
		SelectItem[] items = new SelectItem[3];
		items[0] = new SelectItem(BASIC_VALUE_TYPE, "Text Value");
		items[1] = new SelectItem(HTML_VALUE_TYPE, "Formatted HTML Value");
		items[2] = new SelectItem(KEY_REDIRECT_VALUE_TYPE, "Another Key's Value");
		return items;
	}
	
	@Override
	public boolean responsePageLoad() {
		DynamicBundleEntry dynamicBundleEntry = JSFUtil.getBeanFromScope(DynamicBundleEntry.class);
		if (dynamicBundleEntry != null) {
			if (dynamicBundleEntry.isRedirectKey()) {
				valueType = KEY_REDIRECT_VALUE_TYPE;
			} else if (dynamicBundleEntry.isHtml()) {
				valueType = HTML_VALUE_TYPE;
			} else {
				valueType = BASIC_VALUE_TYPE;
			}
		}
		return super.responsePageLoad();
	}
	
	@SuppressWarnings("unchecked")
	public List<DynamicBundleEntry> suggestKeys(String searchStr) {
		DynamicBundleEntry dynamicBundleEntry = JSFUtil.getBeanFromScope(DynamicBundleEntry.class);
		BeanDao keyDao = new BeanDao( DynamicBundleEntry.class );
		keyDao.addWhereCriteria("bean.entryKey like :similarSearchText OR bean.entryValue like :similarSearchText");
		if (dynamicBundleEntry != null && !dynamicBundleEntry.isNew()) {
			//dont let us create an infinite loop
			keyDao.addWhereCriteria("bean.id != " + dynamicBundleEntry.getId());
		}
		keyDao.setMaxResults( 20 );
		keyDao.setIsReturningActiveBeans( true );
		keyDao.setNamedParameter( "similarSearchText", "%" + searchStr + "%" );
		return (List<DynamicBundleEntry>) keyDao.getAll();
	}

	public void propagateValueTypeToBean() {
		DynamicBundleEntry dynamicBundleEntry = JSFUtil.getBeanFromScope(DynamicBundleEntry.class);
		if (dynamicBundleEntry != null) {
			if (valueType == KEY_REDIRECT_VALUE_TYPE) {
				dynamicBundleEntry.setRedirectKey(true);
				dynamicBundleEntry.setHtml(false);
			} else if (valueType == HTML_VALUE_TYPE) {
				dynamicBundleEntry.setRedirectKey(false);
				dynamicBundleEntry.setHtml(true);
			} else {
				dynamicBundleEntry.setRedirectKey(false);
				dynamicBundleEntry.setHtml(false);
			}
		}
	}

	public void addKeyToRedirectTo( SelectEvent event ) {
		DynamicBundleEntry dynamicBundleEntryKey = (DynamicBundleEntry) event.getObject();
		DynamicBundleEntry dynamicBundleEntry = JSFUtil.getBeanFromScope( DynamicBundleEntry.class );
		dynamicBundleEntry.setKeyToRedirectTo(dynamicBundleEntryKey);
		setSelectedDynamicBundleEntry( null );
	}

	public SelectItem[] getEntryLanguageSelectItems() {
		SiteBeanDao languageDao = new SiteBeanDao(DynamicBundleLanguage.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(languageDao.getAll(), "Not Selected");
	}

	public List<DynamicBundleLanguage> getLanguages() {
		SiteBeanDao languageDao = new SiteBeanDao(DynamicBundleLanguage.class);
		return languageDao.getAll();
	}

	public String switchLanguage(String languageKey) {
		CommonUtil.setContextLocale(new Locale(languageKey));
		return null;
	}

	public DynamicBundleEntry getSelectedDynamicBundleEntry() {
		return selectedDynamicBundleEntry;
	}

	public void setSelectedDynamicBundleEntry(DynamicBundleEntry selectedDynamicBundleEntry) {
		this.selectedDynamicBundleEntry = selectedDynamicBundleEntry;
	}

	public int getValueType() {
		return valueType;
	}

	public void setValueType(int valueType) {
		this.valueType = valueType;
	}

}
