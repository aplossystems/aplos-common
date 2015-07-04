package com.aplos.common.backingpage;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.utils.CommonUtil;

public abstract class LazyDataModelEditPage extends EditPage {
	private static final long serialVersionUID = -8849648476787495696L;
	private DataTableState dataTableState;

	public LazyDataModelEditPage() {
	}

	public LazyDataModelEditPage(Class<? extends AplosAbstractBean> beanClass) {
		init(new BeanDao(beanClass));
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		createDataTableState();
		return true;
	}

	public DataTableState getDataTableState() {
		return dataTableState;
	}

	public void setDataTableState(DataTableState dataTableState) {
		this.dataTableState = dataTableState;
	}

	public DataTableState createDataTableState() {
		if( getDataTableState() == null && getListBeanDao() != null ) {
			setDataTableState( CommonUtil.createDataTableState( this, getClass(), getListBeanDao() ) );
		}
		return getDataTableState();
	}

	public abstract BeanDao getListBeanDao();


}
