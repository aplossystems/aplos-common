package com.aplos.common.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.IndexableFieldInfo;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.collection.PersistentAbstractCollection;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.IndexFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentMapKeyFieldInfo;
import com.aplos.common.persistence.type.applicationtype.EnumIntType;
import com.aplos.common.persistence.type.applicationtype.EnumStringType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class PersistentBeanSaver {
	private static Logger logger = Logger.getLogger( PersistentBeanSaver.class );
	
	public static void saveBean( AplosAbstractBean aplosAbstractBean ) throws AqlException {
		/*
		 * This needs to be outside the try statement otherwise the finally gets hit after the 
		 * return statement is called.
		 */
		if( aplosAbstractBean.isAlreadySaving() ) {
			return;
		} else {
			aplosAbstractBean.setAlreadySaving( true );
		}
		try {
			PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( aplosAbstractBean.getClass() );
		
			if( persistentClass != null ) {
				boolean isInsert = !((PersistenceBean) aplosAbstractBean).isInDatabase();
				((PersistenceBean) aplosAbstractBean).setInDatabase(true);
				
				List<PersistentClass> persistentClassList = new ArrayList<PersistentClass>(); 
				persistentClassList.add( persistentClass.getDbPersistentClass() );
				if( InheritanceType.JOINED_TABLE.equals( persistentClass.getInheritanceType() ) ) {
					PersistentClass currentPersistentClass = persistentClass;
					while( !currentPersistentClass.equals( persistentClass.getDbPersistentClass() ) ) {
						persistentClassList.add( 1, currentPersistentClass );
						currentPersistentClass = currentPersistentClass.getParentPersistentClass();
					}	
				}
				
				savePersistentClassList(aplosAbstractBean, persistentClassList, persistentClass, isInsert );
			} else {
				ApplicationUtil.handleError( new Exception( "Persistent class could not be found for " + aplosAbstractBean.getClass().getSimpleName() ) );
			}
		} finally {
			aplosAbstractBean.setAlreadySaving( false );
		}
	}
	
	public static void savePersistentClassList( AplosAbstractBean aplosAbstractBean, List<PersistentClass> persistentClassList, PersistentClass persistentClass, 
			boolean isInsert ) throws AqlException {
		PersistentContextTransaction persistentContextTransaction = new PersistentContextTransaction();
		try {
			Connection conn = persistentContextTransaction.createConnection();
			if( aplosAbstractBean.isNew() ) {
				ApplicationUtil.getPersistenceContext().addNewBeans( aplosAbstractBean );
			}
			List<CascadeMemory> cascadeMemories = new ArrayList<CascadeMemory>();
			for( PersistentClass tempPersistentClass : persistentClassList ) {
				savePersistentClass( aplosAbstractBean, tempPersistentClass, isInsert, conn, cascadeMemories );
			}

			for( PersistentClass tempPersistentClass : persistentClassList ) {
				handleCascadeObjects( tempPersistentClass, aplosAbstractBean, conn );
			}
			
			PersistentClass dbPersistentClassWithField;
			for( PersistentClass tempPersistentClass : persistentClassList ) {
				List<String> updateCommands = new ArrayList<String>();
				for( int i = 0, n = cascadeMemories.size(); i < n; i++ ) {
					dbPersistentClassWithField = cascadeMemories.get( i ).getFieldInfo().getParentPersistentClass();
					/*
					 * This should fire most of the time except with Joined Tables where the dbPersistentClass
					 * is the root class but we don't want to update that here as the parentClass is generally
					 * a Db table also which will contain this field.
					 */
					if( !(dbPersistentClassWithField.isDbTable() && InheritanceType.JOINED_TABLE.equals( dbPersistentClassWithField.getInheritanceType() )) ) {
						dbPersistentClassWithField = dbPersistentClassWithField.getDbPersistentClass();
					}
					if( dbPersistentClassWithField.equals( tempPersistentClass ) ) {
						updateCommands.add( cascadeMemories.get( i ).reinstateBean(aplosAbstractBean) );
					}
				}
				if( updateCommands.size() > 0 ) {
					StringBuffer updateQueryStrBuf = new StringBuffer( "UPDATE " );
					updateQueryStrBuf.append( tempPersistentClass.determineSqlTableName() );
					updateQueryStrBuf.append( " SET " );
					updateQueryStrBuf.append( StringUtils.join( updateCommands, "," ) );
					updateQueryStrBuf.append( " WHERE id = " ).append( aplosAbstractBean.getId() );
					ApplicationUtil.executeSql( updateQueryStrBuf.toString(), conn );
				}
			}
			
			updateNonDbFieldInfos(aplosAbstractBean, persistentClass);
			
			ApplicationUtil.getPersistenceContext().updateCachedBean( aplosAbstractBean );
		} catch( AqlException ex ) {
			throw ex;
		} catch( Exception ex ) {
			throw new AqlException( ex );
		} finally {
			persistentContextTransaction.commitAndCloseOrRollback();
		}
	}
	
	public static void savePersistentClass( AplosAbstractBean aplosAbstractBean, PersistentClass persistentClass, boolean isInsert, Connection conn, List<CascadeMemory> cascadeMemories ) throws Exception {
		java.sql.PreparedStatement preparedStatement;
		String sql;
		if( isInsert ) {
			sql = persistentClass.getInsertSql();
			preparedStatement = conn.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
		} else {
			sql = persistentClass.getUpdateSql();
			preparedStatement = conn.prepareStatement( sql );
		}
		
		Map<FieldInfo,Cascade> cascadeMap = persistentClass.getCascadeFieldInfoMap();
		
		Object fieldValue;
		List<FieldInfo> fieldInfos;
		if( isInsert ) {
			fieldInfos = persistentClass.getInsertFieldInfos();
		} else {
			fieldInfos = persistentClass.getFullDbFieldInfosWithoutId();
		}
		for( int i = 0, n = fieldInfos.size(); i < n; i++ ) {
			fieldValue = fieldInfos.get( i ).getValue( aplosAbstractBean );
			if( fieldValue == null ) {
				if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
					sql = sql.replaceFirst( "\\?", "null" );
				}
				preparedStatement.setObject( i+1, null );
			} else {
				if( fieldInfos.get( i ) instanceof PersistentClassFieldInfo ) {
					if( ((AplosAbstractBean) fieldValue).getId() != null ) {
						preparedStatement.setLong( i+1, ((AplosAbstractBean) fieldValue).getId() );
						if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
							sql = sql.replaceFirst( "\\?", String.valueOf( ((AplosAbstractBean) fieldValue).getId() ) );
						}
					} else {
						if( !(((PersistentClassFieldInfo) fieldInfos.get( i )).isRemoveEmpty() && ((AplosAbstractBean) fieldValue).isEmptyBean()) ) {
							if( cascadeMap.keySet().contains( fieldInfos.get( i ) ) ) {
								CascadeMemory tempCascadeMemory = new CascadeMemory( fieldInfos.get( i ), (AplosAbstractBean) fieldValue );
								cascadeMemories.add( tempCascadeMemory );	
							} 
						}
						
						preparedStatement.setObject( i+1, null );
						if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
							sql = sql.replaceFirst( "\\?", "null" );
						}
					}
				} else {
					preparedStatement.setObject( i+1, fieldValue );
					if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
						sql = sql.replaceFirst( "\\?", String.valueOf( fieldValue ).replace( "\\", "\\\\" ).replace( "$", "\\$" ) );
					}
				}
			}
		}
		if( !isInsert ) {
			preparedStatement.setObject( fieldInfos.size()+1, aplosAbstractBean.getId() );
			if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
				sql = sql.replaceFirst( "\\?", String.valueOf( aplosAbstractBean.getId() ) );
			}
		}

		if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			logger.debug( sql );
		}
		preparedStatement.executeUpdate();
		
		if( isInsert ) {
			ResultSet rs = preparedStatement.getGeneratedKeys();
			if (rs.next()){
			    aplosAbstractBean.setId( rs.getLong(1) );
			}
		}
	}
	
	
	
	public static void handleCascadeObjects( PersistentClass persistentClass, AplosAbstractBean aplosAbstractBean, Connection conn ) {
		Map<FieldInfo,Cascade> cascadeMap = persistentClass.getFullCascadeFieldInfoMap();
		for( FieldInfo tempFieldInfo : cascadeMap.keySet() ) {
			try {
				if( tempFieldInfo.getParentPersistentClass().getTableClass().isAssignableFrom( aplosAbstractBean.getClass() ) ) {
					if( List.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
						if( tempFieldInfo instanceof CollectionFieldInfo 
								&& ((CollectionFieldInfo) tempFieldInfo).getRelationshipAnnotation() instanceof CollectionOfElements ) {
							saveCollectionCascadeElements( persistentClass, aplosAbstractBean, (CollectionFieldInfo) tempFieldInfo );
						} else {
							saveListCascadeBeans( aplosAbstractBean, tempFieldInfo );
						}
					} else if( Map.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
						if( tempFieldInfo instanceof CollectionFieldInfo && ((CollectionFieldInfo) tempFieldInfo).getRelationshipAnnotation() instanceof CollectionOfElements ) {
							saveMapCascadeElements( persistentClass, aplosAbstractBean, (CollectionFieldInfo) tempFieldInfo );
						} else {
							saveMapCascadeBeans( aplosAbstractBean, tempFieldInfo );
						}
					} else if( Set.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
						if( tempFieldInfo instanceof CollectionFieldInfo && ((CollectionFieldInfo) tempFieldInfo).getRelationshipAnnotation() instanceof CollectionOfElements ) {
							saveCollectionCascadeElements( persistentClass, aplosAbstractBean, (CollectionFieldInfo) tempFieldInfo );
						} else {
							saveSetCascadeBeans( aplosAbstractBean, tempFieldInfo );
						}
					} else {
						saveSingleCascadeBean( aplosAbstractBean, tempFieldInfo );	
					}
				}
			} catch( IllegalAccessException iaEx ) {
				ApplicationUtil.handleError( iaEx );
			}
		}
	}
	
	public static void saveCollectionCascadeElements( PersistentClass persistentClass, AplosAbstractBean aplosAbstractBean, CollectionFieldInfo collectionFieldInfo ) throws IllegalAccessException {
		collectionFieldInfo.getField().setAccessible(true);
		JunctionTable junctionTable = collectionFieldInfo.getJunctionTable( collectionFieldInfo.getParentPersistentClass().getDbPersistentClass().getTableClass() );
		Collection elementCollection = (Collection) collectionFieldInfo.getField().get( aplosAbstractBean );
		
		/*
		 * This can happen if it's a collection of elements
		 */
		if( elementCollection instanceof PersistentAbstractCollection && !((PersistentAbstractCollection) elementCollection).isInitialized() ) {
			return;
		}
		
		StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " );
		sqlBuf.append( junctionTable.getSqlTableName() );
		sqlBuf.append( " WHERE ");
		sqlBuf.append( junctionTable.getPersistentClassFieldInfo().getSqlName() );
		sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
		Connection conn = null;
		try {
			conn = ApplicationUtil.getPersistenceContext().getConnection();
			ApplicationUtil.executeSql( sqlBuf.toString(), conn );
			if( elementCollection != null && elementCollection.size() > 0 ) {
				sqlBuf = new StringBuffer( "INSERT INTO " );
				sqlBuf.append( junctionTable.getSqlTableName() );
				sqlBuf.append( " ( " ).append( junctionTable.getPersistentClassFieldInfo().getSqlName() );
				sqlBuf.append( "," ).append( collectionFieldInfo.getSqlName() );
				sqlBuf.append( " ) VALUES (" ).append( aplosAbstractBean.getId()  );
				sqlBuf.append( " , ?" );
				sqlBuf.append( ")" );
				PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );
			
				for( Object element : elementCollection ) {
					preparedStatement.setObject( 1, convertElement( collectionFieldInfo, element ) );
					preparedStatement.execute();
				}
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		}
	}
	
	public static Object convertElement( FieldInfo fieldInfo, Object element ) {
		if( fieldInfo.getApplicationType() instanceof EnumIntType ) {
			return ((Enum) element).ordinal();
		} else if( fieldInfo.getApplicationType() instanceof EnumStringType ) {
			return ((Enum) element).name();
		} else {
			return element;
		}
	}
	
	public static void saveSetCascadeBeans( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo ) throws IllegalAccessException {
		tempFieldInfo.getField().setAccessible(true);
		Set<AplosAbstractBean> beanSet = (Set<AplosAbstractBean>) tempFieldInfo.getField().get( aplosAbstractBean );
		
		if( beanSet != null && beanSet.size() > 0 ) {
			Set<AplosAbstractBean> iterableBeanSet = new HashSet<AplosAbstractBean>( beanSet );
			for( AplosAbstractBean tempAplosAbstractBean : iterableBeanSet ) {
				if( !tempAplosAbstractBean.isLazilyLoaded() && !tempAplosAbstractBean.isReadOnly() ) {
					boolean wasNew = tempAplosAbstractBean.isNew();
					if( wasNew ) {
						/*
						 * Remove these from set as the hashcode will change and needs to be reset
						 */
						beanSet.remove( tempAplosAbstractBean ); 
					}
					tempAplosAbstractBean.saveDetails();
					if( wasNew ) {
						beanSet.add( tempAplosAbstractBean );
					}
				}
			}
		}
	}
	
	public static void saveListCascadeBeans( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo ) throws IllegalAccessException {
		tempFieldInfo.getField().setAccessible(true);
		List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) tempFieldInfo.getField().get( aplosAbstractBean );
		
		if( beanList != null && beanList.size() > 0 ) {
			for( int i = 0, n = beanList.size(); i < n; i++ ) {
				if( !beanList.get( i ).isLazilyLoaded() && !beanList.get( i ).isReadOnly() ) {
					beanList.get( i ).saveDetails();
				}
			}
		}
	}
	
	public static void saveMapCascadeElements( PersistentClass persistentClass, AplosAbstractBean aplosAbstractBean, CollectionFieldInfo collectionFieldInfo ) throws IllegalAccessException {
		collectionFieldInfo.getField().setAccessible(true);
		JunctionTable junctionTable = collectionFieldInfo.getJunctionTable( persistentClass.getDbPersistentClass().getTableClass() );
		Map elementMap = (Map) collectionFieldInfo.getField().get( aplosAbstractBean );

		/*
		 * This can happen if it's a collection of elements
		 */
		if( elementMap instanceof PersistentAbstractCollection && !((PersistentAbstractCollection) elementMap).isInitialized() ) {
			return;
		}
		
		StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " );
		sqlBuf.append( junctionTable.getSqlTableName() );
		sqlBuf.append( " WHERE ");
		sqlBuf.append( junctionTable.getPersistentClassFieldInfo().getSqlName() );
		sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
		Connection conn = null;
		try {
			conn = ApplicationUtil.getPersistenceContext().getConnection();
			ApplicationUtil.executeSql( sqlBuf.toString(), conn );
			if( elementMap != null && elementMap.size() > 0 ) {
				sqlBuf = new StringBuffer( "INSERT INTO " );
				sqlBuf.append( junctionTable.getSqlTableName() );
				sqlBuf.append( " ( " ).append( junctionTable.getPersistentClassFieldInfo().getSqlName() );
				sqlBuf.append( "," ).append( collectionFieldInfo.getSqlName() );
				sqlBuf.append( "," ).append( collectionFieldInfo.getMapKeyFieldInfo().getSqlName() );
				sqlBuf.append( " ) VALUES (" ).append( aplosAbstractBean.getId()  );
				sqlBuf.append( " , ?, ? " );
				sqlBuf.append( ")" );
				PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );
			
				for( Object mapKey : elementMap.keySet() ) {
					preparedStatement.setObject( 1, convertElement( collectionFieldInfo, elementMap.get( mapKey ) ) );
					if( collectionFieldInfo.getMapKeyFieldInfo() instanceof PersistentMapKeyFieldInfo ) {
						preparedStatement.setObject( 2, ((AplosAbstractBean)mapKey).getId()  );
					} else {
						preparedStatement.setObject( 2, convertElement( collectionFieldInfo, mapKey ) );
					}
					preparedStatement.execute();
				}
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		}
	}
	
	public static void saveMapCascadeBeans( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo ) throws IllegalAccessException {
		tempFieldInfo.getField().setAccessible(true);
		Map<Object,AplosAbstractBean> beanMap = (Map<Object,AplosAbstractBean>) tempFieldInfo.getField().get( aplosAbstractBean );
		
		if( beanMap != null && beanMap.size() > 0 ) {
			Set iterableKeySet = new HashSet<Object>(beanMap.keySet());
			for( Object mapKey : iterableKeySet ) {
				if( !beanMap.get( mapKey ).isLazilyLoaded() && !beanMap.get( mapKey ).isReadOnly() ) {
					AplosAbstractBean removedBean = null;
					if( beanMap.get( mapKey ).isNew() ) {
						/*
						 * Remove these from set as the hashcode will change and needs to be reset
						 */
						removedBean = beanMap.remove( mapKey );
						removedBean.saveDetails();
					} else {
						beanMap.get( mapKey ).saveDetails();
					}
					if( removedBean != null ) {
						beanMap.put( mapKey, removedBean );
					}
				}
			}
		}
	}
	
	public static void saveSingleCascadeBean( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo ) throws IllegalAccessException {
		tempFieldInfo.getField().setAccessible(true);
		AplosAbstractBean tempBean = (AplosAbstractBean) tempFieldInfo.getField().get( aplosAbstractBean );
		if( tempBean != null && !tempBean.isLazilyLoaded() && !tempBean.isReadOnly() ) {
			if( !(tempFieldInfo instanceof PersistentClassFieldInfo 
					&& ((PersistentClassFieldInfo) tempFieldInfo).isRemoveEmpty()
					&& tempBean.isEmptyBean()) ) {
				tempBean.saveDetails();	
			}
		}
	}
	
	public static void updateNonDbFieldInfos( AplosAbstractBean aplosAbstractBean, PersistentClass persistentClass ) throws SQLException, IllegalAccessException{
		FieldInfo tempFieldInfo;
		PersistentClass dbPersistentClass = persistentClass.getDbPersistentClass();
		
		for( String mapKey : dbPersistentClass.getFullNonDbFieldInfoMap().keySet() ) {
			tempFieldInfo = dbPersistentClass.getFullNonDbFieldInfoMap().get( mapKey );
			if( tempFieldInfo.getParentPersistentClass().getTableClass().isAssignableFrom( aplosAbstractBean.getClass() ) ) {
				if( tempFieldInfo instanceof ForeignFieldInfo ) {
					updateForeignFieldInfo( aplosAbstractBean, tempFieldInfo );
					/*
					 * We don't want to update CollectionOfElements as these have already been updated
					 * in the handleCascadeObjects.
					 */
				} else if( tempFieldInfo instanceof CollectionFieldInfo 
						&& !(((CollectionFieldInfo) tempFieldInfo).getRelationshipAnnotation() instanceof CollectionOfElements) ) {
					if( tempFieldInfo instanceof PersistentCollectionFieldInfo ) {
						updatePersistentCollectionFieldInfo( aplosAbstractBean, tempFieldInfo, dbPersistentClass );
					} else  {
						updateCollectionFieldInfo( aplosAbstractBean, tempFieldInfo, dbPersistentClass );
					}
				}
			}
		}
		
		updateIndexableFields(aplosAbstractBean, dbPersistentClass);
	}
	
	private static void updateIndexableFields(AplosAbstractBean aplosAbstractBean, PersistentClass dbPersistentClass) throws SQLException, IllegalAccessException {
		for( FieldInfo fieldInfo : dbPersistentClass.getAliasedFieldInfoMap().values() ) {
			if( fieldInfo instanceof IndexableFieldInfo && !(fieldInfo instanceof CollectionFieldInfo&& !(((CollectionFieldInfo) fieldInfo).getRelationshipAnnotation() instanceof CollectionOfElements)) ) {
				((IndexableFieldInfo) fieldInfo).updateIndexFields(aplosAbstractBean);
			}
		}
	}
	
	public static void updateIndexField( List<AplosAbstractBean> beanList, IndexFieldInfo tempIndexFieldInfo ) throws SQLException, IllegalAccessException  {
		if( beanList != null ) {
			Class<?> tableClass = tempIndexFieldInfo.getParentPersistentClass().getTableClass();
			
			StringBuffer sqlBuf = new StringBuffer( "UPDATE " );
			sqlBuf.append( AplosAbstractBean.getTableName( tableClass ) );
			sqlBuf.append( " SET " ).append( tempIndexFieldInfo.getSqlName() );
			sqlBuf.append( " = ? WHERE id = ?" );
			Connection conn = ApplicationUtil.getPersistenceContext().getConnection();
			PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );
			if( beanList != null && beanList.size() > 0 ) {
				List<String> idList = new ArrayList<String>();
				AplosAbstractBean readOnlyBean;
				for( int i = 0, n = beanList.size(); i < n; i++ ) {
					((PersistenceBean) beanList.get( i )).getHiddenFieldsMap().put( tempIndexFieldInfo.getSqlName(), i );
					if( !((AplosAbstractBean) beanList.get( i )).isReadOnly() ) {
						readOnlyBean = ((AplosAbstractBean) beanList.get( i )).getReadOnlyBean();
						/*
						 * This can be null with new beans
						 */
						if( readOnlyBean != null ) {
							readOnlyBean.getHiddenFieldsMap().put( tempIndexFieldInfo.getSqlName(), i );
						} 
					}
					preparedStatement.setInt( 1, i );
					preparedStatement.setLong( 2, beanList.get(i).getId() );
					preparedStatement.execute();
					logger.debug( sqlBuf.toString() );
				}
			}
		}
	}
	
	private static void updateCollectionFieldInfo(  AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo, PersistentClass dbPersistentClass ) throws SQLException, IllegalAccessException {
		CollectionFieldInfo collectionFieldInfo = (CollectionFieldInfo) tempFieldInfo;
		JunctionTable collectionTable = collectionFieldInfo.getJunctionTable( dbPersistentClass.getTableClass() );
		String sqlTableName = collectionTable.getSqlTableName();

		if( List.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
			tempFieldInfo.getField().setAccessible(true);
			List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) tempFieldInfo.getField().get( aplosAbstractBean );
			
			if( beanList instanceof PersistentAbstractCollection && !((PersistentAbstractCollection)beanList).isInitialized() ) {
				return;
			}

			StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " );
			sqlBuf.append( sqlTableName );
			sqlBuf.append( " WHERE ");
			sqlBuf.append( collectionTable.getPersistentClassFieldInfo().getSqlName() );
			sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
			Connection conn = ApplicationUtil.getPersistenceContext().getConnection();
			ApplicationUtil.executeSql( sqlBuf.toString(), conn );
			
			if( beanList != null && beanList.size() > 0 ) {
				if( beanList.size() > 0 ) {
					sqlBuf = new StringBuffer( "INSERT INTO " );
					sqlBuf.append( sqlTableName );
					sqlBuf.append( " ( " ).append( collectionTable.getPersistentClassFieldInfo().getSqlName() );
					sqlBuf.append( "," ).append( collectionFieldInfo.getSqlName() );
					if( collectionFieldInfo.getIndexFieldInfo() != null ) {
						sqlBuf.append( "," ).append( collectionFieldInfo.getIndexFieldInfo().getSqlName() );
					}
					sqlBuf.append( " VALUES (" ).append( aplosAbstractBean.getId()  );
					sqlBuf.append( " , ? " );
					if( collectionFieldInfo.getIndexFieldInfo() != null ) {
						sqlBuf.append( ", ? " );
					}
					sqlBuf.append( ")" );
					PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );

					for( int i = 0, n = beanList.size(); i < n; i++ ) {
						if( beanList.get( i ).getId() != null ) {
							int columnIdx = 1;
							preparedStatement.setLong( columnIdx++, beanList.get(i).getId() );
							if( collectionFieldInfo.getIndexFieldInfo() != null ) {
								preparedStatement.setInt( columnIdx++, i );
							}
							preparedStatement.execute();
						} else {
							StringBuffer strBuf = new StringBuffer();
							strBuf.append( beanList.get( i ).getClass().getSimpleName() );
							strBuf.append( "(" ).append( beanList.get( i ).toString() );
							strBuf.append( " has not yet been saved, please save child object before saving the " );
							strBuf.append( aplosAbstractBean.getClass().getSimpleName() );
							ApplicationUtil.handleError( new Exception( strBuf.toString() ) );
							return;
						}
					}
				}
			}
		}
	}
	
	private static void updatePersistentCollectionFieldInfo( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo, PersistentClass dbPersistentClass ) throws IllegalAccessException, SQLException {
		PersistentCollectionFieldInfo collectionFieldInfo = (PersistentCollectionFieldInfo) tempFieldInfo;
		JunctionTable collectionTable = collectionFieldInfo.getJunctionTable( dbPersistentClass.getTableClass() );
		if( collectionTable == null ) {
			ApplicationUtil.handleError( new Exception( "Cannot find collection table " + collectionFieldInfo.getSqlName() + " for " + dbPersistentClass.determineSqlTableName() + " tableKeys " + CommonUtil.join( collectionFieldInfo.getJunctionTables().keySet() ) ) );
		}
		String sqlTableName = collectionTable.getSqlTableName();		

		String propertyName = tempFieldInfo.getField().getName();

			
		Connection conn = ApplicationUtil.getPersistenceContext().getConnection();

		tempFieldInfo.getField().setAccessible(true);
		Object beanCollection = tempFieldInfo.getField().get( aplosAbstractBean );
		
		if( beanCollection instanceof PersistentAbstractCollection && !((PersistentAbstractCollection)beanCollection).isInitialized() ) {
			return;
		}
		
		StringBuffer sqlBuf = new StringBuffer( "DELETE FROM " );
		sqlBuf.append( sqlTableName );
		sqlBuf.append( " WHERE ");
		sqlBuf.append( collectionTable.getPersistentClassFieldInfo().getSqlName() );
		sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
		ApplicationUtil.executeSql( sqlBuf.toString(), conn );

		if( List.class.isAssignableFrom( tempFieldInfo.getField().getType() )
				|| Set.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
			Collection<AplosAbstractBean> beanList = (Collection<AplosAbstractBean>) beanCollection;

			
			if( beanList != null && beanList.size() > 0 ) {
				List<String> idList = new ArrayList<String>();
				if( beanList.size() > 0 ) {
					sqlBuf = new StringBuffer( "INSERT INTO " );
					sqlBuf.append( sqlTableName );
					sqlBuf.append( " ( " ).append( collectionTable.getPersistentClassFieldInfo().getSqlName() );
					sqlBuf.append( "," ).append( collectionFieldInfo.getSqlName() );
					if( collectionFieldInfo.getIndexFieldInfo() != null ) {
						sqlBuf.append( "," ).append( collectionFieldInfo.getIndexFieldInfo().getSqlName() );
					}
					if( collectionFieldInfo.isPolymorphic() ) {
						sqlBuf.append( "," ).append( collectionFieldInfo.getPolymorphicFieldInfo().getSqlName() );
					}
					sqlBuf.append( " ) VALUES (" ).append( aplosAbstractBean.getId()  );
					sqlBuf.append( " , ? " );
					if( collectionFieldInfo.getIndexFieldInfo() != null ) {
						sqlBuf.append( ", ? " );
					}
					if( collectionFieldInfo.isPolymorphic() ) {
						sqlBuf.append( " , ? " );
					}
					sqlBuf.append( ")" );
					PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );

					for( AplosAbstractBean tempAplosAbstractBean : beanList ) {
						if( tempAplosAbstractBean.getId() != null ) {
							int columnIdx = 1;
							preparedStatement.setLong( columnIdx++, tempAplosAbstractBean.getId() );

							if( collectionFieldInfo.getIndexFieldInfo() != null ) {
								preparedStatement.setInt( columnIdx++, ((List) beanList).indexOf( tempAplosAbstractBean ) );
							}
							if( collectionFieldInfo.isPolymorphic() ) {
								preparedStatement.setString( columnIdx++, ApplicationUtil.getPersistentApplication().getDiscriminatorValue( tempAplosAbstractBean ) );
							}
							preparedStatement.execute();
						} else {
							StringBuffer strBuf = new StringBuffer();
							strBuf.append( tempAplosAbstractBean.getClass().getSimpleName() );
							strBuf.append( "(" ).append( tempAplosAbstractBean.toString() );
							strBuf.append( " has not yet been saved, please save child object before saving the " );
							strBuf.append( aplosAbstractBean.getClass().getSimpleName() );
							ApplicationUtil.handleError( new Exception( strBuf.toString() ) );
							return;
						}
					}
				}
			}
		} else if( Map.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
			tempFieldInfo.getField().setAccessible(true);
			Map<Object, AplosAbstractBean> beanMap = (Map<Object,AplosAbstractBean>) beanCollection;
			if( beanMap != null && beanMap.size() > 0 ) {
				if( beanMap.size() > 0 ) {
					sqlBuf = new StringBuffer( "INSERT INTO " );
					sqlBuf.append( sqlTableName );
					sqlBuf.append( " ( " ).append( collectionTable.getPersistentClassFieldInfo().getSqlName() );
					sqlBuf.append( "," ).append( collectionFieldInfo.getSqlName() );
					sqlBuf.append( "," ).append( collectionFieldInfo.getMapKeyFieldInfo().getSqlName() );
					if( collectionFieldInfo.isPolymorphic() ) {
						sqlBuf.append( "," ).append( collectionFieldInfo.getPolymorphicFieldInfo().getSqlName() );
					}
					sqlBuf.append( " ) VALUES (" ).append( aplosAbstractBean.getId()  );
					sqlBuf.append( " , ? " );
					sqlBuf.append( " , ? " );
					if( collectionFieldInfo.isPolymorphic() ) {
						sqlBuf.append( " , ? " );
					}
					sqlBuf.append( ")" );
					PreparedStatement preparedStatement = ApplicationUtil.getPreparedStatement( conn, sqlBuf.toString() );
					
					for( Object beanMapKey : beanMap.keySet() ) {
						if( beanMapKey != null ) {
							if( beanMap.get( beanMapKey ).getId() != null ) {
								preparedStatement.setLong( 1, beanMap.get( beanMapKey ).getId() );
								if( collectionFieldInfo.getMapKeyFieldInfo() instanceof PersistentMapKeyFieldInfo ) {
									preparedStatement.setObject( 2, ((AplosAbstractBean) beanMapKey).getId() );
								} else {
									preparedStatement.setObject( 2, beanMapKey );
								}
								if( collectionFieldInfo.isPolymorphic() ) {
									preparedStatement.setString( 3, ApplicationUtil.getPersistentApplication().getDiscriminatorValue( beanMap.get( beanMapKey ) ) );
								}
								preparedStatement.execute();
							} else {
								StringBuffer strBuf = new StringBuffer();
								strBuf.append( beanMap.get( beanMapKey ).getClass().getSimpleName() );
								strBuf.append( "(" ).append( beanMap.get( beanMapKey ).toString() );
								strBuf.append( " has not yet been saved, please save child object before saving the " );
								strBuf.append( aplosAbstractBean.getClass().getSimpleName() );
								ApplicationUtil.handleError( new Exception( strBuf.toString() ) );
								return;
							}
						}
					}
				}
			}
		}
	}
	
	private static void updateForeignFieldInfo( AplosAbstractBean aplosAbstractBean, FieldInfo tempFieldInfo ) throws IllegalAccessException, SQLException {
		Class<?> tableClass = ((ForeignFieldInfo) tempFieldInfo).getPersistentClass().getTableClass();
		String idClause = null;
		Connection conn = ApplicationUtil.getPersistenceContext().getConnection();
		
		try {
			String propertyName = tempFieldInfo.getField().getName();
			boolean updateForeignKey = true;

			if( List.class.isAssignableFrom( tempFieldInfo.getField().getType() ) ) {
				tempFieldInfo.getField().setAccessible(true);
				List<AplosAbstractBean> beanList = (List<AplosAbstractBean>) tempFieldInfo.getField().get( aplosAbstractBean );
				if( beanList instanceof PersistentAbstractCollection && !((PersistentAbstractCollection) beanList).isInitialized() ) {
					updateForeignKey = false;
				} else {
					if( beanList != null && beanList.size() > 0 ) {
						List<String> idList = new ArrayList<String>();
						for( int i = 0, n = beanList.size(); i < n; i++ ) {
							if( beanList.get( i ).getId() != null ) {
								idList.add( beanList.get(i).getId().toString() );	
							} else {
								StringBuffer strBuf = new StringBuffer();
								strBuf.append( beanList.get( i ).getClass().getSimpleName() );
								strBuf.append( " (" ).append( beanList.get( i ).toString() );
								strBuf.append( ") has not yet been saved, please save child object before saving the " );
								strBuf.append( aplosAbstractBean.getClass().getSimpleName() );
								ApplicationUtil.handleError( new Exception( strBuf.toString() ) );
								return;
							}
						}
						if( idList.size() > 0 ) {
							idClause = "IN (" + StringUtils.join( idList, "," ) + ")";
						}
					}
				}
			} else {
				tempFieldInfo.getField().setAccessible(true);
				AplosAbstractBean tempBean = (AplosAbstractBean) tempFieldInfo.getField().get( aplosAbstractBean );
				if( tempBean != null ) {
					idClause = " = " + tempBean.getId();
				}
			}
			
			if( updateForeignKey ) {
				StringBuffer sqlBuf = new StringBuffer( "UPDATE " );
				sqlBuf.append( AplosAbstractBean.getTableName( tableClass ) );
				sqlBuf.append( " SET " ).append( ((ForeignFieldInfo) tempFieldInfo).getForeignFieldInfo().getSqlName() );
				sqlBuf.append( " = null" ).append( " WHERE ");
				sqlBuf.append( ((ForeignFieldInfo) tempFieldInfo).getForeignFieldInfo().getSqlName() );
				sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
				
				ApplicationUtil.executeSql( sqlBuf.toString(), conn );
				
				if( idClause != null ) {	
					sqlBuf = new StringBuffer( "UPDATE " );
					sqlBuf.append( AplosAbstractBean.getTableName( tableClass ) );
					sqlBuf.append( " SET " ).append( ((ForeignFieldInfo) tempFieldInfo).getForeignFieldInfo().getSqlName() );
					sqlBuf.append( " = " ).append( aplosAbstractBean.getId() );
					sqlBuf.append( " WHERE id ").append( idClause );
					
					ApplicationUtil.executeSql( sqlBuf.toString(), conn );
				}
			}
		} catch( IllegalAccessException iaEx ) {
			ApplicationUtil.handleError( iaEx );
		}
	}
}
