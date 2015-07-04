package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.CountryArea;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CountryArea.class)
public class CountryAreaEditPage extends EditPage {
	private static final long serialVersionUID = -5093957045893774407L;
}
