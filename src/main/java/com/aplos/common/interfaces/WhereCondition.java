package com.aplos.common.interfaces;

import com.aplos.common.aql.ProcessedBeanDao;

public interface WhereCondition {
	public void appendCondition( StringBuffer strBuf );
	public void addNamedParameters( ProcessedBeanDao processedBeanDao );
	public boolean evaluateCriteriaTypes( ProcessedBeanDao processedBeanDao );
	public WhereCondition copy();
}
