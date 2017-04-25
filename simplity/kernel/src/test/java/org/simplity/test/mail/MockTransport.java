package org.simplity.test.mail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

// MockTransport.java
public class MockTransport extends Transport {

	public MockTransport(Session session, URLName urlName) {
		super(session, urlName);
	}
    public static void send(Message msg) throws MessagingException {
	msg.saveChanges(); // do this first
	System.out.println("Done deed");
    }
	
	@Override
	public void connect() throws MessagingException {
		System.out.println("Connecting to MockTransport:connect()");
	}

	@Override
	public void connect(String host, int port, String username, String password) throws MessagingException {
		System.out.println("Connecting to MockTransport:connect(String " + host + ", int " + port + ", String "
				+ username + ", String " + password + ")");
	}

	@Override
	public void connect(String host, String username, String password) throws MessagingException {
		System.out.println("Connecting to MockTransport:connect(String " + host + ", String " + username + ", String "
				+ password + ")");
	}


	@Override
	public void close() {
		System.out.println("Closing MockTransport:close()");
	}
	@Override
	public void sendMessage(Message msg, Address[] addresses) throws MessagingException {		
		System.out.println("sendMessage");
	}
}