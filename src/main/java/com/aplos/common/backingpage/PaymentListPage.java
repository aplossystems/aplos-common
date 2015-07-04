package com.aplos.common.backingpage;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PaymentGatewayPost.class)
public class PaymentListPage extends ListPage {
	private static final long serialVersionUID = -5102101797009689973L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new PaymentLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class PaymentLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = -8248787815350622296L;

		public PaymentLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			setEditPageClass( PaymentEditPage.class );
		}

		@Override
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
			getBeanDao().clearWhereCriteria();
			getBeanDao().addWhereCriteria("bean.onlinePaymentOrder = null");
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}
		
		@Override
		public void goToNew( boolean redirect ) {
			PaymentGatewayPost paymentGatewayPost = CommonConfiguration.getCommonConfiguration().getDefaultPaymentGateway().getDirectPost();
			paymentGatewayPost.initialiseNewBean();
			
			//not the right behaviour now : we want it to call the beans method which may bind it to multiple
			//JSFUtil.addToTabSession( AqlBeanDao.getBinding(), newBean);
			paymentGatewayPost.addToScope( determineAssociatedBeanScope() );

			if( redirect ) {
				JSFUtil.redirect( determineEditPageClass( paymentGatewayPost ) );
			}
		}

		@Override
		public String getSearchCriteria() {
			return "bean.paid LIKE :similarSearchText OR bean.decTotal LIKE :similarSearchText OR bean.status LIKE :similarSearchText";
		}

	}

}