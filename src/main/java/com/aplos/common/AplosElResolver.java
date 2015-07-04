package com.aplos.common;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.component.UIViewRoot;

import com.aplos.common.utils.JSFUtil;

public class AplosElResolver extends ELResolver {

	public AplosElResolver() {
		
	}

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
    	if( base != null ) {
    		return null;
    	}

    	if( property instanceof String ) {
    		String attribute = property.toString();
    		Object resolvedProperty = callPrecedenceScopes(attribute);

    		if( resolvedProperty == null ) {
    			resolvedProperty = JSFUtil.getFromFlashViewMap( attribute );
    		}

			if( resolvedProperty == null ) {
				resolvedProperty = JSFUtil.getFromTabSession( attribute );
			}

			if( resolvedProperty != null ) {
				context.setPropertyResolved( true );
				return resolvedProperty;
			} else {
				context.setPropertyResolved( false );
			}
    	}

        return null;
    }

    public Object callPrecedenceScopes( String attribute ) {
		Object result = JSFUtil.getRequest().getAttribute(attribute);
        if (result != null) {
            return result;
        }

        // check UIViewRoot
        UIViewRoot root = JSFUtil.getFacesContext().getViewRoot();
        if (root != null) {
            Map<String, Object> viewMap = root.getViewMap(false);
            if (viewMap != null) {
                result = viewMap.get(attribute);
            }
        }

        return result;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return Object.class;
    }

}

