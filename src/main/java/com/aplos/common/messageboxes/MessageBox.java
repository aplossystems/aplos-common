package com.aplos.common.messageboxes;

import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.utils.JSFUtil;

public class MessageBox {
	private boolean active;
	private String title;
	private String text;
	private String img;
	private String okBtnText = "Ok";
	private String cancelBtnText = "Cancel";
	private MessageBoxListener messageBoxListener;

	protected boolean isOkBtnRendered = true;;
	protected boolean isCancelBtnRendered = false;
	private boolean isTextRendered = true;
	private boolean isTextPanelRendered = false;

	protected String tablePage;

	public MessageBox() {
	}

	public MessageBox( String title, String message ) {
		this.title = title;
		this.text = message;
	}

	public void setIntoSession() {
		setActive( true );
		JSFUtil.getExternalContext().getSessionMap().put( AplosScopedBindings.MESSAGE_BOX, this );
	}

	public String getTablePage() {
		return tablePage;
	}
	public void setTablePage( String tablePage ) {
		this.tablePage = tablePage;
	}
	public String getImg() {
		return img;
	}
	public void setImg( String img ) {
		this.img = img;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle( String title ) {
		this.title = title;
	}
	public String getText() {
		return text;
	}
	public void setText( String text ) {
		this.text = text;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive( boolean active ) {
		this.active = active;
	}
	public boolean isOkBtnRendered() {
		return isOkBtnRendered;
	}
	public void setOkBtnRendered( boolean isOkBtnRendered ) {
		this.isOkBtnRendered = isOkBtnRendered;
	}
	public boolean isCancelBtnRendered() {
		return isCancelBtnRendered;
	}
	public void setCancelBtnRendered( boolean isCancelBtnRendered ) {
		this.isCancelBtnRendered = isCancelBtnRendered;
	}

	public void okBtnAction() {
		setActive( false );
		if( getMessageBoxListener() != null ) {
			getMessageBoxListener().messageBoxOkBtnPressed();
		}
	}
	public String cancelBtnAction() {
		setActive( false );
		if( getMessageBoxListener() != null ) {
			return getMessageBoxListener().messageBoxCancelBtnPressed();
		} else {
			return "";
		}
	}

	public void setOkBtnText(String okBtnText) {
		this.okBtnText = okBtnText;
	}

	public String getOkBtnText() {
		return okBtnText;
	}

	public void setCancelBtnText(String cancelBtnText) {
		this.cancelBtnText = cancelBtnText;
	}

	public String getCancelBtnText() {
		return cancelBtnText;
	}

	public void setTextRendered(boolean isTextRendered) {
		this.isTextRendered = isTextRendered;
	}

	public boolean isTextRendered() {
		return isTextRendered;
	}

	public void setTextPanelRendered(boolean isTextPanelRendered) {
		this.isTextPanelRendered = isTextPanelRendered;
	}

	public boolean isTextPanelRendered() {
		return isTextPanelRendered;
	}

	public MessageBoxListener getMessageBoxListener() {
		return messageBoxListener;
	}

	public void setMessageBoxListener( MessageBoxListener messageBoxListener ) {
		this.messageBoxListener = messageBoxListener;
	}

}
