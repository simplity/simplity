package com.infosys.qreuse.smtpservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class Mailer implements LogicInterface {

	private Properties loadProperties() throws IOException {
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("config.properties"));
		return props;
	}

	private void sendEmail(Session session, String toIds, String ccIds, String bccIds, String subject, String content,
			String attachment) throws MessagingException, UnsupportedEncodingException {
		MimeMessage msg = new MimeMessage(session);
		msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
		msg.addHeader("format", "flowed");
		msg.addHeader("Content-Transfer-Encoding", "8bit");
		msg.setFrom(new InternetAddress("no_reply@journaldev.com", "NoReply-JD"));
		msg.setReplyTo(InternetAddress.parse("no_reply@journaldev.com", false));
		msg.setSubject(subject, "UTF-8");
		msg.setSentDate(new Date());
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toIds, false));
		setContent(msg, content, attachment);

		Transport.send(msg);
	}

	private void setContent(MimeMessage msg, String content, String attachment) throws MessagingException {
		if (attachment.isEmpty()) {
			msg.setText(content, "UTF-8");
			return;
		}
		
		// Create the message body part
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(content);

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(attachment);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(attachment);
		multipart.addBodyPart(messageBodyPart);
		msg.setContent(multipart);
	}

	@Override
	public Value execute(ServiceContext ctx) {

		Properties props = null;
		try {
			props = loadProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String toIds = ctx.getTextValue("toIds");
		String ccIds = ctx.getTextValue("ccIds");
		String bccIds = ctx.getTextValue("bccIds");
		String subject = ctx.getTextValue("subject");
		String content = ctx.getTextValue("content");
		String attachment = ctx.getTextValue("filekey");
		
		Session session = Session.getInstance(props, null);
		try {
			sendEmail(session, toIds, ccIds, bccIds, subject, content,attachment);
		} catch (UnsupportedEncodingException | MessagingException e) {
			e.printStackTrace();
		}
		return Value.newBooleanValue(true);
	}

}