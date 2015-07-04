package com.aplos.common.enums;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

@FacesConverter(value="emailFilterStatusConverter")
public class EmailFilterStatusConverter extends EnumConverter {

	public EmailFilterStatusConverter() {
		super(EmailFilterStatus.class);
	}
}
