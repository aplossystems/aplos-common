package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.module.AplosModule;
import com.aplos.common.utils.ApplicationUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Website.class)
public class WebsiteListPage extends ListPage {

	private static final long serialVersionUID = 432319719828425841L;

	public String firstCmsWebsiteActionListener() {
		AplosModule aplosModule = ApplicationUtil.getAplosContextListener().getAplosModuleByName( "cms" );
		if( aplosModule != null ) {
			return aplosModule.fireNewWebsiteAction();
		} else {
			return null;
		}

	}
}
