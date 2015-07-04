package com.aplos.common.beans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.translations.CurrencyTranslation;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

//@ManagedBean /** removed to allow us to use {@link CommonUtil#getCurrency() } */
//@SessionScoped
@Entity
@Cache
@PluralDisplayName(name="currencies")
public class Currency extends AplosBean {

	private static final long serialVersionUID = 2541385450405945490L;

	private String symbol;
	private BigDecimal baseRate;
	private String prefix="";
	private String suffix="";
	private Integer iso4217;

	public Currency() {}

	public void setSymbol(String symbol) {
		CurrencyTranslation trans = getAplosTranslation();
		if (trans != null) {
			trans.setSymbol(symbol);
		} else {
			this.symbol = symbol;
		}
	}

	public String getSymbol(String language) {
		CurrencyTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			return trans.getSymbol();
		}
		return symbol;
	}

	public String getSymbol() {
		return getSymbol(CommonUtil.getContextLocale().getLanguage());
	}

	@Override
	public String getDisplayName(){
		return getSymbol();
	}

	public BigDecimal getBaseRate() {
		return baseRate;
	}

	public void setBaseRate(BigDecimal newBaseRate) {
		baseRate = newBaseRate;
	}


	public String getPrefixOrSuffix() {
		if (prefix != null && !prefix.equals("")) {
			return prefix;
		}
		return suffix;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String newPrefix) {
		prefix = newPrefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String newSuffix) {
		suffix = newSuffix;
	}

	public List<Currency> getCurrencies() {
		List<Currency> currencies = new BeanDao( Currency.class ).getAll();
		return currencies;
	}

	public String appendSign( String value ) {
		String signedValue = value;
		if( getPrefix() != null ) {
			signedValue = getPrefix() + signedValue;
		}
		if( getSuffix() != null ) {
			signedValue = signedValue + getSuffix();
		}
		return signedValue;
	}

	public BigDecimal getConvertedPrice( BigDecimal baseValue ) {
		BigDecimal baseRateValue = getBaseRate();
		if (baseRateValue == null) {
			baseRateValue = new BigDecimal(1);
		}
		return baseValue.multiply( baseRateValue );
	}

	/**
	 * Im not sure what this method is, how it got here or whether it should be deprecated.
	 * you should probably use convert(double,currency from,currency to)
	 * this method duplicates what convert does but does not error check
	 * @param currency
	 * @param baseValue
	 * @return
	 */
	public static BigDecimal getConvertedPrice( Currency currency, BigDecimal baseValue ) {
		if( currency != null ) {
			return currency.getConvertedPrice( baseValue );
		} else {
			return baseValue;
		}
	}

	public static BigDecimal getConvertedPriceBySession( BigDecimal baseValue ) {
		Currency currency = (Currency) JSFUtil.getBeanFromScope( Currency.class );
		if( currency != null ) {
			return currency.getConvertedPrice( baseValue );
		} else {
			return baseValue;
		}
	}

	public static BigDecimal convert(BigDecimal amount, Currency curr_to) {
		return convert(amount, CommonConfiguration.getCommonConfiguration().getDefaultCurrency(), curr_to);
	}

	public static BigDecimal convert(BigDecimal amount, Currency curr_from, Currency curr_to) {
		if (amount == null) {
			return null;
		}
		if (curr_from == null ||
			curr_to == null ||
			curr_from.equals(curr_to)) {
			return amount;
		} else {
			//convert to GBP (this is base)
			BigDecimal amount_as_gbp = amount;
			if (!curr_from.equals(CommonConfiguration.getCommonConfiguration().getDefaultCurrency())) {
				amount_as_gbp = amount.divide( curr_from.getBaseRate(), RoundingMode.HALF_UP );
			}
			//then convert to the currency we want
			return convert(amount_as_gbp, curr_to.getBaseRate());
		}
	}

	public static BigDecimal convert(BigDecimal amount_in_default_currency, BigDecimal to_base_rate) {
		Currency defaultCurrency = CommonConfiguration.getCommonConfiguration().getDefaultCurrency();
		if (defaultCurrency == null) {
			JSFUtil.addMessageForError("Conversion aborted. CommonConfiguration::DefaultCurrency has not been defined.");
			return amount_in_default_currency;
		}
		if (to_base_rate == null ||
			amount_in_default_currency == null ||
			defaultCurrency.getBaseRate().equals(to_base_rate)) {
			return amount_in_default_currency;
		} else {
			//then convert to the base rate we want
			return amount_in_default_currency.multiply( to_base_rate );
		}
	}

	public Integer getIso4217() {
		return iso4217;
	}

	public void setIso4217(Integer iso4217) {
		this.iso4217 = iso4217;
	}

}





