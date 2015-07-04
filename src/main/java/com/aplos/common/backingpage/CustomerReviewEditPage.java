package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.CustomerReview;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CustomerReview.class)
public class CustomerReviewEditPage extends EditPage {
	private static final long serialVersionUID = -1884024298818809127L;

}
