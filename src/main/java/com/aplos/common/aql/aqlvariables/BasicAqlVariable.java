package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ReflectionUtil;

public class BasicAqlVariable extends AqlVariable {
	private boolean isAsteriskWildcard = false;
	
	public BasicAqlVariable() {
	}
	
	public BasicAqlVariable( String name ) {
		setName( name );
	}
	
	public String getCriteria( BeanDao beanDao ) {
		return getSqlName(true);
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao,
			boolean isAllowingFullTables) {
		return true;
	}
	
	@Override
	public String getOriginalText() {
		return getName();
	}
	
	@Override
	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean,
			Object value) throws Exception {
		ReflectionUtil.setField( (AplosAbstractBean) bean, getName(), value);
	}
	
	public String getSqlName( boolean includeGeneratedAliases ) {
		if( !CommonUtil.isNullOrEmpty( getName() ) && getName().startsWith( ":" ) ) {
			return "?";
		} else if( isAsteriskWildcard() ) {
			return "*";
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

	public boolean isAsteriskWildcard() {
		return isAsteriskWildcard;
	}

	public void setAsteriskWildcard(boolean isAsteriskWildcard) {
		this.isAsteriskWildcard = isAsteriskWildcard;
	}
}
