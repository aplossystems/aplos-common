package com.aplos.common;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

import com.aplos.common.enums.PotentialCompanyStatus;

@FacesConverter(value="potentialCompanyStatusConverter")
public class PotentialCompanyStatusConverter extends EnumConverter {

	public PotentialCompanyStatusConverter() {
		super(PotentialCompanyStatus.class);
	}
}
