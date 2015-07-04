package com.aplos.common.backingpage.marketing;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.marketing.SalesCallTask;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SalesCallTask.class)
public class SalesCallTaskListPage extends ListPage {
	private static final long serialVersionUID = -1811073917963757901L;
	
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new SalesCallTaskLdm(dataTableState, aqlBeanDao);
	}
	
	public static class SalesCallTaskLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = 4771627512098035225L;

		public SalesCallTaskLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}
	}
}
