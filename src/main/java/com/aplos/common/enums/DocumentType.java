package com.aplos.common.enums;
public enum DocumentType {
	COMMA_SEPERATED_VALUES ( ".csv", "text/csv" ),
	MS_WORD_DOCUMENT ( ".doc", "application/vnd.ms-word" ),
	MS_EXCEL_DOCUMENT ( ".xls", "application/vnd.ms-excel" ),
	TEXT_DOCUMENT ( ".txt", "text/plain" );

	private String documentExtension;
	private String documentContentType;

	private DocumentType( String documentExtension, String documentContentType ) {
		this.documentExtension = documentExtension;
		this.documentContentType = documentContentType;
	}

	public void setDocumentContentType(String documentContentType) {
		this.documentContentType = documentContentType;
	}

	public String getDocumentContentType() {
		return documentContentType;
	}

	public void setDocumentExtension(String documentExtension) {
		this.documentExtension = documentExtension;
	}

	public String getDocumentExtension() {
		return documentExtension;
	}
}
