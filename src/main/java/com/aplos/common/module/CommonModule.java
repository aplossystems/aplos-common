package com.aplos.common.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.CommonBundleKey;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.interfaces.BundleKey;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.UserLevelUtil;

public class CommonModule extends AplosModuleImpl {
	private boolean isInternationalizedApplication = false;
	
	@Override
	public List<String> getRestrictedMediaPaths() {
		List<String> restrictedPaths = new ArrayList<String>();
		for (CommonWorkingDirectory awd : CommonWorkingDirectory.values()) {
			if (awd.isRestricted()) {
				restrictedPaths.add(awd.getDirectoryPath(false));
			}
		}
		if (restrictedPaths.size() > 0) {
			return restrictedPaths;
		} else {
			return null;
		}
	}
	
	@Override
	public void clearCache() {
		try {
			FileUtils.cleanDirectory( new File( CommonWorkingDirectory.PROCESSED_RESOURCES_DIR.getDirectoryPath(true) ) );
			FileUtils.cleanDirectory( new File( CommonWorkingDirectory.COMBINED_RESOURCES.getDirectoryPath(true) ) );
			FileUtils.cleanDirectory( new File( CommonWorkingDirectory.MINIFIED_JS.getDirectoryPath(true) ) );
			FileUtils.cleanDirectory( new File( CommonWorkingDirectory.MINIFIED_CSS.getDirectoryPath(true) ) );
		} catch( IOException ioex ) {
			ApplicationUtil.handleError(ioex);
		}
	}
	
	@Override
	public AplosWorkingDirectoryInter[] createWorkingDirectoryEnums() {
		CommonWorkingDirectory.createDirectories( ApplicationUtil.getAplosContextListener().getImplementationModule().getModuleName().replaceAll( " ", "" ) );
		return CommonWorkingDirectory.values();
	}

	@Override
	public UserLevelUtil createUserLevelUtil() {
		return new UserLevelUtil();
	}
	
	@Override
	public void addGlobalAccessPages(List<String> globalAccessPages) {
		globalAccessPages.add("/common/lightBoxImageUploader.jsf");
		globalAccessPages.add("/common/windowIdUpdater.jsf");
		globalAccessPages.add("/common/aplosEmailContent.jsf");
		globalAccessPages.add("/common/emailTemplateContent.jsf");
		globalAccessPages.add("/common/aplosEmailPrintContent.jsf");
		globalAccessPages.add("/common/aplosUrlRewriter.jsf");
	}
	
	@Override
	public CommonDatabaseLoader createDatabaseLoader() {
		return new CommonDatabaseLoader(this);
	}

	@Override
	public ModuleDbConfig createModuleDbConfig() {
		return new CommonModuleDbConfig( this );
	}

	@Override
	public ModuleConfiguration getModuleConfiguration() {
		return CommonConfiguration.getCommonConfiguration();
	}

	@Override
	public void initModule() {
		List<Website> websiteList = ApplicationUtil.getAplosContextListener().getWebsiteList();
		for( Website tempWebsite : websiteList ) {
			tempWebsite.setEmailTemplateMap( CommonConfiguration.getCommonConfiguration().getEmailTemplateMap() );
			tempWebsite.setSmsTemplateMap( CommonConfiguration.getCommonConfiguration().getSmsTemplateMap() );
			tempWebsite.getWebsiteMemory().setPrintTemplateMap( CommonConfiguration.getCommonConfiguration().getPrintTemplateMap() );
			tempWebsite.setBulkMessageFinderMap( CommonConfiguration.getCommonConfiguration().getBulkMessageFinderMap() );
		}

		super.initModule();
	}

	@Override
	public SelectItem[] getCountrySelectItems() {
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		SelectItem[] originalSelectItems = AplosBean.getSelectItemBeans(new BeanDao(Country.class).setIsReturningActiveBeans(true).getAll());
		for( int i = 0, n = originalSelectItems.length; i < n; i++ ) {
			selectItems.add( originalSelectItems[ i ] );
		}

		SelectItem unitedStatesSelectItem = null;
//		SelectItem australiaSelectItem = null;
		SelectItem irelandSelectItem = null;

		SelectItem englandSelectItem = null;
		SelectItem northernIrelandSelectItem = null;
		SelectItem scotlandSelectItem = null;
		SelectItem walesSelectItem = null;

		SelectItem guernseySelectItem = null;
		SelectItem jerseySelectItem = null;
		SelectItem isleOfManSelectItem = null;

		for( int i = 0, n = selectItems.size(); i < n; i++ ) {
			Country tempCountry = (Country) selectItems.get( i ).getValue();
			if( tempCountry.getId().equals( 840l ) ) {
				unitedStatesSelectItem = selectItems.get( i );
//			} else if( tempCountry.getId().equals( 36l ) ) {
//				australiaSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 372l ) ) {
				irelandSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 10002l ) ) {
				englandSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 10001l ) ) {
				northernIrelandSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 10006l ) ) {
				scotlandSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 10007l ) ) {
				walesSelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 831l ) ) {
				guernseySelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 832l ) ) {
				jerseySelectItem = selectItems.get( i );
			} else if( tempCountry.getId().equals( 833l ) ) {
				isleOfManSelectItem = selectItems.get( i );
			}
		}

		selectItems.add( 0, new SelectItem( null, "---" ) );

		selectItems.add( 0, CommonUtil.copySelectItem(englandSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(walesSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(scotlandSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(northernIrelandSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(jerseySelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(guernseySelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(isleOfManSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(irelandSelectItem) );
		selectItems.add( 0, CommonUtil.copySelectItem(unitedStatesSelectItem) );

		selectItems.add( 0, new SelectItem( null, CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText() ) );
		return selectItems.toArray( new SelectItem[ 0 ] );
	}

	@Override
	public void addAvailableBundleKeys(List<BundleKey> availableBundleKeys) {
		availableBundleKeys.addAll( Arrays.asList( CommonBundleKey.values() ) );
	}

	@Override
	public void addImplicitPolymorphismEntries(AplosContextListener aplosContextListener) {
		
	}

	@Override
	public ModuleUpgrader createModuleUpgrader() {
		return new CommonModuleUpgrader(this);
	}

	public void setInternationalizedApplication(
			boolean isInternationalizedApplication) {
		this.isInternationalizedApplication = isInternationalizedApplication;
	}

	public boolean isInternationalizedApplication() {
		return isInternationalizedApplication;
	}

	@Override
	public Currency updateSessionCurrency(HttpServletRequest request) {
		
		return null;
	}
	
}

