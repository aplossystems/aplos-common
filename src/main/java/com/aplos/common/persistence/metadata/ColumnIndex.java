package com.aplos.common.persistence.metadata;

import com.aplos.common.LabeledEnumInter;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignKeyFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ColumnIndex {

	public enum ColumnIndexType implements LabeledEnumInter {
		PRIMARY ( "PRIMARY" ),  
		INDEX ( "INDEX" ),
		UNIQUE ( "UNIQUE" );
		
		private String label;
		
		private ColumnIndexType( String label ) {
			this.label = label;
		}
		
		public String getLabel() {
			return label;
		};
	}
	private ColumnIndexType type;
	private FieldInfo fieldInfo;
	private List<String> columnNames = new ArrayList<>();
	private String indexName;
	private boolean isForeignKeyIndex = false;
	
	public ColumnIndex( String indexName, String columnName, ColumnIndexType columnIndexType ) {
		setIndexName( indexName );
		this.type = columnIndexType;
		this.getColumnNames().clear();
		this.getColumnNames().add(columnName);
	}
	
	public ColumnIndex( FieldInfo fieldInfo, ColumnIndexType columnIndexType ) {
		this.setFieldInfo(fieldInfo);
		this.type = columnIndexType;
		this.getColumnNames().clear();
		this.getColumnNames().add(fieldInfo.getSqlName());
		String indexName = getColumnName();
		if( getType().equals( ColumnIndexType.UNIQUE ) ) {
			indexName = createUniqueIndexName( indexName ); 
		}
		setIndexName( indexName );
	}
	
	public static String createUniqueIndexName( String columnName ) {
		return columnName + "_Unique";
	}
	
	public String toString( boolean includeIndexName, String ownerTableName ) {
		StringBuffer strBuf = new StringBuffer();
		if( includeIndexName ) {
			strBuf.append( determineIndexName( ownerTableName ) ).append( " " );
		}
		strBuf.append( getColumnName() );
		strBuf.append( " " ).append( getType().getLabel() );
		return strBuf.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( getColumnName() );
		strBuf.append( " " ).append( getType().getLabel() );
		return strBuf.toString();
	}
	
	public void addIndexToDb( String ownerTableName ) {
		StringBuffer strBuf = new StringBuffer( "CREATE " );
		if( getType().equals( ColumnIndexType.UNIQUE ) ) {
			strBuf.append( "UNIQUE " );
		} 
		strBuf.append( "INDEX " ).append( determineIndexName( ownerTableName ) ).append( " ON " );
		strBuf.append( ownerTableName );
		strBuf.append( " (" ).append(StringUtils.join(this.columnNames, ",")).append( ")" );
		ApplicationUtil.executeSql( strBuf.toString() );
	}
	
	public String determineIndexName( String ownerTableName ) {
		if( isForeignKeyIndex() ) {
			return ForeignKey.createForeignKeyName((ForeignKeyFieldInfo)getFieldInfo(), ownerTableName);
		} else {
			return indexName;
		}
	}
	
	public ColumnIndexType getType() {
		return type;
	}
	public void setType(ColumnIndexType type) {
		this.type = type;
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	public void setFieldInfo(FieldInfo fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public String getColumnName() {
		if(getColumnNames().size() > 0) {
			return getColumnNames().get(0);
		} else {
			return null;
		}
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public boolean isForeignKeyIndex() {
		return isForeignKeyIndex;
	}

	public void setForeignKeyIndex(boolean isForeignKeyIndex) {
		this.isForeignKeyIndex = isForeignKeyIndex;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(List<String> columnNames) {
		this.columnNames = columnNames;
	}
}
