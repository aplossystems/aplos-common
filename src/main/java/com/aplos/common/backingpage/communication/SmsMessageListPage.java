package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.SmsMessage;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SmsMessage.class)
public class SmsMessageListPage extends ListPage {
	private static final long serialVersionUID = -4146619446471169543L;

	public SmsMessageListPage() { 	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new SmsMessageLdm(dataTableState, aqlBeanDao);
	}
	
	public class SmsMessageLdm extends AplosLazyDataModel {
		public SmsMessageLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public void goToNew(boolean redirect) {
			super.goToNew(redirect);
			SmsMessage smsMessage = resolveAssociatedBean();
			smsMessage.init();
		}
	}
}
