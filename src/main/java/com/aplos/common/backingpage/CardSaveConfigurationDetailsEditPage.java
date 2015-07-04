package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.cardsave.CardSaveGateway;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=CardSaveConfigurationDetails.class)
public class CardSaveConfigurationDetailsEditPage extends EditPage {

	private static final long serialVersionUID = -8516102495219178536L;
	
	private CardSaveGateway newGateway = new CardSaveGateway();
	private String gatewayPassword = null;
	
	@Override
	public boolean responsePageLoad() {
		if (getGatewayPassword() == null) {
			CardSaveConfigurationDetails config = JSFUtil.getBeanFromScope(CardSaveConfigurationDetails.class);
			setGatewayPassword( config.getGatewayPassword() );
		}
		return super.responsePageLoad();
	}
	
	@Override
	public boolean saveBean() {
		if ( !CommonUtil.isNullOrEmpty( getGatewayPassword() ) ) {
			CardSaveConfigurationDetails config = JSFUtil.getBeanFromScope(CardSaveConfigurationDetails.class);
			config.setGatewayPassword(getGatewayPassword());
		}
		return super.saveBean();
	}
	
	public void addGateway() {
		if (newGateway.getSubdomain() != null) {
			//if (newGateway.getPriority() != null) {
				CardSaveConfigurationDetails config = JSFUtil.getBeanFromScope(CardSaveConfigurationDetails.class);
				if (config.getGateways() == null) {
					config.setGateways(new ArrayList<CardSaveGateway>());
				}
				config.getGateways().add(newGateway);
				newGateway = new CardSaveGateway();
				newGateway.setPriority( 100 * (config.getGateways().size() +1) );
//			} else {
//				JSFUtil.addMessageForError("Please enter the gateway priority (1 is highest)");
//			}
		} else {
			JSFUtil.addMessageForError("Please enter the gateway subdomain");
		}
	}
	
	public void removeGateway() {
		String gateway = (String) JSFUtil.getRequest().getAttribute("gateway");
		if (gateway != null) {
			CardSaveConfigurationDetails config = JSFUtil.getBeanFromScope(CardSaveConfigurationDetails.class);
			config.getGateways().remove(gateway);
		}
	}

	public List<SelectItem> getConnectToTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( CardSaveConfigurationDetails.ConnectToType.class );
	}
	
	public List<SelectItem> getIntegrationTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( CardSaveConfigurationDetails.IntegrationType.class );
	}

	public CardSaveGateway getNewGateway() {
		return newGateway;
	}

	public void setNewGateway(CardSaveGateway newGateway) {
		this.newGateway = newGateway;
	}

	public String getGatewayPassword() {
		return gatewayPassword;
	}

	public void setGatewayPassword(String gatewayPassword) {
		this.gatewayPassword = gatewayPassword;
	}
	
}
