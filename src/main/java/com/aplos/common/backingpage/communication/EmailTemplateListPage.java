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
import com.aplos.common.beans.communication.EmailTemplate;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=EmailTemplate.class)
public class EmailTemplateListPage extends ListPage {
	private static final long serialVersionUID = -8099874234052162955L;

	public List<EmailTemplate> getAvailableEmailTemplateList() {
		return new ArrayList<EmailTemplate>( Website.getCurrentWebsiteFromTabSession().getEmailTemplateMap().values() );
	}

	@Override
	public BeanDao getListBeanDao() {
		return new BeanDao( EmailTemplate.class, EmailTemplateEditPage.class );
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new EmailTemplateLazyDataModel(dataTableState, aqlBeanDao);
	}

	public class EmailTemplateLazyDataModel extends AplosLazyDataModel {
		private static final long serialVersionUID = 5795954858074510410L;

		public EmailTemplateLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			dataTableState.setShowingNewBtn(false);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.name LIKE :similarSearchText";
		}

	}

}
