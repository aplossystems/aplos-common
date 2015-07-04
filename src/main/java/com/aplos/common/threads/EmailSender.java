package com.aplos.common.threads;

import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class EmailSender extends AplosThread {
	private static LinkedBlockingQueue<MimeMessage> messageQueue = new LinkedBlockingQueue<MimeMessage>();

	public static void addEmailToQueue( MimeMessage mimeMessage ) {
		if( mimeMessage != null ) {
			messageQueue.add( mimeMessage );
			if( !ApplicationUtil.getAplosContextListener().getEmailSender().isAlive() ) {
				ApplicationUtil.getAplosContextListener().createAndStartEmailSender();
				ApplicationUtil.handleError(new Exception("Email sender required restarting"));
			}
		}
	}
	
	@Override
	public int getQueueSize() {
		return messageQueue.size();
	}

	public void stopThread() {
		setRunning( false );
	}

	@Override
	public void run() {
		MimeMessage currMessage = null;
		while( isRunning() ) {
			try {
				if( currMessage == null ) {
					currMessage = messageQueue.take();	
				}
				if( !isRunning() ) {
					break;
				}
				try {
					Transport.send(currMessage);
					currMessage = null;
				} catch (NoSuchProviderException nspEx) {
					AplosContextListener.getAplosContextListener().handleError( nspEx );
				} catch (javax.mail.MessagingException mEx) {
					AplosContextListener.getAplosContextListener().handleError( mEx );
					if( mEx.getNextException() instanceof java.net.ConnectException ) {
						sleep( 60 * 1000 );
					} else if( mEx.getNextException() instanceof java.net.UnknownHostException ) {
						sleep( 60 * 1000 );
					} else {
						currMessage = null;
					}
				}
			} catch( InterruptedException iEx ) {
				AplosContextListener.getAplosContextListener().handleError( iEx );
				break;
			}
		}
	}

	
	public static void sendMessage(MimeMessage currMessage) {
		try {
			Transport.send(currMessage);
			currMessage = null;
		} catch (NoSuchProviderException nspEx) {
			AplosContextListener.getAplosContextListener().handleError( nspEx );
		} catch (javax.mail.MessagingException mEx) {
			AplosContextListener.getAplosContextListener().handleError( mEx );
		}
	}
}
