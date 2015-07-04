package com.aplos.common.beans.communication;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.utils.CommonUtil;

@Entity
public class MailServerSettings extends AplosBean {
	private static final long serialVersionUID = 8219290765285828995L;
	private String emailAddress; 
	private String outgoingHost; 
	private String incomingHost; 
	private String incomingUsername; 
	private String incomingPassword; 
	private String outgoingUsername; 
	private String outgoingPassword; 
	private boolean isUsingIncomingEmail = false;
	private boolean isDeletingEmailsFromServer = false;

	public MailServerSettings() {
		
	}

	public Session getMailSession() {
		return MailServerSettings.getMailSession( getOutgoingUsername(), getOutgoingPassword(), getOutgoingHost() );
	}
	
	@Override
	public String getDisplayName() {
		return getEmailAddress();
	}

	public static Session getMailSession( final String username, final String password, String outgoingHost ) {
		javax.mail.Session mailSession;
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", outgoingHost );
		
		if( !CommonUtil.isNullOrEmpty( username ) ) {
			props.setProperty("mail.smtp.auth", "true");
			Authenticator mailAuth = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
	
					return new PasswordAuthentication(username, password);
				}
			};
			mailSession = javax.mail.Session.getInstance(props, mailAuth);
		} else {
			props.setProperty("mail.smtp.auth", "false");
			mailSession = javax.mail.Session.getInstance(props);
		}

		return mailSession;
	}

	public String getOutgoingHost() {
		return outgoingHost;
	}

	public void setOutgoingHost(String outgoingHost) {
		this.outgoingHost = outgoingHost;
	}

	public String getIncomingHost() {
		return incomingHost;
	}

	public void setIncomingHost(String incomingHost) {
		this.incomingHost = incomingHost;
	}

	public String getIncomingUsername() {
		return incomingUsername;
	}

	public void setIncomingUsername(String incomingUsername) {
		this.incomingUsername = incomingUsername;
	}

	public String getIncomingPassword() {
		return incomingPassword;
	}

	public void setIncomingPassword(String incomingPassword) {
		this.incomingPassword = incomingPassword;
	}

	public String getOutgoingUsername() {
		return outgoingUsername;
	}

	public void setOutgoingUsername(String outgoingUsername) {
		this.outgoingUsername = outgoingUsername;
	}

	public String getOutgoingPassword() {
		return outgoingPassword;
	}

	public void setOutgoingPassword(String outgoingPassword) {
		this.outgoingPassword = outgoingPassword;
	}

	public boolean isUsingIncomingEmail() {
		return isUsingIncomingEmail;
	}

	public void setUsingIncomingEmail(boolean isUsingIncomingEmail) {
		this.isUsingIncomingEmail = isUsingIncomingEmail;
	}

	public boolean isDeletingEmailsFromServer() {
		return isDeletingEmailsFromServer;
	}

	public void setDeletingEmailsFromServer(boolean isDeletingEmailsFromServer) {
		this.isDeletingEmailsFromServer = isDeletingEmailsFromServer;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
}
