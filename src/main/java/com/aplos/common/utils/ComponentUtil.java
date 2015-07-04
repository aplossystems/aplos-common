package com.aplos.common.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.application.ViewHandler;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UINamingContainer;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.aplos.common.ThemeManager;
import com.aplos.common.appconstants.ComponentConstants;

public class ComponentUtil {
	public static int instanceCount = 0;

	@SuppressWarnings("unchecked")
	public static void handleAttribute(UIComponent component, String name, Object value) {
		List<String> setAttributes = (List<String>) component.getAttributes().get("javax.faces.component.UIComponentBase.attributesThatAreSet");
		if(setAttributes == null) {
			setAttributes = new ArrayList<String>(6);
			component.getAttributes().put("javax.faces.component.UIComponentBase.attributesThatAreSet", setAttributes);
		}
		if(setAttributes != null) {
			if(value == null) {
				ValueExpression ve = component.getValueExpression(name);
				if(ve == null) {
					setAttributes.remove(name);
				} else if(!setAttributes.contains(name)) {
					setAttributes.add(name);
				}
			}
		}
	}

	public static int getIntegerFromDimension( String numStr ) {
		int num;
		try {
			num = Integer.parseInt( numStr );
		} catch( NumberFormatException nfex ) {
			num = Integer.parseInt( numStr.substring( 0, numStr.length() - 2 ) );
		}
		return num;
	}

	public static Boolean determineBooleanAttributeValue( UIComponent component, String attributeKey, Boolean defaultValue ) {
		if( component.getAttributes().get( attributeKey ) == null ) {
			return defaultValue;
		}

		if( component.getAttributes().get( attributeKey ).equals( "true" ) ||
				(component.getAttributes().get( attributeKey ) instanceof Boolean && (Boolean)component.getAttributes().get( attributeKey ) == true)) {
			return true;
		} else if( component.getAttributes().get( attributeKey ).equals( "false" ) ||
				(component.getAttributes().get( attributeKey ) instanceof Boolean && (Boolean)component.getAttributes().get( attributeKey ) == false)) {
			return false;
		} else {
			return defaultValue;
		}
	}

	public static Long determineLongAttributeValue( UIComponent component, String attributeKey, Long defaultValue ) {
		if( component.getAttributes().get( attributeKey ) == null ) {
			return defaultValue;
		}

		if( component.getAttributes().get( attributeKey ) instanceof String ) {
			return new Long( (String) component.getAttributes().get( attributeKey ) );
		} else if( component.getAttributes().get( attributeKey ) instanceof Long ) {
			return (Long) component.getAttributes().get( attributeKey );
		} else {
			return defaultValue;
		}
	}

	public static Integer determineIntegerAttributeValue( UIComponent component, String attributeKey, Integer defaultValue ) {
		if( component.getAttributes().get( attributeKey ) == null ) {
			return defaultValue;
		}

		if( component.getAttributes().get( attributeKey ) instanceof String ) {
			return new Integer( (String) component.getAttributes().get( attributeKey ) );
		} else if( component.getAttributes().get( attributeKey ) instanceof Long ) {
			return ((Long) component.getAttributes().get( attributeKey )).intValue();
		} else if( component.getAttributes().get( attributeKey ) instanceof Integer ) {
			return (Integer) component.getAttributes().get( attributeKey );  //not sure if this ever happens
		} else {
			return defaultValue;
		}
	}

	public static synchronized String getUniqueId( String componentName, String prefix ) {
		return prefix + componentName + "_" + (instanceCount++ % 100000000);
	}

//	public static String getUniqueId( String prefix, int count[], FacesContext facesContext, UIComponent component ) {
//		UIComponent form = _findForm(  facesContext, component );
//		UIComponent foundComponent;
//		do {
//			foundComponent = form.findComponent( prefix + count[ 0 ]++ );
//		}
//		while( foundComponent != null );
//
//		return prefix + "-" + count[ 0 ];
//	}

	public static UIComponent _findForm( FacesContext facesContext, UIComponent component ) {
		if( component == null ) {
			return null;
		}
		if( component instanceof UIForm || component.getClass().getName().endsWith( "RichForm" ) ) {
			return component;
		} else {
			return _findForm( facesContext, component.getParent() );
		}
	}

	public static String findFormClientId(FacesContext facesContext, UIComponent component) {
		return _findForm( facesContext, component ).getClientId( facesContext );
	}

	public static String getTheme(FacesContext facesContext) {
		return ThemeManager.getThemeManager().getTheme();
//		SystemUser systemUser = ((SystemUser) JSFUtil.getSession().getAttribute(AplosScopedBindings.CURRENT_USER));
//		String theme = null;
//		if( systemUser == null || (theme = systemUser.getTheme()) == null ) {
//			if (theme == null) {
//				AplosContextListener contextListener = (AplosContextListener) facesContext.getExternalContext().getApplicationMap().get( AplosScopedBindings.CONTEXT_LISTENER );
//				theme = contextListener.getDefaultTheme();
//				if( systemUser != null ) {
//					systemUser.setTheme(theme);
//					systemUser.aqlSaveDetails();
//				}
//			}
//		}
//		return theme;
	}

//	public static String getAndWriteTheme(FacesContext facesContext)
//			throws IOException {
//		String theme = getTheme(facesContext);
//
//		if( !facesContext.getPartialViewContext().isPartialRequest() ) {
//			ComponentUtil.writeStyleResource( facesContext, "common.css" );
//			ComponentUtil.writeThemeScript(facesContext, theme);
//		}
//
//		return theme;
//	}

	public static void addStateToAttributes(UIComponent component,
			String key, Object state) {
		if (state != null) {
			component.getAttributes().put(key, state);
		}
	}

	public static final void processProperty(final UIComponent component,
			final ValueExpression property, final String propertyName) {
		if (property != null) {
			if (property.isLiteralText()) {
				component.getAttributes().put(propertyName,
						property.getExpressionString());
			} else {
				component.setValueExpression(propertyName, property);
			}
		}
	}

	public static final String getStringFromAttribute( Object obj ) {
		if( obj instanceof String ) {
			return (String) obj;
		} else if( obj instanceof ValueExpression && ((ValueExpression) obj).isLiteralText() ) {
			return ((ValueExpression) obj).getExpressionString();
		}
		return "";
	}

	public static void writeThemeScript(FacesContext context, String theme)
			throws IOException {
		if (theme != null) {
			writeStyleResource(context, theme + ".css" );
		}
	}

	public static void writeScriptResource(FacesContext context,
			String resourcePath ) throws IOException {
		writeScriptResource(context, resourcePath, false);
	}

	public static void writeScriptResource(FacesContext context, String resourcePath, boolean allowOnAjax ) throws IOException {
		if( !context.getPartialViewContext().isPartialRequest() || allowOnAjax ) {
			Set scriptResources = getScriptResourcesAlreadyWritten(context,	resourcePath);
			// Set.add() returns true only if item was added to the set
			// and returns false if item was already present in the set
			if (scriptResources.add(resourcePath)) {
				ViewHandler handler = context.getApplication().getViewHandler();
				String resourceURL = handler.getResourceURL(context,
						ComponentConstants.SCRIPT_PATH + resourcePath);
				ResponseWriter out = context.getResponseWriter();
				out.startElement("script", null);
				out.writeAttribute("type", "text/javascript", null);
				out.writeAttribute("src", resourceURL, null);
				out.endElement("script");
			}
		}
	}

	public static void writeScriptTag(FacesContext context,	String scriptContent, boolean allowOnAjax ) throws IOException {
		if( !context.getPartialViewContext().isPartialRequest() || allowOnAjax ) {
				ResponseWriter out = context.getResponseWriter();
				out.startElement("script", null);
				out.writeAttribute("type", "text/javascript", null);
				out.writeText(scriptContent, null);
				out.endElement("script");

		}
	}

	public static void writeStyleResourceFromDirectory(FacesContext context, String resourcePath, String contextDirectory ) throws IOException {
		Set scriptResources = getScriptResourcesAlreadyWritten(context,
				resourcePath);

		// Set.add() returns true only if item was added to the set
		// and returns false if item was already present in the set
		if (scriptResources.add(resourcePath)) {
			ViewHandler handler = context.getApplication().getViewHandler();
			String resourceURL = handler.getResourceURL(context,
					contextDirectory + resourcePath);
			ResponseWriter out = context.getResponseWriter();
			out.startElement("link", null);
			out.writeAttribute("type", "text/css", null);
			out.writeAttribute("rel", "stylesheet", null);
			out.writeAttribute("href", resourceURL, null);
			out.endElement("link");

		}
	}

	public static void writeStyleResource(FacesContext context, String resourcePath ) throws IOException {
		writeStyleResourceFromDirectory(context, resourcePath, ComponentConstants.STYLE_PATH );
	}

	public static void writeStyleResourceFromScriptsDirectory(FacesContext context, String resourcePath ) throws IOException {
		writeStyleResourceFromDirectory(context, resourcePath, ComponentConstants.SCRIPT_PATH );
	}

	private static Set getScriptResourcesAlreadyWritten(FacesContext context,
			String scriptKey) {
		scriptKey = getScriptWrittenKey(scriptKey);
		ExternalContext external = context.getExternalContext();
		Map requestScope = external.getRequestMap();
		Set written = (Set) requestScope.get(scriptKey);

		if (written == null) {
			written = new HashSet();
			requestScope.put(scriptKey, written);
		}

		return written;
	}
	

    public static UIComponent findComponentFromRoot(String expr) {
        if (expr == null) {
            throw new NullPointerException();
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        final char sepChar = UINamingContainer.getSeparatorChar(ctx);
        final String SEPARATOR_STRING = String.valueOf(sepChar);

        if (expr.length() == 0) {
            // if an empty value is provided, fail fast.
            throw new IllegalArgumentException("\"\"");
        }

        // Identify the base component from which we will perform our search
        UIComponent base = JSFUtil.getFacesContext().getViewRoot();
        if (expr.charAt(0) == sepChar) {
            // Absolute searches start at the root of the tree
            while (base.getParent() != null) {
                base = base.getParent();
            }
            // Treat remainder of the expression as relative
            expr = expr.substring(1);
        } else if (!(base instanceof NamingContainer)) {
            // Relative expressions start at the closest NamingContainer or root
            while (base.getParent() != null) {
                if (base instanceof NamingContainer) {
                    break;
                }
                base = base.getParent();
            }
        }

        // Evaluate the search expression (now guaranteed to be relative)
        UIComponent result = null;
        String[] segments = expr.split(SEPARATOR_STRING);
        for (int i = 0, length = (segments.length - 1); i < segments.length; i++, length--) {
            result = findComponent(base, segments[i], (i == 0));
            // the first element of the expression may match base.id
            // (vs. a child if of base)
            if (i == 0 && result == null && segments[i].equals(base.getId())) {
                result = base;
            }
            if (result != null && (!(result instanceof NamingContainer)) && length > 0) {
                throw new IllegalArgumentException(segments[i]);
            }
            if (result == null) {
            	List<UIComponent> allChildren = new ArrayList<UIComponent>();
                for (Iterator iter = base.getFacetsAndChildren(); iter.hasNext();) {
                	allChildren.add( (UIComponent) iter.next() );
                }

            	List<UIComponent> newAllChildren = new ArrayList<UIComponent>();
                componentWhile : while( allChildren.size() > 0 ) {
	                for( UIComponent tempComponent : allChildren ) {
	                	result = findComponent(tempComponent, segments[i], (i == 0));
	                	if( result != null ) {
	                		base = result;
	                		break componentWhile;
	                	}
	                	for (Iterator iter = tempComponent.getFacetsAndChildren(); iter.hasNext();) {
	                		newAllChildren.add( (UIComponent) iter.next() );
	                    }
	                }
	                allChildren = new ArrayList<UIComponent>(newAllChildren);
            		newAllChildren.clear();
            	} 
                break;
            }
            base = result;
        }

        // Return the final result of our search
        return (result);
    }

    private static UIComponent findComponent(UIComponent base,
                                             String id,
                                             boolean checkId) {
        if (checkId && id.equals(base.getId())) {
            return base;
        }
        // Search through our facets and children
        UIComponent result = null;
        for (Iterator i = base.getFacetsAndChildren(); i.hasNext();) {
            UIComponent kid = (UIComponent) i.next();
            if (!(kid instanceof NamingContainer)) {
                if (checkId && id.equals(kid.getId())) {
                    result = kid;
                    break;
                }
                result = findComponent(kid, id, true);
                if (result != null) {
                    break;
                }
            } else if (id.equals(kid.getId())) {
                result = kid;
                break;
            }
        }
        return (result);

    }

	public static String getScriptWrittenKey(String componentClass) {
		return componentClass + ".SCRIPTS_WRITTEN";
	}

	public static String getImageUrlWithTheme(FacesContext facesContext,
			String image) {
		ViewHandler handler = facesContext.getApplication().getViewHandler();
		String imageUrl = handler.getResourceURL(facesContext,
				ComponentConstants.IMAGE_PATH
						+ getTheme(facesContext) + "/" + image);
		return imageUrl;
	}

	public static String getImageUrlWithThemeWithoutContext(
			FacesContext facesContext, String image) {
		ViewHandler handler = facesContext.getApplication().getViewHandler();
		String imageUrl = ComponentConstants.IMAGE_PATH + getTheme(facesContext) + "/"
				+ image;
		return imageUrl;
	}

	public static String getImageUrl(FacesContext facesContext, String image) {
		ViewHandler handler = facesContext.getApplication().getViewHandler();
		String imageUrl = ComponentConstants.IMAGE_PATH
				+ image;
		return imageUrl;
	}
}
