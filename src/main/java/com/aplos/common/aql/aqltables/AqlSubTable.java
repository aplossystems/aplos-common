package com.aplos.common.aql.aqltables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.SubBeanDao;
import com.aplos.common.persistence.fieldinfo.FieldInfo;


public class AqlSubTable extends AqlTableAbstract {
	private SubBeanDao subBeanDao;
	private BeanDao rootBeanDao;

	public AqlSubTable( ProcessedBeanDao parentProcessedBeanDao, SubBeanDao subBeanDao, BeanDao rootBeanDao, String alias ) {
		super(alias);
		setSubBeanDao(subBeanDao);
		subBeanDao.setParentTable(this);
		setRootBeanDao(rootBeanDao);
		setProcessedBeanDao( new ProcessedBeanDao( getSubBeanDao(), parentProcessedBeanDao ) );
		getProcessedBeanDao().setGeneratingBeans(false);
	}
	
//	@Override
//	public void evaluateInheritance() {
//		getProcessedBeanDao().processInheritanceTables();
//	}
	
	@Override
	public String getSqlString() {
		StringBuffer sqlBuf = new StringBuffer( "(" );
		sqlBuf.append( getProcessedBeanDao().getSelectSql() );
		sqlBuf.append( ") AS " ).append( getAlias() );
		return sqlBuf.toString();
	}
	
	@Override
	public FieldInfo getFieldInfoFromSqlName(String sqlName,
			boolean searchAllClasses) {
		return null;
	}
	
	@Override
	public int generateAlias( int latestTableIdx ) {
		String prefix = "subTable";
		setAlias( prefix + latestTableIdx );
		setAliasGenerated( true );
		return ++latestTableIdx;
	}
	
	public boolean isVariableInScope( String variableName ) {
		return false;
	}
	
//	@Override
//	public Class<? extends AplosAbstractBean> getBeanClass() {
//		return aqlBeanDao.getBeanClass();
//	}
//	
//	@Override
//	public void setBeanClass(Class<? extends AplosAbstractBean> beanClass) {
//		if( aqlBeanDao != null && beanClass != null ) {
//			aqlBeanDao.setBeanClass( beanClass );
//		}
//	}

	public SubBeanDao getSubBeanDao() {
		return subBeanDao;
	}

	public void setSubBeanDao(SubBeanDao subBeanDao) {
		this.subBeanDao = subBeanDao;
	}

	public BeanDao getRootBeanDao() {
		return rootBeanDao;
	}

	public void setRootBeanDao(BeanDao rootBeanDao) {
		this.rootBeanDao = rootBeanDao;
	}

}
