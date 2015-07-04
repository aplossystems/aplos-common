package com.aplos.common.persistence.fieldinfo;

import java.util.List;

import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;


public class JoinedIdFieldInfo extends FieldInfo implements ForeignKeyFieldInfo {
	private FieldInfo baseIdFieldInfo;
	
	public JoinedIdFieldInfo( PersistentClass parentPersistentClass, FieldInfo baseIdFieldInfo ) {
		super( parentPersistentClass, baseIdFieldInfo.getField() );
		setBaseIdFieldInfo(baseIdFieldInfo);
		setPrimaryKey( true );
	}
	
	@Override
	public void addForeignKeysAndIndexes(List<ForeignKeyFieldInfo> foreignKeys,
			List<ColumnIndex> columnIndexes) {
		super.addForeignKeysAndIndexes(foreignKeys, columnIndexes);
		foreignKeys.add( this );
		columnIndexes.add( new ColumnIndex( this, ColumnIndexType.INDEX ) );
	}
	
	public String determineFieldMapKeyName() {
		return getSqlName();
	}
	
	@Override
	public String getReferencedFieldName() {
		return getBaseIdFieldInfo().getSqlName();
	}
	
	@Override
	public String getForeignKeyName( String ownerTableName ) {
		return ForeignKey.createForeignKeyName(this,ownerTableName);
	}
	
	@Override
	public String getReferencedTableName() {
		return getParentPersistentClass().getParentPersistentClass().determineSqlTableName();
	}
	
	@Override
	public String getSqlName() {
		return getBaseIdFieldInfo().getSqlName();
	}
	
	@Override
	public ApplicationType getApplicationType() {
		return getBaseIdFieldInfo().getApplicationType();
	}

	public FieldInfo getBaseIdFieldInfo() {
		return baseIdFieldInfo;
	}

	public void setBaseIdFieldInfo(FieldInfo baseIdFieldInfo) {
		this.baseIdFieldInfo = baseIdFieldInfo;
	}
}
