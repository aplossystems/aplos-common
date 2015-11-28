package com.aplos.common.backingpage.communication;

import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.primefaces.model.UploadedFile;

import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.EmailFrame;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.EmailFromAddressType;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=EmailTemplate.class)
public class EmailTemplateEditPage extends EditPage {
	private static final long serialVersionUID = 988272985710190365L;
	private List<SelectItem> emailFrameSelectItems;
	private UploadedFile attachmentUploadedFile;

	public EmailTemplateEditPage() {
		createEmailFrameSelectItems();
	}
	
	public boolean isReadyToPrint( FileDetails saveableAttachment ) {
		if( saveableAttachment instanceof CreatedPrintTemplate ) {
			CreatedPrintTemplate createdPrintTemplate = (CreatedPrintTemplate) saveableAttachment;
			if( createdPrintTemplate.getPrintTemplate() != null ) {
				return createdPrintTemplate.getPrintTemplate().isReadyToPrint();
			}
		} 
		return true;
	}
	
	@Override
	public boolean responsePageLoad() {
		EmailTemplate emailTemplate = resolveAssociatedBean();
		JSFUtil.addToFlashViewMap(JSFUtil.getBinding(EmailTemplate.class), emailTemplate);
		return super.responsePageLoad();
	}
	
	public String getEmailTemplateContentUrl() {
		BackingPageUrl backingPageUrl = new BackingPageUrl( EmailTemplateContentPage.class );
		backingPageUrl.addContextPath();
		return backingPageUrl.toString();
	}

	public void reloadDefaultTemplate() {
		EmailTemplate emailTemplate = resolveAssociatedBean();
		emailTemplate.loadDefaultValues();
	}
	
	public List<SelectItem> getEmailFromAddressTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( EmailFromAddressType.class );
	}
	
	public void createEmailFrameSelectItems() {
		List<EmailFrame> emailFrameList = new BeanDao( EmailFrame.class ).getAll();
		setEmailFrameSelectItems(Arrays.asList( AplosBean.getSelectItemBeansWithNotSelected(emailFrameList) ));
	}

	public void addAttachment() {
		if (CommonUtil.isFileUploaded(attachmentUploadedFile)) {
			EmailTemplate emailTemplate = resolveAssociatedBean();
			
			FileDetails fileDetails = new FileDetails();
			fileDetails.setFileDetailsOwner(CommonWorkingDirectory.APLOS_EMAIL_FILE_DIR.getAplosWorkingDirectory());
			fileDetails.updateFile(attachmentUploadedFile);
			emailTemplate.getSaveableAttachments().add( fileDetails );
		}
	}
	
	@Override
	public boolean saveBean() {
		EmailTemplate emailTemplate = resolveAssociatedBean();
		if( emailTemplate.isUsingDefaultContent() ) {
			emailTemplate.setContent( emailTemplate.getDefaultContent() );
		}
		boolean beanSaved = super.saveBean();
		return beanSaved;
	}

	public List<SelectItem> getEmailFrameSelectItems() {
		return emailFrameSelectItems;
	}

	public void setEmailFrameSelectItems(List<SelectItem> emailFrameSelectItems) {
		this.emailFrameSelectItems = emailFrameSelectItems;
	}

	public UploadedFile getAttachmentUploadedFile() {
		return attachmentUploadedFile;
	}

	public void setAttachmentUploadedFile(UploadedFile attachmentUploadedFile) {
		this.attachmentUploadedFile = attachmentUploadedFile;
	}
}
