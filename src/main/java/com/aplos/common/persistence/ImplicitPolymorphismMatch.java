package com.aplos.common.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aplos.common.persistence.fieldinfo.FieldInfo;

public class ImplicitPolymorphismMatch {
	private Class<?> baseClass;
	private Map<FieldInfo,List<PersistentClass>> fieldInfoMap = new HashMap<FieldInfo,List<PersistentClass>>();
	private Set<PersistentClass> persistentClasses = new HashSet<PersistentClass>();
	
	public ImplicitPolymorphismMatch( Class<?> baseClass ) {
		setBaseClass(baseClass);
	}
	
	public void addToFieldInfoMap( FieldInfo fieldInfo, PersistentClass persistentClass ) {
		List<PersistentClass> persistentClasses = fieldInfoMap.get( fieldInfo );
		if( persistentClasses == null ) {
			persistentClasses = new ArrayList<PersistentClass>();
			fieldInfoMap.put( fieldInfo, persistentClasses );
		}
		persistentClasses.add( persistentClass );
	}
	
	public Class<?> getBaseClass() {
		return baseClass;
	}
	public void setBaseClass(Class<?> baseClass) {
		this.baseClass = baseClass;
	}
	public Set<PersistentClass> getPersistentClasses() {
		return persistentClasses;
	}
	public void setPersistentClasses(Set<PersistentClass> persistentClasses) {
		this.persistentClasses = persistentClasses;
	}

	public Map<FieldInfo,List<PersistentClass>> getFieldInfoMap() {
		return fieldInfoMap;
	}

	public void setFieldInfoMap(Map<FieldInfo,List<PersistentClass>> fieldInfoMap) {
		this.fieldInfoMap = fieldInfoMap;
	}
}
