package com.aplos.common.backingpage.marketing;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PotentialCompanyInteraction.class)
public class PotentialCompanyInteractionListPage extends ListPage {
	private static final long serialVersionUID = 6393428215031398457L;

	public PotentialCompanyInteractionListPage() {
		super();
		addRequiredStateBinding( PotentialCompany.class );
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new PotentialCompanyInteractionLdm(dataTableState, aqlBeanDao);
	}

	public class PotentialCompanyInteractionLdm extends AplosLazyDataModel {

		private static final long serialVersionUID = 1390346317223481343L;

		public PotentialCompanyInteractionLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters) {
			PotentialCompany potentialCompany = JSFUtil.getBeanFromScope( PotentialCompany.class );
			getBeanDao().setWhereCriteria("bean.potentialCompany.id=" + potentialCompany.getId());
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}

		public void copyLastInteraction() {
			PotentialCompanyInteraction oldInteraction = JSFUtil.getBeanFromScope( PotentialCompanyInteraction.class );
			super.goToNew();
			PotentialCompany potentialCompany = JSFUtil.getBeanFromScope( PotentialCompany.class );
			PotentialCompanyInteraction potentialCompanyInteraction = JSFUtil.getBeanFromScope(PotentialCompanyInteraction.class);
			potentialCompanyInteraction.setPotentialCompany( potentialCompany );
			if( oldInteraction != null ) {
				oldInteraction.copyFieldsIntoNewInteraction( potentialCompanyInteraction );
			} else {
				potentialCompanyInteraction.setPotentialCompanyStatus( potentialCompany.getPotentialCompanyStatus() );
			}
		}

		@Override
		public void goToNew() {
			PotentialCompanyInteraction oldInteraction = JSFUtil.getBeanFromScope( PotentialCompanyInteraction.class );
			super.goToNew();
			PotentialCompany potentialCompany = JSFUtil.getBeanFromScope( PotentialCompany.class );
			PotentialCompanyInteraction potentialCompanyInteraction = JSFUtil.getBeanFromScope(PotentialCompanyInteraction.class);
			potentialCompanyInteraction.setPotentialCompany( potentialCompany );
			potentialCompanyInteraction.setPotentialCompanyStatus( potentialCompany.getPotentialCompanyStatus() );
			if( oldInteraction != null ) {
				potentialCompanyInteraction.setMethod( oldInteraction.getMethod() );
			}
		}
		
	}
	
}
