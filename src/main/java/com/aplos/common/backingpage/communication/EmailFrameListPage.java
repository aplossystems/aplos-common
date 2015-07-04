package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.communication.EmailFrame;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=EmailFrame.class)
public class EmailFrameListPage extends ListPage {
	private static final long serialVersionUID = 5132610384779805242L;

}
