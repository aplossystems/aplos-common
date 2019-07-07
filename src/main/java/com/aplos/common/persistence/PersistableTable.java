package com.aplos.common.persistence;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignKeyFieldInfo;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.metadata.ColumnIndexComparator;
import com.aplos.common.persistence.metadata.FieldInfoComparator;
import com.aplos.common.persistence.metadata.ForeignKey;
import com.aplos.common.persistence.metadata.ForeignKeyFieldInfoComparator;
import com.aplos.common.utils.ApplicationUtil;

public abstract class PersistableTable {
	private static Logger logger = Logger.getLogger( PersistableTable.class );
	private List<ForeignKeyFieldInfo> foreignKeys = new ArrayList<ForeignKeyFieldInfo>();
	private List<ColumnIndex> columnIndexes = new ArrayList<ColumnIndex>();
	private List<FieldInfo> fullDbFieldInfos = new ArrayList<FieldInfo>();
	private List<FieldInfo> fullDbFieldInfosWithoutId = new ArrayList<FieldInfo>();
	private Set<FieldInfo> embeddedFieldInfos = new HashSet<FieldInfo>();

	/*
	 * These are all the columns of the database so consists of the fieldInfoMap
	 * minus any fields that aren't in the database.
	 */
	private Map<String, FieldInfo> fullDbFieldInfoMap;
	private Map<String, FieldInfo> fullNonDbFieldInfoMap = new HashMap<String,FieldInfo>();
	/*
	 * These are all the columns of the database for this particular class only
	 */
	private Map<String, FieldInfo> classDbFieldInfoMap;
	/*
	 * These are all the columns of the database for this particular class only including their aliases
	 */
	private Map<String, FieldInfo> classAliasedFieldInfoMap;
	/*
	 * A map of all the fields including their aliases
	 */
	private Map<String,FieldInfo> aliasedFieldInfoMap = new HashMap<String,FieldInfo>();
	private FieldInfo primaryKeyFieldInfo;
	private Method primaryKeyGetterMethod;
	private Method primaryKeySetterMethod;
	private String cachedJoinedColumnStr;
	private String cachedJoinedForeignKeyStr;
	private String cachedJoinedIndexStr;
	private boolean isDbInitialised = false;

	public List<ForeignKeyFieldInfo> getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(List<ForeignKeyFieldInfo> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}

	public List<ColumnIndex> getColumnIndexes() {
		return columnIndexes;
	}

	public void setColumnIndexes(List<ColumnIndex> columnIndexes) {
		this.columnIndexes = columnIndexes;
	}

	public abstract List<FieldInfo> getFieldInfos();
	
	public void updatePersistentHelperTable() {
		StringBuffer sqlBuf = new StringBuffer();
		sqlBuf.append( "UPDATE persistenceHelperTable SET " );
		sqlBuf.append( " columns = '" ).append( getCachedJoinedColumnStr() ).append( "'" );
		sqlBuf.append( ", indexes = '" ).append( getCachedJoinedIndexStr() ).append( "'" );
		sqlBuf.append( ", foreignKeys = '" ).append( getCachedJoinedForeignKeyStr() ).append( "'" );
		sqlBuf.append( " WHERE tableName = '" ).append( determineSqlTableName() ).append( "'" );
		ApplicationUtil.executeSql( sqlBuf.toString() );
	}
	
	public void initialiseDbInformation() {
		if( !isDbInitialised ) {
			logger.debug( determineSqlTableName() + " initialise information");
			createDbFieldInfoLists();
			setFullDbFieldInfos( new ArrayList<FieldInfo>( getFullDbFieldInfoMap().values() ) );
			createForeignKeysAndIndexes();
			
			Collections.sort( getFullDbFieldInfos(), new FieldInfoComparator() );
			Collections.sort( getForeignKeys(), new ForeignKeyFieldInfoComparator() );
			Collections.sort( getColumnIndexes(), new ColumnIndexComparator() );
	
			StringBuffer strBuf = new StringBuffer();
			for( int i = 0, n = fullDbFieldInfos.size(); i < n; i++ ) {
				strBuf.append( fullDbFieldInfos.get( i ).toString() + ";" );
			}
			setFullDbFieldInfosWithoutId( new ArrayList<FieldInfo>( getFullDbFieldInfos() ) );
			getFullDbFieldInfosWithoutId().remove(determinePrimaryKeyFieldInfo());
			setCachedJoinedColumnStr(strBuf.toString());
	
			strBuf = new StringBuffer();
			for( int i = 0, n = getForeignKeys().size(); i < n; i++ ) {
				strBuf.append( ForeignKey.toString( getForeignKeys().get( i ) ) + ";" );
			}
			setCachedJoinedForeignKeyStr( strBuf.toString() );
	
			strBuf = new StringBuffer();
			for( int i = 0, n = getColumnIndexes().size(); i < n; i++ ) {
				strBuf.append( getColumnIndexes().get( i ).toString() + ";" );
			}
			setCachedJoinedIndexStr( strBuf.toString() );
			isDbInitialised = true;
		}
	}
	
	public abstract void createDbFieldInfoLists();
	
	public abstract FieldInfo determinePrimaryKeyFieldInfo();
	
	public abstract String determineSqlTableName();
	
	public abstract void createForeignKeysAndIndexes();
	
	public FieldInfo getFieldInfoFromSqlName(String sqlName,boolean searchAllClasses) {
		return getAliasedFieldInfoMap().get( sqlName );
	}
	
	public abstract void addFieldInfoToMap( Map<String, FieldInfo> fullDbFieldInfos, String mapKey, FieldInfo fieldInfo );
	
	public boolean confirmState( PersistenceHelperTable persistenceHelperTable, Connection conn ) {
		if( !persistenceHelperTable.getName().equals( determineSqlTableName().toLowerCase() ) ) {
			return false;
		}
		initialiseDbInformation();
		
		if( !persistenceHelperTable.getColumns().equals( getCachedJoinedColumnStr() ) ) {
			persistenceHelperTable.checkColumns( this, fullDbFieldInfos, getCachedJoinedColumnStr(), conn );
		}
		
		if( !persistenceHelperTable.getForeignKeys().equals( getCachedJoinedForeignKeyStr() ) ) {
			persistenceHelperTable.checkForeignKeys( getForeignKeys(), getCachedJoinedForeignKeyStr(), conn );
		}
		
		if( !persistenceHelperTable.getIndexes().equals( getCachedJoinedIndexStr() ) ) {
			persistenceHelperTable.checkIndexes( getColumnIndexes(), getCachedJoinedIndexStr(), conn );
		}
		return true;
	}
	
	public List<String> createTable( boolean isReturningKeysToAddSql, Connection conn ) throws SQLException {
		String tableName = determineSqlTableName(); 
		StringBuffer strBuf = new StringBuffer( "CREATE TABLE ");
		strBuf.append( tableName ).append( " (\n" );
		initialiseDbInformation();
		
		for( int i = 0, n = fullDbFieldInfos.size(); i < n; i++ ) {
			fullDbFieldInfos.get( i ).appendCreateTableStr( strBuf, this, fullDbFieldInfos.get( i ).isPrimaryKey() );
			strBuf.append( ",\n" );
		}
		List<String> keysToAddSqlList = null;
		List<String> primaryKeyColumnNames = new ArrayList<String>();
		if( isReturningKeysToAddSql ) {
			keysToAddSqlList = new ArrayList<String>();
			StringBuffer keysToAddStrBuf;
			for( int i = 0, n = getColumnIndexes().size(); i < n; i++ ) {
				if( getColumnIndexes().get( i ).getType().equals( ColumnIndexType.PRIMARY ) ) {
					primaryKeyColumnNames.add( getColumnIndexes().get( i ).getColumnName() );
				} else { 
					if( !getColumnIndexes().get( i ).isForeignKeyIndex() ) {
						keysToAddStrBuf = new StringBuffer();
						keysToAddStrBuf.append( "CREATE" );
						if( getColumnIndexes().get( i ).getType().equals( ColumnIndexType.UNIQUE ) ) {
							keysToAddStrBuf.append( " UNIQUE" );
						}
						keysToAddStrBuf.append( " INDEX " );
						keysToAddStrBuf.append( getColumnIndexes().get( i ).determineIndexName( tableName ) );
						keysToAddStrBuf.append( " ON " );
						keysToAddStrBuf.append( determineSqlTableName() );
						keysToAddStrBuf.append( " (`" );
						keysToAddStrBuf.append( getColumnIndexes().get( i ).getColumnName() );
						keysToAddStrBuf.append( "`);" );
						keysToAddSqlList.add( keysToAddStrBuf.toString() );
					}
				}
			}
			
			for( int i = 0, n = getForeignKeys().size(); i < n; i++ ) {
				keysToAddStrBuf = new StringBuffer();
				keysToAddStrBuf.append( "ALTER TABLE " );
				keysToAddStrBuf.append( tableName );
				keysToAddStrBuf.append( " ADD CONSTRAINT " );
				keysToAddStrBuf.append( ForeignKey.createForeignKeyName( getForeignKeys().get( i ), determineSqlTableName() ) );
				keysToAddStrBuf.append( " FOREIGN KEY ( " ).append( getForeignKeys().get( i ).getSqlName() );
				keysToAddStrBuf.append( ") REFERENCES " );
				keysToAddStrBuf.append( getForeignKeys().get( i ).getReferencedTableName() );
				keysToAddStrBuf.append( " (" ).append( getForeignKeys().get( i ).getReferencedFieldName() );
				keysToAddStrBuf.append( ");" );
				keysToAddSqlList.add( keysToAddStrBuf.toString() );
			}
		} else {
			for( int i = 0, n = getColumnIndexes().size(); i < n; i++ ) {
				if( getColumnIndexes().get( i ).getType().equals( ColumnIndexType.PRIMARY ) ) {
					primaryKeyColumnNames.add( getColumnIndexes().get( i ).getColumnName() );
				} else { 
					if( getColumnIndexes().get( i ).getType().equals( ColumnIndexType.UNIQUE ) ) {
						strBuf.append( "UNIQUE " );
					}
					strBuf.append( "KEY `" );
					strBuf.append( getColumnIndexes().get( i ).determineIndexName( tableName ) );
					strBuf.append( "` (`" );
					strBuf.append( getColumnIndexes().get( i ).getColumnName() );
					strBuf.append( "`),\n" );
				}
			}
			
			for( int i = 0, n = getForeignKeys().size(); i < n; i++ ) {
				strBuf.append( "CONSTRAINT `" );
				strBuf.append( ForeignKey.createForeignKeyName( getForeignKeys().get( i ), determineSqlTableName() ) );
				strBuf.append( "` FOREIGN KEY (`" );
				strBuf.append( getForeignKeys().get( i ).getSqlName() );
				strBuf.append( "`) REFERENCES `" );
				strBuf.append( getForeignKeys().get( i ).getReferencedTableName() );
				strBuf.append( "` (`" ).append( getForeignKeys().get( i ).getReferencedFieldName() );
				strBuf.append( "`),\n" );
			}
		}
		
		if( primaryKeyColumnNames.size() > 0 ) {
			strBuf.append( "PRIMARY KEY (`" );
			strBuf.append( StringUtils.join( primaryKeyColumnNames, "`,`" ) );
			strBuf.append( "`),\n" );
		}

		strBuf.replace( strBuf.length() - 2, strBuf.length() - 1, "" );
		strBuf.append( ") ENGINE=" ).append( ApplicationUtil.getPersistentApplication().getDbEngineName() );
		strBuf.append( "  DEFAULT CHARSET=" ).append( ApplicationUtil.getPersistentApplication().getDbCharSet() );
		logger.debug( strBuf.toString() );
		ApplicationUtil.executeSql( strBuf.toString(), conn );
		
		if( !isReturningKeysToAddSql ) {
			PersistentApplication.createPersistenceHelperTable( determineSqlTableName(), conn );
		}
		
		return keysToAddSqlList;
	}

	public void updatePrimaryKeyFieldInfo(FieldInfo primaryKeyFieldInfo) {
		setPrimaryKeyFieldInfo( primaryKeyFieldInfo );
		try {
			setPrimaryKeyGetterMethod( primaryKeyFieldInfo.getField().getDeclaringClass().getDeclaredMethod( "getId" ) );
			setPrimaryKeySetterMethod( primaryKeyFieldInfo.getField().getDeclaringClass().getDeclaredMethod( "setId", Long.class ) );
		} catch( NoSuchMethodException nsmEx ) {
			ApplicationUtil.handleError( nsmEx );
		}
	}

	public List<FieldInfo> getFullDbFieldInfos() {
		return fullDbFieldInfos;
	}

	public void setFullDbFieldInfos(List<FieldInfo> fullDbFieldInfos) {
		this.fullDbFieldInfos = fullDbFieldInfos;
	}

	public String getCachedJoinedColumnStr() {
		return cachedJoinedColumnStr;
	}

	public void setCachedJoinedColumnStr(String cachedJoinedColumnStr) {
		this.cachedJoinedColumnStr = cachedJoinedColumnStr;
	}

	public String getCachedJoinedForeignKeyStr() {
		return cachedJoinedForeignKeyStr;
	}

	public void setCachedJoinedForeignKeyStr(String cachedJoinedForeignKeyStr) {
		this.cachedJoinedForeignKeyStr = cachedJoinedForeignKeyStr;
	}

	public String getCachedJoinedIndexStr() {
		return cachedJoinedIndexStr;
	}

	public void setCachedJoinedIndexStr(String cachedJoinedIndexStr) {
		this.cachedJoinedIndexStr = cachedJoinedIndexStr;
	}

	public List<FieldInfo> getFullDbFieldInfosWithoutId() {
		return fullDbFieldInfosWithoutId;
	}

	public void setFullDbFieldInfosWithoutId(
			List<FieldInfo> fullDbFieldInfosWithoutId) {
		this.fullDbFieldInfosWithoutId = fullDbFieldInfosWithoutId;
	}

	public FieldInfo getPrimaryKeyFieldInfo() {
		return primaryKeyFieldInfo;
	}

	private void setPrimaryKeyFieldInfo(FieldInfo primaryKeyFieldInfo) {
		this.primaryKeyFieldInfo = primaryKeyFieldInfo;
	}

	public Map<String, FieldInfo> getFullDbFieldInfoMap() {
		return fullDbFieldInfoMap;
	}

	public void setFullDbFieldInfoMap(Map<String, FieldInfo> fullDbFieldInfoMap) {
		this.fullDbFieldInfoMap = fullDbFieldInfoMap;
	}

	public Method getPrimaryKeyGetterMethod() {
		return primaryKeyGetterMethod;
	}

	public void setPrimaryKeyGetterMethod(Method primaryKeyGetterMethod) {
		this.primaryKeyGetterMethod = primaryKeyGetterMethod;
	}

	public Method getPrimaryKeySetterMethod() {
		return primaryKeySetterMethod;
	}

	public void setPrimaryKeySetterMethod(Method primaryKeySetterMethod) {
		this.primaryKeySetterMethod = primaryKeySetterMethod;
	}

	public Map<String, FieldInfo> getClassDbFieldInfoMap() {
		return classDbFieldInfoMap;
	}

	public void setClassDbFieldInfoMap(Map<String, FieldInfo> classDbFieldInfoMap) {
		this.classDbFieldInfoMap = classDbFieldInfoMap;
	}

	public Map<String, FieldInfo> getFullNonDbFieldInfoMap() {
		return fullNonDbFieldInfoMap;
	}

	public void setFullNonDbFieldInfoMap(Map<String, FieldInfo> fullNonDbFieldInfoMap) {
		this.fullNonDbFieldInfoMap = fullNonDbFieldInfoMap;
	}

	public Map<String,FieldInfo> getAliasedFieldInfoMap() {
		return aliasedFieldInfoMap;
	}

	public void setAliasedFieldInfoMap(Map<String,FieldInfo> fieldInfoMap) {
		this.aliasedFieldInfoMap = fieldInfoMap;
	}

	public Map<String, FieldInfo> getClassAliasedFieldInfoMap() {
		return classAliasedFieldInfoMap;
	}

	public void setClassAliasedFieldInfoMap(Map<String, FieldInfo> classAliasedFieldInfoMap) {
		this.classAliasedFieldInfoMap = classAliasedFieldInfoMap;
	}

	public Set<FieldInfo> getEmbeddedFieldInfos() {
		return embeddedFieldInfos;
	}

	public void setEmbeddedFieldInfos(Set<FieldInfo> embeddedFieldInfos) {
		this.embeddedFieldInfos = embeddedFieldInfos;
	}

}
