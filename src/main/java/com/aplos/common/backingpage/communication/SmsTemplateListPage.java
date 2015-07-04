package com.aplos.common.backingpage.communication;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.templates.smstemplates.CustomSmsTemplate;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SmsTemplate.class)
public class SmsTemplateListPage extends ListPage {
	private static final long serialVersionUID = 9063438222677150909L;
	
	public SmsTemplateListPage() {
		super();
		getBeanDao().setEditPageClass( SmsTemplateEditPage.class );
	}

	public List<SmsTemplate> getAvailableSmsTemplateList() {
		return new ArrayList<SmsTemplate>( Website.getCurrentWebsiteFromTabSession().getSmsTemplateMap().values() );
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new SmsTemplateLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class SmsTemplateLazyDataModel extends AplosLazyDataModel {
		private static final long serialVersionUID = 5795954858074510410L;

		public SmsTemplateLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public void goToNew() {
			CustomSmsTemplate customSmsTemplate = new CustomSmsTemplate();
			customSmsTemplate.addToScope( determineAssociatedBeanScope() );
			JSFUtil.redirect( getBeanDao().getEditPageClass() );
		}

		@Override
		public String getSearchCriteria() {
			return "bean.name LIKE :similarSearchText";
		}

	}

}
