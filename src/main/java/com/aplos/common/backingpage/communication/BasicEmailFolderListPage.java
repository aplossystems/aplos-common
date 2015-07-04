package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.communication.BasicEmailFolder;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicEmailFolder.class)
public class BasicEmailFolderListPage extends ListPage {
	private static final long serialVersionUID = 921359390612699677L;
}
