package com.aplos.common.scheduledjobs;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.persistence.ArchivableTable;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
public class ArchiverJob extends ScheduledJob<Boolean> {
	private static final long serialVersionUID = -7429229430906221778L;

	public ArchiverJob() {	}
	
	@Override
	public Boolean executeCall() throws Exception {
		Map<String, ArchivableTable> archivableTableMap = ApplicationUtil.getPersistentApplication().getArchivableTableMap();
		Calendar cal = GregorianCalendar.getInstance();

		Connection connection = null;
		Connection archiveConnection = null;
		try {
			connection = ApplicationUtil.getConnection();
			archiveConnection = ApplicationUtil.getPersistentApplication().getArchiveConnection();
			connection.setAutoCommit(false);
			archiveConnection.setAutoCommit(false);
			
			String standardDbName = connection.getCatalog();
			String archiveDbName = archiveConnection.getCatalog();
			for( ArchivableTable archivableTable : archivableTableMap.values() ) {
				cal.setTime( new Date() );
				cal.add( Calendar.DAY_OF_YEAR, -archivableTable.getMaxDayAge() );
				String[] beanSelectCriteria = new String[ archivableTable.getArchivableFieldInfos().size() ];
				int count = 0;
				for( FieldInfo tempFieldInfo : archivableTable.getArchivableFieldInfos() ) {
					beanSelectCriteria[ count++ ] = tempFieldInfo.getSqlName();
				}
				
				FieldInfo primaryFieldInfo = archivableTable.getPersistableTable().determinePrimaryKeyFieldInfo();
				
				StringBuffer sqlStrBuf = new StringBuffer( "SELECT " );
				sqlStrBuf.append( primaryFieldInfo.getSqlName() );
				sqlStrBuf.append( " FROM " ).append( standardDbName );
				sqlStrBuf.append( "." ).append( archivableTable.getTableName() );
				sqlStrBuf.append( " WHERE isArchived = false AND (active = false || dateLastModified < " );
				sqlStrBuf.append( FormatUtil.formatDateForDB( cal.getTime(), true ) ).append( ")" );
				
				List<Object[]> idArrayList = ApplicationUtil.getResults( sqlStrBuf.toString() );
				List<Long> idList = new ArrayList<Long>();

				if( idArrayList.size() > 0 ) {
					Long tempId;
					for( Object[] idArray : idArrayList ) {
						tempId = (Long) idArray[ 0 ];
						if( archivableTable.verifyBeforeArchiving( tempId ) ) {
							sqlStrBuf = new StringBuffer( "INSERT INTO " );
							sqlStrBuf.append( archiveDbName ).append( "." );
							sqlStrBuf.append( archivableTable.getTableName() ).append( " (" );
							sqlStrBuf.append( StringUtils.join( beanSelectCriteria, "," ) );
							sqlStrBuf.append( ") SELECT " ).append( StringUtils.join( beanSelectCriteria, "," ) );
							sqlStrBuf.append( " FROM " ).append( standardDbName );
							sqlStrBuf.append( "." ).append( archivableTable.getTableName() );
							sqlStrBuf.append( " WHERE " ).append( primaryFieldInfo.getSqlName() );
							sqlStrBuf.append( " = " ).append( tempId );
							ApplicationUtil.executeSql( sqlStrBuf.toString(), connection );
							
							sqlStrBuf = new StringBuffer( "UPDATE " );
							sqlStrBuf.append( standardDbName ).append( "." );
							sqlStrBuf.append( archivableTable.getTableName() );
							sqlStrBuf.append( " SET isArchived = true " );
							for( FieldInfo tempFieldInfo : archivableTable.getArchivableFieldInfos() ) {
								if( tempFieldInfo != primaryFieldInfo ) {
									sqlStrBuf.append( ", " ).append( tempFieldInfo.getSqlName() );
									sqlStrBuf.append( " = NULL" );
								}
							}
							sqlStrBuf.append( " WHERE " ).append( primaryFieldInfo.getSqlName() );
							sqlStrBuf.append( " = " ).append( tempId );
							ApplicationUtil.executeSql( sqlStrBuf.toString(), connection );
						}
					}
				}
			}
			connection.commit();
			archiveConnection.commit();
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
			ApplicationUtil.rollbackConnection( connection );
			ApplicationUtil.rollbackConnection( archiveConnection );
		} finally {
			ApplicationUtil.closeConnection( connection );
			ApplicationUtil.closeConnection( archiveConnection );
		}
		
		return true;
	}

}
