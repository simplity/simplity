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
 * Data structure to hold information about a file attachment
 *
 * @author simplity.org
 *
 */
public class MailAttachment implements Serializable {

	private static final long serialVersionUID = 8189730674999834850L;

	/**
	 * name of this attachment
	 */
	public final String name;
	/**
	 * valid path from file system. We will redesign this to be URL if needed
	 */
	public final String filepath;

	/**
	 * @param name id of the attachment. must be unique across all attachments for a given mail
	 * @param filepath valid path from file system
	 */
	public MailAttachment(String name, String filepath) {
		this.name = name;
		this.filepath = filepath;
	}
}
