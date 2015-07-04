package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletRequest;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.communication.InboundSms;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class SmsResponsePage extends BackingPage {
	private static final long serialVersionUID = -7307853497890060556L;

	@Override
	public boolean requestPageLoad() {
		HttpServletRequest request = JSFUtil.getRequest();
		InboundSms inboundSms = new InboundSms();
		inboundSms.setType( request.getParameter( "type" ) );
		inboundSms.setTo( request.getParameter( "to" ) );
		inboundSms.setMsisdn( request.getParameter( "msisdn" ) );
		inboundSms.setNetworkCode( request.getParameter( "network-code" ) );
		inboundSms.setMessageId( request.getParameter( "messageId" ) );
		inboundSms.setMessageTimestamp( request.getParameter( "message-timestamp" ) );
		inboundSms.setText( request.getParameter( "text" ) );
		inboundSms.saveDetails();
		ApplicationUtil.getAplosModuleFilterer().registerInboundSms( inboundSms );
		return super.requestPageLoad();
	}
}
