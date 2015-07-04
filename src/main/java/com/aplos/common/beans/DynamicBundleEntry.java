package com.aplos.common.beans;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.beans.translations.DynamicBundleEntryTranslation;
import com.aplos.common.utils.CommonUtil;

@Entity
@PluralDisplayName(name="translations")
public class DynamicBundleEntry extends AplosSiteBean {
	private static final long serialVersionUID = 6981437510722879833L;

	@Column(columnDefinition="LONGTEXT")
	private String entryKey;
	@Column(columnDefinition="LONGTEXT")
	private String entryValue;
	private boolean redirectKey = false;
	private boolean html = false;
	@ManyToOne
	private DynamicBundleEntry keyToRedirectTo = null;

	@Override
	public String getDisplayName() {
		String ret = "[ No Key";
		if (!isNew()) {
			ret += " " + getId();
		}
		ret += " ]";
		if (entryKey != null && !entryKey.equals("")) {
			ret = entryKey;
		}
		return ret;
	}

	public String getValueChainDisplayName() {
		//this format shows the chain and end (determined) value, its useful when displayed in autocomplete
		String ret = "[ No Key";
		if (!isNew()) {
			//allow us to use this in autocomplete (keep it unique if we dont have values)
			ret += " " + getId();
		}
		ret += " ]";
		if (entryKey != null && !entryKey.equals("")) {
			ret = "[ " + entryKey + " ] -> " + getValueChain();
		}
		return ret;
	}

	/**
	 * Returns the chain followed to find the value returned by {@link this{@link #determineValue()}
	 * @return
	 */
	public String getValueChain() {
		String ret = "[ Blank / No Value ]";
		if (redirectKey && keyToRedirectTo != null) {
			//chain the keys here
			ret = keyToRedirectTo.getValueChainDisplayName();
		} else if (!redirectKey && entryValue != null && !entryValue.equals("")) {
			//end of the chain, use getter - it translates
			ret = getEntryValue();
		}
		return ret;
	}

	public void setEntryKey(String entryKey) {
		this.entryKey = entryKey;
	}

	public String getEntryKey() {
		return entryKey;
	}

	public String determineValue() {
		return determineValue( CommonUtil.getContextLocale().getLanguage() );
	}

	public String determineValue(String language) {
		if (redirectKey && keyToRedirectTo != null) {
			return keyToRedirectTo.determineValue();
		} else if (!redirectKey) {
			return getEntryValue( language );
		} else {
			return "";
		}
	}

	public void setEntryValue(String entryValue) {
		setEntryValue( entryValue, CommonUtil.getContextLocale().getLanguage() );
	}

	public void setEntryValue(String entryValue, String language) {
		DynamicBundleEntryTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			trans.setEntryValue(entryValue);
		} else {
			this.entryValue = entryValue;
		}
	}

	/**
	 * Normal usage should reference {@link this#determineValue(String)}
	 * @return
	 */
	public String getEntryValue( String language ) {
		DynamicBundleEntryTranslation trans = getAplosTranslation(language);
		if (trans != null && trans.getValue() != null) {
			return trans.getValue();
		}
		return entryValue;
	}

	/**
	 * Normal usage should reference {@link this#determineValue()}
	 * @return
	 */
	public String getEntryValue() {
		return getEntryValue( CommonUtil.getContextLocale().getLanguage() );
	}

	public void setRedirectKey(boolean redirectKey) {
		this.redirectKey = redirectKey;
	}

	public boolean isRedirectKey() {
		return redirectKey;
	}

	public void setKeyToRedirectTo(DynamicBundleEntry keyToRedirectTo) {
		this.keyToRedirectTo = keyToRedirectTo;
	}

	public DynamicBundleEntry getKeyToRedirectTo() {
		return keyToRedirectTo;
	}

	public boolean isHtml() {
		return html;
	}

	public void setHtml(boolean html) {
		this.html = html;
	}

}
