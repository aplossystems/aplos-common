package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.EmailFrame;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=EmailFrame.class)
public class EmailFrameEditPage extends EditPage {
	private static final long serialVersionUID = -2938869523906941971L;

	private int headerUsingSourceVariable;
	private int footerUsingSourceVariable;

	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		return continueLoad;
	}
	
	public String getEditorOptions( boolean usingSource ) {
		StringBuffer strBuf = new StringBuffer();
		Website website = Website.getCurrentWebsiteFromTabSession();
		strBuf.append( "{ 'websiteId' : '" + website.getId() + "' " );
		if( usingSource ) {
			strBuf.append( ", startupMode : \"source\"" );
		}
		strBuf.append( "}" );
		return strBuf.toString();
	}
	
	public void headerUsingSourceUpdated() {
		EmailFrame emailFrame = resolveAssociatedBean();
		if( getHeaderUsingSourceVariable() == 1 ) {
			emailFrame.setHeaderUsingSource( false );
		} else if( getHeaderUsingSourceVariable() == 2 ) {
			emailFrame.setHeaderUsingSource( true );
		}
	}
	
	public void footerUsingSourceUpdated() {
		EmailFrame emailFrame = resolveAssociatedBean();
		if( getFooterUsingSourceVariable() == 1 ) {
			emailFrame.setFooterUsingSource( false );
		} else if( getFooterUsingSourceVariable() == 2 ) {
			emailFrame.setFooterUsingSource( true );
		}
	}

	public int getFooterUsingSourceVariable() {
		return footerUsingSourceVariable;
	}

	public void setFooterUsingSourceVariable(int footerUsingSourceVariable) {
		this.footerUsingSourceVariable = footerUsingSourceVariable;
	}

	public int getHeaderUsingSourceVariable() {
		return headerUsingSourceVariable;
	}

	public void setHeaderUsingSourceVariable(int headerUsingSourceVariable) {
		this.headerUsingSourceVariable = headerUsingSourceVariable;
	}

}
