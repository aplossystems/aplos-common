package com.aplos.common.beans;

import java.util.HashSet;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
public class DataTransferManager {
	private Set<Class<?>> allAvailableClasses = new HashSet<Class<?>>();
	private Set<Class<?>> transferAllClasses = new HashSet<Class<?>>();

	public DataTransferManager() {
//		Map allClassMetaData = HibernateTransferUtil.getSessionFactory().getAllClassMetadata();
////		HibernateTransferUtil.getSessionFactory().getAllCollectionMetadata()
//		Iterator iter = allClassMetaData.keySet().iterator();
//		String currClassName;
//		while( iter.hasNext() ) {
//			try {
//				currClassName = (String) iter.next();
//				allAvailableClasses.add( Class.forName( currClassName ) );
//			} catch ( ClassNotFoundException cnfEx ) {
//				cnfEx.printStackTrace();
//			}
//		}
//		resetTransferAllClasses();
	}

	public static DataTransferManager getFromSession() {
		return (DataTransferManager) JSFUtil.resolveVariable( CommonUtil.getBinding( DataTransferManager.class ) );
	}


	public void resetTransferAllClasses() {
		this.transferAllClasses = new HashSet( allAvailableClasses );
	}

	public void setTransferAllClasses(Set<Class<?>> transferAllClasses) {
		this.transferAllClasses = transferAllClasses;
	}

	public Set<Class<?>> getTransferAllClasses() {
		return transferAllClasses;
	}
}
