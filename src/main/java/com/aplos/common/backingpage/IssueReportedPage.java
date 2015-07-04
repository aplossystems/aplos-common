package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.SslProtocol;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
@SslProtocol(sslProtocolEnum=SslProtocolEnum.FORCE_HTTP)
public class IssueReportedPage extends BackingPage {

	private static final long serialVersionUID = -8170923194917459924L;
	private static final String SERVLET_EXCEPTION_KEY = "javax.servlet.error.exception";

	public IssueReportedPage() {
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();

		JSFUtil.getResponse().setStatus( 500 );
		FacesContext ctx = FacesContext.getCurrentInstance();
		Throwable throwable = null;
		if (ctx.getExternalContext().getRequestMap().containsKey(SERVLET_EXCEPTION_KEY)) {
			throwable = (Throwable) ctx.getExternalContext().getRequestMap().remove(SERVLET_EXCEPTION_KEY);
		} else if (ctx.getExternalContext().getSessionMap().containsKey(SERVLET_EXCEPTION_KEY)) {
			throwable = (Throwable) ctx.getExternalContext().getSessionMap().remove(SERVLET_EXCEPTION_KEY);
		}

		if( throwable != null ) {
			JSFUtil.addToTabSession( AplosScopedBindings.TECHNICAL_DETAILS, CommonUtil.createStackTraceString(throwable) );
//			if( !aplosContextListener.isDebugMode() ) {
//				ErrorEmailSender.sendErrorEmail(aplosContextListener,throwable);
//			}
		}
		return true;
	}
}
