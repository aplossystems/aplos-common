package com.aplos.common.interfaces;

import java.util.List;

import com.aplos.common.FacebookPage;

public interface FacebookPageHolder {
	public List<FacebookPage> getFacebookPages();
	public boolean saveDetails();
}
