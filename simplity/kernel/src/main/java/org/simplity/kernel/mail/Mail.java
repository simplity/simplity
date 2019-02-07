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
package org.simplity.kernel.mail;

import java.io.Serializable;

/**
 * data structure that holds all data about a mail
 *
 * @author simplity.org
 *
 */
public class Mail implements Serializable {
	private static final long serialVersionUID = -4314888435710523295L;

	/**
	 * valid e-mail id
	 */
	public String fromId;
	/**
	 * comma separated list of valid  e-mail ids
	 */
	public String toIds;
	/**
	 * comma separated list of valid  e-mail ids
	 */
	public String ccIds;
	/**
	 * comma separated list of valid  e-mail ids
	 */
	public String bccIds;

	/**
	 * non-null text to be used as subject
	 */
	public String subject;
	/**
	 * non-null text to be used as content of the mail
	 */
	public String content;
	/**
	 * attachments
	 */
	public MailAttachment[] attachment;
	/**
	 * if content refers to any in-line attachment, they have to be specified here
	 */
	public MailAttachment[] inlineAttachment;
}
