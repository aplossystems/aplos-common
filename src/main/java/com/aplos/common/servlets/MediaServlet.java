
package com.aplos.common.servlets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aplos.common.listeners.AplosContextListener;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.aplos.common.JpegReader;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.filters.CacheHeaderFilter;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.ImageUtil;
import com.aplos.common.utils.JSFUtil;
import com.lowagie.text.pdf.Barcode39;
import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleOp;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.SeekableStream;

public class MediaServlet extends HttpServlet {
	
	private static final long serialVersionUID = 6155809383121572074L;
	private Logger logger = Logger.getLogger( getClass() );
	
	public enum MediaFileType {
		IMAGE,
		FLASH,
		PDF,
		MISC,
		SOUND,
		BARCODE,
		LIVEDRIVE_HOSTED_IMAGE;
	}

	@Override
	protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		try {
			String filePath = request.getParameter( AplosAppConstants.FILE_NAME );
			String type = request.getParameter( AplosAppConstants.TYPE );
			boolean fileRestricted = false;
			FileDetails fileDetails = null;
			String fileDetailsId = request.getParameter( AplosAppConstants.FILE_DETAILS_ID );
			if( fileDetailsId != null ) {
				fileDetails = null;
				try {
					fileDetails = (FileDetails) new BeanDao(FileDetails.class).get(Long.parseLong(fileDetailsId));
				} catch (NumberFormatException nfex) {
					ApplicationUtil.getAplosContextListener().handleError(nfex);
				}
			}

			if (fileDetails == null) {
				logger.info("Access denied for " + filePath + " no file details found");
				response.setStatus(HttpStatus.SC_FORBIDDEN);
				return;
			}

//			if (filePath != null) {
//				String accessKey = request.getParameter( AplosAppConstants.ACCESS_KEY );
//				if (accessKey == null || !accessKey.equals(fileDetails.getAccessKey())) {
//					logger.info("Access denied for " + filePath + " incorrect access key");
//					response.setStatus(HttpStatus.SC_FORBIDDEN);
//					return;
//				}
//
//				filePath = URLDecoder.decode( filePath, "UTF-8" );
//				if( filePath.indexOf( "?" ) > -1 ) {
//					filePath = filePath.substring( 0, filePath.indexOf( "?" ) );
//				}
//
//				if( type == null ) {
//					type = getFileType( filePath );
//				}
//
//				if( filePath != null ) {
////					if (restrictedUrlsCached == null) {
////						restrictedUrlsCached = ApplicationUtil.getAplosModuleFilterer().getRestrictedMediaPaths();
////					}
////					for (String restrictedPath : restrictedUrlsCached) {
////						if (filePath.startsWith(restrictedPath)) {
//
//					//TODO: SESSION/WINDOWID CHECK
////							String sessionId = request.getParameter( AplosAppConstants.SESSION_ID );
////							if (sessionId != null && !sessionId.equals("")) {
////								HttpSession session = //GET IT SOMEHOW
////								String userIdString = request.getParameter( AplosAppConstants.USER_ID );
////								if (userIdString != null && !userIdString.equals("")) {
////
////									Long userId = Long.parseLong(userIdString);
////									SystemUser currentUser = (SystemUser) session.getAttribute(CommonUtil.getBinding(SystemUser.class));
////									if (currentUser != null && currentUser.isLoggedIn() && currentUser.getId().equals(userId)) {
////										fileRestricted=false;
////									} else {
////										fileRestricted=true;
////									}
//					//
////								} else {
////									fileRestricted=true;
////								}
////
////							} else {
////								fileRestricted=true;
////							}
//
////							break;
////						}
////					}
//					if( type == null ||
//							type.equalsIgnoreCase( MediaFileType.IMAGE.name() ) ||
//							type.equalsIgnoreCase( MediaFileType.LIVEDRIVE_HOSTED_IMAGE.name() )) {
//						writeImage( false, filePath, response, request, fileRestricted );
//					} else if( MediaFileType.FLASH.name().equalsIgnoreCase( type ) ||
//							MediaFileType.SOUND.name().equalsIgnoreCase( type ) ||
//							MediaFileType.MISC.name().equalsIgnoreCase( type ) ||
//							MediaFileType.PDF.name().equalsIgnoreCase( type ) ||
//							MediaFileType.SOUND.name().equalsIgnoreCase( type ) ) {
//						writeFile( new File( CommonUtil.appendServerWorkPath( filePath ) ), filePath, response, request, fileRestricted );
//					}
//				} else if( MediaFileType.BARCODE.name().equalsIgnoreCase( type ) ) {
//					writeBarcode( filePath, response, request );
//				}
//			} else if( fileDetails != null ) {

				String accessKey = request.getParameter( AplosAppConstants.ACCESS_KEY );
				if (fileDetails.getFileDetailsOwner() == null || fileDetails.getFileDetailsOwner().allowFileAccess(JSFUtil.getLoggedInUser(request.getSession()))
					|| (accessKey != null && accessKey.equals(fileDetails.getAccessKey()))) {
					if (type == null) {
						type = getFileType(fileDetails.getFilename());
					}

					if (type == null ||
							type.equalsIgnoreCase(MediaFileType.IMAGE.name()) ||
							type.equalsIgnoreCase(MediaFileType.LIVEDRIVE_HOSTED_IMAGE.name())) {
						writeImage(fileDetails, response, request, false);
					} else if (MediaFileType.FLASH.name().equalsIgnoreCase(type) ||
							MediaFileType.SOUND.name().equalsIgnoreCase(type) ||
							MediaFileType.MISC.name().equalsIgnoreCase(type) ||
							MediaFileType.PDF.name().equalsIgnoreCase(type) ||
							MediaFileType.SOUND.name().equalsIgnoreCase(type)) {
						writeFile(fileDetails.getFile(), fileDetails.getName(), response, request, fileRestricted);
					}
				} else {
					logger.info("Access denied for fileDetails " + fileDetails.getId());
					response.setStatus(HttpStatus.SC_UNAUTHORIZED);
				}
//			}
		}
		catch( UnsupportedEncodingException usee ) {
			logger.warn( usee );
		}
	}
	
	public String getFileType( String filename ) {
		if( filename.endsWith( ".jpg" ) || filename.endsWith( ".jpeg" ) || filename.endsWith( ".png" ) || filename.endsWith( ".gif" ) ) {
			return MediaFileType.IMAGE.name();
		} else {
			return MediaFileType.MISC.name();
		}
	}

	private void writeBarcode( String filename, HttpServletResponse response, HttpServletRequest request ) throws FileNotFoundException, IOException {	
		Barcode39 code39 = new Barcode39();
		code39.setCode( request.getParameter( AplosAppConstants.BARCODE_TEXT ));
		// This sets the widths of the bars, this needs to be at least 3 thick from previous
		// experience otherwise some scanners won't pick it up.
		code39.setN( 3 );
		Image barcodeImage = code39.createAwtImage( Color.BLACK, Color.WHITE );
		if( request.getParameter( AplosAppConstants.BARCODE_WIDTH ) != null ) {
			int barcodeWidth = Integer.parseInt( request.getParameter( AplosAppConstants.BARCODE_WIDTH ) );
			barcodeImage = barcodeImage.getScaledInstance( barcodeWidth, -1, Image.SCALE_SMOOTH );
		}
		BufferedImage bufImage = ImageUtil.bufferImage( barcodeImage, BufferedImage.TYPE_BYTE_GRAY );
		response.setContentType( "image/jpg" );
		response.setHeader("Content-Disposition", "inline;");
		ImageIO.write( bufImage, "jpg", response.getOutputStream() );
	}

	public static String getBarcodeUrl( String barcodeText ) throws UnsupportedEncodingException {
		String contextRoot = FormatUtil.stripFirstAndLastSlashes( JSFUtil.getContextPath( true ) );
		if( !contextRoot.equals( "" ) ) {
			contextRoot = "/" + contextRoot;
		}
		String serverUrl = FormatUtil.stripFirstAndLastSlashes(JSFUtil.getServerUrl());
		String barcodeImageUrl = serverUrl + contextRoot + "/media/?";
		barcodeImageUrl += AplosAppConstants.TYPE + "=" + MediaFileType.BARCODE.name() + "&";
		barcodeImageUrl += AplosAppConstants.BARCODE_TEXT + "=" + URLEncoder.encode( barcodeText, "UTF-8" );
		return barcodeImageUrl;
	}

	public static String getFileUrl(FileDetails fileDetails, boolean addContextPath, MediaFileType fileType ) {
		return getFileUrl(fileDetails, addContextPath, fileType, false );
	}

	public static String getFileUrl(FileDetails fileDetails, boolean addContextPath, MediaFileType fileType, boolean provideDefaultIfMissing ) {
		if( addContextPath ) {
			return getFileUrl( fileDetails, JSFUtil.getContextPath(), fileType, provideDefaultIfMissing );
		} else {
			return getFileUrl( fileDetails, "", fileType, provideDefaultIfMissing );
		}
	}

	public static String getFileUrl(String imgFileUrl, String directoryPath, String contextPath, MediaFileType fileType ) {
		return getFileUrl(imgFileUrl, directoryPath, contextPath, fileType, false );
	}

	public static String getFileUrl(String imgFileUrl, String directoryPath, String contextPath, MediaFileType fileType, boolean provideDefaultIfMissing ) {
		return getFileUrl(imgFileUrl, directoryPath, contextPath, false, fileType, provideDefaultIfMissing);
	}

	public static String getFileUrl(String imgFileUrl, String directoryPath, String contextPath, boolean addRandom, MediaFileType fileType, boolean provideDefaultIfMissing ) {
		if ((imgFileUrl == null || imgFileUrl.equals("")) && !provideDefaultIfMissing) {
			return "";
		} else {
			try {
				StringBuffer urlBuf = new StringBuffer();

				if( contextPath != null ) {
					urlBuf.append(contextPath).toString();
				}
				
				urlBuf.append( "/media/?" );
				
				urlBuf.append( AplosAppConstants.FILE_NAME ).append( "=" );
				if (provideDefaultIfMissing && (imgFileUrl == null || imgFileUrl.equals(""))) {
					urlBuf.append( "images/missing-image.png" );
				} else {
					String relativeDir = CommonUtil.getPathRelativeToServerWorkDir(directoryPath);
					if( relativeDir != null ) {
						urlBuf.append( relativeDir.replace("//", "/") );
					}
					urlBuf.append( URLEncoder.encode(imgFileUrl, "UTF-8") );
				}

				if( fileType != null ) {
					urlBuf.append( "&" ).append( AplosAppConstants.TYPE ).append( "=" ).append( fileType.name() );
				}
				if (provideDefaultIfMissing) {
					urlBuf.append( "&provideDefaultIfMissing=" ).append( provideDefaultIfMissing );
				}
				if( addRandom ) {
					urlBuf.append( "&random=" ).append( new Double(Math.random()*10000+1).intValue() );
				}
				return urlBuf.toString();
			} catch (UnsupportedEncodingException usee) {
				return "";
			}
		}
	}

	public static String getImageUrl(FileDetails fileDetails, boolean addContextPath ) {
		return getImageUrl(fileDetails, addContextPath, true );
	}

	public static String getImageUrl(FileDetails fileDetails, boolean addContextPath, boolean provideDefaultImageIfMissing ) {
		if( addContextPath ) {
			return getFileUrl( fileDetails, JSFUtil.getContextPath(), MediaFileType.IMAGE, provideDefaultImageIfMissing );
		} else {
			return getFileUrl( fileDetails, "", MediaFileType.IMAGE, provideDefaultImageIfMissing );
		}
	}

	public static String getFileUrl(FileDetails fileDetails, String contextPath, MediaFileType fileType, boolean provideDefaultIfMissing ) {
		if ((fileDetails == null || fileDetails.getFilename() == null || fileDetails.getFilename().equals("")) && !provideDefaultIfMissing) {
			return "";
		} else {
			try {
				StringBuffer urlBuf = new StringBuffer();
	
				if( contextPath != null ) {
					urlBuf.append(contextPath).toString();
				}
				
				urlBuf.append( "/media/" + fileDetails.getId() + "." + fileDetails.getExtension() + "?" );
				urlBuf.append( AplosAppConstants.FILE_DETAILS_ID ).append( "=" ).append( fileDetails.getId() );
				
				if( fileDetails == null || fileDetails.getFileDetailsOwner() == null || (FileDetails.getSaveableFileDetailsOwner( fileDetails ) != null && FileDetails.getSaveableFileDetailsOwner( fileDetails ).isNew()) ) {
					urlBuf.append("&").append( AplosAppConstants.FILE_NAME ).append( "=" );
					if (provideDefaultIfMissing && (fileDetails == null || CommonUtil.isNullOrEmpty( fileDetails.getFilename() ) ) ) {
						urlBuf.append( "images/missing-image.png" );
					} else {
						urlBuf.append( URLEncoder.encode(fileDetails.determineFileDetailsDirectory(false).replace("//", "/") + fileDetails.getFilename(), "UTF-8") );
					}
					urlBuf.append("&").append( AplosAppConstants.ACCESS_KEY ).append( "=" ).append( fileDetails.getAccessKey() );
				}

				urlBuf.append( "&version=" ).append( fileDetails.getVersion() );
	
				if( fileType != null ) {
					urlBuf.append( "&" ).append( AplosAppConstants.TYPE ).append( "=" ).append( fileType.name() );
				}
				if (provideDefaultIfMissing) {
					urlBuf.append( "&provideDefaultIfMissing=" ).append( provideDefaultIfMissing );
				}
				return urlBuf.toString();
			} catch (UnsupportedEncodingException usee) {
				return "";
			}
		}
	}

	private void writeFile( File file, String filename, HttpServletResponse response, HttpServletRequest request, boolean fileRestricted ) throws FileNotFoundException, IOException {

		if (fileRestricted) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		InputStream stream = new FileInputStream( file );

		response.setContentType(getServletContext().getMimeType(file.getName()));
		String isAttachment = request.getParameter( AplosAppConstants.ATTACHMENT );
		if( filename == null ) {
			filename = file.getName();
		}
		if( CommonUtil.isNullOrEmpty( isAttachment ) ) {
			response.addHeader("Content-Disposition", "inline; filename=\"" + filename + "\"" );
		} else {
			response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"" );
		}
		BufferedInputStream input = null;
		BufferedOutputStream output = null;

		try {
		    input = new BufferedInputStream(stream);
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

	public static boolean isFileAvailable( String filename, String directoryPath ) {
		File imageFile;
		if( !(directoryPath.endsWith( "/" ) || directoryPath.endsWith( "\\" )) ) {
			directoryPath = directoryPath + "/";
		}
		imageFile = new File( directoryPath + filename );
		if ( imageFile.exists() ) {
			return true;
		} else {
			return false;
		}
	}
	
	private void writeImage( FileDetails fileDetails, HttpServletResponse response, HttpServletRequest request, boolean fileRestricted ) throws FileNotFoundException, IOException {
		File resizedImgFile = null;
		File resizedImgDir = null;
		if( fileDetails != null && (request.getParameter( "maxWidth" ) != null || request.getParameter( "maxHeight" ) != null || request.getParameter( "rotate" ) != null) ) {
			StringBuffer resizedImgFileBuf = new StringBuffer( CommonWorkingDirectory.RESIZED_IMAGE_FILES.getDirectoryPath(true) );
			resizedImgFileBuf.append( fileDetails.determineFileDetailsDirectory(false) ).append( fileDetails.getId() );
			resizedImgDir = new File( resizedImgFileBuf.toString() );
			resizedImgFileBuf.append( "/" );
			boolean isFileNameStarted = false;
			if( request.getParameter( "maxWidth" ) != null ) {
				resizedImgFileBuf.append( "maxWidth" ).append( request.getParameter( "maxWidth" ) );
				isFileNameStarted = true;
			}
			if( request.getParameter( "maxHeight" ) != null ) {
				if( isFileNameStarted ) {
					resizedImgFileBuf.append( "_" );
				}
				resizedImgFileBuf.append( "maxHeight" ).append( request.getParameter( "maxHeight" ) );
				isFileNameStarted = true;
			}
			if( request.getParameter( "rotate" ) != null ) {
				if( isFileNameStarted ) {
					resizedImgFileBuf.append( "_" );
				}
				resizedImgFileBuf.append( "rotate" ).append( request.getParameter( "rotate" ) );
				isFileNameStarted = true;
			}
			resizedImgFileBuf.append( "." ).append( CommonUtil.getStringOrEmpty( fileDetails.getExtension() ) );
			resizedImgFile = new File( resizedImgFileBuf.toString() );
		}
		
		String filename = null;
		String fileFormat = null;
		boolean ignoreParams = false;

		if( resizedImgFile != null && resizedImgFile.exists() ) {
			filename = CommonUtil.getPathRelativeToServerWorkDir( resizedImgFile.getAbsolutePath() );
			fileFormat = ImageUtil.getFormatInFile(resizedImgFile);
			ignoreParams = true;
		} else if( fileDetails != null ) {
			filename = fileDetails.determineFileDetailsDirectory(false) + fileDetails.getFilename();
			File imageFile = new File( CommonUtil.appendServerWorkPath( filename ) );
			if( imageFile.exists() ) {
				fileFormat = ImageUtil.getFormatInFile(imageFile);
			}
		}
		
		
		BufferedImage resizedImg = writeImage(ignoreParams, filename, response, request, fileRestricted);
		if( resizedImg != null && fileFormat != null && resizedImgFile != null && !resizedImgFile.exists() ) {
			resizedImgDir.mkdirs();
			resizedImgDir.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream( resizedImgFile );
			ImageIO.write( resizedImg, fileFormat, fileOutputStream );
			fileOutputStream.close();
		}
	}

	private BufferedImage writeImage( boolean ignoreParams, String filename, HttpServletResponse response, HttpServletRequest request, boolean fileRestricted ) throws FileNotFoundException, IOException {
		File imageFile;
		InputStream stream=null;
		String streamUrl = null;
		if( request.getParameter( AplosAppConstants.TYPE ) != null && request.getParameter( AplosAppConstants.TYPE ).equals( AplosAppConstants.LIVEDRIVE_HOSTED_IMAGE )) {
			imageFile = new File( "L:/" + filename );
		} else {
			imageFile = new File( CommonUtil.appendServerWorkPath( filename ) );
		}

		//return a placeholder image to stop the page falling over
		if (fileRestricted) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			streamUrl = "/com/aplos/core/resources/images/unauthorized.png";
		} else if ( !imageFile.exists() ) {
			String provideDefaultIfMissing = request.getParameter("provideDefaultIfMissing");
			if (provideDefaultIfMissing != null && provideDefaultIfMissing.toLowerCase().equals("true")) {
				streamUrl = "/com/aplos/core/resources/images/missing-image.png";
			}
		}
		if ( imageFile.exists() || streamUrl != null ) {
			response.setContentType(getServletContext().getMimeType(filename));
			response.setHeader("Content-Disposition", "inline;");

			CacheHeaderFilter.addResourceCacheHeaders( response );
			BufferedImage writeImage = null;
			String fileFormat = null;
			if( streamUrl != null ) {
				stream = getClass().getResourceAsStream(streamUrl);
				fileFormat = ImageUtil.getFormatFromStream(stream);
				stream = getClass().getResourceAsStream(streamUrl);
			} else if( imageFile.exists() ) {
				fileFormat = ImageUtil.getFormatInFile(imageFile);
			}
			if( fileFormat != null && !ignoreParams ) {
				try {
					if (request.getParameter( "thumb" ) != null) {
						SeekableStream seekableStream;
						if( stream == null ) {
							seekableStream =  new FileSeekableStream(imageFile);
						} else {
							seekableStream = SeekableStream.wrapInputStream(stream,true);
						}
						ParameterBlock pb = new ParameterBlock();
						pb.add(seekableStream);
						BufferedImage readImage = JAI.create(fileFormat, pb).getAsBufferedImage();
						ResampleOp resampleOp = new ResampleOp( 200, 200 );
						resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
						writeImage = resampleOp.filter( readImage, null );
					} else if( request.getParameter( "maxWidth" ) != null || request.getParameter( "maxHeight" ) != null || request.getParameter( "rotate" ) != null ) {

						BufferedImage readImage = null;

						if( stream == null ) {
							readImage = readImage( imageFile, fileFormat ); 
						} else {
							readImage = readImage( stream, fileFormat );
						}
						
						boolean isModified = false;
						BufferedImage modifiedImage = readImage;
						
						Integer maxWidth = null;
						Integer maxHeight = null;

						if( request.getParameter( "maxWidth" ) != null ) {
							maxWidth = Integer.valueOf( request.getParameter( "maxWidth" ) );
						}
						if( request.getParameter( "maxHeight" ) != null ) {
							maxHeight = Integer.valueOf( request.getParameter( "maxHeight" ) );
						}
						
						
						if( (maxWidth != null && maxWidth < readImage.getWidth()) 
								|| (maxHeight != null && maxHeight < readImage.getHeight()) ) {
							if( maxWidth == null ) {
								maxWidth = (int) ((maxHeight / (double) readImage.getHeight()) * readImage.getWidth());   
							} else if( maxHeight == null ) {
								maxHeight = (int) ((maxWidth / (double) readImage.getWidth()) * readImage.getHeight());
							}
							ResampleOp resampleOp = new ResampleOp( DimensionConstrain.createMaxDimension( maxWidth, maxHeight, true ) );
							resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
							modifiedImage = resampleOp.filter( readImage, null );
							isModified = true;
						}
						
						if( request.getParameter( "rotate" ) != null ) {
							int w = modifiedImage.getWidth();
							int h = modifiedImage.getHeight();
							double angle = Math.toRadians(Integer.valueOf( request.getParameter( "rotate" ) ));
							double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
							int neww = (int)Math.floor(w*cos+h*sin), newh = (int)Math.floor(h*cos+w*sin);
							GraphicsConfiguration gc = ((GraphicsEnvironment.getLocalGraphicsEnvironment()).getDefaultScreenDevice()).getDefaultConfiguration();
							BufferedImage result = gc.createCompatibleImage(neww, newh);
							Graphics2D g = result.createGraphics();
							g.translate((neww-w)/2, (newh-h)/2);
							g.rotate(angle, w/2, h/2);
							g.drawRenderedImage(modifiedImage, null);
							g.dispose();
							modifiedImage = result;
							isModified = true;
						}

						if( isModified ) {
							writeImage = modifiedImage;
						}
					}
				} catch (javax.media.jai.util.ImagingException e) {
					e.printStackTrace();
				}
			}
			if( writeImage != null ) {
				ImageIO.write( writeImage, fileFormat.toLowerCase(), response.getOutputStream() );

				if( stream != null ) {
					stream.close();
				}
				return writeImage;
			} else {
				
				BufferedInputStream input = null;
				BufferedOutputStream output = null;
				if (stream == null) {
					stream = new FileInputStream( imageFile );
				}
				
				try {
				    input = new BufferedInputStream(stream);
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
					if( stream != null ) {
						try { stream.close(); } catch (IOException logOrIgnore) {}
					}		
				}

			}
		}
		return null;
	}
	
	public static BufferedImage readImage( FileDetails fileDetails ) throws IOException {
		String filename = fileDetails.determineFileDetailsDirectory(false) + fileDetails.getFilename();
		File imageFile = new File( CommonUtil.appendServerWorkPath( filename ) );
		InputStream stream=null;
		String fileFormat = null;
		if( stream != null ) {
			fileFormat = ImageUtil.getFormatFromStream(stream);
			stream.reset();
		} else if( imageFile.exists() ) {
			fileFormat = ImageUtil.getFormatInFile(imageFile);
		}
		
		if( stream == null ) {
			return readImage( imageFile, fileFormat ); 
		} else {
			return readImage( stream, fileFormat );
		}
	}
	

	public static BufferedImage readImage( InputStream stream, String fileFormat ) {
		BufferedImage readImage;
		SeekableStream seekableStream;
		seekableStream = SeekableStream.wrapInputStream(stream,true);
		ParameterBlock pb = new ParameterBlock();
		pb.add(seekableStream);
		readImage = JAI.create(fileFormat, pb).getAsBufferedImage();
		try {
			seekableStream.close();
		} catch( IOException ioex ) {
			ApplicationUtil.handleError(ioex);
		}
		return readImage;
	}
	
	public static BufferedImage readImage( File imageFile, String fileFormat ) {
		BufferedImage readImage;
		
		JpegReader jpegReader = new JpegReader();
		try {
			readImage = jpegReader.readImage(imageFile);
		} catch( Exception ex ) {
			try {
				SeekableStream seekableStream = new FileSeekableStream(imageFile);
				ParameterBlock pb = new ParameterBlock();
				pb.add(seekableStream);
				readImage = JAI.create(fileFormat, pb).getAsBufferedImage();
			} catch( Exception ex2 ) {
				ApplicationUtil.getAplosContextListener().handleError(ex2);
				return null;
			}
		}	
		return readImage;
	}

}