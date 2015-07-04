package com.aplos.common.application;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class AplosViewHandler extends ViewHandler {
    protected ViewHandler parent;
    private final ThreadLocal<Boolean> bookmarkable = new ThreadLocal<Boolean>();


    public AplosViewHandler(final ViewHandler viewHandler)
    {
        super();
        parent = viewHandler;
    }

    /**
     * <b>NOTE:</b> This method should only be used by the getBookmarkableURL
     * and getActionURL methods, for the purposes of rewriting form URLs (which
     * do not include viewParameters.)
     * 
     * @return Bookmarkable state - defaults to false if not previously set;
     */
    private boolean isBookmarkable()
    {
        Boolean result = bookmarkable.get();
        if (result == null)
        {
            result = false;
            bookmarkable.set(result);
        }
        return result;
    }

    private void setBookmarkable(final boolean value)
    {
        bookmarkable.set(value);
    }

    @Override
    public String getActionURL(final FacesContext context, final String viewId)
    {
        /*
         * When this method is called for forms, getBookmarkableURL is NOT
         * called; therefore, we have a way to distinguish the two.
         */
		AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext( JSFUtil.getRequest() );
        if (!isBookmarkable() && aplosRequestContext.isRequestRewritten() && viewId != null
                && viewId.equals(context.getViewRoot().getViewId()))
        {
            return context.getExternalContext().encodeActionURL( JSFUtil.getContextPath() + aplosRequestContext.getOriginalUrlWithQueryString());
        }
        return parent.getActionURL(context, viewId);
    }

    @Override
    public String getBookmarkableURL(final FacesContext context, final String viewId,
            final Map<String, List<String>> parameters, final boolean includeViewParams)
    {
        /*
         * When this method is called for <h:link> tags, getActionURL is called
         * as part of the parent call
         */
        setBookmarkable(true);
        String result = parent.getBookmarkableURL(context, viewId, parameters, includeViewParams);
        setBookmarkable(false);
        return result;
    }

    /**
     * Canonicalize the given viewId, then pass that viewId to the next
     * ViewHandler in the chain.
     */
    @Override
    public String deriveViewId(final FacesContext context, final String rawViewId)
    {
//        String canonicalViewId = new URLDuplicatePathCanonicalizer().canonicalize(rawViewId);
        return parent.deriveViewId(context, rawViewId);
    }
    

    @Override
    public Locale calculateLocale(final FacesContext facesContext)
    {
        return parent.calculateLocale(facesContext);
    }

    @Override
    public String calculateRenderKitId(final FacesContext facesContext)
    {
        return parent.calculateRenderKitId(facesContext);
    }

    @Override
    public UIViewRoot createView(final FacesContext context, final String viewId)
    {
        UIViewRoot view = parent.createView(context, viewId);
        return view;
    }

    @Override
    public UIViewRoot restoreView(final FacesContext context, final String viewId)
    {
        UIViewRoot view = parent.restoreView(context, viewId);
        return view;
    } 
    
    @Override
    public String getRedirectURL(final FacesContext context, final String viewId,
            final Map<String, List<String>> parameters, final boolean includeViewParams)
    {
        return parent.getRedirectURL(context, viewId, parameters, includeViewParams);
    }

    @Override
    public String getResourceURL(final FacesContext facesContext, final String path)
    {
        return parent.getResourceURL(facesContext, path);
    }

    @Override
    public void renderView(final FacesContext facesContext, final UIViewRoot viewRoot) throws IOException,
            FacesException
    {
        parent.renderView(facesContext, viewRoot);
    }

    @Override
    public void writeState(final FacesContext facesContext) throws IOException
    {
        parent.writeState(facesContext);
    }

    @Override
    public String calculateCharacterEncoding(final FacesContext context)
    {
        return parent.calculateCharacterEncoding(context);
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(final FacesContext context, final String viewId)
    {
        return parent.getViewDeclarationLanguage(context, viewId);
    }

    @Override
    public void initView(final FacesContext context) throws FacesException
    {
        parent.initView(context);
    }
}

