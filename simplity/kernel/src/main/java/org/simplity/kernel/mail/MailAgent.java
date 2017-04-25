package org.simplity.kernel.mail;

import java.util.Properties;

import org.simplity.kernel.Tracer;

public class MailAgent {
	static Properties props;

	public static void initialSetup(MailProperties mailProperties) {
		Tracer.trace("Setting up the Mail Agent");
		props = new Properties();
		props.setProperty("mail.smtp.host", mailProperties.host);
		props.setProperty("mail.smtp.port", mailProperties.port);
	}

	public static Properties getProperties() {
		return props;
	}

}
