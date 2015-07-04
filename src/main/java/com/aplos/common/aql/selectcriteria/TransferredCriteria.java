package com.aplos.common.aql.selectcriteria;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.CommonUtil;

public class TransferredCriteria extends SelectCriteria {
	private SelectCriteria selectCriteria;
	private AqlTableAbstract sourceAqlTable;

	public TransferredCriteria() {
	}

	public TransferredCriteria( ProcessedBeanDao aqlBeanDao, SelectCriteria selectCriteria, AqlTableAbstract sourceAqlTable ) {
		setSelectCriteria(selectCriteria);
		setSourceAqlTable(sourceAqlTable);
		if( CommonUtil.isNullOrEmpty( getSelectCriteria().getAlias() ) ) {
			getSelectCriteria().generateAlias( aqlBeanDao );
		}
	}
	
	@Override
	public AqlVariable getAqlVariable() {
		return getSelectCriteria().getAqlVariable();
	}
	
	@Override
	public String getSqlText(BeanDao beanDao) {
		return getSourceAqlTable().getAlias() + "." + getSelectCriteria().getAlias();
	}

	public SelectCriteria getSelectCriteria() {
		return selectCriteria;
	}

	public void setSelectCriteria(SelectCriteria selectCriteria) {
		this.selectCriteria = selectCriteria;
	}

	public AqlTableAbstract getSourceAqlTable() {
		return sourceAqlTable;
	}

	public void setSourceAqlTable(AqlTableAbstract sourceAqlTable) {
		this.sourceAqlTable = sourceAqlTable;
	}
}
