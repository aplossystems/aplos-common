package com.aplos.common.persistence.fieldinfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.EjbMapKey;
import com.aplos.common.annotations.persistence.HibernateMapKey;
import com.aplos.common.annotations.persistence.IndexColumn;
import com.aplos.common.annotations.persistence.JoinTable;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.IndexableFieldInfo;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.collection.PersistentAbstractCollection;
import com.aplos.common.persistence.collection.PersistentList;
import com.aplos.common.persistence.collection.PersistentMap;
import com.aplos.common.persistence.collection.PersistentSet;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class CollectionFieldInfo extends FieldInfo implements IndexableFieldInfo {
	private Class<?> fieldClass;
	private Annotation relationshipAnnotation;
	private String mappedBy;
	private MapKeyFieldInfo mapKeyFieldInfo;
	private IndexFieldInfo indexFieldInfo;
	private Class<? extends PersistentAbstractCollection> persistentCollectionType;
	/*
	 * There can be multiple junctionTables for the fieldInfo if the field info
	 * is in a super class, then each sub class can have a separate collection table.
	 */
	private Map<Class<?>, JunctionTable> junctionTables = new HashMap<Class<?>,JunctionTable>();
	
	public static Map<Class<?>, Class<? extends PersistentAbstractCollection>> collectionMap;
	
	static {
		collectionMap = new HashMap<Class<?>, Class<? extends PersistentAbstractCollection>>();
		collectionMap.put( List.class, PersistentList.class );
		collectionMap.put( Set.class, PersistentSet.class );
		collectionMap.put( Map.class, PersistentMap.class );
	}
	
	public CollectionFieldInfo( PersistentClass parentPersistentClass, Field field, Class<?> fieldClass, Annotation relationshipAnnotation ) {
		super( parentPersistentClass, field );
		this.fieldClass = fieldClass;
		if( fieldClass.isEnum() ) {
			ApplicationUtil.getPersistentApplication().registerEnumType( (Class<Enum>) fieldClass );
		}
		this.setRelationshipAnnotation(relationshipAnnotation);
	}
	
	public Object getValue( AplosAbstractBean aplosAbstractBean ) {
		Object value = null;
		if( getField() != null ) {
			try {
				getField().setAccessible( true );
				value = getField().get( aplosAbstractBean );
			} catch( IllegalAccessException iaex ) {
				ApplicationUtil.handleError( iaex );
			}
		}
			
		return value;
	}
	
	@Override
	public void addAdditionalFieldInfos(PersistableTable persistableTable) {
		super.addAdditionalFieldInfos(persistableTable);
		if( Map.class.isAssignableFrom( getField().getType() ) ) {
			Class<?> mapKeyClass = (Class<?>) ((ParameterizedTypeImpl) getField().getGenericType()).getActualTypeArguments()[ 0 ];
			String customMapKeyName = null;
			if( getField().getAnnotation( EjbMapKey.class ) != null ) {
				EjbMapKey mapKeyAnnotation = (EjbMapKey) getField().getAnnotation( EjbMapKey.class );
				customMapKeyName = mapKeyAnnotation.name();
			} else if( getField().getAnnotation( HibernateMapKey.class ) != null ) {
				HibernateMapKey mapKeyAnnotation = (HibernateMapKey) getField().getAnnotation( HibernateMapKey.class );
				if( mapKeyAnnotation.columns() != null && mapKeyAnnotation.columns().length > 0
						&& !CommonUtil.isNullOrEmpty( mapKeyAnnotation.columns()[ 0 ].name() ) ) {
					customMapKeyName = mapKeyAnnotation.columns()[ 0 ].name();
				}
			}

			addMapKeyFieldInfo( persistableTable.getFieldInfos(), mapKeyClass, customMapKeyName );
		}
		
		if( getField().getAnnotation( IndexColumn.class ) != null ) {
			if( getIndexFieldInfo() == null ) {
				IndexColumn indexColumnAnnotation = (IndexColumn) getField().getAnnotation( IndexColumn.class );
				setIndexFieldInfo(new IndexFieldInfo( this ));
				getIndexFieldInfo().init();
				getIndexFieldInfo().setSqlName(indexColumnAnnotation.name());
			}
			persistableTable.getFieldInfos().add( getIndexFieldInfo() );
		}
	}

	@Override
	public void updateIndexFields( AplosAbstractBean aplosAbstractBean ) throws SQLException, IllegalAccessException {
		if( getIndexFieldInfo() != null ) {
			getField().setAccessible(true);
			List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) getField().get( aplosAbstractBean );
	
			PersistentBeanSaver.updateIndexField(beanList, getIndexFieldInfo());
		}
	}
	
	public void addMapKeyFieldInfo( List<FieldInfo> fieldInfoList, Class<?> mapKeyClass, String customMapKeyName ) {
		if( getMapKeyFieldInfo() == null ) {
			if( !AplosAbstractBean.class.isAssignableFrom( mapKeyClass ) ) {
				setMapKeyFieldInfo( new MapKeyFieldInfo( this, mapKeyClass, getParentPersistentClass(), getField() ) );
			} else {
				PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( mapKeyClass );
				setMapKeyFieldInfo( new PersistentMapKeyFieldInfo( persistentClass, this,  mapKeyClass, getParentPersistentClass(), getField() ) );
			}
			getMapKeyFieldInfo().init();
			if( !CommonUtil.isNullOrEmpty( customMapKeyName ) ) {
				getMapKeyFieldInfo().setSqlName( customMapKeyName );
			}
		}
		fieldInfoList.add( getMapKeyFieldInfo() );
	}
	
	public String readSqlNameFromJoinTable() {
		if( getField().getAnnotation( JoinTable.class ) != null ) {
			JoinTable joinTableAnnotation = getField().getAnnotation( JoinTable.class );
			if( joinTableAnnotation.inverseJoinColumns() != null && joinTableAnnotation.inverseJoinColumns().length > 0 
					&& !CommonUtil.isNullOrEmpty( joinTableAnnotation.inverseJoinColumns()[ 0 ].name() ) ) {
				return joinTableAnnotation.inverseJoinColumns()[ 0 ].name(); 
			}
		}
		return null;
	}
	
	public void readCollectionTableNameFromJoinTable( JunctionTable collectionTable ) {
		if( getField().getAnnotation( JoinTable.class ) != null ) {
			JoinTable joinTableAnnotation = getField().getAnnotation( JoinTable.class );
			if( joinTableAnnotation.joinColumns() != null && joinTableAnnotation.joinColumns().length > 0 
					&& !CommonUtil.isNullOrEmpty( joinTableAnnotation.joinColumns()[ 0 ].name() ) ) {
				collectionTable.getPersistentClassFieldInfo().setSqlName( joinTableAnnotation.joinColumns()[ 0 ].name() );
			}
		}
	}
	
	public void markAsPrimaryOrUnique( JunctionTable collectionTable ) {
		getJunctionTables().put( collectionTable.getPersistentClass().getDbPersistentClass().getTableClass(), collectionTable );
		PersistentClass ownerPersistentClass = collectionTable.getPersistentClassFieldInfo().getParentPersistentClass();
		if( !(ownerPersistentClass.getInheritanceType().equals( InheritanceType.SINGLE_TABLE ) 
				&& ownerPersistentClass.getParentPersistentClass() != null 
				&& ownerPersistentClass.getParentPersistentClass().getInheritanceType().equals( InheritanceType.SINGLE_TABLE ) 
				&& relationshipAnnotation instanceof CollectionOfElements) ) { 
			if( Map.class.isAssignableFrom( getField().getType() ) ) {
				if( mapKeyFieldInfo != null ) {
					mapKeyFieldInfo.setPrimaryKey( true );
				} else {
					setPrimaryKey( true );
				}
				collectionTable.getPersistentClassFieldInfo().setPrimaryKey( true );
			}
			if( Set.class.isAssignableFrom( getField().getType() ) ) {
				setPrimaryKey( true );
				collectionTable.getPersistentClassFieldInfo().setPrimaryKey( true );
			}
			if( relationshipAnnotation.equals( OneToMany.class ) ) {
				setUnique( true );
			}
			if( getIndexFieldInfo() != null ) {
				collectionTable.getPersistentClassFieldInfo().setPrimaryKey( true );
				getIndexFieldInfo().setPrimaryKey( true );
			}
		}
	}
	
	public JunctionTable getJunctionTable( PersistentClass persistentClass ) {
		while( persistentClass != null ) {
			if( getJunctionTables().get( persistentClass.getTableClass() ) != null ) {
				return getJunctionTables().get( persistentClass.getTableClass() );
			} else {
				persistentClass = persistentClass.getParentPersistentClass();
			}
		}
		return null;
	}
	
	public JunctionTable getJunctionTable( Class<?> subClass ) {
		return getJunctionTables().get( subClass );
	}
	
	@Override
	public void init() {
		setSqlName( "element" );
		setApplicationType( determineDbType( getField(), fieldClass ) );
		setPersistentCollectionType( CollectionFieldInfo.collectionMap.get( getField().getType() ) );
	}
	
	public Class<?> getFieldClass() {
		return fieldClass;
	}
	public void setFieldClass(Class<?> fieldClass) {
		this.fieldClass = fieldClass;
	}

	public Annotation getRelationshipAnnotation() {
		return relationshipAnnotation;
	}

	public void setRelationshipAnnotation(Annotation relationshipAnnotation) {
		this.relationshipAnnotation = relationshipAnnotation;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	public MapKeyFieldInfo getMapKeyFieldInfo() {
		return mapKeyFieldInfo;
	}

	public void setMapKeyFieldInfo(MapKeyFieldInfo mapKeyFieldInfo) {
		this.mapKeyFieldInfo = mapKeyFieldInfo;
	}

	public IndexFieldInfo getIndexFieldInfo() {
		return indexFieldInfo;
	}

	public void setIndexFieldInfo(IndexFieldInfo indexFieldInfo) {
		this.indexFieldInfo = indexFieldInfo;
	}

	public Map<Class<?>, JunctionTable> getJunctionTables() {
		return junctionTables;
	}

	private void setJunctionTables(Map<Class<?>, JunctionTable> junctionTables) {
		this.junctionTables = junctionTables;
	}

	public Class<? extends PersistentAbstractCollection> getPersistentCollectionType() {
		return persistentCollectionType;
	}

	public void setPersistentCollectionType(Class<? extends PersistentAbstractCollection> persistentCollectionType) {
		this.persistentCollectionType = persistentCollectionType;
	}
}
