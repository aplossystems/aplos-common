package com.aplos.common;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ErrorEmailSender;
import com.aplos.common.utils.JSFUtil;

@FacesConverter(value="aplosAbstractBeanConverter")
public class AplosAbstractBeanConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String obj) {
		UISelectItems selectItems = getSelectItems(uiComponent);
		SelectItem[] selectItemAry = getValueSelectItems(facesContext, selectItems);
		if( selectItemAry != null ) {
			HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
			request.setAttribute( getSelectItemsBinding( selectItems ), null );

			try {
				if( obj == null ) {
					return null;
				} else {
					return selectItemAry[ Integer.parseInt( obj ) ].getValue();
				}

			} catch ( Exception ex ) {
				//  There's an error been sent back saying that the obj is out of bounds of the
				//  array, this should give more information if this happens again 8 Nov 2010
				StringBuffer strBuf = new StringBuffer();
				if (obj != null) {
					strBuf.append( obj.toString() + " " );
				}
				for ( int i = 0, n = selectItemAry.length; i < n; i++ ) {
					strBuf.append( selectItemAry[ i ].getLabel() + " " );
				}
				ErrorEmailSender.sendErrorEmail( JSFUtil.getRequest(), ApplicationUtil.getAplosContextListener(), ex, strBuf.toString() );
				if ( selectItemAry.length > 0 ) {
					return selectItemAry[ 0 ];
				}
			}
		}
		return null;
	}

	public static UISelectItems getSelectItems(UIComponent uiComponent) {
		List<UIComponent> childList = uiComponent.getChildren();
		UISelectItems selectItems = null;
		for( int i = 0, n = childList.size(); i < n; i++ ) {
			if( childList.get( i ) instanceof UISelectItems ) {
				selectItems = (UISelectItems) childList.get( i );
				break;
			}
		}
		return selectItems;
	}

	@SuppressWarnings("unchecked")
	public static SelectItem[] getValueSelectItems( FacesContext facesContext, UISelectItems selectItems ) {
		if ( selectItems != null ) {
			HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
			SelectItem selectItemAry[] = (SelectItem[]) request.getAttribute( getSelectItemsBinding( selectItems ) );

			if ( selectItemAry == null ) {
				Object selectItemsObj = selectItems.getValue();
				if( selectItemsObj == null ) {
					selectItemAry = new SelectItem[ 0 ];
				} else if ( selectItemsObj.getClass().isArray() ) {
					selectItemAry = (SelectItem[]) selectItemsObj;
				} else {
					selectItemAry = ((List<SelectItem>) selectItemsObj).toArray(new SelectItem[0]);
				}
				request.setAttribute( getSelectItemsBinding( selectItems ), selectItemAry );
			}

			return selectItemAry;
		} else {
			return null;
		}
	}
	
	public static String getSelectItemsBinding( UISelectItems selectItems ) {
		return selectItems.getClientId().replace( ":", "_" ) + "_selectItems";
	}

	@Override
	public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object obj) {
		SelectItem[] selectItemAry = getValueSelectItems(facesContext, getSelectItems(uiComponent));
		if( selectItemAry != null ) {
			if( obj == null ) {
				for( int i = 0, n = selectItemAry.length; i < n; i++ ) {
					if( selectItemAry[ i ].getValue() == null ) {
						return String.valueOf( i );
					}
				}
			} else {
				for( int i = 0, n = selectItemAry.length; i < n; i++ ) {
					if( selectItemAry[ i ].getValue() != null && selectItemAry[ i ].getValue().equals( obj ) ) {
						return String.valueOf( i );
					}
				}
			}
		}

		return null;
	}
}


