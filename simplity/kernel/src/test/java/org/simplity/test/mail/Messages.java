package org.simplity.test.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

public class Messages extends Message{

  public static Map<String,List<MimeMessage>> messages = new HashMap<String,List<MimeMessage>>();

  public static void addMessage(String toEmail, MimeMessage message) {
    List<MimeMessage> messagesForUser = messages.get(toEmail);
    if (messagesForUser == null) {
      messagesForUser = new ArrayList<MimeMessage>();
    }
    messagesForUser.add(message);
    messages.put(toEmail, messagesForUser);
  }

  public static List<MimeMessage> getMessages(String toEmail) {
    List<MimeMessage> messagesForUser = messages.get(toEmail);
    if (messagesForUser == null) {
      return new ArrayList<MimeMessage>();
    } else {
      return messagesForUser;
    }
  }

  public static void reset() throws Exception {
    messages.clear();
  }

  /**
   * Dumps the contents of the Messages data structure for the current run.
   * @return the string representation of the Messages structure.
   * @throws Exception if one is thrown.
   */
  public static String dumpAllMailboxes() throws Exception {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    for (String email : messages.keySet()) {
      builder.append(dumpMailbox(email)).append(",\n");
    }
    builder.append("}\n");
    return builder.toString();
  }

  /**
   * Dumps the contents of a single Mailbox.
   * @param ownerEmail the owner of the mailbox.
   * @return the string representation of the Mailbox.
   * @throws Exception if one is thrown.
   */
  public static String dumpMailbox(String ownerEmail) throws Exception {
    StringBuilder mailboxBuilder = new StringBuilder();
    List<MimeMessage> messagesForThisUser = messages.get(ownerEmail);
    mailboxBuilder.append(ownerEmail).append(":[\n");
    for (MimeMessage message : messagesForThisUser) {
      mailboxBuilder.append(stringifyMimeMessage(message));
    }
    mailboxBuilder.append("],\n");
    return mailboxBuilder.toString();
  }

  /**
   * Custom stringification method for a given MimeMessage object. This is
   * incomplete, more details can be added, but this is all I needed.
   * @param message the MimeMessage to stringify.
   * @return the stringified MimeMessage.
   */
  public static String stringifyMimeMessage(MimeMessage message) throws Exception {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("From:").append(message.getFrom()[0].toString()).append("\n");
    messageBuilder.append("To:").append(message.getRecipients(RecipientType.TO)[0].toString()).append("\n");
    for (Enumeration<Header> e = message.getAllHeaders(); e.hasMoreElements();) {
      Header header = e.nextElement();
      messageBuilder.append("Header:").append(header.getName()).append("=").append(header.getValue()).append("\n");
    }
    messageBuilder.append("Subject:").append(message.getSubject()).append("\n");    messageBuilder.append(message.getContent() == null ? "No content" : message.getContent().toString());
    return messageBuilder.toString();
  }

@Override
public int getSize() throws MessagingException {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public int getLineCount() throws MessagingException {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public String getContentType() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public boolean isMimeType(String mimeType) throws MessagingException {
	// TODO Auto-generated method stub
	return false;
}

@Override
public String getDisposition() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setDisposition(String disposition) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public String getDescription() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setDescription(String description) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public String getFileName() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setFileName(String filename) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public InputStream getInputStream() throws IOException, MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public DataHandler getDataHandler() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Object getContent() throws IOException, MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setDataHandler(DataHandler dh) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void setContent(Object obj, String type) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void setText(String text) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void setContent(Multipart mp) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void writeTo(OutputStream os) throws IOException, MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public String[] getHeader(String header_name) throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setHeader(String header_name, String header_value) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void addHeader(String header_name, String header_value) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void removeHeader(String header_name) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public Enumeration getAllHeaders() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Enumeration getMatchingHeaders(String[] header_names) throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Enumeration getNonMatchingHeaders(String[] header_names) throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Address[] getFrom() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setFrom() throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void setFrom(Address address) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void addFrom(Address[] addresses) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public Address[] getRecipients(RecipientType type) throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public void addRecipients(RecipientType type, Address[] addresses) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public String getSubject() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setSubject(String subject) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public Date getSentDate() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setSentDate(Date date) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public Date getReceivedDate() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Flags getFlags() throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setFlags(Flags flag, boolean set) throws MessagingException {
	// TODO Auto-generated method stub
	
}

@Override
public Message reply(boolean replyToAll) throws MessagingException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void saveChanges() throws MessagingException {
	// TODO Auto-generated method stub
	
}
}