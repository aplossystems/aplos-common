package com.aplos.common.aql.aqltables.unprocessed;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTable.JoinType;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;

public class ReverseJoinedAqlTable extends UnprocessedAqlTable {
	private String joinPath = "";
	private String parentAlias;
	private Class<? extends AplosAbstractBean> beanClass;
	private AqlTableVariable variableSelectCriteria;
	
	public ReverseJoinedAqlTable( Class<? extends AplosAbstractBean> beanClass, String joinPath, String parentAlias ) {
		setBeanClass( beanClass );
		setJoinPath( joinPath );
		setParentAlias(parentAlias);
		setAlias( getJoinPath().substring( 0, getJoinPath().indexOf( "." ) ) );
	}

	@Override
	public AqlTable createAqlTable( ProcessedBeanDao processedBeanDao, boolean isAddingTable ) {
		AqlTable parentTable = (AqlTable) processedBeanDao.findTable( parentAlias );
		if( parentTable == null ) {
			return null;
		} else if( !(parentTable instanceof AqlTable) ){
			ApplicationUtil.handleError( new Exception( "Parent table is not an aql table (" + getParentAlias() + ")" ) );
		}
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getBeanClass() ); 

		AqlTable newAqlTable = new AqlTable( processedBeanDao, persistentClass, (AqlTable) parentTable, getAlias(), null );
		if( isAddingTable ) {
			processedBeanDao.addQueryTable( getAlias(), newAqlTable );
		}
		if( getVariableSelectCriteria() == null ) {
			setVariableSelectCriteria( new UnevaluatedTableVariable( getJoinPath() ) );
		}
		
		if( !getVariableSelectCriteria().evaluateCriteriaTypes(processedBeanDao, false) ) {
			return null;
		}
		
		FieldInfo associatedFieldInfo = getVariableSelectCriteria().getFieldInfo();
		
		JunctionTable junctionTable = null;
		AqlTable junctionAqlTable;
		if( getVariableSelectCriteria().getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
			junctionTable = ((PersistentCollectionFieldInfo) getVariableSelectCriteria().getFieldInfo()).getJunctionTable(getVariableSelectCriteria().getFieldInfo().getParentPersistentClass().getTableClass());
			junctionAqlTable = new AqlTable( processedBeanDao, junctionTable, parentTable, null, null, true );
			if( isAddingTable ) {
				processedBeanDao.addQueryTable( junctionAqlTable.getAlias(), junctionAqlTable );
			}
			junctionAqlTable.setJoinInferred( true );
			junctionAqlTable.setJoinType( JoinType.RIGHT_OUTER_JOIN);
			junctionAqlTable.updateAssociatedFieldInfo(associatedFieldInfo);
			newAqlTable.updateParentTable(junctionAqlTable);
			associatedFieldInfo = junctionTable.getPersistentClassFieldInfo();
		} 
		
		newAqlTable.setJoinType( JoinType.RIGHT_OUTER_JOIN );
		associatedFieldInfo = getVariableSelectCriteria().getFieldInfo();
		
		if( associatedFieldInfo instanceof ForeignFieldInfo ) {
			newAqlTable.setAssociatedFieldInfo( associatedFieldInfo );
			newAqlTable.setSqlName(associatedFieldInfo.getField().getName());
			newAqlTable.setInternalVariable( new AqlTableVariable( newAqlTable, persistentClass.determinePrimaryKeyFieldInfo() ) );
			newAqlTable.setExternalVariable( new AqlTableVariable( (AqlTable) parentTable, ((ForeignFieldInfo) associatedFieldInfo).getForeignFieldInfo() ) );
		} else {
			newAqlTable.updateAssociatedFieldInfo( associatedFieldInfo );
		}
		
//		FieldInfo internalFieldInfo;
//		FieldInfo externalFieldInfo;
//		if( associatedFieldInfo instanceof CollectionFieldInfo ) {
//			externalFieldInfo = junctionTable.getPersistentClassFieldInfo();
//			internalFieldInfo = newAqlTable.getPersistentTable().determinePrimaryKeyFieldInfo();	
//		} else {
//			externalFieldInfo = ((PersistentClassFieldInfo) associatedFieldInfo).getPersistentClass().determinePrimaryKeyFieldInfo();
//			internalFieldInfo = associatedFieldInfo;
//		}
//		newAqlTable.setInternalVariable( new AqlTableVariable( newAqlTable, internalFieldInfo ) );
//		newAqlTable.setExternalVariable( new AqlTableVariable( (AqlTable) newAqlTable.getParentTable(), externalFieldInfo ) );
			
		return newAqlTable;
	}
	
	public String getJoinPath() {
		return joinPath;
	}
	public void setJoinPath(String joinPath) {
		this.joinPath = joinPath;
	}

	public AqlTableVariable getVariableSelectCriteria() {
		return variableSelectCriteria;
	}

	public void setVariableSelectCriteria(AqlTableVariable variableSelectCriteria) {
		this.variableSelectCriteria = variableSelectCriteria;
	}

	public Class<? extends AplosAbstractBean> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<? extends AplosAbstractBean> beanClass) {
		this.beanClass = beanClass;
	}

	public String getParentAlias() {
		return parentAlias;
	}

	public void setParentAlias(String parentAlias) {
		this.parentAlias = parentAlias;
	}
}
