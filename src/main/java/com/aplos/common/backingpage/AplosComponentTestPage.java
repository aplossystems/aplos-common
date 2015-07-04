package com.aplos.common.backingpage;

import java.util.Calendar;
import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean
@ViewScoped
public class AplosComponentTestPage extends EditPage {

	private static final long serialVersionUID = 5102773599971251770L;

	private Date testDate;
	
	public AplosComponentTestPage() {
		
		testDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(testDate);
		cal.add(Calendar.YEAR, -10);
		testDate = cal.getTime();
		
	}

	public Date getTestDate() {
		return testDate;
	}

	public void setTestDate(Date testDate) {
		this.testDate = testDate;
	}
}
