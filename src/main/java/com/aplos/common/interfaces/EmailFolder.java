package com.aplos.common.interfaces;

import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.EmailActionType;

public interface EmailFolder {
	public Long getId();
	public String getDisplayName();
	public String getEmailFolderSearchCriteria();
	public void aplosEmailAction( EmailActionType emailActionType, AplosEmail aplosEmail );
	public boolean saveDetails( SystemUser systemUser );
}
