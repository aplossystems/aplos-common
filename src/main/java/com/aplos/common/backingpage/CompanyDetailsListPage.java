package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CompanyDetails.class)
public class CompanyDetailsListPage extends ListPage {

	private static final long serialVersionUID = 4067287594108722556L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new CompanyDetailsLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class CompanyDetailsLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 1775719018015780197L;

		public CompanyDetailsLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.companyName LIKE :similarSearchText";
		}

	}

}
