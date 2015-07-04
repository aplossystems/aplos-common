package com.aplos.common.persistence.fieldinfo;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.type.applicationtype.IntType;

public class IndexFieldInfo extends FieldInfo {
	private FieldInfo parentFieldInfo;
	
	public IndexFieldInfo( FieldInfo parentFieldInfo ) {
		super( parentFieldInfo.getParentPersistentClass(), parentFieldInfo.getField() );
		setParentFieldInfo(parentFieldInfo);
	}
	
	@Override
	public void init() {
		setSqlName( "position" );
		setApplicationType( new IntType() );
		getApplicationType().setNullable( false );
	}
	
	@Override
	public void setValue( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
		((PersistenceBean) bean).getHiddenFieldsMap().put( getSqlName(), value );
	}
	
	@Override
	public Object getValue(AplosAbstractBean aplosAbstractBean) {
		return ((PersistenceBean) aplosAbstractBean).getHiddenFieldsMap().get( getSqlName() );
	}
	
	public String determineFieldMapKeyName() {
		return getSqlName();
	}

	public FieldInfo getParentFieldInfo() {
		return parentFieldInfo;
	}

	public void setParentFieldInfo(FieldInfo parentFieldInfo) {
		this.parentFieldInfo = parentFieldInfo;
	}

}
