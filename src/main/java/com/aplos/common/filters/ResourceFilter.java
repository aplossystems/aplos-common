package com.aplos.common.filters;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;

import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UrlEncrypter;

public class ResourceFilter implements Filter {

	FilterConfig fc;
	public static final String RESOURCES_PATH = "resources";
	private Logger logger = Logger.getLogger(getClass());

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		String resourceName = ((HttpServletRequest)servletRequest).getRequestURI();
		String contextPath = ((HttpServletRequest)servletRequest).getContextPath();
		resourceName = resourceName.replaceFirst(contextPath, "");
		//check for css resources written to file
		URL url = ApplicationUtil.getAplosModuleFilterer().getResourceFilterUrl(resourceName,contextPath);
		if (url != null) {
			copyFileContentsToOutputStream((HttpServletResponse)servletResponse, resourceName, url, logger);
		} else {
			if (resourceName.contains("/content/") || resourceName.contains("/media/") || resourceName.equalsIgnoreCase("/sitemap.xml")) {
				filterChain.doFilter(servletRequest, servletResponse);
				//we want css files in the CMS_PAGE_REVISION_CSS_FILES dir to go through template resolver
			} else { //if (!resourceName.startsWith("/" + CmsWorkingDirectory.CMS_PAGE_REVISION_CSS_FILES.getDirectoryPath().replace(CmsWorkingDirectory.CMS_PAGE_REVISION_VIEW_FILES.getDirectoryPath(),""))) {
				if( !resourceName.startsWith( "/" ) ) {
		    		resourceName = "/" + resourceName;
		    	}
		    	if ( resourceName.toLowerCase().endsWith(".jpg") || resourceName.toLowerCase().endsWith(".gif") || resourceName.toLowerCase().endsWith(".png") ||
		    			resourceName.toLowerCase().endsWith( ".js" ) || resourceName.toLowerCase().endsWith( ".css" ) || resourceName.toLowerCase().endsWith( ".jar" ) ||
		    			resourceName.toLowerCase().endsWith( ".xml" ) || resourceName.toLowerCase().endsWith( ".doc" ) || resourceName.toLowerCase().endsWith( ".pdf" )
		    			|| resourceName.toLowerCase().endsWith( ".html" ) || resourceName.toLowerCase().endsWith( ".json" ) || resourceName.toLowerCase().endsWith( ".ico" ) ) {
			    		// write the file

					if( servletRequest.getParameter( AplosAppConstants.EMAIL_TRACKING ) != null
		    				&& servletRequest.getParameter( AplosScopedBindings.ID ) != null ) {
						try {
							String trackingCode = servletRequest.getParameter( AplosAppConstants.EMAIL_TRACKING );
			    			String aplosEmailIdStr = servletRequest.getParameter( AplosScopedBindings.ID );
			    			try {
			    				Long aplosEmailId = Long.parseLong( aplosEmailIdStr );
			    				AplosEmail aplosEmail = new BeanDao( AplosEmail.class ).get( aplosEmailId );
			    				
			    				if( aplosEmail != null ) {
					    			UrlEncrypter urlEncrypter = aplosEmail.createUrlEncrypter();
					    			String idStr = urlEncrypter.decrypt(trackingCode);
				    				Long singleEmailRecordId = Long.parseLong( idStr );
				    				SingleEmailRecord singleEmailRecord = new BeanDao( SingleEmailRecord.class ).get( singleEmailRecordId );
				    				if( singleEmailRecord != null ) {
					    				singleEmailRecord = singleEmailRecord.getSaveableBean();
					    				if( "a".equals( servletRequest.getParameter( AplosAppConstants.TRACKING_TYPE ) ) ) {
					    					singleEmailRecord.setActionedDate(new Date());
					    				} else {
					    					singleEmailRecord.setOpenedDate(new Date());
					    				}
					    				singleEmailRecord.saveDetails();
				    				}
			    				}
			    			} catch( NumberFormatException nfex ) {
			    				ApplicationUtil.handleError( nfex );
			    			}
							
							if( ApplicationUtil.getPersistenceContext().getConnection() != null ) {
								ApplicationUtil.getPersistenceContext().getConnection().commit();
							}
						} catch( SQLException sqlex ) {
							ApplicationUtil.handleError(sqlex);
						}
					}
					
		    		writeFile((HttpServletResponse)servletResponse, resourceName, contextPath, logger);
		        } else {
		    		filterChain.doFilter(servletRequest, servletResponse);
		    	}
			}
		}
	}

	@Override
	public void init(FilterConfig fc) throws ServletException {
		this.fc = fc;
	}

	@Override
	public void destroy() {
		this.fc = null;
	}

	public static void writeFile(HttpServletResponse response, String resourceName, String contextPath, Logger logger) {
    	URL url = JSFUtil.checkFileLocations( resourceName, RESOURCES_PATH, true );
    	if( url == null ) {
    		return;
    	}
    	
    	if ( url.toString().contains( "/com/aplos/" ) && url.toString().endsWith(".css")) {
    		/*
    		 * Does this get used anymore?  The jsf resources should be handling the .css files that 
    		 * need processing now, although it's possible imported css will still need this. A.M. 24 April 2012.
    		 */
    		String lineSep = System.getProperty("line.separator");
     	    BufferedReader br = null;
     	    try {
     	    	br = new BufferedReader(new InputStreamReader( url.openStream() ) );
     	    } catch (FileNotFoundException e2) {
     	    	e2.printStackTrace();
     	    } catch( IOException ioEx ) {
     	    	ioEx.printStackTrace();
     	    }
     	   String nextLine = "";
     	   StringBuffer sb = new StringBuffer();
     	   try {
     		   while ((nextLine = br.readLine()) != null) {
     			   sb.append(nextLine);
     			   sb.append(lineSep);
 			   }
     	   } catch (IOException e1) {
     		   e1.printStackTrace();
     	   }
     	   String cssContent = sb.toString();

     	   String newCssContent = CommonUtil.includeContextRootInPathsForCss(cssContent, contextPath);
     	   
     	   if (! ( cssContent.equals(newCssContent) ) ) {
				InputStream is = new ByteArrayInputStream( newCssContent.getBytes(Charset.defaultCharset()) );

				try {
				    String mimeType = Files.probeContentType(Paths.get(resourceName));
				    response.setContentType(mimeType);
				    response.setStatus(200);

				    // Copy the contents of the file to the output stream
				    byte[] buf = new byte[1024];
				    int count = 0;
				    while ((count = is.read(buf)) >= 0) {
				        response.getOutputStream().write(buf, 0, count);
				    }
				    response.getOutputStream().close();
				    br.close();
				} catch ( ClientAbortException cae ) {
					logger.debug("Aborting transfer.  it happens, no problem.", cae);
				} catch ( SocketException cae ) {
					logger.debug("Aborting transfer.  it happens, no problem.", cae);
				} catch (Exception e) {
				    String message = null;
				    message = "Can't load image file:" + url.toExternalForm();
//				        try {
//				            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
//				        } catch (IOException f) {
				    e.printStackTrace();
//				        }
				}

     	   }
     	   else {
     		   copyFileContentsToOutputStream(response, resourceName, url, logger);
     	   }
    	}

    	else {
    		copyFileContentsToOutputStream(response, resourceName, url, logger);
    	}
    }

    public static void copyFileContentsToOutputStream(HttpServletResponse response, String resourceName, URL url, Logger logger) {
    	URLConnection conn = null;
        InputStream stream = null;

        try {
            conn = url.openConnection();
            conn.setUseCaches(false);
            stream = conn.getInputStream();

            String mimeType = Files.probeContentType(Paths.get(resourceName));
            response.setContentType(mimeType);
            response.setStatus(200);

            // Copy the contents of the file to the output stream
            byte[] buf = new byte[1024];
            int count = 0;

            response.getCharacterEncoding();
            response.setCharacterEncoding("UTF-8");
            while ((count = stream.read(buf)) >= 0) {
                response.getOutputStream().write(buf, 0, count);
            }
            response.getOutputStream().close();
        } catch ( ClientAbortException cae ) {
        	logger.debug("Aborting transfer.  it happens, no problem.", cae);
        } catch (Exception e) {
            String message = null;
            message = "Can't load image file:" + url.toExternalForm();
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            } catch (IOException f) {
                f.printStackTrace();
            }
        }
    }

}
