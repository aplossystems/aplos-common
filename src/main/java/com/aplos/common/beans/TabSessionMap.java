package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class TabSessionMap extends HashMap<String, Object> {
	private static final long serialVersionUID = -494941030574533240L;
	public static int FRONT_END_TAB_SESSION_MAP = 0;
	public static int BACK_END_TAB_SESSION_MAP = 1;
	public static String TABLESS_SESSION = "tablessSession";
	private Map<String, Map[]> tabSessionMapMap = new HashMap<String, Map[]>();;

	public TabSessionMap() {
		
	}

	public Map<String, Object> getTabSession(HttpSession session, boolean create,
			boolean isFrontEnd ) {
		String windowId = null;
		if( JSFUtil.isCurrentViewUsingTabSessions( isFrontEnd )  ) {
			windowId = JSFUtil.getWindowId();
		} else {
			windowId = TABLESS_SESSION;
		}
		return getTabSession(session, create, isFrontEnd, windowId );

	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getTabSession(HttpSession session, boolean create,
			boolean isFrontEnd, String windowId ) {
		Map<String, Object>[] tabSessionMapAry = getTabSessionMapMap().get(windowId);
		boolean isTabSessionCreated = false;
		if (tabSessionMapAry == null) {
			if (create) {
				tabSessionMapAry = new HashMap[2];
				tabSessionMapAry[FRONT_END_TAB_SESSION_MAP] = new HashMap<String, Object>();
				tabSessionMapAry[BACK_END_TAB_SESSION_MAP] = new HashMap<String, Object>();
				getTabSessionMapMap().put(windowId, tabSessionMapAry);
				isTabSessionCreated = true;
				
				/*
				 *  This will populate the backend map with objects that were being added to the
				 *  backend map from the frontend before the windowId was set.
				 */
				if( windowId != null ) {
					Map<String, Object>[] nullWindowIdMapAry = getTabSessionMapMap().get( null );
					if( nullWindowIdMapAry != null ) {
						// Put it into a new array to avoid a ConcurrentModificationException
						List<String> keyList = new ArrayList<String>( nullWindowIdMapAry[FRONT_END_TAB_SESSION_MAP].keySet() );
						for( String key : keyList ) {
							Object tempObject = nullWindowIdMapAry[FRONT_END_TAB_SESSION_MAP].remove( key );
							tabSessionMapAry[FRONT_END_TAB_SESSION_MAP].put( key , tempObject );
						}
						keyList = new ArrayList<String>( nullWindowIdMapAry[BACK_END_TAB_SESSION_MAP].keySet() );
						for( String key : keyList ) {
							Object tempObject = nullWindowIdMapAry[BACK_END_TAB_SESSION_MAP].remove( key );
							tabSessionMapAry[BACK_END_TAB_SESSION_MAP].put( key , tempObject );
						}
					}
				}
			} else {
				return null;
			}
		}

		Map<String, Object> tabSessionMap = null;
		if (isFrontEnd) {
			tabSessionMap = tabSessionMapAry[FRONT_END_TAB_SESSION_MAP];
		} else {
			tabSessionMap = tabSessionMapAry[BACK_END_TAB_SESSION_MAP];
		}
		
		if( isTabSessionCreated ) {
			ApplicationUtil.getAplosModuleFilterer().tabSessionCreated( session, tabSessionMapAry[FRONT_END_TAB_SESSION_MAP] );
		}

		return tabSessionMap;
	}

	@Override
	public Object get(Object key) {
		return getTabSession( JSFUtil.getSessionTemp(), true, false ).get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return getTabSession( JSFUtil.getSessionTemp(), true, false ).put(key, value);
	}

	public Map<String, Map[]> getTabSessionMapMap() {
		return tabSessionMapMap;
	}
}
