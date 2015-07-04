package com.aplos.common.interfaces;

import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.beans.SystemUser;

public interface FileDetailsOwnerInter {
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper();
	public void superSaveBean( SystemUser currentUser );
}
