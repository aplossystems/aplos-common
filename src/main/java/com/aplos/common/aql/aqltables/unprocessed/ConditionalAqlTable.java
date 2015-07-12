package com.aplos.common.aql.aqltables.unprocessed;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.antlr.AqlParser;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.utils.ApplicationUtil;

public class ConditionalAqlTable extends UnprocessedAqlTable {
	private String leftCondition = "";
	private String rightCondition = "";
	private PersistableTable persistableTable;
	
	public ConditionalAqlTable( String alias, PersistableTable persistableTable, String leftCondition, String rightCondition ) {
		setAlias( alias );
		setPersistableTable( persistableTable );
		setLeftCondition( leftCondition );
		setRightCondition( rightCondition );
	}

	@Override
	public AqlTable createAqlTable( ProcessedBeanDao processedBeanDao, boolean isAddingTable ) {
		try {
			AqlParser aqlParser = processedBeanDao.getBeanDao().getAqlParser().updateString( leftCondition );
			UnevaluatedTableVariable leftCriteria = (UnevaluatedTableVariable) aqlParser.parseAqlVariable( processedBeanDao.getBeanDao(), null );
			aqlParser.updateString( rightCondition );
			UnevaluatedTableVariable rightCriteria = (UnevaluatedTableVariable) aqlParser.parseAqlVariable( processedBeanDao.getBeanDao(), null );

			AqlTable parentAqlTable;
			boolean isLeftConditionNewTable = false;
			leftCriteria.init(leftCriteria.getOriginalPath());
			rightCriteria.init(rightCriteria.getOriginalPath());
			if( leftCriteria.getTablePath().equals( getAlias() ) ) {
				isLeftConditionNewTable = true;
				parentAqlTable = (AqlTable) processedBeanDao.getQueryTables().get(rightCriteria.getTablePath());
			} else {
				parentAqlTable = (AqlTable) processedBeanDao.getQueryTables().get(leftCriteria.getTablePath());
			}
			
			if( parentAqlTable == null ) {
				return null;
			} else {
				if( isLeftConditionNewTable ) {
					rightCriteria.evaluateCriteriaTypes(processedBeanDao, false);
				} else {
					leftCriteria.evaluateCriteriaTypes(processedBeanDao, false);
				}
				AqlTable aqlTable = new AqlTable( processedBeanDao, getPersistableTable(), parentAqlTable, getAlias() );
				if( isAddingTable ) {
					processedBeanDao.addQueryTable( aqlTable.getAlias(), aqlTable );
				}
				if( isLeftConditionNewTable ) {
					leftCriteria.evaluateCriteriaTypes(processedBeanDao, false);
				} else {
					rightCriteria.evaluateCriteriaTypes(processedBeanDao, false);
				}
				if( isLeftConditionNewTable ) {
					aqlTable.setInternalVariable( leftCriteria );
					aqlTable.setExternalVariable( rightCriteria );
				} else  {
					aqlTable.setInternalVariable( rightCriteria );
					aqlTable.setExternalVariable( leftCriteria );
				}
				aqlTable.setJoinType( getJoinType() );
				return aqlTable;
			}
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError(tsEx);
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError(rEx);
		}
		return null;
	}

	public String getLeftCondition() {
		return leftCondition;
	}

	public void setLeftCondition(String leftCondition) {
		this.leftCondition = leftCondition;
	}

	public String getRightCondition() {
		return rightCondition;
	}

	public void setRightCondition(String rightCondition) {
		this.rightCondition = rightCondition;
	}

	public PersistableTable getPersistableTable() {
		return persistableTable;
	}

	public void setPersistableTable(PersistableTable persistableTable) {
		this.persistableTable = persistableTable;
	}
}
