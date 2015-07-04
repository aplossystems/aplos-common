package com.aplos.common.persistence.metadata;

import com.aplos.common.persistence.fieldinfo.ForeignKeyFieldInfo;

public class ForeignKey implements ForeignKeyFieldInfo {
	private String sqlName;
	private String foreignKeyName;
	private String referencedTableName;
	private String referencedFieldName;
	private String ownerTableName;
	
	public ForeignKey( String foreignKeyName, String sqlName, String referencedTableName, String referencedFieldName, String ownerTableName ) {
		this.foreignKeyName = foreignKeyName;
		this.sqlName = sqlName;
		this.referencedTableName = referencedTableName;
		this.referencedFieldName = referencedFieldName;
		this.ownerTableName = ownerTableName;
	}
	
	@Override
	public String toString() {
		return toString( this );
	}
	
	public static String toStringWithForeignKeyName( ForeignKeyFieldInfo foreignKeyFieldInfo, String ownerTableName ) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( foreignKeyFieldInfo.getForeignKeyName( ownerTableName ) ).append( " " );
		strBuf.append( toString( foreignKeyFieldInfo ) );
		return strBuf.toString();
	}
	
	public static String toString( ForeignKeyFieldInfo foreignKeyFieldInfo ) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( foreignKeyFieldInfo.getSqlName().toLowerCase() ).append( " " );
		strBuf.append( foreignKeyFieldInfo.getReferencedTableName().toLowerCase() ).append( " " );
		strBuf.append( foreignKeyFieldInfo.getReferencedFieldName().toLowerCase() );
		return strBuf.toString();
	}
	
	public static String createForeignKeyName( ForeignKeyFieldInfo foreignKeyFieldInfo, String ownerTableName ) {
		StringBuffer foreignKeyName = new StringBuffer( "FK" );
		foreignKeyName.append( Integer.toHexString( ownerTableName.toLowerCase().hashCode() ) );
		foreignKeyName.append( Integer.toHexString( foreignKeyFieldInfo.getReferencedFieldName().hashCode() + foreignKeyFieldInfo.getSqlName().hashCode() ) );
		return foreignKeyName.toString().toUpperCase();
	}
	
	@Override
	public String getReferencedFieldName() {
		return referencedFieldName;
	}
	@Override
	public String getReferencedTableName() {
		return referencedTableName;
	}
	@Override
	public String getSqlName() {
		return sqlName;
	}

	public String getForeignKeyName( String ownerTableName ) {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public String getOwnerTableName() {
		return ownerTableName;
	}

	public void setOwnerTableName(String ownerTableName) {
		this.ownerTableName = ownerTableName;
	}
}
