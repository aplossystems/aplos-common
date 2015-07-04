package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.SubscriberReferrer;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SubscriberReferrer.class)
public class SubscriberReferrerEditPage extends EditPage {

	private static final long serialVersionUID = -112146872838534839L;
}
