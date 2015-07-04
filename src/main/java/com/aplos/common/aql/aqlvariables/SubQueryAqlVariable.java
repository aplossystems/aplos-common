package com.aplos.common.aql.aqlvariables;

import java.lang.reflect.Field;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlSubTable;
import com.aplos.common.aql.aqltables.unprocessed.UnprocessedAqlSubTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

public class SubQueryAqlVariable extends AqlTableVariable {
	private AqlSubTable aqlSubTable;
	private UnprocessedAqlSubTable unprocessedAqlSubTable;
	
	public SubQueryAqlVariable( UnprocessedAqlSubTable unprocessedAqlSubTable ) {
		setUnprocessedAqlSubTable(unprocessedAqlSubTable);
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables) {
		setAqlSubTable(getUnprocessedAqlSubTable().createAndAddSubAqlTable(processedBeanDao));
		try {
			Field field = null;
			if( getAqlSubTable().getRootBeanDao().getListBeanClass() != null ) {
				field = getAqlSubTable().getRootBeanDao().getListBeanClass().getDeclaredField( getAqlSubTable().getAlias() );
			} else {
				PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getAqlSubTable().getRootBeanDao().getBeanClass() );
				field = persistentClass.getAliasedFieldInfoMap().get( getAqlSubTable().getAlias() ).getField();
			}
			setField( field );
		} catch( NoSuchFieldException nsfEx ) {
			ApplicationUtil.handleError( nsfEx );
		}
		aqlSubTable.getProcessedBeanDao().setGeneratingBeans(false);
		aqlSubTable.getProcessedBeanDao().preprocessCriteria();
		return true;
	}

	@Override
	public String getCriteria(BeanDao aqlBeanDao) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( " " ).append( getAqlSubTable().getSqlString() );
		return strBuf.toString();
	}

	public AqlSubTable getAqlSubTable() {
		return aqlSubTable;
	}

	public void setAqlSubTable(AqlSubTable aqlSubTable) {
		this.aqlSubTable = aqlSubTable;
	}

	public UnprocessedAqlSubTable getUnprocessedAqlSubTable() {
		return unprocessedAqlSubTable;
	}

	public void setUnprocessedAqlSubTable(UnprocessedAqlSubTable unprocessedAqlSubTable) {
		this.unprocessedAqlSubTable = unprocessedAqlSubTable;
	}
}
