package com.aplos.common.aql.aqlvariables;

import java.lang.reflect.Field;
import java.sql.SQLException;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.MapKeyFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentMapKeyFieldInfo;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.utils.ReflectionUtil;

public class AqlTableVariable extends AqlVariable implements AqlTableVariableInter  {
	private FieldInfo fieldInfo;
	private Field field;
	private CollectionInformation collectionInformation;
	private boolean isFullTable = false;
	private ForeignKeyAqlVariable foreignKeySelectCriteria;
	private FieldInfo fullTableFieldInfo;
	private boolean isUsingForeignKey = false;
	private AqlTableAbstract aqlTable;

	public AqlTableVariable() {
	}
	
	public AqlTableVariable( AqlTable aqlTable, FieldInfo fieldInfo ) {
		AqlTable joinedTable = aqlTable.getJoinedTable(); 
		while( joinedTable != null ) {
			if( joinedTable.isVariableInScope( fieldInfo.getSqlName() ) ) {
				aqlTable = joinedTable;
			} 
			
			joinedTable = joinedTable.getJoinedTable();
		}
		setAqlTable( aqlTable );
		updateFieldInfo( fieldInfo );
		setName( fieldInfo.getSqlName() );
	}
	
	@Override
	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
	 	if( getCollectionInformation() != null ) {
	 		PersistentClass persistentClass = (PersistentClass) getCollectionInformation().getCollectionTable().getPersistentTable();
	 		FieldInfo primaryKeyFieldInfo = persistentClass.determinePrimaryKeyFieldInfo();
        	Field field = primaryKeyFieldInfo.getField();
        	value = SelectCriteria.convertFieldValue(value, primaryKeyFieldInfo, field );
    		if( field != null ) {
    			field.setAccessible(true);
    			field.set(bean, value);
        	} else {
        		ReflectionUtil.setField( (AplosAbstractBean) bean, getName(), value);
        	}
	 	} else {
 	    	if( getFieldInfo() != null ) {
 	    		if( getFieldInfo() instanceof PersistentClassFieldInfo 
 	    				&& value == null 
 	    				&& ((PersistentClassFieldInfo) getFieldInfo()).isRemoveEmpty()) {
 		    		((PersistentClassFieldInfo) getFieldInfo()).createAndSetEmptyBean( bean );
 	    		} else {
 	    			getFieldInfo().setValue( isLoadingFromDatabase, bean, value );
 	    		}
 	    	} else if( getField() != null ) {
 	    		SelectCriteria.standardFieldUpdate(getFieldInfo(), getField(), bean, value);
 	    	} else {
 	    		ReflectionUtil.setField( (AplosAbstractBean) bean, getName(), value);
 	    	}
	 	}
	}
	
	public CollectionInformation loadCollectionInformation( ProcessedBeanDao aqlBeanDao ) {
		AqlTable intermediateTable = null;
		if( getFieldInfo() instanceof CollectionFieldInfo ) {
			PersistentClass persistentClass = (PersistentClass) getAqlTable().getPersistentTable();
			setCollectionInformation(new CollectionInformation((CollectionFieldInfo) getFieldInfo()));
			JunctionTable junctionTable = ((CollectionFieldInfo) getFieldInfo()).getJunctionTable( persistentClass );
			intermediateTable = new AqlTable( aqlBeanDao, junctionTable, getAqlTable(), null, junctionTable.getPersistentClassFieldInfo() );
			aqlBeanDao.getBeanDao().setLatestTableIdx( intermediateTable.generateAlias( aqlBeanDao.getBeanDao().getLatestTableIdx() ) );
			aqlBeanDao.addQueryTable( intermediateTable.getAlias(), intermediateTable );
			intermediateTable.setJoinInferred( true );
			getCollectionInformation().setIntermediateTable(intermediateTable);
			
			MapKeyFieldInfo mapKeyFieldInfo = ((CollectionFieldInfo) getFieldInfo()).getMapKeyFieldInfo(); 
			if( mapKeyFieldInfo instanceof PersistentMapKeyFieldInfo ) {
				PersistentClass dbPersistentClass = ((PersistentMapKeyFieldInfo) mapKeyFieldInfo).getPersistentClass().getDbPersistentClass();
				AqlTable mapKeyTable = new AqlTable( aqlBeanDao, dbPersistentClass, intermediateTable, null, mapKeyFieldInfo );
				aqlBeanDao.getBeanDao().setLatestTableIdx( mapKeyTable.generateAlias( aqlBeanDao.getBeanDao().getLatestTableIdx() ) );
				aqlBeanDao.addQueryTable( mapKeyTable.getAlias(), mapKeyTable );
				mapKeyTable.setJoinInferred( true );
				getCollectionInformation().setMapKeyTable(mapKeyTable);
			}
		}

		if( getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
			if( ((PersistentCollectionFieldInfo) getFieldInfo()).isPolymorphic() ) {
				getCollectionInformation().setPolymorphic( true );
			} else {
				PersistentClass persistentClass = ((PersistentCollectionFieldInfo) getFieldInfo()).getPersistentClass();
				AqlTable collectionTable = new AqlTable( aqlBeanDao, persistentClass.getDbPersistentClass(), intermediateTable, null, getFieldInfo() );
				aqlBeanDao.getBeanDao().setLatestTableIdx( collectionTable.generateAlias( aqlBeanDao.getBeanDao().getLatestTableIdx() ) );
				aqlBeanDao.addQueryTable( collectionTable.getAlias(), collectionTable );
				collectionTable.setJoinInferred( true );
				getCollectionInformation().setCollectionTable(collectionTable);
			}
		}
		return getCollectionInformation();
	}
	
	@Override
	public boolean evaluateCriteriaTypes( ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables ) {
		if( getAqlTable() != null ) {
			getAqlTable().registerSelectCriteria( this );
		}
		return true;
	}
	
	@Override
	public ApplicationType getApplicationType() {
		if( getFieldInfo() != null ) {
			return getFieldInfo().getApplicationType();
		} else {
			return super.getApplicationType();
		}
	}
	
	public String getSqlPath( boolean includeGeneratedAliasForName ) {
		return getAqlTableAbstract().determineSqlVariablePath(true) + "." + getSqlName( includeGeneratedAliasForName );
	}
	
	public String getSubSqlPath( boolean includeGeneratedAliasForName ) {
		return getAqlTableAbstract().getParentTable().determineSqlVariablePath(true) + "." + getSqlName( includeGeneratedAliasForName );
	}
	
	public void createForeignKeySelectCriteria( ProcessedBeanDao processedBeanDao ) {
		setForeignKeySelectCriteria( new ForeignKeyAqlVariable( this ) );
		getForeignKeySelectCriteria().setAqlTable( (AqlTable) getAqlTable().getParentTable() );
		getForeignKeySelectCriteria().setName( getAqlTable().getParentFieldInfo().getSqlName() );
		getForeignKeySelectCriteria().updateFieldInfo(getAqlTable().getParentFieldInfo() );
	}
	
	public FieldInfo findFieldInfo() {
		FieldInfo fieldInfo = getAqlTableAbstract().getFieldInfoFromSqlName( getName(), false );
		AqlTable joinedTable = null;
		if( getAqlTableAbstract() instanceof AqlTable ) {
			joinedTable = getAqlTable().getJoinedTable();
		}
		while( fieldInfo == null && joinedTable != null ) {
			fieldInfo = joinedTable.getPersistentTable().getFieldInfoFromSqlName( getName(), false );
			if( fieldInfo != null ) {
				setAqlTable( joinedTable );
			} else {
				joinedTable = joinedTable.getJoinedTable();
			}
		}
		if( fieldInfo == null ) {
			if( getName().equals( getAqlTableAbstract().determineSqlVariablePath(true) ) ) {
				setName( getAqlTable().getAssociatedFieldInfo().getSqlName() );
				return getAqlTable().getAssociatedFieldInfo();
			}
		}
		return fieldInfo;
	}
    
	@Override
    public Object convertFieldValues( int rowDataIdx, RowDataReceiver rowDataReceiver ) throws SQLException {
    	Object value;
    	if( getFieldInfo() != null ) {
    		value = rowDataReceiver.getObject( rowDataIdx, getFieldInfo().getApplicationType() );
    	} else {
    		value = rowDataReceiver.getObject( rowDataIdx, null );
    	}
    	return SelectCriteria.convertFieldValue( value, getFieldInfo(), getField() );
    }
	
    @Override
	public Object getValue(AplosAbstractBean bean) {
		if( getAqlTable().getParentFieldInfo() != null ) {
			bean = (AplosAbstractBean) getAqlTable().getParentFieldInfo().getValue( bean );
		}
		if( getFieldInfo() != null ) {
    		return getFieldInfo().getValue( bean );
    	} else {
    		return null;
    	}
	}

    @Override
	public String getCriteria( BeanDao aqlBeanDao ) {
    	StringBuffer criteriaStrBuf = new StringBuffer();
    	if( isUsingForeignKey() || 
    			(getAqlTableAbstract() instanceof AqlTable && !getAqlTable().isIncludedInFromClause() && getForeignKeySelectCriteria() != null) ) {
    		criteriaStrBuf.append( getForeignKeySelectCriteria().getCriteria(aqlBeanDao) );
    	} else {
			StringBuffer criteria = new StringBuffer();
			if( getFieldInfo() instanceof PersistentClassFieldInfo ) {
				criteria.append( getAqlTable().getAlias() );
				criteria.append( "." ).append( getFieldInfo().getSqlName() );
			} else if( getCollectionInformation() != null ) {
				if( getCollectionInformation().getCollectionTable() != null ) {
					criteria.append( getCollectionInformation().getCollectionTable().getAlias() );
					criteria.append( ".id" );
				} else {
					criteria.append( getCollectionInformation().getIntermediateTable().getAlias() );
					criteria.append( "." ).append( getFieldInfo().getSqlName() );
				}
			} else {
				if( getAqlTableAbstract() != null ) {
					criteria.append( getAqlTableAbstract().getAlias() );
					criteria.append( "." );
				}
				criteria.append( getName() );
			}
			criteriaStrBuf.append( criteria.toString() );
    	}
    	return criteriaStrBuf.toString();
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}
	
	public void updateFieldInfo( FieldInfo fieldInfo ) {
		this.fieldInfo = fieldInfo;
		if( fieldInfo != null ) {
			setField( fieldInfo.getField() );
		} else {
			setField( null );
		}
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}
	
	public String toString() {
		return getSqlName(true);
	}

	public CollectionInformation getCollectionInformation() {
		return collectionInformation;
	}

	public void setCollectionInformation(CollectionInformation collectionInformation) {
		this.collectionInformation = collectionInformation;
	}

	public boolean isFullTable() {
		return isFullTable;
	}

	public void setFullTable(boolean isFullTable) {
		this.isFullTable = isFullTable;
	}

	public boolean isUsingForeignKey() {
		return isUsingForeignKey;
	}

	public void setUsingForeignKey(boolean isUsingForeignKey) {
		this.isUsingForeignKey = isUsingForeignKey;
	}

	public ForeignKeyAqlVariable getForeignKeySelectCriteria() {
		return foreignKeySelectCriteria;
	}

	public void setForeignKeySelectCriteria(ForeignKeyAqlVariable foreignKeySelectCriteria) {
		this.foreignKeySelectCriteria = foreignKeySelectCriteria;
	}

	public FieldInfo getFullTableFieldInfo() {
		return fullTableFieldInfo;
	}

	public void setFullTableFieldInfo(FieldInfo fullTableFieldInfo) {
		this.fullTableFieldInfo = fullTableFieldInfo;
	}
	
	public void setAqlTable( AqlTable aqlTable ) {
		setAqlTableAbstract(aqlTable);
	}
	
	public AqlTable getAqlTable() {
		return (AqlTable) getAqlTableAbstract();
	}
	
	@Override
	public AqlTableAbstract getAqlTableAbstract() {
		return aqlTable;
	}

	@Override
	public void setAqlTableAbstract(AqlTableAbstract aqlTableAbstract) {
		this.aqlTable = aqlTableAbstract;
	}
}
