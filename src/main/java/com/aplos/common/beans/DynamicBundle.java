package com.aplos.common.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aplos.common.SiteBeanDao;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;

public class DynamicBundle {
	private Map<String,DynamicBundleEntry> dynamicBundleEntryMap;
	private Website website;

	public DynamicBundle( Website website ) {
		this.website = website;
	}

	public void checkPersistentApplication( AplosContextListener aplosContextListener ) {
		boolean keyAdded = false;
		for( PersistentClass tempPersistentClass : ApplicationUtil.getAplosContextListener().getPersistentApplication().getPersistentClassMap().values() ) {
			keyAdded = checkAndAddBeanClassTranslation( tempPersistentClass ) || keyAdded;
			keyAdded = checkAndAddPluralTranslation( tempPersistentClass ) || keyAdded;
		}

		if( keyAdded ) {
			refreshDynamicBundleEntryMap(aplosContextListener);
		}
	}

	public boolean checkAndAddBeanClassTranslation( PersistentClass tempPersistentClass ) {
		String entryKey = tempPersistentClass.getTableClass().getName().replace( "com.aplos.", "" );
		if( !getDynamicBundleEntryMap().containsKey( entryKey ) ) {

			String entryValue = FormatUtil.breakCamelCase( (tempPersistentClass.getTableClass().getSimpleName() ) ).toLowerCase();

			DynamicBundleEntry  tempDynamicBundleEntry = new DynamicBundleEntry();
			tempDynamicBundleEntry.setDeletable( false );
			tempDynamicBundleEntry.setParentWebsite( website );
			tempDynamicBundleEntry.setEntryKey( entryKey );
			tempDynamicBundleEntry.setEntryValue( entryValue );
			tempDynamicBundleEntry.saveDetails( (SystemUser) null);
			return true;
		}
		return false;
	}

	public boolean checkAndAddPluralTranslation( PersistentClass tempPersistentClass ) {
		String entryKey = tempPersistentClass.getTableClass().getName().replace( "com.aplos.", "" ) + "_PLURAL";
		if( !getDynamicBundleEntryMap().containsKey( entryKey ) ) {
			PluralDisplayName pluralBinding = tempPersistentClass.getTableClass().getAnnotation( PluralDisplayName.class );
			String entryValue;
			if( pluralBinding == null ) {
				entryValue = FormatUtil.breakCamelCase( (tempPersistentClass.getTableClass().getSimpleName() + "s") ).toLowerCase();
			} else {
				entryValue = pluralBinding.name();
			}

			DynamicBundleEntry  tempDynamicBundleEntry = new DynamicBundleEntry();
			tempDynamicBundleEntry.setDeletable( false );
			tempDynamicBundleEntry.setParentWebsite( website );
			tempDynamicBundleEntry.setEntryKey( entryKey );
			tempDynamicBundleEntry.setEntryValue( entryValue );
			tempDynamicBundleEntry.saveDetails((SystemUser) null);
			return true;
		}
		return false;
	}

	public void checkBundleKeys( AplosContextListener aplosContextListener, List<BundleKey> availableBundleKeys ) {
		DynamicBundleEntry tempDynamicBundleEntry;
		boolean keyAdded = false;
		for( BundleKey tempBundleKey : availableBundleKeys ) {
			if( !getDynamicBundleEntryMap().containsKey( tempBundleKey.getName() ) ) {
				tempDynamicBundleEntry = new DynamicBundleEntry();
				tempDynamicBundleEntry.setDeletable( false );
				tempDynamicBundleEntry.setParentWebsite( website );
				tempDynamicBundleEntry.setEntryKey( tempBundleKey.getName() );
				tempDynamicBundleEntry.setEntryValue( tempBundleKey.getDefaultValue() );
				tempDynamicBundleEntry.saveDetails((SystemUser) null);
				keyAdded = true;
			}
		}

		if( keyAdded ) {
			refreshDynamicBundleEntryMap(aplosContextListener);
		}
	}

	public String translate( String bundleEntryKey, Locale locale ) {
		DynamicBundleEntry dynamicBundleEntry = getDynamicBundleEntryMap().get( bundleEntryKey );
		if( dynamicBundleEntry != null ) {
			return dynamicBundleEntry.getEntryValue( locale.getLanguage() );
		} else {
			return null;
		}
	}

	public void refreshDynamicBundleEntryMap() {
		refreshDynamicBundleEntryMap( ApplicationUtil.getAplosContextListener() );
	}

	public void refreshDynamicBundleEntryMap( AplosContextListener aplosContextListener ) {
		/*
		 * It's required to send in the aplosContextListener as this is called from start up
		 */
		SiteBeanDao bundleEntryDao = new SiteBeanDao( aplosContextListener, website, DynamicBundleEntry.class );
		List<DynamicBundleEntry> dynamicBundleEntries = bundleEntryDao.getAll();
		dynamicBundleEntryMap = new HashMap<String,DynamicBundleEntry>();
		for( DynamicBundleEntry tempDynamicBundleEntry : dynamicBundleEntries ) {
			getDynamicBundleEntryMap().put( tempDynamicBundleEntry.getEntryKey(), tempDynamicBundleEntry );
		}
	}

	public void setDynamicBundleEntryMap(HashMap<String,DynamicBundleEntry> dynamicBundleEntryMap) {
		this.dynamicBundleEntryMap = dynamicBundleEntryMap;
	}

	public Map<String,DynamicBundleEntry> getDynamicBundleEntryMap() {
		return dynamicBundleEntryMap;
	}
}
