package com.aplos.common.backingpage;

public class EditPageConfig extends BackingPageConfig {
	private static final long serialVersionUID = 3077125188407891418L;
	private BackingPage backingPage;
	private AplosEventListener applyBtnActionListener;
	private OkBtnListener okBtnActionListener;
	private AplosEventListener cancelBtnActionListener;

	public EditPageConfig() {
	}

	public void applyBtnAction( boolean redirect ) {
		if( getApplyBtnActionListener() != null ) {
			getApplyBtnActionListener().actionPerformed( redirect );
		}
	}

	public void okBtnAction( boolean redirect ) {
		if( okBtnActionListener != null ) {
			okBtnActionListener.actionPerformed( redirect );
		}
	}

	public void cancelBtnAction( boolean redirect ) {
		if( cancelBtnActionListener != null ) {
			cancelBtnActionListener.actionPerformed( redirect );
		}
	}

	public void setApplyBtnActionListener(AplosEventListener applyBtnActionListener) {
		this.applyBtnActionListener = applyBtnActionListener;
	}

	public AplosEventListener getApplyBtnActionListener() {
		return applyBtnActionListener;
	}

	public void setOkBtnActionListener(OkBtnListener okBtnActionListener) {
		this.okBtnActionListener = okBtnActionListener;
	}

	public OkBtnListener getOkBtnActionListener() {
		return okBtnActionListener;
	}

	public void setCancelBtnActionListener(AplosEventListener cancelBtnActionListener) {
		this.cancelBtnActionListener = cancelBtnActionListener;
	}

	public AplosEventListener getCancelBtnActionListener() {
		return cancelBtnActionListener;
	}

	public void setBackingPage(BackingPage backingPage) {
		this.backingPage = backingPage;
	}

	public BackingPage getBackingPage() {
		return backingPage;
	}
}
