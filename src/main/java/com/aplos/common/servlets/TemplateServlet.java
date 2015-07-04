package com.aplos.common.servlets;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tools.ant.filters.StringInputStream;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.listeners.PageBindingPhaseListener;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ErrorEmailSender;
import com.lowagie.text.pdf.BaseFont;

public class TemplateServlet extends HttpServlet {
	private static final long serialVersionUID = 6155809383121572074L;
	private Logger logger = Logger.getLogger( getClass() );

	@Override
	protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		try {
			String templateClass = request.getParameter( AplosAppConstants.TEMPLATE_CLASS );
			if (templateClass == null || templateClass.equals("")) {
				return;
			}

			ServletContext servletContext = request.getSession().getServletContext();
			AplosContextListener aplosContextListener = (AplosContextListener) servletContext.getAttribute( AplosScopedBindings.CONTEXT_LISTENER );

			String processedTemplateClass;
			if( PageBindingPhaseListener.getPrintTemplateOverrideMap().containsKey( templateClass ) ) {
				processedTemplateClass = PageBindingPhaseListener.getPrintTemplateOverrideMap().get( templateClass );
			} else {
				processedTemplateClass = templateClass;
			}
			PrintTemplate printTemplate = (PrintTemplate) Class.forName( processedTemplateClass ).newInstance();
			boolean createPdf = false;
			boolean createSizedPdf = false;
			if( request.getParameter( AplosAppConstants.CREATE_PDF ) != null && request.getParameter( AplosAppConstants.CREATE_PDF ).equals( "true" ) ) {
				createPdf = true;
			}
			if( request.getParameter( AplosAppConstants.CREATE_SIZED_PDF ) != null && request.getParameter( AplosAppConstants.CREATE_SIZED_PDF ).equals( "true" ) ) {
				createSizedPdf = true;
			}

			response.setHeader("Content-Disposition", "inline;");
			StringInputStream input = null;
			BufferedOutputStream output = null;

			try {
				printTemplate.initialise(response, request);
			    String templateOutput = printTemplate.getTemplateContent();

			    if( !createSizedPdf ) {
				    output = new BufferedOutputStream(response.getOutputStream());
				    if( createPdf ) {
				    	ITextRenderer renderer = new ITextRenderer();
				        ApplicationUtil.getAplosContextListener().addFonts( renderer );
				        renderer.setDocumentFromString(templateOutput);
				        renderer.layout();
				        renderer.createPDF(output, true);
				    	response.setContentType( "application/pdf" );
				    } else {

					    input = new StringInputStream( templateOutput );
					    byte[] buffer = new byte[8192];
					    int length;
					    while ((length = input.read(buffer)) > 0) {
					        output.write(buffer, 0, length);
					    }
				    }
			    }
			} catch( Exception e ) {
				AplosContextListener.getAplosContextListener().handleError(e);
			    input = new StringInputStream( "There was an error trying to create this report, please contact system support" );
			    byte[] buffer = new byte[8192];
			    int length;
			    while ((length = input.read(buffer)) > 0) {
			        output.write(buffer, 0, length);
			    }
			} finally {
			    if (output != null) {
					try { output.close(); } catch (IOException logOrIgnore) {}
				}
			    if (input != null) {
					try { input.close(); } catch (IOException logOrIgnore) {}
				}
			}
		} catch( ClassNotFoundException cnfex ) {
			AplosContextListener.getAplosContextListener().handleError(cnfex);
		} catch( IllegalAccessException iaex ) {
			AplosContextListener.getAplosContextListener().handleError(iaex);
		} catch( InstantiationException iex ) {
			AplosContextListener.getAplosContextListener().handleError(iex);
		}
	}

}