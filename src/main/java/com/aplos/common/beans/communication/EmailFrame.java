package com.aplos.common.beans.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.AplosUrl.Protocol;
import com.aplos.common.ExternalBackingPageUrl;
import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.SaveableFileDetailsOwnerHelper;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.backingpage.communication.EmailViewerPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

@Entity
public class EmailFrame extends AplosBean implements FileDetailsOwnerInter {
	private static final long serialVersionUID = 2473278151492971069L;
	
	private String name;
	
	@Column(columnDefinition="LONGTEXT")
	private String header;
	@Column(columnDefinition="LONGTEXT")
	private String footer;
	
	@ManyToOne
	private FileDetails headerDetails;
	@ManyToOne
	private FileDetails footerDetails;
	
	private boolean isHeaderUsingSource = false;
	private boolean isFooterUsingSource = false;
	@Column(columnDefinition="LONGTEXT")
	private String htmlHead;
	
	@Transient
	private transient EmailFrameFdoh emailFrameFdoh = new EmailFrameFdoh( this );


	public enum HeaderFooterImageKey {
		HEADER,
		FOOTER;
	}

	@Override
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper() {
		return emailFrameFdoh;
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		FileDetails.saveFileDetailsOwner(this, HeaderFooterImageKey.values(), currentUser);
	}
	
	@Override
	public String getDisplayName() {
		return getName();
	}

	public String parseHeader( AplosEmail aplosEmail, SingleEmailRecord singleEmailRecord ) {
		JDynamiTe jDynamiTe = new JDynamiTe();

		try {
			jDynamiTe.setInput(new ByteArrayInputStream(getHeader().getBytes()));
			jDynamiTe.setVariable("EMAIL_SUBJECT", aplosEmail.getSubject() );
			if( singleEmailRecord.getBulkEmailSource() != null ) {
				ExternalBackingPageUrl emailViewerAplosUrl = new ExternalBackingPageUrl( aplosEmail.getParentWebsite(), EmailViewerPage.class );

				Set keySet = new HashSet();
				Enumeration variableKeys = jDynamiTe.getVariableKeys();
				while( variableKeys.hasMoreElements() ) {
					keySet.add( variableKeys.nextElement() );
				}
				
				/*
				 * The page wasn't loading in https for some reason
				 */
				emailViewerAplosUrl.setScheme(Protocol.HTTP);
				aplosEmail.addEncryptionParameters( emailViewerAplosUrl, singleEmailRecord.getBulkEmailSource() );
				jDynamiTe.setVariable("EMAIL_VIEWER_LINK", "<a href=\"" + emailViewerAplosUrl.toString() + "\" target=\"_blank\">Email not displaying correctly? View it in your browser.</a>" );
				
				EmailTemplate.addCommonJDynamiTeValues(jDynamiTe, singleEmailRecord, keySet);
			}
			jDynamiTe.parse();
		} catch( IOException ioex ) {
			ApplicationUtil.getAplosContextListener().handleError( ioex );
		}
		return jDynamiTe.toString();
	}

	public String parseFooter( AplosEmail aplosEmail, SingleEmailRecord singleEmailRecord ) {
		JDynamiTe jDynamiTe = new JDynamiTe();

		try {
			jDynamiTe.setInput(new ByteArrayInputStream(getFooter().getBytes()));
			Enumeration<String> variableKeysEnumeration = jDynamiTe.getVariableKeys();
			CompanyDetails companyDetails = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails();
			String tempVariableKey;
			String tempVariableValue = null;  
			while( variableKeysEnumeration.hasMoreElements() ) {
				tempVariableKey = variableKeysEnumeration.nextElement();
				if( tempVariableKey.startsWith( "COMPANY_" ) ) {
					tempVariableValue = CommonUtil.getStringOrEmpty( companyDetails.getJDynamiTeValue( tempVariableKey.replace( "COMPANY_", "" ) ) );
				} else if( singleEmailRecord.getBulkEmailSource() != null ) {
					tempVariableValue = singleEmailRecord.getBulkEmailSource().getJDynamiTeValue( tempVariableKey, aplosEmail );
				}
				if( tempVariableValue == null ) {
					tempVariableValue = ApplicationUtil.getAplosModuleFilterer().getJDynamiTeValue( tempVariableKey, aplosEmail, singleEmailRecord.getBulkEmailSource() );
				}
				if( tempVariableValue != null )  {
					jDynamiTe.setVariable(tempVariableKey, tempVariableValue);
				}
			}

			jDynamiTe.parse();
		} catch( IOException ioex ) {
			ApplicationUtil.getAplosContextListener().handleError( ioex );
		}
		return jDynamiTe.toString();
	}
	
	@Override
	public void superSaveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
	}
	
	public String getHeader() {
		return header;
	}
	public void setHeader(String header) {
		this.header = header;
	}
	public String getFooter() {
		return footer;
	}
	public void setFooter(String footer) {
		this.footer = footer;
	}

	public FileDetails getHeaderDetails() {
		return headerDetails;
	}

	public void setHeaderDetails(FileDetails headerDetails) {
		this.headerDetails = headerDetails;
	}

	public FileDetails getFooterDetails() {
		return footerDetails;
	}

	public void setFooterDetails(FileDetails footerDetails) {
		this.footerDetails = footerDetails;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHeaderUsingSource() {
		return isHeaderUsingSource;
	}

	public void setHeaderUsingSource(boolean isHeaderUsingSource) {
		this.isHeaderUsingSource = isHeaderUsingSource;
	}

	public boolean isFooterUsingSource() {
		return isFooterUsingSource;
	}

	public void setFooterUsingSource(boolean isFooterUsingSource) {
		this.isFooterUsingSource = isFooterUsingSource;
	}

	public String getHtmlHead() {
		return htmlHead;
	}

	public void setHtmlHead(String htmlHead) {
		this.htmlHead = htmlHead;
	}


	private class EmailFrameFdoh extends SaveableFileDetailsOwnerHelper {
		public EmailFrameFdoh( EmailFrame emailFrame ) {
			super( emailFrame );
		}
		
		@Override
		public FileDetails getFileDetails(String fileDetailsKey, Object collectionKey) {
			if (HeaderFooterImageKey.HEADER.name().equals(fileDetailsKey)) {
				return getHeaderDetails();		
			} else if (HeaderFooterImageKey.FOOTER.name().equals(fileDetailsKey)) {
				return getFooterDetails();		
			}
			return null;
		}
		
		@Override
		public void setFileDetails(FileDetails fileDetails, String fileDetailsKey, Object collectionKey) {
			if (HeaderFooterImageKey.HEADER.name().equals(fileDetailsKey)) {
				setHeaderDetails(fileDetails);		
			} else if (HeaderFooterImageKey.FOOTER.name().equals(fileDetailsKey)) {
				setFooterDetails(fileDetails);		
			}
		}
		
		@Override
		public String getFileDetailsDirectory(String fileDetailsKey, boolean includeServerWorkPath) {
			return CommonWorkingDirectory.HEADER_FOOTER_DIR.getDirectoryPath(includeServerWorkPath);
		}
	}
}
