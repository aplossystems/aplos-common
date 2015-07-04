package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;

public class PolymorphicIdAqlVariable extends AqlTableVariable {
	private PolymorphicTypeAqlVariable polymorphicTypeSelectCriteria;
	private Long temporaryId;

	public PolymorphicIdAqlVariable() {
	}

	public PolymorphicIdAqlVariable( AqlTable aqlTable, FieldInfo fieldInfo ) {
		super(aqlTable, fieldInfo);
	}
	
	@Override
	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
	 	if( getPolymorphicTypeSelectCriteria().getTemporaryType() != null ) {
	 		PersistentClass persistentClass = ((PersistentClassFieldInfo) getFieldInfo()).getPersistentClass();
	 		PolymorphicTypeAqlVariable.setField( isLoadingFromDatabase, bean, persistentClass, getField(), getPolymorphicTypeSelectCriteria().getTemporaryType(), (Long) value );
	 		getPolymorphicTypeSelectCriteria().setTemporaryType( null );
	 	} else {
	 		setTemporaryId( (Long) value );
	 	}
	}

	public PolymorphicTypeAqlVariable getPolymorphicTypeSelectCriteria() {
		return polymorphicTypeSelectCriteria;
	}

	public void setPolymorphicTypeSelectCriteria(
			PolymorphicTypeAqlVariable polymorphicTypeSelectCriteria) {
		this.polymorphicTypeSelectCriteria = polymorphicTypeSelectCriteria;
	}

	public Long getTemporaryId() {
		return temporaryId;
	}

	public void setTemporaryId(Long temporaryId) {
		this.temporaryId = temporaryId;
	}
}
