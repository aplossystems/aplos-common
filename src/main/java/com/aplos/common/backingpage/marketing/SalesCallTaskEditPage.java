package com.aplos.common.backingpage.marketing;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.listbeans.PotentialCompanyListBean;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.SalesCallTask;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.UserLevelUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SalesCallTask.class)
public class SalesCallTaskEditPage extends EditPage {
	private static final long serialVersionUID = -1305174780275645363L;
	private DataTableState unfinishedPotentialCompanyDts;
	private BeanDao unfinishedPotentialCompanyDao;
	private static final String UNFINISHED_POTENTIAL_COMPANY_DTS = "unfinishedPotentialCompanyDts";
	private DataTableState finishedPotentialCompanyDts;
	private BeanDao finishedPotentialCompanyDao;
	private static final String FINISHED_POTENTIAL_COMPANY_DTS = "finishedPotentialCompanyDts";
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		if( continueLoad ) {
			if( getUnfinishedPotentialCompanyDao() == null ) {
				SalesCallTask salesCallTask = resolveAssociatedBean();
				setUnfinishedPotentialCompanyDao( new BeanDao( PotentialCompany.class ) );
				getUnfinishedPotentialCompanyDao().setSelectCriteria( "bean.id, bean.address.companyName, bean.mainCategory, bean.address, bean.potentialCompanyStatus, bean.lastContactedDate, bean.reminderDate, (SELECT count(*) FROM PotentialCompanyInteraction pci WHERE pci.potentialCompany_id = bean.id) as numberOfInteractions" );
				getUnfinishedPotentialCompanyDao().setListBeanClass( PotentialCompanyListBean.class );
				getUnfinishedPotentialCompanyDao().addReverseJoinTable( SalesCallTask.class, "sct.unfinishedPotentialCompanyList", "bean" );
				getUnfinishedPotentialCompanyDao().addWhereCriteria( "sct.id = " + salesCallTask.getId() );

				setFinishedPotentialCompanyDao( new BeanDao( PotentialCompany.class ) );
				getFinishedPotentialCompanyDao().setSelectCriteria( "bean.id, bean.address.companyName, bean.mainCategory, bean.address, bean.potentialCompanyStatus, bean.lastContactedDate, bean.reminderDate, (SELECT count(*) FROM PotentialCompanyInteraction pci WHERE pci.potentialCompany_id = bean.id) as numberOfInteractions" );
				getFinishedPotentialCompanyDao().setListBeanClass( PotentialCompanyListBean.class );
				getFinishedPotentialCompanyDao().addReverseJoinTable( SalesCallTask.class, "sct.finishedPotentialCompanyList", "bean" );
				getFinishedPotentialCompanyDao().addWhereCriteria( "sct.id = " + salesCallTask.getId() );
			}
			getOrCreateDataTableState();
		}
		return continueLoad;
	}

	public DataTableState getOrCreateDataTableState() {
		if ( getUnfinishedPotentialCompanyDts() == null && getUnfinishedPotentialCompanyDao() != null ) {
			setUnfinishedPotentialCompanyDts( CommonUtil.createDataTableState( this, getClass(), UNFINISHED_POTENTIAL_COMPANY_DTS, getUnfinishedPotentialCompanyDao() ) );
		}
		if ( getFinishedPotentialCompanyDts() == null && getFinishedPotentialCompanyDao() != null ) {
			setFinishedPotentialCompanyDts( CommonUtil.createDataTableState( this, getClass(), FINISHED_POTENTIAL_COMPANY_DTS, getFinishedPotentialCompanyDao() ) );
		}
		return getUnfinishedPotentialCompanyDts();
	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel( DataTableState dataTableState, BeanDao aqlBeanDao) {
		if( UNFINISHED_POTENTIAL_COMPANY_DTS.equals( dataTableState.getTableIdentifier() ) ) {
			return new UnfinishedPotentialCompanyLdm(dataTableState, aqlBeanDao);
		} else if( FINISHED_POTENTIAL_COMPANY_DTS.equals( dataTableState.getTableIdentifier() ) ) {
			return new FinishedPotentialCompanyLdm(dataTableState, aqlBeanDao);
		}
		return null;
	}
	
	public SelectItem[] getSystemUserSelectItems() {
		BeanDao systemUserDao = new BeanDao( SystemUser.class );
		systemUserDao.setSelectCriteria( "DISTINCT(bean)" );
		systemUserDao.addQueryTable( "additionalUserLevel", "bean.additionalUserLevels" );
		UserLevelUtil userLevelUtil = CommonConfiguration.getCommonConfiguration().getUserLevelUtil();
		systemUserDao.addWhereCriteria( "bean.userLevel.id = " + userLevelUtil.getMarketerLevel().getId() + " OR additionalUserLevel.id = " + userLevelUtil.getMarketerLevel().getId() );
		List<SystemUser> systemUserList = systemUserDao.getAll();
		return AplosBean.getSelectItemBeans( systemUserList );
	}

	public DataTableState getUnfinishedPotentialCompanyDts() {
		return unfinishedPotentialCompanyDts;
	}

	public void setUnfinishedPotentialCompanyDts(DataTableState unfinishedPotentialCompanyDts) {
		this.unfinishedPotentialCompanyDts = unfinishedPotentialCompanyDts;
	}

	public BeanDao getUnfinishedPotentialCompanyDao() {
		return unfinishedPotentialCompanyDao;
	}

	public void setUnfinishedPotentialCompanyDao(BeanDao unfinishedPotentialCompanyDao) {
		this.unfinishedPotentialCompanyDao = unfinishedPotentialCompanyDao;
	}

	public DataTableState getFinishedPotentialCompanyDts() {
		return finishedPotentialCompanyDts;
	}

	public void setFinishedPotentialCompanyDts(
			DataTableState finishedPotentialCompanyDts) {
		this.finishedPotentialCompanyDts = finishedPotentialCompanyDts;
	}

	public BeanDao getFinishedPotentialCompanyDao() {
		return finishedPotentialCompanyDao;
	}

	public void setFinishedPotentialCompanyDao(
			BeanDao finishedPotentialCompanyDao) {
		this.finishedPotentialCompanyDao = finishedPotentialCompanyDao;
	}

	public static class FinishedPotentialCompanyLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = -1985029998964369309L;

		public FinishedPotentialCompanyLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super( dataTableState, aqlBeanDao );
			dataTableState.setShowingDeleteColumn(false);
			dataTableState.setShowingNewBtn(false);
			dataTableState.setShowingShowDeletedBtn(false);
		}
	}

	public static class UnfinishedPotentialCompanyLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = -6645487614756095122L;

		public UnfinishedPotentialCompanyLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super( dataTableState, aqlBeanDao );
			dataTableState.setShowingDeleteColumn(false);
			dataTableState.setShowingNewBtn(false);
			dataTableState.setShowingShowDeletedBtn(false);
		}
	}
}
