package com.aplos.common.persistence.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.utils.ApplicationUtil;

public class MetaTable {
	private String name;
	private List<MetaColumn> columns = new ArrayList<>();
	private List<ForeignKey> foreignKeys = new ArrayList<>();
	private List<ColumnIndex> columnIndexes = new ArrayList<>();
	private static final int INDEX_NAME = 6;
	private static final int PKTABLE_NAME = 3;
	private static final int PKCOLUMN_NAME = 4;
	private static final int FKCOLUMN_NAME = 8;
	private static final int FK_NAME = 12;
	private static final int NON_UNIQUE = 4;
	
	private static final int COLUMN_NAME = 9;
	
	public MetaTable() {
	}
	
	public MetaTable( String name ) {
		this.name = name;
	}
	
	public MetaTable( DatabaseMetaData meta, ResultSet rs ) throws SQLException  {
		this( meta, rs, true );
	}
	
	public MetaTable( DatabaseMetaData meta, ResultSet rs, boolean sort ) throws SQLException  {
		name = rs.getString( "TABLE_NAME" );
		
		rs = meta.getColumns( null, null, name, null );
		// This is to stop the duplicates that started happening on the new server
		Set<MetaColumn> columnSet = new HashSet<>();
		while( rs.next() ) {
			columnSet.add( new MetaColumn( meta, rs ) );
		}
		
		loadForeignKeys( meta, sort );
		loadIndexes( meta, sort );

		columns = new ArrayList<>(columnSet);
		if( sort ) {
			Collections.sort( columns, new MetaColumnComparator() );
		}
	}
	
	public void loadIndexes( DatabaseMetaData meta, boolean sort ) throws SQLException {
		ResultSet rs = meta.getIndexInfo(null, null, name, false, true);
		ColumnIndex tempColumnIndex;
		try {
			ColumnIndexType tempColumnIndexType;
			while( rs.next() ) {
//				for( int i = 1, n = 14; i < n; i++ ) {
//					System.out.print( rs.getString( i ) + " " );
//				}
//				System.out.println();
				if( rs.getBoolean( NON_UNIQUE ) ) {
					tempColumnIndexType = ColumnIndexType.INDEX;
				} else {
					if( rs.getString( INDEX_NAME ).equals( "PRIMARY" ) ) {
						tempColumnIndexType = ColumnIndexType.PRIMARY;
						for( int i = 0, n = getColumns().size(); i < n; i++ ) {
							if( getColumns().get( i ).getName().equalsIgnoreCase( rs.getString( COLUMN_NAME ) ) ) {
								getColumns().get( i ).setPrimaryKey( true );
								break;
							}
						}
					} else {
						tempColumnIndexType = ColumnIndexType.UNIQUE;
					}
				}
				tempColumnIndex = new ColumnIndex( rs.getString( INDEX_NAME ), rs.getString( COLUMN_NAME ), tempColumnIndexType );
				columnIndexes.add( tempColumnIndex );
			}
		} catch( IllegalArgumentException iaEx ) {
			ApplicationUtil.getAplosContextListener().handleError( iaEx );
		}
		
		if( sort ) {
			Collections.sort( getColumnIndexes(), new ColumnIndexComparator() );
		}
	}
	
	public void loadForeignKeys( DatabaseMetaData meta, boolean sort ) throws SQLException {
		ResultSet rs = meta.getImportedKeys(null, null, name);
		while( rs.next() ) {
			getForeignKeys().add( new ForeignKey( rs.getString( FK_NAME ), rs.getString( FKCOLUMN_NAME ), rs.getString( PKTABLE_NAME ), rs.getString( PKCOLUMN_NAME ), getName() )  );
		}
		
		if( sort ) {
			Collections.sort( getForeignKeys(), new ForeignKeyFieldInfoComparator() );
		}
	}
	
	public String getJoinedColumns() {
		StringBuffer strBuf = new StringBuffer();
		for( int i = 0, n = columns.size(); i < n; i++ ) {
			strBuf.append( columns.get( i ).toString() + ";" );
		}
		return strBuf.toString();
	}
	
	public String getJoinedForeignKeys( boolean includeForeignKeyNames ) {
		StringBuffer strBuf = new StringBuffer();
		for( int i = 0, n = getForeignKeys().size(); i < n; i++ ) {
			if( includeForeignKeyNames ) {
				strBuf.append( ForeignKey.toStringWithForeignKeyName( getForeignKeys().get( i ), getName() ) );
			} else {
				strBuf.append( ForeignKey.toString( getForeignKeys().get( i ) ) );
			}
			strBuf.append( ";" );
		}
		return strBuf.toString();
	}
	
	public String getJoinedIndexes( boolean includeIndexNames ) {
		StringBuffer strBuf = new StringBuffer();
		for( int i = 0, n = getColumnIndexes().size(); i < n; i++ ) {
			strBuf.append( getColumnIndexes().get( i ).toString( includeIndexNames, getName() ) + ";" );
		}
		return strBuf.toString();
	}

	@Override
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		// Keep space so that the split doesn't create a smaller array.
		strBuf.append( getName() ).append( "^ " );
		strBuf.append( getJoinedColumns() );
		strBuf.append( getName() ).append( "^ " );
		strBuf.append( getForeignKeys() );
		strBuf.append( getName() ).append( "^ " );
		strBuf.append( getJoinedIndexes( false ) );
		
		return strBuf.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<MetaColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<MetaColumn> columns) {
		this.columns = columns;
	}

	public List<ColumnIndex> getColumnIndexes() {
		return columnIndexes;
	}

	public void setColumnIndexes(List<ColumnIndex> columnIndexes) {
		this.columnIndexes = columnIndexes;
	}

	public List<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(List<ForeignKey> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}
	
	public class MetaColumnComparator implements Comparator<MetaColumn> {
		@Override
		public int compare(MetaColumn o1, MetaColumn o2) {
			if( o1.isPrimaryKey() && !o2.isPrimaryKey() ) {
				return -1;
			} else if( !o1.isPrimaryKey() && o2.isPrimaryKey() ) {
				return 1;
			} else {
				return o1.getName().toLowerCase().compareTo( o2.getName().toLowerCase() );
			}
		}
	}
}
