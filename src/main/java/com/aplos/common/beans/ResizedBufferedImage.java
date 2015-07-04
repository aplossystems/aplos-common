package com.aplos.common.beans;

import java.awt.image.BufferedImage;
import java.io.Serializable;

import org.primefaces.model.UploadedFile;

public class ResizedBufferedImage implements Serializable {
	private static final long serialVersionUID = -1096711130834944615L;
	
	private BufferedImage resizedImage;
	private String name;
	private Integer resizeWidth=null;
	private Integer resizeHeight=null;
	private UploadedFile originalUploadedFile;
	private boolean isImageResized = false;

	public ResizedBufferedImage(BufferedImage resizedImage, String name, Integer resizeWidth, Integer resizeHeight) {
		this.setResizedImage(resizedImage);
		this.setName(name);
		this.setResizeHeight(resizeHeight);
		this.setResizeWidth(resizeWidth);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setResizedImage(BufferedImage resizedImage) {
		this.resizedImage = resizedImage;
	}

	public BufferedImage getResizedImage() {
		return resizedImage;
	}

	public void setResizeWidth(Integer resizeWidth) {
		this.resizeWidth = resizeWidth;
	}

	public Integer getResizeWidth() {
		return resizeWidth;
	}

	public void setResizeHeight(Integer resizeHeight) {
		this.resizeHeight = resizeHeight;
	}

	public Integer getResizeHeight() {
		return resizeHeight;
	}

	public String getExtension() {
		return getName().replaceFirst( "(.*)(\\.)(.{3,4})$", "$3").toLowerCase();
	}

	public UploadedFile getOriginalUploadedFile() {
		return originalUploadedFile;
	}

	public void setOriginalUploadedFile(UploadedFile originalUploadedFile) {
		this.originalUploadedFile = originalUploadedFile;
	}

	public boolean isImageResized() {
		return isImageResized;
	}

	public void setImageResized(boolean isImageResized) {
		this.isImageResized = isImageResized;
	}

}