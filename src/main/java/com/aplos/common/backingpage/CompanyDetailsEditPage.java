package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CompanyDetails.class)
public class CompanyDetailsEditPage extends EditPage {
	private static final long serialVersionUID = 7042322473911474008L;


	@Override
	public boolean responsePageLoad() {
		addCompanyDetailsToScope();
		checkEditBean();
		super.responsePageLoad();
		return true;
	}
	
	@Override
	public <T extends AplosBean> T resolveAssociatedBean() {
		return super.resolveAssociatedEditBean();
	}
	
	protected void addCompanyDetailsToScope() {
		CompanyDetails companyDetails = JSFUtil.getBeanFromScope( CompanyDetails.class );
		if( companyDetails == null || companyDetails.isReadOnly() ) {
			CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getSaveableBean().addToScope();
		}
	}

}
