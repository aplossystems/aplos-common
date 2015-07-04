package com.aplos.common;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ConversionUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.ReflectionUtil;

public class AplosLazyDataModel extends LazyDataModel<Object> {
	private static final long serialVersionUID = 4748011470259543647L;

	private BeanDao aqlBeanDao;
	private boolean isUsingBeanDaoClass = true;
	private DataTableState dataTableState;
	private Class<? extends BackingPage> editPageClass;
	protected AplosBean deleteBean;
	private boolean isHardDelete = false;
	private boolean isHardDeleteRequested = false;
	private List<Object> recordArray;
	private Object[] selectedRows; //for use with multiSelect=true on wrapped table
	private Object selectedRow; //for use with single select on wrapped table
	private String searchCriteria = null;
	private List<Field> stateFields = new ArrayList();

	public AplosLazyDataModel( DataTableState dataTableState, BeanDao beanDao ) {
		this(dataTableState, beanDao, true);
	}
	
	public AplosLazyDataModel( DataTableState dataTableState, BeanDao beanDao, boolean calculateRowCount ) {
		setBeanDao( beanDao );
		setDataTableState( dataTableState );
		if( dataTableState.getLazyDataModel() == null ) {
			dataTableState.setLazyDataModel( this );
		}
		if( calculateRowCount ) {
			setRowCount( getCountAll() );
		}
		
		try {
			addStateFields();
		} catch( NoSuchFieldException nsfEx ) {
			ApplicationUtil.handleError(nsfEx);
		}
	}
	
	public String getArchiveStyleClass() {
		AplosBean aplosBean = (AplosBean) getAplosBean();
		if( aplosBean != null && aplosBean.isArchived() ) {
			return "archived-column";
		} else {
			return null;
		}
	}

    @Override
    public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,String> filters) {
    	((BeanDao) getBeanDao()).clearNamedParameters();
    	getBeanDao().clearSearchCriteria();

    	if( sortField != null ) {
    		String sortOrderHql = "ASC";
    		if ( SortOrder.DESCENDING.equals( sortOrder ) ) {
    			sortOrderHql = "DESC";
    		}
    		sortField = sortField.trim();
    		if( sortField.startsWith(("'")) ) {
    			sortField = sortField.substring( 1, sortField.length() - 1 );
    		} else {
    			sortField = sortField.replace( "tableBean", "bean" );
    		}
    		
    		getBeanDao().setOrderBy( sortField + " " + sortOrderHql );
    		overrideOrderBy();
    	} else {
    		getBeanDao().setOrderByCriteria( null );
    	}

		if ( getSearchCriteria() != null && getDataTableState().getSearchText() != null && !getDataTableState().getSearchText().equals( "" ) ) {
			getBeanDao().setSearchCriteria( "( " + getSearchCriteria() + ")" );
		}

    	getBeanDao().addFilters(filters);

    	getBeanDao().setIsReturningActiveBeans( !getDataTableState().isShowingDeleted() );

		addSearchParameters(getBeanDao(), filters);

		setRowCount( getBeanDao().getCountAll() );

		if ( getRowCount() < first ) {
			first = 0;
		}

		getBeanDao().setFirstRowIdx(first);
		getBeanDao().setMaxResults(pageSize);
		setRecordArray( new ArrayList<Object>( getBeanDao().getAll() ) );

		saveStateFieldInformation();
		getDataTableState().updateState( first, pageSize, sortField, sortOrder, filters );

		return getRecordArray();
    }
    
    public List getFilteredRealBeans() {
    	return getRealBeans( getFilteredBeans() );
    }
    
    public List getFilteredBeans() {
    	int firstRowIdx = getBeanDao().getFirstRowIdx();
    	int maxResults = getBeanDao().getMaxResults();
    	getBeanDao().setFirstRowIdx( 0 );
    	getBeanDao().setMaxResults( -1 );
    	List beanList = getBeanDao().getAll();
    	getBeanDao().setFirstRowIdx( firstRowIdx );
    	getBeanDao().setMaxResults( maxResults );
    	return beanList;
    }
    
    public List getRealBeans( List beanList ) {
    	if( beanList.size() > 0 && getBeanDao().getListBeanClass() != null &&
    			AplosAbstractBean.class.isAssignableFrom( getBeanDao().getListBeanClass() ) ) {
    		List<AplosAbstractBean> listBeanList = beanList;
    		String joinedIds = CommonUtil.joinIds( listBeanList );
    		BeanDao realBeanDao = new BeanDao(getBeanDao().getBeanClass());
    		realBeanDao.addWhereCriteria( "bean.id IN (" + joinedIds + ")" );
    		return realBeanDao.getAll();
    	} else {
    		return beanList;
    	}
    }
    
    public List getSelectedRealBeans() {
    	if( getSelectedRows() == null ) {
    		return new ArrayList();
    	} else {
    		return getRealBeans( Arrays.asList( getSelectedRows() ) );
    	}
    }
    
    @Override
    public void setRowIndex(int rowIndex) {
    	/*
    	 * Horrible hack to cover primefaces bad coding not allowing  
    	 * expected overrides but exposing a divison by zero issue.
    	 */
    	if( rowIndex != -1 && getPageSize() == 0 ) {
    		setPageSize( getDataTableState().getMaxRowsOnPage() );
    	}
    	super.setRowIndex(rowIndex);
    }
    
    public void overrideOrderBy() {}

	public void addSearchParameters( BeanDao beanDao, Map<String,String> filters ) {
		String searchText = getDataTableState().getSearchText();
		beanDao.setNamedParameter("similarSearchText", "%" + searchText + "%");
		beanDao.setNamedParameter("exactSearchText", searchText );
		beanDao.setNamedParameter("startsWithSearchText", searchText + "%");
		beanDao.setNamedParameter("endsWithSearchText", "%" + searchText );
		
    	if ( filters != null && filters.size() > 0 ) {
    		Iterator<String> filterKeys = filters.keySet().iterator();
    		String filterKey;
    		String processedFilterKey;
    		while( filterKeys.hasNext() ) {
    			filterKey = filterKeys.next();
    			processedFilterKey = filterKey.trim();
    			if( processedFilterKey.indexOf( ":" ) == -1 ) {
    				processedFilterKey = processedFilterKey.replace( "tableBean", "bean" ).replace( ".", "_" );
    			} else {
    				processedFilterKey = processedFilterKey.substring( 0, processedFilterKey.indexOf( ":" ) );
    			}
    			beanDao.setNamedParameter( processedFilterKey, "%" + filters.get( filterKey ).toString() + "%" );
    		}
		}
	}
	
	/*
   	* imported from AqlAplosLazyDataModel
    */
	
	public void saveStateFieldInformation() {
		getDataTableState().getPageFilters().clear();
		for( int i = 0, n = getStateFields().size(); i < n; i++ ) {
//			Object property = PropertyUtils.getProperty( this, getStateFields().get( i ).getName() );
			Class<?> fieldType = getStateFields().get( i ).getType();
			String fieldName = getStateFields().get( i ).getName();
			
			try {
				if( Set.class.isAssignableFrom( fieldType ) 
						|| List.class.isAssignableFrom( fieldType ) ) {
					Collection set = (Collection) ReflectionUtil.getFieldValue( this, fieldName );
					Iterator iter = set.iterator();
					int count = 0;
					while( iter.hasNext() ) {
						saveValue( fieldName + "_" + count++, iter.next() );
					}
				} else if( Map.class.isAssignableFrom( fieldType ) ) {
				}  else if( Date.class.isAssignableFrom( fieldType ) ) {
					Object currentValue = ReflectionUtil.getFieldValue( this, fieldName );
					if( currentValue != null ) {
						saveValue( fieldName, FormatUtil.formatDateTimeForDB( (Date) currentValue ) );
					}
				} else {
					Object currentValue = ReflectionUtil.getFieldValue( this, fieldName );
					saveValue( fieldName, currentValue );
				}
			} catch( IllegalAccessException iaEx ) {
				ApplicationUtil.handleError( iaEx );
			} catch( NoSuchFieldException nsfEx ) {
				ApplicationUtil.handleError( nsfEx );
			}
		}
	}
	
	public void saveValue( String fieldName, Object value ) {
		if( value != null ) {
			if( value instanceof AplosAbstractBean ) {
				getDataTableState().getPageFilters().put( fieldName, String.valueOf( ApplicationUtil.getClass( (AplosAbstractBean) value ).getName() + "_" + ((AplosAbstractBean) value).getId() ) );
			} else if( value.getClass().isEnum() ) {
				getDataTableState().getPageFilters().put( fieldName, String.valueOf( value.getClass().getName() + "_" + String.valueOf( value ) ) );
			} else {
				getDataTableState().getPageFilters().put( fieldName, String.valueOf( value ) );
			}
		}
	}
	
	public void recoverStateFieldInformation()  {
		try {
			for( int i = 0, n = getStateFields().size(); i < n; i++ ) {
	//			Object property = PropertyUtils.getProperty( this, getStateFields().get( i ).getName() );
				Class<?> fieldType = getStateFields().get( i ).getType();
				String fieldName = getStateFields().get( i ).getName();
				
	//			} else if( fieldType instanceof Object[] ) {
				if( Set.class.isAssignableFrom( fieldType ) ) {
					Set set = (Set) ReflectionUtil.getFieldValue( this, fieldName );
					addFiltersToCollection( getStateFields().get( i ), fieldName, set );
				} else if( List.class.isAssignableFrom( fieldType ) ) {
					List list = (List) ReflectionUtil.getFieldValue( this, fieldName );
					addFiltersToCollection( getStateFields().get( i ), fieldName, list );
				} else if( Map.class.isAssignableFrom( fieldType ) ) {
					
				} else if( Date.class.isAssignableFrom( fieldType ) ) {
					String filterValue = (String) getFilterValue( getStateFields().get( i ), fieldName );
					if( !CommonUtil.isNullOrEmpty( filterValue ) ) {
						try {
							ReflectionUtil.setField( this, fieldName, FormatUtil.getDBSimpleDateTimeFormat().parse( filterValue ) );
						} catch( ParseException pex ) {
							ApplicationUtil.handleError(pex, false);
						}
					}
				} else {
					Object filterValue = getFilterValue( getStateFields().get( i ), fieldName );
					if( filterValue != null ) {
						Field field = ReflectionUtil.getField( this, fieldName ).getField();
						if( field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class) ) {
							filterValue = ConversionUtil.convertToInteger(filterValue);
						}
						ReflectionUtil.setField( this, fieldName, filterValue );
					}
				}
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex, false );
			getDataTableState().getPageFilters().clear();
			getDataTableState().saveDetails();
		} 
	}
	
	public void addFiltersToCollection( Field field, String fieldName, Collection collection ) throws ClassNotFoundException {
		for( String pageFilterKey : getDataTableState().getPageFilters().keySet() ) {
			if( pageFilterKey.startsWith( fieldName ) ) {
				collection.add( getFilterValue( field, pageFilterKey ) );
			}
		}
	}
	
	public Object getFilterValue( Field field, String pageFilterKey ) throws ClassNotFoundException {
		String pageFilterValue = getDataTableState().getPageFilters().get( pageFilterKey );
		if( pageFilterValue != null ) {
			if( pageFilterValue.contains( "_" ) ) {
				String pageFilterValueParts[] = pageFilterValue.split("_", 2);
				Class<?> beanClass = Class.forName(pageFilterValueParts[ 0 ]);
				if( beanClass.isEnum() ) {
					return CommonUtil.getEnumValueOf( (Class<Enum<?>>) beanClass, pageFilterValueParts[ 1 ] );
				} else {
					Long beanId = Long.parseLong( pageFilterValueParts[ 1 ] );
					return new BeanDao( (Class<? extends AplosAbstractBean>) beanClass ).get( beanId );
				}
			} else {
				if( boolean.class.isAssignableFrom( field.getType() ) || Boolean.class.isAssignableFrom( field.getType() ) ) {
					return Boolean.valueOf( pageFilterValue );
				}
			}
		}
		return pageFilterValue;
	}
	
	public void addStateField( String propertyName ) throws NoSuchFieldException {
		try {
			getStateFields().add( ReflectionUtil.getField( this, propertyName ).getField() );
		} catch( IllegalAccessException iaEx ) {
			ApplicationUtil.handleError( iaEx );
		}
	}
	
	public void addStateFields() throws NoSuchFieldException {}

	public int getCountAll() {
		return getBeanDao().getCountAll();
	}

	@Override
	public Object getRowKey(Object object) {
		if (object == null) {
			//return null; //this is likely a mistake with the count if we reach here
			throw new IllegalStateException("getRowKey(Object row) received a null row, this is likely to mean your row count is out of sync with the actual number of returned rows.");
		} else if (object instanceof AplosAbstractBean) {
			return ((AplosAbstractBean) object).getId();
		} else {
			throw new UnsupportedOperationException("getRowKey(Object row) must be implemented/overridden when using rows which do not equate to AplosAbstractBeans.");
		}
    }


	@SuppressWarnings("unchecked")
	@Override
	public Object getRowData(String rowKey) {
		try {
			Long rowIndex = Long.parseLong(rowKey);
			for (Object object : ((List<Object>)getWrappedData())) {
				if (object instanceof AplosAbstractBean) {
					if (rowIndex.equals( ((AplosAbstractBean)object).getId() )) {
						return object;
					}
				} else {
					throw new UnsupportedOperationException("getRowData(String rowKey) must be implemented/overridden when using rows which do not equate to AplosAbstractBeans.");
				}
			}
		} catch (NumberFormatException nfe) {
			throw new UnsupportedOperationException("getRowData(String rowKey) must be implemented/overridden when using rowkeys which do not equate to a longs.");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosBean> T getAssociatedBeanFromScope() {
		return JSFUtil.getBeanFromScope( (Class<? extends AplosBean>) getBeanDao().getBeanClass() );
	}

	public List<Object> getRecordArray() {
		return recordArray;
	}

	public String getSearchCriteria() {
		return searchCriteria;
	}

	public BeanDao getBeanDao() {
		return aqlBeanDao;
	}

	public void setBeanDao(BeanDao aqlBeanDao) {
		this.aqlBeanDao = aqlBeanDao;
	}

	public void determineDeleteBean( boolean isHardDeleteRequested ) {
		deleteBean = getAplosBean();
		setHardDeleteRequested( isHardDeleteRequested );
	}

	@SuppressWarnings("unchecked")
	public Class<? extends AplosBean> determineBeanClass( AplosBean aplosBean ) {

		if (!isUsingBeanDaoClass() && !aplosBean.getClass().getName().contains("$")) {
			return aplosBean.getClass();
		} else {
			//  Use the class of the AqlBeanDao as the object may be a listBean.
			return (Class<? extends AplosBean>) getBeanDao().getBeanClass();
		}

	}

	public void goToNew() {
		goToNew( true );
	}

	public void goToNew( boolean redirect ) {
		AplosBean newBean = (AplosBean) aqlBeanDao.getNew();
		newBean.initialiseNewBean();
		
		//not the right behaviour now : we want it to call the beans method which may bind it to multiple
		//JSFUtil.addToTabSession( AqlBeanDao.getBinding(), newBean);
		newBean.addToScope( BackingPage.determineScope( getBeanDao().getListPageClass(), getBeanDao().getBeanClass() ) );

		if( redirect ) {
			JSFUtil.redirect( determineEditPageClass( newBean ) );
		}
	}

	public AplosBean getAplosBean() {
		return (AplosBean) JSFUtil.getRequest().getAttribute( "tableBean" );
	}

	public AplosBean getRealBean() {
		AplosBean aplosBean = getAplosBean();
		AplosBean loadedAplosBean = new BeanDao( determineBeanClass( aplosBean ) ).get( aplosBean.getId() );
		loadedAplosBean = (AplosBean) loadedAplosBean.getSaveableBean();
		return loadedAplosBean;
	}


	public void selectBean() {
		selectBean( true );
	}

	public void selectBean( boolean redirect ) {
		AplosBean aplosBean = getAplosBean();
		selectBean( determineBeanClass( aplosBean ), aplosBean.getId(), redirect );
	}

	@SuppressWarnings("rawtypes")
	public void selectBean( Class<? extends AplosBean> loadBeanClass, Long loadBeanId, boolean redirect ) {
		AplosBean loadedAplosBean = new BeanDao( loadBeanClass ).get( loadBeanId );
		loadedAplosBean = (AplosBean) loadedAplosBean.getSaveableBean();
		loadedAplosBean.addToScope(BackingPage.determineScope( getBeanDao().getListPageClass(), getBeanDao().getBeanClass() ) );
		if ( redirect ) {
			Class<? extends BackingPage> editPageClass = determineEditPageClass( loadedAplosBean );
			if (editPageClass != null) {
				JSFUtil.redirect( editPageClass );
			} else if (ApplicationUtil.getAplosContextListener().isDebugMode()) {
				JSFUtil.addMessageForError("Edit page class is null. Cannot Redirect.");
			}
		}
	}
	
	protected void setDeleteBean( AplosBean deleteBean ) {
		this.deleteBean = deleteBean;
	}

	protected AplosBean getDeleteBean() {
		return deleteBean;
	}

	public void deleteBean() {
		if( deleteBean != null ) {
			AplosBean loadedBean = (AplosBean) new BeanDao( determineBeanClass( deleteBean ) ).get( deleteBean.getId() ).getSaveableBean();
			if (loadedBean != null && loadedBean.checkForDelete() && loadedBean.getDeletable() && loadedBean.getEditable()) {
				setDeleteBean( loadedBean );
				if( isHardDelete() || isHardDeleteRequested() ) {
					callBeanHardDelete( loadedBean );
				} else {
					callBeanSoftDelete( loadedBean );
				}
			} else {
				JSFUtil.addMessageForError("This item cannot be deleted, it is uneditable or undeleteable.");
			}
		} else {
			ApplicationUtil.getAplosContextListener().handleError( new Exception( "Delete Bean was not set" ) );
		}
	}
	
	public void callBeanHardDelete( AplosBean aplosBean ) {
		aplosBean.hardDelete();
	}
	
	public void callBeanSoftDelete( AplosBean aplosBean ) {
		aplosBean.delete();
	}

	public void reinstateBean() {
		AplosBean reinstateBean = getAplosBean();
		AplosBean loadedBean = (AplosBean) new BeanDao( determineBeanClass( reinstateBean ) ).get( reinstateBean.getId() ).getSaveableBean();
		loadedBean.reinstate();
		//this.updateRecordArray();
	}

	public void lockBean() {
		AplosBean lockBean = getAplosBean();
		if (lockBean != null) {
			AplosBean loadedBean = (AplosBean) new BeanDao( determineBeanClass( lockBean ) ).get( lockBean.getId() ).getSaveableBean();
			if (loadedBean != null) {
				loadedBean.setDeletable(false);
				loadedBean.setEditable(false);
				loadedBean.saveDetails();
			}
		}
	}

	public void unlockBean() {
		AplosBean unlockBean = getAplosBean();
		AplosBean loadedBean = (AplosBean) new BeanDao( determineBeanClass( unlockBean ) ).get( unlockBean.getId() ).getSaveableBean();
		loadedBean.setDeletable(true);
		loadedBean.setEditable(true);
		loadedBean.saveDetails();
	}

	public boolean isUsingBeanDaoClass() {
		return isUsingBeanDaoClass;
	}

	public void setUsingBeanDaoClass(boolean isUsingBeanDaoClass) {
		this.isUsingBeanDaoClass = isUsingBeanDaoClass;
	}

	public DataTableState getDataTableState() {
		return dataTableState;
	}

	public void setDataTableState(DataTableState dataTableState) {
		this.dataTableState = dataTableState;
	}

	public Class<? extends BackingPage> determineEditPageClass() {
		return determineEditPageClass( null );
	}

	public Class<? extends BackingPage> determineEditPageClass( AplosBean aplosBean ) {
		if( editPageClass != null ) {
			return editPageClass;
		} else if( isUsingBeanDaoClass() || aplosBean == null ) {
			return getBeanDao().getEditPageClass();
		} else {
			return aplosBean.getEditPageClass();
		}
	}

	private Class<? extends BackingPage> getEditPageClass() {
		return editPageClass;
	}

	public void setEditPageClass(Class<? extends BackingPage> editPageClass) {
		this.editPageClass = editPageClass;
	}

	public boolean isHardDelete() {
		return isHardDelete;
	}

	public void setHardDelete(boolean isHardDelete) {
		this.isHardDelete = isHardDelete;
	}

	protected void setRecordArray(List<Object> recordArray) {
		this.recordArray = recordArray;
	}

	public Object[] getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(Object[] selectedRows) {
		this.selectedRows = selectedRows;
	}

	public void setSearchCriteria(String searchCriteria) {
		this.searchCriteria = searchCriteria;
	}

	public List<Field> getStateFields() {
		return stateFields;
	}

	public void setStateFields(List<Field> stateFields) {
		this.stateFields = stateFields;
	}

	public Object getSelectedRow() {
		return selectedRow;
	}

	public void setSelectedRow(Object selectedRow) {
		this.selectedRow = selectedRow;
	}

	public boolean isHardDeleteRequested() {
		return isHardDeleteRequested;
	}

	public void setHardDeleteRequested(boolean isHardDeleteRequested) {
		this.isHardDeleteRequested = isHardDeleteRequested;
	}
}
