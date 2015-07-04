package com.aplos.common.backingpage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.utils.FormatUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Subscriber.class)
public class SubscriberListPage extends ListPage {
	private static final long serialVersionUID = -3461451146133148298L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new SubscriberLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class SubscriberLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 7634587148778045714L;
		
		private Long subscriberTotal = 0l;
		private Long subscribedTotal = 0l;

		public SubscriberLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			calculateTotalValues(new HashMap<String, String>());
		}

		@Override
		public String getSearchCriteria() {
			return "CONCAT(bean.firstName, ' ', bean.surname) LIKE :similarSearchText OR bean.emailAddress LIKE :similarSearchText";
		}
		
		public String getSubscribedTotalStr() {
			return FormatUtil.formatHuge( getSubscribedTotal() );
		}
		
		public String getSubscriberTotalStr() {
			return FormatUtil.formatHuge( getSubscriberTotal() );
		}
		
		public String getUnsubscribedTotalStr() {
			return FormatUtil.formatHuge( getSubscriberTotal() - getSubscribedTotal() );
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
			List<Object> results = super.load(first, pageSize, sortField, sortOrder, filters);
			calculateTotalValues(filters);
			return results;
		}

		public void calculateTotalValues(Map<String, String> filters) {
			//copy, so we have same working set
			BeanDao dao =  new BeanDao(Subscriber.class).copy( getBeanDao() );
			dao.setSelectCriteria("bean.isSubscribed, count(bean.id)");
			dao.setGroupBy("bean.isSubscribed");	
			dao.setIsReturningActiveBeans(true);
			addSearchParameters(dao, filters);
			List<Object[]> results = dao.getResultFields();
			long numberOfSubscribers = 0;
			long numberOfSubscribersSubscribed = 0;
			//there will be 0, 1 or 2 results only. 0 is highly unlikely unless a new system.
			for (Object[] resultArr : results) {
				if ( resultArr[0] == null ) {
					subscribedTotal = 0l;
					subscriberTotal = 0l;
					break;
				} else if ( ((Boolean) resultArr[0]) ) {
					numberOfSubscribersSubscribed =  (Long) resultArr[1];
				}
				numberOfSubscribers += (Long) resultArr[1];
			}
			subscribedTotal = numberOfSubscribersSubscribed;
			subscriberTotal = numberOfSubscribers;
			if (subscriberTotal == 0) {
				subscriberTotal = 1l;
			}
		}
		
		public Long getSubscriberTotal() {
			return subscriberTotal;
		}

		public void setSubscriberTotal(Long subscriberTotal) {
			this.subscriberTotal = subscriberTotal;
		}

		public Long getSubscribedTotal() {
			return subscribedTotal;
		}

		public void setSubscribedTotal(Long subscribedTotal) {
			this.subscribedTotal = subscribedTotal;
		}
		
	}

}
