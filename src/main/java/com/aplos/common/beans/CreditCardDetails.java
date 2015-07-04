package com.aplos.common.beans;

import javax.faces.bean.CustomScoped;
import javax.faces.bean.ManagedBean;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;

@Entity
@ManagedBean
@CustomScoped(value="#{ tabSession }")
@PluralDisplayName(name="credit card details")
public class CreditCardDetails extends AplosBean {
	private static final long serialVersionUID = 2825154417556224203L;

	@ManyToOne
	private CreditCardType cardType;

	private String cardNumber;
	private String cardholderName;
	private int expiryMonth;
	private int expiryYear;
	private Integer startMonth = null;
	private Integer startYear = null;
	private Integer issueNo = null;

	/*
	 * If you save this then you are breaking PCI compliance for payment systems.
	 */
	@Transient
	private String cvv;

	public CreditCardDetails() {
		if( CommonConfiguration.getCommonConfiguration() != null ) {
			cardType = CommonConfiguration.getCommonConfiguration().getDefaultCreditCardType();
//			HibernateUtil.initialise( cardType, true );
		}
	}

	public CreditCardDetails( CreditCardDetails copyCreditCardDetails ) {
		copy(copyCreditCardDetails);
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialise(getCardType(), fullInitialisation);
//	}

	public String getHiddenCardNumber() {
		StringBuffer strBuf = new StringBuffer();
		if( getCardNumber() != null ) {
			for( int i = 0, n = getCardNumber().length() - 4; i < n; i++ ) {
				strBuf.append( "*" );
			}
			strBuf.append( getCardNumber().substring( getCardNumber().length() - 4 ) );
		}
		return strBuf.toString();
	}

	public String getExpiryDateString() {
		return CommonUtil.getStringOrEmpty( getExpiryMonth() ) + "/" + CommonUtil.getStringOrEmpty( getExpiryYear() );
	}

	public void copy( CreditCardDetails copyCreditCardDetails ) {
		this.cardType = copyCreditCardDetails.getCardType();
		this.setCardNumber(copyCreditCardDetails.getCardNumber());
		this.cardholderName = copyCreditCardDetails.getCardholderName();
		this.expiryMonth = copyCreditCardDetails.getExpiryMonth();
		this.expiryYear = copyCreditCardDetails.getExpiryYear();
		this.startMonth = copyCreditCardDetails.getStartMonth();
		this.startYear = copyCreditCardDetails.getStartYear();
		this.setCvv(copyCreditCardDetails.getCvv());
		this.issueNo = copyCreditCardDetails.getIssueNo();
	}

	public void setCardholderName(String cardholderName) {
		this.cardholderName = cardholderName;
	}
	public String getCardholderName() {
		return cardholderName;
	}
	public void setExpiryMonth(int expiryMonth) {
		this.expiryMonth = expiryMonth;
	}
	public int getExpiryMonth() {
		return expiryMonth;
	}
	public void setExpiryYear(int expiryYear) {
		this.expiryYear = expiryYear;
	}
	public int getExpiryYear() {
		return expiryYear;
	}
	public void setStartMonth(Integer startMonth) {
		this.startMonth = startMonth;
	}
	public Integer getStartMonth() {
		return startMonth;
	}
	public void setStartYear(Integer startYear) {
		this.startYear = startYear;
	}
	public Integer getStartYear() {
		return startYear;
	}
	public void setIssueNo(Integer issueNo) {
		this.issueNo = issueNo;
	}
	public Integer getIssueNo() {
		return issueNo;
	}

	public void setCardType(CreditCardType cardType) {
		this.cardType = cardType;
	}

	public CreditCardType getCardType() {
		return cardType;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCvv(String cvv) {
		this.cvv = cvv;
	}

	public String getCvv() {
		return cvv;
	}
}
