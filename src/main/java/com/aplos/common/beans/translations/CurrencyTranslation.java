package com.aplos.common.beans.translations;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.AplosTranslation;

@Entity
public class CurrencyTranslation extends AplosTranslation {

	private static final long serialVersionUID = 5748738041032628866L;
	private String symbol="";

	public CurrencyTranslation() {}

	public CurrencyTranslation(AplosBean untranslatedBean) {
		super(untranslatedBean);
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

}
