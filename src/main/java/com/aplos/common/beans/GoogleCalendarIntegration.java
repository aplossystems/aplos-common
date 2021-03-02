package com.aplos.common.beans;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class GoogleCalendarIntegration extends AplosBean {

    private String calendarName;
    private String nextSynToken;

    public GoogleCalendarIntegration() {}

    public GoogleCalendarIntegration(String calendarName) {
        this.calendarName = calendarName;
    }

    public String getNextSynToken() {
        return nextSynToken;
    }

    public void setNextSynToken(String nextSynToken) {
        this.nextSynToken = nextSynToken;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }
}
