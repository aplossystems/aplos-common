package com.aplos.common.messageboxes;


public class WarningBox extends MessageBox {
	MessageBoxListener messageBoxListener;

	public WarningBox() {
		warningBox();
	}

	public WarningBox(String title, String text, MessageBoxListener messageBoxListener) {
		warningBox();
		setTitle(title);
		setText(text);
		setMessageBoxListenerBean(messageBoxListener);
	}

	public void warningBox() {
		setOkBtnRendered( true );
		setCancelBtnRendered( true );
	}

	public MessageBoxListener getMessageBoxListenerBean() {
		return messageBoxListener;
	}

	public void setMessageBoxListenerBean( MessageBoxListener messageBoxListener ) {
		this.messageBoxListener = messageBoxListener;
	}

	@Override
	public void okBtnAction() {
		setActive( false );
		messageBoxListener.messageBoxOkBtnPressed();
	}
}
