package com.aplos.common.aql.aqltables.unprocessed;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqltables.UnionAqlTable;

public class UnprocessedUnionAqlTable extends UnprocessedAqlTable {
	private List<BeanDao> beanDaoList = new ArrayList<BeanDao>();

	@Override
	public AqlTableAbstract createAqlTable( ProcessedBeanDao processedBeanDao, boolean isAddingTable ) {
		UnionAqlTable unionAqlTable = new UnionAqlTable(processedBeanDao);
		for( int i = 0, n = getBeanDaoList().size() ; i < n; i++ ) {
			unionAqlTable.getProcessedBeanDaoList().add( getBeanDaoList().get( i ).createProcessedBeanDao() );
		}
		if( isAddingTable ) {
			processedBeanDao.addQueryTable( unionAqlTable.getAlias(), unionAqlTable );
		}
		return unionAqlTable;
	}

	public List<BeanDao> getBeanDaoList() {
		return beanDaoList;
	}

	public void setBeanDaoList(List<BeanDao> beanDaoList) {
		this.beanDaoList = beanDaoList;
	}
}
