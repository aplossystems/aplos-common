package com.aplos.common.persistence;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aplos.common.ImplicitPolymorphismEntry;
import com.aplos.common.ImplicitPolymorphismVariable;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.comparators.StringComparator;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.JoinedIdFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PolymorphicFieldInfo;
import com.aplos.common.persistence.metadata.MetaTable;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class PersistentApplication {
	private static Logger logger = Logger.getLogger( PersistentApplication.class );
	private Map<Class<?>, PersistentClass> persistentClassMap = new HashMap<Class<?>, PersistentClass>();
	private Map<String, PersistableTable> persistableTableBySqlNameMap = new HashMap<String, PersistableTable>();
    public static final String[] TYPES = {"TABLE", "VIEW"};
    private ComboPooledDataSource cpds;
    private ComboPooledDataSource archiveCpds;
    private String dbEngineName = "InnoDB";
    private String dbCharSet = "UTF8MB4";
    private Map<Class<?>,ImplicitPolymorphismMatch> dynamicMetaValuesMap = new HashMap<Class<?>,ImplicitPolymorphismMatch>();
    private Map<Class<Enum>, EnumHelper> enumHelperMap = new HashMap<Class<Enum>, EnumHelper>();
    private List<PersistentClass> newlyAddedTables;
    private Map<String, Class<?>> classForNameMap = new HashMap<String, Class<?>>();
    private Map<String, ArchivableTable> archivableTableMap = new HashMap<String, ArchivableTable>(); 

	public void prepareForPersistence() {
		transferDependencyClassesToPersistentClasses();
		markDependenciesForInclusion();
		
		for( PersistentClass persistentClass : new ArrayList<PersistentClass>( getPersistentClassMap().values() ) ) {
			if( !persistentClass.getTableClass().isInterface() ) {
				persistentClass.findParentClasses(getPersistentClassMap());
			}
		}
		markParentsForInclusion();
		
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( !persistentClass.getTableClass().isInterface() && persistentClass.getInheritanceType() == null ) {
				persistentClass.determineInheritanceType();
			}
		}

		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( persistentClass.isIncludeInApp() ) {
				loadTypes(persistentClass);
			}
		}

		PersistentClass tempParentPersistentClass;
		ForeignFieldInfo tempForeignFieldInfo;
		PersistentClass tempForeignPersistentClass;
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( InheritanceType.JOINED_TABLE.equals( persistentClass.getInheritanceType() ) ) {
				tempParentPersistentClass = persistentClass.getParentPersistentClass();
				if( tempParentPersistentClass != null && tempParentPersistentClass.getInheritanceType().equals( InheritanceType.JOINED_TABLE ) ) {
					while( tempParentPersistentClass.getPrimaryKeyFieldInfo() == null ) {
						tempParentPersistentClass = tempParentPersistentClass.getParentPersistentClass();
					}
					persistentClass.getFieldInfos().add(new JoinedIdFieldInfo( persistentClass, tempParentPersistentClass.getPrimaryKeyFieldInfo()));
				}
			}
			
			for( FieldInfo tempFieldInfo : new ArrayList<FieldInfo>( persistentClass.getFieldInfos() ) ) {
				if( tempFieldInfo instanceof ForeignFieldInfo  ) {
					tempForeignFieldInfo = (ForeignFieldInfo) tempFieldInfo;
					tempForeignPersistentClass = getPersistentClassMap().get( tempForeignFieldInfo.getPersistentClass().getTableClass() );
					if( tempForeignFieldInfo.getMappedBy() != null ) {
						for( FieldInfo foreignFieldInfo : tempForeignPersistentClass.getFieldInfos() ) {
							if( !(foreignFieldInfo instanceof PolymorphicFieldInfo) && foreignFieldInfo.getField().getName().equals( tempForeignFieldInfo.getMappedBy() ) ) {
								tempForeignFieldInfo.setForeignFieldInfo( foreignFieldInfo );
								((PersistentClassFieldInfo) foreignFieldInfo).getForeignFieldInfos().add( tempForeignFieldInfo );								break;
							}
						}
					} else if( tempForeignFieldInfo.getJoinColumnAnnotation() != null ) {
						for( FieldInfo foreignFieldInfo : tempForeignPersistentClass.getFieldInfos() ) {
							if( !(foreignFieldInfo instanceof PolymorphicFieldInfo) && foreignFieldInfo.getSqlName().equals( tempForeignFieldInfo.getJoinColumnAnnotation().name() ) ) {
								tempForeignFieldInfo.setForeignFieldInfo( foreignFieldInfo );
								((PersistentClassFieldInfo) foreignFieldInfo).getForeignFieldInfos().add( tempForeignFieldInfo );	
								break;
							}
						}
					}
					tempForeignFieldInfo.updateIndexColumns();
				}
			}
			
			persistentClass.checkDynamicMetaValues( getDynamicMetaValuesMap() );
		}
		logger.debug( "Initialising persistent classes" );
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( persistentClass.isIncludeInApp() && persistentClass.isDbTable() ) {
				getPersistableTableBySqlNameMap().put( persistentClass.determineSqlTableName().toLowerCase(), persistentClass );
				List<JunctionTable> collectionTableList = persistentClass.getCollectionTableList(); 
				for( int i = 0, n = collectionTableList.size(); i < n; i++ ) {
					getPersistableTableBySqlNameMap().put( collectionTableList.get( i ).determineSqlTableName().toLowerCase(), collectionTableList.get( i ) );
				}
				persistentClass.generateSqlStatements();
			}
			if( persistentClass.isEntity() ) {
				persistentClass.initialiseDbInformation();
			}
		}
		
//		/*
//		 * Needs to be done after all updateIndexColumns are called as these may add new index columns into 
//		 * other tables.
//		 */
//		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
//			if( persistentClass.isDbTable() ) {
//				persistentClass.generateSqlStatements();
//			}
//			persistentClass.buildProxyFactory();
//		}
		
//		for( )
		
		updateDatabase();
		checkDatabase();
	}
	
	public AplosAbstractBean convertBeanClass( AplosAbstractBean aplosAbstractBean, Class<? extends AplosAbstractBean> newBeanClass ) {
		/*
		 * This only works for Single Table inheritance, a check and code will have to be written for joined table inheritance
		 */
		if( aplosAbstractBean.getClass().isAssignableFrom( newBeanClass ) ) {
			PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( newBeanClass );
//			StringBuffer sqlStrBuf = new StringBuffer();
//			sqlStrBuf.append( "UPDATE " ).append( AplosBean.getTableName( persistentClass.getDbPersistentClass().getTableClass() ) );
//			sqlStrBuf.append( " SET " ).append( persistentClass.getDbPersistentClass().getDiscriminatorFieldInfo().getSqlName() );
//			sqlStrBuf.append( " = '" ).append( persistentClass.getDiscriminatorValue() );
//			sqlStrBuf.append( "' WHERE id = " + aplosAbstractBean.getId() );
//			ApplicationUtil.executeSql( sqlStrBuf.toString() );
//			
//			if( )
			ApplicationUtil.getPersistenceContext().removeBean( aplosAbstractBean );
			
			AplosAbstractBean convertedBean = (AplosAbstractBean) CommonUtil.getNewInstance( newBeanClass );
			persistentClass.copyFields( aplosAbstractBean, convertedBean );
			List<PersistentClass> persistentClassList = new ArrayList<PersistentClass>();
			PersistentClass persistentClassToAdd = persistentClass;
			while( !persistentClassToAdd.getTableClass().isAssignableFrom( aplosAbstractBean.getClass() ) ) {
				persistentClassList.add( persistentClassToAdd );
				persistentClassToAdd = persistentClassToAdd.getParentPersistentClass();
			}
			
			try {
				PersistentBeanSaver.savePersistentClassList( convertedBean, persistentClassList, persistentClassToAdd, true );
			} catch( Exception ex ) {
				ApplicationUtil.handleError(ex);
			}
			convertedBean.setInDatabase(true);
			convertedBean.saveDetails();
			
			return new BeanDao( newBeanClass ).get( aplosAbstractBean.getId() );
		} else {
			ApplicationUtil.handleError( new Exception( "Attempt to change the class of an object to a class not below it in the inheritance tree" ) );
		}
		return null;
//		return null;
	}
	
	public Class<?> classForName( String className ) throws ClassNotFoundException {
		Class<?> foundClass = classForNameMap.get( className );
		if( foundClass == null ) {
			foundClass = Class.forName(className);
			classForNameMap.put( className, foundClass );
		}
		return foundClass;
	}
	
	public String getDiscriminatorValue( AplosAbstractBean aplosAbstractBean ) {
		return ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( aplosAbstractBean.getClass() ).getDiscriminatorValue();
	}
	
	public int getMaxCharLength() {
		if( getDbCharSet().equalsIgnoreCase( "UTF8MB4" ) ) {
			return 191;
		} else {
			return 255;
		}
	}
	
	public void registerEnumType( Class<Enum> enumClass ) {
		if( enumHelperMap.get( enumClass ) == null ) {
			EnumHelper enumHelper = new EnumHelper();
			enumHelper.setEnumClass( enumClass );
			try {
				Method method = null;
				method = enumClass.getDeclaredMethod( "values", new Class[0] );
				Enum[] values = (Enum[]) method.invoke( null, new Object[0] );
				enumHelper.registerValues( values );
			}
			catch (Exception e) {
				ApplicationUtil.handleError(e);
			}
			enumHelperMap.put( enumClass, enumHelper );
		}
	}
	
	public void checkDatabase() {
		Connection connection = null;
		try {
			connection = getCpds().getConnection();
			ResultSet resultSet = connection.prepareStatement( "SELECT * FROM PersistenceHelperTable" ).executeQuery();
			PersistenceHelperTable tempPersistenceHelperTable;
			Map<String,PersistenceHelperTable> tableStateMap = new HashMap<String,PersistenceHelperTable>();
			String tempTableName;
			while( resultSet.next() ) {
				tempPersistenceHelperTable = new PersistenceHelperTable( resultSet );
				tempTableName = resultSet.getString( "tableName" );
				tableStateMap.put( tempTableName, tempPersistenceHelperTable );
			}	
			
			List<PersistentClass> unmatchedPersistentClassList = new ArrayList<PersistentClass>();
			List<JunctionTable> unmatchedCollectionTableList = new ArrayList<JunctionTable>();
			for( PersistableTable persistableTable : getPersistableTableBySqlNameMap().values() ) {
				if( persistableTable instanceof JunctionTable ) {
					unmatchedCollectionTableList.add( (JunctionTable) persistableTable );	
				} else {
					unmatchedPersistentClassList.add( (PersistentClass) persistableTable );
				}
			}
			PersistableTable tempPersistableTable;
			for( String tableName : tableStateMap.keySet() ) {
				tempPersistableTable = getPersistableTableBySqlNameMap().get( tableName );
				if( tempPersistableTable != null ) {
					if( tempPersistableTable instanceof JunctionTable ) {
						unmatchedCollectionTableList.remove( tempPersistableTable );
					} else {
						unmatchedPersistentClassList.remove( tempPersistableTable );
					}
				}
			}
			
			List<String> keysToAddSqlList = new ArrayList<String>();
			setNewlyAddedTables(new ArrayList<PersistentClass>());
			List<String> tempKeysToAddSql;
			while( unmatchedPersistentClassList.size() > 0 ) {
				tempKeysToAddSql = unmatchedPersistentClassList.get( 0 ).createTable( true, connection );
				if( tempKeysToAddSql != null ) {
					keysToAddSqlList.addAll( tempKeysToAddSql );
				}
				getNewlyAddedTables().add( unmatchedPersistentClassList.get( 0 ) );
				unmatchedPersistentClassList.remove(unmatchedPersistentClassList.get( 0 ));
			}
			ApplicationUtil.executeBatchSql( keysToAddSqlList );
			
			
			for( int i = 0, n = unmatchedCollectionTableList.size(); i < n; i++ ) {
				unmatchedCollectionTableList.get( i ).createTable( false, connection );
			}
			
			for( String tableName : tableStateMap.keySet() ) { 
				tempPersistableTable = getPersistableTableBySqlNameMap().get( tableName );
				if( tempPersistableTable != null ) {
					if( tempPersistableTable instanceof JunctionTable ) {
						tempPersistableTable.confirmState( tableStateMap.get( tableName ), connection );
					} else {
//						if( !(tableName.equals( "aplosbean" ) ||
//								tableName.equals( "aplostranslation" )||
//								tableName.equals( "printtemplate" )) ) {
						tempPersistableTable.confirmState( tableStateMap.get( tableName ), connection );
					}
				} else {
					if( (Long) ApplicationUtil.getFirstResult( "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + connection.getCatalog() + "' AND table_name = '" + tableName + "'" )[ 0 ] != 0l ) {
						ApplicationUtil.getAplosContextListener().handleError( new Exception( "Table needs to be deleted, DROP TABLE " + tableName ), false );
					} else {
						PersistentApplication.removePersistenceHelperTable(tableName);
					}
				}
			}
			
			if( getArchiveCpds() != null ) {
				Connection archiveConnection = null;
				try {
					archiveConnection = getArchiveCpds().getConnection();
					resultSet = archiveConnection.prepareStatement( "SELECT * FROM PersistenceHelperTable" ).executeQuery();
					Map<String,PersistenceHelperTable> archiveTableStateMap = new HashMap<String,PersistenceHelperTable>();
					while( resultSet.next() ) {
						tempPersistenceHelperTable = new PersistenceHelperTable( resultSet );
						tempTableName = resultSet.getString( "tableName" );
						archiveTableStateMap.put( tempTableName, tempPersistenceHelperTable );
					}	
					List<ArchivableTable> unmatchedArhivableList = new ArrayList<ArchivableTable>(archivableTableMap.values());
	
					for( ArchivableTable tempArchivableTable : unmatchedArhivableList ) {
						tempArchivableTable.findPersistableTable();
					}
					for( String tableName : archiveTableStateMap.keySet() ) {
						unmatchedArhivableList.remove( archivableTableMap.get( tableName ) );
					}

					for( ArchivableTable archivableTable : unmatchedArhivableList ) {
						archivableTable.createTable( archiveConnection );
					}
					ApplicationUtil.executeBatchSql( keysToAddSqlList );
					for( String archiveTableName : archiveTableStateMap.keySet() ) {
						archivableTableMap.get( archiveTableName ).confirmState( archiveTableStateMap.get( archiveTableName ), archiveConnection );
					}
				} catch( SQLException sqlEx ) {
					ApplicationUtil.getAplosContextListener().handleError(sqlEx);
				} finally {
					ApplicationUtil.closeConnection(archiveConnection);
				}
			}
			
		} catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError(sqlEx);
		} finally {
			ApplicationUtil.closeConnection(connection);
		}
	}
	
	public void checkAgainstimplicitPolymorphisimMap( HashMap<ImplicitPolymorphismVariable,List<ImplicitPolymorphismEntry>> implicitPolymorphismMap ) {
		List<ImplicitPolymorphismVariable> variableList = new ArrayList<ImplicitPolymorphismVariable>( implicitPolymorphismMap.keySet() );
		for( int i = 0, n = variableList.size(); i < n; i++ ) {
			for( ImplicitPolymorphismMatch implicitPolymorphismMatch : dynamicMetaValuesMap.values() ) {
				for( FieldInfo fieldInfo : implicitPolymorphismMatch.getFieldInfoMap().keySet() ) {
					for( PersistentClass parentPersistentClass : implicitPolymorphismMatch.getFieldInfoMap().get( fieldInfo ) ) {
						if( variableList.get( i ).getFullClassName().equals( parentPersistentClass.getTableClass().getName() ) ) {
							if( variableList.get( i ).getVariableName().equalsIgnoreCase( fieldInfo.getField().getName() ) ) {
								logger.info( variableList.get( i ).getFullClassName() + " - " + variableList.get( i ).getVariableName() );
								logger.info( "--------------------------------------" );
								List<ImplicitPolymorphismEntry> entryList = implicitPolymorphismMap.get( variableList.get( i ) );
								List<String> entryStrList = new ArrayList<String>();
								if( entryList != null ) {
									for( ImplicitPolymorphismEntry entry : entryList ) {
										entryStrList.add( entry.getFullClassName() );
									}
									Collections.sort( entryStrList, new StringComparator() );
									for( int j = 0, p = entryStrList.size(); j < p; j++ ) {
										logger.info( entryStrList.get( j ) );
									}
								}
								
								logger.info( "" );
								entryStrList = new ArrayList<String>();
								for( PersistentClass persistentClass : implicitPolymorphismMatch.getPersistentClasses() ) {
									entryStrList.add( persistentClass.getTableClass().getName() );
								}
								Collections.sort( entryStrList, new StringComparator() );
								for( int j = 0, p = entryStrList.size(); j < p; j++ ) {
									logger.info( entryStrList.get( j ) );
								}
								implicitPolymorphismMap.remove( variableList.get( i ) );
							}
						}
					}
				}
			}
		}
	}
	
	public void updateDatabase() {
		Connection connection;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			URL url = getClass().getResource( "/dbConfiguration.xml" );
			if (url != null) {
				Document doc = docBuilder.parse(url.openStream());
				doc.getDocumentElement().normalize();
				Element sessionFactory = doc.getDocumentElement();
				NodeList nodeList = sessionFactory.getElementsByTagName( "property" );
				String connectionDriverClass = null;
				String connectionUrl = null;
				String connectionUsername = null;
				String connectionPassword = null;
				String archiveConnectionUrl = null;
				int count = 0;
				Node node;
				while ( (node = nodeList.item( count++ )) != null ) {
					String name = node.getAttributes().getNamedItem( "name" ).getNodeValue();
					String value = node.getTextContent().trim();
					if ( name.equals( "connection.driver_class" ) ) {
						connectionDriverClass = value;
					} else if ( name.equals( "connection.url" ) ) {
						connectionUrl = value;
					} else if ( name.equals( "archive.connection.url" ) ) {
						archiveConnectionUrl = value;
					}  else if ( name.equals( "connection.username" ) ) {
						connectionUsername = value;
					} else if ( name.equals( "connection.password" ) ) {
						connectionPassword = value;
					}
				}
				setCpds(new ComboPooledDataSource());  
				getCpds().setJdbcUrl( connectionUrl ); 
				
				getCpds().setDriverClass( connectionDriverClass ); //loads the jdbc driver
				getCpds().setUser( connectionUsername ); 
				getCpds().setPassword( connectionPassword ); 
				// the settings below are optional -- c3p0 can work with defaults 
				getCpds().setMinPoolSize(5); 
				getCpds().setAcquireIncrement(5); 
				getCpds().setMaxPoolSize(20);
				getCpds().setIdleConnectionTestPeriod(540);
				getCpds().setCheckoutTimeout(1500);
				getCpds().setUnreturnedConnectionTimeout(1200);
				getCpds().setDebugUnreturnedConnectionStackTraces(true);
				getCpds().setPreferredTestQuery("select 1;");
				getCpds().setTestConnectionOnCheckout(true);

				
				connection = getCpds().getConnection();
				DatabaseMetaData meta = connection.getMetaData();
				
				ResultSet rs = null;

				String catalog = null;
				String schema = null;
				String name = null;
//				CommonUtil.timeTrial("start");
				rs = meta.getTables(catalog, schema, "PersistenceHelperTable", TYPES);
				boolean persistenceHelperExists = false;
				while( rs.next() ) {
					persistenceHelperExists = true;
				}
				
				if( !persistenceHelperExists ) {
					createPersistenceHelper( meta, connection );
				}
				connection.close();
				
				if( archiveConnectionUrl != null ) {
					setArchiveCpds(new ComboPooledDataSource());  
					getArchiveCpds().setJdbcUrl( archiveConnectionUrl ); 
					
					getArchiveCpds().setDriverClass( connectionDriverClass ); //loads the jdbc driver
					getArchiveCpds().setUser( connectionUsername ); 
					getArchiveCpds().setPassword( connectionPassword ); 
					// the settings below are optional -- c3p0 can work with defaults 
					getArchiveCpds().setMinPoolSize(5); 
					getArchiveCpds().setAcquireIncrement(5); 
					getArchiveCpds().setMaxPoolSize(20);
					getArchiveCpds().setIdleConnectionTestPeriod(540);
					getArchiveCpds().setCheckoutTimeout(600);
					getArchiveCpds().setUnreturnedConnectionTimeout(1200);
					getArchiveCpds().setDebugUnreturnedConnectionStackTraces(true);
					getArchiveCpds().setPreferredTestQuery("select 1;");
					getArchiveCpds().setTestConnectionOnCheckout(true);

					
					connection = getArchiveCpds().getConnection();
					meta = connection.getMetaData();
					
					rs = null;

					catalog = null;
					schema = null;
					name = null;
//					CommonUtil.timeTrial("start");
					rs = meta.getTables(catalog, schema, "PersistenceHelperTable", TYPES);
					persistenceHelperExists = false;
					while( rs.next() ) {
						persistenceHelperExists = true;
					}
					
					if( !persistenceHelperExists ) {
						createPersistenceHelper( meta, connection );
					}
					connection.close();
				}
			}
			
		} catch( PropertyVetoException pvEx ) {
			ApplicationUtil.getAplosContextListener().handleError(pvEx);
		} catch( IOException ioEx ) {
			ApplicationUtil.getAplosContextListener().handleError(ioEx);
		} catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError(sqlEx);
		} catch( SAXException saxEx ) {
			ApplicationUtil.getAplosContextListener().handleError(saxEx);
		} catch( ParserConfigurationException pcEx ) {
			ApplicationUtil.getAplosContextListener().handleError(pcEx);
		} finally {
		}
	}

	public boolean isArchivingData() {
		if( getArchiveCpds() != null ) {
			return true;
		}
		return false;
	}
	
	public void createPersistenceHelper( DatabaseMetaData meta, Connection connection ) throws SQLException {
		ResultSet rs = null;

		String catalog = null;
		String schema = null;
		String name = null;
		rs = meta.getTables(catalog, schema, name, TYPES);
		
		List<MetaTable> metaTables = new ArrayList<MetaTable>();
		MetaTable tempMetaTable;

		StringBuffer sqlBuf = new StringBuffer( "CREATE TABLE `PersistenceHelperTable` (" );
		sqlBuf.append( "`id` INT NOT NULL AUTO_INCREMENT," );
		sqlBuf.append( "`tableName` varchar(100) DEFAULT NULL,");
		sqlBuf.append( "`columns` longtext,");
		sqlBuf.append( "`foreignkeys` longtext,");
		sqlBuf.append( "`indexes` longtext,");
		sqlBuf.append( "PRIMARY KEY (`id`) );" );
		connection.prepareStatement( sqlBuf.toString() ).execute();
		
		while ( rs.next() ) {
			tempMetaTable = new MetaTable( meta, rs );
			metaTables.add( tempMetaTable );
			createPersistenceHelperTable( connection, tempMetaTable.getName(), tempMetaTable.getJoinedColumns(), tempMetaTable.getJoinedForeignKeys( false ), tempMetaTable.getJoinedIndexes( false ) );
		}
	}
	
	public static void createPersistenceHelperTable( String tableName, Connection conn ) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables( null, null, tableName, ApplicationUtil.getPersistentApplication().TYPES);
		while ( rs.next() ) {
			MetaTable tempMetaTable = new MetaTable( meta, rs );
			PersistentApplication.createPersistenceHelperTable( conn, tempMetaTable.getName(), tempMetaTable.getJoinedColumns(), tempMetaTable.getJoinedForeignKeys( false ), tempMetaTable.getJoinedIndexes( false ) );
		}
	}
	
	public static void createPersistenceHelperTable( Connection connection, String tableName, String columns, String foreignKeys, String indexes ) throws SQLException {
		String sql = "INSERT INTO PersistenceHelperTable (`tableName`,`columns`,`foreignkeys`,`indexes`) VALUES (?, ?, ?, ?)";
		PreparedStatement preparedStatement = connection.prepareStatement( sql );
		preparedStatement.setString( 1, tableName.toLowerCase());
		preparedStatement.setString( 2, columns);
		preparedStatement.setString( 3, foreignKeys);
		preparedStatement.setString( 4, indexes);
		logger.debug( sql );

		preparedStatement.execute();
	}
	

	public static void updatePersistenceHelperTable( String tableName ) {
		Connection connection = null;
		try {
			connection = ApplicationUtil.getConnection();
			DatabaseMetaData meta = connection.getMetaData();
			updatePersistenceHelperTable(tableName, connection, meta );
		} catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
		} finally {
			ApplicationUtil.closeConnection(connection);
		}
	}
	
	public static void removePersistenceHelperTable( String tableName ) {
		Connection connection = null;
		try {
			connection = ApplicationUtil.getConnection();

			StringBuffer sqlBuf = new StringBuffer();
			sqlBuf.append( "DELETE FROM persistenceHelperTable" );
			sqlBuf.append( " WHERE tableName = '" ).append( tableName ).append( "'" );
			connection.prepareStatement( sqlBuf.toString() ).execute();
		} catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
		} finally {
			ApplicationUtil.closeConnection(connection);
		}
	}
	
	public static void updatePersistenceHelperTable( String tableName, Connection connection, DatabaseMetaData meta ) {
		try {
			ResultSet rs = meta.getTables( null, null, tableName, PersistentApplication.TYPES);
			if( rs.next() ) {
				MetaTable tempMetaTable = new MetaTable( meta, rs );
				String sql = "UPDATE persistenceHelperTable SET columns = ?, indexes = ?, foreignKeys =  ? WHERE tableName = ?";
				PreparedStatement preparedStatement = connection.prepareStatement( sql );
				preparedStatement.setString( 1, tempMetaTable.getJoinedColumns() );
				preparedStatement.setString( 2, tempMetaTable.getJoinedIndexes( false ) );
				preparedStatement.setString( 3, tempMetaTable.getJoinedForeignKeys( false ) );
				preparedStatement.setString( 4, tableName );
				preparedStatement.execute();
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.getAplosContextListener().handleError( sqlEx, false );
		}
	}
	
	public void loadTypes( PersistentClass persistentClass ) {
		PersistentClass parentPersistentClass = getPersistentClassMap().get( persistentClass.getTableClass().getSuperclass() ); 
		if( parentPersistentClass != null && persistentClass.isTypesLoaded() ) {
			loadTypes( parentPersistentClass );
		}
		persistentClass.loadTypes();
	}

//	public void addAnnotatedClassesToHibernate( AnnotationConfiguration annotatedConfiguration ) {
//		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
//			if( persistentClass.isIncludeInApp() ) {
//				annotatedConfiguration.addAnnotatedClass( persistentClass.getTableClass() );
//			}
//		}
//	}

	private void markDependenciesForInclusion() {
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( persistentClass.isIncludeInApp() ) {
				persistentClass.markDependenciesForInclusion();
			}
		}
	}


	private void markParentsForInclusion() {
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			if( persistentClass.isIncludeInApp() ) {
				PersistentClass parentPersistentClass = persistentClass.getParentPersistentClass(); 
				while( parentPersistentClass != null && !parentPersistentClass.isIncludeInApp() ) {
					parentPersistentClass.setIncludeInApp(true);
					parentPersistentClass = parentPersistentClass.getParentPersistentClass();
				}
			}
		}
	}

	public void transferDependencyClassesToPersistentClasses() {
		for( PersistentClass persistentClass : getPersistentClassMap().values() ) {
			for( Class<?> classDependency : persistentClass.getClassDependencySet() ) {
				if( getPersistentClassMap().get( classDependency ) != null ) {
					persistentClass.getPersistentClassDependencySet().add( getPersistentClassMap().get( classDependency ) );
				} else {
					System.err.println( classDependency.getSimpleName() + " is not a recognised persistent class" );
				}
			}
		}
	}
	
	public void closeCpds() {
		getCpds().close();
	}
	
	public Connection getConnection() throws SQLException {
		return getCpds().getConnection();
	}
	
	public Connection getArchiveConnection() throws SQLException {
		return getArchiveCpds().getConnection();
	}

	public void addPersistentClass( PersistentClass persistentClass ) {
		getPersistentClassMap().put( persistentClass.getClass(), persistentClass );
	}

	public void addPersistentClass( Class<?> tableClass ) {
		getPersistentClassMap().put( tableClass, new PersistentClass( tableClass, false ) );
	}

	public void addPersistentClass( Class<?> tableClass, boolean includeInApp ) {
		getPersistentClassMap().put( tableClass, new PersistentClass( tableClass, includeInApp ) );
	}

	public void includePersistentClass( Class<?> tableClass ) {
		getPersistentClassMap().get( tableClass ).setIncludeInApp( true );
	}

	public Map<Class<?>, PersistentClass> getPersistentClassMap() {
		return persistentClassMap;
	}

	private ComboPooledDataSource getCpds() {
		return cpds;
	}

	private void setCpds(ComboPooledDataSource cpds) {
		this.cpds = cpds;
	}
	
	public String getDbEngineName() {
		return dbEngineName;
	}
	
	public void setDbEngineName(String dbEngineName) {
		this.dbEngineName = dbEngineName;
	}

	public String getDbCharSet() {
		return dbCharSet;
	}

	public void setDbCharSet(String dbCharSet) {
		this.dbCharSet = dbCharSet;
	}

	public Map<Class<?>,ImplicitPolymorphismMatch> getDynamicMetaValuesMap() {
		return dynamicMetaValuesMap;
	}

	public void setDynamicMetaValuesMap(Map<Class<?>,ImplicitPolymorphismMatch> dynamicMetaValuesMap) {
		this.dynamicMetaValuesMap = dynamicMetaValuesMap;
	}

	public Map<Class<Enum>, EnumHelper> getEnumHelperMap() {
		return enumHelperMap;
	}

	public void setEnumHelperMap(Map<Class<Enum>, EnumHelper> enumHelperMap) {
		this.enumHelperMap = enumHelperMap;
	}

	public List<PersistentClass> getNewlyAddedTables() {
		return newlyAddedTables;
	}

	public void setNewlyAddedTables(List<PersistentClass> newlyAddedTables) {
		this.newlyAddedTables = newlyAddedTables;
	}

	public Map<String, PersistableTable> getPersistableTableBySqlNameMap() {
		return persistableTableBySqlNameMap;
	}

	public void setPersistableTableBySqlNameMap(
			Map<String, PersistableTable> persistableTableBySqlNameMap) {
		this.persistableTableBySqlNameMap = persistableTableBySqlNameMap;
	}

	public Map<String, ArchivableTable> getArchivableTableMap() {
		return archivableTableMap;
	}

	public void setArchivableTableMap(Map<String, ArchivableTable> archivableTableMap) {
		this.archivableTableMap = archivableTableMap;
	}

	public ComboPooledDataSource getArchiveCpds() {
		return archiveCpds;
	}

	public void setArchiveCpds(ComboPooledDataSource archiveCpds) {
		this.archiveCpds = archiveCpds;
	}
}
