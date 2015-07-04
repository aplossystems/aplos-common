package com.aplos.common.aql.selectcriteria;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.persistence.RowDataReceiver;

public abstract class AbstractSelectCriteria {
	private String alias;
	private boolean isAliasGenerated = false;
	private boolean isAliasGenerationRestricted = false;
	private boolean isFieldInformationRequired = true;
	private int rowDataIdx;
	
	public AbstractSelectCriteria() {
	}
    
    public abstract Object convertFieldValues( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException;
    
    public abstract void addSelectCriterias( List<SelectCriteria> selectCriterias );
	
	public void generateAlias( ProcessedBeanDao processedBeanDao ) {
		setAlias( "aplos_col" + processedBeanDao.getBeanDao().getAndIncrementColumnCount() );
		setAliasGenerated(true);
	}
	
	public Field getField() {
		return null;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public Object convertFieldValue(Object value)  {
		return value;
	}

	public boolean isAliasGenerationRestricted() {
		return isAliasGenerationRestricted;
	}

	public void setAliasGenerationRestricted(boolean isAliasGenerationRestricted) {
		this.isAliasGenerationRestricted = isAliasGenerationRestricted;
	}

	public boolean isAliasGenerated() {
		return isAliasGenerated;
	}

	public void setAliasGenerated(boolean isAliasGenerated) {
		this.isAliasGenerated = isAliasGenerated;
	}

	public boolean isFieldInformationRequired() {
		return isFieldInformationRequired;
	}

	public void setFieldInformationRequired(boolean isFieldInformationRequired) {
		this.isFieldInformationRequired = isFieldInformationRequired;
	}

	public int getRowDataIdx() {
		return rowDataIdx;
	}

	public void setRowDataIdx(int rowDataIdx) {
		this.rowDataIdx = rowDataIdx;
	}

}
