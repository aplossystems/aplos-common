package com.aplos.common.beans;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.AfterSaveListener;
import com.aplos.common.annotations.BeanScope;
import com.aplos.common.annotations.persistence.GeneratedValue;
import com.aplos.common.annotations.persistence.Id;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.DisplayName;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@MappedSuperclass
public abstract class AplosAbstractBean implements Serializable, Cloneable, DisplayName {
	private static final long serialVersionUID = 8677480435243964847L;

	@Id
	@GeneratedValue
	private Long id;
	private boolean active = true;
	private Boolean persistentData = false;
	private Boolean deletable = true;
	private Boolean editable = true;
	
	@Transient
	private AfterSaveListener afterSaveListener;
	@Transient
	private int hashCode = 0;
	@Transient
	private boolean readOnly = false;

	public static PersistentClass persistentClass;
	
	public <T> T initialiseNewBean() {
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getClass() );
		if( persistentClass != null ) {
			persistentClass.addRemoveEmptyFields(this);
		}
		return (T) this;
	}
	
	public void clearFieldsAfterCopy() {
		setId( null );
	}
	
	public void persistenceBeanCreated() {
		
	}
	
	public static JsfScope determineScope( Class<? extends AplosAbstractBean> beanClass ) {
		BeanScope beanScope = beanClass.getAnnotation(BeanScope.class);
		if( beanScope == null ) {
			beanScope = findBeanScopeInParents( beanClass );
			if( beanScope == null ) {
				return JsfScope.FLASH_VIEW;
			} else {
				return beanScope.scope();
			}
		} else {
			return beanScope.scope();
		}
	}
	
	public PersistentClass getPersistentClass() {
		return ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( getClass() );
	}
	
	public boolean isEmptyBean() {
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getClass() );
		for( FieldInfo fieldInfo : persistentClass.getFullDbFieldInfos() ) {
			if( !fieldInfo.getField().getDeclaringClass().isAssignableFrom( AplosBean.class )
					&& !fieldInfo.isEmpty( this ) ) {
				return false;
			}
		}
		return true;
	}
	
	public <T extends AplosAbstractBean> T  getSaveableBean() {
		return getSaveableBean( new BeanMap() );
	}
	
	public <T extends AplosAbstractBean> T  getSaveableBean( BeanMap beanMap ) {
		if( this.isReadOnly() ) {
			AplosAbstractBean newBean = beanMap.findBean( (Class<? extends AplosAbstractBean>) ApplicationUtil.getDbPersistentClass( this ).getTableClass(), getId() );
			if( newBean == null ) {
				newBean = getPersistenceCopyBean( beanMap );
				newBean.setReadOnly( false );	
			}
			return (T) newBean;
		} else {
			return (T) this;
		}
	}
	
	public <T extends AplosAbstractBean> T  getReadOnlyBean() {
		if( !this.isReadOnly() && !this.isNew() ) {
			AplosAbstractBean aplosAbstractBean = ApplicationUtil.getPersistenceContext().findBean( getClass(), getId() );
			if( aplosAbstractBean == null ) {
				BeanDao beanDao = new BeanDao( (Class<? extends AplosAbstractBean>) getClass() );
				aplosAbstractBean = beanDao.get( getId() );
			}
			/*
			 * It can sometimes be null if the bean has been saved but the transaction not yet committed.
			 */
			if( aplosAbstractBean != null ) {
				return (T) aplosAbstractBean;
			}
		}
		return (T) this;
	}
	
	public AplosAbstractBean getLazyBean() {
		AplosAbstractBean newBean = (AplosAbstractBean) CommonUtil.getNewInstance( getClass() );
		newBean.setId( getId() ); 
		newBean.setLazilyLoaded( true );
		newBean.setInDatabase( isInDatabase() );
		newBean.setReadOnly( true );
		return newBean;
	}
	
	public AplosAbstractBean getPersistenceCopyBean( BeanMap beanMap ) {
		try {
			if( ((PersistenceBean) this).isLazilyLoaded() && this.isInDatabase() ) {
				AplosAbstractBean aplosAbstractBean = BeanDao.loadLazyValues( this, true );
				return aplosAbstractBean.getPersistenceCopyBean( beanMap );
			}
			PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( getClass() );
			
			AplosAbstractBean newBean = (AplosAbstractBean) CommonUtil.getNewInstance( getClass() );
			FieldInfo primaryFieldInfo = persistentClass.determinePrimaryKeyFieldInfo();
			if( primaryFieldInfo != null ) {
				primaryFieldInfo.getField().set(newBean, primaryFieldInfo.getField().get(this));
			}
			beanMap.registerBean( newBean );
			persistentClass.copyFieldsAndSetSaveable( this, newBean, beanMap );
			((PersistenceBean) newBean).copyFields( (PersistenceBean) this );
//			cascadePersistenceCopy(persistentClass, newBean);
			return newBean;
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
			return null;
		}
	}
	
//	public void cascadePersistenceCopy( PersistentClass persistentClass, AplosAbstractBean newBean ) throws Exception {
//		Object tempConvertedValue;
//		for( FieldInfo cascadeFieldInfo : persistentClass.getCascadeFieldInfoMap().keySet() ) {
//			if( cascadeFieldInfo instanceof PersistentClassFieldInfo || cascadeFieldInfo instanceof PersistentCollectionFieldInfo ) {
//				tempConvertedValue = SelectCriteria.convertFieldValue(cascadeFieldInfo.getValue( this ), cascadeFieldInfo, cascadeFieldInfo.getField());
//				if( tempConvertedValue instanceof AplosAbstractBean ) {
//					if( cascadeFieldInfo instanceof ForeignFieldInfo && ((AplosAbstractBean) tempConvertedValue).isLazilyLoaded() ) {
//						BeanDao resultBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) ((ForeignFieldInfo) cascadeFieldInfo).getPersistentClass().getTableClass() );
//						resultBeanDao.setWhereCriteria( "bean." + ((ForeignFieldInfo) cascadeFieldInfo).getForeignFieldInfo().getField().getName() + ".id = " + getId() );
//						tempConvertedValue = resultBeanDao.getFirstBeanResult();
//					}
//					if( tempConvertedValue != null ) {
//						tempConvertedValue = ((AplosAbstractBean) tempConvertedValue).getSaveableBean();
//					}
//					cascadeFieldInfo.setValue( ((PersistenceBean) this).isInDatabase(), newBean, tempConvertedValue );
//				} else if ( tempConvertedValue instanceof PersistentCollection ) {
//					PersistentCollection persistentCollection = ((PersistentCollection) tempConvertedValue).getCopy();
//					persistentCollection.convertToSaveableBeans();
//					cascadeFieldInfo.setValue( ((PersistenceBean) this).isInDatabase(), newBean, persistentCollection );
//				}
//			}
//		}
//	}

	public void copy( AplosAbstractBean aplosAbstractBean ) {
		setActive( aplosAbstractBean.isActive() );
		setPersistentData( aplosAbstractBean.getPersistentData() );
		setDeletable( aplosAbstractBean.getDeletable() );
	}

	public boolean equalsId( AplosAbstractBean aplosAbstractBean ) {
		if( aplosAbstractBean != null &&
			((aplosAbstractBean.getId() == null && getId() == null) ||
			(getId() != null && getId().equals( aplosAbstractBean.getId() )))	) {
			return true;
		} else {
			return false;
		}
	}

	//used to force a name in breadcrumbs
	public String getTrailDisplayName() {
		return getDisplayName();
	}

	public String getTableName() {
		return getTableName(ApplicationUtil.getClass( this ));
	}

	public String getBinding() {
		return getBinding( ApplicationUtil.getClass( this ) );
	}
	
	public final void addToScope() {
		addToScope( determineJsfScope() );
	}
	
	public JsfScope determineJsfScope() {
		return determineScope( getClass() );
	}
	
	public static BeanScope findBeanScopeInParents( Class currentClass ) {
		if( AplosAbstractBean.class.isAssignableFrom( currentClass.getSuperclass() ) ) {
			BeanScope beanScope = (BeanScope) currentClass.getSuperclass().getAnnotation(BeanScope.class);
			if( beanScope != null ) {
				return beanScope;
			} else {
				return findBeanScopeInParents( currentClass.getSuperclass() );
			}
		}
		return null;	
	}
	
	public void addToScope( JsfScope associatedBeanScope ) {
		addToScope( this.getBinding(), this, associatedBeanScope );
	}
	
	public static final void addToScope( String binding, Object value, JsfScope associatedBeanScope ) {
		JSFUtil.addToScope(binding, value, associatedBeanScope);
	}

	public final void addToTabSession( HttpSession session, boolean isFrontEnd ) {
		JSFUtil.addToTabSession( this.getBinding(), this, session, isFrontEnd );
	}

	/*
	 * This was made final so that I could remove the old way of doing plural bindings,
	 * this may not be required in the future.
	 */
	public final String getPluralDisplayName() {
		return getPluralDisplayName( ApplicationUtil.getClass(this) );
	}
	
	public static String getPluralDisplayName( Class<? extends AplosAbstractBean> beanClass ) {
		return ApplicationUtil.getAplosContextListener().translate( beanClass.getName().replace( "com.aplos.", "" ) + "_PLURAL" );
	}

	public String getEntityName() {
		return getEntityName( ApplicationUtil.getClass(this) );
	}

//	@Override
//	public final void hibernateInitialise( boolean fullInitialisation ) { 
//		if( !isHibernateInitialised() || (!isFullyInitialised() && fullInitialisation) ) {
//			setHibernateInitialised( true );
//			if( fullInitialisation ) {
//				setFullyInitialised(true);
//			}
//			hibernateInitialiseAfterCheck( fullInitialisation );
//		}
//	}
//	
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		
//	}

	public static String getBinding(Class<?> clazz) {
		return CommonUtil.getBinding(clazz);
	}

	public static String getTableName(Class<?> clazz) {
		return clazz.getSimpleName();
	}

	public static String getIntermediateTableName(Class<?> clazz, Class<?> joinedClass) {
		return clazz.getSimpleName() + "_" + joinedClass.getSimpleName();
	}

	public static String getEntityName(Class<?> clazz) {
		return clazz.getSimpleName().replaceAll("([a-z])([A-Z])", "$1 $2");
	}

	public boolean isNew() {
		return getId() == null;
	}

	public boolean getIsNew() {
		return this.isNew();
	}

	public String getDisplayName() {
		return getEntityName();
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

	public void hardDelete() {
		ApplicationUtil.getAplosModuleFilterer().processHardDeleteEvent(this);
		if( getId() != null ) {
			String tableName = getPersistentClass().getDbPersistentClass().determineSqlTableName();
			for( JunctionTable tempJunctionTable : getPersistentClass().getCollectionTableList() ) {
				StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " ).append( tempJunctionTable.determineSqlTableName() );
				sqlBuf.append( " WHERE " ).append( tempJunctionTable.getPersistentClassFieldInfo().getSqlName() ).append( " = " ).append( getId() );
				ApplicationUtil.executeSql( sqlBuf.toString() );
			}
			for( JunctionTable tempJunctionTable : getPersistentClass().getReverseCollectionTableMap().values() ) {
				StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " ).append( tempJunctionTable.determineSqlTableName() );
				sqlBuf.append( " WHERE " ).append( tempJunctionTable.getCollectionFieldInfo().getSqlName() ).append( " = " ).append( getId() );
				ApplicationUtil.executeSql( sqlBuf.toString() );
			}
			StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " ).append( tableName );
			sqlBuf.append( " WHERE id = " ).append( getId() );
			ApplicationUtil.executeSql( sqlBuf.toString() );
		}
	}

	public static List<? extends AplosAbstractBean> sortByDisplayName( List<? extends AplosAbstractBean> lookupBeanList ) {
		Collections.sort( lookupBeanList, new Comparator<AplosAbstractBean>() {
			@Override
			public int compare(AplosAbstractBean aplosAbstractBean1, AplosAbstractBean aplosAbstractBean2) {
				if ((aplosAbstractBean1 == null || aplosAbstractBean1.getDisplayName() == null) && (aplosAbstractBean2 == null || aplosAbstractBean2.getDisplayName() == null)) {
					return 0;
				}
				if (aplosAbstractBean1 == null || aplosAbstractBean1.getDisplayName() == null) {
					return 1;
				}
				if (aplosAbstractBean2 == null || aplosAbstractBean2.getDisplayName() == null) {
					return -1;
				}
				return aplosAbstractBean1.getDisplayName().compareToIgnoreCase( aplosAbstractBean2.getDisplayName() );
			}
		});
		return lookupBeanList;
	}

	public static String getIdListStrWithCommas( List<? extends AplosAbstractBean> aplosAbstractBeanList ) {
		String idAry[] = new String[ aplosAbstractBeanList.size() ];
		for( int i = 0, n = aplosAbstractBeanList.size(); i < n; i++ ) {
			if( aplosAbstractBeanList.get( i ) != null ) {
				idAry[ i ] = String.valueOf( aplosAbstractBeanList.get( i ).getId() );
			}
		}
		return StringUtils.join( idAry, "," );
	}

	@SuppressWarnings("unchecked")
	public static SelectItem[] getSelectItemBeans(Class<? extends AplosAbstractBean> lookupBeanClass) {
		return getSelectItemBeansWithNotSelected( new BeanDao( lookupBeanClass ).setIsReturningActiveBeans(true).getAll(), null );
	}

	public static SelectItem[] getSelectItemBeans(List<? extends AplosAbstractBean> lookupBeanList) {
		if( lookupBeanList == null ) {
			return null;
		} else {
			return getSelectItemBeans( lookupBeanList, null, true );
		}
	}

	public static SelectItem[] getSelectItemBeans(List<? extends AplosAbstractBean> lookupBeanList, boolean sort ) {
		return getSelectItemBeans( lookupBeanList, null, sort );
	}

	public SelectItem[] getSelectItemBeans() {
		return getSelectItemBeans( (String) null );
	}

	public SelectItem[] getSelectItemBeansWithNotSelected() {
		return getSelectItemBeans( CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText() );
	}

	public static SelectItem[] getSelectItemBeansWithNotSelected(Class<? extends AplosAbstractBean> lookupBeanClass) {
		return getSelectItemBeansWithNotSelected( new BeanDao( lookupBeanClass ).setIsReturningActiveBeans(true).getAll() );
	}

	public static SelectItem[] getSelectItemBeansWithNotSelected( List<? extends AplosAbstractBean> lookupBeanList  ) {
		return getSelectItemBeans( lookupBeanList, CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText(), true );
	}

	public static SelectItem[] getSelectItemBeansWithNotSelected( List<? extends AplosAbstractBean> lookupBeanList, String notSelectedStr  ) {
		return getSelectItemBeans( lookupBeanList, notSelectedStr, true );
	}

	/**
	 * @deprecated Please use {@link #getSelectItemBeans()} instead.
	 * @param lookupBeanClass
	 * @param notSelectedStr
	 * @return
	 */
	@Deprecated
	public static SelectItem[] getSelectItemBeansWithNotSelected( List<? extends AplosAbstractBean> lookupBeanList, String notSelectedStr, boolean sort ) {
		return getSelectItemBeans( lookupBeanList, notSelectedStr, sort );
	}

	public static SelectItem[] getSelectItemBeans( List<? extends AplosAbstractBean> lookupBeanList, String notSelectedStr, boolean sort ) {
		SelectItem[] selectItems;
		if ( sort ) {
			lookupBeanList = sortByDisplayName( lookupBeanList );
		}

		int count = 0;
		if( notSelectedStr == null ) {
			selectItems = new SelectItem[ lookupBeanList.size() ];
		} else {
			selectItems = new SelectItem[ lookupBeanList.size() + 1 ];
			selectItems[ count++ ] = new SelectItem( null, notSelectedStr );
		}

		for ( int i = 0, n = lookupBeanList.size(); i < n; i++ ) {
			selectItems[ count++ ] = new SelectItem( lookupBeanList.get( i ), lookupBeanList.get( i ).getDisplayName() );
		}

		return selectItems;
	}

	public SelectItem[] getSelectItemBeansWithNotSelected( String notSelectedStr ) {
		return getSelectItemBeans( getClass(), notSelectedStr );
	}

	public SelectItem[] getSelectItemBeans( String notSelectedStr ) {
		return getSelectItemBeans( getClass(), notSelectedStr );
	}

	/**
	 * @deprecated Please use {@link #getSelectItemBeans()} instead.
	 * @param lookupBeanClass
	 * @param notSelectedStr
	 * @return
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public SelectItem[] getSelectItemBeansWithNotSelected( Class<? extends AplosAbstractBean> lookupBeanClass, String notSelectedStr ) {
		return getSelectItemBeansWithNotSelected( new BeanDao( lookupBeanClass ).setIsReturningActiveBeans(true).getAll(),notSelectedStr);
	}

	@SuppressWarnings("unchecked")
	public SelectItem[] getSelectItemBeans( Class<? extends AplosAbstractBean> lookupBeanClass, String notSelectedStr ) {
		List<? extends AplosAbstractBean> lookupList = new BeanDao( lookupBeanClass ).setIsReturningActiveBeans(true).getAll();
		//HibernateUtil.initialiseList(lookupList);
		return getSelectItemBeansWithNotSelected( lookupList, notSelectedStr);
	}

	@SuppressWarnings("unchecked")
	public static SelectItem[] getSelectItemBeansStatic( Class<? extends AplosAbstractBean> lookupBeanClass, String notSelectedStr ) {
		return getSelectItemBeansWithNotSelected( new BeanDao( lookupBeanClass ).setIsReturningActiveBeans(true).getAll(),notSelectedStr);
	}

	public SelectItem[] getSelectItems() {
		return getSelectItems( true );
	}

	@SuppressWarnings("unchecked")
	public SelectItem[] getSelectItems( boolean sort ) {
		List<? extends AplosAbstractBean> lookupBeanList;
		if( this instanceof AplosBean ) {
			lookupBeanList = new BeanDao( getClass() ).setIsReturningActiveBeans(true).getAll();
		} else {
			lookupBeanList = new BeanDao( getClass() ).getAll();
		}
		SelectItem[] selectItems;

		if( sort ) {
			lookupBeanList = sortByDisplayName( lookupBeanList );
		}
		selectItems = new SelectItem[ lookupBeanList.size() ];

		for( int i = 0, n = selectItems.length; i < n; i++ ) {
			selectItems[ i ] = new SelectItem( lookupBeanList.get( i ).getId(), lookupBeanList.get( i ).getDisplayName() );
		}

		return selectItems;
	}

	public SelectItem[] getSelectItemsWithNotSelected( String notSelectedStr ) {
		return getSelectItemsWithNotSelected( notSelectedStr, true );
	}

	@SuppressWarnings("unchecked")
	public SelectItem[] getSelectItemsWithNotSelected( String notSelectedStr, boolean sort ) {
		List<? extends AplosAbstractBean> lookupBeanList;
		if( this instanceof AplosBean ) {
			lookupBeanList = new BeanDao( getClass() ).setIsReturningActiveBeans(true).getAll();
		} else {
			lookupBeanList = new BeanDao( getClass() ).getAll();
		}
		SelectItem[] selectItems;
		if( sort ) {
			lookupBeanList = sortByDisplayName( lookupBeanList );
		}

		selectItems = new SelectItem[ lookupBeanList.size() + 1 ];
		selectItems[ 0 ] = new SelectItem( -1l, notSelectedStr );

		for( int i = 0, n = lookupBeanList.size(); i < n; i++ ) {
			selectItems[ i + 1 ] = new SelectItem( lookupBeanList.get( i ).getId(), lookupBeanList.get( i ).getDisplayName() );
		}

		return selectItems;
	}

	public static SelectItem[] getSelectItemsWithNotSelected(List<? extends AplosAbstractBean> lookupBeanList, String notSelectedStr ) {
		return getSelectItemsWithNotSelected( lookupBeanList, notSelectedStr, true );
	}

	public static SelectItem[] getSelectItemsWithNotSelected(List<? extends AplosAbstractBean> lookupBeanList, String notSelectedStr, boolean sort ) {
		SelectItem[] selectItems;
		if( sort ) {
			lookupBeanList = sortByDisplayName( lookupBeanList );
		}

		selectItems = new SelectItem[ lookupBeanList.size() + 1 ];
		selectItems[ 0 ] = new SelectItem( -1l, notSelectedStr );

		for( int i = 0, n = lookupBeanList.size(); i < n; i++ ) {
			selectItems[ i + 1 ] = new SelectItem( lookupBeanList.get( i ).getId(), lookupBeanList.get( i ).getDisplayName() );
		}

		return selectItems;
	}
	
	public boolean saveDetails() {
		return CommonUtil.saveBean(this);
	}

	public boolean saveDetailsWithThrow() throws Exception {
		return CommonUtil.saveBeanWithThrow(this);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AplosAbstractBean)) {
			return false;
		} else {
			AplosAbstractBean aplosAbstractBean = (AplosAbstractBean) o;
			return hashCode() == aplosAbstractBean.hashCode();
		}
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) { generateHashcode(); }
		return hashCode;
	}

	public void generateHashcode() {
		if (getId() == null) {
			hashCode = super.hashCode();
		} else {
			hashCode = (ApplicationUtil.getDbPersistentClass(this).getTableClass().getSimpleName()
					+ CommonUtil.emptyIfNull(getId().toString())).hashCode();
		}
	}

	public void setId( Long id ) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setPersistentData(Boolean persistentData) {
		this.persistentData = persistentData;
	}

	public Boolean getPersistentData() {
		if( persistentData == null ) {
			ApplicationUtil.handleError( new Exception( "Persistent data column set to null" ), false );
			String tableName = getPersistentClass().getDbPersistentClass().determineSqlTableName();
			ApplicationUtil.executeSql( "UPDATE " + tableName + " SET persistentData = true WHERE persistentData IS NULL" );
			return false;
		} else {
			return persistentData;
		}
	}

	public void setDeletable(Boolean deletable) {
		this.deletable = deletable;
	}

	public Boolean getDeletable() {
		if( deletable == null ) {
			ApplicationUtil.handleError( new Exception( "Deletable column set to null" ), false );
			String tableName = getPersistentClass().getDbPersistentClass().determineSqlTableName();
			ApplicationUtil.executeSql( "UPDATE " + tableName + " SET deletable = true WHERE deletable IS NULL" );
			return true;
		} else {
			return deletable;
		}
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setEditable(Boolean editable) {
		this.editable = editable;
	}

	public Boolean getEditable() {
		if (editable == null) {
			ApplicationUtil.handleError( new Exception( "Editable column set to null" ), false );
			String tableName = getPersistentClass().getDbPersistentClass().determineSqlTableName();
			ApplicationUtil.executeSql( "UPDATE " + tableName + " SET editable = true WHERE editable IS NULL" );
			editable = true;
		}
		return editable;
	}

	public AfterSaveListener getAfterSaveListener() {
		return afterSaveListener;
	}

	public void setAfterSaveListener(AfterSaveListener afterSaveListener) {
		this.afterSaveListener = afterSaveListener;
	}
}
