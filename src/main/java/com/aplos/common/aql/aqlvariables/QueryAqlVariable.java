package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.SubBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.utils.ReflectionUtil;

public class QueryAqlVariable extends AqlVariable {
	private SubBeanDao subBeanDao;
	private ProcessedBeanDao processedBeanDao;
	
	public QueryAqlVariable( SubBeanDao aqlBeanDao ) {
		setSubBeanDao( aqlBeanDao );
	}
	
	@Override
	public String getCriteria(BeanDao aqlBeanDao) {
		return getSqlPath(true);
	}
	
//	@Override
//	public String getName() {
//		return getAlias();
//	}
	
	@Override
	public String getSqlPath(boolean includeGeneratedAliasForName) {
		StringBuffer strBuf = new StringBuffer( "(" );
		strBuf.append( getProcessedBeanDao().getSelectSql() );
		strBuf.append( ")" );
		return strBuf.toString();
	}
	
	@Override
	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean,
			Object value) throws Exception {	
	}
	
	@Override
	public ApplicationType getApplicationType() {
		return getProcessedBeanDao().getProcessedSelectCriteriaList().get( 0 ).getAqlVariable().getApplicationType();
	}
	
	@Override
	public void addNamedParameters(ProcessedBeanDao processedBeanDao) {
		// do nothing this will be handled by the getSqlPath method
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables ) {
		setProcessedBeanDao( new ProcessedBeanDao(getSubBeanDao(), processedBeanDao) );
		getProcessedBeanDao().setGeneratingBeans(false);
		getProcessedBeanDao().preprocessCriteria();
//		if( processedBeanDao.getBeanDao().getListBeanClass() != null ) {
//			setField( ReflectionUtil.getDeclaredField( processedBeanDao.getBeanDao().getListBeanClass(), getName() ) );
//		}
		return true;
	}

	public SubBeanDao getSubBeanDao() {
		return subBeanDao;
	}

	public void setSubBeanDao(SubBeanDao subBeanDao) {
		this.subBeanDao = subBeanDao;
	}

	public ProcessedBeanDao getProcessedBeanDao() {
		return processedBeanDao;
	}

	public void setProcessedBeanDao(ProcessedBeanDao processedBeanDao) {
		this.processedBeanDao = processedBeanDao;
	}
}
