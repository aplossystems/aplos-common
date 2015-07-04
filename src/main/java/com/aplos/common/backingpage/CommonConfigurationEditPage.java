package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.CreditCardType;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.communication.EmailFrame;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.enums.PaymentGateway;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.module.CommonModule;
import com.aplos.common.module.CommonModuleDbConfig;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CommonConfiguration.class)
public class CommonConfigurationEditPage extends EditPage {

	private static final long serialVersionUID = 2632066433879427417L;
	private String internationalisationError = "";
	private String defaultLanguageStr=null;
	private Boolean isInternationalizedApplication=null;
	
	public SelectItem[] getEmailFrameSelectItems() {
		return AplosBean.getSelectItemBeansWithNotSelected( EmailFrame.class );
	}
	
	@Override
	public <T extends AplosBean> T resolveAssociatedBean() {
		return resolveAssociatedEditBean();
	}

	@Override
	public boolean responsePageLoad() {
		if( super.resolveAssociatedBean() == null ) {
			CommonConfiguration.getCommonConfiguration().addToScope();
		}
		checkEditBean();
		super.responsePageLoad();
		JSFUtil.resolveVariable( "commonConfiguration" );
		CommonConfiguration commonConfiguration = resolveAssociatedEditBean();
		defaultLanguageStr = commonConfiguration.getDefaultLanguageStr();
		isInternationalizedApplication = commonConfiguration.getIsInternationalizedApplication();
		return true;
	}

	public void passwordEncryptionUpdated() {
		CommonConfiguration commonConfiguration = resolveAssociatedEditBean();
		List<SystemUser> systemUserList = new BeanDao( SystemUser.class ).getAll();
		for( SystemUser tempSystemUser : systemUserList ) {
			tempSystemUser.saveDetails();
		}
		commonConfiguration.saveDetails();
	}

	public List<SelectItem> getPaymentGatewaySelectItem() {
		List<PaymentGateway> paymentGateways = new ArrayList<PaymentGateway>( Arrays.asList( PaymentGateway.values() ) );
		CommonModule aplosCommonModule = (CommonModule) ApplicationUtil.getAplosContextListener().getAplosModuleByClass( CommonModule.class );
		CommonModuleDbConfig commonModuleDbConfig = (CommonModuleDbConfig) aplosCommonModule.getModuleDbConfig();
		if( !commonModuleDbConfig.isSagePayDirectUsed() && !commonModuleDbConfig.isSagePayServerUsed() ) {
			paymentGateways.remove( PaymentGateway.SAGEPAY );
		}
		if( !commonModuleDbConfig.isCardSaveDirectUsed() && !commonModuleDbConfig.isCardSaveServerUsed() ) {
			paymentGateways.remove( PaymentGateway.CARDSAVE );
		}
		if( !commonModuleDbConfig.isPayPalDirectUsed() ) {
			paymentGateways.remove( PaymentGateway.PAYPAL );
		}
		return CommonUtil.getEnumSelectItems( paymentGateways, null );
	}

	public SelectItem[] getSuperuserSystemUserSelectItemBeans() {
		BeanDao dao = new BeanDao(SystemUser.class);
		dao.setWhereCriteria("bean.userLevel.id=" + CommonConfiguration.retrieveUserLevelUtil().getSuperUserLevel().getId());
		return getSystemUserSelectItemBeans(dao);
	}

	public SelectItem[] getAdminSystemUserSelectItemBeans() {
		if (CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel() == null) {
			SelectItem[] noItem = new SelectItem[1];
			noItem[0] = new SelectItem(null, "No admin userlevel set");
			return noItem;
		}
		BeanDao dao = new BeanDao(SystemUser.class);
		dao.setWhereCriteria("bean.userLevel.id=" + CommonConfiguration.retrieveUserLevelUtil().getAdminUserLevel().getId());
		return getSystemUserSelectItemBeans(dao);
	}

	public SelectItem[] getDebugSystemUserSelectItemBeans() {
		BeanDao dao = new BeanDao(SystemUser.class);
		dao.setWhereCriteria("bean.userLevel.id=" + CommonConfiguration.retrieveUserLevelUtil().getDebugUserLevel().getId());
		return getSystemUserSelectItemBeans(dao);
	}

	@SuppressWarnings("unchecked")
	public SelectItem[] getSystemUserSelectItemBeans(BeanDao dao) {
		List<SystemUser> users = (List<SystemUser>) AplosAbstractBean.sortByDisplayName( dao.setIsReturningActiveBeans(true).getAll() );
		SelectItem[] selectItems = new SelectItem[ users.size() + 1 ];
		selectItems[ 0 ] = new SelectItem( null, "Not Selected" );
		for( int i = 0, n = users.size(); i < n; i++ ) {
			selectItems[ i + 1 ] = new SelectItem( users.get( i ), users.get( i ).getFullName() + " (" + users.get( i ).getUsername() + ")" );
		}
		return selectItems;
	}

	public SelectItem[] getUserLevelSelectItemBeans() {
		BeanDao dao = new BeanDao(UserLevel.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(dao.setIsReturningActiveBeans(true).getAll(), "Not Selected");
	}

	public SelectItem[] getCreditCardTypeSelectItemBeans() {
		BeanDao dao = new BeanDao(CreditCardType.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(dao.setIsReturningActiveBeans(true).getAll(), "Not Selected");
	}

	public SelectItem[] getCurrencySelectItemBeans() {
		BeanDao dao = new BeanDao(Currency.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(dao.setIsReturningActiveBeans(true).getAll(), "Not Selected");
	}

	public SelectItem[] getVatTypeSelectItems() {
		BeanDao dao = new BeanDao(VatType.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(dao.setIsReturningActiveBeans(true).getAll(), "Not Selected");
	}

	public SelectItem[] getCompanyDetailsSelectItemBeans() {
		BeanDao dao = new BeanDao(CompanyDetails.class);
		return AplosAbstractBean.getSelectItemBeansWithNotSelected(dao.setIsReturningActiveBeans(true).getAll(), "Not Selected");
	}

	public void setInternationalisationError(String internationalisationError) {
		this.internationalisationError = internationalisationError;
	}

	public String getInternationalisationError() {
		return internationalisationError;
	}

	public Boolean getIsInternationalisationAvailable() {
		return true;
	}

	@Override
	public void applyBtnAction() {
		super.applyBtnAction();
	}
	
	@Override
	public boolean saveBean() {
		CommonConfiguration commonConfiguration = resolveAssociatedEditBean();
		commonConfiguration.setIsInternationalizedApplication(isInternationalizedApplication);
		commonConfiguration.setDefaultLanguageStr(defaultLanguageStr);
		return super.saveBean();
		
	}

	public void setDefaultLanguageStr(String defaultLanguageStr) {
		this.defaultLanguageStr = defaultLanguageStr;
	}

	public String getDefaultLanguageStr() {
		return defaultLanguageStr;
	}

	public void setIsInternationalizedApplication(
			Boolean isInternationalizedApplication) {
		this.isInternationalizedApplication = isInternationalizedApplication;
	}

	public Boolean getIsInternationalizedApplication() {
		return isInternationalizedApplication;
	}
}



