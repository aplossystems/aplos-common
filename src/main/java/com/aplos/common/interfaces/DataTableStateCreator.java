package com.aplos.common.interfaces;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;

public interface DataTableStateCreator {
	public DataTableState getDefaultDataTableState( Class<?> parentClass );
	public AplosLazyDataModel getAplosLazyDataModel( DataTableState dataTableState, BeanDao aqlBeanDao );
}
