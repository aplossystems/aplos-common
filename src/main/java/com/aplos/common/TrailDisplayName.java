package com.aplos.common;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
/*
 * This is to make sure that the breadcrumbs and history show correctly
 * depending on the internationalisation that is choosen.
 *
 * Really this should be separated into more than one class with an interface
 * but as it's so simple it's been kept in one class.
 */
public class TrailDisplayName {
	private String dynamicBundleKey;
	private String plainText = "";
	private String name;
	
	public TrailDisplayName() {
		createName();
	}
	
	public TrailDisplayName( AplosAbstractBean aplosAbstractBean ) {
		if ( aplosAbstractBean != null ) {
			setName( CommonUtil.firstLetterToUpperCase( aplosAbstractBean.getTrailDisplayName()) );
		}
	}

	public String determineName() {
		String determinedName = null;
		if( !CommonUtil.isNullOrEmpty( name ) ) {
			determinedName = name;
		} else if( dynamicBundleKey != null ) {
			determinedName = ApplicationUtil.getAplosContextListener().translate( dynamicBundleKey );
		} else {
			determinedName = getPlainText();
		}

		return CommonUtil.firstLetterToUpperCase(determinedName);
	}
	
	public void createName() {
		String name = null;
	}

	public String getDynamicBundleKey() {
		return dynamicBundleKey;
	}
	public void setDynamicBundleKey(String dynamicBundleKey) {
		this.dynamicBundleKey = dynamicBundleKey;
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}
	
//	public String getName() {
//		return name;
//	}

	public void setName(String name) {
		this.name = name;
	}
}
