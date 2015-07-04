package com.aplos.common;

import java.util.List;
import java.util.Map;

import org.primefaces.model.SortOrder;

import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Website;

public class SiteDaoLdm extends AplosLazyDataModel {

	private static final long serialVersionUID = 1555953324490660582L;

	public SiteDaoLdm(DataTableState dataTableState, SiteBeanDao siteBeanDao) {
		super( dataTableState, siteBeanDao );
	}

	@Override
	public List<Object> load(int first, int pageSize, String sortField,	SortOrder sortOrder, Map<String, String> filters) {
		if( Website.getCurrentWebsiteFromTabSession() != null ) {
			((SiteBeanDao) getBeanDao()).setWebsiteId( Website.getCurrentWebsiteFromTabSession().getId() );
		}
		return super.load(first, pageSize, sortField, sortOrder, filters);
	}
}
