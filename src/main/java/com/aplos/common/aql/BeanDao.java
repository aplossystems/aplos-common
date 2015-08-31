package com.aplos.common.aql;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.Modifier;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.aplos.common.aql.antlr.AqlParser;
import com.aplos.common.aql.aqltables.AqlSubTable;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqltables.unprocessed.ConditionalAqlTable;
import com.aplos.common.aql.aqltables.unprocessed.JoinedAqlTable;
import com.aplos.common.aql.aqltables.unprocessed.ReverseJoinedAqlTable;
import com.aplos.common.aql.aqltables.unprocessed.UnprocessedAqlSubTable;
import com.aplos.common.aql.aqltables.unprocessed.UnprocessedAqlTable;
import com.aplos.common.aql.aqlvariables.SubQueryAqlVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.selectcriteria.PersistentClassSelectCriteria;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.ArrayRowDataReceiver;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistenceContext;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class BeanDao {
	private List<SelectCriteria> unprocessedSelectCriteriaList = new ArrayList<SelectCriteria>();
	private int latestTableIdx = 0;
	private AqlSubTable activeBeanHandler;
	private int columnCount = 0;
	private List<String> unprocessedFilterList = new ArrayList<String>();
	private List<String> unprocessedSearchList = new ArrayList<String>();
	private String orderByCriteria;
	private String groupByCriteria;
	private WhereConditionGroup whereConditionGroup = new WhereConditionGroup();
	private AqlSubTable parentTable;
	private Set<UnprocessedAqlSubTable> subQueryTables = new HashSet<UnprocessedAqlSubTable>(); 
	private Map<String,String> namedParameters = new HashMap<String,String>();
	private int firstRowIdx = -1;
	private boolean isCreatingPersistentSelectCriteria = true;
	private boolean isGeneratingBeans = true;

	/*
	 * Imported from AqlBeanDao
	 */
	private Class<? extends AplosAbstractBean> beanClass;
	private Class<? extends AplosAbstractBean> listBeanClass;
	private String entity;
	private String binding;
	private String countFields = "";
	private Class<? extends BackingPage> listPageClass;
	private Class<? extends BackingPage> editPageClass;
	private int maxResults = -1;
	private boolean isReadOnly = false;
	private Boolean isReturningActiveBeans = true;
	private Boolean isReturningArchivedBeans = false;
	private boolean isSqlQuery = false;
	private Map<String,UnprocessedAqlTable> unprocessedAqlTables = new HashMap<String,UnprocessedAqlTable>();
	private Boolean isStrictPolymorphism = null;
	private boolean isInitialisingPaths = true;
	private PersistentClass rootPersistentClass;
	private String rootAlias;
	private UnprocessedAqlTable rootTable;
	private boolean isAddingDefaultOrderByToLists = true;
	private AqlParser aqlParser = AqlParser.getInstance( "" );

	public BeanDao( String aql ) {
		// TODO parse string, it must contain a valid beanClass in FROM statement
//		this( beanClass, "bean" );
		try {
			getAqlParser().updateString( aql );
			getAqlParser().parseSelectStatement(this);
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError( tsEx );
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError( rEx );
		}
	}
	
	protected BeanDao() {
		
	}

	public BeanDao(Class<? extends AplosAbstractBean> beanClass) {
		this( beanClass, "bean" );
	}

	public BeanDao(Class<? extends AplosAbstractBean> beanClass, String beanAlias ) {
		init( beanClass, null, beanAlias );
	}

	public BeanDao(Class<? extends AplosAbstractBean> beanClass, Class<? extends BackingPage> backingPageClass ) {
		init( beanClass, backingPageClass, "bean" );
	}

	public void init( Class<? extends AplosAbstractBean> beanClass, Class<? extends BackingPage> backingPageClass, String beanAlias ) {
		setBeanClass( beanClass, backingPageClass, beanAlias );
	}
	
	public void processAqlTables( ProcessedBeanDao processedBeanDao ) {
		List<UnprocessedAqlTable> tempUnprocessedAqlTables = new ArrayList<UnprocessedAqlTable>(getUnprocessedAqlTables().values());
		while( tempUnprocessedAqlTables.size() > 0 ) {
			boolean tableAdded = false;
			for( int i = tempUnprocessedAqlTables.size() - 1; i >= 0; i-- ) {
				AqlTableAbstract aqlTable = tempUnprocessedAqlTables.get( i ).createAqlTable( processedBeanDao, true );
				if( aqlTable != null ) {
					tableAdded = true;
					tempUnprocessedAqlTables.remove( i );
				} 
			}	
			if( !tableAdded ) {
				ApplicationUtil.handleError( new Exception( "AqlTable creation caught in a loop contact a system administrator" ) );
				return;
			}
		}
	}

	public BeanDao copy( BeanDao aqlBeanDao ) {
		setListBeanClass( aqlBeanDao.getListBeanClass() );
		setRootAlias( aqlBeanDao.getRootAlias() );
		setRootPersistentClass( aqlBeanDao.getRootPersistentClass() );
		setRootTable( aqlBeanDao.getRootTable() );
//		setEntity( AqlBeanDao.getEntity() );
//		setBinding( AqlBeanDao.getBinding() );
		WhereConditionGroup whereConditionGroup = (WhereConditionGroup) aqlBeanDao.getWhereConditionGroup().copy();
		whereConditionGroup.setAqlBeanDao(this);
		setWhereConditionGroup( whereConditionGroup );
		setUnprocessedSearchList( aqlBeanDao.getUnprocessedSearchList() );
		setUnprocessedAqlTables( new HashMap<String,UnprocessedAqlTable>( getUnprocessedAqlTables() ) );
		setCountFields( aqlBeanDao.getCountFields() );
		setGroupByCriteria( aqlBeanDao.getGroupByCriteria() );
		setOrderByCriteria( aqlBeanDao.getOrderByCriteria() );
		setMaxResults( aqlBeanDao.getMaxResults() );
		setUnprocessedFilterList( aqlBeanDao.getUnprocessedFilterList() );
//		setSessionRetriever( AqlBeanDao.getSessionRetriever() );
//		setAliasesForOptimisation( AqlBeanDao.getAliasesForOptimisation() );
		setIsReturningActiveBeans( aqlBeanDao.getIsReturningActiveBeans() );

		return this;
	}
	
	public void processWhereCriteria( String whereCriteria, boolean distributeChildDaos ) {
		processWhereCriteria( whereCriteria, distributeChildDaos, getWhereConditionGroup() );
	}
	
	public void processWhereCriteria( String whereCriteria, boolean distributeChildDaos, WhereConditionGroup outerWhereConditionGroup ) {
		getAqlParser().updateString( whereCriteria );
		try {
			WhereConditionGroup innerWhereConditionGroup = getAqlParser().parseWhereCriteria( this );
			outerWhereConditionGroup.addWhereCondition( "AND", innerWhereConditionGroup );
		} catch( TokenStreamException ex ) {
			ApplicationUtil.handleError( ex );
		} catch( RecognitionException ex ) {
			ApplicationUtil.handleError( ex );
		}
	}

	public AplosAbstractBean getNew() {
		try {
			AplosAbstractBean bean = getBeanClass().newInstance();
			return bean;
		} catch (Exception e) {
			// Oh dear
			System.err.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	public Class<? extends AplosAbstractBean> determineInstantingBeanClass() {
		if( getListBeanClass() == null ) {
			return getBeanClass();
		} else {
			return getListBeanClass();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setBeanClass(Class<? extends AplosAbstractBean> beanClass, Class<? extends BackingPage> backingPageClass ) {
		setBeanClass(beanClass, backingPageClass, "bean");
	}

	@SuppressWarnings("unchecked")
	public void setBeanClass(Class<? extends AplosAbstractBean> beanClass, Class<? extends BackingPage> backingPageClass, String beanAlias ) {
		this.beanClass = beanClass;
		this.entity = beanClass.getSimpleName();
		this.binding = CommonUtil.firstLetterToLowerCase( getEntity() );
		
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
		setRootPersistentClass(persistentClass);
		setRootAlias( beanAlias );

		if( !Modifier.isAbstract(beanClass.getModifiers()) && backingPageClass == null ) {
//			setEditPageClass( ApplicationUtil.getAplosContextListener().getEditPageClasses().get( binding ) );
//			setListPageClass( ApplicationUtil.getAplosContextListener().getListPageClasses().get( binding ) );
//			AplosAbstractBean aplosAbstractBean = (AplosAbstractBean) CommonUtil.getNewInstance(beanClass, Logger.instance);
//			if ( aplosAbstractBean instanceof AplosBean ) {
				setEditPageClass( JSFUtil.getEditPageClass( getBeanClass() ) );
				setListPageClass( ApplicationUtil.getAplosContextListener().getListPageClasses().get( getBeanClass().getSimpleName() ) );
//			}
		} else if( backingPageClass != null ) {
			try {
				if( backingPageClass.getSimpleName().contains( "EditPage" ) ) {
					setEditPageClass( backingPageClass );
					try {
						setListPageClass( (Class<? extends BackingPage>) Class.forName(backingPageClass.getName().replace("EditPage", "ListPage")) );
					} catch( ClassNotFoundException cnfex ) {
						// do nothing this is probably expected.
					}

				} else if( backingPageClass.getSimpleName().contains( "ListPage" ) ){

					setListPageClass( backingPageClass );
					setEditPageClass( (Class<? extends BackingPage>) Class.forName(backingPageClass.getName().replace("EditPage", "ListPage")) );

				}
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends AplosAbstractBean> T getSaveable(long id) {
		T aplosAbstractBean = get( id, null, true );
		aplosAbstractBean = aplosAbstractBean.getSaveableBean();
		return aplosAbstractBean;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends AplosAbstractBean> T get(long id) {
		return get( id, null, true );
	}
	
	public <T extends AplosAbstractBean> T get(long id, AplosAbstractBean aplosAbstractBean, boolean isCheckingCache ) {
		return get(id, aplosAbstractBean, isCheckingCache, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends AplosAbstractBean> T get(long id, AplosAbstractBean aplosAbstractBean, boolean isCheckingCache, boolean canRecur ) {
		PersistenceContext persistenceContext = ApplicationUtil.getPersistenceContext( true );
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( getBeanClass() );
		AplosAbstractBean cachedBean = null;
		if( isCheckingCache ) {
			cachedBean = persistenceContext.findBean( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), id );
		}
		if( cachedBean == null || ((PersistenceBean) cachedBean).isLazilyLoaded() ) {
			if( cachedBean != null ) {
				aplosAbstractBean = cachedBean;
			}
			WhereConditionGroup oldWhereConditionGroup = (WhereConditionGroup) getWhereConditionGroup().copy();
			addWhereCriteria( "bean.id = " + id );
			Boolean oldIsReturningActiveBeans = getIsReturningActiveBeans();
			setIsReturningActiveBeans( null );
			boolean isStrictPolymorphismSet = getIsStrictPolymorphism() != null;
			if( !isStrictPolymorphismSet ) {
				setIsStrictPolymorphism(false);
			}
			
			if( aplosAbstractBean == null ) {
				aplosAbstractBean = (AplosAbstractBean) getFirstBeanResult();
	
				if( aplosAbstractBean != null && ((PersistenceBean) aplosAbstractBean).isLazilyLoaded() 
						&& persistentClass.getInheritanceType().equals(InheritanceType.JOINED_TABLE) 
						&& persistentClass.getSubPersistentClasses().size() > 0 ) {
					if( canRecur ) {
						BeanDao fullBeanDao = new BeanDao( aplosAbstractBean.getClass() );
						aplosAbstractBean = fullBeanDao.get( id, null, true, false );
					} else {
						ApplicationUtil.handleError( new Exception( "Get recur attempted when it was prohibited" ) );
					}
				}
			} else {
				int oldMaxResults = getMaxResults();
				setMaxResults(1);

				Connection conn = null;
				List resultList = null;
				try {
					conn = ApplicationUtil.getConnection();
					ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
					resultList = ApplicationUtil.getResults( conn, processedBeanDao.getAllPreparedStatement(conn) );
					PersistentClassSelectCriteria persistentClassSelectCriteria = (PersistentClassSelectCriteria) processedBeanDao.getProcessedSelectCriteriaList().get( 0 );
					persistentClassSelectCriteria.updateBean( processedBeanDao, aplosAbstractBean, new ArrayRowDataReceiver((Object[]) resultList.get( 0 )));
//					if( resultList != null ) {
////						List objList = new ArrayList();
//						List<Object> beanInstances = new ArrayList<Object>();
//						beanInstances.add( aplosAbstractBean );
//						processedBeanDao.find( resultList, beanInstances );
//					} else {
//						aplosAbstractBean = null;
//					}
				} catch( Exception ex ) {
					String sql = null;
					try {
						ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
						processedBeanDao.getAllPreparedStatement(conn);
						sql = processedBeanDao.getPrintableSqlString( processedBeanDao.generateSqlString(true) );
					} catch( Exception ex1 ) {
						sql = "Error generating sql occurred";
					}
					throw new RuntimeException( "GeneratedMessage " + sql, ex );
				} finally {
					ApplicationUtil.closeConnection(conn);
				}

				setMaxResults(oldMaxResults);
			}
			if( !isStrictPolymorphismSet ) {
				setIsStrictPolymorphism(null);
			}
			if( aplosAbstractBean != null ) {
				if( isCheckingCache ) {
					persistenceContext.registerBean( aplosAbstractBean );
				}
			} 
			
			/*
			 * This can happen for example the systemUserId is set to -1 in the aplosAbstractBean 
			 * when no user is set.  Also a user could make an error with the id for the bean.
			 */
//			else {
//				ApplicationUtil.handleError( new Exception( "Cannot find " + getBeanClass().getSimpleName() + " with id " + id ) );
//			}
			setIsReturningActiveBeans( oldIsReturningActiveBeans );
			setWhereConditionGroup(oldWhereConditionGroup);
		} else {
			aplosAbstractBean = cachedBean;
		}
		
		if( aplosAbstractBean instanceof AplosBean 
				&& ((AplosBean) aplosAbstractBean).isArchived() ) {
			((AplosBean) aplosAbstractBean).unarchive();
		}
		return (T) aplosAbstractBean;
	}
	
	public void addAqlSubQueryTable( String alias, SubBeanDao subBeanDao ) {
		UnprocessedAqlSubTable aqlSubTable = new UnprocessedAqlSubTable(subBeanDao, this, alias); 
		getSubQueryTables().add( aqlSubTable );
//		if( activeBeanHandler ) {
//			setIsReturningActiveBeans( null );
//			setOrderBy( null );
//			setActiveBeanHandler( aqlSubTable );
//		}
	}
	
	public JoinedAqlTable addQueryTable( String alias, String idJoinPath ) {
		JoinedAqlTable joinedAqlTable = new JoinedAqlTable( alias, idJoinPath );
		if( alias != null ) {
			getUnprocessedAqlTables().put( alias, joinedAqlTable );
		} else {
			getUnprocessedAqlTables().put( idJoinPath, joinedAqlTable );
		}
		return joinedAqlTable;	
	}
	
	public JoinedAqlTable addQueryTable( String alias, UnevaluatedTableVariable variableSelectCriteria ) {
		JoinedAqlTable joinedAqlTable = new JoinedAqlTable( alias, variableSelectCriteria );
		if( alias != null ) {
			getUnprocessedAqlTables().put( alias, joinedAqlTable );
		} else {
			getUnprocessedAqlTables().put( variableSelectCriteria.getOriginalPath(), joinedAqlTable );
		}
		return joinedAqlTable;	
	}
	
	public ConditionalAqlTable addQueryTable( String alias, PersistableTable persistableTable, String leftCondition, String rightCondition ) {
		ConditionalAqlTable conditionalAqlTable = new ConditionalAqlTable( alias, persistableTable, leftCondition, rightCondition );
		getUnprocessedAqlTables().put( alias, conditionalAqlTable );
		return conditionalAqlTable;	
	}
	
//	public CommaAqlTable addQueryTable( String alias, Class<? extends AplosAbstractBean> beanClass, String parentAlias ) {
//		CommaAqlTable commaAqlTable = new CommaAqlTable( alias, beanClass, parentAlias );
//		getUnprocessedAqlTables().put( alias, commaAqlTable );
//		return commaAqlTable;	
//	}
	
	public ReverseJoinedAqlTable addReverseJoinTable( Class<? extends AplosAbstractBean> beanClass, String joinPath, String parentAlias ) {
		ReverseJoinedAqlTable reverseJoinedAqlTable = new ReverseJoinedAqlTable( beanClass, joinPath, parentAlias );
		getUnprocessedAqlTables().put( reverseJoinedAqlTable.getAlias(), reverseJoinedAqlTable );
		return reverseJoinedAqlTable;	
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosAbstractBean> T getFirstBeanResult() {
		List<Object> beanList;

		int oldMaxResults = getMaxResults();
		setMaxResults(1);
		beanList = getAll();
		setMaxResults(oldMaxResults);

		if( beanList.size() > 0 ) {
			return (T) beanList.get( 0 );
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public Object getFirstResult() {
		int oldMaxResults = getMaxResults();
		setMaxResults(1);

		Connection conn = null;
		List resultList = null;
		try {
			conn = ApplicationUtil.getConnection();
			ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
			processedBeanDao.setGeneratingBeans(false);
			resultList = ApplicationUtil.getResults( conn, processedBeanDao.getAllPreparedStatement(conn) );
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		} catch( Exception ex ) {
			ApplicationUtil.handleError(ex);
		} finally {
			ApplicationUtil.closeConnection(conn);
		}

		setMaxResults(oldMaxResults);

		if( resultList.size() > 0 ) {
			if( resultList.get( 0 ).getClass().isArray() 
					&& ((Object[]) resultList.get( 0 )).length == 1) {
				return ((Object[]) resultList.get( 0 ))[ 0 ];
			} else {
				return resultList.get( 0 );
			}
		} else {
			return null;
		}
	}
	
	public ProcessedBeanDao createProcessedBeanDao() {
		return new ProcessedBeanDao(this);
	}
	
	public void removeQueryTable( String alias ) {
		getUnprocessedAqlTables().remove( alias );
	}
	
	public int getAndIncrementColumnCount() {
		if( getParentTable() != null ) {
			return getParentTable().getProcessedBeanDao().getBeanDao().getAndIncrementColumnCount();
		} else {
			return ++columnCount;
		}
	}
	
	public BeanDao setIsReturningActiveBeans(Boolean isReturningActiveBeans) {
		if( getActiveBeanHandler() == null ) {
			this.isReturningActiveBeans = isReturningActiveBeans;
		} else {
			getActiveBeanHandler().getProcessedBeanDao().getBeanDao().setIsReturningActiveBeans(isReturningActiveBeans);
		}
		return this;
	}
	
	public BeanDao setOrderBy(String orderBy) {
		this.setOrderByCriteria(orderBy);
		return this;
	}
	
	public void setNamedParameter( String name, String value ) {
		getNamedParameters().put(name, value);
	}

	public BeanDao setSelectCriteria( String newSelectCriteria ) {
		getUnprocessedSelectCriteriaList().clear();
		getAqlParser().updateString( newSelectCriteria );
		getAqlParser().parseAllSelectCriteria(this);
		return this;
	}

	public void addSelectCriteria( String[] newSelectCriteria ) {
		for( int i = 0, n = newSelectCriteria.length; i < n; i++ ) {
			addSelectCriteria( newSelectCriteria[ i ] );
		}
	}

	public BeanDao addSelectCriteria( String newSelectCriteria ) {
		getAqlParser().updateString( newSelectCriteria );
		getAqlParser().parseAllSelectCriteria( this );
		return this;
	}

	public BeanDao addSelectCriteria( SelectCriteria selectCriteria ) {
		getUnprocessedSelectCriteriaList().add( selectCriteria );
		if( getParentTable() != null ) {
			selectCriteria.setFieldInformationRequired(true);
		}
		return this;
	}

	public BeanDao addSelectCriteria( SubBeanDao subBeanDao, String alias ) {
		UnprocessedAqlSubTable aqlSubTable = new UnprocessedAqlSubTable( subBeanDao, this, alias );
		SubQueryAqlVariable subQuerySelectCriteria = new SubQueryAqlVariable( aqlSubTable );
		subBeanDao.setFieldInformationRequired( false );
		SelectCriteria selectCriteria = new SelectCriteria(subQuerySelectCriteria);
		selectCriteria.setAlias( alias );
		getUnprocessedSelectCriteriaList().add( selectCriteria );
		return this;
	}

	@SuppressWarnings("unchecked")
	public int getCountAll() {
		Connection conn = null;
		List<Object[]> countList = null;
		try {
			conn = ApplicationUtil.getConnection();
			ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
			countList = ApplicationUtil.getResults( conn, processedBeanDao.getCountAllPreparedStatement(conn) );
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		} finally {
			ApplicationUtil.closeConnection(conn);
		}
		
		if( CommonUtil.isNullOrEmpty(getGroupByCriteria()) ) {
			if( isSqlQuery() ) {
				return ((BigInteger) countList.get( 0 )[0]).intValue();
			} else {
				return ((Long) countList.get( 0 )[0]).intValue();
			}
		} else {
			return countList.size();
		}
	}
	
	public BeanDao setSearchCriteria(String expression) {
		if( expression.equals( "" ) ) {
			getUnprocessedSearchList().clear();
		}
		addSearchFilters(expression);
		return this;
	}
	
	
	public void clearWhereCriteria() {
		setWhereConditionGroup(new WhereConditionGroup());
	}
	
	public List getAll() {
    	ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
    	processedBeanDao.setGeneratingBeans(isGeneratingBeans());
    	List myList = processedBeanDao.getAll();
    	return myList;
	}
	
	public List getResultFields() {
		Connection conn = null;
		List resultList = null;
		try {
			conn = ApplicationUtil.getConnection();
	    	ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
	    	processedBeanDao.setGeneratingBeans(false);
			resultList = ApplicationUtil.getResults( conn, processedBeanDao.getAllPreparedStatement(conn) );
			removeArrayFromSingleResultList(resultList);
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		} catch( Exception ex ) {
			ApplicationUtil.handleError(ex);
		} finally {
			ApplicationUtil.closeConnection(conn);
		}
			
		return resultList;
	}
	
	public List getBeanResults() {
    	ProcessedBeanDao processedBeanDao = createProcessedBeanDao();
    	processedBeanDao.setGeneratingBeans(false);
    	List resultList = processedBeanDao.getAll();
		return resultList;
	}
	
	public List removeArrayFromSingleResultList(List resultList) {
		if( resultList.size() > 0 && ((Object[]) resultList.get( 0 )).length == 1 ) {
			List oldResultList = new ArrayList(resultList);
			resultList.clear();
			for( int i = 0, n = oldResultList.size(); i < n; i++ ) {
				resultList.add( ((Object[]) oldResultList.get( i ))[ 0 ] );
			}
		}
		return resultList;
	}
	
	public static <T extends AplosAbstractBean> T loadLazyValues( AplosAbstractBean aplosAbstractBean, boolean isCheckingCache ) {
		/*
		 * This is a check for when a ListBean has been sent in, it will find the closet persisted class to 
		 * fill the fields with.
		 */
		PersistentClass persistentClass = null;
		Class<?> beanClass = aplosAbstractBean.getClass();
		while( persistentClass == null && !beanClass.isAssignableFrom(Object.class ) ) {
			persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
			beanClass = beanClass.getSuperclass();
		}	
		
		AplosAbstractBean loadedBean;
		
		if( InheritanceType.SINGLE_TABLE.equals( persistentClass.getInheritanceType() )
				|| InheritanceType.JOINED_TABLE.equals( persistentClass.getInheritanceType() ) ) {
			loadedBean = loadLazyValues( aplosAbstractBean, false, isCheckingCache );
		} else {
			loadedBean = loadLazyValues( aplosAbstractBean, true, isCheckingCache );
		}
		return (T) loadedBean;
	}
	
	public static <T extends AplosAbstractBean> T loadLazyValues( AplosAbstractBean aplosAbstractBean, boolean fillProvidedBean, boolean isCheckingCache ) {
		/*
		 * This is a check for when a ListBean has been sent in, it will find the closet persisted class to 
		 * fill the fields with.
		 */
		Class<?> beanClass = aplosAbstractBean.getClass();
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( beanClass );
		while( persistentClass == null && !beanClass.isAssignableFrom(Object.class ) ) {
			beanClass = beanClass.getSuperclass();
			persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
			fillProvidedBean = true;
		}

		if( isCheckingCache ) {
			AplosAbstractBean cachedBean = null;
			if( isCheckingCache ) {
				cachedBean = ApplicationUtil.getPersistenceContext().findBean( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), aplosAbstractBean.getId() );
			}
			if( cachedBean != null && !((PersistenceBean) cachedBean).isLazilyLoaded() ) {
				return (T) cachedBean;
			}
		}

		BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) persistentClass.getTableClass() );
		AplosAbstractBean loadedBean;
		if( fillProvidedBean ) {
			loadedBean = aqlBeanDao.get(aplosAbstractBean.getId(), aplosAbstractBean, isCheckingCache);
		} else {
			loadedBean = aqlBeanDao.get(aplosAbstractBean.getId(), null, isCheckingCache);
		}
		return (T) loadedBean;
	}
	
	public void addSearchFilters(String searchCriteria) {
		getUnprocessedSearchList().clear();
    	getUnprocessedSearchList().add( searchCriteria );
	}
	
	public Map<String,String> addFilters(Map<String,String> filters) {
		Map<String,String> unmatchedFiltersMap = new HashMap<String,String>();
		getUnprocessedFilterList().clear();
		if ( filters.size() > 0 ) {
    		Iterator<String> filterKeys = filters.keySet().iterator();
    		String filterExpression;
    		String filterKey;
    		while( filterKeys.hasNext() ) {

    			filterKey = filterKeys.next().trim();
    			if( filterKey.startsWith( "tableBean." ) ) {
    				filterExpression = filterKey.replace( "tableBean", "bean" );
    				filterKey = filterExpression.replace( ".", "_" );
    			} else if( filterKey.indexOf( ":" ) > -1 ) {
    				filterExpression = filterKey.substring( filterKey.indexOf( ":" ) + 1, filterKey.length() );
    				filterKey = filterKey.substring( 0, filterKey.indexOf( ":" ) );
    			} else {
    				continue;
    			}
        		getUnprocessedFilterList().add( filterExpression + " LIKE :" + filterKey );
    		}
    	}
		return unmatchedFiltersMap;
	}

	public BeanDao setWhereCriteria(String newWhereCriteria) {
		setWhereConditionGroup( new WhereConditionGroup() );
		addWhereCriteria(newWhereCriteria);
		return this;
	}

	public BeanDao addWhereCriteria(String newWhereCriteria) {
		getAqlParser().updateString( newWhereCriteria );
		try {
			getWhereConditionGroup().addWhereCondition( "AND", getAqlParser().parseWhereCriteria( this ) );
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError( tsEx );
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError( rEx );
		}
		return this;
	}
	
	public void clearSearchCriteria() {
		getUnprocessedSearchList().clear();
		for( UnprocessedAqlSubTable aqlSubTable : getSubQueryTables() ) {
			aqlSubTable.getSubBeanDao().clearSearchCriteria();
		}
	}
    
    public void preCriteriaEvaluation(ProcessedBeanDao processedBeanDao) {}
    
    public void clearNamedParameters() {
    	getNamedParameters().clear();

		for( UnprocessedAqlSubTable aqlSubTable : getSubQueryTables() ) {
			aqlSubTable.getSubBeanDao().clearNamedParameters();
		}
    }

	public int getLatestTableIdx() {
		return latestTableIdx;
	}

	public void setLatestTableIdx(int latestTableIdx) {
		this.latestTableIdx = latestTableIdx;
	}

	public AqlSubTable getActiveBeanHandler() {
		return activeBeanHandler;
	}

	public void setActiveBeanHandler(AqlSubTable activeBeanHandler) {
		this.activeBeanHandler = activeBeanHandler;
	}
	
	public List<SelectCriteria> getUnprocessedSelectCriteriaList() {
		return unprocessedSelectCriteriaList;
	}

	public void setUnprocessedSelectCriteriaList(
			List<SelectCriteria> unprocessedSelectCriteriaList) {
		this.unprocessedSelectCriteriaList = unprocessedSelectCriteriaList;
	}

	public List<String> getUnprocessedFilterList() {
		return unprocessedFilterList;
	}

	public void setUnprocessedFilterList(List<String> unprocessedFilterList) {
		this.unprocessedFilterList = unprocessedFilterList;
	}

	public AqlSubTable getParentTable() {
		return parentTable;
	}

	public void setParentTable(AqlSubTable parentTable) {
		this.parentTable = parentTable;
	}

	public List<String> getUnprocessedSearchList() {
		return unprocessedSearchList;
	}

	public void setUnprocessedSearchList(List<String> unprocessedSearchList) {
		this.unprocessedSearchList = unprocessedSearchList;
	}

	public Map<String, String> getNamedParameters() {
		return namedParameters;
	}

	public int getFirstRowIdx() {
		return firstRowIdx;
	}

	public void setFirstRowIdx(int firstRowIdx) {
		this.firstRowIdx = firstRowIdx;
	}
	
	/*
	 * Imported from AqlBeanDao
	 */

	public Class<? extends AplosAbstractBean> getBeanClass() {
		return beanClass;
	}

	public String getEntityName() {
		if( beanClass != null ) {
			return AplosBean.getEntityName( beanClass );
		} else {
			return "";
		}
	}

	public BeanDao setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setListBeanClass(Class<? extends AplosAbstractBean> listBeanClass) {
		this.listBeanClass = listBeanClass;
	}

	public Class<? extends AplosAbstractBean> getListBeanClass() {
		return listBeanClass;
	}

	public Boolean getIsReturningActiveBeans() {
		return isReturningActiveBeans;
	}

	public String getCountFields() {
		return countFields;
	}
	
	public void setCountFields( String countFields ) {
		this.countFields = countFields;
	}

	public boolean isSqlQuery() {
		return isSqlQuery;
	}

	public Class<? extends BackingPage> getListPageClass() {
		return listPageClass;
	}

	public void setListPageClass(Class<? extends BackingPage> listPageClass) {
		this.listPageClass = listPageClass;
	}

	public Class<? extends BackingPage> getEditPageClass() {
		return editPageClass;
	}

	public void setEditPageClass(Class<? extends BackingPage> editPageClass) {
		this.editPageClass = editPageClass;
	}

	public String getEntity() {
		return entity;
	}

	public String getBinding() {
		return binding;
	}

	public BeanDao setGroupBy( String groupBy ) {
		this.setGroupByCriteria(groupBy);
		return this;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public Map<String,UnprocessedAqlTable> getUnprocessedAqlTables() {
		return unprocessedAqlTables;
	}

	public void setUnprocessedAqlTables(Map<String,UnprocessedAqlTable> unprocessedAqlTables) {
		this.unprocessedAqlTables = unprocessedAqlTables;
	}

	public Set<UnprocessedAqlSubTable> getSubQueryTables() {
		return subQueryTables;
	}

	public void setSubQueryTables(Set<UnprocessedAqlSubTable> subQueryTables) {
		this.subQueryTables = subQueryTables;
	}

	public WhereConditionGroup getWhereConditionGroup() {
		return whereConditionGroup;
	}

	public void setWhereConditionGroup(WhereConditionGroup whereConditionGroup) {
		this.whereConditionGroup = whereConditionGroup;
	}

	public Boolean getIsStrictPolymorphism() {
		return isStrictPolymorphism;
	}

	public void setIsStrictPolymorphism(Boolean isStrictPolymorphism) {
		this.isStrictPolymorphism = isStrictPolymorphism;
	}

	public boolean isInitialisingPaths() {
		return isInitialisingPaths;
	}

	public void setInitialisingPaths(boolean isInitialisingPaths) {
		this.isInitialisingPaths = isInitialisingPaths;
	}

	public PersistentClass getRootPersistentClass() {
		return rootPersistentClass;
	}

	public void setRootPersistentClass(PersistentClass rootPersistentClass) {
		this.rootPersistentClass = rootPersistentClass;
	}
	
	public AqlTableAbstract createRootTable( ProcessedBeanDao processedBeanDao ) {
		if( getRootTable() == null ) {
			return new AqlTable( processedBeanDao, getRootPersistentClass(), null, getRootAlias() );
		} else {
			return getRootTable().createAqlTable(processedBeanDao, false);
		}
	}

	public String getRootAlias() {
		return rootAlias;
	}

	public void setRootAlias(String rootAlias) {
		this.rootAlias = rootAlias;
	}

	public boolean isCreatingPersistentSelectCriteria() {
		return isCreatingPersistentSelectCriteria;
	}

	public void setCreatingPersistentSelectCriteria(
			boolean isCreatingPersistentSelectCriteria) {
		this.isCreatingPersistentSelectCriteria = isCreatingPersistentSelectCriteria;
	}

	public String getOrderByCriteria() {
		return orderByCriteria;
	}

	public void setOrderByCriteria(String orderByCriteria) {
		this.orderByCriteria = orderByCriteria;
	}

	public boolean isAddingDefaultOrderByToLists() {
		return isAddingDefaultOrderByToLists;
	}

	public void setAddingDefaultOrderByToLists(boolean isAddingDefaultOrderByToLists) {
		this.isAddingDefaultOrderByToLists = isAddingDefaultOrderByToLists;
	}

	public boolean isGeneratingBeans() {
		return isGeneratingBeans;
	}

	public void setGeneratingBeans(boolean isGeneratingBeans) {
		this.isGeneratingBeans = isGeneratingBeans;
	}

	public UnprocessedAqlTable getRootTable() {
		return rootTable;
	}

	public void setRootTable(UnprocessedAqlTable rootTable) {
		this.rootTable = rootTable;
	}

	public Boolean getIsReturningArchivedBeans() {
		return isReturningArchivedBeans;
	}

	public void setIsReturningArchivedBeans(Boolean isReturningArchivedBeans) {
		this.isReturningArchivedBeans = isReturningArchivedBeans;
	}

	public String getGroupByCriteria() {
		return groupByCriteria;
	}

	public void setGroupByCriteria(String groupByCriteria) {
		this.groupByCriteria = groupByCriteria;
	}

	public AqlParser getAqlParser() {
		return aqlParser;
	}

	public void setAqlParser(AqlParser aqlParser) {
		this.aqlParser = aqlParser;
	}
}
