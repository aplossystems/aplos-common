package com.aplos.common.filters;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aplos.common.utils.JSFUtil;

public class RedirectFilter implements Filter {
	FilterConfig fc;

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		boolean isResourceRequest = request.getRequestURI().contains( "/javax.faces.resource" );
		
		if( !isResourceRequest ) {
			ServletContext servletContext = request.getServletContext();
			
	
			if( request.getCookies() != null ) {
				Cookie jSessionCookie = null;
				Cookie jSessionBackupCookie = null;
				for( Cookie tempCookie : request.getCookies() ) {
					if( tempCookie.getName().equals( "JSESSIONID" ) ) {
						jSessionCookie = tempCookie;
						break;
					} else if( tempCookie.getName().equals( "JSESSIONID_BK" ) ) {
						jSessionBackupCookie = tempCookie;
						break;
					}	
				}
				if( jSessionCookie == null && jSessionBackupCookie != null ) {
					try {
						Field innerRequestfield = RequestFacade.class.getDeclaredField( "request" );
						innerRequestfield.setAccessible(true);
						Request innerRequest = (Request) innerRequestfield.get( request );
						Field requestedSessionIdField = Request.class.getDeclaredField( "requestedSessionId" );
						requestedSessionIdField.setAccessible(true);
						Object oldValue = requestedSessionIdField.get( innerRequest );
						requestedSessionIdField.set( innerRequest, jSessionBackupCookie.getValue() );
						HttpSession session = request.getSession( false );
						if( session != null && session.getId().equals( jSessionBackupCookie.getValue() ) ) {
							response.addHeader( "Set-Cookie", "JSESSIONID=" + jSessionBackupCookie.getValue() + "; Path=" + JSFUtil.getContextPath() + "/; HttpOnly" );
							System.out.println( "Cookie set " + jSessionBackupCookie.getValue() );
						} else {
							requestedSessionIdField.set( innerRequest, oldValue );
							System.out.println( "Set cookie failed " + jSessionBackupCookie.getValue() );
							if( session != null ) {
								System.out.println( "Pulled session " + session.getId() );
							}
						}
					} catch( Exception ex ) {
						ex.printStackTrace();
					}
				}
			}
	
			List<SiteRedirect> siteRedirectList = (List<SiteRedirect>) servletContext.getAttribute("siteRedirects");
			if (siteRedirectList == null) {
				loadSiteRedirects(servletContext);
				siteRedirectList = (List<SiteRedirect>) servletContext.getAttribute("siteRedirects");
			}
	
			String serverName = request.getServerName();
			SiteRedirect siteRedirect = null;
			for( SiteRedirect tempSiteRedirect : siteRedirectList ) {
				if( serverName.startsWith( tempSiteRedirect.getSourceSiteUrl() ) ) {
					siteRedirect = tempSiteRedirect;
					break;
				}
			}
	
			if( siteRedirect != null ) {
				String oldUrl = request.getRequestURI();
				StringBuffer urlBuf = new StringBuffer( oldUrl );
				if( request.getQueryString() != null ) {
					urlBuf.append( "?" ).append( request.getQueryString() );
				}
				if( !oldUrl.startsWith( "/" ) ) {
					urlBuf.insert( 0, "/" );
				}
				urlBuf.insert( 0, siteRedirect.getDestinationSiteUrl() ).insert( 0, "http://" );
				response.setContentType("text/html");
				response.setDateHeader("Expires", 0);
				response.setHeader("Location", urlBuf.toString() );
				response.setStatus(301);
			} else {
				List<UrlRedirect> urlRedirectList = (List<UrlRedirect>) servletContext.getAttribute("urlRedirects");
				String requestUrl = request.getRequestURI();
				if( request.getQueryString() != null ) {
					requestUrl = requestUrl +  "?" + request.getQueryString();
				}
				if( requestUrl.startsWith( JSFUtil.getContextPath() ) ) {
					requestUrl = requestUrl.replaceFirst( JSFUtil.getContextPath(), "" );
				}
				UrlRedirect urlRedirect = null;
				for( UrlRedirect tempUrlRedirect : urlRedirectList ) {
					if( requestUrl.matches( tempUrlRedirect.getSourceUrl() ) ) {
						urlRedirect = tempUrlRedirect;
						break;
					}
				}
				
				if( urlRedirect != null ) {
					StringBuffer destinationUrlBuf = new StringBuffer( urlRedirect.getDestinationUrl() );
					if( !urlRedirect.getDestinationUrl().startsWith( "/" ) ) {
						destinationUrlBuf.insert( 0 , "/" ); 
					}
					destinationUrlBuf.insert( 0, JSFUtil.getContextPath() );
					response.setContentType("text/html");
					response.setDateHeader("Expires", 0);
					response.setHeader("Location", destinationUrlBuf.toString());
					response.setStatus(301);
				} else {
					chain.doFilter(request, response);
				}
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	public void loadSiteRedirects(ServletContext servletContext) {
		List<SiteRedirect> siteRedirectList = new ArrayList<SiteRedirect>();
		List<UrlRedirect> urlRedirectList = new ArrayList<UrlRedirect>();
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			// URL url = SagePayConfigurationDetails.class.getResource(
			// srcContextRoot +
			// "resources/payment/sagePayConfigurationDetails.xml");
			URL url = getClass().getResource( "/../siteRedirects.xml" );
			if (url != null) {
				Document doc = docBuilder.parse(url.openStream());
				doc.getDocumentElement().normalize();

				NodeList nodeList = doc.getElementsByTagName( "siteRedirect" );
				SiteRedirect tempSiteRedirect;
				String tempSourceSiteUrl;
				String tempDestinationSiteUrl;
				for( int i = 0, n = nodeList.getLength(); i < n; i++ ) {
					tempSourceSiteUrl = getFirstElementValueOrEmpty((Element)nodeList.item( i ), "sourceSite" );
					tempDestinationSiteUrl = getFirstElementValueOrEmpty((Element)nodeList.item( i ), "destinationSite");
					tempSiteRedirect = new SiteRedirect( tempSourceSiteUrl, tempDestinationSiteUrl );
					siteRedirectList.add( tempSiteRedirect );
				}

				nodeList = doc.getElementsByTagName( "urlRedirect" );
				UrlRedirect tempUrlRedirect;
				String tempSourceUrl;
				String tempDestinationUrl;
				for( int i = 0, n = nodeList.getLength(); i < n; i++ ) {
					tempSourceUrl = getFirstElementValueOrEmpty((Element)nodeList.item( i ), "sourceUrl" );
					tempDestinationUrl = getFirstElementValueOrEmpty((Element)nodeList.item( i ), "destinationUrl");
					tempUrlRedirect = new UrlRedirect( tempSourceUrl, tempDestinationUrl );
					urlRedirectList.add( tempUrlRedirect );
				}
			}
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();

		} catch (Throwable t) {
			t.printStackTrace();
		}
		servletContext.setAttribute("siteRedirects",siteRedirectList);
		servletContext.setAttribute("urlRedirects",urlRedirectList);
	}

	public static String getFirstElementValueOrEmpty( Element element, String elementName ) {
		NodeList nodeList = element.getElementsByTagName( elementName );
		if( nodeList.getLength() > 0 ) {
			if( nodeList.item( 0 ).getFirstChild() != null ) {
				return nodeList.item( 0 ).getFirstChild().getNodeValue();
			}
		} 

		return "";
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.fc = filterConfig;
	}

	@Override
	public void destroy() {
		this.fc = null;
	}

	private class SiteRedirect {
		private String sourceSiteUrl;
		private String destinationSiteUrl;

		public SiteRedirect( String sourceSiteUrl, String destinationSiteUrl ) {
			this.sourceSiteUrl = sourceSiteUrl;
			this.destinationSiteUrl = destinationSiteUrl;
		}

		public String getSourceSiteUrl() {
			return sourceSiteUrl;
		}

		public void setSourceSiteUrl(String sourceSiteUrl) {
			this.sourceSiteUrl = sourceSiteUrl;
		}

		public String getDestinationSiteUrl() {
			return destinationSiteUrl;
		}

		public void setDestinationSiteUrl(String destinationSiteUrl) {
			this.destinationSiteUrl = destinationSiteUrl;
		}

	}

	private class UrlRedirect {
		private String sourceUrl;
		private String destinationUrl;

		public UrlRedirect( String sourceUrl, String destinationUrl ) {
			this.setSourceUrl(sourceUrl);
			this.setDestinationUrl(destinationUrl);
		}

		public String getSourceUrl() {
			return sourceUrl;
		}

		public void setSourceUrl(String sourceUrl) {
			this.sourceUrl = sourceUrl;
		}

		public String getDestinationUrl() {
			return destinationUrl;
		}

		public void setDestinationUrl(String destinationUrl) {
			this.destinationUrl = destinationUrl;
		}

	}
}
