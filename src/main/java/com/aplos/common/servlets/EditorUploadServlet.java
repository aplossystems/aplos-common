package com.aplos.common.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.aplos.common.AplosUrl;
import com.aplos.common.AplosUrl.Protocol;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

public class EditorUploadServlet extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = -7743474450406185758L;
	private static Logger logger = Logger.getLogger( EditorUploadServlet.class );
	//private Logger logger = Logger.getLogger(getClass());

	@Override
	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		String returnMessage = "";
		String webFileAddress = "";
		String originalName = "";
		File f = null;
		try {
			// Use an advanced form of the constructor that specifies a
			// character
			// encoding of the request (not of the file contents) and a file
			// rename policy.
			MultipartRequest multipartRequest = new MultipartRequest( req, CommonWorkingDirectory.EDITOR_UPLOAD_DIR.getDirectoryPath(true), 10 * 1024 * 1024, "ISO-8859-1", new DefaultFileRenamePolicy() );
			multipartRequest.getParameterNames();

			String name = multipartRequest.getFileNames().nextElement().toString();
			f = multipartRequest.getFile( name );

			AplosWorkingDirectoryInter aplosWorkingDirectoryEnum = CommonWorkingDirectory.EDITOR_UPLOAD_DIR;

			originalName = multipartRequest.getOriginalFileName( "upload" );
			
			FileDetails fileDetails = new FileDetails();
			fileDetails.setName( originalName );
			fileDetails.setFileDetailsOwner(aplosWorkingDirectoryEnum.getAplosWorkingDirectory());
			fileDetails.saveDetails();
			String idFilename = fileDetails.getId() + originalName.substring( originalName.lastIndexOf(".") );
			fileDetails.setFilename(idFilename);
			fileDetails.saveDetails();
			File newFile = new File( aplosWorkingDirectoryEnum.getDirectoryPath(true) + "/" + idFilename );
			f.renameTo( newFile );
			
			webFileAddress = "/media/?" + AplosAppConstants.FILE_NAME + "=" + aplosWorkingDirectoryEnum.getDirectoryPath(false) + "/" + idFilename;
			String websiteId = req.getParameter("websiteId"); 
			if( !CommonUtil.isNullOrEmpty( websiteId ) ) {
				Website website = ApplicationUtil.getAplosContextListener().getWebsite( Long.parseLong( websiteId ) );
				
				if( website != null ) {
					AplosUrl aplosUrl = new AplosUrl( webFileAddress, false );
					aplosUrl.setHost( website );
					aplosUrl.setScheme(Protocol.HTTP);
					webFileAddress = aplosUrl.toString();
				}
			}
			fileDetails.saveDetails();
		} catch( IOException lEx ) {
			lEx.printStackTrace();
			returnMessage = "error reading or saving file";
		}

		PrintWriter out = resp.getWriter();
		out.print( "<html><body><script type=\"text/javascript\">window.parent.CKEDITOR.tools.callFunction(" + req.getParameter( "CKEditorFuncNum" ) + ", '" + webFileAddress + "', '" + returnMessage + "'); </script></body></html>");

	}

	@Override 
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		logger.info( "get!" );
	}

}
