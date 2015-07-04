package com.aplos.common.module;

import java.math.BigInteger;
import java.sql.Connection;
import java.util.Set;

import org.apache.log4j.Logger;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentApplication;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public abstract class ModuleUpgrader {
	private static Logger logger = Logger.getLogger( ModuleUpgrader.class );

	/* If your upgrader uses the old version (passes a configuration object)
	 * you need to make the following changes:
	 *
	 * 1) change implements ModuleUpgrader to extends ModuleUpgrader
	 * 2) remove ModuleConfiguration moduleConfiguration;
	 * 3) change constructor to have no parameters and set super(configuration.class)
	 * 4) change switch's to use getMajorNumber() / getMinorNumber() / getPatchNumber()
	 * 5) remove hibernate session.merge
	 * 6) change configuration.setModuleVersionPatch(1) etc to saveUpgradedPatchNumber(1); etc
	 * 7) change your module to pass no parameters to the upgrader call
	 *
	 * */

	private Class<? extends ModuleConfiguration> configurationClass = null;
	private AplosModule aplosModule;
	private Integer majorVersionNumber;
	private Integer minorVersionNumber;
	private Integer patchVersionNumber;

	public ModuleUpgrader( AplosModule aplosModule, Class<? extends ModuleConfiguration> configurationClass ) {
		this.setAplosModule(aplosModule);
		this.configurationClass = configurationClass;
		determineModuleVersion();
	}

	public void manageUpgradeModule() {
		int originalMajorVersion = getMajorVersionNumber();
		int originalMinorVersion = getMinorVersionNumber();
		int originalPatchVersion = getPatchVersionNumber();
		upgradeModule();

		if ( originalMajorVersion != getMajorVersionNumber() ||
				originalMinorVersion != getMinorVersionNumber() ||
				originalPatchVersion != getPatchVersionNumber() ) {
			updateModuleVersion();
		}
	}

	public void determineModuleVersion() {
		Object[] versionNumbers = (Object[]) ApplicationUtil.getFirstResult("SELECT moduleVersionMajor,moduleVersionMinor,moduleVersionPatch FROM " + AplosBean.getTableName( configurationClass ));

		if( versionNumbers == null ) {
			ModuleConfiguration moduleConfiguration = (ModuleConfiguration) CommonUtil.getNewInstance( configurationClass, null );

			setMajorVersionNumber(moduleConfiguration.getMaximumModuleVersionMajor());
			setMinorVersionNumber(moduleConfiguration.getMaximumModuleVersionMinor());
			setPatchVersionNumber(moduleConfiguration.getMaximumModuleVersionPatch());
		} else {
			setMajorVersionNumber((Integer) versionNumbers[ 0 ]);
			setMinorVersionNumber((Integer) versionNumbers[ 1 ]);
			setPatchVersionNumber((Integer) versionNumbers[ 2 ]);
		}
	}

	public void updateModuleVersion() {
		ModuleConfiguration moduleConfiguration = getAplosModule().getModuleConfiguration().getSaveableBean();
		moduleConfiguration.setModuleVersionMajor(getMajorVersionNumber());
		moduleConfiguration.setModuleVersionMinor(getMinorVersionNumber());
		moduleConfiguration.setModuleVersionPatch(getPatchVersionNumber());
		moduleConfiguration.saveDetails();
//		ApplicationUtil.executeSql("UPDATE " + AplosBean.getTableName( configurationClass ) + " SET moduleVersionMajor = " + getMajorVersionNumber() + ", moduleVersionMinor = " + getMinorVersionNumber() + ", moduleVersionPatch = " + getPatchVersionNumber() );
		/*
		 * Evict module configuration from the session as it may have been loaded and will overwrite the above changes
		 */
//		HibernateUtil.getCurrentSession().evict(aplosModule.getModuleConfiguration());
//		HibernateUtil.getCurrentSession().flush();
		logger.info("** Aplos " + aplosModule.getPackageDisplayName() + " module upgraded to version " + getMajorVersionNumber() + "." + getMinorVersionNumber() + "." + getPatchVersionNumber() + " **");
	}

	protected abstract void upgradeModule();
	
	public void setDefault( Class<? extends AplosAbstractBean> beanClass, String variableName, String value ) {
		setDefault(beanClass, variableName, value, true);
	}
	
	public void setDefault( Class<? extends AplosAbstractBean> beanClass, String variableName, String value, boolean addNullCheck ) {
		try {
			PersistentClass persistentClass = ApplicationUtil.getPersistentClass( beanClass );
			if( persistentClass != null ) {
				beanClass = (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass();
			}
			StringBuffer strBuf = new StringBuffer( "UPDATE " );
			strBuf.append( AplosBean.getTableName( beanClass ) ).append( " SET " );
			strBuf.append( variableName ).append( " = " ).append( value );
			if( addNullCheck ) {
				strBuf.append( " WHERE " ).append( variableName ).append( " IS NULL" );
			}
			ApplicationUtil.executeSql( strBuf.toString() );
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
	}
	
	public FieldInfo findFieldInfo( Class<? extends AplosAbstractBean> beanClass, String columnName ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( beanClass );
		return persistentClass.getFieldInfoFromSqlName( columnName, true );
	}
	
	public void renameOldTableColumn( String oldTableName, String oldColumnName, FieldInfo newColumnFieldInfo ) {
		StringBuffer sqlBuf = new StringBuffer( "ALTER TABLE " ).append( oldTableName );
		sqlBuf.append( " CHANGE " ).append( oldColumnName ).append( " " );
		sqlBuf.append( newColumnFieldInfo.getSqlName() ).append( " " ).append( newColumnFieldInfo.getApplicationType().getMySqlType().name() );
		ApplicationUtil.executeSql( sqlBuf.toString() );
		
	}
	
	public void transferColumn( Class<? extends AplosAbstractBean> beanClass, String oldVariableName, String newVariableName, boolean includeNullCheck ) {
		transferColumn(beanClass, oldVariableName, newVariableName, includeNullCheck, true);
	}
	
	public void transferColumn( Class<? extends AplosAbstractBean> beanClass, String oldVariableName, String newVariableName, boolean includeNullCheck, boolean dropColumn ) {
		try {
			PersistentClass dbPersistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass ).getDbPersistentClass(); 
			if( dbPersistentClass != null ) {
				transferColumn(dbPersistentClass, oldVariableName, newVariableName, includeNullCheck, dropColumn);
			}
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		PersistentApplication.updatePersistenceHelperTable(AplosBean.getTableName( beanClass ));
	}
	

	public void transferColumn( PersistentClass dbPersistentClass, String oldVariableName, String newVariableName, boolean includeNullCheck, boolean dropColumn ) {
		Class<? extends AplosAbstractBean> beanClass = (Class<? extends AplosAbstractBean>) dbPersistentClass.getTableClass();
		StringBuffer hqlBuf = new StringBuffer( "UPDATE " );
		hqlBuf.append( AplosBean.getTableName( beanClass ) ).append( " SET " ); 
		hqlBuf.append( newVariableName ).append( " = " ).append( oldVariableName );
		if( includeNullCheck ) {
			hqlBuf.append( " WHERE " + newVariableName + " IS NULL" );
		}
		if( ApplicationUtil.executeSql( hqlBuf.toString() ) ) {
			if( dropColumn ) {
				ApplicationUtil.executeSql( "ALTER TABLE " + AplosBean.getTableName( beanClass ) + " DROP COLUMN " + oldVariableName );
			}
		}
	}
	
	public void transferJunctionTable( String oldTableName, Class<? extends AplosAbstractBean> beanClass, String variableName, boolean dropOldTable ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
		JunctionTable junctionTable = persistentClass.getCollectionTableMap().get( persistentClass.determineSqlTableName().toLowerCase() + "_" + variableName.toLowerCase() );
		transferTable(oldTableName, junctionTable, dropOldTable);
	}

	public boolean transferTable( String oldTableName, Class<? extends AplosAbstractBean> beanClass, boolean dropOldTable ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
		return transferTable(oldTableName, persistentClass, dropOldTable);
	}
	
	public boolean transferTable( String oldTableName, PersistableTable persistableTable, boolean dropOldTable ) {
		try {
			StringBuffer insertSqlBuf = new StringBuffer( "INSERT INTO " );
			insertSqlBuf.append( persistableTable.determineSqlTableName() ).append(" (");
			for( int i = 0, n = persistableTable.getFullDbFieldInfos().size(); i < n; i++ ) {
				insertSqlBuf.append( "`" ).append( persistableTable.getFullDbFieldInfos().get( i ).getSqlName() ).append( "`," );
			}
			insertSqlBuf.replace( insertSqlBuf.length() - 1, insertSqlBuf.length(), "" );
			insertSqlBuf.append( ") SELECT " );
			for( int i = 0, n = persistableTable.getFullDbFieldInfos().size(); i < n; i++ ) {
				insertSqlBuf.append( "`" ).append( persistableTable.getFullDbFieldInfos().get( i ).getSqlName() ).append( "`," );
			}
			insertSqlBuf.replace( insertSqlBuf.length() - 1, insertSqlBuf.length(), "" );
			insertSqlBuf.append( " FROM " ).append( oldTableName );
			boolean isWhereAdded = false;
			for( int i = 0, n = persistableTable.getFullDbFieldInfos().size(); i < n; i++ ) {
				if( persistableTable.getFullDbFieldInfos().get( i ).isUnique() || persistableTable.getFullDbFieldInfos().get( i ).isPrimaryKey() ) {
					if( !isWhereAdded ) {
						insertSqlBuf.append( " WHERE " );
						isWhereAdded = true;
					} else {
						insertSqlBuf.append( " AND " );
					}
					insertSqlBuf.append( persistableTable.getFullDbFieldInfos().get( i ).getSqlName() ).append( " IS NOT NULL" );
				}
			}
			
			if( ApplicationUtil.executeSql( insertSqlBuf.toString() ) ) {
				if( dropOldTable ) {
					ApplicationUtil.executeSql( "DROP TABLE " + oldTableName );
				}
				return true;
			}
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		PersistentApplication.updatePersistenceHelperTable(persistableTable.determineSqlTableName());
		return false;
	}
	
	public void dropColumn( Class<? extends AplosAbstractBean> beanClass, String variableName ) {
		try {
			ApplicationUtil.executeSql( "ALTER TABLE " + AplosBean.getTableName( beanClass ) + " DROP COLUMN " + variableName );
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		PersistentApplication.updatePersistenceHelperTable(AplosBean.getTableName( beanClass ));
	}
	
	public void dropTable( Class<? extends AplosAbstractBean> beanClass, boolean dropTableWithRows ) {
		dropTable( AplosBean.getTableName( beanClass ), dropTableWithRows, null );
	}
	
	public static void dropTable( Class<? extends AplosAbstractBean> beanClass ) {
		dropTable( AplosBean.getTableName( beanClass ) );
	}
	
	public static void dropTable( String tableName ) {
		dropTable( tableName, false, null );
	}
	
	public static void dropTable( String tableName, boolean dropTableWithRows ) {
		dropTable( tableName, dropTableWithRows, null );
	}
	
	public static void dropTable( String tableName, boolean dropTableWithRows, Connection conn ) {
		boolean dropTable = true;
		try {
			if( !dropTableWithRows ) {
				Object[] firstResult = ApplicationUtil.getFirstResult( "SELECT COUNT(*) FROM " + tableName );
				if( firstResult != null ) {
					Object rowCount = ApplicationUtil.getFirstResult( "SELECT COUNT(*) FROM " + tableName )[0];
					if( (rowCount instanceof BigInteger && ((BigInteger) rowCount).intValue() > 0) 
							|| (rowCount instanceof Long && ((Long) rowCount).intValue() > 0) ) {
						dropTable = false;
						ApplicationUtil.getAplosContextListener().handleError( new Exception( "Cannot delete " + tableName + " as it has rows" ) );
					}
				} else {
					dropTable = false;
				}
			}
			if( dropTable ) {
				if( conn != null ) {
					ApplicationUtil.executeSql( "DROP TABLE " + tableName, conn );
				} else {
					ApplicationUtil.executeSql( "DROP TABLE " + tableName );
				}
			}
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		if( dropTable ) {
			ApplicationUtil.executeSql( "DELETE FROM PersistenceHelperTable WHERE tableName = '" + tableName + "'" );
		}
	}

	public void setAplosModule(AplosModule aplosModule) {
		this.aplosModule = aplosModule;
	}

	public AplosModule getAplosModule() {
		return aplosModule;
	}

	public void setMajorVersionNumber(Integer majorVersionNumber) {
		this.majorVersionNumber = majorVersionNumber;
	}

	public Integer getMajorVersionNumber() {
		return majorVersionNumber;
	}

	public void setMinorVersionNumber(Integer minorVersionNumber) {
		this.minorVersionNumber = minorVersionNumber;
	}

	public Integer getMinorVersionNumber() {
		return minorVersionNumber;
	}

	public void setPatchVersionNumber(Integer patchVersionNumber) {
		this.patchVersionNumber = patchVersionNumber;
	}

	public Integer getPatchVersionNumber() {
		return patchVersionNumber;
	}
}