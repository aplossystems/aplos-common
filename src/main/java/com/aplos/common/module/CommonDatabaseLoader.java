package com.aplos.common.module;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.CompanyDetails;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.CountryArea;
import com.aplos.common.beans.CreditCardType;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.communication.BasicEmailFolder;
import com.aplos.common.enums.VatExemption;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class CommonDatabaseLoader extends DatabaseLoader {
	public CommonDatabaseLoader( AplosModuleImpl aplosModule ) {
		super( aplosModule );
	}

	@Override
	public void loadTables() {
		SystemUser systemUser = CommonConfiguration.getCommonConfiguration().getDefaultAdminUser();
		createCreditCardTypes(systemUser);
		createDefaultCurrency(systemUser);
		createDefaultCompanyDetails(systemUser);
		createCountries();

		if( getCommonModuleDbConfig().isCountryAreaUsed() ) {
			createCountryAreas();
		}
	}
	
	@Override
	public void newTableAdded(Class<?> tableClass) {	
		if( tableClass == VatType.class ) {
			createVatTypes( JSFUtil.getLoggedInUser() );
		} else if( tableClass == BasicEmailFolder.class ) {
			BasicEmailFolder inboxEmailFolder = new BasicEmailFolder( "Inbox" );
			inboxEmailFolder.saveDetails();
			CommonConfiguration.getCommonConfiguration().setInboxEmailFolder( inboxEmailFolder );
			BasicEmailFolder sentEmailFolder = new BasicEmailFolder( "Sent email" );
			sentEmailFolder.saveDetails();
			CommonConfiguration.getCommonConfiguration().setSentEmailFolder( sentEmailFolder );
		}
	}

	public static void createVatTypes( SystemUser systemUser ) {
		VatType vatType = new VatType();
		vatType.setCode("UK");
		vatType.setPercentage( new BigDecimal( 20 ) );
		vatType.setZeroRateAllowed( false );
		vatType.saveDetails( systemUser );
		CommonConfiguration.getCommonConfiguration().setUkVatType( vatType );
		CommonConfiguration.getCommonConfiguration().setDefaultVatType( vatType );
		CommonConfiguration.getCommonConfiguration().saveDetails();

		vatType = new VatType();
		vatType.setCode("EU");
		vatType.setPercentage( new BigDecimal( 20 ) );
		vatType.setZeroRateAllowed( true );
		vatType.saveDetails( systemUser );

		vatType = new VatType();
		vatType.setCode("Z");
		vatType.setPercentage( new BigDecimal( 0 ) );
		vatType.setZeroRateAllowed( false );
		vatType.saveDetails( systemUser );
	}

	public CommonModule getAplosCommonModule() {
		return (CommonModule) getAplosModule();
	}

	public CommonModuleDbConfig getCommonModuleDbConfig() {
		return (CommonModuleDbConfig) getAplosCommonModule().getModuleDbConfig();
	}

	public static void createDefaultCompanyDetails( SystemUser systemUser ) {
		CompanyDetails companyDetails = new CompanyDetails();
		companyDetails.initialiseNewBean();
		companyDetails.getAddress().setCompanyName( "My Company" );
		companyDetails.saveDetails(systemUser);
	}

	public static void createDefaultCurrency( SystemUser systemUser ) {
		Currency currency = new Currency();
		currency.setPrefix("\u00A3");
		currency.setSymbol( "GBP" );
		currency.setIso4217( 826 );
		currency.setBaseRate( new BigDecimal( 1 ) );
		currency.saveDetails(systemUser);
		CommonConfiguration.getCommonConfiguration().setDefaultCurrency(currency);
		CommonConfiguration.getCommonConfiguration().saveDetails();

		currency = new Currency();
		currency.setPrefix("\u20AC");
		currency.setSymbol( "EUR" );
		currency.setIso4217( 978 );
		currency.setBaseRate( new BigDecimal( 1 ) );
		currency.saveDetails(systemUser);

		currency = new Currency();
		currency.setPrefix("$");
		currency.setSymbol( "USD" );
		currency.setIso4217( 840 );
		currency.setBaseRate( new BigDecimal( 1 ) );
		currency.saveDetails(systemUser);

		/*
		 INSERT INTO Currency ( prefix, symbol, baseRate ) VALUES ( '\u00A3', 'GBP', 1 );
		 INSERT INTO Currency ( prefix, symbol, baseRate ) VALUES ( '\u20AC', 'EUR', 1 );
		 INSERT INTO Currency ( prefix, symbol, baseRate ) VALUES ( '$', 'USD', 1 );
		 */
	}

	public static void createCreditCardTypes( SystemUser systemUser ) {
		CreditCardType creditCardType = new CreditCardType();
		creditCardType.setId( 1l );
		creditCardType.setName( "American Express" );
		creditCardType.setSagePayTag( "AMEX" );
		creditCardType.setPayPalTag( "Amex" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 2l );
		creditCardType.setName( "JCB" );
		creditCardType.setSagePayTag( "JCB" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 3l );
		creditCardType.setName( "Maestro" );
		creditCardType.setSagePayTag( "MAESTRO" );
		creditCardType.setPayPalTag( "Maestro" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 4l );
		creditCardType.setName( "Mastercard" );
		creditCardType.setSagePayTag( "MC" );
		creditCardType.setPayPalTag( "MasterCard" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 5l );
		creditCardType.setName( "Solo" );
		creditCardType.setSagePayTag( "SOLO" );
		creditCardType.setPayPalTag( "Maestro" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 6l );
		creditCardType.setName( "Switch" );
		creditCardType.setSagePayTag( "MAESTRO" );
		creditCardType.setPayPalTag( "Maestro" );
		creditCardType.saveDetails(systemUser);

		creditCardType = new CreditCardType();
		creditCardType.setId( 7l );
		creditCardType.setName( "Visa" );
		creditCardType.setSagePayTag( "VISA" );
		creditCardType.setPayPalTag( "Visa" );
		creditCardType.saveDetails(systemUser);
		CommonConfiguration.getCommonConfiguration().setDefaultCreditCardType(creditCardType);
		CommonConfiguration.getCommonConfiguration().setVisaCardType(creditCardType);
		CommonConfiguration.getCommonConfiguration().saveDetails();

//		creditCardType = new CreditCardType();
//		creditCardType.setId( 8l );
//		creditCardType.setName( "PayPal" );
//		creditCardType.setSagePayTag( "PAYPAL" );
//		creditCardType.aqlSaveDetails(getAdminUser(), getHibernateSession());

		/*
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 1, 'American Express', true, 'AMEX' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 2, 'JCB', true, 'JCB' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 3, 'Maestro', true, 'MAESTRO' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 4, 'Mastercard', true, 'MC' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 5, 'Solo', true, 'SOLO' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 6, 'Switch', true, 'MAESTRO' );
		 INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 7, 'Visa', true, 'VISA' );
		 //INSERT INTO CreditCardType ( id, name, active, sagePayTag ) VALUES ( 8, 'PayPal', true, 'PAYPAL' );
		 */
	}

	public void createCountries() {
		Object[][] countries = {
				{4l, "AF", "AFG", "Afghanistan", VatExemption.EXEMPT, VatExemption.EXEMPT},
				{248l, "AX", "ALA", "Aland Islands", VatExemption.EXEMPT},
				{8l, "AL", "ALB", "Albania", VatExemption.EXEMPT},
				{12l, "DZ", "DZA", "Algeria", VatExemption.EXEMPT},
				{16l, "AS", "ASM", "American Samoa", VatExemption.EXEMPT},
				{20l, "AD", "AND", "Andorra", VatExemption.EXEMPT},
				{24l, "AO", "AGO", "Angola", VatExemption.EXEMPT},
				{660l, "AI", "AIA", "Anguilla", VatExemption.EXEMPT},
				{10l, "AQ", "ATA", "Antarctica", VatExemption.EXEMPT},
				{28l, "AG", "ATG", "Antigua and Barbuda", VatExemption.EXEMPT},
				{32l, "AR", "ARG", "Argentina", VatExemption.EXEMPT},
				{51l, "AM", "ARM", "Armenia", VatExemption.EXEMPT},
				{533l, "AW", "ABW", "Aruba", VatExemption.EXEMPT},
				{36l, "AU", "AUS", "Australia", VatExemption.EXEMPT},
				{40l, "AT", "AUT", "Austria", VatExemption.EU_EXEMPT},
				{31l, "AZ", "AZE", "Azerbaijan", VatExemption.EXEMPT},
				{44l, "BS", "BHS", "Bahamas", VatExemption.EXEMPT},
				{48l, "BH", "BHR", "Bahrain", VatExemption.EXEMPT},
				{50l, "BD", "BGD", "Bangladesh", VatExemption.EXEMPT},
				{52l, "BB", "BRB", "Barbados", VatExemption.EXEMPT},
				{112l, "BY", "BLR", "Belarus", VatExemption.EXEMPT},
				{56l, "BE", "BEL", "Belgium", VatExemption.EU_EXEMPT},
				{84l, "BZ", "BLZ", "Belize", VatExemption.EXEMPT},
				{204l, "BJ", "BEN", "Benin", VatExemption.EXEMPT},
				{60l, "BM", "BMU", "Bermuda", VatExemption.EXEMPT},
				{64l, "BT", "BTN", "Bhutan", VatExemption.EXEMPT},
				{68l, "BO", "BOL", "Bolivia", VatExemption.EXEMPT},
				{70l, "BA", "BIH", "Bosnia and Herzegovina", VatExemption.EXEMPT},
				{72l, "BW", "BWA", "Botswana", VatExemption.EXEMPT},
				{74l, "BV", "BVT", "Bouvet Island", VatExemption.EXEMPT},
				{76l, "BR", "BRA", "Brazil", VatExemption.EXEMPT},
				{86l, "IO", "IOT", "British Indian Ocean Territory", VatExemption.EXEMPT},
				{96l, "BN", "BRN", "Brunei Darussalam", VatExemption.EXEMPT},
				{100l, "BG", "BGR", "Bulgaria", VatExemption.EU_EXEMPT},
				{854l, "BF", "BFA", "Burkina Faso", VatExemption.EXEMPT},
				{108l, "BI", "BDI", "Burundi", VatExemption.EXEMPT},
				{116l, "KH", "KHM", "Cambodia", VatExemption.EXEMPT},
				{120l, "CM", "CMR", "Cameroon", VatExemption.EXEMPT},
				{124l, "CA", "CAN", "Canada", VatExemption.EXEMPT},
				{132l, "CV", "CPV", "Cape Verde", VatExemption.EXEMPT},
				{136l, "KY", "CYM", "Cayman Islands", VatExemption.EXEMPT},
				{140l, "CF", "CAF", "Central African Republic", VatExemption.EXEMPT},
				{148l, "TD", "TCD", "Chad", VatExemption.EXEMPT},
				{152l, "CL", "CHL", "Chile", VatExemption.EXEMPT},
				{156l, "CN", "CHN", "China", VatExemption.EXEMPT},
				{162l, "CX", "CXR", "Christmas Island", VatExemption.EXEMPT},
				{166l, "CC", "CCK", "Cocos (Keeling) Islands", VatExemption.EXEMPT},
				{170l, "CO", "COL", "Colombia", VatExemption.EXEMPT},
				{174l, "KM", "COM", "Comoros", VatExemption.EXEMPT},
				{178l, "CG", "COG", "Congo", VatExemption.EXEMPT},
				{180l, "CD", "COD", "Congo, Democratic Republic of the", VatExemption.EXEMPT},
				{184l, "CK", "COK", "Cook Islands", VatExemption.EXEMPT},
				{188l, "CR", "CRI", "Costa Rica", VatExemption.EXEMPT},
				{384l, "CI", "CIV", "Cote d'Ivoire", VatExemption.EXEMPT},
				{191l, "HR", "HRV", "Croatia", VatExemption.EXEMPT},
				{192l, "CU", "CUB", "Cuba", VatExemption.EXEMPT},
				{196l, "CY", "CYP", "Cyprus", VatExemption.EU_EXEMPT},
				{203l, "CZ", "CZE", "Czech Republic", VatExemption.EU_EXEMPT},
				{208l, "DK", "DNK", "Denmark", VatExemption.EU_EXEMPT},
				{262l, "DJ", "DJI", "Djibouti", VatExemption.EXEMPT},
				{212l, "DM", "DMA", "Dominica", VatExemption.EXEMPT},
				{214l, "DO", "DOM", "Dominican Republic", VatExemption.EXEMPT},
				{218l, "EC", "ECU", "Ecuador", VatExemption.EXEMPT},
				{818l, "EG", "EGY", "Egypt", VatExemption.EXEMPT},
				{222l, "SV", "SLV", "El Salvador", VatExemption.EXEMPT},
				{226l, "GQ", "GNQ", "Equatorial Guinea", VatExemption.EXEMPT},
				{232l, "ER", "ERI", "Eritrea", VatExemption.EXEMPT},
				{233l, "EE", "EST", "Estonia", VatExemption.EU_EXEMPT},
				{231l, "ET", "ETH", "Ethiopia", VatExemption.EXEMPT},
				{238l, "FK", "FLK", "Falkland Islands (Malvinas)", VatExemption.EXEMPT},
				{234l, "FO", "FRO", "Faroe Islands", VatExemption.EXEMPT},
				{242l, "FJ", "FJI", "Fiji", VatExemption.EXEMPT},
				{246l, "FI", "FIN", "Finland", VatExemption.EU_EXEMPT},
				{250l, "FR", "FRA", "France", VatExemption.EU_EXEMPT},
				{254l, "GF", "GUF", "French Guiana", VatExemption.EXEMPT},
				{258l, "PF", "PYF", "French Polynesia", VatExemption.EXEMPT},
				{260l, "TF", "ATF", "French Southern Territories", VatExemption.EXEMPT},
				{266l, "GA", "GAB", "Gabon", VatExemption.EXEMPT},
				{270l, "GM", "GMB", "Gambia", VatExemption.EXEMPT},
				{268l, "GE", "GEO", "Georgia", VatExemption.EXEMPT},
				{276l, "DE", "DEU", "Germany", VatExemption.EU_EXEMPT},
				{288l, "GH", "GHA", "Ghana", VatExemption.EXEMPT},
				{292l, "GI", "GIB", "Gibraltar", VatExemption.EU_EXEMPT},
				{300l, "GR", "GRC", "Greece", VatExemption.EU_EXEMPT},
				{304l, "GL", "GRL", "Greenland", VatExemption.EXEMPT},
				{308l, "GD", "GRD", "Grenada", VatExemption.EXEMPT},
				{312l, "GP", "GLP", "Guadeloupe", VatExemption.EXEMPT},
				{316l, "GU", "GUM", "Guam", VatExemption.EXEMPT},
				{320l, "GT", "GTM", "Guatemala", VatExemption.EXEMPT},
				{831l, "GG", "GGY", "Guernsey", VatExemption.EXEMPT},
				{324l, "GN", "GIN", "Guinea", VatExemption.EXEMPT},
				{624l, "GW", "GNB", "Guinea-Bissau", VatExemption.EXEMPT},
				{328l, "GY", "GUY", "Guyana", VatExemption.EXEMPT},
				{332l, "HT", "HTI", "Haiti", VatExemption.EXEMPT},
				{334l, "HM", "HMD", "Heard Island and McDonald Islands", VatExemption.EXEMPT},
				{336l, "VA", "VAT", "Holy See (Vatican City State)", VatExemption.EXEMPT},
				{340l, "HN", "HND", "Honduras", VatExemption.EXEMPT},
				{344l, "HK", "HKG", "Hong Kong", VatExemption.EXEMPT},
				{348l, "HU", "HUN", "Hungary", VatExemption.EU_EXEMPT},
				{352l, "IS", "ISL", "Iceland", VatExemption.EXEMPT},
				{356l, "IN", "IND", "India", VatExemption.EXEMPT},
				{360l, "ID", "IDN", "Indonesia", VatExemption.EXEMPT},
				{364l, "IR", "IRN", "Iran, Islamic Republic of", VatExemption.EXEMPT},
				{368l, "IQ", "IRQ", "Iraq", VatExemption.EXEMPT},
				{372l, "IE", "IRL", "Ireland", VatExemption.EU_EXEMPT},
				{833l, "IM", "IMN", "Isle of Man", VatExemption.EXEMPT},
				{376l, "IL", "ISR", "Israel", VatExemption.EXEMPT},
				{380l, "IT", "ITA", "Italy", VatExemption.EU_EXEMPT},
				{388l, "JM", "JAM", "Jamaica", VatExemption.EXEMPT},
				{392l, "JP", "JPN", "Japan", VatExemption.EXEMPT},
				{832l, "JE", "JEY", "Jersey", VatExemption.EXEMPT},
				{400l, "JO", "JOR", "Jordan", VatExemption.EXEMPT},
				{398l, "KZ", "KAZ", "Kazakhstan", VatExemption.EXEMPT},
				{404l, "KE", "KEN", "Kenya", VatExemption.EXEMPT},
				{296l, "KI", "KIR", "Kiribati", VatExemption.EXEMPT},
				{408l, "KP", "PRK", "North Korea", VatExemption.EXEMPT},
				{410l, "KR", "KOR", "South Korea", VatExemption.EXEMPT},
				{414l, "KW", "KWT", "Kuwait", VatExemption.EXEMPT},
				{417l, "KG", "KGZ", "Kyrgyzstan", VatExemption.EXEMPT},
				{418l, "LA", "LAO", "Lao People's Democratic Republic", VatExemption.EXEMPT},
				{428l, "LV", "LVA", "Latvia", VatExemption.EU_EXEMPT},
				{422l, "LB", "LBN", "Lebanon", VatExemption.EXEMPT},
				{426l, "LS", "LSO", "Lesotho", VatExemption.EXEMPT},
				{430l, "LR", "LBR", "Liberia", VatExemption.EXEMPT},
				{434l, "LY", "LBY", "Libyan Arab Jamahiriya", VatExemption.EXEMPT},
				{438l, "LI", "LIE", "Liechtenstein", VatExemption.EXEMPT},
				{440l, "LT", "LTU", "Lithuania", VatExemption.EU_EXEMPT},
				{442l, "LU", "LUX", "Luxembourg", VatExemption.EU_EXEMPT},
				{446l, "MO", "MAC", "Macao", VatExemption.EXEMPT},
				{807l, "MK", "MKD", "Macedonia, the former Yugoslav Republic of", VatExemption.EXEMPT},
				{450l, "MG", "MDG", "Madagascar", VatExemption.EXEMPT},
				{454l, "MW", "MWI", "Malawi", VatExemption.EXEMPT},
				{458l, "MY", "MYS", "Malaysia", VatExemption.EXEMPT},
				{462l, "MV", "MDV", "Maldives", VatExemption.EXEMPT},
				{466l, "ML", "MLI", "Mali", VatExemption.EXEMPT},
				{470l, "MT", "MLT", "Malta", VatExemption.EU_EXEMPT},
				{584l, "MH", "MHL", "Marshall Islands", VatExemption.EXEMPT},
				{474l, "MQ", "MTQ", "Martinique", VatExemption.EU_EXEMPT},
				{478l, "MR", "MRT", "Mauritania", VatExemption.EXEMPT},
				{480l, "MU", "MUS", "Mauritius", VatExemption.EXEMPT},
				{175l, "YT", "MYT", "Mayotte", VatExemption.EXEMPT},
				{484l, "MX", "MEX", "Mexico", VatExemption.EXEMPT},
				{583l, "FM", "FSM", "Micronesia, Federated States of", VatExemption.EXEMPT},
				{498l, "MD", "MDA", "Moldova", VatExemption.EXEMPT},
				{492l, "MC", "MCO", "Monaco", VatExemption.EU_EXEMPT},
				{496l, "MN", "MNG", "Mongolia", VatExemption.EXEMPT},
				{499l, "ME", "MNE", "Montenegro", VatExemption.EXEMPT},
				{500l, "MS", "MSR", "Montserrat", VatExemption.EXEMPT},
				{504l, "MA", "MAR", "Morocco", VatExemption.EXEMPT},
				{508l, "MZ", "MOZ", "Mozambique", VatExemption.EXEMPT},
				{104l, "MM", "MMR", "Myanmar", VatExemption.EXEMPT},
				{516l, "NA", "NAM", "Namibia", VatExemption.EXEMPT},
				{520l, "NR", "NRU", "Nauru", VatExemption.EXEMPT},
				{524l, "NP", "NPL", "Nepal", VatExemption.EXEMPT},
				{528l, "NL", "NLD", "Netherlands", VatExemption.EU_EXEMPT},
				{530l, "AN", "ANT", "Netherlands Antilles", VatExemption.EXEMPT},
				{540l, "NC", "NCL", "New Caledonia", VatExemption.EXEMPT},
				{554l, "NZ", "NZL", "New Zealand", VatExemption.EXEMPT},
				{558l, "NI", "NIC", "Nicaragua", VatExemption.EXEMPT},
				{562l, "NE", "NER", "Niger", VatExemption.EXEMPT},
				{566l, "NG", "NGA", "Nigeria", VatExemption.EXEMPT},
				{570l, "NU", "NIU", "Niue", VatExemption.EXEMPT},
				{574l, "NF", "NFK", "Norfolk Island", VatExemption.EXEMPT},
				{580l, "MP", "MNP", "Northern Mariana Islands", VatExemption.EXEMPT},
				{578l, "NO", "NOR", "Norway", VatExemption.EXEMPT},
				{512l, "OM", "OMN", "Oman", VatExemption.EXEMPT},
				{586l, "PK", "PAK", "Pakistan", VatExemption.EXEMPT},
				{585l, "PW", "PLW", "Palau", VatExemption.EXEMPT},
				{275l, "PS", "PSE", "Palestinian Territory, Occupied", VatExemption.EXEMPT},
				{591l, "PA", "PAN", "Panama", VatExemption.EXEMPT},
				{598l, "PG", "PNG", "Papua New Guinea", VatExemption.EXEMPT},
				{600l, "PY", "PRY", "Paraguay", VatExemption.EXEMPT},
				{604l, "PE", "PER", "Peru", VatExemption.EXEMPT},
				{608l, "PH", "PHL", "Philippines", VatExemption.EXEMPT},
				{612l, "PN", "PCN", "Pitcairn", VatExemption.EXEMPT},
				{616l, "PL", "POL", "Poland", VatExemption.EU_EXEMPT},
				{620l, "PT", "PRT", "Portugal", VatExemption.EU_EXEMPT},
				{630l, "PR", "PRI", "Puerto Rico", VatExemption.EXEMPT},
				{634l, "QA", "QAT", "Qatar", VatExemption.EXEMPT},
				{638l, "RE", "REU", "Reunion", VatExemption.EXEMPT},
				{642l, "RO", "ROU", "Romania", VatExemption.EU_EXEMPT},
				{643l, "RU", "RUS", "Russian Federation", VatExemption.EXEMPT},
				{646l, "RW", "RWA", "Rwanda", VatExemption.EXEMPT},
				{652l, "BL", "BLM", "Saint Bartholemy", VatExemption.EXEMPT},
				{654l, "SH", "SHN", "Saint Helena", VatExemption.EXEMPT},
				{659l, "KN", "KNA", "Saint Kitts and Nevis", VatExemption.EXEMPT},
				{662l, "LC", "LCA", "Saint Lucia", VatExemption.EXEMPT},
				{663l, "MF", "MAF", "Saint Martin (French part)", VatExemption.EXEMPT},
				{666l, "PM", "SPM", "Saint Pierre and Miquelon", VatExemption.EXEMPT},
				{670l, "VC", "VCT", "Saint Vincent and the Grenadines", VatExemption.EXEMPT},
				{882l, "WS", "WSM", "Samoa", VatExemption.EXEMPT},
				{674l, "SM", "SMR", "San Marino", VatExemption.EXEMPT},
				{678l, "ST", "STP", "Sao Tome and Principe", VatExemption.EXEMPT},
				{682l, "SA", "SAU", "Saudi Arabia", VatExemption.EXEMPT},
				{686l, "SN", "SEN", "Senegal", VatExemption.EXEMPT},
				{688l, "RS", "SRB", "Serbia", VatExemption.EXEMPT},
				{690l, "SC", "SYC", "Seychelles", VatExemption.EXEMPT},
				{694l, "SL", "SLE", "Sierra Leone", VatExemption.EXEMPT},
				{702l, "SG", "SGP", "Singapore", VatExemption.EXEMPT},
				{703l, "SK", "SVK", "Slovakia", VatExemption.EU_EXEMPT},
				{705l, "SI", "SVN", "Slovenia", VatExemption.EU_EXEMPT},
				{90l, "SB", "SLB", "Solomon Islands", VatExemption.EXEMPT},
				{706l, "SO", "SOM", "Somalia", VatExemption.EXEMPT},
				{710l, "ZA", "ZAF", "South Africa", VatExemption.EXEMPT},
				{239l, "GS", "SGS", "South Georgia and the South Sandwich Islands", VatExemption.EXEMPT},
				{724l, "ES", "ESP", "Spain", VatExemption.EU_EXEMPT},
				{144l, "LK", "LKA", "Sri Lanka", VatExemption.EXEMPT},
				{736l, "SD", "SDN", "Sudan", VatExemption.EXEMPT},
				{740l, "SR", "SUR", "SuriName, active", VatExemption.EXEMPT},
				{744l, "SJ", "SJM", "Svalbard and Jan Mayen", VatExemption.EXEMPT},
				{748l, "SZ", "SWZ", "Swaziland", VatExemption.EXEMPT},
				{752l, "SE", "SWE", "Sweden", VatExemption.EU_EXEMPT},
				{756l, "CH", "CHE", "Switzerland", VatExemption.EXEMPT},
				{760l, "SY", "SYR", "Syrian Arab Republic", VatExemption.EXEMPT},
				{158l, "TW", "TWN", "Taiwan, Province of China", VatExemption.EXEMPT},
				{762l, "TJ", "TJK", "Tajikistan", VatExemption.EXEMPT},
				{834l, "TZ", "TZA", "Tanzania, United Republic of", VatExemption.EXEMPT},
				{764l, "TH", "THA", "Thailand", VatExemption.EXEMPT},
				{626l, "TL", "TLS", "Timor-Leste", VatExemption.EXEMPT},
				{768l, "TG", "TGO", "Togo", VatExemption.EXEMPT},
				{772l, "TK", "TKL", "Tokelau", VatExemption.EXEMPT},
				{776l, "TO", "TON", "Tonga", VatExemption.EXEMPT},
				{780l, "TT", "TTO", "Trinidad and Tobago", VatExemption.EXEMPT},
				{788l, "TN", "TUN", "Tunisia", VatExemption.EXEMPT},
				{792l, "TR", "TUR", "Turkey", VatExemption.EXEMPT},
				{795l, "TM", "TKM", "Turkmenistan", VatExemption.EXEMPT},
				{796l, "TC", "TCA", "Turks and Caicos Islands", VatExemption.EXEMPT},
				{798l, "TV", "TUV", "Tuvalu", VatExemption.EXEMPT},
				{800l, "UG", "UGA", "Uganda", VatExemption.EXEMPT},
				{804l, "UA", "UKR", "Ukraine", VatExemption.EXEMPT},
				{784l, "AE", "ARE", "United Arab Emirates", VatExemption.EXEMPT},
				{826l, "GB", "GBR", "United Kingdom", VatExemption.EXEMPT},
				{840l, "US", "USA", "United States", VatExemption.EXEMPT},
				{581l, "UM", "UMI", "United States Minor Outlying Islands", VatExemption.EXEMPT},
				{858l, "UY", "URY", "Uruguay", VatExemption.EXEMPT},
				{860l, "UZ", "UZB", "Uzbekistan", VatExemption.EXEMPT},
				{548l, "VU", "VUT", "Vanuatu", VatExemption.EXEMPT},
				{862l, "VE", "VEN", "Venezuela", VatExemption.EXEMPT},
				{704l, "VN", "VNM", "Vietnam", VatExemption.EXEMPT},
				{92l, "VG", "VGB", "Virgin Islands, British", VatExemption.EXEMPT},
				{850l, "VI", "VIR", "Virgin Islands, U.S.", VatExemption.EXEMPT},
				{876l, "WF", "WLF", "Wallis and Futuna", VatExemption.EXEMPT},
				{732l, "EH", "ESH", "Western Sahara", VatExemption.EXEMPT},
				{887l, "YE", "YEM", "Yemen", VatExemption.EXEMPT},
				{894l, "ZM", "ZMB", "Zambia", VatExemption.EXEMPT},
				{716l, "ZW", "ZWE", "Zimbabwe", VatExemption.EXEMPT},

				/*
				 * This is just a temporary measure before we allow the countries to
				 * have parents.  This is required as the delivery companies work of
				 * countries that aren't actually ISO recognised countries.
				 */
				{10001l, "GB", "GBR", "Northern Ireland", VatExemption.NOT_EXEMPT},
				{10002l, "GB", "GBR", "England", VatExemption.NOT_EXEMPT},
				{10003l, "ES", "ESP", "Balearic Islands", VatExemption.EU_EXEMPT},
				{10004l, "ES", "ESP", "Canary Islands", VatExemption.EXEMPT},
				{10005l, "PT", "PRT", "Madeira Islands", VatExemption.EU_EXEMPT},
				{10006l, "GB", "GBR", "Scotland", VatExemption.NOT_EXEMPT},
				{10007l, "GB", "GBR", "Wales", VatExemption.NOT_EXEMPT},
				{10008l, "ES", "ESP", "Tenerife", VatExemption.EXEMPT}
		};

		//  This is a crazy amount of code for doing something rather simple
		//  The id's can't be set using hibernate, but hibernate has to be used
		//  in order for the tables inheritance to work correctly.  So we have to
		//  update the id using sql afterwards but this needs to be done in order
		//  so not to create unique constraint failures.

		Country tempCountry;
		Map<String,Long> countryMap = new HashMap<String,Long>();
		List<Country> tempCountryList = new ArrayList<Country>();
		for (int i=0; i < countries.length; i++) {
			tempCountry = new Country();
			tempCountry.setId((Long)countries[i][0]);
			tempCountry.setIso2((String)countries[i][1]);
			tempCountry.setIso3((String)countries[i][2]);
			tempCountry.setName((String)countries[i][3]);
			tempCountry.setActive(true);
			countryMap.put( tempCountry.getName(), (Long)countries[i][0] );
			tempCountryList.add( tempCountry );
		}

		Collections.sort( tempCountryList, new Comparator<Country>() {
			@Override
			public int compare(Country o1, Country o2) {
				if( o1.getId() > o2.getId() ) {
					return 1;
				} else {
					return -1;
				}
			}

		});

		for( int i = 0, n = tempCountryList.size(); i < n; i++ ) {
			tempCountryList.get( i ).setId( null );
			tempCountryList.get( i ).saveDetails();
		}

		for( int i = tempCountryList.size() - 1; i >= 0; i-- ) {
			tempCountry = tempCountryList.get( i );
			ApplicationUtil.executeSql( "UPDATE Country SET id = " + countryMap.get( tempCountry.getName() ) + " WHERE id = " + tempCountry.getId() );
		}




		/*
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (4, 'AF', 'AFG', 'Afghanistan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (248, 'AX', 'ALA', 'Aland Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (8, 'AL', 'ALB', 'Albania', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (12, 'DZ', 'DZA', 'Algeria', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (16, 'AS', 'ASM', 'American Samoa', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (20, 'AD', 'AND', 'Andorra', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (24, 'AO', 'AGO', 'Angola', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (660, 'AI', 'AIA', 'Anguilla', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (10, 'AQ', 'ATA', 'Antarctica', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (28, 'AG', 'ATG', 'Antigua and Barbuda', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (32, 'AR', 'ARG', 'Argentina', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (51, 'AM', 'ARM', 'Armenia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (533, 'AW', 'ABW', 'Aruba', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (36, 'AU', 'AUS', 'Australia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (40, 'AT', 'AUT', 'Austria', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (31, 'AZ', 'AZE', 'Azerbaijan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (44, 'BS', 'BHS', 'Bahamas', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (48, 'BH', 'BHR', 'Bahrain', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (50, 'BD', 'BGD', 'Bangladesh', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (52, 'BB', 'BRB', 'Barbados', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (112, 'BY', 'BLR', 'Belarus', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (56, 'BE', 'BEL', 'Belgium', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (84, 'BZ', 'BLZ', 'Belize', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (204, 'BJ', 'BEN', 'Benin', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (60, 'BM', 'BMU', 'Bermuda', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (64, 'BT', 'BTN', 'Bhutan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (68, 'BO', 'BOL', 'Bolivia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (70, 'BA', 'BIH', 'Bosnia and Herzegovina', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (72, 'BW', 'BWA', 'Botswana', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (74, 'BV', 'BVT', 'Bouvet Island', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (76, 'BR', 'BRA', 'Brazil', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (86, 'IO', 'IOT', 'British Indian Ocean Territory', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (96, 'BN', 'BRN', 'Brunei Darussalam', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (100, 'BG', 'BGR', 'Bulgaria', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (854, 'BF', 'BFA', 'Burkina Faso', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (108, 'BI', 'BDI', 'Burundi', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (116, 'KH', 'KHM', 'Cambodia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (120, 'CM', 'CMR', 'Cameroon', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (124, 'CA', 'CAN', 'Canada', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (132, 'CV', 'CPV', 'Cape Verde', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (136, 'KY', 'CYM', 'Cayman Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (140, 'CF', 'CAF', 'Central African Republic', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (148, 'TD', 'TCD', 'Chad', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (152, 'CL', 'CHL', 'Chile', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (156, 'CN', 'CHN', 'China', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (162, 'CX', 'CXR', 'Christmas Island', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (166, 'CC', 'CCK', 'Cocos (Keeling) Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (170, 'CO', 'COL', 'Colombia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (174, 'KM', 'COM', 'Comoros', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (178, 'CG', 'COG', 'Congo', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (180, 'CD', 'COD', 'Congo, Democratic Republic of the', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (184, 'CK', 'COK', 'Cook Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (188, 'CR', 'CRI', 'Costa Rica', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (384, 'CI', 'CIV', 'Cote d''Ivoire', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (191, 'HR', 'HRV', 'Croatia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (192, 'CU', 'CUB', 'Cuba', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (196, 'CY', 'CYP', 'Cyprus', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (203, 'CZ', 'CZE', 'Czech Republic', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (208, 'DK', 'DNK', 'Denmark', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (262, 'DJ', 'DJI', 'Djibouti', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (212, 'DM', 'DMA', 'Dominica', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (214, 'DO', 'DOM', 'Dominican Republic', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (218, 'EC', 'ECU', 'Ecuador', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (818, 'EG', 'EGY', 'Egypt', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (222, 'SV', 'SLV', 'El Salvador', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (226, 'GQ', 'GNQ', 'Equatorial Guinea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (232, 'ER', 'ERI', 'Eritrea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (233, 'EE', 'EST', 'Estonia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (231, 'ET', 'ETH', 'Ethiopia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (238, 'FK', 'FLK', 'Falkland Islands (Malvinas)', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (234, 'FO', 'FRO', 'Faroe Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (242, 'FJ', 'FJI', 'Fiji', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (246, 'FI', 'FIN', 'Finland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (250, 'FR', 'FRA', 'France', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (254, 'GF', 'GUF', 'French Guiana', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (258, 'PF', 'PYF', 'French Polynesia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (260, 'TF', 'ATF', 'French Southern Territories', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (266, 'GA', 'GAB', 'Gabon', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (270, 'GM', 'GMB', 'Gambia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (268, 'GE', 'GEO', 'Georgia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (276, 'DE', 'DEU', 'Germany', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (288, 'GH', 'GHA', 'Ghana', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (292, 'GI', 'GIB', 'Gibraltar', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (300, 'GR', 'GRC', 'Greece', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (304, 'GL', 'GRL', 'Greenland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (308, 'GD', 'GRD', 'Grenada', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (312, 'GP', 'GLP', 'Guadeloupe', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (316, 'GU', 'GUM', 'Guam', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (320, 'GT', 'GTM', 'Guatemala', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (831, 'GG', 'GGY', 'Guernsey', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (324, 'GN', 'GIN', 'Guinea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (624, 'GW', 'GNB', 'Guinea-Bissau', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (328, 'GY', 'GUY', 'Guyana', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (332, 'HT', 'HTI', 'Haiti', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (334, 'HM', 'HMD', 'Heard Island and McDonald Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (336, 'VA', 'VAT', 'Holy See (Vatican City State)', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (340, 'HN', 'HND', 'Honduras', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (344, 'HK', 'HKG', 'Hong Kong', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (348, 'HU', 'HUN', 'Hungary', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (352, 'IS', 'ISL', 'Iceland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (356, 'IN', 'IND', 'India', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (360, 'ID', 'IDN', 'Indonesia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (364, 'IR', 'IRN', 'Iran, Islamic Republic of', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (368, 'IQ', 'IRQ', 'Iraq', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (372, 'IE', 'IRL', 'Ireland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (833, 'IM', 'IMN', 'Isle of Man', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (376, 'IL', 'ISR', 'Israel', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (380, 'IT', 'ITA', 'Italy', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (388, 'JM', 'JAM', 'Jamaica', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (392, 'JP', 'JPN', 'Japan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (832, 'JE', 'JEY', 'Jersey', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (400, 'JO', 'JOR', 'Jordan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (398, 'KZ', 'KAZ', 'Kazakhstan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (404, 'KE', 'KEN', 'Kenya', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (296, 'KI', 'KIR', 'Kiribati', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (408, 'KP', 'PRK', 'North Korea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (410, 'KR', 'KOR', 'South Korea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (414, 'KW', 'KWT', 'Kuwait', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (417, 'KG', 'KGZ', 'Kyrgyzstan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (418, 'LA', 'LAO', 'Lao People''s Democratic Republic', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (428, 'LV', 'LVA', 'Latvia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (422, 'LB', 'LBN', 'Lebanon', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (426, 'LS', 'LSO', 'Lesotho', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (430, 'LR', 'LBR', 'Liberia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (434, 'LY', 'LBY', 'Libyan Arab Jamahiriya', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (438, 'LI', 'LIE', 'Liechtenstein', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (440, 'LT', 'LTU', 'Lithuania', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (442, 'LU', 'LUX', 'Luxembourg', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (446, 'MO', 'MAC', 'Macao', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (807, 'MK', 'MKD', 'Macedonia, the former Yugoslav Republic of', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (450, 'MG', 'MDG', 'Madagascar', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (454, 'MW', 'MWI', 'Malawi', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (458, 'MY', 'MYS', 'Malaysia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (462, 'MV', 'MDV', 'Maldives', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (466, 'ML', 'MLI', 'Mali', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (470, 'MT', 'MLT', 'Malta', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (584, 'MH', 'MHL', 'Marshall Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (474, 'MQ', 'MTQ', 'Martinique', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (478, 'MR', 'MRT', 'Mauritania', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (480, 'MU', 'MUS', 'Mauritius', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (175, 'YT', 'MYT', 'Mayotte', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (484, 'MX', 'MEX', 'Mexico', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (583, 'FM', 'FSM', 'Micronesia, Federated States of', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (498, 'MD', 'MDA', 'Moldova', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (492, 'MC', 'MCO', 'Monaco', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (496, 'MN', 'MNG', 'Mongolia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (499, 'ME', 'MNE', 'Montenegro', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (500, 'MS', 'MSR', 'Montserrat', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (504, 'MA', 'MAR', 'Morocco', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (508, 'MZ', 'MOZ', 'Mozambique', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (104, 'MM', 'MMR', 'Myanmar', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (516, 'NA', 'NAM', 'Namibia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (520, 'NR', 'NRU', 'Nauru', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (524, 'NP', 'NPL', 'Nepal', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (528, 'NL', 'NLD', 'Netherlands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (530, 'AN', 'ANT', 'Netherlands Antilles', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (540, 'NC', 'NCL', 'New Caledonia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (554, 'NZ', 'NZL', 'New Zealand', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (558, 'NI', 'NIC', 'Nicaragua', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (562, 'NE', 'NER', 'Niger', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (566, 'NG', 'NGA', 'Nigeria', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (570, 'NU', 'NIU', 'Niue', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (574, 'NF', 'NFK', 'Norfolk Island', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (580, 'MP', 'MNP', 'Northern Mariana Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (578, 'NO', 'NOR', 'Norway', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (512, 'OM', 'OMN', 'Oman', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (586, 'PK', 'PAK', 'Pakistan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (585, 'PW', 'PLW', 'Palau', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (275, 'PS', 'PSE', 'Palestinian Territory, Occupied', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (591, 'PA', 'PAN', 'Panama', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (598, 'PG', 'PNG', 'Papua New Guinea', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (600, 'PY', 'PRY', 'Paraguay', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (604, 'PE', 'PER', 'Peru', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (608, 'PH', 'PHL', 'Philippines', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (612, 'PN', 'PCN', 'Pitcairn', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (616, 'PL', 'POL', 'Poland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (620, 'PT', 'PRT', 'Portugal', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (630, 'PR', 'PRI', 'Puerto Rico', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (634, 'QA', 'QAT', 'Qatar', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (638, 'RE', 'REU', 'Reunion', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (642, 'RO', 'ROU', 'Romania', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (643, 'RU', 'RUS', 'Russian Federation', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (646, 'RW', 'RWA', 'Rwanda', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (652, 'BL', 'BLM', 'Saint Bartholemy', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (654, 'SH', 'SHN', 'Saint Helena', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (659, 'KN', 'KNA', 'Saint Kitts and Nevis', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (662, 'LC', 'LCA', 'Saint Lucia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (663, 'MF', 'MAF', 'Saint Martin (French part)', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (666, 'PM', 'SPM', 'Saint Pierre and Miquelon', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (670, 'VC', 'VCT', 'Saint Vincent and the Grenadines', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (882, 'WS', 'WSM', 'Samoa', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (674, 'SM', 'SMR', 'San Marino', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (678, 'ST', 'STP', 'Sao Tome and Principe', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (682, 'SA', 'SAU', 'Saudi Arabia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (686, 'SN', 'SEN', 'Senegal', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (688, 'RS', 'SRB', 'Serbia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (690, 'SC', 'SYC', 'Seychelles', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (694, 'SL', 'SLE', 'Sierra Leone', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (702, 'SG', 'SGP', 'Singapore', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (703, 'SK', 'SVK', 'Slovakia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (705, 'SI', 'SVN', 'Slovenia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (90, 'SB', 'SLB', 'Solomon Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (706, 'SO', 'SOM', 'Somalia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (710, 'ZA', 'ZAF', 'South Africa', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (239, 'GS', 'SGS', 'South Georgia and the South Sandwich Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (724, 'ES', 'ESP', 'Spain', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (144, 'LK', 'LKA', 'Sri Lanka', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (736, 'SD', 'SDN', 'Sudan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (740, 'SR', 'SUR', 'SuriName, active', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (744, 'SJ', 'SJM', 'Svalbard and Jan Mayen', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (748, 'SZ', 'SWZ', 'Swaziland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (752, 'SE', 'SWE', 'Sweden', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (756, 'CH', 'CHE', 'Switzerland', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (760, 'SY', 'SYR', 'Syrian Arab Republic', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (158, 'TW', 'TWN', 'Taiwan, Province of China', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (762, 'TJ', 'TJK', 'Tajikistan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (834, 'TZ', 'TZA', 'Tanzania, United Republic of', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (764, 'TH', 'THA', 'Thailand', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (626, 'TL', 'TLS', 'Timor-Leste', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (768, 'TG', 'TGO', 'Togo', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (772, 'TK', 'TKL', 'Tokelau', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (776, 'TO', 'TON', 'Tonga', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (780, 'TT', 'TTO', 'Trinidad and Tobago', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (788, 'TN', 'TUN', 'Tunisia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (792, 'TR', 'TUR', 'Turkey', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (795, 'TM', 'TKM', 'Turkmenistan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (796, 'TC', 'TCA', 'Turks and Caicos Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (798, 'TV', 'TUV', 'Tuvalu', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (800, 'UG', 'UGA', 'Uganda', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (804, 'UA', 'UKR', 'Ukraine', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (784, 'AE', 'ARE', 'United Arab Emirates', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (826, 'GB', 'GBR', 'United Kingdom', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (840, 'US', 'USA', 'United States', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (581, 'UM', 'UMI', 'United States Minor Outlying Islands', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (858, 'UY', 'URY', 'Uruguay', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (860, 'UZ', 'UZB', 'Uzbekistan', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (548, 'VU', 'VUT', 'Vanuatu', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (862, 'VE', 'VEN', 'Venezuela', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (704, 'VN', 'VNM', 'Vietnam', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (92, 'VG', 'VGB', 'Virgin Islands, British', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (850, 'VI', 'VIR', 'Virgin Islands, U.S.', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (876, 'WF', 'WLF', 'Wallis and Futuna', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (732, 'EH', 'ESH', 'Western Sahara', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (887, 'YE', 'YEM', 'Yemen', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (894, 'ZM', 'ZMB', 'Zambia', true, true);
		INSERT INTO Country (id, ISO2, ISO3, Name, active, deletable) VALUES (716, 'ZW', 'ZWE', 'Zimbabwe', true, true);
		*/
	}

	public void createCountryAreas() {
		Country UK = (Country) new BeanDao( Country.class ).get( 826 );
		String[] areas = {  "Avon", "Bedfordshire", "Berkshire", "Borders", "Buckinghamshire",
							"Cambridgeshire", "Central", "Cheshire", "Cleveland", "Clwyd", "Cornwall",
							"County Antrim", "County Armagh","County Down",	"County Fermanagh",
							"County Londonderry","County Tyrone","Cumbria",	"Derbyshire","Devon",
							"Dorset","Dumfries and Galloway","Durham","Dyfed","East Sussex","Essex",
							"Fife","Gloucestershire","Grampian","Greater Manchester","Gwent","Gwynedd County",
							"Hampshire","Herefordshire","Hertfordshire","Highlands and Islands",
							"Humberside","Isle of Wight","Kent","Lancashire","Leicestershire","Lincolnshire",
							"Lothian","Merseyside","Mid Glamorgan","Norfolk","North Yorkshire",
							"Northamptonshire",	"Northumberland","Nottinghamshire",	"Oxfordshire","Powys",
							"Rutland","Shropshire","Somerset","South Glamorgan","South Yorkshire",
							"Staffordshire","Strathclyde","Suffolk","Surrey","Tayside","Tyne and Wear",
							"Warwickshire","West Glamorgan","West Midlands","West Sussex","West Yorkshire",
							"Wiltshire","Worcestershire" };

		for (int i=0; i < areas.length; i++) {
			CountryArea countryArea = new CountryArea();
			countryArea.setActive(true);
			countryArea.setCountry(UK);
			countryArea.setName(areas[i]);
			countryArea.saveDetails();
		}

		Country NorthernIreland = (Country) new BeanDao( Country.class ).get( 372 );
		String[] northernIrelandAreas = { "County Carlow", "County Cavan",
			"County Clare", "County Cork", "County Donegal", "County Dublin", "County Galway",
			"County Kerry", "County Kildare", "County Kilkenny", "County Laois", "County Leitrim",
			"County Limerick", "County Longford", "County Louth", "County Mayo", "County Meath",
			"County Monaghan", "County Offaly", "County Roscommon", "County Sligo",
			"County Tipperary", "County Waterford", "County Westmeath", "County Wexford",
			"County Wicklow"
				};

		for (int i=0; i < northernIrelandAreas.length; i++) {
			CountryArea countryArea = new CountryArea();
			countryArea.setActive(true);
			countryArea.setCountry(NorthernIreland);
			countryArea.setName(northernIrelandAreas[i]);
			countryArea.saveDetails();
		}

		//  Northern Island
//
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Carlow');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Cavan');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Clare');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Cork');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Donegal');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Dublin');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Galway');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Kerry');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Kildare');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Kilkenny');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Laois');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Leitrim');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Limerick');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Longford');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Louth');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Mayo');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Meath');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Monaghan');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Offaly');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Roscommon');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Sligo');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Tipperary');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Waterford');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Westmeath');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Wexford');
//		INSERT INTO CountryArea (country_id, active, name) VALUES(372, true, 'County Wicklow');

	    //  UK Areas
	    /*

	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Avon', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Bedfordshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Berkshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Borders', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Buckinghamshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Cambridgeshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Central', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Cheshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Cleveland', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Clwyd', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Cornwall', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Antrim', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Armagh', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Down', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Fermanagh', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Londonderry', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('County Tyrone', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Cumbria', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Derbyshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Devon', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Dorset', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Dumfries and Galloway', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Durham', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Dyfed', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('East Sussex', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Essex', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Fife', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Gloucestershire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Grampian', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Greater Manchester', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Gwent', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Gwynedd County', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Hampshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Herefordshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Hertfordshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Highlands and Islands', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Humberside', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Isle of Wight', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Kent', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Lancashire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Leicestershire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Lincolnshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Lothian', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Merseyside', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Mid Glamorgan', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Norfolk', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('North Yorkshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Northamptonshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Northumberland', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Nottinghamshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Oxfordshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Powys', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Rutland', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Shropshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Somerset', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('South Glamorgan', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('South Yorkshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Staffordshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Strathclyde', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Suffolk', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Surrey', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Tayside', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Tyne and Wear', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Warwickshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('West Glamorgan', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('West Midlands', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('West Sussex', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('West Yorkshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Wiltshire', true, 826);
	    INSERT INTO CountryArea (name, active, country_id) VALUES ('Worcestershire', true, 826);
*/
	    //  US States

	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Alaska', 'AK',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Alabama', 'AL',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('American Samoa', 'AS',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Arizona', 'AZ',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Arkansas', 'AR',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('California', 'CA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Colorado', 'CO',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Connecticut', 'CT',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Delaware', 'DE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('District of Columbia', 'DC',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Federated States of Micronesia', 'FM',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Florida', 'FL',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Georgia', 'GA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Guam', 'GU',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Hawaii', 'HI',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Idaho', 'ID',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Illinois', 'IL',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Indiana', 'IN',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Iowa', 'IA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Kansas', 'KS',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Kentucky', 'KY',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Louisiana', 'LA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Maine', 'ME',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Marshall Islands', 'MH',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Maryland', 'MD',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Massachusetts', 'MA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Michigan', 'MI',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Minnesota', 'MN',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Mississippi', 'MS',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Missouri', 'MO',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Montana', 'MT',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Nebraska', 'NE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Nevada', 'NV',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Hampshire', 'NH',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Jersey', 'NJ',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Mexico', 'NM',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New York', 'NY',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('North Carolina', 'NC',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('North Dakota', 'ND',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Northern Mariana Islands', 'MP',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Ohio', 'OH',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Oklahoma', 'OK',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Oregon', 'OR',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Palau', 'PW',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Pennsylvania', 'PA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Puerto Rico', 'PR',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Rhode Island', 'RI',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('South Carolina', 'SC',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('South Dakota', 'SD',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Tennessee', 'TN',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Texas', 'TX',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Utah', 'UT',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Vermont', 'VT',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Virgin Islands', 'VI',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Virginia', 'VA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Washington', 'WA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('West Virginia', 'WV',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Wisconsin', 'WI',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Wyoming', 'WY',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Africa', 'AE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Americas (except Canada)', 'AA',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Canada', 'AE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Europe', 'AE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Middle East', 'AE',840, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Pacific', 'AP',840, true)" );

	    /** Canadian States **/
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Ontario', 'ON',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Quebec', 'QC',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Nova Scotia', 'NS',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Brunswick', 'NB',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Manitoba', 'MB',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('British Columbia', 'BC',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Prince Edward Island', 'PE',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Saskatchewan', 'SK',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Alberta', 'AB',124, true)" );
	    ApplicationUtil.executeSql( "INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Newfoundland and Labrador', 'NL',124, true)" );

//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Alaska', 'AK',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Alabama', 'AL',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('American Samoa', 'AS',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Arizona', 'AZ',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Arkansas', 'AR',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('California', 'CA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Colorado', 'CO',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Connecticut', 'CT',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Delaware', 'DE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('District of Columbia', 'DC',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Federated States of Micronesia', 'FM',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Florida', 'FL',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Georgia', 'GA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Guam', 'GU',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Hawaii', 'HI',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Idaho', 'ID',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Illinois', 'IL',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Indiana', 'IN',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Iowa', 'IA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Kansas', 'KS',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Kentucky', 'KY',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Louisiana', 'LA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Maine', 'ME',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Marshall Islands', 'MH',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Maryland', 'MD',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Massachusetts', 'MA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Michigan', 'MI',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Minnesota', 'MN',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Mississippi', 'MS',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Missouri', 'MO',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Montana', 'MT',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Nebraska', 'NE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Nevada', 'NV',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Hampshire', 'NH',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Jersey', 'NJ',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New Mexico', 'NM',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('New York', 'NY',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('North Carolina', 'NC',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('North Dakota', 'ND',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Northern Mariana Islands', 'MP',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Ohio', 'OH',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Oklahoma', 'OK',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Oregon', 'OR',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Palau', 'PW',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Pennsylvania', 'PA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Puerto Rico', 'PR',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Rhode Island', 'RI',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('South Carolina', 'SC',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('South Dakota', 'SD',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Tennessee', 'TN',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Texas', 'TX',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Utah', 'UT',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Vermont', 'VT',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Virgin Islands', 'VI',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Virginia', 'VA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Washington', 'WA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('West Virginia', 'WV',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Wisconsin', 'WI',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Wyoming', 'WY',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Africa', 'AE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Americas (except Canada)', 'AA',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Canada', 'AE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Europe', 'AE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Middle East', 'AE',840, true);
//	    INSERT INTO CountryArea (name, areaCode, country_id, active) VALUES ('Armed Forces Pacific', 'AP',840, true);
	}
}
