package com.aplos.common.backingpage;

import java.io.Serializable;

public class AplosEventListener implements Serializable {
	private static final long serialVersionUID = 5295328266805305538L;

	private BackingPage backingPage;

	public void actionPerformed( boolean redirect ) {
	}

	public void setBackingPage(BackingPage backingPage) {
		this.backingPage = backingPage;
	}

	public BackingPage getBackingPage() {
		return backingPage;
	}
}
