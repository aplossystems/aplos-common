package com.aplos.common.backingpage.communication;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.EmailType;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=AplosEmail.class)
public class OutgoingEmailListPage extends AplosEmailListPage {
	private static final long serialVersionUID = 8887383549141097312L;

	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel( DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new OutgoingAplosEmailLdm(dataTableState, aqlBeanDao);
	}

	public class OutgoingAplosEmailLdm extends AplosEmailLdm {
		private static final long serialVersionUID = -7397254615143164517L;

		public OutgoingAplosEmailLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			setMailServerSettings(null);
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters ) {
			return load(first, pageSize, sortField, sortOrder, filters, true, true);
		}
		
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters, boolean clearWhereCriteria, boolean clearSelectJoin ) {
			if( clearWhereCriteria ) {
				getBeanDao().clearWhereCriteria();
			}
			getBeanDao().addWhereCriteria( "bean.emailType != " + EmailType.INCOMING.ordinal() );
			return super.load(first, pageSize, sortField, sortOrder, filters, false, clearSelectJoin );
		}
	}
}
