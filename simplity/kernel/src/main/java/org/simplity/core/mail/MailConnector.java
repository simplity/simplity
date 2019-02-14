/*
 * Copyright (c) 2017 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.mail;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crude, initial design. Needs to be improved based on actual app requirements
 *
 * @author simplity.org
 */
public class MailConnector {
	private static final Logger logger = LoggerFactory.getLogger(MailSetup.class);

	static Properties mailProps;

	/**
	 * initial set-up.
	 * 
	 * @param mailProperties
	 */
	public static void initialize(MailSetup mailProperties) {
		logger.info("Setting up the Mail Agent");

		mailProps = new Properties();
		mailProps.setProperty("mail.smtp.host", mailProperties.host);
		mailProps.setProperty("mail.smtp.port", mailProperties.port);
	}

	/**
	 * create MimeMessage and values (fromId, toIds, ccIds, bccIds, subject,
	 * content, and attachment) and send the object to MailConnector
	 *
	 * @param mail
	 */
	public void sendEmail(Mail mail) {
		if (mailProps == null) {
			throw new ApplicationError("Mail server not set up for this applicaiton. MailAction can not be executed.");
		}
		try {
			Session session = Session.getInstance(mailProps, null);
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/html; charset=UTF-8");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress(mail.fromId, "NoReply-JD"));
			msg.setReplyTo(InternetAddress.parse(mail.fromId, false));
			msg.setSubject(mail.subject, AppConventions.CHAR_ENCODING);
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.toIds, false));
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mail.ccIds, false));
			msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(mail.bccIds, false));

			Multipart multipart = new MimeMultipart();

			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setText(mail.content, "US-ASCII", "html");
			multipart.addBodyPart(bodyPart);

			if (mail.inlineAttachment != null) {
				MailAttachment[] inlineMailAttachment = mail.inlineAttachment;
				for (int i = 0; i < inlineMailAttachment.length; i++) {
					bodyPart = new MimeBodyPart();
					bodyPart.setDisposition(Part.INLINE);
					bodyPart.attachFile(inlineMailAttachment[i].filepath); // attach
																			// inline
																			// image
																			// file
					bodyPart.setHeader("Content-ID", inlineMailAttachment[i].name);
					multipart.addBodyPart(bodyPart);
				}
			}

			if (mail.attachment != null) {
				DataSource dataSource = null;
				MailAttachment[] mailAttachment = mail.attachment;
				for (int i = 0; i < mailAttachment.length; i++) {
					bodyPart = new MimeBodyPart();
					dataSource = new FileDataSource(mailAttachment[i].filepath);
					bodyPart.setDataHandler(new DataHandler(dataSource));
					bodyPart.setFileName(mailAttachment[i].name);
					multipart.addBodyPart(bodyPart);
				}
			}

			msg.setContent(multipart);
			Transport.send(msg);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}
