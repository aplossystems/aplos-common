package com.aplos.common.backingpage.payments.cardsave;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.SslProtocol;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.enums.SslProtocolEnum;

@ManagedBean
@ViewScoped
@WindowId(required=false)
@GlobalAccess
@SslProtocol(sslProtocolEnum=SslProtocolEnum.FORCE_SSL)
public class CardSaveThreeDCallbackPage extends BackingPage {
	private static final long serialVersionUID = -6737299787952050237L;

}












