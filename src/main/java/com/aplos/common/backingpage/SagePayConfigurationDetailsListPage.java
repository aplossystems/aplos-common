package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SagePayConfigurationDetails.class)
public class SagePayConfigurationDetailsListPage extends ListPage {
	private static final long serialVersionUID = 3201052434378716561L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new SagePayConfigurationLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class SagePayConfigurationLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 6052458813535044105L;

		public SagePayConfigurationLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.name LIKE :similarSearchText";
		}

	}

}
