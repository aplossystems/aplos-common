package com.aplos.common.backingpage;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.HqlMessageSourceFinder;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=HqlMessageSourceFinder.class)
public class HqlMessageSourceFinderEditPage extends EditPage {
	private static final long serialVersionUID = -6424938076940947155L;
	private List<SelectItem> sourceTypeSelectItems;
	private List<BulkMessageSource> bulkMessageSources;
	
	public HqlMessageSourceFinderEditPage() {
		createSourceTypeSelectItems();
	}
	
	public void testHql() {
		HqlMessageSourceFinder hqlMessageSourceFinder = resolveAssociatedBean();
		setBulkMessageSources(hqlMessageSourceFinder.getBulkMessageSources());
	}
	
	@SuppressWarnings("unchecked")
	public void createSourceTypeSelectItems() {
		HqlMessageSourceFinder hqlMessageSourceFinder = resolveAssociatedBean();
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		if (!hqlMessageSourceFinder.isSmsRequired() && !hqlMessageSourceFinder.isEmailRequired()) {
			selectItems.add(new SelectItem(null,"You must select at least one option (SMS / Email"));
			setSourceTypeSelectItems( selectItems );
			return;
		}
		List<BasicBulkMessageFinder> bulkMessageFinders = null;
		
		if( hqlMessageSourceFinder.isSmsRequired() ) {
			bulkMessageFinders = Website.getCurrentWebsiteFromTabSession().getBulkSmsFinders();
		} else {
			bulkMessageFinders = Website.getCurrentWebsiteFromTabSession().getBulkEmailFinders();	
		} 
		
		boolean existingSourceTypeFound = false;
		if( bulkMessageFinders != null ) {
			selectItems.add( new SelectItem( null, CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText() ));
			for( int i = 0, n = bulkMessageFinders.size(); i < n; i++ ) {
				if( bulkMessageFinders.get( i ).getSourceType().equals(hqlMessageSourceFinder.getSourceType()) ) {
					existingSourceTypeFound = true;
				}
				selectItems.add( new SelectItem( bulkMessageFinders.get( i ).getSourceType().getName(), FormatUtil.breakCamelCase( bulkMessageFinders.get( i ).getBulkMessageFinderClass().getSimpleName() ) ) );
			}
		}

		//if we no longer support the selected source type, change the selection
		if( !existingSourceTypeFound ) {
			hqlMessageSourceFinder.setSourceType( null );
		}
		setSourceTypeSelectItems( selectItems );
	}
	
	public void updateList() {
		createSourceTypeSelectItems();
	}

	public List<SelectItem> getSourceTypeSelectItems() {
		return sourceTypeSelectItems;
	}

	public void setSourceTypeSelectItems(List<SelectItem> sourceTypeSelectItems) {
		this.sourceTypeSelectItems = sourceTypeSelectItems;
	}

	public String getSelectedSourceType() {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		if( bulkEmailSourceGroup.getSourceType() == null ) {
			return null;
		} else {
			return bulkEmailSourceGroup.getSourceType().getName();
		}
	}

	public void setSelectedSourceType(String selectedSourceType) {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		if( selectedSourceType == null ) {
			bulkEmailSourceGroup.setSourceType(null);
		} else {
			try {
				bulkEmailSourceGroup.setSourceType((Class<? extends BulkMessageSource>) Class.forName( selectedSourceType ));
			} catch( ClassNotFoundException cnfEx ) {
				ApplicationUtil.handleError( cnfEx );
			}
		}
	}

	public List<BulkMessageSource> getBulkMessageSources() {
		return bulkMessageSources;
	}

	public void setBulkMessageSources(List<BulkMessageSource> bulkMessageSources) {
		this.bulkMessageSources = bulkMessageSources;
	}
}
