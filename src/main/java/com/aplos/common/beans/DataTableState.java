package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosDataListModel;
import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Index;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.annotations.persistence.Transient;

@Entity
public class DataTableState extends AplosBean {
	private static final long serialVersionUID = -5701156538543335434L;

	@Transient
	private AplosLazyDataModel lazyDataModel;
	@Transient
	private AplosDataListModel dataListModel;

	private String rowsPerPageTemplate = "5,10,15,25,50,100,250,500";
	private int maxRowsOnPage = 10;
	private int firstRowIndex = 0;
	private String sortField;
	private String searchText;
	private boolean isUsingPagination = true;
	private boolean isPaginatorAlwaysVisible = true;
	private boolean isShowingEditPencilColumn = false;
	private boolean isShowingIdColumn = true;
	private boolean isShowingIdColumnFilter = true;
	private boolean isShowingDeleteColumn = true;
	private boolean isShowingDeleted = false;
	private boolean isShowingNewBtn = true;
	private boolean isShowingResetBtn = true;
	private boolean isShowingShowDeletedBtn = true;
	private boolean isShowingQuickView = false;
	private boolean isShowingSearch = true;
	private boolean isShowingRowHighlight = true;
	private boolean isUsingDisplayId = false;
	@Index(name="parentClass")
	private Class<?> parentClass;
	@CollectionOfElements
	private Map<String,String> columnFilters = new HashMap<String,String>();
	@CollectionOfElements
	private Map<String,String> pageFilters = new HashMap<String,String>();
	private SortOrder sortOrder;
	@OneToOne
	private SystemUser systemUser;
	private String tableIdentifier;
	@Transient
	private boolean isShowingHeader = true;
	@Transient
	private boolean isShowingFooter = true;
	@Transient
	private boolean isShowingSortBy = true;
	@Transient
	private boolean isShowingFilterBy = true;

	public List<String> getDynamicColumns() {
		ArrayList<String> dynamicColumns = new ArrayList<String>();
		dynamicColumns.add( "testing" );
		return dynamicColumns;
	}
	
	public void clear() {
		sortOrder = null;
		pageFilters = new HashMap<String,String>();
		sortField = null;
		searchText = null;
		maxRowsOnPage = 10;
		firstRowIndex = 0;
	}

//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialiseMap( getColumnFilters(), fullInitialisation );
//		HibernateUtil.initialiseMap( getPageFilters(), fullInitialisation );
//	}

	public void updateState( int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,String> filters ) {
		setFirstRowIndex(first);
		setMaxRowsOnPage(pageSize);
		setSortField(sortField);
		setSortOrder(sortOrder);
		setColumnFilters( filters );
		saveDetails();
	}

	public String getRowsPerPageTemplate() {
		return rowsPerPageTemplate;
	}
	public void setRowsPerPageTemplate(String rowsPerPageTemplate) {
		this.rowsPerPageTemplate = rowsPerPageTemplate;
	}
	public boolean isPaginatorAlwaysVisible() {
		return isPaginatorAlwaysVisible;
	}
	public void setPaginatorAlwaysVisible(boolean isPaginatorAlwaysVisible) {
		this.isPaginatorAlwaysVisible = isPaginatorAlwaysVisible;
	}
	public boolean isUsingPagination() {
		return isUsingPagination;
	}
	public void setUsingPagination(boolean isUsingPagination) {
		this.isUsingPagination = isUsingPagination;
	}
	public int getMaxRowsOnPage() {
		return maxRowsOnPage;
	}
	public void setMaxRowsOnPage(int maxRowsOnPage) {
		this.maxRowsOnPage = maxRowsOnPage;
	}
	public SystemUser getSystemUser() {
		return systemUser;
	}
	public void setSystemUser(SystemUser systemUser) {
		this.systemUser = systemUser;
	}
	public boolean isShowingIdColumn() {
		return isShowingIdColumn;
	}
	public void setShowingIdColumn(boolean isShowingIdColumn) {
		this.isShowingIdColumn = isShowingIdColumn;
	}
	public boolean isShowingDeleteColumn() {
		return isShowingDeleteColumn;
	}
	public void setShowingDeleteColumn(boolean isShowingDeleteColumn) {
		this.isShowingDeleteColumn = isShowingDeleteColumn;
	}
	public boolean isShowingEditPencilColumn() {
		return isShowingEditPencilColumn;
	}
	public void setShowingEditPencilColumn(boolean isShowingEditPencilColumn) {
		this.isShowingEditPencilColumn = isShowingEditPencilColumn;
	}

	public int getFirstRowIndex() {
		return firstRowIndex;
	}

	public void setFirstRowIndex(int firstRowIndex) {
		this.firstRowIndex = firstRowIndex;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public Map<String,String> getColumnFilters() {
		return columnFilters;
	}

	public void setColumnFilters(Map<String,String> columnFilters) {
		this.columnFilters = columnFilters;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setLazyDataModel(AplosLazyDataModel lazyDataModel) {
		this.lazyDataModel = lazyDataModel;
	}

	public AplosLazyDataModel getLazyDataModel() {
		return lazyDataModel;
	}

	public boolean isShowingDeleted() {
		return isShowingDeleted;
	}

	public void setShowingDeleted(boolean isShowingDeleted) {
		this.isShowingDeleted = isShowingDeleted;
	}

	public void toggleShowingDeleted() {
		setShowingDeleted(!isShowingDeleted());
	}

	public Class<?> getParentClass() {
		return parentClass;
	}

	public void setParentClass(Class<?> parentClass) {
		this.parentClass = parentClass;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getTableIdentifier() {
		return tableIdentifier;
	}

	public void setTableIdentifier(String tableIdentifier) {
		this.tableIdentifier = tableIdentifier;
	}

	public boolean isShowingNewBtn() {
		return isShowingNewBtn;
	}

	public void setShowingNewBtn(boolean isShowingNewBtn) {
		this.isShowingNewBtn = isShowingNewBtn;
	}

	public boolean isShowingQuickView() {
		return isShowingQuickView;
	}

	public void setShowingQuickView(boolean isShowingQuickView) {
		this.isShowingQuickView = isShowingQuickView;
	}

	public boolean isShowingIdColumnFilter() {
		return isShowingIdColumnFilter;
	}

	public void setShowingIdColumnFilter(boolean isShowingIdColumnFilter) {
		this.isShowingIdColumnFilter = isShowingIdColumnFilter;
	}

	public boolean isShowingSearch() {
		return isShowingSearch;
	}

	public void setShowingSearch(boolean isShowingSearch) {
		this.isShowingSearch = isShowingSearch;
	}

	public Map<String,String> getPageFilters() {
		return pageFilters;
	}

	public void setPageFilters(Map<String,String> pageFilters) {
		this.pageFilters = pageFilters;
	}

	public boolean isShowingShowDeletedBtn() {
		return isShowingShowDeletedBtn;
	}

	public void setShowingShowDeletedBtn(boolean isShowingShowDeletedBtn) {
		this.isShowingShowDeletedBtn = isShowingShowDeletedBtn;
	}

	public boolean isShowingRowHighlight() {
		return isShowingRowHighlight;
	}

	public void setShowingRowHighlight(boolean isShowingRowHighlight) {
		this.isShowingRowHighlight = isShowingRowHighlight;
	}

	public boolean isUsingDisplayId() {
		return isUsingDisplayId;
	}

	public void setUsingDisplayId(boolean isUsingDisplayId) {
		this.isUsingDisplayId = isUsingDisplayId;
	}

	public boolean isShowingHeader() {
		return isShowingHeader;
	}

	public void setShowingHeader(boolean isShowingHeader) {
		this.isShowingHeader = isShowingHeader;
	}

	public boolean isShowingFooter() {
		return isShowingFooter;
	}

	public void setShowingFooter(boolean isShowingFooter) {
		this.isShowingFooter = isShowingFooter;
	}

	public boolean isShowingSortBy() {
		return isShowingSortBy;
	}

	public void setShowingSortBy(boolean isShowingSortBy) {
		this.isShowingSortBy = isShowingSortBy;
	}

	public boolean isShowingFilterBy() {
		return isShowingFilterBy;
	}

	public void setShowingFilterBy(boolean isShowingFilterBy) {
		this.isShowingFilterBy = isShowingFilterBy;
	}

	public boolean isShowingResetBtn() {
		return isShowingResetBtn;
	}

	public void setShowingResetBtn(boolean isShowingResetBtn) {
		this.isShowingResetBtn = isShowingResetBtn;
	}

	public AplosDataListModel getDataListModel() {
		return dataListModel;
	}

	public void setDataListModel(AplosDataListModel dataListModel) {
		this.dataListModel = dataListModel;
	}

}
