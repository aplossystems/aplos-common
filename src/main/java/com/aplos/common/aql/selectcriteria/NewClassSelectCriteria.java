package com.aplos.common.aql.selectcriteria;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.utils.ApplicationUtil;


public class NewClassSelectCriteria extends SelectCriteria {
	private Class<?> instantiableClass;
	private List<SelectCriteria> unprocessedSelectCriteria = new ArrayList<SelectCriteria>();
	private List<SelectCriteria> processedSelectCriteria = new ArrayList<SelectCriteria>();

	public NewClassSelectCriteria( Class<?> instantiableClass ) {
		setInstantiableClass(instantiableClass);
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao,
			boolean isAllowingFullTables) {
		SelectCriteria tempSelectCriteria;
		for( int i = getUnprocessedSelectCriteria().size() - 1; i > -1; i-- ) {
			tempSelectCriteria = getUnprocessedSelectCriteria().get( i );
			tempSelectCriteria.getAqlVariable().evaluateCriteriaTypes( processedBeanDao, true );
			getProcessedSelectCriteria().add( 0, tempSelectCriteria );
		}
		return true;
	}
    
	@Override
    public void addSelectCriterias( List<SelectCriteria> selectCriterias ) {
    	for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
    		getProcessedSelectCriteria().get( i ).addSelectCriterias(selectCriterias);
    	}
    }
	
	@Override
	public Object convertFieldValues( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException {
		Object tempBaseBean = null;
    	try {
    		Class[] constructorClasses = new Class[ getProcessedSelectCriteria().size() ];
    		Object[] arguments = new Object[ getProcessedSelectCriteria().size() ];
    		AqlVariable tempAqlVariable;
    		for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
    			tempAqlVariable = getProcessedSelectCriteria().get( i ).getAqlVariable();
				constructorClasses[ i ] = tempAqlVariable.getApplicationType().getJavaClass();
				arguments[ i ] = getProcessedSelectCriteria().get( i ).convertFieldValues( processedBeanDao, rowDataReceiver );
				if( tempAqlVariable instanceof AqlTableVariable 
						&& ((AqlTableVariable) tempAqlVariable).getFullTableFieldInfo() != null ) {
					PersistentClassFieldInfo persistentClassFieldInfo = (PersistentClassFieldInfo) ((AqlTableVariable) tempAqlVariable).getFullTableFieldInfo();
					constructorClasses[ i ] = persistentClassFieldInfo.getPersistentClass().getTableClass();
					arguments[ i ] = persistentClassFieldInfo.getAplosAbstractBean( true, persistentClassFieldInfo.getPersistentClass(), arguments[ i ] );
				}
	    	}
    		
    		tempBaseBean = getInstantiableClass().getConstructor( constructorClasses ).newInstance( arguments );
	    	
    	} catch( IllegalAccessException iaex ) {
			ApplicationUtil.handleError( iaex );
    	} catch( InstantiationException iex ) {
			ApplicationUtil.handleError( iex );
    	} catch( NoSuchMethodException iex ) {
			ApplicationUtil.handleError( iex );
    	} catch( InvocationTargetException iex ) {
			ApplicationUtil.handleError( iex );
    	}
    	return tempBaseBean;
	}

	public Class<?> getInstantiableClass() {
		return instantiableClass;
	}

	public void setInstantiableClass(Class<?> instantiableClass) {
		this.instantiableClass = instantiableClass;
	}

	public List<SelectCriteria> getUnprocessedSelectCriteria() {
		return unprocessedSelectCriteria;
	}

	public void setUnprocessedSelectCriteria(
			List<SelectCriteria> unprocessedSelectCriteria) {
		this.unprocessedSelectCriteria = unprocessedSelectCriteria;
	}

	public List<SelectCriteria> getProcessedSelectCriteria() {
		return processedSelectCriteria;
	}

	public void setProcessedSelectCriteria(List<SelectCriteria> processedSelectCriteria) {
		this.processedSelectCriteria = processedSelectCriteria;
	}
}
