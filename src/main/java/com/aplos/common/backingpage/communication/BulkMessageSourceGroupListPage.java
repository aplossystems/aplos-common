package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.EmailTemplate;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BulkMessageSourceGroup.class)
public class BulkMessageSourceGroupListPage extends ListPage {

	private static final long serialVersionUID = 6301730747573172369L;

	@Override
	public BeanDao getListBeanDao() {
		BeanDao bulkMessageSourceGroupDao = new BeanDao( BulkMessageSourceGroup.class );
		bulkMessageSourceGroupDao.addWhereCriteria( "bean.class = 'BulkMessageSourceGroup'" );
		return bulkMessageSourceGroupDao;
	}

}
