package com.aplos.common.backingpage;

import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.communication.AplosEmailEditPage;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SystemUser.class)
public class SystemUserListPage extends ListPage {
	private static final long serialVersionUID = -6335874292448225935L;

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new UserListLazyDataModel(dataTableState, aqlBeanDao);
	}
	
	public SelectItem[] getUserLevelSelectItems() {
		BeanDao userLevelDao = new BeanDao( UserLevel.class );
		userLevelDao.addWhereCriteria( "bean.clearanceLevel >= " + JSFUtil.getLoggedInUser().getUserLevel().getClearanceLevel() );
		List<UserLevel> userLevels = userLevelDao.getAll();
		SelectItem[] userLevelSelectItems = new SelectItem[ userLevels.size() + 1 ];
		userLevelSelectItems[ 0 ] = new SelectItem( "", CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText() );
		for( int i = 0, n = userLevels.size(); i < n; i++ ) {
			userLevelSelectItems[ i + 1 ] = new SelectItem( userLevels.get( i ).getId(), userLevels.get( i ).getName() );
		}
		return userLevelSelectItems;
	}
	
	public void emailSystemUser( SystemUser systemUser ) {
		AplosEmail aplosEmail = new AplosEmail( CommonEmailTemplateEnum.GENERAL_EMAIL, systemUser, systemUser );
		aplosEmail.redirectToEditPage();
	}
	
	public boolean isSendEmailAllowed() {
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if( currentUser != null ) {
			return currentUser.getUserLevel().checkAccess( AplosEmailEditPage.class );
		}
		return false;
	}

	public String resetAccount() {
		SystemUser user = (SystemUser) JSFUtil.getRequest().getAttribute("tableBean");
		user = user.getSaveableBean();
		user.resetFailedLoginAttempts();
		user.setLockedOutByAttempts(false);
		user.saveDetails();
		return null;
	}

	public class UserListLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = -5662139473936489392L;

		public UserListLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}

		@Override
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
			return load(first, pageSize, sortField, sortOrder, filters, true);
		}

		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters, boolean clearWhereCriteria ) {
			if( clearWhereCriteria ) {
				getBeanDao().clearWhereCriteria();
			}
			getBeanDao().addWhereCriteria( "bean.userLevel.clearanceLevel >= " + JSFUtil.getLoggedInUser().getUserLevel().getClearanceLevel() );
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}

		@Override
		public String getSearchCriteria() {
			return "bean.username LIKE :similarSearchText";
		}

	}

}
