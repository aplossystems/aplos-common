package com.aplos.common.filters;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CacheHeaderFilter implements Filter {
  FilterConfig fc;

  @Override
public void doFilter(ServletRequest req,
                       ServletResponse res,
                       FilterChain chain)
                       throws IOException,
                              ServletException {
    HttpServletResponse response =
      (HttpServletResponse) res;
    // set the provided HTTP response parameters
    for (Enumeration e=fc.getInitParameterNames();
        e.hasMoreElements();) {
      String headerName = (String)e.nextElement();
      response.addHeader(headerName,
                 fc.getInitParameter(headerName));
    }
    addResourceCacheHeaders(response);
    // pass the request/response on
    chain.doFilter(req, response);
  }
  
  public static void addResourceCacheHeaders( HttpServletResponse response ) {
		Calendar cal = new GregorianCalendar();
		cal.setTime( new Date() );
		cal.add( Calendar.MONTH, 1 );
	    // Add an extra day also just so it's easier to see that the expires is different
	    // the actual date when testing through a browser.
		cal.add( Calendar.DAY_OF_YEAR, 1 );
		SimpleDateFormat sdf = new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss z" );
		response.addHeader( "Expires", sdf.format( cal.getTime() ) );
	    response.addHeader( "Cache-Control", "public, max-age=" + (3600 * 31) );
  }

  @Override
public void init(FilterConfig filterConfig) {
    this.fc = filterConfig;
  }
  @Override
public void destroy() {
    this.fc = null;
  }
}
