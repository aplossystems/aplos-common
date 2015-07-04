package com.aplos.common.utils;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.lookups.UserLevel;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class UserLevelUtil extends AplosBean {
	private static final long serialVersionUID = 4136364742080313800L;

	@OneToOne
	private UserLevel superUserLevel;

	@OneToOne
	private UserLevel adminUserLevel;

	@OneToOne
	private UserLevel debugUserLevel;
	
	@OneToOne
	private UserLevel marketerLevel;

	public static boolean isCurrentUserAdminLevel() {
		return JSFUtil.getLoggedInUser().isAdmin();
	}

	public static boolean isCurrentUserSuperLevel() {
		return JSFUtil.getLoggedInUser().isSuperuser();
	}

	public void setSuperUserLevel(UserLevel superUserLevel) {
		this.superUserLevel = superUserLevel;
	}

	public UserLevel getSuperUserLevel() {
		return superUserLevel;
	}

	public void setAdminUserLevel(UserLevel adminUserLevel) {
		this.adminUserLevel = adminUserLevel;
	}

	public UserLevel getAdminUserLevel() {
		return adminUserLevel;
	}

	public void setDebugUserLevel(UserLevel debugUserLevel) {
		this.debugUserLevel = debugUserLevel;
	}

	public UserLevel getDebugUserLevel() {
		return debugUserLevel;
	}

	public UserLevel getMarketerLevel() {
		return marketerLevel;
	}

	public void setMarketerLevel(UserLevel marketerLevel) {
		this.marketerLevel = marketerLevel;
	}
}
