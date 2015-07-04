package com.aplos.common.tabpanels;

import java.io.ObjectStreamClass;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ConversionUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TabClass extends AplosBean { // so we can deactivate it

	private static final long serialVersionUID = -5137170002260174082L;
	private static Logger logger = Logger.getLogger( TabClass.class );
	private Class<? extends BackingPage> backingPageClass;
	// this says whether we are letting the customer have this backingpage type
	private boolean isPaidForAndAccessible = false;
	private Long classSerialUID;

	public TabClass() { /* for hibernate */ }

	public TabClass( Class<? extends BackingPage> backingPageClass ) {
		this.backingPageClass = backingPageClass;
		Long classUID = ObjectStreamClass.lookup(backingPageClass).getSerialVersionUID();
		if (classUID != null) {
			 this.classSerialUID = classUID;
		}
	}

	public TabClass( Class<? extends BackingPage> backingPageClass, boolean isPaidForAndAccessible ) {
		this.backingPageClass = backingPageClass;
		this.isPaidForAndAccessible = isPaidForAndAccessible;
		Long classUID = ObjectStreamClass.lookup(backingPageClass).getSerialVersionUID();
		if (classUID != null) {
			 this.classSerialUID = classUID;
		}
	}

	@Override
	public String getDisplayName() {
		if (backingPageClass != null) {
			//com.aplos.cms.backingpage.someclass
			String name = backingPageClass.getName();
			int backingPageCutoff = name.lastIndexOf(".backingpage")-1;
			return "[" + name.substring(name.lastIndexOf(".", backingPageCutoff)+1,backingPageCutoff+1) + "] " + FormatUtil.breakCamelCase(name.substring(name.lastIndexOf(".")+1));
		} else {
			return "[None] Empty Dynamic Tab Class";
		}
	}

	public void setBackingPageClass(Class<? extends BackingPage> backingPageClass) {
		this.backingPageClass = backingPageClass;
	}

	public Class<? extends BackingPage> getBackingPageClass() {
		return backingPageClass;
	}

	public static boolean checkSerialUIDUniqueness(Long classUID, Long tabClassId) {
		BeanDao classDao = new BeanDao(TabClass.class);
		classDao.setWhereCriteria("bean.classSerialUID=" + classUID);
		if (tabClassId != null) {
			classDao.addWhereCriteria("bean.id != " + tabClassId);
		}
		classDao.setIsReturningActiveBeans(null);
		int count = classDao.getCountAll();
		if (count > 0) {
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public static void propogateClassChangesToDb(List<Class<? extends BackingPage>> allModuleBackingPageClasses) {

		//TODO: would it be faster to pull out all tab classes first?

		List<Object[]> tabClassObjects = ApplicationUtil.getResults("SELECT id, backingPageClass, classSerialUID, active FROM " + AplosBean.getTableName( TabClass.class ) );
		Map<String, Object[]> tabClassByBackingPageMap = new HashMap<String, Object[]>();
		Map<Long, Object[]> tabClassBySerialUidMap = new HashMap<Long, Object[]>();
		final int TAB_CLASS_ID = 0;
		final int TAB_CLASS_BACKING_PAGE_CLASS = 1;
		final int TAB_CLASS_SERIAL_UID = 2;
		final int TAB_CLASS_ACTIVE = 3;
		Object[] previousValue;
		for( Object tabClassProperties[] : tabClassObjects ) {
			previousValue = tabClassByBackingPageMap.put( (String) tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ], tabClassProperties );
			if( previousValue != null ) {
				StringBuffer namesBuffer = new StringBuffer();
				namesBuffer.append("(");
				namesBuffer.append(previousValue[ TAB_CLASS_ID ]);
				namesBuffer.append(")");
				namesBuffer.append(previousValue[ TAB_CLASS_BACKING_PAGE_CLASS ]);
				namesBuffer.append(", ");
				namesBuffer.append("(");
				namesBuffer.append(tabClassProperties[ TAB_CLASS_ID ]);
				namesBuffer.append(")");
				namesBuffer.append(tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ]);
				throw new RuntimeException(namesBuffer.toString() + " declare the same backing page class.");
			}
			if( tabClassProperties[ TAB_CLASS_SERIAL_UID ] != null ) {
				previousValue = tabClassBySerialUidMap.put( ConversionUtil.convertToLong( tabClassProperties[ TAB_CLASS_SERIAL_UID ] ), tabClassProperties );
				if( previousValue != null ) {
					StringBuffer namesBuffer = new StringBuffer();
					namesBuffer.append("(");
					namesBuffer.append(previousValue[ TAB_CLASS_ID ]);
					namesBuffer.append(")");
					namesBuffer.append(previousValue[ TAB_CLASS_BACKING_PAGE_CLASS ]);
					namesBuffer.append(", ");
					namesBuffer.append("(");
					namesBuffer.append(tabClassProperties[ TAB_CLASS_ID ]);
					namesBuffer.append(")");
					namesBuffer.append(tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ]);
					throw new RuntimeException(namesBuffer.toString() + " share the SerialVersionUID '" + tabClassProperties[ TAB_CLASS_SERIAL_UID ] + "'. SerialVersionUID must be unique.");
				}
			}
		}

		Map<Long, Class<? extends BackingPage>> serialUidCheckMap = new HashMap<Long, Class<? extends BackingPage>>();
		Class<? extends BackingPage> previousBackingPageClass;
		for (Class<? extends BackingPage> backingPageClass : allModuleBackingPageClasses) {
			Long classUID = ObjectStreamClass.lookup(backingPageClass).getSerialVersionUID();
			previousBackingPageClass = serialUidCheckMap.put( classUID, backingPageClass );
			if( previousBackingPageClass != null ) {
				StringBuffer namesBuffer = new StringBuffer();
				namesBuffer.append(previousBackingPageClass.getName());
				namesBuffer.append(", ");
				namesBuffer.append(backingPageClass.getName());
				throw new RuntimeException(namesBuffer.toString() + " share the SerialVersionUID '" + classUID + "'. SerialVersionUID must be unique.");
			}
		}
		
		List<Class<? extends BackingPage>> newBackingPageClasses = new ArrayList<Class<? extends BackingPage>>();
		int backingPageChanges = 0;
		int hashCodeChanges = 0;
		int newTabClasses = 0;
		Object tabClassProperties[];
		List<Long> activeTabClasses = new ArrayList<Long>();
		Long tempTabClassSerialUid;
		Long tempTabClassId;
		Long tempClassUID;
		for (Class<? extends BackingPage> backingPageClass : allModuleBackingPageClasses) {
			try {
				if (Modifier.isAbstract( backingPageClass.getModifiers() )) {
					continue;
				}
				
				tempClassUID = ObjectStreamClass.lookup(backingPageClass).getSerialVersionUID();
				if (tempClassUID != null) { 
					/**
					 * Search by backing page BEFORE we search by UID otherwise we can't distinguish
					 * between the class name changing and an accidental duplicate UID
					 */
				
					tabClassProperties = tabClassByBackingPageMap.get( backingPageClass.getName() );
					if( tabClassProperties == null ) {
						tabClassProperties = tabClassBySerialUidMap.get( tempClassUID );
						// Set the backingPageClass to null so that it's reset later on
						if( tabClassProperties != null && tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ] != null ) {
							try {
								Class tabBackingPageClass = Class.forName( (String) tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ] );
								if( allModuleBackingPageClasses.contains( tabBackingPageClass ) ) {
									throw new Exception(tabBackingPageClass.getName() + " and " + backingPageClass.getName() + " share the SerialVersionUID '" + tempClassUID + "'. SerialVersionUID must be unique.");
								}
							} catch( ClassNotFoundException cnfex ) {
								tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ] = null;	
							}
						}
					}
					if (tabClassProperties != null) {
						//make sure UIDs match (they can never change)
						tempTabClassSerialUid = null;
						if( tabClassProperties[ TAB_CLASS_SERIAL_UID ] != null ) {
							tempTabClassSerialUid = ConversionUtil.convertToLong( tabClassProperties[ TAB_CLASS_SERIAL_UID ] );
						}
						String tabClassBackingPageClass = String.valueOf( tabClassProperties[ TAB_CLASS_BACKING_PAGE_CLASS ] );
						tempTabClassId = null;
						if( tabClassProperties[ TAB_CLASS_ID ] != null ) {
							tempTabClassId = ConversionUtil.convertToLong( tabClassProperties[ TAB_CLASS_ID ] );
						}
						if (tempTabClassSerialUid != null && !tempTabClassSerialUid.equals(tempClassUID)) {
							ApplicationUtil.executeSql( "UPDATE tabClass SET classSerialUID = " + tempClassUID + " WHERE backingPageClass = '" + tabClassBackingPageClass + "'" );
							/*
							 * Just update the row, I can't see why we would need to throw the exception below.
							 */
//							throw new Exception("SerialVersionUID mismatch! Stored UID does not equal current UID for " + tabClassBackingPageClass);
						} else if (tempTabClassSerialUid == null) {
							hashCodeChanges++;
							if (checkSerialUIDUniqueness(tempClassUID, tempTabClassId)) {
								BeanDao existingTabClassDao = new BeanDao(TabClass.class);
								existingTabClassDao.setWhereCriteria("bean.id = " + tempTabClassId);
								existingTabClassDao.setIsReturningActiveBeans(null);
								TabClass existingTabClass = existingTabClassDao.getFirstBeanResult();
								existingTabClass.setClassSerialUID(tempClassUID);
								existingTabClass.saveDetails();
							} else {
								BeanDao existingTabClassDao = new BeanDao(TabClass.class);
								existingTabClassDao.setWhereCriteria("bean.classSerialUID=" + tempClassUID);
								existingTabClassDao.addWhereCriteria("bean.id != " + tempTabClassId);
								existingTabClassDao.setMaxResults(1);
								existingTabClassDao.setIsReturningActiveBeans(null);
								TabClass existingTabClass = existingTabClassDao.getFirstBeanResult();
								throw new Exception("Could not update " + backingPageClass.getName() + " tab class. Its SerialVersionUID '" + tempClassUID + "' matches an existing tab class '" + existingTabClass.getDisplayName() + "'");
							}
						}
						
						if (tabClassBackingPageClass == null || !tabClassBackingPageClass.equals(backingPageClass.getName())) {
							backingPageChanges++;

							ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName( TabClass.class ) + " bean SET backingPageClass = '" + backingPageClass.getName() + "' WHERE bean.id = " + tempTabClassId );
						}
						
						activeTabClasses.add( tempTabClassId );
					} else {
						newBackingPageClasses.add(backingPageClass);
					}
				} else {
					throw new Exception("SerialVersionUID has not been declared for " + backingPageClass.getName());
				}
			} catch (Exception ex) {
				ApplicationUtil.getAplosContextListener().handleError( null, null, ex, "startup", false );
			}
		}
		
//		HibernateUtil.startNewTransaction();  // This takes a long time due to updates before this method.
		List<Long> newlyActiveTabClasses = new ArrayList<Long>();
		List<Long> newlyInActiveTabClasses = new ArrayList<Long>();
		for( Object tempTabClassProperties[] : tabClassObjects ) {
			tempTabClassId = ConversionUtil.convertToLong( tempTabClassProperties[ TAB_CLASS_ID ] );
			if( activeTabClasses.contains( tempTabClassId ) ) {
				if( !((Boolean) tempTabClassProperties[ TAB_CLASS_ACTIVE ]) ) {
					newlyActiveTabClasses.add( tempTabClassId );
				}
			} else {
				if( ((Boolean) tempTabClassProperties[ TAB_CLASS_ACTIVE ]) ) {
					newlyInActiveTabClasses.add( tempTabClassId );
				}
			}
		}
		
		if( newlyInActiveTabClasses.size() > 0 ) {
			ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName( TabClass.class ) + " SET active = false WHERE id IN ( " + StringUtils.join( newlyInActiveTabClasses, "," ) + ")" );	
		}
		if( newlyActiveTabClasses.size() > 0 ) {
			ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName( TabClass.class ) + " SET active = true WHERE id IN ( " + StringUtils.join( newlyActiveTabClasses, "," ) + ")" );
		}

//		HibernateUtil.startNewTransaction();
//		activeTabClasses.add( -1l );
//		HibernateUtil.getCurrentSession().createSQLQuery("UPDATE " + AplosBean.getTableName( TabClass.class ) + " SET active = false WHERE id NOT IN ( " + StringUtils.join( activeTabClasses, "," ) + ")" ).executeUpdate();
//		HibernateUtil.getCurrentSession().createSQLQuery("UPDATE " + AplosBean.getTableName( TabClass.class ) + " SET active = true WHERE id IN ( " + StringUtils.join( activeTabClasses, "," ) + ")" ).executeUpdate();
		
		for (Class<? extends BackingPage> newBackingPageClass : newBackingPageClasses) {
			try {
				//despite the loop above we have to recheck uniqueness in case
				//a new hash was added as part of that loop which now makes this non-unique
				Long classUID = ObjectStreamClass.lookup(newBackingPageClass).getSerialVersionUID();
				if (classUID != null) {
					//AplosTabClassHash aplosTabClassHash = newBackingPageClass.getAnnotation(AplosTabClassHash.class);
					if (checkSerialUIDUniqueness(classUID, null)) {
						TabClass newPropogation = new TabClass(newBackingPageClass);
						newPropogation.saveDetails((SystemUser) null);
						newTabClasses++;
					} else {
						throw new Exception(newBackingPageClass.getName() + " could not be added as a tab class. Its SerialVersionUID (" + classUID + ") is not unique!");
					}
				} else {
					throw new Exception("SerialVersionUID has not been declared for " + newBackingPageClass.getName());
				}
			} catch (Exception ex) {
				ApplicationUtil.getAplosContextListener().handleError( null, null, ex, "startup", false );
			}
		}


		logger.info("** Tab Class Updates complete. " + newTabClasses + " new tab classes. " + backingPageChanges + " class updates. " + hashCodeChanges + " SerialVersionUID updates.");
		
		//now tab classes have been set, update the menu structure so we dont try to load something thats broken (ie remove references to inactive or missign rows)
		ApplicationUtil.executeSql( "DELETE FROM menutab_defaultpagebindings WHERE defaultPageBindings_id NOT IN (SELECT id FROM tabclass WHERE active=1)");
//		HibernateUtil.getCurrentSession().createSQLQuery( "UPDATE tabpanel SET associatedBackingPage_id=null WHERE associatedBackingPage_id NOT IN (SELECT id FROM tabclass WHERE active=1)").executeUpdate();
		ApplicationUtil.executeSql( "UPDATE menutab SET tabActionClass_id=null WHERE tabActionClass_id NOT IN (SELECT id FROM tabclass WHERE active=1)");
		ApplicationUtil.executeSql( "UPDATE tabclass SET isPaidForAndAccessible=0 WHERE active=0");
	}

	//######### These methods find our classes for the db, adapted from http://snippets.dzone.com/posts/show/4831

    public static TabClass get(Class<? extends BackingPage> bpClass) {
    	TabClass tabClass = null;
    	BeanDao tabDao = new BeanDao(TabClass.class);
		tabDao.setWhereCriteria("bean.backingPageClass='" + bpClass.getName() + "'");

		tabClass = tabDao.getFirstBeanResult();
		if (tabClass == null) {
			tabClass = new TabClass(bpClass);
			tabClass.saveDetails();
		}
		return tabClass;
    }

    //########

	public void setPaidForAndAccessible(boolean isPaidForAndAccessible) {
		this.isPaidForAndAccessible = isPaidForAndAccessible;
	}

	public boolean isPaidForAndAccessible() {
		return isPaidForAndAccessible;
	}

	public Long getClassSerialUID() {
		return classSerialUID;
	}

	public void setClassSerialUID(Long classUID) {
		this.classSerialUID = classUID;
	}

}
