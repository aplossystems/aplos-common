package com.aplos.common.persistence.fieldinfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.type.applicationtype.MySqlType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class PersistentCollectionFieldInfo extends CollectionFieldInfo implements ForeignKeyFieldInfo {
	private PersistentClass persistentClass;
	private PolymorphicFieldInfo polymorphicFieldInfo;
	private boolean isPolymorphic = false;
	
	public PersistentCollectionFieldInfo( PersistentClass parentPersistentClass, Field field, PersistentClass persistentClass, Class<?> fieldType, Annotation relationshipAnnotation ) {
		super( parentPersistentClass, field, fieldType, relationshipAnnotation);
		setPersistentClass(persistentClass);
		if( field.getAnnotation( ManyToAny.class ) != null ) {
			setPolymorphic(true);
		}
	}
	
	@Override
	public void markAsPrimaryOrUnique(JunctionTable collectionTable) {
		super.markAsPrimaryOrUnique(collectionTable);
		if( getRelationshipAnnotation() instanceof OneToMany ) {
			this.setUnique( true );
		}
	}
	
	@Override
	public void addForeignKeysAndIndexes( List<ForeignKeyFieldInfo> foreignKeys,
			List<ColumnIndex> columnIndexes) {
		super.addForeignKeysAndIndexes( foreignKeys, columnIndexes);
		if( !isPolymorphic() ) {
			ColumnIndex columnIndex = new ColumnIndex( this, ColumnIndexType.INDEX );
			columnIndex.setForeignKeyIndex( true );
			columnIndexes.add(columnIndex);
			foreignKeys.add( this );
		}
	}
	
	@Override
	public String getForeignKeyName( String ownerTableName ) {
		return ForeignKey.createForeignKeyName(this,ownerTableName);
	}
	
	@Override
	public String getReferencedTableName() {
		return getPersistentClass().determineSqlTableName();
	}

	@Override
	public String getReferencedFieldName() {
		return getPersistentClass().determinePrimaryKeyFieldInfo().getSqlName();
	}
	
	@Override
	public void addMapKeyFieldInfo( List<FieldInfo> fieldInfoList, Class<?> mapKeyClass, String customMapKeyName ) {
		if( !CommonUtil.isNullOrEmpty( customMapKeyName ) ) {
			boolean isFieldInClass = false;
			Field[] fields = persistentClass.getTableClass().getDeclaredFields();
			for( int i = 0, n = fields.length; i < n; i++ ) {
				if( customMapKeyName.equals( fields[ i ].getName() ) ) {
					isFieldInClass = true;
					break;
				}
			}
			if( !isFieldInClass ) {
				super.addMapKeyFieldInfo( fieldInfoList, mapKeyClass, customMapKeyName );
			}
		} else {
			super.addMapKeyFieldInfo( fieldInfoList, mapKeyClass, customMapKeyName );
		}
	}
	
	@Override
	public void init() {
		if( CommonUtil.isNullOrEmpty(getSqlName()) ) {
			String sqlName = readSqlNameFromJoinTable();
			if( CommonUtil.isNullOrEmpty( sqlName ) ) {
				sqlName = getField().getName() + "_id";
			}
			setSqlName( sqlName );
		}
		setApplicationType( MySqlType.BIGINT.getNewDbType() );
		getApplicationType().setNullable( false );
		setPersistentCollectionType( CollectionFieldInfo.collectionMap.get( getField().getType() ) );
	}
	
	@Override
	public void addAdditionalFieldInfos(PersistableTable persistableTable) {
		super.addAdditionalFieldInfos(persistableTable);
		if( isPolymorphic ) {
			String sqlName = getField().getName() + "_type";
			ManyToAny manyToAnyColumn = getField().getAnnotation(ManyToAny.class);
			if( manyToAnyColumn != null ) {
				if( manyToAnyColumn.metaColumn() != null 
						&& !CommonUtil.isNullOrEmpty( manyToAnyColumn.metaColumn().name() ) ) {
					sqlName = manyToAnyColumn.metaColumn().name(); 
				}
			}

			if( getPolymorphicFieldInfo() == null ) {
				polymorphicFieldInfo = new PolymorphicFieldInfo( getParentPersistentClass(), this );
				polymorphicFieldInfo.setSqlName( sqlName );
				polymorphicFieldInfo.getApplicationType().setNullable(true);
				polymorphicFieldInfo.getApplicationType().setColumnSize(ApplicationUtil.getPersistentApplication().getMaxCharLength());
			}
			persistableTable.getFieldInfos().add( polymorphicFieldInfo );
		} 
	}
	
	@Override
	public void setPrimaryKey(boolean isPrimaryKey) {
		if( polymorphicFieldInfo != null ) {
			polymorphicFieldInfo.setPrimaryKey( isPrimaryKey );
		}
		super.setPrimaryKey(isPrimaryKey);
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
}
