package com.aplos.common.aql.aqltables.unprocessed;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTable.JoinType;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

public class CommaAqlTable extends UnprocessedAqlTable {
	private PersistableTable persistableTable;
	private String parentAlias;
	
	public CommaAqlTable( String alias, Class<? extends AplosAbstractBean> beanClass, String parentAlias ) {
		setAlias( alias );
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get(beanClass);
		setPersistableTable( persistentClass );
		setParentAlias(parentAlias);
	}

	@Override
	public AqlTableAbstract createAqlTable( ProcessedBeanDao processedBeanDao, boolean isAddingTable ) {
		AqlTableAbstract parentAqlTable = processedBeanDao.getQueryTables().get(getParentAlias());
		if( parentAqlTable == null ) {
			return null;
		}
		AqlTable aqlTable = new AqlTable( processedBeanDao, getPersistableTable(), parentAqlTable, getAlias() );
		aqlTable.setJoinType( JoinType.COMMA_JOIN );
		if( isAddingTable ) {
			processedBeanDao.addQueryTable( aqlTable.getAlias(), aqlTable );
		}
		return aqlTable;
	}

	public PersistableTable getPersistableTable() {
		return persistableTable;
	}

	public void setPersistableTable(PersistableTable persistableTable) {
		this.persistableTable = persistableTable;
	}

	public String getParentAlias() {
		return parentAlias;
	}

	public void setParentAlias(String parentAlias) {
		this.parentAlias = parentAlias;
	}
}
