package com.aplos.common.servlets;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tools.ant.filters.StringInputStream;

import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.templates.PrintTemplate;

public class XmlServlet extends HttpServlet {
	private static final long serialVersionUID = 6155809383121572074L;
	private Logger logger = Logger.getLogger( getClass() );

	@Override
	protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		try {
			String templateClass = request.getParameter( AplosAppConstants.TEMPLATE_CLASS );
			PrintTemplate xmlTemplate = (PrintTemplate) Class.forName( templateClass ).newInstance();

			response.setHeader("Content-Disposition", "inline;");
			StringInputStream input = null;
			BufferedOutputStream output = null;

			try {
				xmlTemplate.initialise(response, request);
			    input = new StringInputStream( xmlTemplate.getTemplateContent() );
			    output = new BufferedOutputStream(response.getOutputStream());
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
		}
		catch( UnsupportedEncodingException usee ) {
			logger.warn( usee );
		} catch( ClassNotFoundException cnfex ) {
			logger.warn( cnfex );
		} catch( IllegalAccessException iaex ) {
			logger.warn( iaex );
		} catch( InstantiationException iex ) {
			logger.warn( iex );
		}
	}

}