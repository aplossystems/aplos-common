package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.PageRequest;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PageRequest.class)
public class PageRequestListPage extends ListPage {
	private static final long serialVersionUID = 4254603330573726408L;
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new PageRequestLdm(dataTableState, aqlBeanDao);
	}
	
	public class PageRequestLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = 1713702560436522536L;
		
		private static final String ALL_PAGES = "All pages";
		private static final String FRONTEND_ONLY = "Frontend only";
		private static final String BACKEND_ONLY = "Backend only";
		
		private boolean isShowingOnlyStatus404 = false;
		private boolean isShowingOnlySessionIdCreated = false;
		private String selectedPageVisibility;
		
		public PageRequestLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters) {
			getBeanDao().clearWhereCriteria();
			if( isShowingOnlyStatus404() ) {
				getBeanDao().addWhereCriteria( "bean.isStatus404 = true" );
			}
			if( FRONTEND_ONLY.equals( getSelectedPageVisibility() ) ) {
				getBeanDao().addWhereCriteria( "bean.isFrontend = true" );
			} else if( BACKEND_ONLY.equals( getSelectedPageVisibility() ) ) {
				getBeanDao().addWhereCriteria( "bean.isFrontend = false" );
			}
			if( isShowingOnlySessionIdCreated() ) {
				getBeanDao().addWhereCriteria( "bean.sessionCreated = true" );
			}
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}
		
		public String getSessionIdStyle() {
			PageRequest pageRequest = (PageRequest) getAplosBean();
			if( pageRequest != null && pageRequest.isSessionCreated() ) {
				return "background-color:green";
			}
			return "";
		}
		
		public List<SelectItem> getPageVisibilitySelectItems() {
			List<SelectItem> selectItems = new ArrayList<SelectItem>();
			selectItems.add( new SelectItem( ALL_PAGES ) );
			selectItems.add( new SelectItem( FRONTEND_ONLY ) );
			selectItems.add( new SelectItem( BACKEND_ONLY ) );
			return selectItems;
		}

		public boolean isShowingOnlyStatus404() {
			return isShowingOnlyStatus404;
		}

		public void setShowingOnlyStatus404(boolean isShowingOnlyStatus404) {
			this.isShowingOnlyStatus404 = isShowingOnlyStatus404;
		}

		public String getSelectedPageVisibility() {
			return selectedPageVisibility;
		}

		public void setSelectedPageVisibility(String selectedPageVisibility) {
			this.selectedPageVisibility = selectedPageVisibility;
		}

		public boolean isShowingOnlySessionIdCreated() {
			return isShowingOnlySessionIdCreated;
		}

		public void setShowingOnlySessionIdCreated(
				boolean isShowingOnlySessionIdCreated) {
			this.isShowingOnlySessionIdCreated = isShowingOnlySessionIdCreated;
		}
	}
}
