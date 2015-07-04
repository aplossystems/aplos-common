package com.aplos.common.aql.aqlvariables;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.persistence.type.applicationtype.BigIntType;

public class FunctionAqlVariable extends AqlVariable {
	private List<AqlVariable> unprocessedAqlVariableList = new ArrayList<AqlVariable>();
	private List<AqlVariable> processedAqlVariableList = new ArrayList<AqlVariable>();
	

	public FunctionAqlVariable() {}
    
    public void addNamedParameters( ProcessedBeanDao processedBeanDao ) {
    	for( AqlVariable tempAqlVariable : getProcessedAqlVariableList() ) {
			tempAqlVariable.addNamedParameters( processedBeanDao );
    	}
    }
    
    @Override
    public ApplicationType getApplicationType() {
    	/*
    	 * This is just because most functions COUNT, MAX etc. will return a long,
    	 * however other cases will apply.
    	 */
    	return new BigIntType();
    }
    
    @Override
    public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean,
    		Object value) throws Exception {	
    }
	
	public boolean evaluateCriteriaTypes( ProcessedBeanDao aqlBeanDao, boolean isAllowingFullTables ) {
		List<AqlVariable> tempCriteriaList = new ArrayList<AqlVariable>();
		
		for( int i = 0, n = getUnprocessedAqlVariableList().size(); i < n; i++ ) { 
			if( !getUnprocessedAqlVariableList().get( i ).evaluateCriteriaTypes( aqlBeanDao, false ) ) {
				return false;
			}
			AqlVariable evaluatedVariable = getUnprocessedAqlVariableList().get( i );
			
			if( getUnprocessedAqlVariableList().get( i ) instanceof AqlTableVariable ) {
				AqlTableVariable aqlTableVariable = (AqlTableVariable) getUnprocessedAqlVariableList().get( i ); 
				if( aqlTableVariable.getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
					PersistentClass persistentClass = (PersistentClass) aqlTableVariable.getAqlTable().getPersistentTable();
					JunctionTable collectionTable = ((PersistentCollectionFieldInfo) aqlTableVariable.getFieldInfo()).getJunctionTable( persistentClass.getTableClass() );
	
					aqlBeanDao.findOrCreateAqlTable(collectionTable.getSqlTableName(), false);
				}
			}
			tempCriteriaList.add( getUnprocessedAqlVariableList().get( i ) );
		}

		setProcessedAqlVariableList(tempCriteriaList);
		return true;
	}

	@Override
	public String getCriteria( BeanDao aqlBeanDao ) {
		StringBuffer sqlNameBuf = new StringBuffer();
		sqlNameBuf.append( getName() ).append( "(" );
		for( int i = 0, n = processedAqlVariableList.size(); i < n; i++ ) {
			if( processedAqlVariableList.get( i ).isDistinct() ) {
				sqlNameBuf.append( "DISTINCT " );
	    	}
			sqlNameBuf.append( processedAqlVariableList.get( i ).getCriteria(aqlBeanDao) );
			if( i < (n-1) ) {
				sqlNameBuf.append(",");
			}
		}
		sqlNameBuf.append( ")" );
		return sqlNameBuf.toString();
	}

	@Override
	public String getOriginalText() {
		StringBuffer sqlNameBuf = new StringBuffer();
		sqlNameBuf.append( getName() ).append( "(" );
		for( int i = 0, n = unprocessedAqlVariableList.size(); i < n; i++ ) {
			if( unprocessedAqlVariableList.get( i ).isDistinct() ) {
				sqlNameBuf.append( "DISTINCT " );
	    	}
			sqlNameBuf.append( unprocessedAqlVariableList.get( i ).getOriginalText() );
			if( i < (n-1) ) {
				sqlNameBuf.append(",");
			}
		}
		sqlNameBuf.append( ")" );
		return sqlNameBuf.toString();
	}

	public List<AqlVariable> getUnprocessedAqlVariableList() {
		return unprocessedAqlVariableList;
	}

	public void setUnprocessedAqlVariableList(
			List<AqlVariable> unprocessedAqlVariableList) {
		this.unprocessedAqlVariableList = unprocessedAqlVariableList;
	}

	public List<AqlVariable> getProcessedAqlVariableList() {
		return processedAqlVariableList;
	}

	public void setProcessedAqlVariableList(
			List<AqlVariable> processedAqlVariableList) {
		this.processedAqlVariableList = processedAqlVariableList;
	}
	
	@Override
	public String getSqlName(boolean includeGeneratedAliases) {
		StringBuffer sqlNameBuf = new StringBuffer();
		sqlNameBuf.append( getName() ).append( "(" );
		for( int i = 0, n = processedAqlVariableList.size(); i < n; i++ ) {
			sqlNameBuf.append( processedAqlVariableList.get( i ).getSqlPath(true) );
			if( i < (n-1) ) {
				sqlNameBuf.append(",");
			}
		}
		sqlNameBuf.append( ")" );
		return sqlNameBuf.toString();
	}
}
