package com.aplos.common;

import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

/**
 * Extend the SMTPAppender by buffering messages until either a certain limit
 * has been reached, or if no more messages are queued, then send whatever is
 * buffered after a certain delay.
 *
 */
public class BufferedSMTPAppender extends SMTPAppender {

	public BufferedSMTPAppender() {
		super();
		setEvaluatorClass( BufferedTrigger.class.getName() );
	}

	public class BufferedTrigger extends TimerTask implements TriggeringEventEvaluator {

		// Configuration options. The limit must be <= the buffer size set in
		// log4j.properties.
		private final long delay = 30000;
		private final long limit = 2;

		private int count = 0;
		private long timestamp = 0;

		{ new java.util.Timer().scheduleAtFixedRate( this, delay, delay ); }

		@Override
		public synchronized boolean isTriggeringEvent( LoggingEvent e )  {
			if (count == limit-1) {
				count = 0;
				return true;
			} else {
				// Set timeout check
				timestamp = new Date().getTime();

				count++;
				return false;
			}
		}

		@Override
		public synchronized void run() {
			// If timestamp is uninitialised, then ignore
			if (timestamp == 0) {
				return;
			}

			// If the buffer is empty, then ignore
			if (count == 0) {
				return;
			}

			// Otherwise reset the count and send an email
			long currentTime = new Date().getTime();
			if (currentTime - timestamp > delay) {
				count = 0;
				sendBuffer();
			}
		}
	}
}
