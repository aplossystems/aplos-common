package com.aplos.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Scraper {
	private String pageSource;
	private List<String> cookies = new ArrayList<String>();
	private Matcher currentMatcher;
	private String referer;
	private String contentType;
	private String keepAlive;
	private String connection;
	private String host;
	private boolean isUsingGZipInputStream = true;
	private boolean isInstanceFollowRedirects = true;
	
	public Scraper() {
	
	}

	public Scraper(String url) throws IOException {
		openConnection(url);
	}

	public Scraper(String url, String data) throws IOException {
		openConnection(url, data);
	}

	public void openConnection(String url, String postData) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0");
		if( getReferer() != null ) {
			conn.setRequestProperty("Referer", getReferer() );
		}
		if( getHost() != null ) {
			conn.setRequestProperty("Host", getHost() );
		}
		if( getContentType() != null ) {
			conn.setRequestProperty("Content-Type", getContentType() );
		}
		if( getKeepAlive() != null ) {
			conn.setRequestProperty("Keep-Alive", getKeepAlive() );
		}
		if( getConnection() != null ) {
			conn.setRequestProperty("Connection", getConnection() );
		}
		conn.setRequestProperty( "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty( "Accept-Encoding", "gzip, deflate");
		conn.setRequestProperty( "Accept-Language", "en-gb,en;q=0.5");
		conn.setInstanceFollowRedirects(isInstanceFollowRedirects());

		byte[] postDataBytes = null; 
		if( postData != null ) {
			postDataBytes = postData.getBytes();
			conn.setRequestProperty("Content-Length", String.valueOf( postDataBytes.length ) );
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
		}

		String cookie = getCookieString();
		if (!cookie.equals("")) {
			conn.setRequestProperty("Cookie", cookie);
		}

		if( postData != null ) {
			OutputStream ostream = conn.getOutputStream();
			postData += "\r";
			ostream.write(postDataBytes);
			ostream.flush();
			ostream.close();
		}

		saveCookies(conn);

		getSource(conn);
	}

	public void openConnection(String url) throws IOException {
		openConnection( url, null );
	}

	private void getSource(HttpURLConnection conn) throws IOException {		
		InputStream inputStream = conn.getInputStream();
		if( isUsingGZipInputStream() ) {
			inputStream = new GZIPInputStream(inputStream);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		setPageSource("");
		while ((line = reader.readLine()) != null) {
			setPageSource(getPageSource() + line);
		}
	}

	private String getCookieString() {
		String cookie = "";
		boolean first = true;
		for (String s : cookies) {
			if (!first) {
				cookie += "; ";
			}
			cookie += s;

			if (first) {
				first = false;
			}
		}
		return cookie;
	}

	@SuppressWarnings("unused")
	private void saveCookies(URLConnection conn) {
		for (int i = 0;; i++) {
			String headerName = conn.getHeaderFieldKey(i);
			String headerValue = conn.getHeaderField(i);

		//	System.out.println(headerName + ": " + headerValue + "\n");

			if (headerName == null && headerValue == null) {
				// No more headers
				break;
			}
			if ("Set-Cookie".equalsIgnoreCase(headerName)) {
				// Parse cookie
				String[] fields = headerValue.split(";\\s*");

				String cookieValue = fields[0];
				String expires = null;
				String path = null;
				String domain = null;
				boolean secure = false;

				// Parse each field
				for (int j = 1; j < fields.length; j++) {
					if ("secure".equalsIgnoreCase(fields[j])) {
						secure = true;
					} else if (fields[j].indexOf('=') > 0) {
						String[] f = fields[j].split("=");
						if ("expires".equalsIgnoreCase(f[0])) {
							expires = f[1];
						} else if ("domain".equalsIgnoreCase(f[0])) {
							domain = f[1];
						} else if ("path".equalsIgnoreCase(f[0])) {
							path = f[1];
						}
					}
				}

				cookies.add(cookieValue);
			}
		}
	}

	public String getMatch(String regex) {
		return getMatch(regex, 1);
	}

	public String getMatch(String regex, int number) {
		currentMatcher = Pattern.compile(regex).matcher(getPageSource());
		if (currentMatcher.find()) {
			return currentMatcher.group(number);
		}

		return null;
	}

	public Matcher getMatcher(String regex) {
		return Pattern.compile(regex).matcher(getPageSource());
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(String keepAlive) {
		this.keepAlive = keepAlive;
	}

	public String getConnection() {
		return connection;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	public String getPageSource() {
		return pageSource;
	}

	public void setPageSource(String pageSource) {
		this.pageSource = pageSource;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public boolean isUsingGZipInputStream() {
		return isUsingGZipInputStream;
	}

	public void setUsingGZipInputStream(boolean isUsingGZipInputStream) {
		this.isUsingGZipInputStream = isUsingGZipInputStream;
	}

	public boolean isInstanceFollowRedirects() {
		return isInstanceFollowRedirects;
	}

	public void setInstanceFollowRedirects(boolean isInstanceFollowRedirects) {
		this.isInstanceFollowRedirects = isInstanceFollowRedirects;
	}

}
