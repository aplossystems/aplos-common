package com.aplos.common.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignKeyFieldInfo;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.metadata.MetaTable;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class PersistenceHelperTable {
	private static Logger logger = Logger.getLogger( PersistenceHelperTable.class );
	private Long id;
	private String name;
	private String columns;
	private String foreignKeys;
	private String indexes;
	private boolean isClassDataRefreshed = false;
	
	public PersistenceHelperTable( String name ) {
		setName( name );
	}
	
	public PersistenceHelperTable( ResultSet resultSet ) throws SQLException {
		loadFromResultSet( resultSet );
	}
	
	public void loadFromResultSet( ResultSet resultSet ) throws SQLException {
		setId( resultSet.getLong( "id" ) );
		setName( resultSet.getString( "tableName" ) );
		setColumns( resultSet.getString( "columns" ) );
		setForeignKeys( resultSet.getString( "foreignKeys" ) );
		setIndexes( resultSet.getString( "indexes" ) );
	}
	
	public void refreshClassDataIfRequired( Connection connection ) {
		if( !isClassDataRefreshed() ) {
			try {
				DatabaseMetaData meta = connection.getMetaData();
				ResultSet rs = meta.getTables( null, null, getName(), PersistentApplication.TYPES);
				if( rs.next() ) {
					MetaTable tempMetaTable = new MetaTable( meta, rs );
					
					setForeignKeys( tempMetaTable.getJoinedForeignKeys( false ) );
					setIndexes( tempMetaTable.getJoinedIndexes( false ) );
					setColumns( tempMetaTable.getJoinedColumns() );
					StringBuffer sqlBuf = new StringBuffer();
					sqlBuf.append( "UPDATE persistenceHelperTable SET " );
					sqlBuf.append( " foreignKeys = ?" );
					sqlBuf.append( ", columns = ?" );
					sqlBuf.append( ", indexes = ?" );
					sqlBuf.append( " WHERE tableName = ?" );

					PreparedStatement preparedStatement = connection.prepareStatement(sqlBuf.toString());
					preparedStatement.setString( 1, getForeignKeys() );
					preparedStatement.setString( 2, getColumns() );
					preparedStatement.setString( 3, getIndexes() );
					preparedStatement.setString( 4, getName() );
					preparedStatement.execute();
				}
			}  catch( SQLException sqlEx ) {
				ApplicationUtil.getAplosContextListener().handleError(sqlEx);
			}
			setClassDataRefreshed(true);
		}
	}
	
	public void refreshIndexes( Connection conn ) {
		try {
			DatabaseMetaData meta = conn.getMetaData();
			ResultSet rs = meta.getTables( null, null, getName(), PersistentApplication.TYPES);
			if( rs.next() ) {
				MetaTable tempMetaTable = new MetaTable( meta, rs );
				setIndexes( tempMetaTable.getJoinedIndexes( false ) );
				StringBuffer sqlBuf = new StringBuffer();
				sqlBuf.append( "UPDATE persistenceHelperTable SET " );
				sqlBuf.append( "indexes = '" ).append( getIndexes() ).append( "'" );
				sqlBuf.append( " WHERE tableName = '" ).append( getName() ).append( "'" );
				conn.prepareStatement( sqlBuf.toString() ).execute();
			}
		}  catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError(sqlEx);
		}
	}
	
	public void checkColumns( PersistableTable persistableTable, List<FieldInfo> fullDbFieldInfos, String joinedColumns, Connection conn ) {
		refreshClassDataIfRequired(conn);
		boolean changeMade = false;
		if( getColumns().equals( joinedColumns ) ) {
			changeMade = true;
		} else {
			String[] persistedColumns = getColumns().split( ";" );
			ArrayList<FieldInfo> unmatchedFieldInfos = new ArrayList<FieldInfo>( fullDbFieldInfos );
			boolean matched;
			for( int i = 0, n = persistedColumns.length; i < n; i++ ) {
				matched = false;
				for( int j = 0, p = unmatchedFieldInfos.size(); j < p; j++ ) {
					if( persistedColumns[ i ].equals( unmatchedFieldInfos.get( j ).toString() ) ) {
						matched = true;
						unmatchedFieldInfos.remove( j );
						break;
					} else {
						if( persistedColumns[ i ].split( " " )[ 0 ].equals( unmatchedFieldInfos.get( j ).getSqlName() ) ) {
							try {
								unmatchedFieldInfos.get( j ).alterColumn( persistableTable, getName(), conn );
							} catch( SQLException sqlEx ) {
								ApplicationUtil.handleError(sqlEx, "Editing table " + persistableTable.determineSqlTableName() + " and field " + getName() );
							}
//							ApplicationUtil.getAplosContextListener().handleError( new Exception( "Column names match but other settings are off for " + getName() + " " + persistedColumns[ i ].split( " " )[ 0 ] ), false );
							unmatchedFieldInfos.remove( j );
							matched = true;
							changeMade = true;
							break;
						}
					}
				}
				if( !matched ) {
					ApplicationUtil.getAplosContextListener().handleError( new Exception( "Column needs to be deleted , ALTER TABLE " + getName() + " DROP COLUMN " + persistedColumns[ i ].split( " " )[ 0 ]  ), false );
				}
			}
			for( int i = 0, n = unmatchedFieldInfos.size(); i < n; i++ ) {
				int listIdx = -1;
				for( int j = 0, p = fullDbFieldInfos.size(); j < p; j++ ) {
					if( unmatchedFieldInfos.get( i ).getSqlName().equals( fullDbFieldInfos.get( j ).getSqlName() ) ) {
						listIdx = j;
					}
				}
				try {
					if( listIdx > 0 ) {
						unmatchedFieldInfos.get( i ).addColumnToDb( persistableTable, fullDbFieldInfos.get( listIdx - 1 ), conn );
					} else {
						unmatchedFieldInfos.get( i ).addColumnToDb( persistableTable, null, conn );
					}
				} catch( SQLException sqlEx ) {
					throw new AqlException( sqlEx );
				}
				changeMade = true;
				logger.info( "Column added for " + getName() + " " + unmatchedFieldInfos.get( i ).getSqlName() );
			}
		}

		if( changeMade ) {
			try {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet rs = meta.getTables( null, null, getName(), PersistentApplication.TYPES);
				if( rs.next() ) {
					MetaTable tempMetaTable = new MetaTable( meta, rs );
					StringBuffer sqlBuf = new StringBuffer();
					sqlBuf.append( "UPDATE persistenceHelperTable SET " );
					sqlBuf.append( " columns = ?" );
					sqlBuf.append( " WHERE tableName = ?" );

					PreparedStatement preparedStatement = conn.prepareStatement(sqlBuf.toString());
					preparedStatement.setString( 1, tempMetaTable.getJoinedColumns() );
					preparedStatement.setString( 2, getName() );
					preparedStatement.execute();
				}
			} catch( SQLException sqlEx ) {
				ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
			}
		}
	}
	
	public void checkIndexes( List<ColumnIndex> columnIndexes, String joinedIndexes, Connection conn ) {
		refreshClassDataIfRequired(conn);
		boolean changeMade = false;
		DatabaseMetaData meta = null;
		if( getIndexes().equals( joinedIndexes ) ) {
			changeMade = true;
		} else {
			String[] persistedIndexes = getIndexes().split( ";" );
			String[] persistedIndexesWithNames = null;
			ArrayList<ColumnIndex> unmatchedIndexes = new ArrayList<ColumnIndex>( columnIndexes );
			boolean matched;
			
			for( int i = 0, n = persistedIndexes.length; i < n; i++ ) {
				matched = false;
				for( int j = 0, p = unmatchedIndexes.size(); j < p; j++ ) {
					if( persistedIndexes[ i ].equals( unmatchedIndexes.get( j ).toString() ) ) {
						matched = true;
						unmatchedIndexes.remove( j );
						break;
					}
				}
				if( !matched ) {
					if( persistedIndexesWithNames == null ) {
						try {
							if( meta == null ) {
								meta = conn.getMetaData();
							}
							persistedIndexesWithNames = getPersistedIndexesWithNames( meta, conn );
						} catch( SQLException sqlEx ) {
							ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
						}
					}
					if( persistedIndexes[ i ].split( " " )[ 0 ].equals( persistedIndexesWithNames[ i ].split( " " )[ 1 ] ) ) {
						String indexName = persistedIndexesWithNames[ i ].split( " " )[ 0 ];
						dropIndexFromDb( indexName );
						logger.info( "Index dropped from " + getName() + " " + indexName );
					}
					changeMade = true;
				}
			}
			
			boolean updatePrimary = false;
			for( int i = unmatchedIndexes.size() - 1; i >= 0; i-- ) {
				if( unmatchedIndexes.get( i ).getType().equals( ColumnIndexType.PRIMARY ) ) {
					unmatchedIndexes.remove( i );
					updatePrimary = true; 
				}
			}
			
			if( updatePrimary ) {
				List<String> primaryKeys = new ArrayList<String>();
				for( int i = 0, n = columnIndexes.size(); i < n; i++ ) {
					if( columnIndexes.get( i ).getType().equals( ColumnIndexType.PRIMARY ) ) {
						primaryKeys.add( columnIndexes.get( i ).getColumnName() );
					}
				}
				StringBuffer strBuf = new StringBuffer();
				strBuf.append( "Alter table " );
				strBuf.append( getName() );
				strBuf.append( " drop primary key, add primary key(" );
				strBuf.append( StringUtils.join( primaryKeys.toArray( new String[0] ), "," ) );
				strBuf.append( ")" );
				ApplicationUtil.executeSql( strBuf.toString() );
			}
			
			for( int i = 0, n = unmatchedIndexes.size(); i < n; i++ ) {
				changeMade = true;
				unmatchedIndexes.get( i ).addIndexToDb( getName() );
				logger.info( "Index added for " + getName() + " " + unmatchedIndexes.get( i ).getFieldInfo().getSqlName() );
			}
		}


		if( changeMade ) {
			try {
				if( meta == null ) {
					meta = conn.getMetaData();
				}
				ResultSet rs = meta.getTables( null, null, getName(), PersistentApplication.TYPES);
				if( rs.next() ) {
					MetaTable tempMetaTable = new MetaTable( meta, rs );
					StringBuffer sqlBuf = new StringBuffer();
					sqlBuf.append( "UPDATE persistenceHelperTable SET " );
					sqlBuf.append( " indexes = '" ).append( tempMetaTable.getJoinedIndexes( false ) ).append( "'" );
					sqlBuf.append( " WHERE tableName = '" ).append( getName() ).append( "'" );
					conn.prepareStatement( sqlBuf.toString() ).execute();
				}
			} catch( SQLException sqlEx ) {
				ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
			}
		}
	}
	
	public String[] getPersistedIndexesWithNames( DatabaseMetaData meta, Connection connection ) throws SQLException {
		MetaTable metaTable = new MetaTable( getName() );

		if( meta == null ) {
			meta = connection.getMetaData();
		}
		metaTable.loadIndexes( meta, true );
		return metaTable.getJoinedIndexes( true ).split( ";" );
	}
	
	public void dropIndexFromDb( String indexName ) {
		StringBuffer strBuf = new StringBuffer( "DROP INDEX `" );
		strBuf.append( indexName ).append( "` ON " );
		strBuf.append( getName() );
		ApplicationUtil.executeSql( strBuf.toString() );
	}

	public void checkForeignKeys( List<ForeignKeyFieldInfo> foreignKeys, String joinedForeignKeys, Connection conn ) {
		refreshClassDataIfRequired(conn);
		boolean changeMade = false;
		DatabaseMetaData meta = null;
		if( getForeignKeys().equals( joinedForeignKeys ) ) {
			changeMade = true;
		} else {
			String[] persistedForeignKeys = getForeignKeys().split( ";" );
			String[] persistedForeignKeysWithNames = null;
			ArrayList<ForeignKeyFieldInfo> unmatchedForeignKeys = new ArrayList<ForeignKeyFieldInfo>( foreignKeys );
			boolean matched;
			
			for( int i = 0, n = persistedForeignKeys.length; i < n; i++ ) {
				if( !CommonUtil.isNullOrEmpty( persistedForeignKeys[ i ] ) ) {
					matched = false;
					for( int j = 0, p = unmatchedForeignKeys.size(); j < p; j++ ) {
						if( persistedForeignKeys[ i ].equals( ForeignKey.toString( unmatchedForeignKeys.get( j ) ) ) ) {
							matched = true;
							unmatchedForeignKeys.remove( j );
							break;
						}
					}
					if( !matched ) {
						if( persistedForeignKeysWithNames == null ) {
							MetaTable metaTable = new MetaTable( getName() );
		
							try {
								if( meta == null ) {
									meta = conn.getMetaData();
								}
								metaTable.loadForeignKeys( meta, true );
								persistedForeignKeysWithNames = metaTable.getJoinedForeignKeys( true ).split( ";" );
							} catch( SQLException sqlEx ) {
								ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
							}
						}
						String foreignKeyName = persistedForeignKeysWithNames[ i ].split( " " )[ 0 ];
						dropForeignKeyFromDb( foreignKeyName );
						logger.info( "Foreign key dropped from " + getName() + " " + foreignKeyName );
						changeMade = true;
					}
				}
			}
			for( int i = 0, n = unmatchedForeignKeys.size(); i < n; i++ ) {
				changeMade = true;
				String foreignKeyName = ForeignKey.createForeignKeyName( unmatchedForeignKeys.get( i ), getName() );
				StringBuffer strBuf = new StringBuffer( "ALTER TABLE " );
				strBuf.append( getName() ).append( " ADD CONSTRAINT " ).append( foreignKeyName );
				strBuf.append( " FOREIGN KEY (" ).append( unmatchedForeignKeys.get( i ).getSqlName() );
				strBuf.append( ") REFERENCES " );
				strBuf.append( unmatchedForeignKeys.get( i ).getReferencedTableName() ).append( "(" );
				strBuf.append( unmatchedForeignKeys.get( i ).getReferencedFieldName() ).append( ")" );
				ApplicationUtil.executeSql( strBuf.toString() );
				logger.info( "Foreign key added for " + getName() + " " + unmatchedForeignKeys.get( i ).getSqlName() );
			}
		}

		if( changeMade ) {
			try {
				if( meta == null ) {
					meta = conn.getMetaData();
				}
				ResultSet rs = meta.getTables( null, null, getName(), PersistentApplication.TYPES);
				if( rs.next() ) {
					MetaTable tempMetaTable = new MetaTable( meta, rs );
					StringBuffer sqlBuf = new StringBuffer();
					sqlBuf.append( "UPDATE persistenceHelperTable SET " );
					sqlBuf.append( " foreignKeys = '" ).append( tempMetaTable.getJoinedForeignKeys( false ) ).append( "'" );
					sqlBuf.append( " WHERE tableName = '" ).append( getName() ).append( "'" );
					conn.prepareStatement( sqlBuf.toString() ).execute();
				}
				refreshIndexes( conn );
			} catch( SQLException sqlEx ) {
				ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
			}
		}
	}
	
	public void dropForeignKeyFromDb( String foreignKeyName ) {
		StringBuffer strBuf = new StringBuffer( "ALTER TABLE " );
		strBuf.append( getName() ).append( " DROP FOREIGN KEY " );
		strBuf.append( foreignKeyName );
		ApplicationUtil.executeSql( strBuf.toString() );
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(String foreignKeys) {
		this.foreignKeys = foreignKeys;
	}

	public String getIndexes() {
		return indexes;
	}

	public void setIndexes(String indexes) {
		this.indexes = indexes;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isClassDataRefreshed() {
		return isClassDataRefreshed;
	}

	public void setClassDataRefreshed(boolean isClassDataRefreshed) {
		this.isClassDataRefreshed = isClassDataRefreshed;
	}
}
