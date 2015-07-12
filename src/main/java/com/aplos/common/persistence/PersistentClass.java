package com.aplos.common.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.DiscriminatorValue;
import com.aplos.common.annotations.persistence.Embedded;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Id;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.JoinTable;
import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.annotations.persistence.OneToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.AqlProxy;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.aql.IndividualWhereCondition;
import com.aplos.common.aql.JavassistProxyFactory;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.WhereConditionGroup;
import com.aplos.common.aql.antlr.AqlParser;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.module.ModuleUpgrader;
import com.aplos.common.persistence.collection.PersistentAbstractCollection;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.EmbeddedFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.IndexFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.PolymorphicFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ReflectionUtil;

public class PersistentClass extends PersistableTable {
	public enum InheritanceType {
		NONE,
		SINGLE_TABLE,
		JOINED_TABLE,
		TABLE_PER_CLASS,
		MAPPED_SUPER_CLASS,
		EMBEDDED;
	}
	private Class<?> tableClass;
	private boolean includeInApp = false;
	private Set<Class<?>> classDependencySet = new HashSet<Class<?>>();
	private Set<PersistentClass> persistentClassDependencySet = new HashSet<PersistentClass>();
	private boolean isTypesLoaded = false;
	private List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
	private List<FieldInfo> removeEmptyFieldInfos = new ArrayList<FieldInfo>();
	private HashMap<FieldInfo, Cascade> cascadeFieldInfoMap = new HashMap<FieldInfo, Cascade>();
	/*
	 * Cascade field infos for the full table not just for the class
	 */
	private HashMap<FieldInfo, Cascade> fullCascadeFieldInfoMap = new HashMap<FieldInfo, Cascade>();
	private Map<String, JunctionTable> collectionTableMap = new HashMap<String, JunctionTable>();
	private Map<String, JunctionTable> reverseCollectionTableMap = new HashMap<String, JunctionTable>();
	private PersistentClass parentPersistentClass;
	private InheritanceType inheritanceType = null;
	private Set<PersistentClass> subPersistentClasses = new HashSet<PersistentClass>();
	private boolean isDbTable = false;
	private boolean isEntity = false;
	private PersistentClass dbPersistentClass;
	private FieldInfo discriminatorFieldInfo;
	private String discriminatorValue;
	private Set<Class<?>> assignedMetaValueBaseClasses;
	private JavassistProxyFactory javassistProxyFactory;
	private Map<String,PersistentClass> persistentClassFamilyMap = new HashMap<String,PersistentClass>();
	private String updateSql;
	private String insertSql;

	public PersistentClass( Class<?> tableClass, boolean includeInApp ) {
		this.setTableClass(tableClass);
		this.includeInApp = includeInApp;
		if( tableClass.getAnnotation( Entity.class ) != null ) { 
			setEntity( true );
		}
		
		try {
			Field persistentClassField = ReflectionUtil.getField( tableClass, "persistentClass");
			if( persistentClassField != null ) {
				persistentClassField.setAccessible(true);
				persistentClassField.set( null, this );
			}
		} catch( IllegalAccessException iaex ) {
			ApplicationUtil.handleError( iaex );
		}
		
		for( Field field : tableClass.getDeclaredFields() ) {
			for( Annotation annotation : field.getAnnotations() ) {
				if( annotation instanceof ManyToOne || annotation instanceof OneToOne  ) {
					getClassDependencySet().add( field.getType() );	
					break;
				} else if( annotation instanceof OneToMany || annotation instanceof ManyToMany) {
					try {
						if( field.getGenericType() instanceof Class ) {
							getClassDependencySet().add( (Class<?>) field.getGenericType() );
						} else {
							Type actualTypes[] = ((ParameterizedTypeImpl) field.getGenericType()).getActualTypeArguments();
							for( int i = 0, n = actualTypes.length; i < n; i++ ) {
								if( AplosAbstractBean.class.isAssignableFrom( (Class<?>) actualTypes[ i ] ) ) {
									getClassDependencySet().add( (Class<?>) actualTypes[ i ] );
								}
							}
						}
					} catch( Exception ex ) {
						ApplicationUtil.getAplosContextListener().handleError( ex );
					}	
					break;
				}
			}
		}
	}
	
	public void addRemoveEmptyFields( AplosAbstractBean aplosAbstractBean ) {
		for( FieldInfo fieldInfo : getRemoveEmptyFieldInfos() ) {
			try {
				AplosAbstractBean newInstance = (AplosAbstractBean) CommonUtil.getNewInstance( ((PersistentClassFieldInfo) fieldInfo).getPersistentClass().getTableClass() );
	    		newInstance.initialiseNewBean();
	    		fieldInfo.getField().setAccessible(true);
	    		fieldInfo.getField().set(aplosAbstractBean, newInstance);
			} catch( Exception ex ) {
				ApplicationUtil.handleError(ex);
			}
		}
		
		if( getParentPersistentClass() != null ) {
			getParentPersistentClass().addRemoveEmptyFields(aplosAbstractBean);
		}
	}
	
	public void clearDbData( Connection conn ) throws SQLException {
		for( JunctionTable junctionTable : getCollectionTableList() ) {
			junctionTable.clearDbData( conn );
		}
		
		
		ModuleUpgrader.dropTable( determineSqlTableName(), true, conn );
		createTable(false, conn);
	}
	
	protected void buildProxyFactory() {
		HashSet proxyInterfaces = new HashSet();
		proxyInterfaces.add( AqlProxy.class );

		if ( getTableClass().isInterface() ) {
			proxyInterfaces.add( getTableClass() );
		}

		javassistProxyFactory = new JavassistProxyFactory();
		try {
			javassistProxyFactory.postInstantiate( determineSqlTableName(),
					getTableClass(),
					proxyInterfaces,
					determinePrimaryKeyGetterMethod(),
					determinePrimaryKeySetterMethod()
			);
		}
		catch ( Exception ex ) {
			ApplicationUtil.handleError( ex );
		}
	}
	
	public void checkDynamicMetaValues( Map<Class<?>,ImplicitPolymorphismMatch> dynamicMetaValuesMap ) {
		if( getAssignedMetaValueBaseClasses() == null ) {
			setAssignedMetaValueBaseClasses( new HashSet<Class<?>>() );

			boolean matchFound;
			for( Class<?> baseClass : dynamicMetaValuesMap.keySet() ) {
				matchFound = false;
				if( baseClass.isInterface() ) {
					PersistentClass currentPersistentClass = this;
					while( currentPersistentClass != null ) {
						for( Class<?> interfaceClass : currentPersistentClass.getTableClass().getInterfaces() ) {
							if( checkInterfaceAgainstMetaValueClass( interfaceClass, baseClass ) ) {
								matchFound = true;
								break;
							}
						}
						currentPersistentClass = currentPersistentClass.getParentPersistentClass();
					}
				} else {
					if( getTableClass().equals( baseClass ) ) {
						matchFound = true;
					}
				}
				if( matchFound && isIncludeInApp() ) {
					getAssignedMetaValueBaseClasses().add( baseClass );
					dynamicMetaValuesMap.get( baseClass ).getPersistentClasses().add( this );
					PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( baseClass );
					persistentClass.getPersistentClassFamilyMap().put( determineMetaValueName(), this );
				}
			}
			
			if( getParentPersistentClass() != null ) {
				if( getParentPersistentClass().getAssignedMetaValueBaseClasses() == null ) {
					getParentPersistentClass().checkDynamicMetaValues(dynamicMetaValuesMap);
				}
				
				getAssignedMetaValueBaseClasses().addAll( getParentPersistentClass().getAssignedMetaValueBaseClasses() );
				for( Class<?> baseClass : getParentPersistentClass().getAssignedMetaValueBaseClasses() ) {	
					dynamicMetaValuesMap.get( baseClass ).getPersistentClasses().add( this );
				}
			}
		}
	}
	
	public void copyFields( AplosAbstractBean srcBean, AplosAbstractBean destBean ) {
		try {
			Object tempConvertedValue;
			for( FieldInfo fieldInfo : getFieldInfos() ) {
				tempConvertedValue = SelectCriteria.convertFieldValue(fieldInfo.getValue( srcBean ), fieldInfo, fieldInfo.getField());
				fieldInfo.setValue( ((PersistenceBean) srcBean).isInDatabase(), destBean, tempConvertedValue );
			}
			
			if( getParentPersistentClass() != null ) {
				getParentPersistentClass().copyFields( srcBean, destBean );
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
		}
	}
	
	public void copyFieldsAndSetSaveable( AplosAbstractBean srcBean, AplosAbstractBean destBean,  BeanMap beanMap  ) {
		try {
			Object tempConvertedValue;
			for( FieldInfo fieldInfo : getFieldInfos() ) {
				if( fieldInfo.getField() != null && !getEmbeddedFieldInfos().contains( fieldInfo ) ) {
					fieldInfo.getField().setAccessible(true);
					if( fieldInfo instanceof IndexFieldInfo ) {
						tempConvertedValue = SelectCriteria.convertFieldValue(fieldInfo.getValue( srcBean ), fieldInfo, fieldInfo.getField());
					} else {
						tempConvertedValue = fieldInfo.getField().get( srcBean );
					}
					
					if( tempConvertedValue instanceof AplosAbstractBean ) {
						AplosAbstractBean aplosAbstractBean = ((AplosAbstractBean) tempConvertedValue);
						if( aplosAbstractBean != null ) {
							AplosAbstractBean foundBean = beanMap.findBean( (Class<? extends AplosAbstractBean>) ApplicationUtil.getDbPersistentClass(aplosAbstractBean).getTableClass(), aplosAbstractBean.getId());
							if( foundBean != null ) {
								tempConvertedValue = foundBean;
							}
						}
					}
					
					/* 
					 * This did have the additional condition:
					 * && fieldInfo.getCascadeAnnotation() != null
					 * however I removed it because when saving the object issues would occur because the collection
					 * would be set to lazy objects when updating the cache but this would also effect the live version,
					 * which caused issues if the live version had objects in that had id's but weren't yet commited to
					 * the database (set as newBeans in the persistenceContext
					 */
					if( tempConvertedValue instanceof PersistentAbstractCollection ) {
						tempConvertedValue = ((PersistentAbstractCollection) tempConvertedValue).getCopy();
						if( fieldInfo instanceof PersistentCollectionFieldInfo || fieldInfo instanceof ForeignFieldInfo ) {
							((PersistentAbstractCollection) tempConvertedValue).convertToSaveableBeans(beanMap);
						}
					} else if( tempConvertedValue instanceof AplosAbstractBean 
							&& ((PersistentClassFieldInfo) fieldInfo).getCascadeAnnotation() != null ) {
						if( fieldInfo instanceof ForeignFieldInfo && ((AplosAbstractBean) tempConvertedValue).isLazilyLoaded() ) {
							BeanDao resultBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) ((ForeignFieldInfo) fieldInfo).getPersistentClass().getTableClass() );
							resultBeanDao.setWhereCriteria( "bean." + ((ForeignFieldInfo) fieldInfo).getForeignFieldInfo().getField().getName() + ".id = " + srcBean.getId() );
							tempConvertedValue = resultBeanDao.getFirstBeanResult();
						}
						if( tempConvertedValue != null ) {
							tempConvertedValue = ((AplosAbstractBean) tempConvertedValue).getSaveableBean(beanMap);
						}
					}
					if( fieldInfo instanceof IndexFieldInfo ) {
						fieldInfo.setValue( ((PersistenceBean) srcBean).isInDatabase(), destBean, tempConvertedValue );
					} else {
						fieldInfo.getField().set( destBean, tempConvertedValue );
					}
				}
			}
			
			if( getParentPersistentClass() != null ) {
				getParentPersistentClass().copyFieldsAndSetSaveable( srcBean, destBean, beanMap );
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
		}
	}
	
	public void copyFieldsExceptPersistentClasses( AplosAbstractBean srcBean, AplosAbstractBean destBean ) {
		try {
			Object tempConvertedValue;
			for( FieldInfo fieldInfo : getFieldInfos() ) {
				if( fieldInfo.getField() != null && !getEmbeddedFieldInfos().contains( fieldInfo ) ) {
					fieldInfo.getField().setAccessible(true);
					if( fieldInfo instanceof IndexFieldInfo ) {
						tempConvertedValue = SelectCriteria.convertFieldValue(fieldInfo.getValue( srcBean ), fieldInfo, fieldInfo.getField());
					} else {
						tempConvertedValue = fieldInfo.getField().get( srcBean );
					}
					
					if( tempConvertedValue instanceof PersistentAbstractCollection ) {
						PersistentAbstractCollection persistentCollection = (PersistentAbstractCollection) SelectCriteria.convertFieldValue(fieldInfo.getValue( destBean ), fieldInfo, fieldInfo.getField());
						persistentCollection.replaceCollection( (PersistentAbstractCollection) tempConvertedValue );
						persistentCollection.replaceCollectionWithLazyBeans();
						tempConvertedValue = persistentCollection;
					} else if( tempConvertedValue instanceof AplosAbstractBean ) {
						if( ((PersistentClassFieldInfo) fieldInfo).isRemoveEmpty() && ((AplosAbstractBean) tempConvertedValue).isEmptyBean() ) {
							tempConvertedValue = null;
						} else {
							tempConvertedValue = ((AplosAbstractBean) tempConvertedValue).getLazyBean();
						}
					}

					if( fieldInfo instanceof IndexFieldInfo ) {
						fieldInfo.setValue( ((PersistenceBean) srcBean).isInDatabase(), destBean, tempConvertedValue );
					} else {
						fieldInfo.getField().set( destBean, tempConvertedValue );
					}
				}
			}
			
			if( getParentPersistentClass() != null ) {
				getParentPersistentClass().copyFieldsExceptPersistentClasses( srcBean, destBean );
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
		}
	}

	public void createPolymorphicWhereClause( ProcessedBeanDao processedBeanDao, AqlTable aqlTable, WhereConditionGroup whereConditionGroup ) {
		String fieldValue = "'" + getTableClass().getSimpleName() + "'";
		AqlTableVariable aqlVariable = new AqlTableVariable( aqlTable, getDbPersistentClass().getDiscriminatorFieldInfo() );
		aqlVariable.evaluateCriteriaTypes(processedBeanDao, false);

		AqlParser aqlParser = processedBeanDao.getBeanDao().getAqlParser().updateString( fieldValue );
		try {
			AqlVariable rightHandVariable = aqlParser.parseAqlVariable( processedBeanDao.getBeanDao(), null );
			IndividualWhereCondition whereCondition = new IndividualWhereCondition( processedBeanDao.getBeanDao(), aqlVariable, "=", rightHandVariable );
			whereConditionGroup.addWhereCondition( "OR", whereCondition );
		} catch( TokenStreamException tsex ) {
			ApplicationUtil.handleError( tsex );
		} catch( RecognitionException rex ) {
			ApplicationUtil.handleError( rex );
		}
		
		for( PersistentClass subPersistentClass : getSubPersistentClasses() ) {
			subPersistentClass.createPolymorphicWhereClause( processedBeanDao, aqlTable, whereConditionGroup );
		}
	}
	
	public String determineMetaValueName() {
		// TODO this will need to be changed at some point to take account of annotations
		return getTableClass().getSimpleName();
	}
	
	public boolean checkInterfaceAgainstMetaValueClass( Class<?> interfaceClass, Class<?> baseClass ) {
		if(interfaceClass.getName().startsWith( "com.aplos" )) {
			if( baseClass.equals( interfaceClass ) ) {
				return true;
			} else {
				for( Class<?> innerInterfaceClass : interfaceClass.getInterfaces() ) {
					if( checkInterfaceAgainstMetaValueClass( innerInterfaceClass, baseClass ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void createDbFieldInfoLists() {
		/*
		 * Needs to be a map otherwise more than one field with the same name might be added.
		 */
		if( getFullDbFieldInfoMap() == null ) {
			Map<String, FieldInfo> fullDbFieldInfos = new HashMap<String, FieldInfo>();
			setFullNonDbFieldInfoMap(new HashMap<String, FieldInfo>());
			setFullDbFieldInfoMap(fullDbFieldInfos);
			addFieldInfosToMap( getFieldInfos(), fullDbFieldInfos );
			setFullCascadeFieldInfoMap(getCascadeFieldInfoMap());
			if( isIncludingParentFieldInfos() ) {
				getParentPersistentClass().addParentFieldInfos( fullDbFieldInfos );
				getParentPersistentClass().addParentCascadeFieldInfos( getFullCascadeFieldInfoMap() );
			}
	
			Map<String, FieldInfo> classDbFieldInfos = new HashMap<String, FieldInfo>(fullDbFieldInfos);
			if( InheritanceType.SINGLE_TABLE.equals( getInheritanceType() ) ) {
				for( PersistentClass subPersistentClass : getSubPersistentClasses() ) {
					subPersistentClass.addSubFieldInfos( fullDbFieldInfos, getFullCascadeFieldInfoMap() );
				}
			}
			
			setAliasedFieldInfoMap( new HashMap<String, FieldInfo>(fullDbFieldInfos) );

			FieldInfo tempFieldInfo;
			for( String mapKey : new ArrayList<String>(getAliasedFieldInfoMap().keySet()) ) {
				tempFieldInfo = fullDbFieldInfos.get( mapKey ); 
				if( tempFieldInfo instanceof PersistentClassFieldInfo ) {
					getAliasedFieldInfoMap().put( tempFieldInfo.getSqlName(), tempFieldInfo);
				}
			}

			Map<String, FieldInfo> classAliasedFieldInfos = new HashMap<String, FieldInfo>(classDbFieldInfos);
			for( String mapKey : new ArrayList<String>(classAliasedFieldInfos.keySet()) ) {
				tempFieldInfo = fullDbFieldInfos.get( mapKey ); 
				if( tempFieldInfo instanceof PersistentClassFieldInfo ) {
					classAliasedFieldInfos.put( tempFieldInfo.getSqlName(), tempFieldInfo);
				}
			}
			setClassAliasedFieldInfoMap(classAliasedFieldInfos);
			
			for( String mapKey : new ArrayList<String>(fullDbFieldInfos.keySet()) ) {
				tempFieldInfo = fullDbFieldInfos.get( mapKey ); 
				if( tempFieldInfo instanceof CollectionFieldInfo 
						|| tempFieldInfo instanceof ForeignFieldInfo 
						|| tempFieldInfo instanceof EmbeddedFieldInfo ) {
					getFullNonDbFieldInfoMap().put( mapKey, fullDbFieldInfos.get( mapKey ) );
					fullDbFieldInfos.remove( mapKey );
					classDbFieldInfos.remove( mapKey );
				}
			}
			setClassDbFieldInfoMap(classDbFieldInfos);
		}
	}
	
	@Override
	public FieldInfo getFieldInfoFromSqlName(String sqlName, boolean searchAllClasses) {
		if( InheritanceType.JOINED_TABLE.equals( getInheritanceType() ) && searchAllClasses ) {
			FieldInfo fieldInfo = super.getFieldInfoFromSqlName(sqlName,searchAllClasses);
			if( fieldInfo == null && getParentPersistentClass() != null ) {
				fieldInfo = getParentPersistentClass().getFieldInfoFromSqlName(sqlName,searchAllClasses);
			}
			return fieldInfo;
		} else {
			return super.getFieldInfoFromSqlName(sqlName,searchAllClasses);
		}
	}
	
	public boolean isIncludingParentFieldInfos() {
		return getParentPersistentClass() != null && 
				(getParentPersistentClass().getInheritanceType().equals( InheritanceType.MAPPED_SUPER_CLASS ) 
						|| getParentPersistentClass().getInheritanceType().equals( InheritanceType.TABLE_PER_CLASS )
						|| getParentPersistentClass().getInheritanceType().equals( InheritanceType.SINGLE_TABLE ));
	}
	
	@Override
	public void addFieldInfoToMap(Map<String, FieldInfo> fullDbFieldInfos,
			String mapKey, FieldInfo fieldInfo) {
		FieldInfo oldFieldInfo = fullDbFieldInfos.put( mapKey, fieldInfo );
		/*
		 * Ignore the warnings for single tables as two subclasses can share the same field name.
		 */
		if( oldFieldInfo != null ) {
			fullDbFieldInfos.put( mapKey, oldFieldInfo );
			if( InheritanceType.SINGLE_TABLE.equals( oldFieldInfo.getParentPersistentClass().getInheritanceType() ) ) {
				oldFieldInfo.addAdditionalDeclaringClass( fieldInfo );
			} else {
				ApplicationUtil.handleError( new Exception( "Field infos share the same mapKey - " + mapKey ) );
			}
		}	
	}
	
	public void addFieldInfosToMap( List<FieldInfo> fieldInfos, Map<String, FieldInfo> fieldInfoMap ) {
		for( int i = 0, n = fieldInfos.size(); i < n; i++ ) {
			addFieldInfoToMap( fieldInfoMap, fieldInfos.get( i ).determineFieldMapKeyName(), getFieldInfos().get( i ) );
		}
	}
	
	public void addSubFieldInfos( Map<String, FieldInfo> parentFieldInfoMap,  Map<FieldInfo, Cascade> parentCascadeFieldInfoMap ) {
		createDbFieldInfoLists();
		addFieldInfosToMap( getFieldInfos(), parentFieldInfoMap );
		for( FieldInfo fieldInfo : getCascadeFieldInfoMap().keySet() ) {
			parentCascadeFieldInfoMap.put( fieldInfo, getCascadeFieldInfoMap().get( fieldInfo ) );
		}
		if( getInheritanceType().equals( InheritanceType.SINGLE_TABLE ) ) {
			for( PersistentClass subPersistentClass : getSubPersistentClasses() ) {
				subPersistentClass.addSubFieldInfos( parentFieldInfoMap, parentCascadeFieldInfoMap );
			}
		}
	}
	
	public void addParentFieldInfos( Map<String, FieldInfo> fieldInfoMap ) {
		createDbFieldInfoLists();
		addFieldInfosToMap( getFieldInfos(), fieldInfoMap );
		if( isIncludingParentFieldInfos() ) {
			getParentPersistentClass().addParentFieldInfos( fieldInfoMap );
		}
	}
	
	public void addParentCascadeFieldInfos( Map<FieldInfo, Cascade> fullCascadeFieldInfoMap ) {
		for( FieldInfo tempFieldInfo : getCascadeFieldInfoMap().keySet() ) {
			fullCascadeFieldInfoMap.put( tempFieldInfo, getCascadeFieldInfoMap().get(tempFieldInfo) );
		}
		if( isIncludingParentFieldInfos() ) {
			getParentPersistentClass().addParentCascadeFieldInfos( fullCascadeFieldInfoMap );
		}
	}
	
	@Override
	public void createForeignKeysAndIndexes() {
		for( int i = getFullDbFieldInfos().size() - 1; i > -1; i-- ) {
			getFullDbFieldInfos().get( i ).addForeignKeysAndIndexes( getForeignKeys(), getColumnIndexes() );
		}		
	}
	
//	@Override
//	public Map<String, FieldInfo> getFullDbFieldInfoMap() {
//		if( getDbPersistentClass() == null ) {
//			return super.getFullDbFieldInfoMap();	
//		} else {
//			return getDbPersistentClass().getFullDbFieldInfoMap();
//		}
//	}
	
	@Override
	public List<FieldInfo> getFullDbFieldInfos() {
		if( !getDbPersistentClass().equals(this) && InheritanceType.SINGLE_TABLE.equals( getInheritanceType() ) ) {
			return getDbPersistentClass().getFullDbFieldInfos();
		} else {
			return super.getFullDbFieldInfos();	
		}
	}
	
	public List<JunctionTable> getCollectionTableList() {
		return new ArrayList<JunctionTable>( getCollectionTableMap().values() );
	}
	
	public JunctionTable getCollectionTable( String tableName ) {
		return getCollectionTableMap().get( tableName );
	}
	
	public List<FieldInfo> getInsertFieldInfos() {
//		if( InheritanceType.JOINED_TABLE.equals( getInheritanceType() ) 
//				&& !this.equals( getDbPersistentClass() ) ) {
			return getFullDbFieldInfos();
//		} else {
//			return getFullDbFieldInfosWithoutId();
//		}
	}
	
	public void generateSqlStatements() {
		initialiseDbInformation();
		List<FieldInfo> insertFieldInfos = getInsertFieldInfos();
		StringBuffer strBuf = new StringBuffer( "INSERT INTO " );
		strBuf.append( determineSqlTableName() ).append( " (" );
		for( int i = 0, n = insertFieldInfos.size(); i < n; i++ ) {
			strBuf.append( "`" ).append( insertFieldInfos.get( i ).getSqlName() ).append( "`," );
		}
		strBuf.replace( strBuf.length() - 1, strBuf.length(), "" );
		strBuf.append( ") VALUES (" );
		for( int i = 0, n = insertFieldInfos.size(); i < n; i++ ) {
			strBuf.append("?").append( "," );
		}
		strBuf.replace( strBuf.length() - 1, strBuf.length(), "" );
		strBuf.append( ")" );
		setInsertSql( strBuf.toString() );

		strBuf = new StringBuffer( "UPDATE " );
		strBuf.append( determineSqlTableName() ).append( " SET " );
		for( int i = 0, n = getFullDbFieldInfosWithoutId().size(); i < n; i++ ) {
			strBuf.append( getFullDbFieldInfosWithoutId().get( i ).getSqlName() ).append(" = ");
			strBuf.append("?").append( "," );
		}
		strBuf.replace( strBuf.length() - 1, strBuf.length(), "" );
		strBuf.append( " WHERE " ).append( determinePrimaryKeyFieldInfo().getSqlName() );
		strBuf.append( " = " ).append( "?" );
		setUpdateSql( strBuf.toString() );
	}
	
	public String determineSqlTableName() {
		if( InheritanceType.SINGLE_TABLE.equals( getInheritanceType() ) &&
				getParentPersistentClass() != null &&
				InheritanceType.SINGLE_TABLE.equals( getParentPersistentClass().getInheritanceType() ) ) {
			return getParentPersistentClass().determineSqlTableName();
		} else {
			// don't get SimpleName as inner classes would then return a result without the $ appendage
			return getTableClass().getName().substring( getTableClass().getName().lastIndexOf( "." ) + 1 );
		}
	}
	
	public void addSubPersistentClass( PersistentClass persistentClass ) {
		getSubPersistentClasses().add( persistentClass );
	}
	
	public void findParentClasses( Map<Class<?>, PersistentClass> persistentClassMap ) {
		setParentPersistentClass( persistentClassMap.get( getTableClass().getSuperclass() ) );
		if( getParentPersistentClass() != null ) {
			getParentPersistentClass().addSubPersistentClass( this );
		} else {
			Class<?> parentClass = getTableClass().getSuperclass();
			boolean ancestorIsEntity = false; 
			outerWhile : while( !parentClass.equals( Object.class ) ) {
				if( persistentClassMap.get( parentClass ) != null ) {
					ancestorIsEntity = true;
					break;
				} else {
					for( Annotation tempAnnotation : parentClass.getAnnotations() ) {
						if( tempAnnotation instanceof Entity || tempAnnotation instanceof MappedSuperclass ) {
							createNewPersistentClass(persistentClassMap, parentClass );
							ancestorIsEntity = true;
							break outerWhile;
						}
					}
				}
				parentClass = parentClass.getSuperclass();
			}
			setParentPersistentClass( persistentClassMap.get( getTableClass().getSuperclass() ) );
			if( getParentPersistentClass() != null ) {
				getParentPersistentClass().addSubPersistentClass( this );				
			} else if( ancestorIsEntity ) {
				setParentPersistentClass( createNewPersistentClass(persistentClassMap, getTableClass().getSuperclass() ) );
				getParentPersistentClass().addSubPersistentClass( this );
				getParentPersistentClass().findParentClasses(persistentClassMap);
			}
		}
	}
	
	public PersistentClass createNewPersistentClass( Map<Class<?>, PersistentClass> persistentClassMap, Class<?> tableClass ) {
		PersistentClass persistentClass = new PersistentClass( tableClass, true );
		persistentClassMap.put( tableClass, persistentClass );
		markDependenciesForInclusion();
		persistentClass.findParentClasses(persistentClassMap);
		return persistentClass;
	}
	
	public void determineInheritanceType() {
		if( getTableClass().getAnnotation( DiscriminatorValue.class ) != null ) {
			setDiscriminatorValue( getTableClass().getAnnotation( DiscriminatorValue.class ).value() );
		} else {
			setDiscriminatorValue( getTableClass().getSimpleName() );
		}
		
		if( getInheritanceType() == null ) {
			if( getParentPersistentClass() != null ) {
				if( getParentPersistentClass().getInheritanceType() == null ) {
					getParentPersistentClass().determineInheritanceType();
				}
			}
			if( getInheritanceType() == null ) {
				if( getTableClass().getAnnotations().length == 0 ) {
					if( getParentPersistentClass() != null ) {
						setInheritanceType(getParentPersistentClass().getInheritanceType());
					} else {
						setInheritanceType(InheritanceType.EMBEDDED);
					}
				} else {
					for( Annotation tempAnnotation : getTableClass().getAnnotations() ) {
						if( tempAnnotation instanceof MappedSuperclass ) {
							setInheritanceType(InheritanceType.MAPPED_SUPER_CLASS);	
							break;
						} else if( tempAnnotation instanceof DiscriminatorColumn || tempAnnotation instanceof DiscriminatorValue ) {
							setInheritanceType(InheritanceType.SINGLE_TABLE);		
							break;
						} else if( tempAnnotation instanceof Inheritance ) {
							if( ((Inheritance) tempAnnotation).strategy().equals( ((Inheritance) tempAnnotation).strategy().JOINED ) ) {
								setInheritanceType(InheritanceType.JOINED_TABLE);	
								break;
							} else if( ((Inheritance) tempAnnotation).strategy().equals( ((Inheritance) tempAnnotation).strategy().SINGLE_TABLE ) ) {
								setInheritanceType(InheritanceType.SINGLE_TABLE);	
								break;
							} else if( ((Inheritance) tempAnnotation).strategy().equals( ((Inheritance) tempAnnotation).strategy().TABLE_PER_CLASS ) ) {
								setInheritanceType(InheritanceType.TABLE_PER_CLASS);	
								break;
							}
						}
					}
					if( getInheritanceType() == null ) {
						if( getParentPersistentClass() != null && !getParentPersistentClass().getInheritanceType().equals( InheritanceType.MAPPED_SUPER_CLASS ) ) {
							if( getParentPersistentClass().getInheritanceType().equals( InheritanceType.NONE ) ) {
								getParentPersistentClass().setInheritanceType(InheritanceType.SINGLE_TABLE);
							} 
							setInheritanceType(getParentPersistentClass().getInheritanceType());
						} else {
							if( getSubPersistentClasses().size() > 0 ) {
								setInheritanceType(InheritanceType.SINGLE_TABLE);
							} else {
								setInheritanceType(InheritanceType.NONE);
							}
						}
					}
				}
			}
		}
		
		if( (InheritanceType.JOINED_TABLE.equals( getInheritanceType() ) && (getParentPersistentClass() == null || getParentPersistentClass().getInheritanceType().equals( InheritanceType.MAPPED_SUPER_CLASS ))) 
				|| InheritanceType.TABLE_PER_CLASS.equals( getInheritanceType() ) 
				|| (InheritanceType.SINGLE_TABLE.equals( getInheritanceType() ) && (getParentPersistentClass() == null || getParentPersistentClass().getInheritanceType().equals( InheritanceType.MAPPED_SUPER_CLASS ))) 
				|| InheritanceType.NONE.equals( getInheritanceType() ) ) {
			setDbTable(true);
			setDbPersistentClass(this);
			addToFamilyPersistentClassMaps( this );
		} else if(InheritanceType.MAPPED_SUPER_CLASS.equals( getInheritanceType() )) {
			setDbPersistentClass(null);
		} else {
			PersistentClass dbPersistentClass = getParentPersistentClass();
			while( dbPersistentClass != null 
					&& (!dbPersistentClass.isDbTable() 
							|| (getParentPersistentClass() != null && InheritanceType.JOINED_TABLE.equals( dbPersistentClass.getParentPersistentClass().getInheritanceType() ))) ) {
				dbPersistentClass = dbPersistentClass.getParentPersistentClass();
			}
			if( dbPersistentClass != null ) {
				setDbPersistentClass( dbPersistentClass );
				
				if( InheritanceType.JOINED_TABLE.equals( getInheritanceType() ) ) {
					setDbTable(true);
				}

//				addToFamilyPersistentClassMaps( dbPersistentClass );
				PersistentClass familyPersistentClass = dbPersistentClass;
				familyPersistentClass.getPersistentClassFamilyMap().put( getDiscriminatorValue(), this );
				while( familyPersistentClass.getParentPersistentClass() != null 
						&& InheritanceType.MAPPED_SUPER_CLASS.equals( familyPersistentClass.getParentPersistentClass().getInheritanceType() ) ) {
					familyPersistentClass = familyPersistentClass.getParentPersistentClass();
					familyPersistentClass.getPersistentClassFamilyMap().put( getDiscriminatorValue(), this );
				}
			}
		}
	}
	
	public void addToFamilyPersistentClassMaps( PersistentClass familyPersistentClass ) {
		familyPersistentClass.getPersistentClassFamilyMap().put( getDiscriminatorValue(), this );
		while( familyPersistentClass.getParentPersistentClass() != null 
				&& InheritanceType.MAPPED_SUPER_CLASS.equals( familyPersistentClass.getParentPersistentClass().getInheritanceType() ) ) {
			familyPersistentClass = familyPersistentClass.getParentPersistentClass();
			familyPersistentClass.getPersistentClassFamilyMap().put( getDiscriminatorValue(), this );
		}
	}

	public void markDependenciesForInclusion() {
		for( PersistentClass persistentClass : persistentClassDependencySet ) {
			if( !persistentClass.isIncludeInApp() ) {
				persistentClass.setIncludeInApp( true );
				persistentClass.markDependenciesForInclusion();
			}
		}
	}
	
	public PersistentClass getPersistentClassForField( String fieldName ) {
		if( getFieldInfoFromSqlName( fieldName, false ) != null ) {
			return this;
		} else if( getParentPersistentClass() != null ){
			return getParentPersistentClass().getPersistentClassForField( fieldName );
		}
		return null;
	}
	
	public void loadTypes() {
		if( !isTypesLoaded ) {
			setTypesLoaded( true );
			Field[] declaredFields = getTableClass().getDeclaredFields();
			FieldInfo tempFieldInfo = null;
			PersistentClass tempPersistentClass = null;
			String mappedBy = null;
			Class<?> tempCollectionArgumentType;
			Map<Class<?>, PersistentClass> persistentClassMap = ApplicationUtil.getAplosContextListener().getPersistentApplication().getPersistentClassMap();
			JoinColumn joinColumnAnnotation = null;
			JoinTable joinTableAnnotation = null;
			DynamicMetaValues dynamicMetaValuesAnnotation = null;
			for( int i = 0, n = declaredFields.length; i < n; i++ ) {
				if( !Modifier.isStatic(declaredFields[ i ].getModifiers()) && !Modifier.isTransient(declaredFields[ i ].getModifiers()) && declaredFields[ i ].getAnnotation(Transient.class) == null ) {
					joinColumnAnnotation = null;
					joinTableAnnotation = null;
					dynamicMetaValuesAnnotation = null;
					Id idAnnotation = null;
					Annotation relationshipAnnotation = null;
					Cascade cascadeAnnotation = null;
					mappedBy = null;
					tempFieldInfo = null;
					
					for( Annotation tempAnnotation : declaredFields[ i ].getAnnotations() ) {
						if( tempAnnotation instanceof OneToMany 
								|| tempAnnotation instanceof ManyToMany 
								|| tempAnnotation instanceof ManyToOne 
								|| tempAnnotation instanceof OneToOne
								|| tempAnnotation instanceof ManyToAny
								|| tempAnnotation instanceof Any
								|| tempAnnotation instanceof CollectionOfElements
								|| tempAnnotation instanceof Embedded ) {
							relationshipAnnotation = tempAnnotation;
							
							if( (tempAnnotation instanceof OneToMany && !CommonUtil.isNullOrEmpty( ((OneToMany) tempAnnotation).mappedBy() )) ) {
								mappedBy = ((OneToMany) tempAnnotation).mappedBy();
							} else if( (tempAnnotation instanceof OneToOne && !CommonUtil.isNullOrEmpty( ((OneToOne) tempAnnotation).mappedBy())) ) {
								mappedBy = ((OneToOne) tempAnnotation).mappedBy();
							}
						} else if( tempAnnotation instanceof JoinColumn ) {
							joinColumnAnnotation = (JoinColumn) tempAnnotation;
						} else if( tempAnnotation instanceof JoinTable ) {
							joinTableAnnotation = (JoinTable) tempAnnotation;
						} else if( tempAnnotation instanceof Id ) {
							idAnnotation = (Id) tempAnnotation;
						} else if( tempAnnotation instanceof DynamicMetaValues ) {
							dynamicMetaValuesAnnotation = (DynamicMetaValues) tempAnnotation;
						} else if( tempAnnotation instanceof Cascade ) {
							cascadeAnnotation = (Cascade) tempAnnotation;
						}
					}
					
					String sqlTableName = null;
					if( joinTableAnnotation != null && !CommonUtil.isNullOrEmpty( joinTableAnnotation.name() ) ) {
						sqlTableName = joinTableAnnotation.name();
					}
					
					if( declaredFields[ i ].getType().isEnum() ) {
						ApplicationUtil.getPersistentApplication().registerEnumType( (Class<Enum>) declaredFields[ i ].getType() );
					}
					
					if( relationshipAnnotation != null ) {
						if( (relationshipAnnotation instanceof OneToMany && joinColumnAnnotation != null) 
							 || mappedBy != null ) {
							tempPersistentClass = persistentClassMap.get( getMainFieldClass( declaredFields[ i ] ) );
							tempFieldInfo = new ForeignFieldInfo( tempPersistentClass, this, declaredFields[ i ] );
							if( mappedBy != null ) {
								((ForeignFieldInfo)tempFieldInfo).setMappedBy(mappedBy);
							} else if( joinColumnAnnotation != null ) {
								((ForeignFieldInfo)tempFieldInfo).setJoinColumnAnnotation(joinColumnAnnotation);
							}
						} else if( relationshipAnnotation instanceof OneToMany 
								|| relationshipAnnotation instanceof ManyToMany
								|| relationshipAnnotation instanceof ManyToAny ) {
							Class<?> mainFieldClass = getMainFieldClass( declaredFields[ i ] );
							tempPersistentClass = persistentClassMap.get( mainFieldClass );
							
							if( tempPersistentClass != null ) {
								tempFieldInfo = new PersistentCollectionFieldInfo( this, declaredFields[ i ], tempPersistentClass, tempPersistentClass.getTableClass(), relationshipAnnotation );
							} else {
								tempFieldInfo = new CollectionFieldInfo(this,declaredFields[i], mainFieldClass, relationshipAnnotation);
							}
							((CollectionFieldInfo) tempFieldInfo).setMappedBy( mappedBy );
						} else if( relationshipAnnotation instanceof ManyToOne 
								|| relationshipAnnotation instanceof OneToOne
								|| relationshipAnnotation instanceof Any ) {
							if( persistentClassMap.get( declaredFields[i].getType() ) != null ) {
								tempFieldInfo = new PersistentClassFieldInfo( persistentClassMap.get( declaredFields[i].getType() ),this,declaredFields[i]);
							} else {
								ApplicationUtil.getAplosContextListener().handleError( new Exception( "Persistent class not found for " + declaredFields[i].getType() ) );
							}
						} else if( relationshipAnnotation instanceof CollectionOfElements ) {
							if( Map.class.isAssignableFrom( declaredFields[ i ].getType() ) ) {
								tempCollectionArgumentType = (Class<?>) ((ParameterizedTypeImpl) declaredFields[ i ].getGenericType()).getActualTypeArguments()[ 1 ];
							} else {
								tempCollectionArgumentType = (Class<?>) ((ParameterizedTypeImpl) declaredFields[ i ].getGenericType()).getActualTypeArguments()[ 0 ];
							}
							tempFieldInfo = new CollectionFieldInfo( this, declaredFields[ i ], tempCollectionArgumentType, relationshipAnnotation );
						} else if( relationshipAnnotation instanceof Embedded ) {
							tempPersistentClass = persistentClassMap.get( declaredFields[ i ].getType() );
							tempFieldInfo = new EmbeddedFieldInfo(tempPersistentClass,this,declaredFields[i]);
						}
					}

					if( tempFieldInfo == null ) {
						tempFieldInfo = new FieldInfo(this,declaredFields[i]);
					}
					
					tempFieldInfo.init();
					getFieldInfos().add( tempFieldInfo );
					if( tempFieldInfo instanceof PersistentClassFieldInfo 
							&& ((PersistentClassFieldInfo) tempFieldInfo).isRemoveEmpty() ) {
						getRemoveEmptyFieldInfos().add( tempFieldInfo );
					}
					if( cascadeAnnotation != null || relationshipAnnotation instanceof CollectionOfElements ) {
						tempFieldInfo.setCascadeAnnotation(cascadeAnnotation);
						getCascadeFieldInfoMap().put( tempFieldInfo, cascadeAnnotation );
					}

					
					updatePersistentClassFamily( tempFieldInfo, sqlTableName, dynamicMetaValuesAnnotation );

					if( !(tempFieldInfo instanceof CollectionFieldInfo) ) {
						tempFieldInfo.addAdditionalFieldInfos( this );	
					}
					
					if( idAnnotation != null ) {
						updatePrimaryKeyFieldInfo(tempFieldInfo);
						tempFieldInfo.setPrimaryKey(true);
						tempFieldInfo.getApplicationType().setNullable(false);
					}
				}
			}
			
			if( InheritanceType.JOINED_TABLE.equals( getInheritanceType() ) ) {
				if( (getParentPersistentClass() == null || !getParentPersistentClass().getInheritanceType().equals( InheritanceType.JOINED_TABLE )) ) {
					if( getSubPersistentClasses().size() > 0 || getTableClass().getAnnotation( DiscriminatorColumn.class ) != null ) {
						tempFieldInfo = new PolymorphicFieldInfo( this, null );
						tempFieldInfo.init();
						setDiscriminatorFieldInfo(tempFieldInfo);
						getFieldInfos().add( tempFieldInfo );
					}
				}
			}
			
			if( InheritanceType.SINGLE_TABLE.equals( getInheritanceType() ) ) {
				if( (getParentPersistentClass() == null || !getParentPersistentClass().getInheritanceType().equals( InheritanceType.SINGLE_TABLE )) ) {
					if( getSubPersistentClasses().size() > 0 ) { //|| getTableClass().getAnnotation( DiscriminatorColumn.class ) != null ) {
						tempFieldInfo = new PolymorphicFieldInfo( this, null );
						tempFieldInfo.init();
						setDiscriminatorFieldInfo(tempFieldInfo);
						getFieldInfos().add( tempFieldInfo );
					}
				} else {
					/* set fields as nullable as these fields might not be used if the parent class
					 * implementation is used.
					 */
					for( int i = 0, n = getFieldInfos().size(); i < n; i++ ) {
						if( !(getFieldInfos().get( i ) instanceof CollectionFieldInfo) ) {
							getFieldInfos().get( i ).getApplicationType().setNullable(true);
						}
					}
				}
			}
		}
	}
	
	public FieldInfo determinePrimaryKeyFieldInfo() {
		if( getPrimaryKeyFieldInfo() != null ) {
			return getPrimaryKeyFieldInfo();
		} else if( getParentPersistentClass() != null ) {
			return getParentPersistentClass().determinePrimaryKeyFieldInfo();
		}
		return null;
	}
	
	public Method determinePrimaryKeyGetterMethod() {
		if( getPrimaryKeyGetterMethod() != null ) {
			return getPrimaryKeyGetterMethod();
		} else if( getParentPersistentClass() != null ) {
			return getParentPersistentClass().determinePrimaryKeyGetterMethod();
		}
		return null;
	}
	
	public Method determinePrimaryKeySetterMethod() {
		if( getPrimaryKeyGetterMethod() != null ) {
			return getPrimaryKeyGetterMethod();
		} else if( getParentPersistentClass() != null ) {
			return getParentPersistentClass().determinePrimaryKeySetterMethod();
		}
		return null;
	}
	
	public Class<?> getMainFieldClass( Field field ) {
		PersistentClass tempPersistentClass = ApplicationUtil.getPersistentClass( field.getType() );
		if( tempPersistentClass != null || AplosAbstractBean.class.isAssignableFrom( field.getType() ) ) {
			return field.getType();
		} else if( Map.class.isAssignableFrom( field.getType() ) ) {
			return (Class<?>) ((ParameterizedTypeImpl) field.getGenericType()).getActualTypeArguments()[ 1 ];
		} else {
			return (Class<?>) ((ParameterizedTypeImpl) field.getGenericType()).getActualTypeArguments()[ 0 ];
		}
	}
	
	public void addCollectionTableNames( List<String> collectionTableNames ) {
		for( JunctionTable collectionTable : getCollectionTableMap().values() ) {
			collectionTableNames.add( collectionTable.getSqlTableName() );
		}
	}
	
	public void updatePersistentClassFamily( FieldInfo fieldInfo, String sqlTableName, DynamicMetaValues dynamicMetaValuesAnnotation ) {
		if( getInheritanceType().equals( InheritanceType.SINGLE_TABLE ) 
				&& getParentPersistentClass() != null && getParentPersistentClass().getInheritanceType().equals( InheritanceType.SINGLE_TABLE ) ) {
			getParentPersistentClass().updatePersistentClassFamily( fieldInfo, sqlTableName, dynamicMetaValuesAnnotation );
		} else {
			if( getInheritanceType().equals( InheritanceType.MAPPED_SUPER_CLASS ) || getInheritanceType().equals( InheritanceType.TABLE_PER_CLASS ) ) {
				for( PersistentClass subPersistentClass : getSubPersistentClasses() ) {
					subPersistentClass.updatePersistentClassFamily( fieldInfo, sqlTableName, dynamicMetaValuesAnnotation);
				}
			}
			
			if( isEntity() ) {
				if( fieldInfo instanceof CollectionFieldInfo ) {
					if( CommonUtil.isNullOrEmpty( sqlTableName ) ) {
						sqlTableName = JunctionTable.generateSqlTableName((CollectionFieldInfo) fieldInfo, this);
					} else {
						sqlTableName = sqlTableName.toLowerCase();
					}
					JunctionTable collectionTable = getCollectionTableMap().get( sqlTableName );
					if( collectionTable == null ) {
						collectionTable = new JunctionTable( (CollectionFieldInfo)fieldInfo, this, sqlTableName );
						getCollectionTableMap().put( collectionTable.getSqlTableName(), collectionTable );
						if( fieldInfo instanceof PersistentCollectionFieldInfo ) {
							((PersistentCollectionFieldInfo) fieldInfo).getPersistentClass().getReverseCollectionTableMap().put( collectionTable.getSqlTableName(), collectionTable );
						}
					} else {
						collectionTable.updateCollectionFieldInfo((CollectionFieldInfo)fieldInfo);
					}
				}

				if( dynamicMetaValuesAnnotation != null && isIncludeInApp() ) {
					Class<?> baseClass = null;
					if( fieldInfo instanceof CollectionFieldInfo ) {
						baseClass = ((CollectionFieldInfo) fieldInfo).getFieldClass();
					} else if( fieldInfo instanceof PersistentClassFieldInfo ) {
						baseClass = ((PersistentClassFieldInfo) fieldInfo).getPersistentClass().getTableClass();
					}
					ImplicitPolymorphismMatch implicitPolymorphismMatch = ApplicationUtil.getPersistentApplication().getDynamicMetaValuesMap().get( baseClass );
					if( implicitPolymorphismMatch == null ) {
						implicitPolymorphismMatch = new ImplicitPolymorphismMatch( baseClass );
							ApplicationUtil.getPersistentApplication().getDynamicMetaValuesMap().put( baseClass, implicitPolymorphismMatch );
					}
					implicitPolymorphismMatch.addToFieldInfoMap( fieldInfo, this );
				}
			}
			
		}
	}
	
	@Override
	public String toString() {
		return getTableClass().getName();
	}

	public void setIncludeInApp(boolean includeInApp) {
		this.includeInApp = includeInApp;
	}
	public boolean isIncludeInApp() {
		return includeInApp;
	}

	public void setTableClass(Class<?> tableClass) {
		this.tableClass = tableClass;
	}

	public Class<?> getTableClass() {
		return tableClass;
	}

	public Set<Class<?>> getClassDependencySet() {
		return classDependencySet;
	}

	public Set<PersistentClass> getPersistentClassDependencySet() {
		return persistentClassDependencySet;
	}

	public boolean isTypesLoaded() {
		return isTypesLoaded;
	}

	public void setTypesLoaded(boolean isTypesLoaded) {
		this.isTypesLoaded = isTypesLoaded;
	}

	public PersistentClass getParentPersistentClass() {
		return parentPersistentClass;
	}

	public void setParentPersistentClass(PersistentClass parentPersistentClass) {
		this.parentPersistentClass = parentPersistentClass;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public void setInheritanceType(InheritanceType inheritanceType) {
		this.inheritanceType = inheritanceType;
	}

	public Set<PersistentClass> getSubPersistentClasses() {
		return subPersistentClasses;
	}

	public void setSubPersistentClasses(Set<PersistentClass> subPersistentClasses) {
		this.subPersistentClasses = subPersistentClasses;
	}

	public boolean isDbTable() {
		return isDbTable;
	}

	public void setDbTable(boolean isDbTable) {
		this.isDbTable = isDbTable;
	}

	public List<FieldInfo> getFieldInfos() {
		return fieldInfos;
	}

	public void setFieldInfos(List<FieldInfo> fieldInfos) {
		this.fieldInfos = fieldInfos;
	}

	public Map<String, JunctionTable> getCollectionTableMap() {
		return collectionTableMap;
	}

	public void setCollectionTableMap(Map<String, JunctionTable> collectionTableMap) {
		this.collectionTableMap = collectionTableMap;
	}

	/**
	 * @return the isEntity
	 */
	public boolean isEntity() {
		return isEntity;
	}

	/**
	 * @param isEntity the isEntity to set
	 */
	public void setEntity(boolean isEntity) {
		this.isEntity = isEntity;
	}

	public Set<Class<?>> getAssignedMetaValueBaseClasses() {
		return assignedMetaValueBaseClasses;
	}

	public void setAssignedMetaValueBaseClasses(
			Set<Class<?>> assignedMetaValueBaseClasses) {
		this.assignedMetaValueBaseClasses = assignedMetaValueBaseClasses;
	}

	public String getUpdateSql() {
		return updateSql;
	}

	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	public String getInsertSql() {
		return insertSql;
	}

	public void setInsertSql(String insertSql) {
		this.insertSql = insertSql;
	}

	public JavassistProxyFactory getJavassistProxyFactory() {
		return javassistProxyFactory;
	}

	public void setJavassistProxyFactory(JavassistProxyFactory javassistProxyFactory) {
		this.javassistProxyFactory = javassistProxyFactory;
	}

	public PersistentClass getDbPersistentClass() {
		return dbPersistentClass;
	}

	public void setDbPersistentClass(PersistentClass dbPersistentClass) {
		this.dbPersistentClass = dbPersistentClass;
	}

	public FieldInfo getDiscriminatorFieldInfo() {
		return discriminatorFieldInfo;
	}

	public void setDiscriminatorFieldInfo(FieldInfo discriminatorFieldInfo) {
		this.discriminatorFieldInfo = discriminatorFieldInfo;
	}

	public Map<String,PersistentClass> getPersistentClassFamilyMap() {
		return persistentClassFamilyMap;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public HashMap<FieldInfo, Cascade> getCascadeFieldInfoMap() {
		return cascadeFieldInfoMap;
	}

	public void setCascadeFieldInfoMap(HashMap<FieldInfo, Cascade> cascadeFieldInfoMap) {
		this.cascadeFieldInfoMap = cascadeFieldInfoMap;
	}

	public HashMap<FieldInfo, Cascade> getFullCascadeFieldInfoMap() {
		return fullCascadeFieldInfoMap;
	}

	public void setFullCascadeFieldInfoMap(HashMap<FieldInfo, Cascade> fullCascadeFieldInfoMap) {
		this.fullCascadeFieldInfoMap = fullCascadeFieldInfoMap;
	}

	private List<FieldInfo> getRemoveEmptyFieldInfos() {
		return removeEmptyFieldInfos;
	}

	public Map<String, JunctionTable> getReverseCollectionTableMap() {
		return reverseCollectionTableMap;
	}

	public void setReverseCollectionTableMap(
			Map<String, JunctionTable> reverseCollectionTableMap) {
		this.reverseCollectionTableMap = reverseCollectionTableMap;
	}
}
