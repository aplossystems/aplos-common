package com.aplos.common.enums;

import com.aplos.common.beans.AplosWorkingDirectory;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.utils.CommonUtil;

public enum CommonWorkingDirectory implements AplosWorkingDirectoryInter {
	USER_DIR ( System.getProperty( "user.home" ), false ),
	SERVER_WORK_DIR ( "", false ),
	UPLOAD_DIR ( "uploads/", false ),
	EDITOR_UPLOAD_DIR ( "uploads/editor/", false ),
	PROCESSED_RESOURCES_DIR ( "processedResources/", false ),
	CUSTOMER_REVIEW_IMAGE_DIR ( "customerReviewImages/", false ),
	LANGUAGE_ICONS ( "languageIcons/", false ),
	APLOS_FILE_UPLOADS ( "aplosFileUploads/", false ),
	HEADER_FOOTER_DIR ( "headerFooter/", false ),
	RESIZED_IMAGE_FILES ( "resizedImageFiles/", false ),
	IMAGE_PRINTER ( "imagePrinter/", false ),
	APLOS_EMAIL_FILE_DIR ( "aplosEmailFileDir/", false ),
	POTENTIAL_COMPANY_CSV ("potential_companys/", false ),
	BASIC_CONTACT_DETAILS_CSV ("basic_contact_details_csv/", false),
	COMBINED_RESOURCES ("combinedResources/", false),
	MINIFIED_JS ("minifiedJs/", false),
	MINIFIED_CSS ("minifiedCss/", false);

	String directoryPath;
	boolean restricted;
	private AplosWorkingDirectory aplosWorkingDirectory;

	private CommonWorkingDirectory( String directoryPath, boolean restricted ) {
		this.directoryPath = directoryPath;
		this.restricted = restricted;
	}

	public String getDirectoryPath( boolean includeServerWorkPath ) {
		if( includeServerWorkPath ) {
			return CommonUtil.appendServerWorkPath( directoryPath );
		} else {
			return directoryPath;
		}
	}
	
	public static void createDirectories( String projectName ) {
		/*
		 * The USER_DIR puts C:\ on some servers, which can cause issues as a double \\ will occur and the when comparing absolute file names 
		 * the File will return it with only the one back slash.  So any addtional backslashes are avoided here first.
		 */
		SERVER_WORK_DIR.directoryPath = USER_DIR.directoryPath;
		if( !SERVER_WORK_DIR.directoryPath.endsWith( System.getProperty("file.separator") ) ) {
			SERVER_WORK_DIR.directoryPath += System.getProperty("file.separator");
		}
		SERVER_WORK_DIR.directoryPath += "aplosWorkingFiles" + System.getProperty("file.separator") + projectName + System.getProperty("file.separator");

		for( CommonWorkingDirectory commonWorkingDirectory : CommonWorkingDirectory.values() ) {
			if( commonWorkingDirectory == USER_DIR || commonWorkingDirectory == SERVER_WORK_DIR ) {
				CommonUtil.createDirectory( commonWorkingDirectory.getDirectoryPath(false) );
			} else {
				CommonUtil.createDirectory( commonWorkingDirectory.getDirectoryPath(true) );
			}
		}
	}
	
	public boolean isRestricted() {
		return restricted;
	}

	public AplosWorkingDirectory getAplosWorkingDirectory() {
		return aplosWorkingDirectory;
	}

	public void setAplosWorkingDirectory(AplosWorkingDirectory aplosWorkingDirectory) {
		this.aplosWorkingDirectory = aplosWorkingDirectory;
	}
	
	
}
