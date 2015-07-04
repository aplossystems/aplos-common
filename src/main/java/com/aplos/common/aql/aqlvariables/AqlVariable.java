package com.aplos.common.aql.aqlvariables;

import java.sql.SQLException;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.persistence.type.applicationtype.BigIntType;
import com.aplos.common.utils.CommonUtil;

public abstract class AqlVariable {
	private String name;
	private boolean isAddingParentheses = false;
	private SelectCriteria associatedSelectCriteria;
	/*
	 * isDistinct belongs on the AqlVariable because it may be part of a function i.e. count(distinct bean.id),
	 * otherwise it would have been placed on the SelectCriteria
	 */
	private boolean isDistinct = false;
    
    public void addNamedParameters( ProcessedBeanDao processedBeanDao ) {
		if( getName().startsWith( ":" ) ) {
			processedBeanDao.addNamedParameter( getName().substring( 1 ) );
		}
    }
	
	public String getSqlPath( boolean includeGeneratedAliasForName ) {
		return getSqlName( includeGeneratedAliasForName );
	}
	
	public String getSubSqlPath( boolean includeGeneratedAliasForName ) {
		return getSqlName( includeGeneratedAliasForName );
	}
	
	public String getOriginalText() {
		return "";
	}
	
	public Object getValue(AplosAbstractBean bean) {
		return null;
	}
	
	public String getSqlName( boolean includeGeneratedAliases ) {
		if( !CommonUtil.isNullOrEmpty( getName() ) && getName().startsWith( ":" ) ) {
			return "?";
		} else {
			if( isAddingParentheses() ) {
				StringBuffer strBuf = new StringBuffer( "(" );
				strBuf.append( getName() ).append( ")" );
				return strBuf.toString();
			} else {
				return getName();
			}
		}
	}
	
	public abstract void setField( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value ) throws Exception;
    
	public abstract boolean evaluateCriteriaTypes( ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables );
	
	public abstract String getCriteria( BeanDao beanDao );

    
    public Object convertFieldValues( int rowDataIdx, RowDataReceiver rowDataReceiver ) throws SQLException {
		return rowDataReceiver.getObject( rowDataIdx, null );
    }

    public ApplicationType getApplicationType() {
    	/*
    	 * TODO this is randomly set as not sure how it will be used after infrastructure change.
    	 */
    	return new BigIntType();
    }
	
	@Override
	public String toString() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isAddingParentheses() {
		return isAddingParentheses;
	}

	public void setAddingParentheses(boolean isAddingParentheses) {
		this.isAddingParentheses = isAddingParentheses;
	}

	public SelectCriteria getAssociatedSelectCriteria() {
		return associatedSelectCriteria;
	}

	public void setAssociatedSelectCriteria(SelectCriteria associatedSelectCriteria) {
		this.associatedSelectCriteria = associatedSelectCriteria;
	}

	public boolean isDistinct() {
		return isDistinct;
	}

	public void setDistinct(boolean isDistinct) {
		this.isDistinct = isDistinct;
	}
}
