package com.aplos.common;

import com.aplos.common.beans.AplosBean;

public abstract class SaveableFileDetailsOwnerHelper extends FileDetailsOwnerHelper {
	private AplosBean aplosBean;
	
	public SaveableFileDetailsOwnerHelper( AplosBean aplosBean ) {
		setAplosBean( aplosBean );
	}
	
	public boolean isNew() {
		return getAplosBean().isNew();
	}
	
	public void saveDetails() {
		getAplosBean().saveDetails();
	}

	public AplosBean getAplosBean() {
		return aplosBean;
	}

	public void setAplosBean(AplosBean aplosBean) {
		this.aplosBean = aplosBean;
	}
}
