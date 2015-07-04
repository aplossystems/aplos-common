package com.aplos.common.beans.cardsave.directintegration;

import java.math.BigDecimal;
import java.util.List;

import net.thepaymentgateway.common.NullableBool;
import net.thepaymentgateway.common.NullableInt;
import net.thepaymentgateway.paymentsystem.AddressDetails;
import net.thepaymentgateway.paymentsystem.CardDetails;
import net.thepaymentgateway.paymentsystem.CardDetailsTransaction;
import net.thepaymentgateway.paymentsystem.CreditCardDate;
import net.thepaymentgateway.paymentsystem.CrossReferenceTransaction;
import net.thepaymentgateway.paymentsystem.CustomerDetails;
import net.thepaymentgateway.paymentsystem.MerchantDetails;
import net.thepaymentgateway.paymentsystem.MessageDetails;
import net.thepaymentgateway.paymentsystem.NullableTRANSACTION_TYPE;
import net.thepaymentgateway.paymentsystem.OutGatewayOutput;
import net.thepaymentgateway.paymentsystem.OutTransactionOutputMessage;
import net.thepaymentgateway.paymentsystem.ThreeDSecureAuthentication;
import net.thepaymentgateway.paymentsystem.ThreeDSecureBrowserDetails;
import net.thepaymentgateway.paymentsystem.ThreeDSecureInputData;
import net.thepaymentgateway.paymentsystem.TransactionControl;
import net.thepaymentgateway.paymentsystem.TransactionDetails;

import com.aplos.common.AplosUrl;
import com.aplos.common.ExternalBackingPageUrl;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.payments.cardsave.CardSaveThreeDCallbackPage;
import com.aplos.common.beans.Address;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.CreditCardDetails;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.PaymentGatewayPost;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails.CardSaveUrlType;
import com.aplos.common.beans.cardsave.CardSavePost;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.CardSaveOrder;
import com.aplos.common.interfaces.PaymentGatewayDirectPost;
import com.aplos.common.interfaces.PaymentSystemCartItem;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@DynamicMetaValueKey(oldKey="SAGE_PAY")
public class CardSaveDirectPost extends CardSavePost implements PaymentGatewayDirectPost {

	private static final long serialVersionUID = 6737260960861081464L;

	@Transient
	private PageState pageState=PageState.NORMAL;

	private String crossReference = null;
	private String requestPostUrl;
    private boolean isApply3dSecure = true;
    @Transient
    private CardDetailsTransaction cardDetailsTransaction;
    @Transient
    private CrossReferenceTransaction crossReferenceTransaction;
    
    public CardSaveDirectPost() {}

    public CardSaveDirectPost( CardSaveConfigurationDetails cardSaveDetails, CardSaveOrder cardSaveOrder ) {
    	super( cardSaveDetails, cardSaveOrder );
    }

    @Override
    public void insertTestCardDetails(Address billingAddress, CreditCardDetails creditCardDetails, boolean isForThreeDAuth) {
    	if( isForThreeDAuth ) {
    		billingAddress.setContactFirstName( "Geoff" );
    		billingAddress.setContactSurname( "Wayne" );
    		billingAddress.setLine1( "113 glendower road" );
    		billingAddress.setCity( "birmingham" );
    		billingAddress.setState( "west midlands" );
    		billingAddress.setPostcode( "b421sx" );
    		billingAddress.setCountry( (Country) new BeanDao( Country.class ).get( 10002l ) );

			creditCardDetails.setCardType( CommonConfiguration.getCommonConfiguration().getVisaCardType() );
			creditCardDetails.setCardholderName( "Geoff Wayne" );
			creditCardDetails.setCardNumber( "4976350000006891" );
			creditCardDetails.setStartMonth( 1 );
			creditCardDetails.setStartYear( 7 );
			creditCardDetails.setExpiryMonth( 12 );
			creditCardDetails.setExpiryYear( 15 );
			creditCardDetails.setCvv( "341" );
    	} else {
			billingAddress.setContactFirstName( "John" );
			billingAddress.setContactSurname( "Watson" );
			billingAddress.setLine1( "32 edward street" );
			billingAddress.setCity( "Camborne" );
			billingAddress.setState( "Cornwall" );
			billingAddress.setPostcode( "tr148pa" );
			billingAddress.setCountry( (Country) new BeanDao( Country.class ).get( 10002l ) );
			
			creditCardDetails.setCardType( CommonConfiguration.getCommonConfiguration().getVisaCardType() );
			creditCardDetails.setCardholderName( "John Watson" );
			creditCardDetails.setCardNumber( "4976000000003436" );
			creditCardDetails.setStartMonth( 1 );
			creditCardDetails.setStartYear( 7 );
			creditCardDetails.setExpiryMonth( 12 );
			creditCardDetails.setExpiryYear( 15 );
			creditCardDetails.setCvv( "452" );
    	}
    }
	
	public PaymentGatewayPost recurPayment( BigDecimal partPayment ) {
		CardSaveConfigurationDetails config = CommonConfiguration.getCommonConfiguration().determineCardSaveCfgDetails();
		CardSaveDirectPost cardSaveDirectPost = new CardSaveDirectPost();
		cardSaveDirectPost.saveDetails();
		cardSaveDirectPost.setTransactionType(com.aplos.common.beans.cardsave.CardSavePost.TransactionType.SALE);
		String userAgent = null;
		if( JSFUtil.getRequest() != null ) {
			userAgent = JSFUtil.getRequest().getHeader("user-agent");	
		}
		cardSaveDirectPost.setApply3dSecure(false);
		cardSaveDirectPost.setDecTotal( partPayment );
		CrossReferenceTransaction crossReferenceTransaction = createCrossReferenceTransaction(String.valueOf(getCardSaveOrder().getId() + "-" + cardSaveDirectPost.getId()), userAgent, partPayment, config.getRecurringMerchantDetails());
		cardSaveDirectPost.setCrossReferenceTransaction(crossReferenceTransaction);
		cardSaveDirectPost.setRequestPostUrl(getCardSaveDetails().getSystemUrl(CardSaveConfigurationDetails.IntegrationType.DIRECT, CardSaveUrlType.AUTHORISE));
		cardSaveDirectPost.postRequest();
		cardSaveDirectPost.setOnlinePaymentOrder(getOnlinePaymentOrder());
		cardSaveDirectPost.saveDetails();
		return cardSaveDirectPost;
	}

	public enum PageState {
		NORMAL,
		THREE_D_REDIRECT;
	}
	
	public CardSaveConfigurationDetails getCardSaveDetails() {
		return (CardSaveConfigurationDetails) getOnlinePaymentConfigurationDetails();
	}
    
	public NullableTRANSACTION_TYPE getNullableTransactionType() {
		try {
			return new NullableTRANSACTION_TYPE(getTransactionType().name());
		} catch (Exception e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
		return null;
	}
	
	/**
	 * Sets parameters in TransactionDetails, currently unmodified from default / example
	 */
	public TransactionControl createCardSaveTransactionControl() {
	    NullableBool boEchoCardType = new NullableBool(true);
	    NullableBool boEchoAmountReceived = new NullableBool(true);
	    NullableBool boEchoAVSCheckResult = new NullableBool(true);
	    NullableBool boEchoCV2CheckResult = new NullableBool(true);
	    NullableBool boThreeDSecureOverridePolicy = new NullableBool(isApply3dSecure());
	    NullableInt nDuplicateDelay = new NullableInt(60);
	    return new TransactionControl (	
	    		boEchoCardType, 
	    		boEchoAVSCheckResult, 
	    		boEchoCV2CheckResult, 
	    		boEchoAmountReceived, 
	    		nDuplicateDelay, 
	    		null,  
	    		null, 
	    		boThreeDSecureOverridePolicy,  
	    		null,  
	    		null, 
	    		null
	    );
	}
	
	protected TransactionDetails getCardSaveTransactionDetails(BigDecimal total, String orderId, String userAgent, String crossReference ) {
			//10.10 submitted as 1010 etc - bigdecimal gives 10, we want 1000, so we multiply to move the dp
		    //watch out if we have rounded without scaling anywhere - if we have a 3rd dp it might cause rounding with intValue()
	        NullableInt nAmount = new NullableInt( new BigDecimal(100).multiply( total ).intValue() ); 
	        NullableInt nCurrencyCode = new NullableInt( getCurrency().getIso4217() ); //ISO 4217, http://en.wikipedia.org/wiki/ISO_4217
	        ThreeDSecureBrowserDetails tdsbdThreeDSecureBrowserDetails = new ThreeDSecureBrowserDetails( DeviceCategory.COMPUTER_BROWSER.value(), "*/*", userAgent);
	        MessageDetails messageDetails = null; 
	        if( crossReference != null ) {
	        	messageDetails = new MessageDetails(getNullableTransactionType(), crossReference, null );
	        } else {
	        	messageDetails = new MessageDetails(getNullableTransactionType());
	        }
	        return new TransactionDetails(messageDetails, nAmount, nCurrencyCode, orderId, getBasket(), createCardSaveTransactionControl(), tdsbdThreeDSecureBrowserDetails);
	}
    
	public CardDetails createCardSaveCardDetails(CreditCardDetails creditCardDetails) {	
			NullableInt nExpiryDateMonth = new NullableInt(creditCardDetails.getExpiryMonth());
		  	NullableInt nExpiryDateYear = new NullableInt(creditCardDetails.getExpiryYear());
	      	CreditCardDate ccdExpiryDate = new CreditCardDate(nExpiryDateMonth, nExpiryDateYear);
	      	CreditCardDate ccdStartDate = null;
	      	if( creditCardDetails.getStartMonth() != null && creditCardDetails.getStartYear() != null ) {
		      	NullableInt nStartDateMonth = new NullableInt(creditCardDetails.getStartMonth());
		      	NullableInt nStartDateYear = new NullableInt(creditCardDetails.getStartYear());
		      	ccdStartDate = new CreditCardDate(nStartDateMonth, nStartDateYear);
	      	}
	        return new CardDetails(creditCardDetails.getCardholderName(), creditCardDetails.getCardNumber(), ccdExpiryDate, ccdStartDate, CommonUtil.getStringOrEmpty(creditCardDetails.getIssueNo()), creditCardDetails.getCvv());
	}
	
	public static AddressDetails createCardSaveAddressDetails(Address address) {
			//if we select england we get id 10002, we need UK - 826 - but its not an option for some reason and this breaks the chekout
			//TODO: need to check for other special cases
			NullableInt countryCode;
			if (	address.getCountry() != null &&
					address.getCountry().getId() != null && (
					address.getCountry().getId().equals(10001l) || //NI
					address.getCountry().getId().equals(10002l) || //ENG
					address.getCountry().getId().equals(10006l) || //SCOT
					address.getCountry().getId().equals(10007l) )) { //WAL
				countryCode = new NullableInt(826);
			} else {
				countryCode = new NullableInt(address.getCountry().getId().intValue());
			}
			return new AddressDetails(address.getLine1(), address.getLine2(), address.getLine3(), "", address.getCity(), address.getState(), address.getPostcode(), countryCode);
	}
    
	public CustomerDetails createCardSaveCustomerDetails(AddressDetails billingAddress, String customerEmail, String customerTelephone) {
		return createCardSaveCustomerDetails(billingAddress, customerEmail, customerTelephone, "");
	}
	 
	public CustomerDetails createCardSaveCustomerDetails(AddressDetails billingAddress, String customerEmail, String customerTelephone, String customerIp) {
    		return new CustomerDetails(billingAddress, customerEmail, customerTelephone, customerIp);
    }
	
	@Override
	public void createRequestMessage(String userAgent, List<? extends PaymentSystemCartItem> cartItems, CreditCardDetails creditCardDetails, Currency currency) {
		setCurrency( currency );
	
		cardDetailsTransaction = createCardDetailsTransaction(
    		userAgent, //this is for 3d secure, even when we are not using it
    		creditCardDetails
		);
		
	}
	
	public void createCrossReferenceTransaction(String userAgent, BigDecimal partPayment) {
		CardSaveConfigurationDetails config = CommonConfiguration.getCommonConfiguration().determineCardSaveCfgDetails();

		BigDecimal totalAmount = getDecTotal();
		if( partPayment != null ) {
			totalAmount = partPayment;
		}
		setDecTotal( totalAmount );
		setCrossReferenceTransaction(createCrossReferenceTransaction( String.valueOf(getCardSaveOrder().getId() + "-" + getId()), userAgent, totalAmount, config.getMerchantDetails()));
	}
	
	public CrossReferenceTransaction createCrossReferenceTransaction( String orderId, String userAgent, BigDecimal partPayment, MerchantDetails merchantDetails ) {
		CrossReferenceTransaction tempCrossReferenceTransaction;
		CardSaveConfigurationDetails config = CommonConfiguration.getCommonConfiguration().determineCardSaveCfgDetails();
		 
		//CrossReferenceTransaction is the other available type
        tempCrossReferenceTransaction = null;
        if (config != null) {
        	tempCrossReferenceTransaction = new CrossReferenceTransaction(
	        		config.getRequestGatewayEntryPointList(), 
	        		merchantDetails, 
	        		getCardSaveTransactionDetails(partPayment, orderId, userAgent, crossReference), 
	        		null, 
	        		null, 
	        		""
	        );
        } else {
        	JSFUtil.addMessageForError("Cardsave configuration is null. No request created.");
        }
    	return tempCrossReferenceTransaction;
	}
	
	public CardDetailsTransaction createCardDetailsTransaction(String userAgent, CreditCardDetails creditCardDetails) {
		String emailAddress = CommonUtil.getStringOrEmpty( getBillingAddress().getEmailAddress() ); 
		String phoneNumber = CommonUtil.getStringOrEmpty( getBillingAddress().getPhone() ); 
		//CrossReferenceTransaction is the other available type
        CardDetailsTransaction cdtCardDetailsTransaction = null;
        String orderId;
        if( getCardSaveOrder() != null ) {
        	orderId = String.valueOf(getCardSaveOrder().getId());
        } else {
        	orderId = String.valueOf(getId());
        }
        
        if (getCardSaveDetails() != null) {
	     	cdtCardDetailsTransaction = new CardDetailsTransaction(
	     			getCardSaveDetails().getRequestGatewayEntryPointList(), 
	     			getCardSaveDetails().getMerchantDetails(), 
	        		getCardSaveTransactionDetails(getDecTotal(), orderId, userAgent, null), 
	        		createCardSaveCardDetails(creditCardDetails), 
	        		createCardSaveCustomerDetails(createCardSaveAddressDetails(getBillingAddress().getCopy()), emailAddress, phoneNumber), 
	        		""
	        );
        } else {
        	JSFUtil.addMessageForError("Cardsave configuration is null. No request created.");
        }
        return cdtCardDetailsTransaction;
	}
	
	public void postRequest() {
		postRequest( null, null );
	}


	//added the 3d secure strings (last 2 params) because the session will be different when processign the 3d secure so we cant access them like that
	public void postRequest( String cardsaveMD, String cardsavePAReq ) {
	
	/* Unlike with sagepay the request url is not fully built here - we have the domain/port but
	 * we need to add the subdomain - the gateway itself, because with card save we have several
	 * gateways assigned and have to try them all 
	 * 
	 * All the cardsave objects are wrapped so we are not required to handle the post ourself
	 * the wrappers handle it, including the requests to each gateway in order
	 */
	
	String defaultError = "Couldn't communicate with payment gateway";
	boolean transactionProcessed;
	OutGatewayOutput gatewayOutput = new OutGatewayOutput();
    OutTransactionOutputMessage messageToGateway = new OutTransactionOutputMessage();
	try {
		if (getStatus().equals("3DAUTH")) {
			//it appears the only thing that changes when using 3dauth is this part, sending/processing the transaction
			//we don't need a new message/ url etc like with sagepay
			CardSaveConfigurationDetails config = CommonConfiguration.getCommonConfiguration().determineCardSaveCfgDetails();
			//the values passed in are set below, line ~255			
			ThreeDSecureInputData threeDSecureData = new ThreeDSecureInputData(
					cardsaveMD, 
					cardsavePAReq
			);
	        ThreeDSecureAuthentication secureAuth = new ThreeDSecureAuthentication(
	        		config.getRequestGatewayEntryPointList(), 
	        		config.getMerchantDetails(), 
	        		threeDSecureData, 
	        		"" //passthrough data, i think it gets returned from cardsave unchanged
	        );
	        transactionProcessed = secureAuth.processTransaction(gatewayOutput, messageToGateway);
	        if (transactionProcessed) {
	        	pageState = PageState.NORMAL;
	        }
		} else { 
			if (cardDetailsTransaction != null) {
				transactionProcessed = cardDetailsTransaction.processTransaction(gatewayOutput, messageToGateway);
		        setRequestPost(cardDetailsTransaction.getLastRequest());
			} else if( getCrossReferenceTransaction() != null ) {
				transactionProcessed = getCrossReferenceTransaction().processTransaction(gatewayOutput, messageToGateway);
		        setRequestPost(getCrossReferenceTransaction().getLastRequest());
			} else {
				defaultError  = "CardDetailsTransaction was null when processed.";
				transactionProcessed=false;
			}			
		} 
        stripCardDetailsFromRequestPost();
	} catch (Exception e2) {
		defaultError += " " + e2.getMessage();
		ApplicationUtil.getAplosContextListener().handleError( e2 );
		transactionProcessed=false;
	}
	//the payment list page is searchable by status, so we need to set suitable statuses here (match sagepay)
	
    if ( !transactionProcessed ) {
    	
        // could not communicate with the payment gateway 
        // NextFormMode = PAYMENT_FORM;
    	setStatus("ERROR");
    	setPageError( defaultError );
    	setProcessed(false);
        
    } else {
		setResponseStr(gatewayOutput.Output.getMessage());
    	switch (gatewayOutput.Output.getStatusCode()) {
            case 0:
                // status code of 0 - means transaction successful 
            	if ( getTransactionType().equals(TransactionType.PREAUTH) ) {
                	setCrossReference( messageToGateway.Output.getCrossReference() );
            		setStatus("REGISTERED");
            	} else {
            		/*
            		 * Should really make a check to see if this is required as
            		 * it's only needed for recurring payments and may be a security hole
            		 */
                	setCrossReference( messageToGateway.Output.getCrossReference() );
            		setStatus("OK");
            		setPaid( true );
            	}
            	
            	setPageError( gatewayOutput.Output.getMessage() ); //should be empty
            	setProcessed( true );
                break;
            case 3:
                // status code of 3 - means 3D Secure authentication required 
            	setStatus("3DAUTH");
            	//this was in sagepay, not sure what it corresponds to here, so seeing as sage pay returns 3 things and so does card save
            	//we're assuming md is equivalent to cardsave's cross reference (because the other two - acs/pareq - match directly)
    			//threeDMd = responseAttrMap.get( "MD" ); 
    			//JSFUtil.addToTabSession( "sagepayMD", threeDMd );
            	setCrossReference( messageToGateway.Output.getCrossReference() );
            	JSFUtil.getSessionTemp().setAttribute( "cardsaveMD", crossReference );
    			JSFUtil.getSessionTemp().setAttribute( "cardsaveACSURL", messageToGateway.Output.getThreeDSecureOutputData().getACSURL() );
    			JSFUtil.getSessionTemp().setAttribute( "cardsavePAReq", messageToGateway.Output.getThreeDSecureOutputData().getPaREQ() );
    			AplosUrl threeDCallBackAplosUrl = new ExternalBackingPageUrl( CardSaveThreeDCallbackPage.class );
				JSFUtil.getSessionTemp().setAttribute( "cardsaveThreeDCallback", threeDCallBackAplosUrl.toString()  );
//    			if (JSFUtil.getExternalContext().getRequestServerName().equals("localhost")) {
//    				JSFUtil.getSessionTemp().setAttribute( "cardsaveThreeDCallback", JSFUtil.getServerUrl() + JSFUtil.getContextPath() + "/common/payments/cardsave/cardSaveThreeDCallback.jsf?=" +  );
//    			} else {
//    				JSFUtil.getSessionTemp().setAttribute( "cardsaveThreeDCallback", JSFUtil.getServerUrl().replace( "http:", "https:") + JSFUtil.getContextPath() + "/common/payments/cardsave/cardSaveThreeDCallback.jsf?jsessionid=" + JSFUtil.getRequest().getSession().getId() );
//    			}
    			setPageState(PageState.THREE_D_REDIRECT);
                break;
            case 5:
                // status code of 5 - means transaction declined 
            	if ( getCrossReference() != null ) {
            		setStatus("REJECTED");
            	} else {
            		setStatus("NOTAUTHED");
            	}
            	setPageError( gatewayOutput.Output.getMessage() );
                setProcessed(false);
                break;
            case 20:
                // status code of 20 - means duplicate transaction 
            	setStatus("INVALID"); //not sure which to use for duplicate as none maps directly
            	setPageError( gatewayOutput.Output.getMessage() );
            	//this contains details of previous (duplicate)
            	setStatusDetail( gatewayOutput.Output.getPreviousTransactionResult().getMessage() );
				try {
					if (gatewayOutput.Output.getPreviousTransactionResult().getStatusCode().getValue() == 0) {
	                	setProcessed(true);
	                } else {
	                	setProcessed(false);
	                }
				} catch (Exception e1) {
					ApplicationUtil.getAplosContextListener().handleError( e1 );
					setProcessed(false);
				}
                //TODO: need to do already-paid handling here, need to check how it currently works
                //m_boDuplicateTransaction = true;
                break;
            case 30:
                // status code of 30 - means an error occurred 
                setStatus("ERROR");
            	setPageError( gatewayOutput.Output.getMessage() );
                if (gatewayOutput.Output.getErrorMessages().getCount() > 0) {
                    String newstatus = getPageError() + "\n";
                    for (int nLoopIndex = 0; nLoopIndex < gatewayOutput.Output.getErrorMessages().getCount(); nLoopIndex++) {
                    	try {
							newstatus += "<li>" + gatewayOutput.Output.getErrorMessages().getAt(nLoopIndex) + "</li>";
						} catch (Exception e) {
							ApplicationUtil.getAplosContextListener().handleError( e );
						}
                    }
                    newstatus += "</ul>";
                    setPageError(newstatus);
                }
                setProcessed(false);
                break;
            default:
                // unhandled status code  
                setStatus("UNKNOWN");
            	setPageError( gatewayOutput.Output.getMessage() );
            	setProcessed(false);
                break;
        }

    }    
}

// no need for this, just set transactiontype to preauth and make the message as usual, example in TransactionEditPage::makeOnlinePaymentCall
//	public final void authorise() {
//		setTransactionType(TransactionType.AUTHORISE);
//		StringBuffer requestPostBuf = new StringBuffer();
//        requestPostBuf.append( "VPSProtocol=" + getSagePayDetails().getProtocol());
//        requestPostBuf.append( "&TxType=" + getTransactionType().name());  //** PAYMENT by default.  You can change this in the includes file **
//        requestPostBuf.append( "&Vendor=" + getSagePayDetails().getVendorName());
//        requestPostBuf.append( "&VendorTxCode=" ).append( getVendorTxCode() ).append( "auth" ); //** As generated above **
//
//        requestPostBuf.append( "&Amount=" + FormatUtil.formatTwoDigit( getDecTotal().doubleValue(), false ) ); //** Formatted to 2 decimal places with leading digit but no commas or currency symbols **
//        
//        try {
//    		requestPostBuf.append( "&Description=" + URLEncoder.encode( getDescription(), "UTF-8" ) );
//        } catch ( UnsupportedEncodingException uee ) {
//        	Logger.warn( uee );
//        	setPaid( false );
//        }
//
//		requestPostBuf.append( "&RelatedVPSTxId=" ).append( getVpsTxId() );
//		requestPostBuf.append( "&RelatedVendorTxCode=" ).append( getVendorTxCode() );
//		requestPostBuf.append( "&RelatedSecurityKey=" ).append( getSecurityKey() );
//		setAuthoriseRequestPost( requestPostBuf.toString() );
//	}
	
	public void setPageState(PageState pageState) {
		this.pageState = pageState;
	}

	public PageState getPageState() {
		return pageState;
	}

	public void setCrossReference(String crossReference) {
		this.crossReference = crossReference;
	}

	public String getCrossReference() {
		return crossReference;
	}
	
	@Override
	public void addToScope( JsfScope associatedBeanScope ) {
		super.addToScope( associatedBeanScope );
		addToScope(CommonUtil.getBinding(PaymentGatewayPost.class), this, associatedBeanScope);
	}

	public boolean isApply3dSecure() {
		return isApply3dSecure;
	}

	public void setApply3dSecure(boolean apply3dSecure) {
		this.isApply3dSecure = apply3dSecure;
	}

	public String getRequestPostUrl() {
		return requestPostUrl;
	}

	public void setRequestPostUrl(String requestPostUrl) {
		this.requestPostUrl = requestPostUrl;
	}

	public CrossReferenceTransaction getCrossReferenceTransaction() {
		return crossReferenceTransaction;
	}

	public void setCrossReferenceTransaction(CrossReferenceTransaction crossReferenceTransaction) {
		this.crossReferenceTransaction = crossReferenceTransaction;
	}
	
}
