package com.aplos.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlEntityUtil {
	
	/*
	 * We should be able to use these methods to do most of this work
	 * Jakarta Commons Lang library's StringEscapeUtils.escapeHtml() and unescapeHtml() methods 
	 */
	
	/*
	 * This code is currently being used to encode text from the CKEditor
	 * and before going into the frontend views, pdfs and emails.
	 * There's currently an issue that it doesn't check the content for XML, to 
	 * overcome this we just ignore the " character, however for the long term
	 * this isn't suitable as other characters will be included in xml tags that we
	 * won't want to encode, we'll have to introduce and xml parser at some point and
	 * just encode the nodes.
	 */

	//changed encodingMapByUnicode from <Integer, CharacterEncoding> to <String, CharacterEncoding> as otherwise get by
	//entity name (String) never hit when it should - 18/5/12
	private static HashMap<String, CharacterEncoding> encodingMapByUnicode = new HashMap<String, CharacterEncoding>();
	private static HashMap<String, CharacterEncoding> encodingMapByEntity = new HashMap<String, CharacterEncoding>();
	private static HashMap<String, CharacterEncoding> encodingMapByCharacter = new HashMap<String, CharacterEncoding>();
	
	public enum EncodingType {
		CHARACTER,
		UNICODE,
		ENTITY;
	}

	private static CharacterEncoding[] characterEncodings;
	static {
		characterEncodings = new CharacterEncoding[]{
			new CharacterEncoding( "\"", "quot", 34 ),
			new CharacterEncoding( "&", "amp", 38 ),
			new CharacterEncoding( "'", "apos", 39 ),
			new CharacterEncoding( "<", "lt", 60 ),
			new CharacterEncoding( ">", "gt", 62 ),
			new CharacterEncoding( "€", "euro", 128 ),
			new CharacterEncoding( " ", "nbsp", 160 ),//Alt+0160
			new CharacterEncoding( "¡", "iexcl", 161 ), //inverted exclamation - not an i
			new CharacterEncoding( "¢", "cent", 162 ),
			new CharacterEncoding( "£", "pound", 163 ),
			new CharacterEncoding( "¤", "curren", 164 ),
			new CharacterEncoding( "¥", "yen", 165 ),
			new CharacterEncoding( "¦", "brvbar", 166 ),
			new CharacterEncoding( "§", "sect", 167 ),
			new CharacterEncoding( "¨", "uml", 168 ),
			new CharacterEncoding( "©", "copy", 169 ),//Alt+0169
			new CharacterEncoding( "ª", "ordf", 170 ),
			new CharacterEncoding( "«", "laquo", 171 ),
			new CharacterEncoding( "¬", "not", 172 ),
			//new CharacterEncoding( "", "shy", 173 ), //http://www.w3schools.com/tags/ref_entities.asp hasnt got the character to copy...
			new CharacterEncoding( "®", "reg", 174 ),
			new CharacterEncoding( "¯", "macr", 175 ),
			new CharacterEncoding( "°", "deg", 176 ),
			new CharacterEncoding( "±", "plusmn", 177 ),
			new CharacterEncoding( "²", "sup2", 178 ),
			new CharacterEncoding( "³", "sup3", 179 ),
			new CharacterEncoding( "´", "acute", 180 ),
			new CharacterEncoding( "µ", "micro", 181 ),
			new CharacterEncoding( "¶", "para", 182 ),
			new CharacterEncoding( "·", "middot", 183 ),
			new CharacterEncoding( "¸", "cedil", 184 ),
			new CharacterEncoding( "¹", "sup1", 185 ),
			new CharacterEncoding( "º", "ordm", 186 ),
			new CharacterEncoding( "»", "raquo", 187 ),
			new CharacterEncoding( "¼", "frac14", 188 ),
			new CharacterEncoding( "½", "frac12", 189 ),
			new CharacterEncoding( "¾", "frac34", 190 ),
			new CharacterEncoding( "¿", "iquest", 191 ),
			new CharacterEncoding( "À", "Agrave", 192 ),
			new CharacterEncoding( "Á", "Aacute", 193 ),
			new CharacterEncoding( "Â", "Acirc", 194 ),
			new CharacterEncoding( "Ã", "Atilde", 195 ),
			new CharacterEncoding( "Ä", "Auml", 196 ),
			new CharacterEncoding( "Å", "Aring", 197 ),
			new CharacterEncoding( "Æ", "AElig", 198 ),
			new CharacterEncoding( "Ç", "Ccedil", 199 ),
			new CharacterEncoding( "È", "Egrave", 200 ),
			new CharacterEncoding( "É", "Eacute", 201 ),
			new CharacterEncoding( "Ê", "Ecirc", 202 ),
			new CharacterEncoding( "Ë", "Euml", 203 ),
			new CharacterEncoding( "Ì", "Igrave", 204 ),
			new CharacterEncoding( "Í", "Iacute", 205 ),
			new CharacterEncoding( "Î", "Icirc", 206 ),
			new CharacterEncoding( "Ï", "Iuml", 207 ),
			new CharacterEncoding( "Ð", "ETH", 208 ),
			new CharacterEncoding( "Ñ", "Ntilde", 209 ),
			new CharacterEncoding( "Ò", "Ograve", 210 ),
			new CharacterEncoding( "Ó", "Oacute", 211 ),
			new CharacterEncoding( "Ô", "Ocirc", 212 ),
			new CharacterEncoding( "Õ", "Otilde", 213 ),
			new CharacterEncoding( "Ö", "Ouml", 214 ),
			new CharacterEncoding( "Ø", "Oslash", 216 ),
			new CharacterEncoding( "Ù", "Ugrave", 217 ),
			new CharacterEncoding( "Ú", "Uacute", 218 ),
			new CharacterEncoding( "Û", "Ucirc", 219 ),
			new CharacterEncoding( "Ü", "Uuml", 220 ),
			new CharacterEncoding( "Ý", "Yacute", 221 ),
			new CharacterEncoding( "Þ", "THORN", 222 ),
			new CharacterEncoding( "ß", "szlig", 223 ),
			new CharacterEncoding( "à", "agrave", 224 ),
			new CharacterEncoding( "á", "aacute", 225 ),
			new CharacterEncoding( "â", "acirc", 226 ),
			new CharacterEncoding( "ã", "atilde", 227 ),
			new CharacterEncoding( "ä", "auml", 228 ),
			new CharacterEncoding( "å", "aring", 229 ),
			new CharacterEncoding( "æ", "aelig", 230 ),
			new CharacterEncoding( "ç", "ccedil", 231 ),
			new CharacterEncoding( "è", "egrave", 232 ),
			new CharacterEncoding( "é", "eacute", 233 ),
			new CharacterEncoding( "ê", "ecirc", 234 ),
			new CharacterEncoding( "ë", "euml", 235 ),
			new CharacterEncoding( "ì", "igrave", 236 ),
			new CharacterEncoding( "í", "iacute", 237 ),
			new CharacterEncoding( "î", "icirc", 238 ),
			new CharacterEncoding( "ï", "iuml", 239 ),
			new CharacterEncoding( "ð", "eth", 240 ),
			new CharacterEncoding( "ñ", "ntilde", 241 ),
			new CharacterEncoding( "ò", "ograve", 242 ),
			new CharacterEncoding( "ó", "oacute", 243 ),
			new CharacterEncoding( "ô", "ocirc", 244 ),
			new CharacterEncoding( "õ", "otilde", 245 ),
			new CharacterEncoding( "ö", "ouml", 246 ),
			new CharacterEncoding( "ø", "oslash", 248 ),
			new CharacterEncoding( "ù", "ugrave", 249 ),
			new CharacterEncoding( "ú", "uacute", 250 ),
			new CharacterEncoding( "û", "ucirc", 251 ),
			new CharacterEncoding( "ü", "uuml", 252 ),
			new CharacterEncoding( "ý", "yacute", 253 ),
			new CharacterEncoding( "þ", "thorn", 254 ),
			new CharacterEncoding( "ÿ", "yuml", 255 ),
			new CharacterEncoding( "é", "eacute", 233 ),//Alt+0233
			new CharacterEncoding( "×", "times", 215 ),
			new CharacterEncoding( "÷", "divide", 247 ),
			new CharacterEncoding( "¦", "ndash", 8211 ),//Alt+2013
			new CharacterEncoding( "‘", "lsquo", 8216 ),//Alt+0145
			new CharacterEncoding( "’", "rsquo", 8217 ),//Alt+0146
			new CharacterEncoding( "“", "ldquo", 8220 ),//Alt+0147
			new CharacterEncoding( "”", "rdquo", 8221 ),//Alt+0148
			new CharacterEncoding( "·", "middot", 185 ),//Alt+0183
			new CharacterEncoding( "–", "ndash", 8211 ),//not an ordinary hyphen! (longer), rss feeds dont like it
			new CharacterEncoding( "→", "rarr", 8594 ),
			new CharacterEncoding( "→", "rarr", 8658 ),
			new CharacterEncoding( "•", "bull", 8226 ) //should be a double right arrow but renders as a square, used in nease
		};
		
		for( CharacterEncoding tempEncoding : characterEncodings ) {
			encodingMapByCharacter.put( tempEncoding.getCharacter(), tempEncoding );
			encodingMapByUnicode.put( tempEncoding.getUnicode().toString(), tempEncoding );
			encodingMapByEntity.put( tempEncoding.getEntity(), tempEncoding );
		}
	}

	public static String quotesToEntites( String content ) {
		if ( content != null ) {
			content = content.replaceAll("\\\"", "&quot;");
		}
		return content;
	}

	public static String entitiesToQuotes( String content ) {
		if ( content != null ) {
			content = content.replaceAll("&amp;quot;", "&quot;");
			content = content.replaceAll("&quot;", "\"");
		}
		return content;
	}

	public static String replaceEntitiesWith( String content, EncodingType encodingType ) {
		if( content != null && encodingType != null && !encodingType.equals( EncodingType.ENTITY ) ) {
			String replacementStr;
			CharacterEncoding characterEncoding;
			
			Pattern pattern = Pattern.compile("&amp;([a-zA-Z]{3,6});");
			Matcher m = pattern.matcher( content );
			List<String> matchedEntities = new ArrayList<String>();
			while( m.find() ) {
				String entityName = m.group( 1 );
				if( !matchedEntities.contains( entityName ) ) {  // make sure that replace isn't called more than once on the same entityName
					characterEncoding = encodingMapByEntity.get( entityName);
					if( characterEncoding != null ) {
						if( encodingType.equals( EncodingType.UNICODE ) ) {
							replacementStr = "&#" + String.valueOf( characterEncoding.getUnicode() ) + ";";
						} else {
							replacementStr = characterEncoding.getCharacter();
						}
						content = content.replaceAll( "&amp;" + entityName + ";", replacementStr );
					}
					matchedEntities.add( entityName );
				}
			}

			pattern = Pattern.compile("&([a-zA-Z]{3,6});");
			m = pattern.matcher( content );
			matchedEntities.clear();
			while( m.find() ) {
				String entityName = m.group( 1 );
				characterEncoding = encodingMapByEntity.get( entityName );
				if( !matchedEntities.contains( entityName ) ) {  // make sure that replace isn't called more than once on the same entityName
					if( encodingType.equals( EncodingType.UNICODE ) ) {
						replacementStr = "&#" + String.valueOf( characterEncoding.getUnicode() ) + ";";
					} else {
						replacementStr = characterEncoding.getCharacter();
					}
					if( encodingMapByEntity.containsKey( entityName ) ) {
						content = content.replaceAll( "&" + entityName + ";", replacementStr );
					}
					matchedEntities.add( entityName );
				}
			}
		}

		return content;
	}
	
	public static String replaceUnicodeWith( String content, EncodingType encodingType) {
		return replaceUnicodeWith( content, encodingType, false );
	}

	public static String replaceUnicodeWith( String content, EncodingType encodingType, boolean replaceMissingEntities ) {
		if( content != null && encodingType != null && !encodingType.equals( EncodingType.UNICODE ) ) {
			String replacementStr;
			CharacterEncoding characterEncoding;
			
			Pattern pattern = Pattern.compile("&amp;#([0-9]{3,6});");
			Matcher m = pattern.matcher( content );
			List<String> matchedEntities = new ArrayList<String>();
			while( m.find() ) {
				String entityName = m.group( 2 );
				if( !matchedEntities.contains( entityName ) ) {  // make sure that replace isn't called more than once on the same entityName
					characterEncoding = encodingMapByUnicode.get( entityName);
					if( characterEncoding != null ) {
						if( encodingType.equals( EncodingType.ENTITY ) ) {
							replacementStr = "&" + String.valueOf( characterEncoding.getEntity() ) + ";";
						} else {
							replacementStr = characterEncoding.getCharacter();
						}
						content = content.replaceAll( "&amp;#" + entityName + ";", replacementStr );
					} else if (replaceMissingEntities) {
						content = content.replaceAll( "&amp;#" + entityName + ";", "" );
					}
					matchedEntities.add( entityName );
				}
			}

			pattern = Pattern.compile("&#([0-9]{3,6});");
			m = pattern.matcher( content );
			matchedEntities.clear();
			while( m.find() ) {
				String entityName = m.group( 1 );
				characterEncoding = encodingMapByUnicode.get( entityName );
				if( characterEncoding != null && !matchedEntities.contains( entityName ) ) {  // make sure that replace isn't called more than once on the same entityName
					if( encodingType.equals( EncodingType.ENTITY ) ) {
						replacementStr = "&" + String.valueOf( characterEncoding.getEntity() ) + ";";
					} else {
						replacementStr = characterEncoding.getCharacter();
					}
					if( encodingMapByUnicode.containsKey( entityName ) ) {
						content = content.replaceAll( "&#" + entityName + ";", replacementStr );
					}
					matchedEntities.add( entityName );
				} else if (characterEncoding == null && replaceMissingEntities) {
					content = content.replaceAll( "&#" + entityName + ";", "" );
				}
			}
		}

		return content;
	}

	public static String stripTags( String content ) {
		if ( content != null ) {
			content = content.replaceAll("\\<.*?\\>", "");
		}
		return content;
	}

	public static String stripPTags( String content ) {
		if ( content != null ) {
			content = content.replaceAll("\\<\\/?p\\>", "");
		}
		return content;
	}

	public static String replaceCharactersWith( String content, EncodingType encodingType ) {
		return replaceCharactersWith(content, encodingType, true);
	}

	public static String replaceCharactersWith( String content, EncodingType encodingType, boolean ignoreTagDelimiters ) {
		if( content != null && encodingType != null && !encodingType.equals( EncodingType.CHARACTER ) ) {
			String replacementStr;
			for (CharacterEncoding tempEncoding : characterEncodings ) {
				// & character is checked after and " character is often in attributes tags so we just avoid this for now
				if( ignoreTagDelimiters && (tempEncoding.getCharacter().equals( "<" ) || tempEncoding.getCharacter().equals( ">" )) ) {
					continue;
				}
				if (content.contains(tempEncoding.getCharacter()) && !tempEncoding.getCharacter().equals( "&" ) && !tempEncoding.getCharacter().equals( "\"" ) ) {
					if( encodingType.equals( EncodingType.UNICODE ) ) {
						replacementStr = "&#" + String.valueOf( tempEncoding.getUnicode() );
					} else {
						replacementStr = "&" + String.valueOf( tempEncoding.getEntity() );
					}
					content = content.replaceAll(tempEncoding.getCharacter(), replacementStr + ";");
				}
			}

			Pattern pattern = Pattern.compile("&(?![a-zA-Z]{3,6};|#[0-9]{1,4};)");
			Matcher m = pattern.matcher( content );
			StringBuffer strBuf = new StringBuffer( content );
			CharacterEncoding ampCharacterEncoding = encodingMapByCharacter.get( "&" ); 
			while( m.find() ) {
				if( encodingType.equals( EncodingType.UNICODE ) ) {
					replacementStr = "&#" + String.valueOf( ampCharacterEncoding.getUnicode() );
				} else {
					replacementStr = "&" + String.valueOf( ampCharacterEncoding.getEntity() );
				}
				replacementStr += ";";
				strBuf.replace( m.start(), m.start() + 1, replacementStr );
				m = pattern.matcher( strBuf.toString() ); 
			}
			content = strBuf.toString();
		}
		return content;
	}

	public static String replaceCharactersAndEntitiesWithUnicode(String content) {
		content = replaceCharactersWith(content, EncodingType.UNICODE );
		content = replaceEntitiesWith(content, EncodingType.UNICODE);
		return content;
	}
	
	private static class CharacterEncoding {
		private String character;
		private String entity;
		private Integer unicode;
		
		public CharacterEncoding( String character, String entity, Integer unicode ) {
			this.setCharacter(character);
			this.setEntity(entity);
			this.setUnicode(unicode);
		}

		public String getCharacter() {
			return character;
		}

		public void setCharacter(String character) {
			this.character = character;
		}

		public String getEntity() {
			return entity;
		}

		public void setEntity(String entity) {
			this.entity = entity;
		}

		public Integer getUnicode() {
			return unicode;
		}

		public void setUnicode(Integer unicode) {
			this.unicode = unicode;
		}
	}
}