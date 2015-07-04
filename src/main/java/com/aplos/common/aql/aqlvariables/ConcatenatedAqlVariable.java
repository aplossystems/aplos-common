package com.aplos.common.aql.aqlvariables;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;

public class ConcatenatedAqlVariable extends FunctionAqlVariable {
	private List<String> joinOperators = new ArrayList<String>();
	

	public ConcatenatedAqlVariable() {}
    
    public void addNamedParameters( ProcessedBeanDao processedBeanDao ) {
    	for( AqlVariable tempAqlVariable : getProcessedAqlVariableList() ) {
			tempAqlVariable.addNamedParameters( processedBeanDao );
    	}
    }
    
    public void addAqlVariable( AqlVariable aqlVariable, String joinOperator ) {
    	getUnprocessedAqlVariableList().add( aqlVariable );
    	getJoinOperators().add( joinOperator );
    }

	@Override
	public String getCriteria( BeanDao aqlBeanDao ) {
		return getSqlName(true);
	}
	
	@Override
	public String getSqlName(boolean includeGeneratedAliases) {
		StringBuffer sqlNameBuf = new StringBuffer();
		if( isAddingParentheses() ) {
			sqlNameBuf.append( "(" );
		}
		for( int i = 0, n = getProcessedAqlVariableList().size(); i < n; i++ ) {
			sqlNameBuf.append( getProcessedAqlVariableList().get( i ).getSqlPath(true) );
			if( i < (n-1) ) {
				sqlNameBuf.append( getJoinOperators().get( i ) );
			}
		}
		if( isAddingParentheses() ) {
			sqlNameBuf.append( ")" );
		}
		return sqlNameBuf.toString();
	}

	public List<String> getJoinOperators() {
		return joinOperators;
	}

	public void setJoinOperators(List<String> joinOperators) {
		this.joinOperators = joinOperators;
	}
}
