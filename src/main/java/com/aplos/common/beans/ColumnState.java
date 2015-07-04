package com.aplos.common.beans;

import com.aplos.common.annotations.persistence.Entity;

@Entity
public class ColumnState extends AplosAbstractBean {
	private static final long serialVersionUID = -6638219122754352511L;

	private String columnEl;
	private String sortOrder;
	private String filterBy;

	public String getColumnEl() {
		return columnEl;
	}
	public void setColumnEl(String columnEl) {
		this.columnEl = columnEl;
	}
	public String getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
	public String getFilterBy() {
		return filterBy;
	}
	public void setFilterBy(String filterBy) {
		this.filterBy = filterBy;
	}
}
