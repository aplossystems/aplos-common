package com.aplos.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.aplos.common.MenuCacher;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.facelets.AplosFaceletCache;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.AplosModuleFilterer;
import com.aplos.common.persistence.PersistenceContext;
import com.aplos.common.persistence.PersistentApplication;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldLoader.FieldLoader;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.persistence.type.applicationtype.BigIntType;
import com.aplos.common.persistence.type.applicationtype.BooleanType;
import com.aplos.common.persistence.type.applicationtype.DateTimeType;
import com.aplos.common.persistence.type.applicationtype.DecimalType;
import com.aplos.common.persistence.type.applicationtype.DoubleType;
import com.aplos.common.persistence.type.applicationtype.IntType;
import com.aplos.common.persistence.type.applicationtype.LongTextType;
import com.aplos.common.persistence.type.applicationtype.VarCharType;
import com.aplos.common.threads.JobScheduler;

public class ApplicationUtil {
	private static Logger logger = Logger.getLogger( ApplicationUtil.class );
	private static Map<Class<? extends AplosAbstractBean>, Map<String, FieldLoader>> fieldLoaderMap = new HashMap<Class<? extends AplosAbstractBean>,Map<String,FieldLoader>>();
	private static Map<Class<? extends AplosAbstractBean>, Map<String, FieldInfo>> fieldInfoSetterMap = new HashMap<Class<? extends AplosAbstractBean>,Map<String,FieldInfo>>();
	private static Map<Integer, ApplicationType> mySqlApplicationTypeMap;
	
	static {
		mySqlApplicationTypeMap = new HashMap<Integer, ApplicationType>();
		mySqlApplicationTypeMap.put( new Integer( 8 ), new DoubleType() );
		mySqlApplicationTypeMap.put( new Integer( -5 ), new BigIntType() );
		mySqlApplicationTypeMap.put( new Integer( 4 ), new IntType() );
		mySqlApplicationTypeMap.put( new Integer( -7 ), new BooleanType() );
		mySqlApplicationTypeMap.put( new Integer( 93 ), new DateTimeType() );
		mySqlApplicationTypeMap.put( new Integer( 12 ), new VarCharType() );
		mySqlApplicationTypeMap.put( new Integer( -1 ), new LongTextType() );
		mySqlApplicationTypeMap.put( new Integer( 3 ), new DecimalType() );
	}
	
	private static ThreadLocal<PersistenceContext> persistenceContextTl = new ThreadLocal<PersistenceContext>();

	public static PersistenceContext getPersistenceContext() {
		return getPersistenceContext( true );
	}
	
	public static PersistentClass getPersistentClass( Class<?> tableClass ) {
		return getPersistentApplication().getPersistentClassMap().get( tableClass );
	}
	
	public static Class getClass( Object object ) {
		return object.getClass();
	}
	
	public static PersistentClass getDbPersistentClass( AplosAbstractBean aplosAbstractBean ) {
		return ApplicationUtil.getPersistentClass( aplosAbstractBean.getClass() ).getDbPersistentClass();
	}
	
	public static void startNewTransaction() {
		
	}

	public static Map<String, FieldLoader> getFieldLoaderMap( Class<? extends AplosAbstractBean> beanClass ) {
		if( fieldLoaderMap.get( beanClass ) == null ) {
			fieldLoaderMap.put( beanClass, new HashMap<String, FieldLoader>() );
		}
		return fieldLoaderMap.get( beanClass );
	}

	public static Map<String, FieldInfo> getFieldInfoSetterMap( Class<? extends AplosAbstractBean> beanClass ) {
		if( fieldInfoSetterMap.get( beanClass ) == null ) {
			fieldInfoSetterMap.put( beanClass, new HashMap<String, FieldInfo>() );
		}
		return fieldInfoSetterMap.get( beanClass );
	}
	
	public static PersistenceContext getPersistenceContext( boolean create ) {
		PersistenceContext persistenceContext = persistenceContextTl.get();
		if( persistenceContext == null && create ) {
			persistenceContext = new PersistenceContext();
			persistenceContextTl.set( persistenceContext );
		}
		return persistenceContext;
	}

	public static AplosModuleFilterer getAplosModuleFilterer() {
		return getAplosModuleFilterer(getAplosContextListener());
	}
	
	public static void handleError( Throwable ex ) {
		getAplosContextListener().handleError( ex );
	}
	
	public static void handleError( Throwable ex, String message ) {
		getAplosContextListener().handleError( ex, message );
	}
	
	public static void handleError( Throwable ex, boolean redirect ) {
		getAplosContextListener().handleError( ex, redirect );
	}
	
	public static void handleError( String message, boolean isShowingMessage ) {
		getAplosContextListener().handleError( new Exception( message ), !isShowingMessage );
		if( isShowingMessage ) {
			JSFUtil.addMessageForWarning( message );
		}
	}

	public static AplosModuleFilterer getAplosModuleFilterer(
			AplosContextListener aplosContextListener) {
		return aplosContextListener.getAplosModuleFilterer();
	}

	public static JobScheduler getJobScheduler() {
		return getAplosContextListener().getJobScheduler();
	}

	public static AplosFaceletCache getAplosFaceletCache() {
		return getAplosContextListener().getAplosFaceletCache();
	}
	
	public static Map<Thread,Map<String,Object>> getThreadSessionMap() {
		return ApplicationUtil.getAplosContextListener().getThreadSessionMap();
	}

	public static AplosContextListener getAplosContextListener() {
		return AplosContextListener.getAplosContextListener();
	}

	public static PersistentApplication getPersistentApplication() {
		return AplosContextListener.getAplosContextListener().getPersistentApplication();
	}
	
	public static Connection getConnection() throws SQLException {
		return AplosContextListener.getAplosContextListener().getPersistentApplication().getConnection();
	}

	public static MenuCacher getMenuCacher() {
		AplosContextListener aplosContextListener = getAplosContextListener();
		if (aplosContextListener != null) {
			return aplosContextListener.getMenuCacher();
		} else {
			return null;
		}
	}
	
	public static void closeConnection( Connection conn ) {
		try {
			if( conn != null ) {
				conn.close();
			}
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		}
	}
	
	public static void rollbackConnection( Connection conn ) {
		try {
			if( conn != null ) {
				conn.rollback();
			}
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		}
	}

	public static boolean executeSql( String sql ) {
		Connection conn = null;
		boolean statementExecuted = false;
		try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			executeSql(sql, conn);
			statementExecuted = true;
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx, sql);
		} finally {
			closeConnection( conn );
		}
		return statementExecuted;
	}
	
	public static Long insertQueryGetId(String query) {
	    Long lastIdx=-1l;
		Connection conn = null;
	    try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
	        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
	        stmt.execute();

	        ResultSet rs = stmt.getGeneratedKeys();
	        if (rs.next()){
	            lastIdx=rs.getLong(1);
	        }
	        rs.close();

	        stmt.close();
	    } catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		}
	    
	    return lastIdx;
	}

	public static void executeSql( String sql, Connection conn ) throws SQLException {
		logger.debug( "Excecute SQL: " + sql );
		conn.prepareStatement( sql ).execute();
	}

	public static boolean executeBatchSql( List<String> sqlStatements ) {
		Connection conn = null;
		boolean statementExecuted = false;
		try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			for( int i = 0, n = sqlStatements.size(); i < n; i++ ) {
				stmt.addBatch(sqlStatements.get( i ));	
			}
			stmt.executeBatch();
			conn.commit();
		} catch( SQLException sqlEx ) {
			try {
				if( conn != null ) {
					conn.rollback();
				}
			} catch( SQLException ex ) {
				getAplosContextListener().handleError(ex);
			}
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		}
		return statementExecuted;
	}

	public static PreparedStatement getPreparedStatement( Connection conn, String sql ) {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn.prepareStatement( sql );
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		}
		return preparedStatement;
	}

	public static List<Object[]> getResults( PreparedStatement preparedStatement ) {
		Connection conn = null;
		List<Object[]> resultList = null;
		try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			resultList = getResults( conn, preparedStatement );
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		} 
		return resultList;
	}

	public static List<Object[]> getResults( Connection conn, PreparedStatement preparedStatement ) {
		ResultSet resultSet = null;
		List<Object[]> resultList = new ArrayList<Object[]>();
		try {
//			CommonUtil.timeTrial( "Start");
			resultSet = preparedStatement.executeQuery();
//			CommonUtil.timeTrial( "Middle");
			ResultSetMetaData rsmd = resultSet.getMetaData();
			int columnCount = rsmd.getColumnCount();
			if( resultSet != null ) {
				Object[] tempObjectArray;
				ApplicationType[] columnApplicationTypes = new ApplicationType[ columnCount ];
				for( int i = 0, n = columnApplicationTypes.length; i < n; i++ ) {
					columnApplicationTypes[ i ] = mySqlApplicationTypeMap.get( rsmd.getColumnType( i + 1 ) );
				}
				while( resultSet.next() ) {
					tempObjectArray = new Object[ columnCount ];
					resultList.add( tempObjectArray );
					for( int i = 0; i < columnCount; i++ ) {
						if( columnApplicationTypes[ i ] == null ) {
							tempObjectArray[ i ] = resultSet.getObject( i + 1 );
							ApplicationUtil.handleError( new Exception( "Column type not found" ) );
						} else {
							tempObjectArray[ i ] = columnApplicationTypes[ i ].getResultSetObject(resultSet, i + 1 );
						}
						if( tempObjectArray[ i ] instanceof Timestamp ) {
							tempObjectArray[ i ] = new Date( ((Timestamp) tempObjectArray[ i ]).getTime() );
						}
					}
				}
			}
//			CommonUtil.timeTrial( "End");
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		}
		return resultList;
	}

	public static Object getFirstUniqueResult( String sql ) {
		Object[] firstResult = getFirstResult( sql );
		if( firstResult != null && firstResult.length > 0 ) {
			return firstResult[ 0 ];
		} else {
			return null;
		}
	}
	

	public static Object[] getFirstResult( String sql ) {
		Connection conn = null;
		List<Object[]> resultList = null;
		try {
			logger.debug( "Excecute SQL: " + sql );
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			PreparedStatement preparedStatment = conn.prepareStatement( sql );
			preparedStatment.setMaxRows( 1 );
			resultList = getResults( conn, preparedStatment );
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		} 
		
		if( resultList != null && resultList.size() > 0 ) {
			return resultList.get( 0 );
		} else {
			return null;
		}
	}

	public static List<Object[]> getResults( String sql ) {
		Connection conn = null;
		List<Object[]> resultList = null;
		try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			resultList = getResults( conn, conn.prepareStatement( sql ) );
			logger.debug( sql );
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		} 
		return resultList;
	}
	
	

	public static List<Object[]> list( String sql ) {
		Connection conn = null;
		boolean statementExecuted = false;
		List<Object[]> resultRows = new ArrayList<Object[]>();
		try {
			conn = getAplosContextListener().getPersistentApplication().getConnection();
			ResultSet rs = conn.prepareStatement( sql ).executeQuery(); 
			int columnCount = rs.getMetaData().getColumnCount();
			while( rs.next() ) {
				Object[] columnValues = new Object[ columnCount ];
				resultRows.add( columnValues );
				for( int i = 0, n = columnValues.length; i < n; i++ ) {
					columnValues[ i ] = rs.getObject( i + 1 );
				}
			}
		} catch( SQLException sqlEx ) {
			getAplosContextListener().handleError(sqlEx);
		} finally {
			closeConnection( conn );
		}
		return resultRows;
	}
	
	/*
	 * Imported from HibernateUtil
	 */
	

	
	public static Long getIdFromProxy( AplosAbstractBean aplosAbstractBean ) {
		return aplosAbstractBean.getId();
	}
	
	public static String[] extractColumnNames(String hqlStr,
			HashMap<String, String> aliasMap) {
		Pattern pattern = Pattern.compile("select(.*?)from",
				Pattern.CASE_INSENSITIVE);
		Matcher m = pattern.matcher(hqlStr);
		new ArrayList<String>();
		if (m.find()) {
			String combinedColumns = m.group(1).trim();
			String[] columns = combinedColumns.split(",");
			for (int i = 0, n = columns.length; i < n; i++) {
				columns[i] = stripAlias(columns[i], aliasMap);
			}
			return columns;
		} else {
			return null;
		}
	}

	public static String[] getResultFieldNames(String[] columns) {
		String[] resultFieldNames = new String[columns.length];
		System.arraycopy(columns, 0, resultFieldNames, 0, columns.length);
		for (int i = 0, n = columns.length; i < n; i++) {
			if (columns[i].toLowerCase().indexOf(" as ") > -1) {
				resultFieldNames[i] = columns[i].substring(
						columns[i].toLowerCase().indexOf(" as ") + 4).trim();
			}
		}

		return resultFieldNames;
	}
  
	public static String stripAlias(String variableName,
			HashMap<String, String> aliasMap) {
		variableName = variableName.trim();
		for (String aliasKey : aliasMap.keySet()) {
			if (variableName.contains("(")) {
				if (aliasMap.get(aliasKey).equals("")) {
					variableName = variableName.replaceAll("\\(" + aliasKey
							+ "\\.", "(" + aliasMap.get(aliasKey));
				} else {

					if (variableName.endsWith("." + aliasKey)
							|| variableName.equals(aliasKey)) {
						variableName = variableName.replaceAll(
								"\\(" + aliasKey, "(" + aliasMap.get(aliasKey));
					} else {
						variableName = variableName.replaceAll("\\(" + aliasKey
								+ "\\.", "(" + aliasMap.get(aliasKey) + ".");
					}
				}

			} else {
				if (aliasMap.get(aliasKey).equals("")) {
					variableName = variableName.replaceAll("^" + aliasKey
							+ "\\.", aliasMap.get(aliasKey));
				} else {
					if (variableName.endsWith("." + aliasKey)
							|| variableName.equals(aliasKey)) {
						variableName = variableName.replaceAll("^" + aliasKey,
								aliasMap.get(aliasKey));
					} else {
						variableName = variableName.replaceAll("^" + aliasKey
								+ "\\.", aliasMap.get(aliasKey) + ".");
					}
				}
			}
		}
		return variableName;
	}
	
  public static List<AplosBean> getParentsByChildInList(Class<? extends AplosAbstractBean> parentclass, Long idOfChildWeHave, String childListName) {
  	String hql = "SELECT parentBean FROM " + AplosBean.getTableName(parentclass) +
  				" parentBean WHERE " + idOfChildWeHave +
  				" IN (SELECT childBean.id FROM parentBean." + childListName + " childBean)";
  	return new BeanDao(hql).getAll();
  }

	public static boolean checkUniqueValue(AplosBean aplosBean,
			String uniqueColumnName) {
		return checkUniqueValue(aplosBean, uniqueColumnName,
				(Class<? extends AplosBean>) getClass(aplosBean));
	}

	public static boolean checkUniqueValue(AplosBean aplosBean,
			String uniqueColumnName, Class<? extends AplosBean> classToSearch) {
		Method method;
		Object val = "";
		try {

			method = getClass(aplosBean).getMethod(
					"get" + uniqueColumnName.substring(0, 1).toUpperCase()
							+ uniqueColumnName.substring(1));
			try {
				val = method.invoke(aplosBean);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		AplosBean dbBean = null;

		if (val instanceof AplosBean) {
			// we should have checked any object we are using before hand, so
			// really, id should never be null
			if (((AplosBean) val).getId() != null) {
				BeanDao aqlBeanDao = new BeanDao(classToSearch)
						.addWhereCriteria("bean."
								+ CommonUtil
										.firstLetterToLowerCase(uniqueColumnName)
								+ ".id='" + ((AplosBean) val).getId() + "'");
				if (aplosBean.getId() != null) {
					aqlBeanDao.addWhereCriteria("bean.id != " + aplosBean.getId());
				}
				dbBean = (AplosBean) aqlBeanDao.getFirstBeanResult();
			}
		} else {
			BeanDao aqlBeanDao = new BeanDao(classToSearch)
					.addWhereCriteria("bean."
							+ CommonUtil
									.firstLetterToLowerCase(uniqueColumnName)
							+ "='" + val.toString() + "'");
			if (aplosBean.getId() != null) {
				aqlBeanDao.addWhereCriteria("bean.id != " + aplosBean.getId());
			}
			dbBean = (AplosBean) aqlBeanDao.getFirstBeanResult();
		}

		if (dbBean == null) {
			return true;
		} else {
			return false;
		}
	}
	
	public static AplosBean loadBeanByUniqueValue(AplosBean aplosBean, String uniqueColumnName) {
		Method method;
		Object val="";
		try {

		  method = aplosBean.getClass().getMethod("get" + uniqueColumnName.substring(0,1).toUpperCase() + uniqueColumnName.substring(1));
		  try {
			  val = method.invoke(aplosBean);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		AplosBean dbBean = null;

		if (val instanceof AplosBean) {
			//we should have checked any object we are using before hand, so really, id should never be null
			if (((AplosBean)val).getId() != null) {
				BeanDao beanDao = new BeanDao( aplosBean.getClass() ).addWhereCriteria( "bean." + CommonUtil.firstLetterToLowerCase( uniqueColumnName ) + ".id='" + ((AplosBean)val).getId() + "'");
				dbBean = (AplosBean) beanDao.getFirstBeanResult();
			}
		} else {
			BeanDao beanDao = new BeanDao( aplosBean.getClass() ).addWhereCriteria( "bean." + CommonUtil.firstLetterToLowerCase( uniqueColumnName ) +  "= :uniqueColumnName" );
			beanDao.setNamedParameter( "uniqueColumnName", val.toString() );
			dbBean = beanDao.getFirstBeanResult();
		}

		return dbBean;
	}

	/** TODO: Update method to accept and search on multiple columns **/
	public static AplosBean saveNewOrLoadBeanByUniqueValue(AplosBean bean, String uniqueColumnName) {
		AplosBean dbBean = loadBeanByUniqueValue(bean, uniqueColumnName);

		if (dbBean != null) {
			bean = dbBean;
		} else {
			bean.saveDetails();
		}

		return bean;
	}
}
