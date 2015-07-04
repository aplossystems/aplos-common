package com.aplos.common;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class MenuHelperBeanHolder {
	private String expressionString;
	
	public MenuHelperBeanHolder( String expressionString ) {
		setExpressionString(expressionString);
	}
	
	public void setValueInParent() {
		ExpressionFactory expf = JSFUtil.getFacesContext().getApplication().getExpressionFactory();
		ValueExpression ve = expf.createValueExpression(JSFUtil.getFacesContext().getELContext(),
				getExpressionString(), Object.class);
		try {
			BackingPage backingPage = JSFUtil.getCurrentBackingPage();
			AplosAbstractBean associatedBean = backingPage.resolveAssociatedBean();
			if( associatedBean != null && !associatedBean.isNew() ) {
				ve.setValue( JSFUtil.getFacesContext().getELContext(), associatedBean );
			}
		} catch( Exception ex ) {
			ApplicationUtil.handleError( new Exception( getExpressionString(), ex ), false );
		}
	}

	public String getExpressionString() {
		return expressionString;
	}

	public void setExpressionString(String expressionString) {
		this.expressionString = expressionString;
	}
}
