package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.enums.SingleEmailRecordStatus;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.utils.FormatUtil;

@Entity
public class SingleEmailRecord extends AplosAbstractBean {
	private static final long serialVersionUID = -8259254452213082306L;
	
	@ManyToOne
	private AplosEmail aplosEmail;
	private SingleEmailRecordStatus status = SingleEmailRecordStatus.UNSENT;
	private Date openedDate;
	private Date actionedDate;
	@CollectionOfElements
	private List<String> toAddresses = new ArrayList<String>();
	private Date emailSentDate;
	@ManyToMany(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private List<FileDetails> saveableAttachments = new ArrayList<FileDetails>();
	
	@Any( metaColumn = @Column( name = "bulk_email_source_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinColumn( name = "bulk_email_source_id" )
    @DynamicMetaValues
	private BulkEmailSource bulkEmailSource;
	
	public SingleEmailRecord() {
	}
	
	public SingleEmailRecord( AplosEmail aplosEmail, List<String> toAddresses ) {
		setAplosEmail( aplosEmail );
		getToAddresses().addAll(toAddresses);

		for( FileDetails tempFileDetails : aplosEmail.getSaveableAttachments() ) {
			getSaveableAttachments().add( tempFileDetails );
		}
	}
	
	public SingleEmailRecord( AplosEmail aplosEmail, BulkEmailSource bulkEmailSource ) {
		setAplosEmail( aplosEmail );
		if (bulkEmailSource != null) {
			getToAddresses().add(bulkEmailSource.getEmailAddress());
			setBulkEmailSource(bulkEmailSource);
		}

		for( FileDetails tempFileDetails : aplosEmail.getSaveableAttachments() ) {
			if( tempFileDetails instanceof CreatedPrintTemplate && getAplosEmail().getEmailTemplate() != null ) {
				CreatedPrintTemplate createdPrintTemplate = (CreatedPrintTemplate) tempFileDetails;
				if( !createdPrintTemplate.getPrintTemplate().isReadyToPrint() ) {
					getSaveableAttachments().add( getAplosEmail().getEmailTemplate().generateAttachment(createdPrintTemplate, this, bulkEmailSource, getAplosEmail().determineEmailGenerator(this) ) );
					continue;
				}
			}
			getSaveableAttachments().add( tempFileDetails );
		}
	}
	
	public String getFirstEmailAddress() {
		if( getToAddresses().size() > 0 ) {
			return getToAddresses().get( 0 );
		}
		return null;
	}
	
	public String getEmailSentDateTimeStr() {
		return FormatUtil.formatDateTime( getEmailSentDate(), true );
	}
	
	public String getOpenedDateTimeStr() {
		return FormatUtil.formatDateTime( getOpenedDate(), true );
	}
	
	public String getActionedDateTimeStr() {
		return FormatUtil.formatDateTime( getActionedDate(), true );
	}
	
	public AplosEmail getAplosEmail() {
		return aplosEmail;
	}
	public void setAplosEmail(AplosEmail aplosEmail) {
		this.aplosEmail = aplosEmail;
	}
	public SingleEmailRecordStatus getStatus() {
		return status;
	}
	public void setStatus(SingleEmailRecordStatus status) {
		this.status = status;
	}
	public BulkEmailSource getBulkEmailSource() {
		return bulkEmailSource;
	}
	public void setBulkEmailSource(BulkEmailSource bulkEmailSource) {
		this.bulkEmailSource = bulkEmailSource;
	}

	public Date getEmailSentDate() {
		return emailSentDate;
	}

	public void setEmailSentDate(Date emailSentDate) {
		this.emailSentDate = emailSentDate;
	}

	public Date getOpenedDate() {
		return openedDate;
	}

	public void setOpenedDate(Date openedDate) {
		this.openedDate = openedDate;
	}

	public Date getActionedDate() {
		return actionedDate;
	}

	public void setActionedDate(Date actionedDate) {
		this.actionedDate = actionedDate;
	}

	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setToAddresses(List<String> toAddresses) {
		this.toAddresses = toAddresses;
	}

	public List<FileDetails> getSaveableAttachments() {
		return saveableAttachments;
	}

	public void setSaveableAttachments(List<FileDetails> saveableAttachments) {
		this.saveableAttachments = saveableAttachments;
	}
}
