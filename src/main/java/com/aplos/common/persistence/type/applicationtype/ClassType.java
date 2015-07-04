package com.aplos.common.persistence.type.applicationtype;

import java.lang.reflect.Field;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;

public class ClassType extends VarCharType {
	
	public Object getValue( Field field, AplosAbstractBean aplosAbstractBean ) {
		try {
			field.setAccessible(true);
			Class myClass = (Class) field.get(aplosAbstractBean);
			if( myClass != null ) {
				return myClass.getName();
			} else {
				return null;
			}
		} catch (IllegalArgumentException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		} catch (IllegalAccessException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		}
		return null;
	}
}
