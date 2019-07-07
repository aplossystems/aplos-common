package com.aplos.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.metadata.FieldInfoComparator;
import com.aplos.common.utils.ApplicationUtil;

public class ArchivableTable {
	private String tableName;
	private String[] fieldNames;
	private PersistableTable persistableTable;
	private Set<FieldInfo> archivableFieldInfos = new HashSet<FieldInfo>();
	private int maxDayAge;
	protected BeanDao standardBeanDao;
	
	public ArchivableTable() {
	}

	public ArchivableTable( Class<? extends AplosAbstractBean> beanClass, String[] fieldNames, int maxDayAge ) {
		setTableName( beanClass.getSimpleName().toLowerCase() );
		setFieldNames(fieldNames);
		setMaxDayAge( maxDayAge );
	}
	
	public void findPersistableTable() {
		setPersistableTable( ApplicationUtil.getPersistentApplication().getPersistableTableBySqlNameMap().get( getTableName() ) );
		standardBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) ((PersistentClass) getPersistableTable()).getTableClass() );
		
		getArchivableFieldInfos().add( getPersistableTable().determinePrimaryKeyFieldInfo() );

		for( int i = 0, n = getFieldNames().length; i < n; i++ ) {
			getArchivableFieldInfos().add( getPersistableTable().getFieldInfoFromSqlName( getFieldNames()[ i ], true) );
		}
	}
	
	public boolean verifyBeforeArchiving( Long objectId ) {
		return true;
	}
	
	public void unarchive( AplosBean aplosBean ) {
		Connection connection = null;
		Connection archiveConnection = null;

		try {
			AplosBean saveableBean = aplosBean.getSaveableBean();
			connection = ApplicationUtil.getConnection();
			archiveConnection = ApplicationUtil.getPersistentApplication().getArchiveConnection();
			connection.setAutoCommit(false);
			archiveConnection.setAutoCommit(false);
			
			String standardDbName = connection.getCatalog();
			String archiveDbName = archiveConnection.getCatalog();

			String[] beanSelectCriteria = new String[ getArchivableFieldInfos().size() ];
			int count = 0;
			for( FieldInfo tempFieldInfo : getArchivableFieldInfos() ) {
				beanSelectCriteria[ count++ ] = tempFieldInfo.getSqlName();
			}
			
			List<String> sqlCommands = new ArrayList<String>();
			FieldInfo primaryFieldInfo = getPersistableTable().determinePrimaryKeyFieldInfo();

			StringBuffer sqlStrBuf = new StringBuffer( "SELECT " );
			sqlStrBuf.append( StringUtils.join( beanSelectCriteria, "," ) );
			sqlStrBuf.append( " FROM " ).append( archiveDbName );
			sqlStrBuf.append( "." ).append( getTableName() );
			sqlStrBuf.append( " WHERE " ).append( primaryFieldInfo.getSqlName() );
			sqlStrBuf.append( " = " ).append( aplosBean.getId() );
			
			Object[] results = ApplicationUtil.getFirstResult( sqlStrBuf.toString() );
			if( results != null ) {
				count = 0;
				for( FieldInfo tempFieldInfo : getArchivableFieldInfos() ) {
					if( tempFieldInfo != primaryFieldInfo ) {
						tempFieldInfo.setValue( false, saveableBean, results[ count ] );
					}
					count++;
				}
				
				sqlStrBuf = new StringBuffer( "UPDATE " );
				sqlStrBuf.append( standardDbName ).append( "." );
				sqlStrBuf.append( getTableName() );
				sqlStrBuf.append( " as original LEFT OUTER JOIN " );
				sqlStrBuf.append( archiveDbName ).append( "." );
				sqlStrBuf.append( getTableName() );
				sqlStrBuf.append( " as archived ON original.id = archived.id SET original.isArchived = false " );
				for( FieldInfo tempFieldInfo : getArchivableFieldInfos() ) {
					if( tempFieldInfo != primaryFieldInfo ) {
						sqlStrBuf.append( ", original." ).append( tempFieldInfo.getSqlName() );
						sqlStrBuf.append( " = archived."  );
						sqlStrBuf.append( tempFieldInfo.getSqlName() );
					}
				}
				sqlStrBuf.append( " WHERE original." ).append( primaryFieldInfo.getSqlName() );
				sqlStrBuf.append( " = " ).append( aplosBean.getId() );
				sqlCommands.add( sqlStrBuf.toString() );
				
				sqlStrBuf = new StringBuffer( "DELETE FROM " );
				sqlStrBuf.append( archiveDbName ).append( "." );
				sqlStrBuf.append( getTableName() );
				sqlStrBuf.append( " WHERE " ).append( primaryFieldInfo.getSqlName() );
				sqlStrBuf.append( " = " ).append( aplosBean.getId() );
				sqlCommands.add( sqlStrBuf.toString() );
			} else {
				ApplicationUtil.handleError( new Exception( "Archived table could not be found for " + getTableName() + " " + aplosBean.getId() ), false);
			}
			
			ApplicationUtil.executeBatchSql( sqlCommands );
			connection.commit();
			archiveConnection.commit();
			saveableBean.setArchived( false );
			saveableBean.saveDetails();
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
			ApplicationUtil.rollbackConnection( connection );
			ApplicationUtil.rollbackConnection( archiveConnection );
		} finally {
			ApplicationUtil.closeConnection( connection );
			ApplicationUtil.closeConnection( archiveConnection );
		}
	}
	
	public List<String> createTable(Connection conn) throws SQLException {
		StringBuffer strBuf = new StringBuffer( "CREATE TABLE ");
		strBuf.append( getTableName() ).append( " (\n" );

		List<String> primaryKeyColumnNames = new ArrayList<String>();
		for( FieldInfo tempFieldInfo : getArchivableFieldInfos() ) {
			tempFieldInfo.appendCreateTableStr( strBuf, this.getPersistableTable(), tempFieldInfo.isPrimaryKey() );
			strBuf.append( ",\n" );
			if( tempFieldInfo.isPrimaryKey() ) {
				primaryKeyColumnNames.add( tempFieldInfo.getSqlName() );
			}
		}
		List<String> keysToAddSqlList = null;
		
		if( primaryKeyColumnNames.size() > 0 ) {
			strBuf.append( "PRIMARY KEY (`" );
			strBuf.append( StringUtils.join( primaryKeyColumnNames, "`,`" ) );
			strBuf.append( "`),\n" );
		}

		strBuf.replace( strBuf.length() - 2, strBuf.length() - 1, "" );
		strBuf.append( ") ENGINE=" ).append( ApplicationUtil.getPersistentApplication().getDbEngineName() );
		strBuf.append( "  DEFAULT CHARSET=" ).append( ApplicationUtil.getPersistentApplication().getDbCharSet() );
		ApplicationUtil.executeSql( strBuf.toString(), conn );
		
		PersistentApplication.createPersistenceHelperTable( getTableName(), conn );
		
		return keysToAddSqlList;
	}
	
	public boolean confirmState( PersistenceHelperTable persistenceHelperTable, Connection conn ) {
		if( !persistenceHelperTable.getName().equals( getTableName().toLowerCase() ) ) {
			return false;
		}

		StringBuffer strBuf = new StringBuffer();
		List<FieldInfo> sortedFieldInfos = new ArrayList<FieldInfo>(getArchivableFieldInfos());
		Collections.sort( sortedFieldInfos, new FieldInfoComparator() );
		for( FieldInfo tempFieldInfo : sortedFieldInfos ) {
			strBuf.append( tempFieldInfo.toString() + ";" );
		}
		
		
		String cachedFieldStr = strBuf.toString();
		if( !persistenceHelperTable.getColumns().equals( cachedFieldStr ) ) {
			persistenceHelperTable.checkColumns( this.getPersistableTable(), sortedFieldInfos, cachedFieldStr, conn );
		}
		
		return true;
	}
	
	public PersistableTable getPersistableTable() {
		return persistableTable;
	}
	public void setPersistableTable(PersistableTable persistableTable) {
		this.persistableTable = persistableTable;
	}
	public Set<FieldInfo> getArchivableFieldInfos() {
		return archivableFieldInfos;
	}
	public void setArchivableFieldInfos(Set<FieldInfo> archivableFieldInfos) {
		this.archivableFieldInfos = archivableFieldInfos;
	}
	public int getMaxDayAge() {
		return maxDayAge;
	}
	public void setMaxDayAge(int maxDayAge) {
		this.maxDayAge = maxDayAge;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}
}
