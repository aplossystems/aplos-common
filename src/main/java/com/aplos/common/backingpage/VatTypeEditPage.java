package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.VatType;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=VatType.class)
public class VatTypeEditPage extends EditPage {

	private static final long serialVersionUID = -1123215782182248126L;
}
