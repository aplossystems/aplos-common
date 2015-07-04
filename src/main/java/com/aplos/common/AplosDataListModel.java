package com.aplos.common;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;

import org.primefaces.model.SelectableDataModel;

import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ComponentUtil;
import com.aplos.common.utils.JSFUtil;

public class AplosDataListModel extends ListDataModel implements SelectableDataModel {
	
	public AplosDataListModel( List dataList ) {
		super( dataList );
	}
	
	@Override
	public Object getRowData(String rowKey) {
		return ((List) getWrappedData()).get(Integer.parseInt(rowKey));
	}
	
	@Override
	public Object getRowKey(Object object) {
		return ((List) getWrappedData()).indexOf(object);
	}


	public void selectBean() {
		selectBean( true );
	}

	public void selectBean( boolean redirect ) {
		AplosBean aplosBean = getAplosBean();
		selectBean( aplosBean.getClass(), aplosBean.getId(), redirect );
	}
	
	public AplosBean getAplosBean() {
		return (AplosBean) JSFUtil.getRequest().getAttribute( "tableBean" );
	}

	@SuppressWarnings("rawtypes")
	public void selectBean( Class<? extends AplosBean> loadBeanClass, Long loadBeanId, boolean redirect ) {
		AplosBean loadedAplosBean = new BeanDao( loadBeanClass ).get( loadBeanId );
		loadedAplosBean = (AplosBean) loadedAplosBean.getSaveableBean();
		loadedAplosBean.addToScope();
	}

	public String getDeleteBeanImage() {
		return ComponentUtil.getImageUrl(FacesContext.getCurrentInstance(), "action_delete.gif");
	}

	public String getReinstateBeanImage() {
		return ComponentUtil.getImageUrl(FacesContext.getCurrentInstance(), "action_add.gif");
	}

}
