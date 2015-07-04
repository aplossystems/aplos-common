package com.aplos.common.beans.communication;

import java.util.Date;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.enums.SingleEmailRecordStatus;
import com.aplos.common.enums.SmsStatus;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.utils.FormatUtil;

@Entity
public class SingleSmsRecord extends AplosAbstractBean {
	private static final long serialVersionUID = -578753890855228759L;
	
	@ManyToOne
	private SmsMessage smsMessage;
	private SmsStatus status = SmsStatus.UNSENT;
	private String destinationPhone;
	private Date actionedDate;
	private Date smsSentDate;
	
	@Any( metaColumn = @Column( name = "bulk_sms_source_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinColumn( name = "bulk_sms_source_id" )
    @DynamicMetaValues
	private BulkSmsSource bulkSmsSource;
	
	public SingleSmsRecord() {
	}
	
	public SingleSmsRecord( SmsMessage smsMessage, String destinationPhone, BulkSmsSource bulkSmsSource ) {
		setSmsMessage( smsMessage );
		setDestinationPhone(destinationPhone);
		setBulkSmsSource(bulkSmsSource);
	}
	
	public String getSmsSentDateTimeStr() {
		return FormatUtil.formatDateTime( getSmsSentDate(), true );
	}
	
	public String getActionedDateTimeStr() {
		return FormatUtil.formatDateTime( getActionedDate(), true );
	}

	public String getDestinationPhone() {
		return destinationPhone;
	}

	public void setDestinationPhone(String destinationPhone) {
		this.destinationPhone = destinationPhone;
	}

	public Date getActionedDate() {
		return actionedDate;
	}

	public void setActionedDate(Date actionedDate) {
		this.actionedDate = actionedDate;
	}

	public Date getSmsSentDate() {
		return smsSentDate;
	}

	public void setSmsSentDate(Date smsSentDate) {
		this.smsSentDate = smsSentDate;
	}

	public BulkSmsSource getBulkSmsSource() {
		return bulkSmsSource;
	}

	public void setBulkSmsSource(BulkSmsSource bulkSmsSource) {
		this.bulkSmsSource = bulkSmsSource;
	}

	public SmsStatus getStatus() {
		return status;
	}

	public void setStatus(SmsStatus status) {
		this.status = status;
	}

	public SmsMessage getSmsMessage() {
		return smsMessage;
	}

	public void setSmsMessage(SmsMessage smsMessage) {
		this.smsMessage = smsMessage;
	}
}
