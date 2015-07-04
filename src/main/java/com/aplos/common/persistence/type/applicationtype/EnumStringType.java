package com.aplos.common.persistence.type.applicationtype;

import java.lang.reflect.Field;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;

public class EnumStringType extends VarCharType {
	
	public Object getValue( Field field, AplosAbstractBean aplosAbstractBean ) {
		try {
			field.setAccessible(true);
			Enum myEnum = (Enum) field.get(aplosAbstractBean);
			if( myEnum != null ) {
				return myEnum.name();
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
	
	@Override
	public Class<?> getJavaClass() {
		return Enum.class;
	}
}
