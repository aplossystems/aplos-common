package com.aplos.common;

import java.lang.reflect.Array;

import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.BackingPageState;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

public class BeanMenuHelper {
	protected Class<?> genericClass;
	private boolean isWithNotSelected = true;
	private boolean isShowingNewBtn = true;
	private boolean isShowingEditBtn = true;
	private boolean isSavingBackingPageBean = true;
	
	public BeanMenuHelper(Class<?> genericClass) {
		this.genericClass = genericClass;
	}
	
	public SelectItem[] getSelectItems() {
		if( genericClass.isEnum() ) {
			if( isWithNotSelected() ) {
				return (SelectItem[]) CommonUtil.getEnumSelectItemsWithNotSelected((Class<? extends LabeledEnumInter>)genericClass).toArray( (Object[]) Array.newInstance(genericClass, 0) );
			} else {
				return (SelectItem[]) CommonUtil.getEnumSelectItems((Class<? extends LabeledEnumInter>)genericClass).toArray( (Object[]) Array.newInstance(genericClass, 0) );
			}
		} else {
			if( isWithNotSelected() ) {
				return AplosBean.getSelectItemBeansWithNotSelected((Class<? extends AplosAbstractBean>) genericClass);
			} else {
				return AplosBean.getSelectItemBeans((Class<? extends AplosAbstractBean>) genericClass);
			}
		}
	}
	
	public boolean determineIsShowingNewBtn() {
		if( isShowingNewBtn() && AplosBean.class.isAssignableFrom( genericClass ) ) {
			return true;
		}
		return false;
	}
	
	public boolean determineIsShowingEditBtn() {
		if( isShowingNewBtn() && AplosBean.class.isAssignableFrom( genericClass ) ) {
			return true;
		}
		return false;
	}
	
	public String getButtonLabel() {
		return "Add " + FormatUtil.breakCamelCase( genericClass.getSimpleName() );
	}
	
	public AplosBean createNewBean() {
		return (AplosBean) CommonUtil.getNewInstance( getGenericClass() );
	}
	
	public void goToNew( ActionEvent actionEvent ) {

		Object currentComponent = actionEvent.getSource(); 
		while( currentComponent != null && !(currentComponent instanceof UINamingContainer) ) {
			currentComponent = ((UIComponent) currentComponent).getParent();
		}
		
		String valueExpressionString = ((UINamingContainer)currentComponent).getValueExpression( "value" ).getExpressionString();
		goToNew( valueExpressionString, isSavingBackingPageBean(), createNewBean() );
	}
	
	public static void goToNew( String valueExpressionString, boolean isSavingBackingPageBean, AplosBean newBean ) {
		saveBackingPageBean(isSavingBackingPageBean);
		newBean.redirectToEditPage(true);
		
		BackingPageState backingPageState = JSFUtil.getCurrentStateFromNavigationStack();
		backingPageState.setMenuHelperBeanHolder( new MenuHelperBeanHolder(valueExpressionString) );
	}
	
	public void goToEditPage( Object object ) {
		saveBackingPageBean( isSavingBackingPageBean() );
		AplosBean aplosAbstractBean = (AplosBean) object;
		aplosAbstractBean.redirectToEditPage();
	}
	
	public static void saveBackingPageBean( boolean isSavingBackingPageBean ) {
		if( isSavingBackingPageBean ) {
			BackingPage backingPage = JSFUtil.getCurrentBackingPage();
			if( backingPage != null ) {
				AplosAbstractBean aplosAbstractBean = backingPage.resolveAssociatedBean();
				if( aplosAbstractBean != null && !aplosAbstractBean.isNew() ) {
					aplosAbstractBean.saveDetails();
				}
			}
		}
	}
	
	public String getBeanClassDisplayName() {
		return FormatUtil.breakCamelCase( getGenericClass().getSimpleName() );
	}
	
	protected Class<?> getGenericClass() {
		return genericClass;
	}

	public boolean isWithNotSelected() {
		return isWithNotSelected;
	}

	public void setWithNotSelected(boolean isWithNotSelected) {
		this.isWithNotSelected = isWithNotSelected;
	}

	public boolean isShowingNewBtn() {
		return isShowingNewBtn;
	}

	public void setShowingNewBtn(boolean isShowingNewBtn) {
		this.isShowingNewBtn = isShowingNewBtn;
	}

	public boolean isShowingEditBtn() {
		return isShowingEditBtn;
	}

	public void setShowingEditBtn(boolean isShowingEditBtn) {
		this.isShowingEditBtn = isShowingEditBtn;
	}

	/**
	 * @return the isSavingBackingPageBean
	 */
	public boolean isSavingBackingPageBean() {
		return isSavingBackingPageBean;
	}

	/**
	 * @param isSavingBackingPageBean the isSavingBackingPageBean to set
	 */
	public void setSavingBackingPageBean(boolean isSavingBackingPageBean) {
		this.isSavingBackingPageBean = isSavingBackingPageBean;
	}
	
}
