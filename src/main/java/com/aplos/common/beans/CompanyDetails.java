package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.SaveableFileDetailsOwnerHelper;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.utils.JSFUtil;


@Entity
@PluralDisplayName(name="company details")
@DynamicMetaValueKey(oldKey="COMP_DETAILS")
public class CompanyDetails extends AplosBean implements FileDetailsOwnerInter, BulkEmailSource {

	private static final long serialVersionUID = 703422076540281994L;

	@ManyToOne(fetch=FetchType.LAZY)
	@RemoveEmpty
	@Cascade({CascadeType.ALL})
	private Address address;
	private String web;
	private String vatNo;
	private String director;
	private String directorsFirstName;
	private String directorsSurname;
	private String regNo;
//	private String logoFilename;
	private String salutation;
	@Column(columnDefinition="LONGTEXT")
	private String openingHours;
	@Column(columnDefinition="LONGTEXT")
	private String emailSignature;

	private String bankName;
	private String accountName;
	private String ibanNumber;
	private String accountNumber;
	private String sortCode;
	private String swiftCode;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private FileDetails logoDetails;
	
	@Transient 
	private CompanyDetailsFdoh companyDetailsFdoh = new CompanyDetailsFdoh(this);

	public enum CompanyDetailsImageKey {
		LOGO;
	}

	public String getDirectorsFullName() {
		List<String> directorsNameParts = new ArrayList<String>();
		if( directorsFirstName != null ) {
			directorsNameParts.add( directorsFirstName );
		}
		if( directorsSurname != null ) {
			directorsNameParts.add( directorsSurname );
		}
		return StringUtils.join( directorsNameParts, " " );
	}
	
	public enum JDynamiTeKeys {
		NAME,
		ADDRESS,
		PHONE_1,
		FAX,
		URL,
		EMAIL,
		REG_NO;
	}
	
	public String getJDynamiTeValue( String variableKey ) {
		if( variableKey.equals( JDynamiTeKeys.NAME.name() ) ) {
			return getCompanyName();
		} else if( variableKey.equals( JDynamiTeKeys.ADDRESS.name() ) ) {
			return getAddress().join( ",", true );
		} else if( variableKey.equals( JDynamiTeKeys.PHONE_1.name() ) ) {
			return getAddress().getPhone();
		} else if( variableKey.equals( JDynamiTeKeys.FAX.name() ) ) {
			return getAddress().getFax();
		} else if( variableKey.equals( JDynamiTeKeys.URL.name() ) ) {
			return getWeb();
		} else if( variableKey.equals( JDynamiTeKeys.EMAIL.name() ) ) {
			return getAddress().getSubscriber().getEmailAddress();
		} else if( variableKey.equals( JDynamiTeKeys.REG_NO.name() ) ) {
			return getRegNo();
		}
		return null;
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialise(getAddress(), fullInitialisation);
//		HibernateUtil.initialise(getLogoDetails(), fullInitialisation);
//	}
	
	@Override
	public void superSaveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
	}
	
	@Override
	public String getEmailAddress() {
		return getAddress().getSubscriber().getEmailAddress();
	}
	
	@Override
	public String getFirstName() {
		return getAddress().getContactFirstName();
	}
	
	@Override
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return null;
	}
	
	@Override
	public Long getMessageSourceId() {
		return getId();
	}
	
	@Override
	public String getSourceUniqueDisplayName() {
		StringBuffer strBuf = new StringBuffer( getCompanyName() );
		strBuf.append( " (" ).append( getId() ).append( ")" );
		return strBuf.toString();
	}
	
	@Override
	public String getSurname() {
		return getAddress().getContactSurname();
	}

	@Override
	public String getDisplayName() {
		return getAddress().getCompanyName();
	}

	public String getWeb() {
		return web;
	}

	public void setWeb(String web) {
		this.web = web;
	}

	public String getVatNo() {
		return vatNo;
	}

	public void setVatNo(String vatNo) {
		this.vatNo = vatNo;
	}

	public String getDirector() {
		return director;
	}

	public void setDirector(String director) {
		this.director = director;
	}

	public String getRegNo() {
		return regNo;
	}

	public void setRegNo(String regNo) {
		this.regNo = regNo;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( getClass() != CompanyDetails.class ) {
			addToScope( AplosBean.getBinding( CompanyDetails.class ), this, associatedBeanScope );
		}
	}

	public String getLogoUrl( boolean addContextPath, boolean addRandom ) {
		return getLogoDetails().getFullFileUrl(addContextPath);
	}
	
	@Override
	public void saveBean( SystemUser currentUser ) {
		FileDetails.saveFileDetailsOwner(this, getFileDetailsKeys(), currentUser);
	}
	
	public Enum[] getFileDetailsKeys() {
		return CompanyDetailsImageKey.values();
	}

	public String getCompanyName() {
		return getAddress().getCompanyName();
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getSalutation() {
		return salutation;
	}

	public void setOpeningHours(String openingHours) {
		this.openingHours = openingHours;
	}

	public String getOpeningHours() {
		return openingHours;
	}

	public void setEmailSignature(String emailSignature) {
		this.emailSignature = emailSignature;
	}

	public String getEmailSignature() {
		return emailSignature;
	}

	public void setDirectorsFirstName(String directorsFirstName) {
		this.directorsFirstName = directorsFirstName;
	}

	public String getDirectorsFirstName() {
		return directorsFirstName;
	}

	public void setDirectorsSurname(String directorsSurname) {
		this.directorsSurname = directorsSurname;
	}

	public String getDirectorsSurname() {
		return directorsSurname;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getBankName() {
		return bankName;
	}

	public void setIbanNumber(String ibanNumber) {
		this.ibanNumber = ibanNumber;
	}

	public String getIbanNumber() {
		return ibanNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setSortCode(String sortCode) {
		this.sortCode = sortCode;
	}

	public String getSortCode() {
		return sortCode;
	}

	public void setSwiftCode(String swiftCode) {
		this.swiftCode = swiftCode;
	}

	public String getSwiftCode() {
		return swiftCode;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getAccountName() {
		return accountName;
	}

	public FileDetails getLogoDetails() {
		return logoDetails;
	}

	public void setLogoDetails(FileDetails logoDetails) {
		this.logoDetails = logoDetails;
	}
	
	@Override
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper() {
		return companyDetailsFdoh;
	}
	
	protected class CompanyDetailsFdoh extends SaveableFileDetailsOwnerHelper {
		public CompanyDetailsFdoh( CompanyDetails companyDetails ) {
			super( companyDetails );
		}

		@Override
		public String getFileDetailsDirectory(String fileDetailsKey, boolean includeServerWorkPath) {
			if (CompanyDetailsImageKey.LOGO.name().equals(fileDetailsKey)) {
				return CommonWorkingDirectory.UPLOAD_DIR.getDirectoryPath(includeServerWorkPath);
			}
			return null;
		}

		@Override
		public void setFileDetails(FileDetails fileDetails, String fileDetailsKey, Object collectionKey) {
			if (CompanyDetailsImageKey.LOGO.name().equals(fileDetailsKey)) {
				setLogoDetails(fileDetails);		
			}
		}

		@Override
		public FileDetails getFileDetails(String fileDetailsKey, Object collectionKey) {
			if( CompanyDetailsImageKey.LOGO.name().equals( fileDetailsKey ) ) {
				return getLogoDetails();
			}
			return null;
		}
	}
	
}
