package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.HqlMessageSourceFinder;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=HqlMessageSourceFinder.class)
public class HqlMessageSourceFinderListPage extends ListPage {
	private static final long serialVersionUID = 161492324071415496L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new HqlMessageSourceFinderLdm(dataTableState, aqlBeanDao);
	}
	
	public class HqlMessageSourceFinderLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = 8781077269681526926L;

		public HqlMessageSourceFinderLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public void goToNew() {
			super.goToNew();
			HqlMessageSourceFinder hqlMessageSourceFinder = getAssociatedBeanFromScope();
			hqlMessageSourceFinder.setEmailRequired( true );
		}
	}
}
