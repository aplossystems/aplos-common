package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;
import java.util.List;

import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.type.applicationtype.MySqlType;

public class PersistentMapKeyFieldInfo extends MapKeyFieldInfo implements ForeignKeyFieldInfo {
	private PersistentClass persistentClass;
	
	public PersistentMapKeyFieldInfo( PersistentClass persistentClass, CollectionFieldInfo collectionFieldInfo
			, Class<?> fieldClass, PersistentClass parentPersistentClass, Field field) {
		super(collectionFieldInfo, fieldClass, parentPersistentClass, field);
		setPersistentClass(persistentClass);
	}
	
	@Override
	public void addForeignKeysAndIndexes(List<ForeignKeyFieldInfo> foreignKeys,
			List<ColumnIndex> columnIndexes) {
		super.addForeignKeysAndIndexes(foreignKeys, columnIndexes);
		columnIndexes.add( new ColumnIndex( this, ColumnIndexType.INDEX ) );
		foreignKeys.add( this );
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
	
	public String determineFieldMapKeyName() {
		return getSqlName();
	}
	
	@Override
	public void init() {
		setSqlName( "mapkey_id" );
		setApplicationType( MySqlType.BIGINT.getNewDbType() );
		getApplicationType().setNullable(false);
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}
}
