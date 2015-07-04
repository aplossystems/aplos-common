package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=VatType.class)
public class VatTypeListPage extends ListPage  {

	private static final long serialVersionUID = 715609058599021L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new VatTypeLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class VatTypeLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = -4961441263034285761L;

		public VatTypeLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.code LIKE :similarSearchText OR bean.percentage LIKE :similarSearchText";
		}

	}
}
