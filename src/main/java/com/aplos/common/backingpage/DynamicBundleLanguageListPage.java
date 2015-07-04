package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.DynamicBundleLanguage;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=DynamicBundleLanguage.class)
public class DynamicBundleLanguageListPage extends ListPage {
	private static final long serialVersionUID = 4159494673158034204L;

}
