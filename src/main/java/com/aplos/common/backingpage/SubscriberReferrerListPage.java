package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SubscriberReferrer;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SubscriberReferrer.class)
public class SubscriberReferrerListPage extends ListPage {
	private static final long serialVersionUID = -7488822180124067940L;
}
