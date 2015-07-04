package com.aplos.common;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

@FacesConverter(value="aplosBeanConverter")
public class AplosBeanConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		if (arg2 == null || arg2.equals("null")) {
			return null;
		} else {
			String obj[] = arg2.split(":");
			if (obj[0] == "Transient") {
				return new TransientObjectConverter().getAsObject(arg0, arg1, arg2);
			}
			try {
				Class<?> objClass = Class.forName( obj[0] );
				PersistentClass persistentClass = (PersistentClass) ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( objClass );
				return new BeanDao( (Class<? extends AplosAbstractBean>) persistentClass.getTableClass() ).get( Long.parseLong(obj[1]) );
			} catch( ClassNotFoundException cnfEx ) {
				ApplicationUtil.handleError(cnfEx);
				return null;
			}
		}
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		if (arg2 == null) {
			return "null";
		}

		AplosAbstractBean bean = (AplosAbstractBean) arg2;
		if (bean.isNew()) {
			return new TransientObjectConverter().getAsString(arg0, arg1, bean);
		}

		return ApplicationUtil.getClass( bean ).getName() + ":" + bean.getId().toString();
	}
}
