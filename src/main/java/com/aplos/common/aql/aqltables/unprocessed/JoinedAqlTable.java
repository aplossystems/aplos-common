package com.aplos.common.aql.aqltables.unprocessed;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;

public class JoinedAqlTable extends UnprocessedAqlTable {
	private String joinPath = "";
	private String parentAlias;
	private AqlTableVariable aqlTableVariable;
	
	public JoinedAqlTable( String alias, AqlTableVariable variableSelectCriteria ) {
		setAlias( alias );
		setAqlTableVariable( variableSelectCriteria );
	}
	
	public JoinedAqlTable( String alias, String joinPath ) {
		setAlias( alias );
		setJoinPath( joinPath );
	}

	@Override
	public AqlTable createAqlTable( ProcessedBeanDao processedBeanDao, boolean isAddingTable ) {
		if( getAqlTableVariable() == null ) {
			setAqlTableVariable( new UnevaluatedTableVariable( getJoinPath() ) );
		}
		if( !getAqlTableVariable().evaluateCriteriaTypes(processedBeanDao, false) ) {
			return null;
		}
		PersistableTable persistableTable = null;

		AqlTable parentAqlTable = getAqlTableVariable().getAqlTable();
		if( getAqlTableVariable().getFieldInfo() instanceof PersistentClassFieldInfo ) {
			persistableTable = ((PersistentClassFieldInfo) getAqlTableVariable().getFieldInfo()).getPersistentClass();
		} else if( getAqlTableVariable().getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
			JunctionTable junctionTable = ((PersistentCollectionFieldInfo) getAqlTableVariable().getFieldInfo()).getJunctionTable(getAqlTableVariable().getFieldInfo().getParentPersistentClass().getTableClass());
			parentAqlTable = new AqlTable( processedBeanDao, junctionTable, parentAqlTable, null, junctionTable.getPersistentClassFieldInfo(), true );
			if( isAddingTable ) {
				processedBeanDao.addQueryTable( parentAqlTable.getAlias(), parentAqlTable );
			}
			parentAqlTable.setJoinInferred( true );
			persistableTable = ((PersistentCollectionFieldInfo) getAqlTableVariable().getFieldInfo()).getPersistentClass();
		} else {
			PersistentClass dbPersistentClass = ((CollectionFieldInfo) getAqlTableVariable().getFieldInfo()).getParentPersistentClass().getDbPersistentClass();
			persistableTable = ((CollectionFieldInfo) getAqlTableVariable().getFieldInfo()).getJunctionTable( dbPersistentClass );
		}
		String binding = getJoinPath();
		if( getAlias() != null ) {
			binding = getAlias();
		}
		
		AqlTable aqlTable = (AqlTable) processedBeanDao.getQueryTables().get( binding ); 
		if( aqlTable == null ) {
			aqlTable = new AqlTable( processedBeanDao, persistableTable, parentAqlTable, getAlias(), getAqlTableVariable().getFieldInfo() );
			aqlTable.setParentFieldInfo(getAqlTableVariable().getFieldInfo());
			aqlTable.setJoinType( getJoinType() );
			
			if( isAddingTable ) {
				if( getAlias() != null ) {
					processedBeanDao.addQueryTable( aqlTable.getAlias(), aqlTable );
				} else {
					processedBeanDao.addQueryTable( getJoinPath(), aqlTable );
				}
			}
		}
		return aqlTable;
	}
	
	public String getJoinPath() {
		return joinPath;
	}
	public void setJoinPath(String joinPath) {
		this.joinPath = joinPath;
	}
	public String getParentAlias() {
		return parentAlias;
	}
	public void setParentAlias(String parentAlias) {
		this.parentAlias = parentAlias;
	}

	public AqlTableVariable getAqlTableVariable() {
		return aqlTableVariable;
	}

	public void setAqlTableVariable(AqlTableVariable aqlTableVariable) {
		this.aqlTableVariable = aqlTableVariable;
	}
}
