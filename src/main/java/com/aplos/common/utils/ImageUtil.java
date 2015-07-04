package com.aplos.common.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;

import org.primefaces.model.UploadedFile;

import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.ResizedBufferedImage;
import com.aplos.common.filters.ResourceFilter;
import com.aplos.common.servlets.MediaServlet;
import com.aplos.common.servlets.MediaServlet.MediaFileType;
import com.sun.media.jai.codec.FileCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;

public class ImageUtil {

	/* ************************************************************************
	 * Image Manipulation
	 */

	public static BufferedImage resizeImage(BufferedImage bufImage,	int maxWidth, int maxHeight) {
		int curWidth = bufImage.getWidth();
		int curHeight = bufImage.getHeight();

		if ( (curWidth == maxWidth && (curHeight <= maxHeight)) ||
			 (curHeight == maxHeight && (curWidth <= maxWidth)) ) {
			//we are already at the right size
			return bufImage;
		}

		double widthRatio = curWidth / (double) maxWidth;
		double heightRatio = curHeight / (double) maxHeight;

		double largestRatio;
		if (widthRatio > heightRatio) {
			largestRatio = widthRatio;
		} else {
			largestRatio = heightRatio;
		}

		if (largestRatio > 1) {
			int newWidth = (int) (curWidth / largestRatio);
			int newHeight = (int) (curHeight / largestRatio);
			BufferedImage bdest = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
//			BufferedImage bdest = new BufferedImage(newWidth, newHeight, bufImage.getType());
			java.awt.Graphics2D graphics2D = bdest.createGraphics();

			// this algorithm is a lot slower than our getScaledInstance in this
			// class, but slightly better quality.
			graphics2D.drawImage(bufImage.getScaledInstance(newWidth,
					newHeight, Image.SCALE_SMOOTH), 0, 0, null);

			// ResampleOp resampleOp = new ResampleOp (newWidth,newHeight);
			// resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
			// BufferedImage rescaledTomato = resampleOp.filter(bufImage, null);
			return bdest;
			// return getScaledInstance(bufImage, newWidth, newHeight,
			// RenderingHints.VALUE_INTERPOLATION_BICUBIC, true );
		}

		return bufImage;
	}
	
	public static String getFullFileUrl( FileDetails fileDetails, boolean addContextPath ) {
		if( fileDetails == null ) {
			return null;
		} else {
			return fileDetails.getFullFileUrl(true);
		}
	}

	public static BufferedImage getScaledInstance(BufferedImage img,
			int targetWidth, int targetHeight, Object hint,
			boolean higherQuality) {
		int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
				: BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

	public static BufferedImage bufferImage(Image image, int type) {
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
				image.getHeight(null), type);
		Graphics2D g = bufferedImage.createGraphics();
		g.drawImage(image, null, null);
		// waitForImage(bufferedImage);
		return bufferedImage;
	}

	// public static BufferedImage resizeImage(BufferedImage bufImage, int
	// width, int height) {
	// int imgWidth = bufImage.getWidth();
	// int imgHeight = bufImage.getHeight();
	//
	// int largestDim = Math.max( imgWidth, imgHeight );
	// int resizeDim = Math.max( width, height );
	// if( largestDim > resizeDim ) {
	// double ratio = (double) resizeDim / (double)largestDim;
	// BufferedImage bdest = new BufferedImage( (int)( imgWidth * ratio ),
	// (int)( imgHeight * ratio ), BufferedImage.TYPE_INT_RGB );
	// java.awt.Graphics2D g = bdest.createGraphics();
	// java.awt.geom.AffineTransform at =
	// java.awt.geom.AffineTransform.getScaleInstance( ratio, ratio );
	// g.drawRenderedImage( bufImage, at );
	// return bdest;
	// }
	//
	// return bufImage;
	// }

	/**
	 * @deprecated should be using {@link ImageUtil#saveImageToFile(ResizedBufferedImage, String, String)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	String id) {
		return uploadImage(upFile, directoryPath, id, null );
	}

	/**
	 * @deprecated should be using {@link ImageUtil#uploadImage(ResizedBufferedImage, String, String String)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	String id, String suffix ) {
		return uploadImage(upFile, directoryPath, id, suffix, -1, -1 );
	}

	/**
	 * @deprecated should be using {@link ImageUtil#saveImageToFile(ResizedBufferedImage, String, Long)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	Long id) {
		return uploadImage(upFile, directoryPath, id, null );
	}

	/**
	 * @deprecated should be using {@link ImageUtil#saveImageToFile(ResizedBufferedImage, String, Long)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath, Long id, int maxWidth, int maxHeight) {
		return uploadImage(upFile, directoryPath, String.valueOf( id ), null, maxWidth, maxHeight);
	}

	/**
	 * @deprecated should be using {@link ImageUtil#saveImageToFile(ResizedBufferedImage, String, Long, String)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	Long id, String suffix ) {
		return uploadImage(upFile, directoryPath, String.valueOf( id ), suffix, -1, -1 );
	}

	/**
	 * @deprecated should be using {@link ImageUtil#uploadImage(ResizedBufferedImage, String, Long, String, Integer maxWidth, Integer maxHeight)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	Long id, String suffix, int maxWidth, int maxHeight) {
		return uploadImage(upFile, directoryPath, String.valueOf( id ), suffix, maxWidth, maxHeight);
	}

	/**
	 * @deprecated should be using {@link ImageUtil#saveImageToFile(ResizedBufferedImage, String, String, String)} in conjunction with an image uploader component
	 */
	@Deprecated
	public static String uploadImage(UploadedFile upFile, String directoryPath,	String prefix, String suffix, int maxWidth, int maxHeight) {
		String newFilename = null;
		try {
			// Use the JAI library as it accepts more formats than the standard
			// ImageIO one.
			String fileFormat = ImageUtil.getFormatFromStream(upFile.getInputstream());
			fileFormat = fileFormat.toLowerCase();
			SeekableStream seekableStream =  new FileCacheSeekableStream(upFile.getInputstream());
			ParameterBlock pb = new ParameterBlock();
			pb.add(seekableStream);
			BufferedImage mainBufImage = JAI.create(fileFormat, pb).getAsBufferedImage();
//			BufferedImage mainBufImage = ImageIO.read(upFile.getInputStream());

			String fileExtension = upFile.getFileName().substring(upFile.getFileName().lastIndexOf(".") + 1, upFile.getFileName().length());
			newFilename = prefix;
			if( suffix != null ) {
				newFilename += "_" + suffix;
			}
			newFilename += "." + fileExtension.toLowerCase();

			java.io.File ioFile = new java.io.File(directoryPath);

			directoryPath = ioFile.getAbsolutePath();
			ioFile.mkdirs();
			ioFile = new File(directoryPath + "/" + newFilename);

			try {

				if (!(maxWidth == -1 && maxHeight == -1)) {
					mainBufImage = resizeImage(mainBufImage, maxWidth,
							maxHeight);
				}

				write( mainBufImage, directoryPath + "/" + newFilename, 1.0f, getFormatName(upFile.getInputstream()));
//				ImageIO.write(mainBufImage, getFormatName(upFile
//						.getInputStream()), ioFile = new File(directoryPath
//						+ "/" + newFilename));

			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Error writing file.");
			}
		} catch (IOException ioEx) {
			ErrorEmailSender.sendErrorEmail(JSFUtil.getRequest(),ApplicationUtil.getAplosContextListener(),
					ioEx);
			JSFUtil
					.addMessage("The color space of the image is not recognised, please resave the image through a program like photoshop or paint.");
			ioEx.printStackTrace();
		}

		return newFilename;
	}

	//new methods, using resized buffered image

	public static String saveImageToFile(ResizedBufferedImage resizedBufferedImage, String directoryPath, Long id) {
		return saveImageToFile(resizedBufferedImage, directoryPath, id, null);
	}

	public static String saveImageToFile(ResizedBufferedImage resizedBufferedImage, String directoryPath, String id) {
		return saveImageToFile(resizedBufferedImage, directoryPath, id, null, resizedBufferedImage.getResizeWidth(), resizedBufferedImage.getResizeHeight());
	}

	public static String saveImageToFile(ResizedBufferedImage resizedBufferedImage, String directoryPath, Long id, String suffix) {
		return saveImageToFile(resizedBufferedImage, directoryPath, String.valueOf( id ), suffix, resizedBufferedImage.getResizeWidth(), resizedBufferedImage.getResizeHeight());
	}

	public static String saveImageToFile(ResizedBufferedImage resizedBufferedImage, String directoryPath,	Long id, String suffix, Integer maxWidth, Integer maxHeight) {
		return saveImageToFile(resizedBufferedImage, directoryPath, String.valueOf( id ), suffix, maxWidth, maxHeight);
	}

	public static String saveImageToFile(ResizedBufferedImage resizedBufferedImage, String directoryPath, String prefix, String suffix, Integer maxWidth, Integer maxHeight) {
		String newFilename = null;
		String fileFormat = resizedBufferedImage.getExtension();
		fileFormat = fileFormat.toLowerCase();
		String fileName = resizedBufferedImage.getName().substring(resizedBufferedImage.getName().lastIndexOf(".") + 1, resizedBufferedImage.getName().length());
		newFilename = prefix;
		if( !CommonUtil.isNullOrEmpty( suffix )  ) {
			newFilename += "_" + suffix;
		}
		newFilename += "." + fileName.toLowerCase();
		java.io.File ioFile = new java.io.File(directoryPath);
		directoryPath = ioFile.getAbsolutePath();
		ioFile.mkdirs();
		ioFile = new File(directoryPath + "/" + newFilename);
		try {
			BufferedImage mainBufImage = resizedBufferedImage.getResizedImage();
			if (!(maxWidth == null && maxHeight == null)) {
				//wont resize again unless we have overridden dimensions (eg for thumbnails)
				mainBufImage = resizeImage(mainBufImage, maxWidth, maxHeight);
			}
			write( mainBufImage, directoryPath + "/" + newFilename, 1.0f, fileFormat);
//				ImageIO.write(mainBufImage, getFormatName(upFile
//						.getInputStream()), ioFile = new File(directoryPath
//						+ "/" + newFilename));

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error writing file.");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error writing file.");
		}
		return newFilename;
	}
	
	public static BufferedImage readImageFromResource( String resourcePath ) {
		BufferedImage bufferedImage = null;
		URL imageUrl = JSFUtil.checkFileLocations( resourcePath, ResourceFilter.RESOURCES_PATH, true );
		try {
			InputStream stream = imageUrl.openStream();
			String fileFormat = ImageUtil.getFormatFromStream(stream);
			stream = imageUrl.openStream();
			bufferedImage = MediaServlet.readImage( stream, fileFormat );
		} catch( IOException ioex ) {
			ApplicationUtil.handleError(ioex);
		}
		
		return bufferedImage;
	}

	public static void write( BufferedImage mainBufImage, String filename, float compressionQuality, String formatName ) throws IOException {
		write( mainBufImage, new File( filename), compressionQuality, formatName );
	}

	public static void write(BufferedImage mainBufImage, File newFile, float compressionQuality, String formatName) throws IOException {
		ImageOutputStream out = ImageIO.createImageOutputStream(newFile);
		write( mainBufImage, out, compressionQuality, formatName );
	}

	// This a higher quality write method than the standard ImageIO.write, however it seems to reduce the lightness of images, although making
	// them smoother, beware.
	public static void write(BufferedImage mainBufImage, ImageOutputStream out,
			float compressionQuality, String formatName) throws IOException {
		Iterator writers = ImageIO.getImageWritersBySuffix(formatName);
		if (!writers.hasNext()) {
			throw new IllegalStateException("No writers for image?!");
		}

		ImageWriter writer = (ImageWriter) writers.next();

		List thumbNails = null;
		IIOImage iioImage = new IIOImage(mainBufImage, thumbNails,
				(IIOMetadata) null);

		ImageWriteParam imageWriteParam = null;
		if (!formatName.equalsIgnoreCase("png")) {
			imageWriteParam = writer.getDefaultWriteParam();
			imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

			if (formatName.equalsIgnoreCase("gif") || formatName.equalsIgnoreCase("bmp")) {
				imageWriteParam.setCompressionType(imageWriteParam
						.getCompressionTypes()[0]);
			}
			imageWriteParam.setCompressionQuality(compressionQuality);
		}
		writer.setOutput(out);
		writer.write((IIOMetadata) null, iioImage, imageWriteParam);
		out.flush();
		out.close();

	}

	// public static BufferedImage cmykRasterToSRGB(Raster raster,
	// ICC_Profile cmykProfile) {
	// int w = raster.getWidth();
	// int h = raster.getHeight();
	//
	// ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
	// BufferedImage rgbImage = new BufferedImage(w, h,
	// BufferedImage.TYPE_INT_RGB);
	// WritableRaster rgbRaster = rgbImage.getRaster();
	// ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
	// ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
	// cmykToRgb.filter(raster, rgbRaster);
	// return rgbImage;
	// }

	// Returns the format of the image in the file 'f'.
	// Returns null if the format is not known.
	public static String getFormatInFile(File f) {
		return getFormatName(f);
	}

	// Returns the format of the image in the input stream 'is'.
	// Returns null if the format is not known.
	public static String getFormatFromStream(InputStream is) {
		return getFormatName(is);
	}

	// Returns the format name of the image in the object 'o'.
	// 'o' can be either a File or InputStream object.
	// Returns null if the format is not known.
	private static String getFormatName(Object o) {
		try {
			// Create an image input stream on the image
			ImageInputStream iis = ImageIO.createImageInputStream(o);

			// Find all image readers that recognize the image format
			Iterator iter = ImageIO.getImageReaders(iis);
			if (!iter.hasNext()) {
				// No readers found
				iis.close();
				return null;
			}

			// Use the first reader
			ImageReader reader = (ImageReader) iter.next();

			// Close stream
			iis.close();

			// Return the format name
			String formatName = reader.getFormatName();
			reader.dispose();
			return formatName;
		} catch (IOException e) {
		}
		// The image could not be read
		return null;
	}

	/**
	 * @deprecated
	 * This has been replaced by the media servlet method
	 * @param imgFileUrl
	 * @param directoryPath
	 * @param contextPath
	 * @return
	 */
	@Deprecated
	public static String getImageUrl(String imgFileUrl, String directoryPath, String contextPath) {
		return MediaServlet.getFileUrl( imgFileUrl, directoryPath, contextPath, MediaFileType.IMAGE );
	}

	public static String getImageUrl(String imgFileUrl, String directoryPath) {
		return getImageUrl(imgFileUrl, directoryPath, JSFUtil.getRequest().getContextPath());
	}

}
