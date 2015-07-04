package com.aplos.common.messageboxes;

import com.aplos.common.beans.AplosBean;

public class DependencyWarningBox extends MessageBox {
	AplosBean deleteBean;

	public DependencyWarningBox() {
		dependencyWarningBox();
	}

	public DependencyWarningBox(String title, String text, AplosBean deleteBean) {
		dependencyWarningBox();
		setTitle(title);
		setText(text);
		setDeleteBean(deleteBean);
	}

	public void dependencyWarningBox() {
		setOkBtnRendered( true );
		setCancelBtnRendered( true );
	}

	public AplosBean getDeleteBean() {
		return deleteBean;
	}

	public void setDeleteBean( AplosBean deleteBean ) {
		this.deleteBean = deleteBean;
	}

	@Override
	public void okBtnAction() {
		setActive( false );
		//DaoTableComponent tablePageRef = ((ListPage)JSFUtil.getSession().getAttribute( tablePage )).getMainTblCom();
		deleteBean.delete();
		//tablePageRef.updateRecordArray();
	}

}
