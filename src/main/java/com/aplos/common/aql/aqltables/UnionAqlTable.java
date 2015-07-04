package com.aplos.common.aql.aqltables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.utils.ApplicationUtil;

public class UnionAqlTable extends AqlTableAbstract {
	private List<ProcessedBeanDao> processedBeanDaoList = new ArrayList<ProcessedBeanDao>();
	private PersistableTable persistableTable;
	
	public UnionAqlTable( ProcessedBeanDao processedBeanDao ) {
		setProcessedBeanDao(processedBeanDao);
		PersistableTable persistableTable = ApplicationUtil.getPersistentClass(processedBeanDao.getBeanDao().getBeanClass());
		setPersistableTable(persistableTable);
	}

	@Override
	public String getSqlString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( "(" );
		boolean firstAdded = false;
		for( ProcessedBeanDao tempProcessedBeanDao : getProcessedBeanDaoList() ) {
			if( firstAdded ) {
				strBuf.append( " UNION " );
			} else {
				firstAdded = true;
			}
			strBuf.append( "(" );
			strBuf.append( tempProcessedBeanDao.getSelectSql() );
			strBuf.append( ")" );
		}
		strBuf.append( ") as " );
		strBuf.append( getAlias() );
		return strBuf.toString();
	}
	
	@Override
	public boolean isVariableInScope( String variableName ) {
		FieldInfo fieldInfo = getPersistableTable().getAliasedFieldInfoMap().get( variableName );
		
		if( fieldInfo != null ) {
			return true;
		} else if( getProcessedBeanDao().getBeanDao().getListBeanClass() != null ) {
			try {
				return getProcessedBeanDao().getBeanDao().getListBeanClass().getDeclaredField( variableName ) != null;
			} catch( NoSuchFieldException nsfEx ) {
				ApplicationUtil.handleError( nsfEx );
			}
		}
		return false;
	}
	
	@Override
	public int generateAlias(int latestTableIdx) {
		String prefix = "unionTable";
		setAlias( prefix + latestTableIdx );
		setAliasGenerated( true );
		return ++latestTableIdx;
	}
	
	@Override
	public FieldInfo getFieldInfoFromSqlName(String sqlName,
			boolean searchAllClasses) {
		return null;
	}

	public List<ProcessedBeanDao> getProcessedBeanDaoList() {
		return processedBeanDaoList;
	}

	public void setProcessedBeanDaoList(List<ProcessedBeanDao> processedBeanDaoList) {
		this.processedBeanDaoList = processedBeanDaoList;
	}

	public PersistableTable getPersistableTable() {
		return persistableTable;
	}

	public void setPersistableTable(PersistableTable persistableTable) {
		this.persistableTable = persistableTable;
	}
}
