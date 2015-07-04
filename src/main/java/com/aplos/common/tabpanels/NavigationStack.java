package com.aplos.common.tabpanels;

import java.util.Stack;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;

import com.aplos.common.AplosUrl;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.BackingPageState;
import com.aplos.common.servlets.EditorUploadServlet;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
public class NavigationStack extends Stack<BackingPageState> {

	private static final long serialVersionUID = 3581938327719498725L;
	private static Logger logger = Logger.getLogger( NavigationStack.class );
	private int navigationStackSizeLimit = 20; // just fits into dropdown, will display 19 (not current page)
	
	public NavigationStack() {}

	@Override
	public boolean add(BackingPageState state) {
		if (push(state) != null) {
			return true;
		}
		return false;
	}

	@Override
	public synchronized BackingPageState push(BackingPageState state) {
		BackingPageState lastState = null;
		if (size() > 0) {
			lastState = super.peek();
		}
		//make sure it replaces the last element for this page if its the same, don't stack it multiple times
		if (lastState != null &&
			((lastState.getRedirectUrl() != null && state.getRedirectUrl() != null
			&& lastState.getRedirectUrl().equals(state.getRedirectUrl())) ||
			(lastState.getBackingPageClass() != null &&
			lastState.getBackingPageClass().equals(state.getBackingPageClass())))) {
			super.pop(); //get rid of the duplicate, replace with our new state
		}
		while (size() >= navigationStackSizeLimit) {
			super.remove(0); //remove the oldest record / bottom of the stack
		}
		return super.push(state);
	}

	/** For backtracking via history
	 *  it takes us back to a specific step in the history, erasing any steps reached after that point
	 */
	public synchronized void navigateBackTo(int index) {
		if (this.size() > index) {
			BackingPageState state = this.get(index);
			for (int i=this.size()-1; i >= index; i--) {
				//remove anywhere we went after where we are headed, as its now useless information
				this.remove(i);
			}
			navigateTo(state);
		} else {
			logger.info("Index " + index + " was out of bounds on the navigation stack (stack size: " + this.size() + ")");
			navigateBack(); //if there is room to navigate back then why not
		}
	}
	
	public synchronized void navigateTo(int index) {
		if (this.size() > index) {
			BackingPageState state = this.get(index);
			navigateTo(state);
		} else {
			logger.info("Index " + index + " was out of bounds on the navigation stack (stack size: " + this.size() + ")");
		}
	}
	
	protected synchronized boolean navigateBackHistoryStyle() {
		if (size() >= 2) {
			BackingPageState bpState = this.get(this.size() - 2);
			navigateTo(bpState);
			
			return true;
		}
		return false;
	}

	public synchronized BackingPageState navigateBack() {
		if (size() >= 2) {
			this.pop(); //get rid of ourselves
			BackingPageState backingPageState = this.pop();
			navigateTo(backingPageState);
			if( backingPageState.getMenuHelperBeanHolder() != null ) {
				backingPageState.getMenuHelperBeanHolder().setValueInParent();
			}
			return backingPageState;
		}
		return null;
	}

	public static void navigateTo(BackingPageState state) {
		state.restoreState();
		JSFUtil.redirect(new AplosUrl(state.getRedirectUrl()), true);
	}

	/**
	 * Saves the state of this page to recall it again later
	 * When overriding this method extra attributes should be stored using {@link BackingPageState#setState(Object[])}
	 * @return
	 */
	public BackingPageState saveState( BackingPage backingPage ) {
		BackingPageState state = new BackingPageState();
		state.saveState(backingPage);
		add(state);
		return state;
	}

	public void setNavigationStackSizeLimit(int navigationStackSizeLimit) {
		this.navigationStackSizeLimit = navigationStackSizeLimit;
	}

	public int getNavigationStackSizeLimit() {
		return navigationStackSizeLimit;
	}

}