package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.aql.selectcriteria.PersistentClassSelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.type.applicationtype.MySqlType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class PersistentClassFieldInfo extends FieldInfo implements ForeignKeyFieldInfo {
	private PersistentClass persistentClass;
	private boolean isPolymorphic = false;
	private PolymorphicFieldInfo polymorphicFieldInfo;
	private Set<ForeignFieldInfo> foreignFieldInfos = new HashSet<ForeignFieldInfo>();
	private boolean isRemoveEmpty = false;
	
	public PersistentClassFieldInfo( PersistentClass persistentClass, PersistentClass parentPersistentClass, Field field ) {
		super( parentPersistentClass, field );
		this.persistentClass = persistentClass;
		if( field != null && field.getAnnotation( Any.class ) != null ) {
			setPolymorphic(true);
		}
	}
	
//	@Override
//	public void updateIndexFields( AplosAbstractBean aplosAbstractBean ) throws IllegalAccessException, SQLException {
//		for( ForeignFieldInfo tempForeignFieldInfo : getForeignFieldInfos() ) {
//			if( tempForeignFieldInfo.getIndexFieldInfo() != null ) {
//				getField().setAccessible(true);
//				AplosAbstractBean foreignBean = (AplosAbstractBean) getField().get( aplosAbstractBean );
//				if( foreignBean != null ) {
//					tempForeignFieldInfo.getField().setAccessible(true);
//					List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) tempForeignFieldInfo.getField().get( foreignBean );
//					PersistentBeanSaver.updateIndexField(beanList, tempForeignFieldInfo.getIndexFieldInfo());
//				}
//			}
//		}
//	}
	
	public AplosAbstractBean createAndSetEmptyBean( AplosAbstractBean targetBean ) {
		try {
			AplosAbstractBean resultBean = (AplosAbstractBean) CommonUtil.getNewInstance( getPersistentClass().getTableClass() );
			resultBean.initialiseNewBean();
			resultBean.setInDatabase(false);
			resultBean.setLazilyLoaded(false);
			getField().setAccessible(true);
			getField().set(targetBean, resultBean);
			return resultBean;
		} catch( IllegalAccessException iaex ) {
			ApplicationUtil.handleError( iaex );
		}
		return null;
	}
	
	@Override
	public void addForeignKeysAndIndexes(List<ForeignKeyFieldInfo> foreignKeys,
			List<ColumnIndex> columnIndexes) {
		super.addForeignKeysAndIndexes(foreignKeys, columnIndexes);
		if( !isPolymorphic() ) {
			ColumnIndex columnIndex = new ColumnIndex( this, ColumnIndexType.INDEX );
			columnIndex.setForeignKeyIndex( true );
			columnIndexes.add( columnIndex );
			foreignKeys.add( this );
		}
	}
	
	@Override
	public void setValue( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value ) throws Exception {
    	if( value != null ) {
    		setFieldToPersistentClass( isLoadingFromDatabase, getPersistentClass(), getField(), bean, value);
    	} else {
    		getField().setAccessible(true);
    		getField().set(bean, null);
    	}
    		
	}
	
	@Override
	public boolean isEmpty(AplosAbstractBean aplosAbstractBean) {
		AplosAbstractBean valueBean = (AplosAbstractBean) getValue(aplosAbstractBean);
		if( valueBean == null ) {
			return true;
		} else if( isRemoveEmpty() ) {
			return valueBean.isEmptyBean();
		} else {
			return false;
		}
	}
    
    public static void setFieldToPersistentClass( boolean isLoadingFromDatabase, PersistentClass persistentClass, Field field, AplosAbstractBean bean,  Object value ) throws IllegalAccessException, InstantiationException {
    	Long beanId = null;
		if( value instanceof BigInteger ) {
			beanId = ((BigInteger)value).longValue();
		} else if( value instanceof Long ) {
			beanId = (Long) value;
		} else if( value instanceof AplosAbstractBean ) {
			field.setAccessible(true);
			field.set(bean, value);
			return;
		}
    	AplosAbstractBean aplosAbstractBean = getAplosAbstractBean( isLoadingFromDatabase, persistentClass, beanId );
    	Class<?> tableClass = persistentClass.getTableClass();
    	if( !(tableClass.isInterface() || Modifier.isAbstract( tableClass.getModifiers() )) ) {
			field.setAccessible(true);
			field.set(bean, aplosAbstractBean);
    	} else {
    		((PersistenceBean) bean).getHiddenFieldsMap().put( field.getName(), beanId );
    	}
    }
    
    public static AplosAbstractBean getAplosAbstractBean( boolean isLoadingFromDatabase, PersistentClass persistentClass,  Object value ) throws IllegalAccessException, InstantiationException {
		Long beanId = null;
		if( value instanceof BigInteger ) {
			beanId = ((BigInteger)value).longValue();
		} else if( value instanceof Long ) {
			beanId = (Long) value;
		}

		Class<?> tableClass = persistentClass.getTableClass();
    	if( !(tableClass.isInterface() || Modifier.isAbstract( tableClass.getModifiers() )) ) {
    		AplosAbstractBean aplosAbstractBean = ApplicationUtil.getPersistenceContext().findBean((Class<? extends AplosAbstractBean>)tableClass, beanId);
    		if( aplosAbstractBean == null ) {
				aplosAbstractBean = PersistentClassSelectCriteria.createNewBean( tableClass, isLoadingFromDatabase, beanId );
				/*
				 * Currently causes issues with single table objects like the website object
				 */
//				ApplicationUtil.getPersistenceContext().registerBean(aplosAbstractBean);
    		}
    		return aplosAbstractBean;
    	}
    	return null;
    }
	
	@Override
	public String getForeignKeyName( String ownerTableName ) {
		return ForeignKey.createForeignKeyName(this, ownerTableName );
	}
	
	@Override
	public String getReferencedFieldName() {
		return getPersistentClass().determinePrimaryKeyFieldInfo().getSqlName();
	}
	
	@Override
	public String getReferencedTableName() {
		return getPersistentClass().determineSqlTableName();
	}
	
	@Override
	public void init() {
		if( CommonUtil.isNullOrEmpty( getSqlName() ) ) {
			String sqlName = getField().getName() + "_id";
			if( isPolymorphic ) {
				JoinColumn joinColumn = getField().getAnnotation(JoinColumn.class);
				if( joinColumn != null && !CommonUtil.isNullOrEmpty( joinColumn.name() ) ) {
					sqlName = joinColumn.name();
				}
			} 
			setSqlName( sqlName );
		}
		if( getField() != null && getField().getAnnotation(RemoveEmpty.class) != null ) {
			setRemoveEmpty( true );
		}
		setApplicationType( MySqlType.BIGINT.getNewDbType() );
	}
	
	@Override
	public void addAdditionalFieldInfos(PersistableTable persistableTable) {
		if( isPolymorphic() ) {
			String sqlName = "DTYPE";
			Column metaColumn = getField().getAnnotation(Any.class).metaColumn();
			if( metaColumn != null && !CommonUtil.isNullOrEmpty( metaColumn.name() ) ) {
				sqlName = metaColumn.name();
			}
			setPolymorphicFieldInfo(new PolymorphicFieldInfo( getParentPersistentClass(), this ));
			getPolymorphicFieldInfo().setSqlName(sqlName);
			getPolymorphicFieldInfo().getApplicationType().setNullable(true);
			getPolymorphicFieldInfo().getApplicationType().setColumnSize(ApplicationUtil.getPersistentApplication().getMaxCharLength());
			persistableTable.getFieldInfos().add( getPolymorphicFieldInfo() );
		}
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isPolymorphic() {
		return isPolymorphic;
	}

	public void setPolymorphic(boolean isPolymorphic) {
		this.isPolymorphic = isPolymorphic;
	}

	public PolymorphicFieldInfo getPolymorphicFieldInfo() {
		return polymorphicFieldInfo;
	}

	public void setPolymorphicFieldInfo(PolymorphicFieldInfo polymorphicFieldInfo) {
		this.polymorphicFieldInfo = polymorphicFieldInfo;
	}

	public Set<ForeignFieldInfo> getForeignFieldInfos() {
		return foreignFieldInfos;
	}

	public void setForeignFieldInfos(Set<ForeignFieldInfo> foreignFieldInfos) {
		this.foreignFieldInfos = foreignFieldInfos;
	}

	public boolean isRemoveEmpty() {
		return isRemoveEmpty;
	}

	public void setRemoveEmpty(boolean isRemoveEmpty) {
		this.isRemoveEmpty = isRemoveEmpty;
	} 

}
