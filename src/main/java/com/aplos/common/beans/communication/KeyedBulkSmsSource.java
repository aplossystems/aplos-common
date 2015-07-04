package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.InternationalNumber;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class KeyedBulkSmsSource extends AplosAbstractBean implements BulkSmsSource {
	private static final long serialVersionUID = 6742582719829874630L;
	
	@Any( metaColumn = @Column( name = "bulkSmsSource_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */ } )
    @JoinColumn( name = "bulkSmsSource_id" )
	@DynamicMetaValues
	private BulkSmsSource bulkSmsSource;
	@ManyToOne(fetch=FetchType.LAZY)
	private InternationalNumber internationalNumber;
	private String customDisplayName;
    
    public KeyedBulkSmsSource() {
	}
    
    @Override
    public boolean determineIsSmsSubscribed(SmsMessage smsMessage) {
    	return getBulkSmsSource().determineIsSmsSubscribed(smsMessage);
    }
    
    @Override
    public void setSmsSubscribed(boolean isSmsSubscribed) {
    	getBulkSmsSource().setSmsSubscribed(isSmsSubscribed);
    }
    
    public KeyedBulkSmsSource( BulkSmsSource bulkSmsSource, InternationalNumber internationalNumber ) {
    	setBulkSmsSource(bulkSmsSource);
    	setInternationalNumber(internationalNumber);
	}
    
    public KeyedBulkSmsSource( BulkSmsSource bulkSmsSource, String internationalNumber ) {
    	setBulkSmsSource(bulkSmsSource);
    	setInternationalNumber( InternationalNumber.parseMobileNumberStr( internationalNumber ) );
	}
    
    public KeyedBulkSmsSource( BulkSmsSource bulkSmsSource, String internationalNumber, String customDisplayName ) {
    	setBulkSmsSource(bulkSmsSource);
    	setInternationalNumber( InternationalNumber.parseMobileNumberStr( internationalNumber ) );
    	setCustomDisplayName( customDisplayName );
	}

	public boolean isNew() {
		return getId() == null;
	}
	
	public boolean saveDetails() {
		if( getInternationalNumber() != null ) {
			getInternationalNumber().saveDetails();
		}
		return super.saveDetails();
	}
	
	@Override
	public Long getMessageSourceId() {
		return getBulkSmsSource().getMessageSourceId();
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		HibernateUtil.initialise(getBulkSmsSource(), fullInitialisation);
//		HibernateUtil.initialise(getInternationalNumber(), fullInitialisation);
//	}
	
	@Override
	public String getSourceUniqueDisplayName() {
		if( CommonUtil.isNullOrEmpty( getCustomDisplayName() ) ) {
			StringBuffer strBuf = new StringBuffer();
			if( getBulkSmsSource() != null ) {
				strBuf.append( getBulkSmsSource() );
			}
			if( getInternationalNumber() != null ) {
				if( strBuf.length() > 0 ) {
					strBuf.append( " " );
				}
				strBuf.append( getInternationalNumber().getSafeFullNumber() );
			}
			return strBuf.toString();
		} else {
			return getCustomDisplayName();
		}
	}
	
	@Override
	public String getFirstName() {
		if( getBulkSmsSource() != null ) {
			return getBulkSmsSource().getFirstName(); 
		}
		return null;
	}
	
	@Override
	public String getSurname() {
		return getBulkSmsSource().getSurname();
	}

	public InternationalNumber getInternationalNumber() {
		return internationalNumber;
	}

	public void setInternationalNumber(InternationalNumber internationalNumber) {
		this.internationalNumber = internationalNumber;
	}

	public BulkSmsSource getBulkSmsSource() {
		return bulkSmsSource;
	}

	public void setBulkSmsSource(BulkSmsSource bulkSmsSource) {
		this.bulkSmsSource = bulkSmsSource;
	}

	public String getCustomDisplayName() {
		return customDisplayName;
	}

	public void setCustomDisplayName(String customDisplayName) {
		this.customDisplayName = customDisplayName;
	}
}
