package com.aplos.common.persistence.fieldLoader;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.utils.CommonUtil;

public class PropertyFieldLoader extends FieldLoader {
	
	public PropertyFieldLoader( String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		super(propertyName, persistentClass, fieldInfo);
	}

	@Override
	public Object getValue(PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded) {
		/*
		 * If the target bean has been loaded than the returned object is from a lazy bean and shouldn't be used
		 */
		if( getFieldInfo() != null && isTargetBeanLoaded ) {
			return getFieldInfo().getConvertedValue( (AplosAbstractBean) targetBean );
		} else {
			return returnedObject;
		}
	}

}
