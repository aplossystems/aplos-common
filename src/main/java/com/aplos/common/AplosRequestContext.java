package com.aplos.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.beans.PageRequest;
import com.aplos.common.tabpanels.MenuTab;
import com.aplos.common.tabpanels.TabPanel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;


public class AplosRequestContext {
	private String originalUrl;
	private String currentUrl;
	private String redirectionUrl;
	private String dynamicViewEl;
	private TabPanel mainTabPanel;
	private TabPanel subTabPanel;
	private Map<TabPanel, List<MenuTab>> menuTabMap = new HashMap<TabPanel, List<MenuTab>>();
	private boolean isMenuProcessed = false;
	private boolean isDynamicViewProcessed = false;
	private Map<String,String> requestParameterMap = new HashMap<String,String>();
	private boolean isAccessAllowed = false;
	private PageRequest pageRequest;
	private boolean isSessionInvalid = false;
	
	public AplosRequestContext( String originalUrl, HttpServletRequest request ) {
		setOriginalUrl(originalUrl);
		setCurrentUrl(originalUrl);
		setRequestParameterMap(new HashMap<String,String>());
		
		if( request.getQueryString() != null ) {
			String[] queryStringParts = request.getQueryString().split( "&" );
			String tempParameterValue;
			for( int i = 0, n = queryStringParts.length; i < n; i++ ) {
				String[] variableParts = queryStringParts[ i ].split( "=" );
				if( variableParts.length == 2 ) {
					tempParameterValue = variableParts[ 1 ];
					if( tempParameterValue.contains( "<" ) ) {
						tempParameterValue = "stripped";
					}
					getRequestParameterMap().put( variableParts[ 0 ], tempParameterValue);
				}
			}
		}
	}
	
	public void handleSessionTimeout() {
		ApplicationUtil.getAplosContextListener().handleSessionTimeout( JSFUtil.getRequest(), JSFUtil.getResponse() );
	}
	
	public void redirectToDynamicView() {
		if( getDynamicViewEl() != null ) {
			FacesContext facesContext = JSFUtil.getFacesContext();
			String viewId = null;
			if( getDynamicViewEl().contains( "'" ) ) {
				viewId = getDynamicViewEl().replace( "'", "" );
			} else {
				ExpressionFactory expf = facesContext.getApplication().getExpressionFactory();
				MethodExpression dynamicViewExpresssion = expf.createMethodExpression(facesContext.getELContext(), "#{ " + getDynamicViewEl() + "}", String.class, new Class[] {} );
				viewId = (String) dynamicViewExpresssion.invoke( facesContext.getELContext(), null );
			}
			if( viewId != null ) {
				setDynamicViewProcessed( true );
				setCurrentUrl( viewId );
				JSFUtil.dispatch( viewId );
			}
		}
	}
	
	
	public List<MenuTab> getProcessedMenuTabs( TabPanel tabPanel ) {
		List<MenuTab> menuTabs = getMenuTabMap().get( tabPanel );
		if( menuTabs == null ) {
			menuTabs = tabPanel.getProcessedTabList();
			getMenuTabMap().put( tabPanel, menuTabs );
		}
		return menuTabs;
	}
	
	public void determineTabPanelState() {
		if( !isMenuProcessed() ) {
			TabPanel lowestTabPanel = ApplicationUtil.getMenuCacher().getLowestTabPanel( true );
			TabPanel mainTabPanel = ApplicationUtil.getMenuCacher().getCurrentMainTabPanel( true );
			if( mainTabPanel != null ) {
				
				if( lowestTabPanel == null || lowestTabPanel.getId().equals( mainTabPanel.getId() ) ) {
					setMainTabPanel( mainTabPanel );
				} else {
					List<MenuTab> processedMainTabList = mainTabPanel.getProcessedTabList();
					if( processedMainTabList.size() < 2 ) {
						TabPanel tempTabPanel = lowestTabPanel;
						// Find the next tab panel below the main tab panel
						while( tempTabPanel.getParent().getParent().getId() != mainTabPanel.getId() ) {
							tempTabPanel = (TabPanel) tempTabPanel.getParent().getParent();
						}
						if( !tempTabPanel.isLinkedToBean() ) {
							if( lowestTabPanel.getId() == tempTabPanel.getId() ) {
								setMainTabPanel( tempTabPanel );
							} else {
								mainTabPanel = tempTabPanel;
								setMainTabPanel( mainTabPanel );
								setSubTabPanel( lowestTabPanel );
							}
						} else {
							setMainTabPanel( mainTabPanel );
							setSubTabPanel( lowestTabPanel );
						}
					} else {
						setMainTabPanel( mainTabPanel );
						setSubTabPanel( lowestTabPanel );	
					}
				}
			}
			setMenuProcessed( true );
		}
	}
	
	public boolean isRequestRewritten() {
		if( !getCurrentUrl().equals( getOriginalUrl() ) ) {
			return true; 
		} else {
			return false;
		}
	}
	
	public String getCurrentUrlWithQueryString() {
		return appendQueryString( getCurrentUrl() );
	}
	
	public String getOriginalUrlWithQueryString() {
		return appendQueryString( getOriginalUrl() );
	}
	
	public String appendQueryString( String url ) {
		if( getRequestParameterMap().size() > 0 ) {
			url += "?";
			String[] parameters = new String[ getRequestParameterMap().size() ];
			int count = 0;
			for( String parameterName : getRequestParameterMap().keySet() ) {
				parameters[ count++ ] = parameterName + "=" + getRequestParameterMap().get( parameterName );
			}
			url += StringUtils.join( parameters, "&" );
		}
		return url;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public String getCurrentUrl() {
		return currentUrl;
	}

	public void setCurrentUrl(String currentUrl) {
		this.currentUrl = currentUrl;
	}

	public String getRedirectionUrl() {
		return redirectionUrl;
	}

	public void setRedirectionUrl(String redirectionUrl) {
		this.redirectionUrl = redirectionUrl;
	}

	public String getDynamicViewEl() {
		return dynamicViewEl;
	}

	public void setDynamicViewEl(String dynamicViewEl) {
		this.dynamicViewEl = dynamicViewEl;
	}

	public boolean isDynamicViewProcessed() {
		return isDynamicViewProcessed;
	}

	public void setDynamicViewProcessed(boolean isDynamicViewProcessed) {
		this.isDynamicViewProcessed = isDynamicViewProcessed;
	}

	public Map<String,String> getRequestParameterMap() {
		return requestParameterMap;
	}

	public void setRequestParameterMap(Map<String,String> requestParameterMap) {
		this.requestParameterMap = requestParameterMap;
	}

	public TabPanel getMainTabPanel() {
		return mainTabPanel;
	}

	public void setMainTabPanel(TabPanel mainTabPanel) {
		this.mainTabPanel = mainTabPanel;
	}

	public TabPanel getSubTabPanel() {
		return subTabPanel;
	}

	public void setSubTabPanel(TabPanel subTabPanel) {
		this.subTabPanel = subTabPanel;
	}

	public boolean isMenuProcessed() {
		return isMenuProcessed;
	}

	public void setMenuProcessed(boolean isMenuProcessed) {
		this.isMenuProcessed = isMenuProcessed;
	}

	public Map<TabPanel, List<MenuTab>> getMenuTabMap() {
		return menuTabMap;
	}

	public void setMenuTabMap(Map<TabPanel, List<MenuTab>> menuTabMap) {
		this.menuTabMap = menuTabMap;
	}

	public boolean isAccessAllowed() {
		return isAccessAllowed;
	}

	public void setAccessAllowed(boolean isAccessAllowed) {
		this.isAccessAllowed = isAccessAllowed;
	}

	public PageRequest getPageRequest() {
		return pageRequest;
	}

	public void setPageRequest(PageRequest pageRequest) {
		this.pageRequest = pageRequest;
	}

	public boolean isSessionInvalid() {
		return isSessionInvalid;
	}

	public void setSessionInvalid(boolean isSessionInvalid) {
		this.isSessionInvalid = isSessionInvalid;
	}
}
