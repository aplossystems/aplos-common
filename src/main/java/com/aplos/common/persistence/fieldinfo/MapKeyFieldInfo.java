package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;

import com.aplos.common.persistence.PersistentClass;

public class MapKeyFieldInfo extends FieldInfo {
	private CollectionFieldInfo collectionFieldInfo;
	private Class<?> fieldClass;
	
	public MapKeyFieldInfo( CollectionFieldInfo collectionFieldInfo, Class<?> fieldClass, PersistentClass parentPersistentClass, Field field ) {
		super( parentPersistentClass, field );
		this.collectionFieldInfo = collectionFieldInfo;
		this.fieldClass = fieldClass;
	}
	
	@Override
	public void init() {
		setSqlName( "mapkey" );
		setApplicationType( determineDbType( getField(), getFieldClass() ) );
		getApplicationType().setNullable(false);
	}
	
	public String determineFieldMapKeyName() {
		return getSqlName();
	}

	public CollectionFieldInfo getCollectionFieldInfo() {
		return collectionFieldInfo;
	}

	public void setCollectionFieldInfo(CollectionFieldInfo collectionFieldInfo) {
		this.collectionFieldInfo = collectionFieldInfo;
	}

	public Class<?> getFieldClass() {
		return fieldClass;
	}

	public void setFieldClass(Class<?> fieldClass) {
		this.fieldClass = fieldClass;
	}
}
