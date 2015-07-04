package com.aplos.common.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aplos.common.beans.SystemUser;
import com.aplos.common.enums.WebServiceCallTypes;

/* Utility class to simplify communicating with Networking Ease or other web services */

@ManagedBean
public class WebServiceUtil {
	private static Logger logger = Logger.getLogger( WebServiceUtil.class );

	public static Document contactNease(WebServiceCallTypes callType, Map<String, String> params) throws NetworkingEaseWebServiceException {
		return contactNease(callType, params, null, null);
	}

	public static Document contactNease(WebServiceCallTypes callType, Map<String, String> params, SystemUser systemUser) throws NetworkingEaseWebServiceException {
		if (systemUser != null) {
			return contactNease(callType, params, systemUser.getNeaseUsername(), systemUser.getNeasePassword());
		} else {
			return contactNease(callType, params, null, null);
		}
	}

	public static Document contactNease(WebServiceCallTypes callType, SystemUser systemUser) throws NetworkingEaseWebServiceException {
		if (systemUser != null) {
			return contactNease(callType, new HashMap<String, String>(), systemUser.getNeaseUsername(), systemUser.getNeasePassword());
		} else {
			return contactNease(callType, new HashMap<String, String>(), null, null);
		}
	}

	public static Document contactNease(WebServiceCallTypes callType) throws IOException, ParserConfigurationException, SAXException {
		return contactNease(callType, new HashMap<String, String>(), null, null);
	}

	public static Document contactNease(WebServiceCallTypes callType, Map<String, String> params, String neaseUsername, String neasePassword) throws NetworkingEaseWebServiceException {
		try {
			StringBuffer parsedParams = new StringBuffer("token=" + getNeaseToken() + "&callType=" + callType.toString());
			if (neaseUsername != null) {
				parsedParams.append("&neaseUsername=");
				parsedParams.append(URLEncoder.encode(neaseUsername, "UTF-8"));
				if (neasePassword != null) {
					parsedParams.append("&neasePassword=");
					parsedParams.append(URLEncoder.encode(neaseUsername, "UTF-8"));
				}
			}
			Iterator<Entry<String, String>> paramIterator = params.entrySet().iterator();
		    while (paramIterator.hasNext()) {
		        Map.Entry<String, String> param = paramIterator.next();
		        parsedParams.append("&");
		        parsedParams.append(param.getKey());
		        parsedParams.append("=");
		        parsedParams.append(URLEncoder.encode(param.getValue(), "UTF-8"));
			}
			HttpURLConnection conn = (HttpURLConnection) new URL(getNeaseUrl() + "webService?" + parsedParams.toString()).openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			DataOutputStream wr = new DataOutputStream (conn.getOutputStream ());
			if (ApplicationUtil.getAplosContextListener().isDebugMode()){
				logger.info("Nease Request Url : " + conn.getURL().toString());
				logger.info("Nease Params : " + parsedParams.toString());
			}
		    wr.writeBytes(parsedParams.toString());
		    wr.flush();
		    wr.close();

			//RESPONSE:
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(conn.getInputStream());
			//Parse the xml back into items
			NodeList nodes = doc.getElementsByTagName("error");
			if (nodes.item(0) != null) {
				JSFUtil.addMessageForError("Nease Web Service : " + nodes.item(0).getTextContent());
				throw new NetworkingEaseWebServiceException(nodes.item(0).getTextContent());  //its an IOException so don't need a separate throws declaration in existing code
			} else {
				//else we have a valid response
				return doc;
			}
		} catch (UnsupportedEncodingException e) {
			throw new NetworkingEaseWebServiceException(e);
		} catch (MalformedURLException e) {
			throw new NetworkingEaseWebServiceException(e);
		} catch (IOException e) {
			throw new NetworkingEaseWebServiceException(e);
		} catch (SAXException e) {
			throw new NetworkingEaseWebServiceException(e);
		} catch (ParserConfigurationException e) {
			throw new NetworkingEaseWebServiceException(e);
		}
	}

	public static String getNeaseUrl() {
		String url = "https://";

		if( JSFUtil.isLocalHost() ) {
			url += "localhost:8080/NetworkingEase/";
		} else {
			url += "app.networkingease.co.uk/";
		}

		return url;
	}

	public static String getNeaseToken() {
		return "JKHDAIU23756UHV423";
	}

	public static class NetworkingEaseWebServiceException extends IOException {

		private static final long serialVersionUID = 7717020413072003221L;

		public NetworkingEaseWebServiceException(String message) {
			super(message);
		}

		public NetworkingEaseWebServiceException(String message, Throwable cause) {
			super(message, cause);
		}

		public NetworkingEaseWebServiceException(Throwable cause) {
			super(cause);
		}

	}

}
