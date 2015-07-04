package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.communication.BasicEmailFolder;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicEmailFolder.class)
public class BasicEmailFolderEditPage extends EditPage {
	private static final long serialVersionUID = 1711277494658026763L;

}
