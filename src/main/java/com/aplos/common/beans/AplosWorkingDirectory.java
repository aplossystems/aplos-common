package com.aplos.common.beans;

import java.util.HashMap;
import java.util.Map;

import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.utils.ApplicationUtil;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class AplosWorkingDirectory extends AplosBean implements FileDetailsOwnerInter {
	private static final long serialVersionUID = 2072214484823468070L;
	private String enumName;
	@Transient
	private static Map<Long, AplosWorkingDirectoryInter> aplosWorkingDirectoryEnumMap = new HashMap<Long, AplosWorkingDirectoryInter>();
	@Transient
	private AplosWorkingDirectoryFdoh aplosWorkingDirectoryFdoh = new AplosWorkingDirectoryFdoh();
	
	public String getEnumName() {
		return enumName;
	}

	public void setEnumName(String enumName) {
		this.enumName = enumName;
	}

	public AplosWorkingDirectoryInter getAplosWorkingDirectoryEnum() {
		return aplosWorkingDirectoryEnumMap.get( getId() );
	}

	public void setAplosWorkingDirectoryEnum(AplosWorkingDirectoryInter aplosWorkingDirectoryEnum) {
		if( getId() != null ) {
			aplosWorkingDirectoryEnumMap.put( getId(), aplosWorkingDirectoryEnum );
		}
	}
	
	@Override
	public void superSaveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
	}
	
	@Override
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper() {
		return aplosWorkingDirectoryFdoh;
	}
	
	private class AplosWorkingDirectoryFdoh extends FileDetailsOwnerHelper {
		
		@Override
		public void setFileDetails(FileDetails fileDetails, String fileDetailsKey, Object collectionKey) {	
		}
		
		@Override
		public FileDetails getFileDetails(String fileDetailsKey, Object collectionKey) {
			return null;
		}
		
		@Override
		public String getFileDetailsDirectory(String fileDetailsKey, boolean includeServerWorkPath) {
			return ApplicationUtil.getAplosContextListener().getAplosWorkingDirectoryEnum( getEnumName() ).getDirectoryPath( includeServerWorkPath );
		}

	}
}
