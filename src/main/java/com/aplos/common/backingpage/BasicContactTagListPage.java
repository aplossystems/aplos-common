package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.BasicContactTag;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicContactTag.class)
public class BasicContactTagListPage extends ListPage {
	private static final long serialVersionUID = 5886369316199551764L;
	
	
	
}
