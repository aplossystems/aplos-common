package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.DynamicBundleEntry;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=DynamicBundleEntry.class)
public class DynamicBundleEntryListPage extends ListPage {
	private static final long serialVersionUID = 1443111876759396552L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new DynamicBundleEntryLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class DynamicBundleEntryLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = -5376206539639159621L;

		public DynamicBundleEntryLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.entryKey LIKE :similarSearchText OR bean.entryValue LIKE :similarSearchText";
		}

	}
}
