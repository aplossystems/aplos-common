package com.aplos.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aplos.common.beans.FileDetails;

public abstract class FileDetailsOwnerHelper {
	private Map<String,Map<Object,FileDetails>> tempFileDetailsMap = new HashMap<String,Map<Object,FileDetails>>();

	public abstract String getFileDetailsDirectory( String fileDetailsKey, boolean includeServerWorkPath );
	public abstract void setFileDetails( FileDetails fileDetails, String fileDetailsKey, Object collectionKey );
	public abstract FileDetails getFileDetails( String fileDetailsKey, Object collectionKey );

	public Map<Object,FileDetails> getTempFileDetailsMap( String fileDetailsKey ) {
		return tempFileDetailsMap.get( fileDetailsKey );
	}

	public FileDetails getTempFileDetails( String fileDetailsKey, Object collectionKey ) {
		Map<Object,FileDetails> innerTempFileDetailsMap = tempFileDetailsMap.get( fileDetailsKey );
		if( innerTempFileDetailsMap == null ) {
			return null;
		} else {
			return innerTempFileDetailsMap.get( collectionKey );
		}
	}
	
	public FileDetails determineFileDetails( String fileDetailsKey, Object collectionKey ) {
		FileDetails fileDetails = getFileDetails( fileDetailsKey, collectionKey );
		if( fileDetails == null ) {
			fileDetails = getTempFileDetails(fileDetailsKey, collectionKey);
		}
		return fileDetails;
	}
	
	public void setTempFileDetails( FileDetails fileDetails, String fileDetailsKey, Object collectionKey ) {
		Map<Object,FileDetails> innerTempFileDetailsMap = tempFileDetailsMap.get( fileDetailsKey );
		if( innerTempFileDetailsMap == null ) {
			innerTempFileDetailsMap = new HashMap<Object,FileDetails>();
			tempFileDetailsMap.put( fileDetailsKey, innerTempFileDetailsMap );
		}
		innerTempFileDetailsMap.put( collectionKey, fileDetails );
	}

	public List<FileDetails> getFileDetailsList( String fileDetailsKey ) {
		return null;
	}
	
	public FileDetails createCustomFileDetails( String fileDetailsKey ) {
		return null;
	}
	public void saveCompleted( String fileDetailsKey, Object collectionKey ) {}
}
