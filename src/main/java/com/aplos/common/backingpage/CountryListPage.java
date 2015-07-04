package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Country.class)
public class CountryListPage extends ListPage {
	private static final long serialVersionUID = -5153081413768676912L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new CountryLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class CountryLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 7634587148778045714L;

		public CountryLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			/*
			 * You do not want the user to add new countries as this will break 
			 * payments as the countryCode needs to be set correctly.
			 */
			getDataTableState().setShowingNewBtn(false);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.name LIKE :similarSearchText";
		}

	}
}
