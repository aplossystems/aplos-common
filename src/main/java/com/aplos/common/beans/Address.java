package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.RemoveEmpty;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.enums.ContactTitle;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;

@Entity
@PluralDisplayName(name="addresses")
@Cache
public class Address extends AplosBean {
	private static final long serialVersionUID = -2497780881028749833L;
	private ContactTitle contactTitle;
	private String companyName;
	private String contactFirstName;
	private String contactSurname;
	private String line1;
	private String line2;
	private String line3;
	private String city;
	private String state;

	@ManyToOne
	private Country country;

	@ManyToOne
	private CountryArea countryArea;

	private String postcode;
	private String phone;
	private String phone2;
	private String mobile;
	private String fax;
	@ManyToOne
	@Cascade({CascadeType.ALL})
	@RemoveEmpty
	private Subscriber subscriber;
	private boolean isSmsSubscribed = true;

	public static final String NO_ADDRESS = "No Address";
	public static final String NO_ADDRESS_LINE_1 = "No Address line 1";
	public static final String CREATE_ADDRESS = "Create Address";

	public Address getCopy() {
		Address copyAddress = new Address();
		copyAddress.initialiseNewBean();
		copyAddress.copy( this );
		return copyAddress;
	}

	public void copyAddressOnly( Address srcAddress ) {
		if( srcAddress != null ) {
			setCompanyName( srcAddress.getCompanyName() );
			setLine1( srcAddress.getLine1() );
			setLine2( srcAddress.getLine2() );
			setLine3( srcAddress.getLine3() );
			setCity( srcAddress.getCity() );
			setState( srcAddress.getState() );
			setCountry( srcAddress.getCountry() );
			setPostcode( srcAddress.getPostcode() );
		}
	}

	public boolean validateXss() {
		if (!CommonUtil.validateXss(companyName)) {
			return false;
		}
		if (!CommonUtil.validateXss(contactFirstName)) {
			return false;
		}
		if (!CommonUtil.validateXss(contactSurname)) {
			return false;
		}
		if (!CommonUtil.validateXss(line1)) {
			return false;
		}
		if (!CommonUtil.validateXss(line2)) {
			return false;
		}
		if (!CommonUtil.validateXss(line3)) {
			return false;
		}
		if (!CommonUtil.validateXss(city)) {
			return false;
		}
		if (!CommonUtil.validateXss(state)) {
			return false;
		}
		if (!CommonUtil.validateXss(postcode)) {
			return false;
		}
		if (!CommonUtil.validateXss(phone)) {
			return false;
		}
		if (!CommonUtil.validateXss(phone2)) {
			return false;
		}
		if (!CommonUtil.validateXss(mobile)) {
			return false;
		}
		if (!CommonUtil.validateXss(fax)) {
			return false;
		}

		if (subscriber != null) {
			if (!subscriber.validateXss()) {
				return false;
			}
		}

		return true;
	}

	public boolean encodeAgainstXss() {
		CommonUtil.encodeAgainstXss(companyName);
		CommonUtil.encodeAgainstXss(contactFirstName);
		CommonUtil.encodeAgainstXss(contactSurname);
		CommonUtil.encodeAgainstXss(line1);
		CommonUtil.encodeAgainstXss(line2);
		CommonUtil.encodeAgainstXss(line3);
		CommonUtil.encodeAgainstXss(city);
		CommonUtil.encodeAgainstXss(state);
		CommonUtil.encodeAgainstXss(postcode);
		CommonUtil.encodeAgainstXss(phone);
		CommonUtil.encodeAgainstXss(phone2);
		CommonUtil.encodeAgainstXss(mobile);
		CommonUtil.encodeAgainstXss(fax);

		if (subscriber != null) {
			subscriber.encodeAgainstXss();
		}

		return true;
	}

	public void copy( Address srcAddress ) {
		copyAddressOnly( srcAddress );
		setContactFirstName( srcAddress.getContactFirstName() );
		setContactSurname( srcAddress.getContactSurname() );
		setEmailAddress( srcAddress.getEmailAddress() );
		setPhone( srcAddress.getPhone() );
		setMobile( srcAddress.getMobile() );
		setFax( srcAddress.getFax() );
	}

	public String getAddressString() {
		StringBuffer address = new StringBuffer();
		address.append(line1);
		if (line2!=null && !line2.equals("")) {
			address.append(", ");
			address.append(line2);
		}
		if (line3!=null && !line3.equals("")) {
			address.append(", ");
			address.append(line3);
		}
		if (city!=null && !city.equals("")) {
			address.append(", ");
			address.append(city);
		}
		if (state!=null && !state.equals("")) {
			address.append(", ");
			address.append(state);
		}
		if (country!=null && !country.equals("")) {
			address.append(", ");
			address.append(country);
		}
		if (postcode!=null && !postcode.equals("")) {
			address.append(", ");
			address.append(postcode.toUpperCase());
		}
		return address.toString();
	}

	public void updateStateField() {
		if ( getCountry() != null &&
				(getCountry().getId().equals( 840l ) || getCountry().getId().equals( 124l )) &&
				getCountryArea() != null ) {
			setState( getCountryArea().getAreaCode() );
		}
	}

	public String getContactFullName() {
		return FormatUtil.getFullName( getContactFirstName(), getContactSurname());
	}

	public String getContactFullName( boolean isIncludingTitle ) {
		if( isIncludingTitle ) {
			return FormatUtil.getFullName( getContactTitle(), getContactFirstName(), getContactSurname());
		} else {
			return getContactFullName();
		}
	}

	/**
	 * @deprecated
	 *
	 * This has been moved to getHtmlForEditing
	 */
	@Deprecated
	@Override
	public String toString() {
		if (line1 == null && line2 == null && line3 == null && city == null
						&& state == null && postcode == null) {
			return "New address";
		} else {
			String value = ((line1 != null && !line1.equals("")) ? line1 + ",<br />" : "") +
					//((line2 != null && !line2.equals("")) ? line2 + ",<br />" : "") +
					//((line3 != null && !line3.equals("")) ? line3 + ",<br />" : "") +
					((city != null && !city.equals("")) ? city + ",<br />" : "") +
					//((state != null && !state.equals("")) ? state + ",<br />" : "") +
					((postcode != null && !postcode.equals("")) ? postcode + "" : "");

			// Remove possible trailing comma
			value = value.replaceAll( ",<br />$", "" );
			if( value.equals( "" ) ) {
				return "Edit address";
			} else {
				return value;
			}
		}
	}

	public String getHtmlForEditing() {
		if (line1 == null && line2 == null && line3 == null && city == null
						&& state == null && postcode == null) {
			return "New address";
		} else {
			String value = ((line1 != null && !line1.equals("")) ? line1 + ",<br />" : "") +
					//((line2 != null && !line2.equals("")) ? line2 + ",<br />" : "") +
					//((line3 != null && !line3.equals("")) ? line3 + ",<br />" : "") +
					((city != null && !city.equals("")) ? city + ",<br />" : "") +
					//((state != null && !state.equals("")) ? state + ",<br />" : "") +
					((postcode != null && !postcode.equals("")) ? postcode + "" : "");

			// Remove possible trailing comma
			value = value.replaceAll( ",<br />$", "" );
			if( value.equals( "" ) ) {
				return "Edit address";
			} else {
				return value;
			}
		}
	}

	public String getToHtmlFull() {
		return toHtmlFull( "New address" );
	}

	public String toHtmlFull( String returnOnNull ) {
		return toHtmlFull(returnOnNull, true );
	}

	public String toHtmlFull( String returnOnNull, boolean includePunctuation ) {
		return toHtmlFull(returnOnNull, includePunctuation, true);
	}
	
	public String toHtmlFull( String returnOnNull, boolean includePunctuation, boolean includeLineBreaks ) {
		return toHtmlFull(returnOnNull, includePunctuation, includeLineBreaks, true );
	}

	public String toHtmlFull( String returnOnNull, boolean includePunctuation, boolean includeLineBreaks, boolean includeCompanyName ) {
		String htmlAddress;
		if( includePunctuation ) {
			if( includeLineBreaks ) {
				htmlAddress = join( ",<br/>", includeCompanyName );
			} else {
				htmlAddress = join( ", ", includeCompanyName );
			}
		} else {
			if( includeLineBreaks ) {
				htmlAddress = join( "<br/>", includeCompanyName );
			} else {
				htmlAddress = join( " ", includeCompanyName );
			}
		}
		if( htmlAddress.equals( "" ) ) {
			return returnOnNull;
		} else {
			return htmlAddress;
		}
	}

	public List<String> getCompletedAddressDetailsList( boolean includeCompanyName ) {
		List<String> addressDetails = new ArrayList<String>();
		if( companyName != null && !companyName.equals("") && includeCompanyName ) {
			addressDetails.add( companyName );
		}
		if( line1 != null && !line1.equals("") ) {
			addressDetails.add( line1 );
		}
		if( line2 != null && !line2.equals("") ) {
			addressDetails.add( line2 );
		}
		if( line3 != null && !line3.equals("") ) {
			addressDetails.add( line3 );
		}
		if( city != null && !city.equals("") ) {
			addressDetails.add( city );
		}
		if( state != null && !state.equals("") ) {
			addressDetails.add( state );
		}
		if( postcode != null && !postcode.equals("") ) {
			addressDetails.add( postcode.toUpperCase() );
		}
		return addressDetails;
	}
	
	public String join( String separator ) {
		return join( separator, true );
	}

	public String join( String separator, boolean includeCompanyName ) {
		return StringUtils.join( getCompletedAddressDetailsList( includeCompanyName ), separator );
	}

	public String getToHtmlFullOrEmpty() {
		return toHtmlFull( "" );
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine3(String line3) {
		this.line3 = line3;
	}

	public String getLine3() {
		return line3;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return city;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhone() {
		return phone;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public String getFax() {
		return fax;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getMobile() {
		return mobile;
	}

	public void setEmailAddress(String emailAddress) {
		getSubscriber().setEmailAddress(emailAddress);
	}

	public String getEmailAddress() {
		return getSubscriber().getEmailAddress();
	}

	public void setContactFirstName(String contactFirstName) {
		this.contactFirstName = contactFirstName;
	}

	public String getContactFirstName() {
		return contactFirstName;
	}

	public void setContactSurname(String contactSurname) {
		this.contactSurname = contactSurname;
	}

	public String getContactSurname() {
		return contactSurname;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public Country getCountry() {
		return country;
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		if ( countryHasStates() && countryArea != null ) {
			setState( countryArea.getAreaCode() );
		}
		line1 = CommonUtil.firstLetterToUpperCase(line1);
		line2 = CommonUtil.firstLetterToUpperCase(line2);
		line3 = CommonUtil.firstLetterToUpperCase(line3);
		city = CommonUtil.firstLetterToUpperCase(city);
		state = CommonUtil.firstLetterToUpperCase(state);
		if( postcode != null ) {
			postcode = postcode.toUpperCase();
		}
		super.saveBean(currentUser);
	}

	private boolean countryHasStates() {
		return country != null && country.getId() != null &&
				(country.getId().equals(840l) || country.getId().equals(124l));
	}

	public void setCountryArea(CountryArea countryArea) {
		this.countryArea = countryArea;
	}

	public CountryArea getCountryArea() {
		return countryArea;
	}

	public String getCityOrCountryAreaStr() {
		if ( city != null ) {
			return city;
		} else if (countryArea != null) {
			return countryArea.getName();
		} else {
			return null;
		}
	}

	public void setPhone2(String phone2) {
		this.phone2 = phone2;
	}

	public String getPhone2() {
		return phone2;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getCompanyName() {
		return companyName;
	}

	public ContactTitle getContactTitle() {
		return contactTitle;
	}

	public void setContactTitle(ContactTitle contactTitle) {
		this.contactTitle = contactTitle;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(Subscriber subscriber) {
		this.subscriber = subscriber;
	}

	public boolean isSmsSubscribed() {
		return isSmsSubscribed;
	}

	public void setSmsSubscribed(boolean isSmsSubscribed) {
		this.isSmsSubscribed = isSmsSubscribed;
	}

}
