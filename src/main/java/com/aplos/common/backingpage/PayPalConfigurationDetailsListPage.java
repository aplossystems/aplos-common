package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PayPalConfigurationDetails.class)
public class PayPalConfigurationDetailsListPage extends ListPage {
	private static final long serialVersionUID = -7964093821322904224L;

}
