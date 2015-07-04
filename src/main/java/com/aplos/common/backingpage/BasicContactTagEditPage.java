package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.beans.BasicContactTag;

@ManagedBean
@ViewScoped
@GlobalAccess
@AssociatedBean(beanClass=BasicContactTag.class)
public class BasicContactTagEditPage extends EditPage {
	private static final long serialVersionUID = -1485923007971458593L;

}
