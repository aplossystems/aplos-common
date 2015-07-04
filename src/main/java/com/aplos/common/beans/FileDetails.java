package com.aplos.common.beans;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.primefaces.model.UploadedFile;

import com.aplos.common.AplosUrl;
import com.aplos.common.AplosUrl.Protocol;
import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.SaveableFileDetailsOwnerHelper;
import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.interfaces.FileUploaderInter;
import com.aplos.common.servlets.MediaServlet;
import com.aplos.common.servlets.MediaServlet.MediaFileType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ImageUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Cache
public class FileDetails extends AplosBean {
	private static final long serialVersionUID = -4438430118907447096L;
	private String name;
	private String filename;
	private int version = 0;
	@Any( metaColumn = @Column( name = "fileDetailsOwner_type" ), fetch=FetchType.LAZY )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */
    } )
    @JoinColumn(name="fileDetailsOwner_id")
	@DynamicMetaValues
	private FileDetailsOwnerInter fileDetailsOwner;

	private String fileDetailsKey; 
	@Transient
	private transient ResizedBufferedImage resizedBufferedImage;
	@Transient
	private Boolean isShowFileUploader;
	@Transient
	private boolean isShowFileUploading = false;
	@Transient
	private transient UploadedFile uploadedFile;
	@Transient
	private transient FileUploaderInter fileUploader2;
	
	public FileDetails() {
		// TODO Auto-generated constructor stub
	}
	
	public FileDetails( FileDetailsOwnerInter fileDetailsOwner, String fileDetailsKey ) {
		setFileDetailsOwner(fileDetailsOwner);
		setFileDetailsKey(fileDetailsKey);
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialise(getFileDetailsOwner(), false);
//	}
	
	public FileDetails getCopy() {
		FileDetails newFileDetails = new FileDetails();
		newFileDetails.setName( getName() );
		newFileDetails.setFilename( getFilename() );
		newFileDetails.setFileDetailsOwner(getFileDetailsOwner());
		newFileDetails.setFileDetailsKey(getFileDetailsKey());
		return newFileDetails;
	}
	
	public void updateFile( ResizedBufferedImage resizedBufferedImage ) {
		if( isNew() ) {
			super.saveDetails();
		}
		FileDetailsOwnerInter fileDetailsOwnerRef = getFileDetailsOwner();
		String directoryPath = determineFileDetailsDirectory(fileDetailsOwnerRef,true);
		setVersion( getVersion() + 1 );
		setFilename( ImageUtil.saveImageToFile( resizedBufferedImage, directoryPath, getId() ) );
		super.saveDetails();

		if( fileDetailsOwnerRef != null ) {
			File directoryFile = new File( CommonWorkingDirectory.RESIZED_IMAGE_FILES.getDirectoryPath(true) + determineFileDetailsDirectory(false) +  getId() );
			if( directoryFile.exists() ) {
				try {
					FileUtils.deleteDirectory(directoryFile);
				} catch( IOException ioex ) {
					ApplicationUtil.getAplosContextListener().handleError(ioex);
				}
			}
		}
	}
	
	@Override
	public void hardDelete() {
		File file = getFile();
		if( file != null && file.exists() ) {
			file.delete();
		}
		super.hardDelete();
	}
	
	public void updateFile( UploadedFile uploadedFile ) {
		if( isNew() ) {
			super.saveDetails();
		}
		FileDetailsOwnerInter fileDetailsOwnerRef = getFileDetailsOwner();
		String directoryPath = determineFileDetailsDirectory(fileDetailsOwnerRef,true);
		setVersion( getVersion() + 1 );
		setFilename(CommonUtil.uploadFile(uploadedFile, directoryPath, this.getId()));
		setName(uploadedFile.getFileName());
		super.saveDetails();
	}
	
	/*
	 * This method makes sure a transient instance exception isn't thrown in the case that the fileDetailsOwner
	 * is new, the reference is added back into the fileDetails when the owner is saved.
	 */
	@Override
	public void saveBean(SystemUser currentUser ) {
		FileDetailsOwnerInter fileDetailsOwnerRef = getFileDetailsOwner();
		boolean ownerIsNew = false;
		if( fileDetailsOwner.getFileDetailsOwnerHelper() instanceof SaveableFileDetailsOwnerHelper ) {
			ownerIsNew = ((SaveableFileDetailsOwnerHelper)fileDetailsOwner.getFileDetailsOwnerHelper()).isNew();
		} 
		if ( ownerIsNew ) {
			setFileDetailsOwner(null);
		}
		super.saveBean(currentUser);
		setFileDetailsOwner( fileDetailsOwnerRef );
	}
	
	public static SaveableFileDetailsOwnerHelper getSaveableFileDetailsOwner( FileDetails fileDetails ) {
		return getSaveableFileDetailsOwner(fileDetails.getFileDetailsOwner());
	}
	
	public static SaveableFileDetailsOwnerHelper getSaveableFileDetailsOwner( FileDetailsOwnerInter fileDetailsOwnerInter ) {
		FileDetailsOwnerHelper fileDetailsOwnerHelper = fileDetailsOwnerInter.getFileDetailsOwnerHelper();
		if( fileDetailsOwnerHelper instanceof SaveableFileDetailsOwnerHelper ) {
			return (SaveableFileDetailsOwnerHelper) fileDetailsOwnerHelper;
		}
		return null;
	}
	
	public static boolean saveFileDetailsOwner( FileDetailsOwnerInter fileDetailsOwner, Enum[] fileDetailKeys, SystemUser systemUser ) {
		if( fileDetailsOwner.getFileDetailsOwnerHelper() instanceof SaveableFileDetailsOwnerHelper ) {
			boolean ownerWasNew = ((SaveableFileDetailsOwnerHelper)fileDetailsOwner.getFileDetailsOwnerHelper()).isNew();
			return saveFileDetailsOwner(fileDetailsOwner, fileDetailKeys, systemUser, ownerWasNew);
		}
		return false;
	}
	
	/*
	 * This method makes sure that the fileDetails have a reference to the owner, this may not
	 * be the case if the owner isNew as the reference would have been removed so not to cause
	 * a transient instance exception.
	 */
	public static boolean saveFileDetailsOwner( FileDetailsOwnerInter fileDetailsOwner, Enum[] fileDetailKeys, SystemUser systemUser, boolean ownerWasNew ) {
		if( fileDetailsOwner.getFileDetailsOwnerHelper() instanceof SaveableFileDetailsOwnerHelper ) {
			SaveableFileDetailsOwnerHelper saveableFdoh = (SaveableFileDetailsOwnerHelper) fileDetailsOwner.getFileDetailsOwnerHelper();
			FileDetails tempFileDetails;
			for( int i = 0, n = fileDetailKeys.length; i < n; i++ ) {
				Map<Object,FileDetails> fileDetailsMap = saveableFdoh.getTempFileDetailsMap( fileDetailKeys[ i ].name() );
				if( fileDetailsMap != null ) {
					Iterator<Object> collectionKeys = fileDetailsMap.keySet().iterator();
					Object collectionKey;
					while( collectionKeys.hasNext() ) {
						collectionKey = collectionKeys.next();
						tempFileDetails = fileDetailsMap.get( collectionKey );
						if( tempFileDetails != null && tempFileDetails.isNew() ) {
							if( CommonUtil.isFileUploaded( tempFileDetails.getUploadedFile() ) || tempFileDetails.getResizedBufferedImage() != null ) {
								/*
								 * TODO This should throw an exception and not allow the rest of the object to save.  An example of 
								 * why is in the altrui donor profile frontend form, the user can select an image without a valid
								 * type and it simply will not save the image and not warn the customer until the next page.
								 */
								boolean savedSuccessfully = tempFileDetails.getFileUploader2().saveFileOwnerObject(false);
								if( savedSuccessfully ) {
									fileDetailsOwner.getFileDetailsOwnerHelper().setFileDetails(tempFileDetails, fileDetailKeys[ i ].name(), collectionKey);
								}
							}
						}
					}
				}
			}
			fileDetailsOwner.superSaveBean( systemUser );
			if( ownerWasNew ) {
				for( int i = 0, n = fileDetailKeys.length; i < n; i++ ) {
					List<FileDetails> fileDetailsList = saveableFdoh.getFileDetailsList( fileDetailKeys[ i ].name() );
					if( fileDetailsList != null ) {
						for( int j = 0, p = fileDetailsList.size(); j < p; j++ ) {
							tempFileDetails = fileDetailsList.get( j );
							if( tempFileDetails != null ) {
								tempFileDetails.setFileDetailsOwner(fileDetailsOwner);
								tempFileDetails.saveDetails();
							}
						}
					} else {
						tempFileDetails = saveableFdoh.getFileDetails( fileDetailKeys[ i ].name(), null );
						if( tempFileDetails != null ) {
							tempFileDetails.setFileDetailsOwner(fileDetailsOwner);
							tempFileDetails.saveDetails();
						}
					}
				}
				return true;
			} 
		}
		return false;
	}
	
	public void writeStringToFile( String content ) {
		try {
		    File file = new File( determineFileDetailsDirectory(true) + getFilename() );
		    file.createNewFile();
		    FileOutputStream fileOut = new FileOutputStream(file);
		    fileOut.write( content.getBytes() );
		    fileOut.close();
		} catch( IOException ioex ) {
			ApplicationUtil.handleError(ioex);
		}
	}

	public AplosUrl getAplosUrl() {
		return getAplosUrl( false );
	}

	public AplosUrl getAplosUrl( boolean isAttachment ) {
		String localUrl = MediaServlet.getFileUrl( this, determineFileDetailsDirectory(true), false, MediaFileType.PDF );
		AplosUrl aplosUrl = new AplosUrl( localUrl, false );
		if( isAttachment ) {
			aplosUrl.addQueryParameter( AplosAppConstants.ATTACHMENT, true );
		}
		aplosUrl.addContextPath();
		return aplosUrl;
	}
	
	public void redirectToAplosUrl() {
		JSFUtil.redirect( getAplosUrl() );
	}
	
	public void redirectToAplosUrl( boolean isAttachment ) {
		JSFUtil.redirect( getAplosUrl( isAttachment ) );
	}

	public String getExternalFileUrlByServerUrl() {
		String localUrl = MediaServlet.getImageUrl( this, determineFileDetailsDirectory(true), false );
		AplosUrl aplosUrl = new AplosUrl( localUrl, false );
		String serverAndContext = (String) JSFUtil.getServerUrl() + JSFUtil.getContextPath().replace("/", "");
		return serverAndContext + aplosUrl.toString();
	}

	public String getExternalFileUrl() {
		String localUrl = MediaServlet.getImageUrl( this, determineFileDetailsDirectory(true), false );
		AplosUrl aplosUrl = new AplosUrl( localUrl, false );
		aplosUrl.setHost( Website.getCurrentWebsiteFromTabSession(), true );
		aplosUrl.setScheme( Protocol.HTTP );
		return aplosUrl.toString();
	}

	public String getFullFileUrl(boolean addContextPath) {
		return MediaServlet.getImageUrl( this, determineFileDetailsDirectory(true), addContextPath );
	}

	public boolean fileExists( String directory ) {
		return new File( directory + getFilename() ).exists();
	}

	public boolean fileExists() {
		if( getFilename() != null ) {
			return MediaServlet.isFileAvailable( getFilename(), determineFileDetailsDirectory(true) );
		} 
		return false;
	}
	
	public File getFile() {
		return new File( determineFileDetailsDirectory(true) + "/" + getFilename() );
	}
	
	public String determineFileDetailsDirectory( boolean includeServerWorkPath ) {
		return determineFileDetailsDirectory( getFileDetailsOwner(), includeServerWorkPath );
	}
	
	public String determineFileDetailsDirectory( FileDetailsOwnerInter fileDetailsOwner, boolean includeServerWorkPath ) {
		if( fileDetailsOwner == null ) {
			return "";
		} else {
			return fileDetailsOwner.getFileDetailsOwnerHelper().getFileDetailsDirectory( getFileDetailsKey(), includeServerWorkPath );
		}
	}
	
	public String getExtension() {
		if( getFilename() != null && getFilename().contains( "." ) ) {
			return getFilename().substring( getFilename().lastIndexOf( "." ) + 1 ); 
		} else {
			return null;
		}
	}

	@Override
	public String getDisplayName() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public ResizedBufferedImage getResizedBufferedImage() {
		return resizedBufferedImage;
	}

	public void setResizedBufferedImage(ResizedBufferedImage resizedBufferedImage) {
		this.resizedBufferedImage = resizedBufferedImage;
	}

	public boolean isShowFileUploading() {
		return isShowFileUploading;
	}

	public void setShowFileUploading(boolean isShowFileUploading) {
		this.isShowFileUploading = isShowFileUploading;
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public boolean isShowFileUploader() {
		if( isShowFileUploader == null ) {
			isShowFileUploader = !fileExists();
		}
		return isShowFileUploader;
	}

	public void setShowFileUploader(boolean isShowFileUploader) {
		this.isShowFileUploader = isShowFileUploader;
	}

	public FileDetailsOwnerInter getFileDetailsOwner() {
		return fileDetailsOwner;
	}

	public void setFileDetailsOwner(FileDetailsOwnerInter fileDetailsOwner) {
		this.fileDetailsOwner = fileDetailsOwner;
	}

	public String getFileDetailsKey() {
		return fileDetailsKey;
	}

	public void setFileDetailsKey(String fileDetailsKey) {
		this.fileDetailsKey = fileDetailsKey;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public FileUploaderInter getFileUploader2() {
		return fileUploader2;
	}

	public void setFileUploader2(FileUploaderInter fileUploader2) {
		this.fileUploader2 = fileUploader2;
	}
}
