package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CardSaveConfigurationDetails.class)
public class CardSaveConfigurationDetailsListPage extends ListPage {
	private static final long serialVersionUID = 7036239839911260126L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new CardSaveConfigurationLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class CardSaveConfigurationLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 6052458813535044105L;

		public CardSaveConfigurationLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.name LIKE :similarSearchText";
		}

	}

}
