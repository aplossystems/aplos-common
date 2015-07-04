package com.aplos.common.backingpage;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.CountryArea;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CountryArea.class)
public class CountryAreaListPage extends ListPage {
	private static final long serialVersionUID = -2480979971904497647L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new CountryAreaLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class CountryAreaLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = -8248787815350622296L;

		public CountryAreaLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public void goToNew() {
			super.goToNew();

			CountryArea countryArea = (CountryArea) JSFUtil.getBeanFromScope( CountryArea.class );
			countryArea.setCountry((Country)JSFUtil.getBeanFromScope(Country.class));
		}

		@Override
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
			getBeanDao().clearWhereCriteria();
			getBeanDao().addWhereCriteria("country_id = " + ((Country)JSFUtil.getBeanFromScope(Country.class)).getId());
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}

	}

}
