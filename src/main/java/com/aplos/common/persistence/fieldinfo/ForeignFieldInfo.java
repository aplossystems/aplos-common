package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import com.aplos.common.annotations.persistence.IndexColumn;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.IndexableFieldInfo;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.collection.PersistentAbstractCollection;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class ForeignFieldInfo extends PersistentClassFieldInfo implements IndexableFieldInfo {
	private FieldInfo foreignFieldInfo;
	private String mappedBy;
	private JoinColumn joinColumnAnnotation;
	private IndexFieldInfo indexFieldInfo;
	private Class<? extends PersistentAbstractCollection> persistentCollectionType;
	
	public ForeignFieldInfo( PersistentClass foreignPersistentClass, PersistentClass parentPersistentClass, Field field) {
		super( foreignPersistentClass, parentPersistentClass, field);
		setSqlName( "" );
	}
	
	@Override
	public void init() {
		setPersistentCollectionType( CollectionFieldInfo.collectionMap.get( getField().getType() ) );
	}
	
	@Override
	public void setValue( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value ) throws Exception {
		if( getPersistentCollectionType() != null ) {
    		getField().setAccessible(true);
    		getField().set(bean, value);
		} else {
    		setFieldToPersistentClass( isLoadingFromDatabase, getPersistentClass(), getField(), bean, value);
		}
	}
	
	@Override
	public Object getValue(AplosAbstractBean aplosAbstractBean) {
		try {
			getField().setAccessible(true);
			return getField().get(aplosAbstractBean);
		} catch (IllegalArgumentException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		} catch (IllegalAccessException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		}
		return null;
	}
	
	public void updateIndexColumns() {
		if( getField().getAnnotation( IndexColumn.class ) != null ) {
			IndexColumn indexColumnAnnotation = (IndexColumn) getField().getAnnotation( IndexColumn.class ); 
			setIndexFieldInfo(new IndexFieldInfo( getForeignFieldInfo() ));
			if( !CommonUtil.isNullOrEmpty( indexColumnAnnotation.name()) ) {
				getIndexFieldInfo().setSqlName(indexColumnAnnotation.name());
			}
			getIndexFieldInfo().init();
			getIndexFieldInfo().getApplicationType().setNullable( true );
			getForeignFieldInfo().getParentPersistentClass().getFieldInfos().add( getIndexFieldInfo() );
		}
	}
	
	/*
	 * This was edited out as it stopped the loading of the collection as the fieldInfo could not be
	 * found by the sqlName;
	 */
//	public String determineFieldMapKeyName() {
//		return getForeignFieldInfo().getParentPersistentClass().determineSqlTableName() + "_" + getForeignFieldInfo().getSqlName();
//	}
	
	public List<AplosAbstractBean> getIndexableBeanList(AplosAbstractBean aplosAbstractBean) throws IllegalAccessException {
		
		return null;
	}
	
	@Override
	public void updateIndexFields(AplosAbstractBean aplosAbstractBean)
			throws IllegalAccessException, SQLException {
		if( getIndexFieldInfo() != null && List.class.isAssignableFrom( getField().getType() ) ) {
			getForeignFieldInfo().getField().setAccessible(true);
			getField().setAccessible(true);
			List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) getField().get( aplosAbstractBean );
			PersistentBeanSaver.updateIndexField(beanList, getIndexFieldInfo());
		}
	}
	
	public String getMappedBy() {
		return mappedBy;
	}
	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	public JoinColumn getJoinColumnAnnotation() {
		return joinColumnAnnotation;
	}

	public void setJoinColumnAnnotation(JoinColumn joinColumnAnnotation) {
		this.joinColumnAnnotation = joinColumnAnnotation;
	}

	public FieldInfo getForeignFieldInfo() {
		return foreignFieldInfo;
	}

	public void setForeignFieldInfo(FieldInfo foreignFieldInfo) {
		this.foreignFieldInfo = foreignFieldInfo;
	}

	public IndexFieldInfo getIndexFieldInfo() {
		return indexFieldInfo;
	}

	public void setIndexFieldInfo(IndexFieldInfo indexFieldInfo) {
		this.indexFieldInfo = indexFieldInfo;
	}

	public Class<? extends PersistentAbstractCollection> getPersistentCollectionType() {
		return persistentCollectionType;
	}

	public void setPersistentCollectionType(Class<? extends PersistentAbstractCollection> persistentCollectionType) {
		this.persistentCollectionType = persistentCollectionType;
	}
}
