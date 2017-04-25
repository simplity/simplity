package org.simplity.kernel.mail;

import java.io.Serializable;

public class MailAttachment implements Serializable {

	private static final long serialVersionUID = 8189730674999834850L;
	
	public String name;
	public String filepath;
	
	public MailAttachment() {
		
	}
	
	/**
	 * name - id of the attachment
	 * filepath - complete file name (along with file path)
	 */
	public MailAttachment(String name, String filepath) {
		this.name = name;
		this.filepath = filepath;
	}

}