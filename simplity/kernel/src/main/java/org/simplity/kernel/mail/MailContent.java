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

/**
 * data structure to hold data for mail content
 *
 * @author simplity.org
 *
 */
public class MailContent {

	/**
	 * in case we use template, the root path to the template folder. null if
	 * text is used.
	 */
	public String templatePath;
	/**
	 * name of template to be used. non-null if templatePAth is non-null.
	 */
	public String template;
	/**
	 * data sheets that has data for fields in the template. one mail sent per
	 * sheet.non-null if template is used.
	 */
	public String[] inputSheetNames;
	/**
	 * actual text to be used as content, in case template is not used. null if
	 * template is used, non-null if template is not used
	 */
	public String text;
}
