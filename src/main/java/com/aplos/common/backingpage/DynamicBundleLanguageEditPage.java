package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.SiteBeanDao;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.DynamicBundleLanguage;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=DynamicBundleLanguage.class)
public class DynamicBundleLanguageEditPage extends EditPage {

	private static final long serialVersionUID = 624687876701295478L;

	public DynamicBundleLanguageEditPage() {

		//check language is unique before saving

		getEditPageConfig().setOkBtnActionListener( new OkBtnListener(this) {
			/**
			 *
			 */
			private static final long serialVersionUID = -5982234280640446521L;

			@Override
			public void actionPerformed( boolean redirect ) {
				if (checkLanguageCodeUnique()) {
					super.actionPerformed( redirect );
				}
			}
		});

		getEditPageConfig().setApplyBtnActionListener( new SaveBtnListener(this) {
			/**
			 *
			 */
			private static final long serialVersionUID = -9023150444319126882L;

			@Override
			public void actionPerformed( boolean redirect ) {
				if (checkLanguageCodeUnique()) {
					super.actionPerformed( redirect );
				}
			}
		});
	}

	public boolean checkLanguageCodeUnique() {
		DynamicBundleLanguage currentLanguage = JSFUtil.getBeanFromScope(DynamicBundleLanguage.class);
		SiteBeanDao languageDao = new SiteBeanDao(DynamicBundleLanguage.class);
		languageDao.setWhereCriteria("bean.languageKey='" + currentLanguage.getLanguageKey() + "'");
		if (!currentLanguage.isNew()) {
			languageDao.addWhereCriteria("bean.id != " + currentLanguage.getId());
		}
		int keyCount = languageDao.setIsReturningActiveBeans(true).getCountAll();
		if (keyCount < 1) {
			return true;
		} else {
			JSFUtil.addMessageForError("The language key '" + currentLanguage.getLanguageKey() + "' is already in use. Please choose a unique value.");
			return false;
		}
	}

}
