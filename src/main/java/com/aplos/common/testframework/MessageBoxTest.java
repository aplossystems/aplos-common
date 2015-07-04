package com.aplos.common.testframework;

import org.apache.log4j.Logger;

public class MessageBoxTest {
	private static Logger logger = Logger.getLogger( MessageBoxTest.class );
	private String title = "Foo";
	private String text = "Bar";
	private boolean active = false;
	private boolean okBtnRendered = true;
	private boolean cancelBtnRendered = false;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isOkBtnRendered() {
		return okBtnRendered;
	}

	public void setOkBtnRendered(boolean okBtnRendered) {
		this.okBtnRendered = okBtnRendered;
	}

	public boolean isCancelBtnRendered() {
		return cancelBtnRendered;
	}

	public void setCancelBtnRendered(boolean cancelBtnRendered) {
		this.cancelBtnRendered = cancelBtnRendered;
	}

	public void okBtnAction() {
		logger.info("OK!");
	}

	public void cancelBtnAction() {
		logger.info("Cancel!");
	}

}
