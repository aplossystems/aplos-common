package com.aplos.common.servlets;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aplos.common.enums.DocumentType;

public class DocumentServlet extends HttpServlet {

	private static final long serialVersionUID = -8682798031860372393L;

	//private ServletContext mContext;

	//private final Logger logger = LoggerFactory.getLogger( getClass().getName() );

	@Override
	public void init( ServletConfig config ) throws ServletException {
		super.init( config );
		//mContext = config.getServletContext();
	}

	/**
	 * Chained to doRequest.
	 *
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doPost( request, response );
	}

	/**
	 * Gateway to AppLayer: handles login and session stuff.
	 *
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doRequest( request, response );
	}

	private void doRequest( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		//Session session = HibernateUtil.getCurrentSession();
		//session.beginTransaction();
		DocumentType type = null;
		try {
			type = DocumentType.valueOf( request.getParameter("type") );
		} catch (Exception e) {}
		String filePath = request.getParameter("file");
		if( filePath != null ) {
			File file = new File( filePath );
			file.getAbsoluteFile();
			if( file.exists() && !file.isDirectory() ) {
			    ServletOutputStream stream = null;
			    BufferedInputStream buf = null;
			    try {
			      stream = response.getOutputStream();
			      if (type == null){
			    	  response.setContentType(getServletContext().getMimeType(filePath));
			      } else {
			    	  response.setContentType(type.getDocumentContentType());
			      }
			      response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" );
			      response.setContentLength((int) file.length());
			      FileInputStream input = new FileInputStream(file);
			      buf = new BufferedInputStream(input);
			      int readBytes = 0;
			      while ((readBytes = buf.read()) != -1) {
					stream.write(readBytes);
				}
			    } catch (IOException ioe) {
			      throw new ServletException(ioe.getMessage());
			    } finally {
			      if (stream != null) {
					stream.close();
				}
			      if (buf != null) {
					buf.close();
				}
			    }
			}
		}
	}

	// Clean up resources
	@Override
	public void destroy() {}
}
