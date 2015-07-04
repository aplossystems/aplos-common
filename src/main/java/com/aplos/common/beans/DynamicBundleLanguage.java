package com.aplos.common.beans;

import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.SaveableFileDetailsOwnerHelper;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.utils.ImageUtil;

@Entity
@PluralDisplayName(name="languages")
@DynamicMetaValueKey(oldKey="DYN_BNDL_LANG")
public class DynamicBundleLanguage extends AplosSiteBean implements FileDetailsOwnerInter {

	private static final long serialVersionUID = -4122460771703191588L;

	private String name;
	private String languageKey = "en";
//	private String iconUrl;
	
	@ManyToOne
	private FileDetails iconDetails;
	
	@Transient
	private DynamicBundleLanguageFdoh dynamicBundleLanguageFdoh = new DynamicBundleLanguageFdoh(this);

	@Override
	public String getDisplayName() {
		return getName();
	}
	
	@Override
	public void superSaveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setLanguageKey(String languageKey) {
		this.languageKey = languageKey;
	}

	public String getLanguageKey() {
		return languageKey;
	}
	
	public String getFullIconUrl() {
		return getFullIconUrl(false);
	}

	public String getFullIconUrl(boolean addContextPath) {
		return ImageUtil.getFullFileUrl( iconDetails, addContextPath );
	}
	
	@Override
	public boolean isValidForSave() {
		return this.isNew() && super.isValidForSave();
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		FileDetails.saveFileDetailsOwner(this, LanguageImageKey.values(), currentUser);
	}

	public FileDetails getIconDetails() {
		return iconDetails;
	}

	public void setIconDetails(FileDetails iconDetails) {
		this.iconDetails = iconDetails;
	}

//#################

	public enum LanguageImageKey {
		ICON;
	}
	
	@Override
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper() {
		return dynamicBundleLanguageFdoh;
	}

	
	private class DynamicBundleLanguageFdoh extends SaveableFileDetailsOwnerHelper {
		public DynamicBundleLanguageFdoh( DynamicBundleLanguage dynamicBundleLanguage ) {
			super( dynamicBundleLanguage );
		}
		@Override
		public String getFileDetailsDirectory(String fileDetailsKey, boolean includeServerWorkPath) {
			if (LanguageImageKey.ICON.name().equals(fileDetailsKey)) {
				return CommonWorkingDirectory.LANGUAGE_ICONS.getDirectoryPath(includeServerWorkPath);
			}
			return null;
		}

		@Override
		public void setFileDetails(FileDetails fileDetails, String fileDetailsKey, Object collectionKey) {
			if (LanguageImageKey.ICON.name().equals(fileDetailsKey)) {
				setIconDetails(fileDetails);		
			}
		}

		@Override
		public FileDetails getFileDetails(String fileDetailsKey, Object collectionKey) {
			if( LanguageImageKey.ICON.name().equals( fileDetailsKey ) ) {
				return getIconDetails();
			}
			return null;
		}
	}
	
}
