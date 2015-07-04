package com.aplos.common.aql.aqltables.unprocessed;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.SubBeanDao;
import com.aplos.common.aql.aqltables.AqlSubTable;

public class UnprocessedAqlSubTable {
	private SubBeanDao subBeanDao;
	private BeanDao rootBeanDao;
	private String alias;

	public UnprocessedAqlSubTable( SubBeanDao subBeanDao, BeanDao rootBeanDao, String alias ) {
		setSubBeanDao(subBeanDao);
		setRootBeanDao(rootBeanDao);
	}
	
	public AqlSubTable createAndAddSubAqlTable(ProcessedBeanDao processedBeanDao) {
		AqlSubTable aqlSubTable = new AqlSubTable( processedBeanDao, getSubBeanDao(), getRootBeanDao(), getAlias() );
		return aqlSubTable;
	}

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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
}
