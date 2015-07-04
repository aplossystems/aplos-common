package com.aplos.common.messageboxes;


public class TextAreaBox extends MessageBox {
	public TextAreaBox() {
		warningBox();
	}

	public TextAreaBox(String title, String text, MessageBoxListener messageBoxListener) {
		warningBox();
		setTitle(title);
		setText(text);
		setMessageBoxListener(messageBoxListener);
	}

	public void warningBox() {
		setOkBtnRendered( true );
		setCancelBtnRendered( true );
		setTextRendered( false );
		setTextPanelRendered( true );
	}
}
