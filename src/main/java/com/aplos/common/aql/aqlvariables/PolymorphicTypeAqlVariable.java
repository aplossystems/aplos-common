package com.aplos.common.aql.aqlvariables;

import java.lang.reflect.Field;

import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;

public class PolymorphicTypeAqlVariable extends AqlTableVariable {
	private PolymorphicIdAqlVariable polymorphicIdSelectCriteria;
	private String temporaryType;

	public PolymorphicTypeAqlVariable() {
	}

	public PolymorphicTypeAqlVariable( AqlTable aqlTable, FieldInfo fieldInfo ) {
		super(aqlTable, fieldInfo);
	}
	
	@Override
	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
	 	if( getPolymorphicIdSelectCriteria().getTemporaryId() != null ) {
	 		PersistentClass persistentClass = ((PersistentClassFieldInfo) getPolymorphicIdSelectCriteria().getFieldInfo()).getPersistentClass();
	 		setField( isLoadingFromDatabase, bean, persistentClass, getPolymorphicIdSelectCriteria().getField(), (String) value, getPolymorphicIdSelectCriteria().getTemporaryId() );
	 		getPolymorphicIdSelectCriteria().setTemporaryId( null );
	 	} else {
	 		setTemporaryType( (String) value );
	 	}
	}
	
	public static void setField( boolean isLoadingFromDatabase, AplosAbstractBean bean, PersistentClass persistentClass, Field field, String objectType, Long id ) throws Exception {
		PersistentClass subPersistentClass = persistentClass.getPersistentClassFamilyMap().get( objectType );
		PersistentClassFieldInfo.setFieldToPersistentClass( isLoadingFromDatabase, subPersistentClass, field, bean, id );
	}

	public PolymorphicIdAqlVariable getPolymorphicIdSelectCriteria() {
		return polymorphicIdSelectCriteria;
	}

	public void setPolymorphicIdSelectCriteria(
			PolymorphicIdAqlVariable polymorphicIdSelectCriteria) {
		this.polymorphicIdSelectCriteria = polymorphicIdSelectCriteria;
	}

	public String getTemporaryType() {
		return temporaryType;
	}

	public void setTemporaryType(String temporaryType) {
		this.temporaryType = temporaryType;
	}
	
}
