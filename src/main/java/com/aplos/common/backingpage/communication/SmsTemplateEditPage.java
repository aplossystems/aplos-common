package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.templates.smstemplates.CustomSmsTemplate;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SmsTemplate.class)
public class SmsTemplateEditPage extends EditPage {
	private static final long serialVersionUID = -2296583950864243347L;
	
	public boolean isCustomTemplate() {
		SmsTemplate smsTemplate = resolveAssociatedBean();
		if( smsTemplate instanceof CustomSmsTemplate ) {
			return true;
		}
		return false;
	}

	public void reloadDefaultTemplate() {
		SmsTemplate smsTemplate = JSFUtil.getBeanFromScope( SmsTemplate.class );
		smsTemplate.loadDefaultValues();
	}
	
	@Override
	public boolean saveBean() {
		SmsTemplate smsTemplate = resolveAssociatedBean();
		if( smsTemplate.isUsingDefaultContent() ) {
			smsTemplate.setContent( smsTemplate.getDefaultContent() );
		}
		boolean beanSaved = super.saveBean();
		return beanSaved;
	}
}
