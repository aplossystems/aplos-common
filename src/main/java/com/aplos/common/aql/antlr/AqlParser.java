package com.aplos.common.aql.antlr;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import antlr.ASTFactory;
import antlr.MismatchedTokenException;
import antlr.RecognitionException;
import antlr.TokenStream;
import antlr.TokenStreamException;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.IndividualWhereCondition;
import com.aplos.common.aql.OrderByCriteria;
import com.aplos.common.aql.SubBeanDao;
import com.aplos.common.aql.WhereConditionGroup;
import com.aplos.common.aql.aqltables.AqlTable.JoinType;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.aql.aqlvariables.BasicAqlVariable;
import com.aplos.common.aql.aqlvariables.BetweenWhereCondition;
import com.aplos.common.aql.aqlvariables.ConcatenatedAqlVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.aqlvariables.FunctionAqlVariable;
import com.aplos.common.aql.aqlvariables.IntervalAqlVariable;
import com.aplos.common.aql.aqlvariables.QueryAqlVariable;
import com.aplos.common.aql.selectcriteria.NewClassSelectCriteria;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.WhereCondition;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

public class AqlParser extends antlr.LLkParser implements AqlTokenTypes {
	public static Set<Integer> functionsSet = new HashSet<Integer>();
	public static Set<Integer> logicalOperatorSet = new HashSet<Integer>();
	public static Set<Integer> variableSet = new HashSet<Integer>();
	public static Set<Integer> standAloneConstantSet = new HashSet<Integer>();
	public static Set<Integer> numberConstantSet = new HashSet<Integer>();
	public static Set<Integer> joinsSet = new HashSet<Integer>();
	public static Set<Integer> concatenationSet = new HashSet<Integer>();
	
	private Stack<OpenedGroup> openedGroups = new Stack<OpenedGroup>();
	private Integer uniqueFunctionId = 0;
	
	static {
		functionsSet.add(COUNT);
		functionsSet.add(SUM);
		functionsSet.add(NOW);
		functionsSet.add(MAX);
		functionsSet.add(MIN);
		functionsSet.add(UPPER);
		functionsSet.add(LOWER);
		functionsSet.add(DATE_FORMAT);
		functionsSet.add(ACOS);
		functionsSet.add(SIN);
		functionsSet.add(RADIANS);
		functionsSet.add(COS);
		functionsSet.add(CONCAT);
		functionsSet.add(COALESCE);
		functionsSet.add(DATE_ADD);
		functionsSet.add(AVG);
		functionsSet.add(DATEDIFF);
		functionsSet.add(SUBSTRING);
		functionsSet.add(YEAR);
		functionsSet.add(MONTH);
		functionsSet.add(DAY);
	}
	
	static {
		concatenationSet.add(PLUS);
		concatenationSet.add(MINUS);
		concatenationSet.add(DIV);
		concatenationSet.add(STAR);
	}
	
	static {
		joinsSet.add(LEFT);
		joinsSet.add(RIGHT);
		joinsSet.add(OUTER);
		joinsSet.add(INNER);
		joinsSet.add(JOIN);
		joinsSet.add(COMMA);
	}
	
	static {
		logicalOperatorSet.add(EQ);
		logicalOperatorSet.add(NE);
		logicalOperatorSet.add(LE);
		logicalOperatorSet.add(LT);
		logicalOperatorSet.add(GE);
		logicalOperatorSet.add(GT);
		logicalOperatorSet.add(IN);
		logicalOperatorSet.add(NOT_IN);
		logicalOperatorSet.add(NOT);
		logicalOperatorSet.add(IS);
		logicalOperatorSet.add(LIKE);
	}
	
	static {
		variableSet.add(IDENT);
		variableSet.add(STAR);
		variableSet.add(DOT);
		variableSet.add(CLASS);
	}
	
	static {
		numberConstantSet.add(NUM_DOUBLE);
		numberConstantSet.add(NUM_FLOAT);
		numberConstantSet.add(NUM_INT);
		numberConstantSet.add(NUM_LONG);
	}
	
	static {
		standAloneConstantSet.add(NULL);
		standAloneConstantSet.add(QUOTED_STRING);
		standAloneConstantSet.add(TRUE);
		standAloneConstantSet.add(FALSE);
	}
	
	public static AqlParser getInstance(String hql) {
        // [jsd] The fix for HHH-558...
        AqlBaseLexer lexer = new AqlBaseLexer( new StringReader( hql ) );
		return new AqlParser( lexer );
	}

	private AqlParser(TokenStream lexer) {
		this(lexer,3);
		initialize();
	}

	protected AqlParser(TokenStream lexer, int k) {
	  super(lexer,k);
//	  tokenNames = _tokenNames;
//	  buildTokenTypeASTClassMap();
	  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
	}

	private void initialize() {
		// Initialize the error handling delegate.
//		parseErrorHandler = new ErrorCounter();
		setASTFactory(new ASTFactory());	// Create nodes that track line and column number.
	}
	
	public WhereConditionGroup parseWhereCriteria( BeanDao beanDao ) throws RecognitionException, TokenStreamException {
		WhereConditionGroup whereConditionGroup = new WhereConditionGroup();
		Integer localFunctionId = uniqueFunctionId++;
		String logicalOperator = null;
		
		do {
			if( whereConditionGroup.getWhereConditions().size() > 0 ) {
				logicalOperator = LT(1).getText();
				match(LA(1));
			}
			if( LA(1) == OPEN ) {
				match(OPEN);
				getOpenedGroups().add( new OpenedGroup( localFunctionId ) );
				whereConditionGroup.addWhereCondition( logicalOperator, parseWhereCriteria( beanDao ) );
			} else {
				WhereCondition whereCondition;
				AqlVariable leftHandVariable = parseAqlVariable( beanDao, null );
				if( LA(1) == BETWEEN ) {
					whereCondition = parseBetweenWhereCondition(leftHandVariable, beanDao);
				} else {
	
					IndividualWhereCondition individualWhereCondition = new IndividualWhereCondition( beanDao );
					individualWhereCondition.setLeftHandVariable( leftHandVariable );
					String conditionalOperator = parseConditionOperator();
					individualWhereCondition.setConditionalOperator(conditionalOperator);
					OpenedGroup disabledGroup = disableLastOpenGroup();
					individualWhereCondition.setRightHandVariable( parseAqlVariable( beanDao, null ) );
					if( disabledGroup != null ) {
						disabledGroup.setAvailable(true);
					}
					
					if( individualWhereCondition.getRightHandVariable().getName() != null
							&& individualWhereCondition.getRightHandVariable().getName().equalsIgnoreCase( "null" ) ) {
						if( individualWhereCondition.getConditionalOperator().equals( "!=" ) ) {
							individualWhereCondition.setConditionalOperator( "IS NOT" );
						} else if( individualWhereCondition.getConditionalOperator().equals( "==" ) 
								|| individualWhereCondition.getConditionalOperator().equals( "=" ) ) {
							individualWhereCondition.setConditionalOperator( "IS" );
						}
					}
					
					whereCondition = individualWhereCondition;
				}
				
				whereConditionGroup.addWhereCondition( logicalOperator, whereCondition );
			}
			
			while( LA(1) == CLOSE ) {
				if( getOpenedGroups().size() > 0 ) {
					if( getOpenedGroups().peek().isAvailable() ) {
						getOpenedGroups().pop();
						match( CLOSE );
						return whereConditionGroup;
					} else {
						break;
					}
				} else {
					throw new RecognitionException( "Too many close brackets added to where clause" );
				}
			}
		} while( LA(1) == AND || LA(1) == OR );
		
		checkForUnclosedGroups( localFunctionId );
		return whereConditionGroup;
	}
	
	public void checkForUnclosedGroups( int localFunctionId ) throws RecognitionException {
		if( getOpenedGroups().size() > 0 && getOpenedGroups().peek().getFunctionId() >= localFunctionId ) {
			throw new RecognitionException( "Not all groups have been closed in where clause" );
		}
	}
	
	public OpenedGroup disableLastOpenGroup() {
		if( isClosetGroupAvailable() ) {
			getOpenedGroups().peek().setAvailable( false );
			return getOpenedGroups().peek();
		}
		return null;
	}
	
	public boolean isClosetGroupAvailable() {
		if( getOpenedGroups().size() > 0 ) {
			return getOpenedGroups().peek().isAvailable();
		}
		return false;
	}
	
//	public void parseWhereCondition( WhereConditionGroup whereConditionGroup ) {
//		parseLeftCriteria( whereConditionGroup );
//	}
	
	public String parseConditionOperator() throws RecognitionException, TokenStreamException {
		String conditionalOperator = null;
		if( logicalOperatorSet.contains( LA(1) ) ) {
			if( LA(1) == NOT && LA(2) == IN ) {
				conditionalOperator = LT(1).getText() + " " + LT(2).getText();
				match(NOT);
				match(IN);
			} else if( LA(1) == IS && LA(2) == NOT ) {
				conditionalOperator = LT(1).getText() + " " + LT(2).getText();
				match(IS);
				match(NOT);
			} else {
				conditionalOperator = LT(1).getText();
				match(LA(1));
			}
			return conditionalOperator;
		} else {
			throw new RecognitionException( "No logical operator found for where condition: " + LT(1) );
		}
	}
	
	public void parseAllSelectCriteria( BeanDao aqlBeanDao ) {
		try {
			Integer localFunctionId = uniqueFunctionId++;
			OpenedGroup disabledGroup = null;
			NewClassSelectCriteria newClassSelectCriteria = null;
			if( LA(1) == NEW ) {
				match( NEW );
				StringBuffer variableStrBuf = new StringBuffer();
				
				while( variableSet.contains( LA(1) ) ) {
					variableStrBuf.append( LT(1).getText() );
					match( LA(1) );
				}
				
				try {
					aqlBeanDao.setGeneratingBeans( false );
					newClassSelectCriteria = new NewClassSelectCriteria( (Class<?>) Class.forName( variableStrBuf.toString() ) );
				} catch( ClassNotFoundException cnfEx ) {
					ApplicationUtil.handleError( cnfEx );
					return;
				}
				match( OPEN );
				disabledGroup = new OpenedGroup( localFunctionId );
				disabledGroup.setAvailable(false);
				getOpenedGroups().add( disabledGroup );
			}
			List<SelectCriteria> selectCriterias = new ArrayList<SelectCriteria>();
			while( LA(1) != EOF ) {
				if( LA(1) == COMMA ) {
					match( COMMA );
				} else {
					if( LA(1) == CLOSE ) {
						if( getOpenedGroups().peek().equals( disabledGroup ) ) {
							getOpenedGroups().pop();
							match( CLOSE );
						}
					} else {
						selectCriterias.add( parseIndividualSelectCriteria( aqlBeanDao ) );
					}
				}
			}
	
			if( newClassSelectCriteria == null ) {
				for( int i = 0, n = selectCriterias.size(); i < n; i++ ) {
					aqlBeanDao.addSelectCriteria( selectCriterias.get( i ) );
				}
			} else {
				for( int i = 0, n = selectCriterias.size(); i < n; i++ ) {
					newClassSelectCriteria.getUnprocessedSelectCriteria().add(selectCriterias.get( i ));
				}
				aqlBeanDao.addSelectCriteria(newClassSelectCriteria);
			}

			closeOpenedGroups( null );
			checkForUnclosedGroups( localFunctionId );
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError(tsEx);
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError(rEx);
		}
	}
	
	public List<AqlVariable> parseGroupBy( BeanDao aqlBeanDao ) {
		try {
			List<AqlVariable> aqlVariables = new ArrayList<AqlVariable>();
			while( LA(1) != EOF ) {
				if( LA(1) == COMMA ) {
					match( COMMA );
				} else {
					aqlVariables.add( parseAqlVariable( aqlBeanDao, null ) );
				}
			}
			return aqlVariables;
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError(tsEx);
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError(rEx);
		}
		return null;
	}
	
	public List<OrderByCriteria> parseOrderBy( BeanDao aqlBeanDao ) {
		try {
			List<OrderByCriteria> orderByCriteria = new ArrayList<OrderByCriteria>();
			OrderByCriteria currentOrderByCriteria;
			while( LA(1) != EOF ) {
				if( LA(1) == COMMA ) {
					match( COMMA );
				} else {
					AqlVariable aqlVariable = (AqlVariable) parseAqlVariable( aqlBeanDao, null );
					currentOrderByCriteria = new OrderByCriteria(aqlBeanDao,aqlVariable);
					if( LA(1) == IDENT || LA(1) == DESCENDING || LA(1) == ASCENDING ) {
						currentOrderByCriteria.setOrderByDirection( OrderByCriteria.orderByDirectionMap.get( LT(1).getText().toLowerCase() ) );
						match( LA(1) );
					}
					orderByCriteria.add( currentOrderByCriteria );
				}
			}
			return orderByCriteria;
		} catch( TokenStreamException tsEx ) {
			ApplicationUtil.handleError(tsEx);
		} catch( RecognitionException rEx ) {
			ApplicationUtil.handleError(rEx);
		}
		return null;
	}
	
	public AqlVariable parseQuerySelectCriteria( BeanDao aqlBeanDao, SelectCriteria querySelectCriteria ) throws RecognitionException, TokenStreamException {
		SubBeanDao subBeanDao = null;
		match(SELECT);
		List<SelectCriteria> selectCriterias = new ArrayList<SelectCriteria>();
		while( LA(1) != FROM ) {
			if( LA(1) == COMMA ) {
				match( COMMA );
			} else {
				selectCriterias.add( parseIndividualSelectCriteria( aqlBeanDao ) );
			}
		}
		match(FROM);
		if( LA(1) == IDENT ) {
			subBeanDao = new SubBeanDao( parseIndividualSelectCriteria(aqlBeanDao) );
			for( int i = 0, n = selectCriterias.size(); i < n; i++ ) {
				subBeanDao.addSelectCriteria(selectCriterias.get( i ));
			}
		}
		while( joinsSet.contains( LA(1) ) ) {
			StringBuffer join = new StringBuffer();
			while( joinsSet.contains( LA(1) ) ) {
				if( join.length() > 0 ) {
					join.append( "_" );
				}
				join.append( LT(1).getText().toUpperCase() );
				match(LA(1));
			}
			JoinType joinType = JoinType.valueOf( join.toString() );

			String tableName = LT(1).getText();
			String alias = null;
			SelectCriteria selectCriteria = null;
			if( LA( 2 ) == DOT ) {
				selectCriteria = parseIndividualSelectCriteria(subBeanDao);
			} else {
				match( IDENT );
				
				if( LA(1) == IDENT ) {
					alias = LT(1).getText();
					match( IDENT );
				}
			}
			
			if( selectCriteria == null && LA(1) == ON ) {
			} else {
				if( selectCriteria == null ) {
					subBeanDao.addQueryTable( alias, tableName );
				} else {
					subBeanDao.addQueryTable( selectCriteria.getAlias(), (UnevaluatedTableVariable) selectCriteria.getAqlVariable() );
				}
			}
		}
		if( LA(1) == WHERE ) {
			match( WHERE );
			subBeanDao.setWhereConditionGroup( parseWhereCriteria( subBeanDao ) );
		}
		QueryAqlVariable queryAqlVariable = new QueryAqlVariable( subBeanDao );
		if( LA(1) == GROUP && LA(2) == LITERAL_by ) {
			match( LA(1) );
			match( LA(1) );

			List<AqlVariable> aqlVariables = new ArrayList<AqlVariable>();
			StringBuffer strBuf = new StringBuffer();
			do {
				if( LA(1) == COMMA ) {
					match( COMMA );
					strBuf.append(",");
				} else {
					AqlVariable aqlVariable = parseAqlVariable( aqlBeanDao, null );
					strBuf.append( aqlVariable.getOriginalText() );
					aqlVariables.add( aqlVariable );
				}
			} while( LA(1) == COMMA );
			
			subBeanDao.setGroupByCriteria( strBuf.toString() );
		}
		if( querySelectCriteria != null ) {
			querySelectCriteria.updateAqlVariable( queryAqlVariable );
			checkForAlias( querySelectCriteria );
			aqlBeanDao.addAqlSubQueryTable(querySelectCriteria.getAlias(), subBeanDao);
		}
		return queryAqlVariable;
	}
	
	public void parseSelectStatement( BeanDao beanDao ) throws TokenStreamException, RecognitionException {
		match(SELECT);
		List<SelectCriteria> selectCriterias = new ArrayList<SelectCriteria>();
		while( LA(1) != FROM ) {
			if( LA(1) == COMMA ) {
				match( COMMA );
			} else {
				selectCriterias.add( parseIndividualSelectCriteria( beanDao ) );
			}
		}
		match(FROM);
		if( LA(1) == IDENT ) {
			String tableName = LT(1).getText();
			String alias = null;
			match( IDENT );
			if( LA(1) == IDENT ) {
				alias = LT(1).getText();
				match( IDENT );
			}
			PersistentClass persistentClass = (PersistentClass) ApplicationUtil.getPersistentApplication().getPersistableTableBySqlNameMap().get( tableName.toLowerCase() );
			
			if( alias != null ) {
				beanDao.setBeanClass( (Class<? extends AplosAbstractBean>) persistentClass.getTableClass(), null, alias );
			} else {
				beanDao.setBeanClass( (Class<? extends AplosAbstractBean>) persistentClass.getTableClass(), null );
			}
			for( int i = 0, n = selectCriterias.size(); i < n; i++ ) {
				beanDao.addSelectCriteria( selectCriterias.get( i ));
			}
		}
		while( joinsSet.contains( LA(1) ) ) {
			StringBuffer join = new StringBuffer();
			while( joinsSet.contains( LA(1) ) ) {
				if( join.length() > 0 ) {
					join.append( "_" );
				}
				join.append( LT(1).getText().toUpperCase() );
				match(LA(1));
			}
			JoinType joinType = JoinType.valueOf( join.toString() );

			String tableName = LT(1).getText();
			String alias = null;
			SelectCriteria variableSelectCriteria = null;
			if( LA( 2 ) == DOT ) {
				variableSelectCriteria = parseIndividualSelectCriteria(beanDao);
			} else {
				match( IDENT );
				
				if( LA(1) == IDENT ) {
					alias = LT(1).getText();
					match( IDENT );
				}
			}
			
			if( variableSelectCriteria == null && LA(1) == ON ) {
			} else {
				if( variableSelectCriteria == null ) {
					beanDao.addQueryTable( alias, tableName );
				} else {
					beanDao.addQueryTable( variableSelectCriteria.getAlias(), (UnevaluatedTableVariable) variableSelectCriteria.getAqlVariable() );
				}
			}
		}
		if( LA(1) == WHERE ) {
			match( WHERE );
			beanDao.setWhereConditionGroup( parseWhereCriteria( beanDao ) );
		}
	}
	
	public SelectCriteria parseIndividualSelectCriteria( BeanDao beanDao ) throws RecognitionException, TokenStreamException {
		SelectCriteria selectCriteria = new SelectCriteria(); 
		parseAqlVariable( beanDao, selectCriteria );
		return selectCriteria;
	}
	
	public AqlVariable parseAqlVariable( BeanDao beanDao, SelectCriteria selectCriteria ) throws RecognitionException, TokenStreamException {
		AqlVariable aqlVariable = null;
		Integer localFunctionId = uniqueFunctionId++;
		ConcatenatedAqlVariable concatenatedSelectCriteria = null;

		while( LA(1) == OPEN ) {
			match(OPEN);
			getOpenedGroups().add( new OpenedGroup( localFunctionId ) );
		}
		boolean isDistinct = false;
		OpenedGroup distinctOpenedGroup = null;
		if( LA(1) == DISTINCT ) {
			match( DISTINCT );
			isDistinct = true;

			if( LA(1) == OPEN ) {
				match(OPEN);
				distinctOpenedGroup = new OpenedGroup( localFunctionId );
				getOpenedGroups().add( distinctOpenedGroup );
				distinctOpenedGroup.setAvailable(false);
			}
		}
		
		switch( LA(1) ) {
			case NULL:
				aqlVariable = new BasicAqlVariable( LT(1).getText() );
				match( LA(1) );
				break;
			case COLON:
				match( COLON );
				if( LA(1) == IDENT ) {
					aqlVariable = new BasicAqlVariable( ":" + LT(1).getText() );
					match( LA(1) );
					break;
				} else {
					throw new RecognitionException( "Variable name expected after colon not: " + LT(1) );
				}
			/* 
			 * The star is to catch the wildcard variable, for example count(*)
			 */
			case STAR:
				aqlVariable = new BasicAqlVariable( "*" );
				match(LA(1));
				break;
			case IDENT:
				aqlVariable = parseEvaluatedTableVariable();
				break;
			case SELECT:
				OpenedGroup disabledGroup = disableLastOpenGroup();
				aqlVariable = parseQuerySelectCriteria( beanDao, selectCriteria );
				if( disabledGroup != null ) {
					disabledGroup.setAvailable(true);
				}
				break;
			case INTERVAL:
				aqlVariable = parseIntervalSelectCriteria(beanDao);
				break;
			default:
				if( functionsSet.contains( LA(1) ) ) {
					aqlVariable = parseFunction(beanDao);
				} else if( standAloneConstantSet.contains( LA(1) ) 
						|| numberConstantSet.contains( LA(1) )
						|| LA(1) == MINUS ) {
					StringBuffer aqlVariableStrBuf = new StringBuffer();
					
					
					if( standAloneConstantSet.contains( LA(1) ) ) {
						aqlVariableStrBuf.append( LT(1).getText() );
						match( LA(1) );
					} else {
						if( LA(1) == MINUS ) {
							aqlVariableStrBuf.append( LT(1).getText() );
							match( LA(1) );
						}
						while( numberConstantSet.contains( LA(1) ) ) { 
							aqlVariableStrBuf.append( LT(1).getText() );
							match( LA(1) );
						}
					}

					aqlVariable = new BasicAqlVariable( aqlVariableStrBuf.toString() );
				}
			break;
		}
		
		if( isDistinct ) {
			if( distinctOpenedGroup != null ) {
				distinctOpenedGroup.setAvailable(true);
				if( LA(1) == CLOSE ) {
					if( getOpenedGroups().peek().equals( distinctOpenedGroup ) ) {
						getOpenedGroups().pop();
						match( CLOSE );
					} else {
						throw new RecognitionException( "Distinct method was not closed" );
					}
				}
			}
			aqlVariable.setDistinct( true );
		}
		
		closeOpenedGroups( aqlVariable );
		if( LA(1) != COMMA ) {
			checkForUnclosedGroups( localFunctionId );
		}

		if( selectCriteria != null ) {
			selectCriteria.setAqlVariable(aqlVariable);
			if( (LA(1) == IDENT || LA(1) == AS) ) {
				if( LA(1) == AS ) {
					match( AS );
				}
				selectCriteria.setAlias( LT(1).getText() );
				match( LA(1) );
			}
		}

		/*
		 * This is also checking for in statements e.g. IN (123,345,435)
		 */
		if( concatenationSet.contains( LA(1) ) || 
				(LA(1) == COMMA && isClosetGroupAvailable()) ) {
			if( concatenatedSelectCriteria == null ) {
				concatenatedSelectCriteria = new ConcatenatedAqlVariable();
			}
			concatenatedSelectCriteria.addAqlVariable( aqlVariable, LT(1).getText() );
			match( LA(1) );
			aqlVariable = parseConcatenationSelectCriteria( beanDao, concatenatedSelectCriteria );
		} 
		
		if( aqlVariable != null ) {
			return aqlVariable;
		} else {
			throw new RecognitionException( "Select critieria expection, found " + LT(1) );
		}
	}
	
	public IntervalAqlVariable parseIntervalSelectCriteria( BeanDao beanDao ) throws TokenStreamException, RecognitionException {
		if( LA(1) != INTERVAL ) {
			throw new RecognitionException( LT(1).getText() + " found when keyword interval expected" );
		} else {
			match( LA(1) );
		}
		
		AqlVariable expression = parseAqlVariable(beanDao, null);
		
		String type = LT(1).getText();
		match( LA(1) );

		IntervalAqlVariable intervalAqlVariable = new IntervalAqlVariable(expression, type);
		return intervalAqlVariable;
	}
	
	public BetweenWhereCondition parseBetweenWhereCondition( AqlVariable leftHandVariable, BeanDao beanDao ) throws TokenStreamException, RecognitionException {
		if( LA(1) != BETWEEN ) {
			throw new RecognitionException( LT(1).getText() + " found when keyword between expected" );
		} else {
			match( LA(1) );
		}
		
		AqlVariable firstDateExpression = parseAqlVariable(beanDao, null);
		
		if( LA(1) == AND ) {
			match( LA(1) );	
		} else {
			throw new RecognitionException( LT(1).getText() + " found when keyword and expected" );
		}
		
		AqlVariable secondDateExpression = parseAqlVariable(beanDao, null);

		BetweenWhereCondition betweenWhereCondition = new BetweenWhereCondition( beanDao, leftHandVariable, firstDateExpression, secondDateExpression);
		return betweenWhereCondition;
	}
	
	public ConcatenatedAqlVariable parseConcatenationSelectCriteria( BeanDao beanDao, ConcatenatedAqlVariable concatenatedAqlVariable ) throws TokenStreamException, RecognitionException {
		boolean continueSearch = true;
		OpenedGroup disabledOpenGroup = disableLastOpenGroup();
		while( continueSearch ) {
			AqlVariable aqlVariable = parseAqlVariable( beanDao, null );

			if( concatenationSet.contains( LA(1) ) || LA(1) == COMMA ) {
				concatenatedAqlVariable.addAqlVariable( aqlVariable, LT(1).getText() );
				match( LA(1) );
			} else {
				concatenatedAqlVariable.addAqlVariable( aqlVariable, null );
				continueSearch = false;
			}
		}
		if( disabledOpenGroup != null ) {
			disabledOpenGroup.setAvailable(true);
		}
		closeOpenedGroups( concatenatedAqlVariable );
		return concatenatedAqlVariable;
	}
	
	private void closeOpenedGroups( AqlVariable aqlVariable ) throws RecognitionException, TokenStreamException {
		while( LA(1) == CLOSE ) {
			if( getOpenedGroups().size() > 0 ) {
				if( getOpenedGroups().peek().isAvailable() ) {
					getOpenedGroups().pop();
					match( CLOSE );
					if( aqlVariable != null ) {
						aqlVariable.setAddingParentheses( true );
					}
				} else {
					break;
				}
			} else {
				throw new RecognitionException( "Too many close brackets added" );
			}
		}
	}
	
	public UnevaluatedTableVariable parseEvaluatedTableVariable() throws TokenStreamException, RecognitionException {
		StringBuffer variableStrBuf = new StringBuffer();
		
		int lastColumn = LT(1).getColumn();
		while( variableSet.contains( LA(1) ) && lastColumn == LT(1).getColumn() ) {
			variableStrBuf.append( LT(1).getText() );
			lastColumn = LT(1).getColumn() + LT(1).getText().length();
			match( LA(1) );
		}
		UnevaluatedTableVariable variableSelectCriteria = new UnevaluatedTableVariable( variableStrBuf.toString() );
		return variableSelectCriteria;
	}
	
	public FunctionAqlVariable parseFunction( BeanDao aqlBeanDao ) throws TokenStreamException, RecognitionException {
		FunctionAqlVariable functionSelectCriteria = null;
		Integer localFunctionId = uniqueFunctionId++;
		
		if( functionsSet.contains( LA(1) ) ) {
			functionSelectCriteria = new FunctionAqlVariable();
			functionSelectCriteria.setName( LT(1).getText() );
			match( LA(1) );
		} else {
			throw new RecognitionException( LT(1).getText() + " is not a recognised function" );
		}
		OpenedGroup openedGroup;
		if( LA(1) == OPEN ) {
			match( LA(1) );
			openedGroup = new OpenedGroup( localFunctionId );
			openedGroup.setAvailable(false);
			getOpenedGroups().add( openedGroup );
		} else {
			throw new RecognitionException( "Open bracket expected after function " + functionSelectCriteria.getName() );
		}
		
		
		while( LA(1) != CLOSE ) {
			if( LA(1) == COMMA ) {
				match( COMMA );
			} else {
				functionSelectCriteria.getUnprocessedAqlVariableList().add( parseAqlVariable(aqlBeanDao, null) );
			}
		}
		match( CLOSE );
		if( getOpenedGroups().peek().equals( openedGroup ) ) {
			getOpenedGroups().pop();
		} else {
			throw new RecognitionException( "Attempt at closing a group that wasn't opened for this function" );
		}
		return functionSelectCriteria;
	}
	
	public void checkForAlias( SelectCriteria selectCriteria ) throws TokenStreamException, RecognitionException {
		if( LA(1) == AS ) {
			match( AS );
			if( LA(1) == IDENT ) {
				selectCriteria.setAlias( LT(1).getText() );
				match( LA(1) );
			} else {
				throw new RecognitionException( LT(1).getText() + " not allowed as an alias" );
			}
		}
	}
			
    public void match(int t) throws MismatchedTokenException, TokenStreamException {
        if (LA(1) != t)
            throw new MismatchedTokenException(tokenNames, LT(1), t, false, getFilename());
        else
        // mark token as consumed -- fetch next token deferred until LA/LT
            consume();
    }

	public Stack<OpenedGroup> getOpenedGroups() {
		return openedGroups;
	}

	public void setOpenedGroups(Stack<OpenedGroup> openedGroups) {
		this.openedGroups = openedGroups;
	}

	private class OpenedGroup {
		private int functionId;
		private boolean isAvailable = true;
		
		public OpenedGroup( int functionId ) {
			setFunctionId( functionId );
		}

		public int getFunctionId() {
			return functionId;
		}

		public void setFunctionId(int functionId) {
			this.functionId = functionId;
		}

		public boolean isAvailable() {
			return isAvailable;
		}

		public void setAvailable(boolean isAvailable) {
			this.isAvailable = isAvailable;
		}
	}
}
