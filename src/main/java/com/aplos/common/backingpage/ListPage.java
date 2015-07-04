package com.aplos.common.backingpage;

import java.lang.reflect.Modifier;
import java.util.List;

import com.aplos.common.TrailDisplayName;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

public abstract class ListPage extends BackingPage {

	private static final long serialVersionUID = 4076542286063551147L;
	private DataTableState dataTableState;

	@SuppressWarnings("unchecked")
	public ListPage() {
		super();
		BeanDao listBeanDao = getListBeanDao();
		if( listBeanDao != null ) {
			setBeanDao( listBeanDao );
		}
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		getOrCreateDataTableState();
		return true;
	}

	public DataTableState getOrCreateDataTableState() {
		if ( getDataTableState() == null && getListBeanDao() != null ) {
			setDataTableState( CommonUtil.createDataTableState( this, getClass(), getListBeanDao() ) );
		}
		return getDataTableState();
	}

	public BeanDao getListBeanDao() {
		return getBeanDao();
	}

	@Override
	public Class<? extends BackingPage> getDefaultPreviousPageClass() {
		return getBeanDao().getEditPageClass();
	}

	@Override
	public String getPageHeading() {
		if( getBeanPluralBinding() != null ) {
			return "View " + getBeanPluralBinding();
		} else {
			return FormatUtil.breakCamelCase( getClass().getSimpleName() );
		}
	}

	@Override
	public TrailDisplayName determineTrailDisplayName(String requestUrl) {
		TrailDisplayName trailDisplayName = getTrailDisplayNameFromMenuTab( requestUrl );

		if( trailDisplayName == null ) {
			String beanPluralBinding = getBeanPluralBinding();
			if ( !CommonUtil.isNullOrEmpty( beanPluralBinding ) ) {
				trailDisplayName = new TrailDisplayName();
				trailDisplayName.setPlainText(beanPluralBinding);
				return trailDisplayName;
			}
		}

		if( trailDisplayName == null ) {
			trailDisplayName = getTrailDisplayNameFromClassName();
		}

		return trailDisplayName;
	}

	public void emailSelectedBeans() {
		if ( !emailBeans( getDataTableState().getLazyDataModel().getSelectedRealBeans() ) ) {
			JSFUtil.addMessageForError("Please select the " + FormatUtil.breakCamelCase( getBeanPluralBinding() ) + " you wish to message.");
		}
	}

	public void emailFilteredBeans() {
		if ( !emailBeans( getDataTableState().getLazyDataModel().getFilteredRealBeans() ) ) {
			JSFUtil.addMessageForError("Please select the " + FormatUtil.breakCamelCase( getBeanPluralBinding() ) + " you wish to message.");
		}
	}
	
	public boolean emailBeans( List<AplosBean> beanList ) {
		return emailBeans( beanList, CommonEmailTemplateEnum.GENERAL_EMAIL );
	}
	
	public boolean emailBeans( List<AplosBean> beanList, EmailTemplateEnum emailTemplateEnum ) {
		if( beanList.size() > 0 ) {
			BulkMessageSourceGroup besGroup = new BulkMessageSourceGroup();
			besGroup.setName("Selected " + FormatUtil.breakCamelCase( getBeanPluralBinding() ) );
			besGroup.setSourceType( (Class<? extends BulkMessageSource>) getBeanDao().getBeanClass());
			for (int i=0, n = beanList.size(); i < n; i++) {
				besGroup.getBulkMessageSources().add((BulkEmailSource)beanList.get(i));
			}
	
			besGroup.saveDetails();
			AplosEmail aplosEmail = new AplosEmail( emailTemplateEnum, besGroup );
			aplosEmail.setUsingEmailSourceAsOwner(true);
			aplosEmail.redirectToEditPage();
			return true;
		} 
		return false;
	}

	public String getBeanPluralBinding() {
		if ( resolveAssociatedBean() != null ) {
			return resolveAssociatedBean().getPluralDisplayName();
		} else if ( getDataTableState() != null && getDataTableState().getLazyDataModel().getAssociatedBeanFromScope() != null ) {
			return getDataTableState().getLazyDataModel().getAssociatedBeanFromScope().getPluralDisplayName();
		} else if ( getBeanDao() != null && !Modifier.isAbstract(getBeanDao().getBeanClass().getModifiers()) ) {
			return getBeanDao().getNew().getPluralDisplayName();
		} else {
			return "";
		}
	}

	public DataTableState getDataTableState() {
		return dataTableState;
	}

	public void setDataTableState(DataTableState dataTableState) {
		this.dataTableState = dataTableState;
	}

}
