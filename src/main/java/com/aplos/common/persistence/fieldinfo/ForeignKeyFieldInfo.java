package com.aplos.common.persistence.fieldinfo;

public interface ForeignKeyFieldInfo {
	public String getForeignKeyName( String ownerTableName );
	public String getSqlName();
	public String getReferencedTableName();
	public String getReferencedFieldName();
}
