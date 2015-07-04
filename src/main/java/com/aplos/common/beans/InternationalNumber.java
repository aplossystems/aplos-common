package com.aplos.common.beans;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
public class InternationalNumber extends AplosBean {
	private static final long serialVersionUID = -8842884871542794341L;
	
	private String phoneNumber;
	private String countryCode;
	
	public InternationalNumber() {
	}
	
	public InternationalNumber( String countryCode, String phoneNumber ) {
		this.countryCode = countryCode;
		this.phoneNumber = phoneNumber;
	}
	
	@Override
	public boolean isEmptyBean() {
		return super.isEmptyBean() 
				&& CommonUtil.isNullOrEmpty( getPhoneNumber() ) 
				&& CommonUtil.isNullOrEmpty( getCountryCode() );
	}
	
	public void copy(InternationalNumber internationalNumber) {
		super.copy(internationalNumber);
		setCountryCode( internationalNumber.getCountryCode() );
		setPhoneNumber( internationalNumber.getPhoneNumber() );
	}
	
	public static InternationalNumber parseMobileNumberStr( String mobileNumberStr ) {
		if( !CommonUtil.isNullOrEmpty(mobileNumberStr) ) {
			String countryCode;
			String mobileNumber;
			
			if( mobileNumberStr.startsWith( "+" ) ) {
				mobileNumberStr = mobileNumberStr.substring( 1 );
			}
			if( mobileNumberStr.startsWith( "00" ) ) {
				mobileNumberStr = mobileNumberStr.substring( 2 );
			}
			
			if( mobileNumberStr.length() > 2 ) {
				if( mobileNumberStr.startsWith( "0" ) ) {
					countryCode = "44";
					mobileNumber = mobileNumberStr.substring( 1 );
				} else {
					countryCode = mobileNumberStr.substring( 0, 2 );
					mobileNumber = mobileNumberStr.substring( 2 );
				}
			} else {
				countryCode = "44";
				mobileNumber = mobileNumberStr;
			}
			
			return new InternationalNumber( countryCode, mobileNumber );
		} else {
			return null;
		}	
	}
	
	public String getSafeFullNumber() {
		return FormatUtil.stripNonNumeric(getCountryCode() + getPhoneNumber());
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
}
