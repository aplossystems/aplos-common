package com.aplos.common.beans;

public class ScannerHook {
	private String prefix;
	private String suffix;
	private boolean isScannerPrefixUsingCtrl;
	
	public ScannerHook( String prefix, String suffix, boolean isScannerPrefixUsingCtrl ) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.setScannerPrefixUsingCtrl(isScannerPrefixUsingCtrl);
	}
	
	public String getJavascriptString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( "'" ).append( getPrefix() );
		strBuf.append( "','" ).append( getSuffix() );
		strBuf.append( "'," ).append( isScannerPrefixUsingCtrl() );
		return strBuf.toString();
	}
	
	public String toString() {
		return getJavascriptString();
	};
	
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public boolean isScannerPrefixUsingCtrl() {
		return isScannerPrefixUsingCtrl;
	}

	public void setScannerPrefixUsingCtrl(boolean isScannerPrefixUsingCtrl) {
		this.isScannerPrefixUsingCtrl = isScannerPrefixUsingCtrl;
	}
}
