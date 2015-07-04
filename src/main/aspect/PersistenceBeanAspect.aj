
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.reflect.FieldSignature;

import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.collection.PersistentList;
import com.aplos.common.persistence.collection.PersistentMap;
import com.aplos.common.persistence.collection.PersistentSet;
import com.aplos.common.persistence.fieldLoader.FieldLoader;
import com.aplos.common.persistence.fieldLoader.ListFieldLoader;
import com.aplos.common.persistence.fieldLoader.MapFieldLoader;
import com.aplos.common.persistence.fieldLoader.PersistentClassFieldLoader;
import com.aplos.common.persistence.fieldLoader.PropertyFieldLoader;
import com.aplos.common.persistence.fieldLoader.SetFieldLoader;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.utils.ApplicationUtil;

public aspect PersistenceBeanAspect {
	@Transient
	public Map<String, Object> PersistenceBean.hiddenFieldsMap = new HashMap<String, Object>();
	@Transient
	public Map<String, Boolean> PersistenceBean.loadedTableMap = new HashMap<String, Boolean>();
	@Transient
	public Map<String, Boolean> PersistenceBean.proceedMap = new HashMap<String, Boolean>();
	@Transient
	private boolean PersistenceBean.isLazilyLoaded = false;
	@Transient
	private boolean PersistenceBean.isAlreadyLoading = false;
	@Transient
	private boolean PersistenceBean.isInDatabase = false;
	@Transient
	private boolean PersistenceBean.isAlreadySaving = false;
	
	declare parents: AplosAbstractBean+ implements PersistenceBean;
	
	pointcut checkForLazyObject(AplosAbstractBean xe): target( xe ) && get(* *)
		&& !get(static * *) && !get(final * *) 
		&& !get(Long id) && !get(@Transient * *);

	Object around( AplosAbstractBean xe ): checkForLazyObject( xe ) {
		String propertyName = thisJoinPointStaticPart.getSignature().getName();

		PersistenceBean targetBean = ((PersistenceBean) xe);
		if( new Boolean( true ).equals( targetBean.getProceedMap().get( propertyName ) ) ) {
			return proceed( xe );
		} else {
			Map<String, FieldLoader> fieldLoaderMap = ApplicationUtil.getFieldLoaderMap( xe.getClass() );
			FieldLoader fieldLoader = fieldLoaderMap.get( propertyName );
			if( fieldLoader == null ) { 
				Class fieldType = ((FieldSignature) thisJoinPointStaticPart.getSignature()).getFieldType();
				PersistentClass persistentClass = targetBean.getPersistentClass();
				FieldInfo fieldInfo = null;
				if( persistentClass != null ) {
					fieldInfo = persistentClass.getFieldInfoFromSqlName( propertyName, true );
					if( fieldInfo != null ) {
						if( fieldInfo instanceof ForeignFieldInfo ) {
							if( ((ForeignFieldInfo) fieldInfo).getPersistentCollectionType() == null ) {
								fieldLoader = new PersistentClassFieldLoader( targetBean, propertyName, persistentClass, fieldInfo );
							} else if( PersistentList.class.isAssignableFrom( ((ForeignFieldInfo) fieldInfo).getPersistentCollectionType() ) ) {
								fieldLoader = new ListFieldLoader( propertyName, persistentClass, fieldInfo );
							} else if( PersistentMap.class.isAssignableFrom( ((ForeignFieldInfo) fieldInfo).getPersistentCollectionType() ) ) {
								fieldLoader = new MapFieldLoader( propertyName, persistentClass, fieldInfo );
							} else if( PersistentSet.class.isAssignableFrom( ((ForeignFieldInfo) fieldInfo).getPersistentCollectionType() ) ) {
								fieldLoader = new SetFieldLoader( propertyName, persistentClass, fieldInfo );
							}
						} else {
							if( List.class.isAssignableFrom( fieldType ) && fieldInfo instanceof CollectionFieldInfo ) {
								fieldLoader = new ListFieldLoader( propertyName, persistentClass, fieldInfo );
							} else if( Map.class.isAssignableFrom( fieldType ) && fieldInfo instanceof CollectionFieldInfo ) {
								fieldLoader = new MapFieldLoader( propertyName, persistentClass, fieldInfo );
							} else if( Set.class.isAssignableFrom( fieldType ) && fieldInfo instanceof CollectionFieldInfo ) {
								fieldLoader = new SetFieldLoader( propertyName, persistentClass, fieldInfo );
							} else if( fieldInfo instanceof PersistentClassFieldInfo ) {
								fieldLoader = new PersistentClassFieldLoader( targetBean, propertyName, persistentClass, fieldInfo );
							}
						}
					}
				}
				if( fieldLoader == null ) {
					fieldLoader = new PropertyFieldLoader( propertyName, persistentClass, fieldInfo );
				}
				fieldLoaderMap.put( propertyName, fieldLoader );
			}
			boolean isTargetBeanLoaded = false;
			if( !fieldLoader.getPropertyName().equals( "id" ) ) {
				PersistenceBean oldTargetBean = targetBean;
				targetBean = loadTargetBeanIfRequired( targetBean, fieldLoader.getPropertyName() );
				if( oldTargetBean !=  targetBean ) {
					isTargetBeanLoaded = true;
				}
			}
			if( targetBean.getProceedMap().get( propertyName ) == null && !targetBean.isLazilyLoaded() ) {
				if( fieldLoader instanceof PropertyFieldLoader ) {
					targetBean.getProceedMap().put( propertyName, true );
				} else {
					targetBean.getProceedMap().put( propertyName, false );
				}
			}
			return fieldLoader.getValue( targetBean, proceed( xe ), isTargetBeanLoaded );
		}
	}
	
	pointcut checkForReadOnly(AplosAbstractBean xe): target( xe ) && set(* *)
		&& !set(static * *) && !set(@Transient * *);
	
	void around( AplosAbstractBean xe ): checkForReadOnly( xe ) {
		AplosAbstractBean targetBean = ((AplosAbstractBean) xe);
//		System.out.println( "checkReadOnly " + thisJoinPointStaticPart.getSignature().getName() );
		if( targetBean.isReadOnly() ) {
			String propertyName = thisJoinPointStaticPart.getSignature().getName();
//			System.out.println( "checkReadOnly " + propertyName );
			Map<String, FieldInfo> fieldInfoSetterMap = ApplicationUtil.getFieldInfoSetterMap( xe.getClass() );
			if( !fieldInfoSetterMap.containsKey( propertyName ) ) {
				PersistentClass persistentClass = ApplicationUtil.getPersistentClass( targetBean.getClass() );
				if( persistentClass != null ) {
					fieldInfoSetterMap.put( propertyName, persistentClass.getFieldInfoFromSqlName( propertyName, true ) );
				} else {
					fieldInfoSetterMap.put( propertyName, null );
				}
			}  
			if( fieldInfoSetterMap.get( propertyName ) != null ) {
				ApplicationUtil.handleError( new Exception( "Illegal set on immutable object" ), ApplicationUtil.getAplosContextListener().isRedirectingOnIllegalSet() );
			}
	//			JSFUtil.addMessage( "You cannot edit this bean as it is not saveable" );
	//		} else {
				proceed( xe );
	//		}
		} else {
			proceed( xe );
		}
	}
	
	public PersistenceBean loadTargetBeanIfRequired( PersistenceBean originalBean, String propertyName ) {
		PersistenceBean targetBean = originalBean;
		if( originalBean.isLazilyLoaded() && !originalBean.isAlreadyLoading() && originalBean.getId() != null ) {
			originalBean.setAlreadyLoading(true);
			if( ((AplosAbstractBean) originalBean).isReadOnly() ) {
				targetBean = (PersistenceBean) BeanDao.loadLazyValues( (AplosAbstractBean) originalBean, true );
			}
			originalBean.setAlreadyLoading(false);
		}
		return targetBean;
	}
	
	public void PersistenceBean.copyFields( PersistenceBean srcBean ) {
		hiddenFieldsMap = new HashMap<String, Object>( srcBean.getHiddenFieldsMap() );
		loadedTableMap = new HashMap<String, Boolean>( srcBean.getLoadedTableMap() );
		setLazilyLoaded( srcBean.isLazilyLoaded() );
		/* We don't want to copy this across as the new bean won't ever be in a loading state */
//		setAlreadyLoading( srcBean.isAlreadyLoading() );
		setInDatabase( srcBean.isInDatabase() );
	}

	public Map<String, Boolean> PersistenceBean.getLoadedTableMap() {
		return loadedTableMap;
	}

	public Map<String, Boolean> PersistenceBean.getProceedMap() {
		return proceedMap;
	}

	public Map<String, Object> PersistenceBean.getHiddenFieldsMap() {
		return hiddenFieldsMap;
	}

	public boolean PersistenceBean.isLazilyLoaded() {
		return isLazilyLoaded;
	}

	public void PersistenceBean.setLazilyLoaded( boolean isLazilyLoaded ) {
		this.isLazilyLoaded = isLazilyLoaded;
	}

	public boolean PersistenceBean.isInDatabase() {
		return isInDatabase;
	}

	public void PersistenceBean.setInDatabase( boolean isInDatabase ) {
		this.isInDatabase = isInDatabase;
	}

	public boolean PersistenceBean.isAlreadyLoading() {
		return isAlreadyLoading;
	}

	public void PersistenceBean.setAlreadyLoading( boolean isAlreadyLoading ) {
		this.isAlreadyLoading = isAlreadyLoading;
	}

	public boolean PersistenceBean.isAlreadySaving() {
		return isAlreadySaving;
	}

	public void PersistenceBean.setAlreadySaving( boolean isAlreadySaving ) {
		this.isAlreadySaving = isAlreadySaving;
	}
	
	public boolean PersistenceBean.isTableLoaded( AqlTable aqlTable ) {
		Boolean isTableLoaded = getLoadedTableMap().get( ((PersistentClass) aqlTable.getPersistentTable()).getTableClass().getSimpleName() );
		if( isTableLoaded == null ) {
			return false;
		} else {
			return isTableLoaded;
		}
	}
	
	public void PersistenceBean.addLoadedTable( AqlTable aqlTable ) {
		if( getLoadedTableMap().size() == 0 ) {
			PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getClass() );
			/*
			 * This may occur for classes that override the underlying persistentClass but aren't
			 * persisted themselves like the List Beans.
			 */
			if( persistentClass == null ) {
				Class beanClass = getClass();
				while( persistentClass == null && beanClass != Object.class ) {
					beanClass = beanClass.getSuperclass(); 
					persistentClass = ApplicationUtil.getPersistentClass( beanClass );
				}
			}
			getLoadedTableMap().put( persistentClass.getTableClass().getSimpleName(), false );
			while( persistentClass.getParentPersistentClass() != null && InheritanceType.JOINED_TABLE.equals( persistentClass.getParentPersistentClass().getInheritanceType() ) ) {
				persistentClass = persistentClass.getParentPersistentClass();
				getLoadedTableMap().put( persistentClass.getTableClass().getSimpleName(), false );
			}
		}
		getLoadedTableMap().put( ((PersistentClass) aqlTable.getPersistentTable()).getTableClass().getSimpleName(), true );
		for( Boolean tempBoolean : getLoadedTableMap().values() ) {
			if( !tempBoolean ) {
				setLazilyLoaded( true );
				return;
			}
		}
		setLazilyLoaded( false );
	}
}
