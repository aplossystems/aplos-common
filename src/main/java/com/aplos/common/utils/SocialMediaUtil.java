package com.aplos.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import com.aplos.common.FacebookPage;
import com.aplos.common.interfaces.FacebookPageHolder;

public class SocialMediaUtil {

	public static boolean twitterLoginSuccess() {
		
		String consumerKey = "uc0tmyLBOOYyKlwaHSagBpj3q";
		String consumerSecretKey = "VvriRZo7Pf8vcggTnfMDg4gifnReItVjcLB2ADlPIDN1V5Hgzk";
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL( "https://api.twitter.com/oauth/request_token" ).openConnection();
			String userCredentials = "OAuth oauth_callback=\"https://www.smsreserve.co.uk\", oauth_consumer_key=\"" + consumerKey + "\", oauth_nonce=\"b82da06a9f7151c3b165cf2d92c5c146\", oauth_signature=\"BChxcWGzjH0KruFrdRpvFb0Zs98%3D\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1425035316\", oauth_version=\"1.0\"";
			String basicAuth = new String(new Base64().encode(userCredentials.getBytes())).replace( "\n", "").replace("\r","");
			conn.setRequestProperty("Authorization", basicAuth);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			String pageSource = "";
			while ((line = reader.readLine()) != null) {
				pageSource = pageSource + line;
			}
			
		} catch( IOException ioex ) {
			ApplicationUtil.handleError( ioex );
		}
		return true;
	}

	public static boolean facebookLoginSuccess( String userId, String accessToken, FacebookPageHolder facebookPageHolder ) {
		try {	
			String longLivedAccessToken = getLongLivedAccessToken( accessToken );

			HttpURLConnection conn = (HttpURLConnection) new URL( "https://graph.facebook.com/" + userId + "/accounts?access_token=" + longLivedAccessToken ).openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			String pageSource = "";
			while ((line = reader.readLine()) != null) {
				pageSource = pageSource + line;
			}
			JSONArray jsonArray = (JSONArray) new JSONObject(pageSource).get( "data" );
			if( jsonArray.length() == 0 ) {
				if( facebookPageHolder.getFacebookPages().size() > 0 ) {
					facebookPageHolder.getFacebookPages().clear();
					facebookPageHolder.saveDetails();
				}
				facebookPageHolder.getFacebookPages().clear();
				JSFUtil.addMessage( "You don't have any pages linked to this facebook account" );
			} else {
				facebookPageHolder.getFacebookPages().clear();
				for( int i = 0, n = jsonArray.length(); i < n; i++ ) {
					JSONObject dataMap = (JSONObject) jsonArray.get( i );
					String pageId = (String) dataMap.get( "id" );
					String pageAccessToken = (String) dataMap.get( "access_token" );
					String pageName = (String) dataMap.get( "name" );
					FacebookPage facebookPage = new FacebookPage( pageId, pageName, pageAccessToken );
					facebookPage.saveDetails();
					facebookPageHolder.getFacebookPages().add( facebookPage );
					
					InputStream is;
					if (conn.getResponseCode() >= 400) {
					    is = conn.getErrorStream();
					} else {
					    is = conn.getInputStream();
					}
					reader = new BufferedReader(new InputStreamReader(is));
					pageSource = "";
					while ((line = reader.readLine()) != null) {
						pageSource = pageSource + line;
					}

					if (conn.getResponseCode() >= 400) {
						ApplicationUtil.handleError( new Exception( pageSource ), false );
					}
				}
				facebookPageHolder.saveDetails();
				StringBuffer strBuf = new StringBuffer( "You have added " );
				if( jsonArray.length() == 1 ) {
					strBuf.append( "1 facebook page " );
				} else {
					strBuf.append( jsonArray.length() + " facebook pages " );
				}
				strBuf.append( "to your account" );
				JSFUtil.addMessage( strBuf.toString() );
			}
			return true;
		} catch( IOException ioex ) {
			ApplicationUtil.handleError( ioex );
		} catch( JSONException jsonEx ) {
			ApplicationUtil.handleError( jsonEx );
		}
		return false;
	}
	
	public static String getLongLivedAccessToken( String accessToken ) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL( "https://graph.facebook.com/oauth/access_token?client_id=563666830430370&client_secret=0087dae1a27aba213cf6bb3e5a1851fc&grant_type=fb_exchange_token&fb_exchange_token=" + accessToken ).openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.setDoInput(true);
	//		DataOutputStream wr = new DataOutputStream (conn.getOutputStream ());
	//		wr.writeBytes(urlParameters);
	//		wr.flush ();
	//		wr.close ();
	
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			String pageSource = "";
			while ((line = reader.readLine()) != null) {
				pageSource = pageSource + line;
			}
			String[] keyValuePairs = pageSource.split( "&" );
			for( int i = 0, n = keyValuePairs.length; i < n; i++ ) {
				String[] keyAndValue = keyValuePairs[ i ].split( "=" );
				if( "access_token".equals( keyAndValue[ 0 ] ) ) {
					return keyAndValue[ 1 ];
				}
			}
		} catch( IOException ioex ) {
			ApplicationUtil.handleError( ioex );
		}
		return null;
	}
}
