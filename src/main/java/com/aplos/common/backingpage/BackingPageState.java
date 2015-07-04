package com.aplos.common.backingpage;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.aplos.common.MenuHelperBeanHolder;
import com.aplos.common.TrailDisplayName;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class BackingPageState implements Serializable {
	private static final long serialVersionUID = -7015198804447580827L;

	private Class<? extends BackingPage> backingPageClass;
	private TrailDisplayName trailDisplayName;
	private String redirectUrl;
	private String originalUrl;
	private AplosAbstractBean associatedBean=null;
	private JsfScope jsfScope;
	private Long associatedBeanId=null;
	private Object[] state = null;
	private Date stateSetDate = new Date();
	private List<String> requestCommandParameters;
	private MenuHelperBeanHolder menuHelperBeanHolder;

	public BackingPageState() {	}

	public BackingPageState getCopy() {
		BackingPageState copiedState = new BackingPageState();
		copiedState.setAssociatedBean(getAssociateBean());
		copiedState.setState(getState().clone());
		copiedState.setTrailDisplayName(getTrailDisplayName());
		copiedState.setBackingPageClass(getBackingPageClass());
		return copiedState;
	}

	public void restoreState() {
		AplosAbstractBean associate = loadAssociateBean();
		if (associate != null ) {
			associate.addToScope( getJsfScope() );
		}
	}

	/**
	 * Saves the state of this page to recall it again later
	 * When overriding this method extra attributes should be stored using {@link BackingPageState#setState(Object[])}
	 * @return
	 */
	public void saveState( BackingPage backingPage ) {
		setBackingPageClass(backingPage.getClass());
		AplosBean associatedBean = null;
		if( backingPage instanceof EditPage ) {
			associatedBean = backingPage.resolveAssociatedBean();
		}
		setJsfScope( backingPage.determineAssociatedBeanScope() );
		setTrailDisplayName(backingPage.determineTrailDisplayName());
		String redirUrl = JSFUtil.getCurrentPath( true ) + ".jsf?";
		String queryString = JSFUtil.getRequest().getQueryString();
		if (queryString != null) {
			if (!queryString.contains("lang=")) {
				redirUrl += "lang=" + CommonUtil.getContextLocale().getLanguage() + "&" + queryString;
			} else {
				redirUrl += queryString;
			}
		} else {
			redirUrl += "lang=" + CommonUtil.getContextLocale().getLanguage();
		}

		setRequestCommandParameters( (List<String>) JSFUtil.getFromTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST ) );
		JSFUtil.addToTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST, null );
		
		setRedirectUrl( redirUrl );
		setOriginalUrl( JSFUtil.getAplosContextOriginalUrl() );

		if (associatedBean != null) {
			setAssociatedBean(associatedBean);
		}
	}

	public void setBackingPageClass(Class<? extends BackingPage> backingPageClass) {
		this.backingPageClass = backingPageClass;
	}

	public Class<? extends BackingPage> getBackingPageClass() {
		return backingPageClass;
	}

	public void setTrailDisplayName(TrailDisplayName trailDisplayName) {
		this.trailDisplayName = trailDisplayName;
	}

	public TrailDisplayName getTrailDisplayName() {
		return trailDisplayName;
	}

	public void setAssociatedBean(AplosAbstractBean associatedBean) {
		this.associatedBean = associatedBean;
	}

	public AplosAbstractBean getAssociateBean() {
		return associatedBean;
	}

	public AplosAbstractBean loadAssociateBean() {
		if (associatedBean == null || associatedBean.isNew() ) {
			return associatedBean;
		} else {
			BackingPage backingPage = (BackingPage) JSFUtil.resolveVariable(backingPageClass);
			BeanDao dao = new BeanDao(backingPage.getBeanDao().getBeanClass());
			AplosAbstractBean aplosAbstractBean = dao.get(associatedBean.getId());
			if( associatedBean.isReadOnly() ) {
				return aplosAbstractBean;
			} else {
				return aplosAbstractBean.getSaveableBean();
			}
		}
	}

	public void setState(Object[] state) {
		this.state = state;
	}

	public Object[] getState() {
		return state;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public Date getStateSetDate() {
		return stateSetDate;
	}

	public void setStateSetDate(Date stateSetDate) {
		this.stateSetDate = stateSetDate;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public JsfScope getJsfScope() {
		return jsfScope;
	}

	public void setJsfScope(JsfScope jsfScope) {
		this.jsfScope = jsfScope;
	}

	public List<String> getRequestCommandParameters() {
		return requestCommandParameters;
	}

	public void setRequestCommandParameters(List<String> requestCommandParameters) {
		this.requestCommandParameters = requestCommandParameters;
	}

	public Long getAssociatedBeanId() {
		return associatedBeanId;
	}

	public void setAssociatedBeanId(Long associatedBeanId) {
		this.associatedBeanId = associatedBeanId;
	}

	public MenuHelperBeanHolder getMenuHelperBeanHolder() {
		return menuHelperBeanHolder;
	}

	public void setMenuHelperBeanHolder(MenuHelperBeanHolder menuHelperBeanHolder) {
		this.menuHelperBeanHolder = menuHelperBeanHolder;
	}

}