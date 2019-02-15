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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * data structure that holds properties required to set-up a mail server
 *
 * @author simplity.org
 *
 */
public class MailSetup {
	private static final Logger logger = LoggerFactory.getLogger(MailSetup.class);
	/**
	 * host
	 */
	String host;
	/**
	 * port
	 */
	String port;

	/**
	 * called from Application before using it
	 *
	 * @return error message in case of any error. null if all OK.
	 */
	public String configure() {
		String msg = "Mail set up not yet implemneted ";
		logger.error(msg);
		return msg;
	}

}
