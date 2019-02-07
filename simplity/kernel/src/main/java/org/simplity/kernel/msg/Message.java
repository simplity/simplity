/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.msg;

import org.simplity.kernel.comp.IComponent;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationUtil;

/**
 * messages externalize text and allow multi-lingual features.
 *
 * @author simplity.org
 */
public class Message implements IComponent {
	private static final ComponentType MY_TYPE = ComponentType.MSG;
	private static String DELIMITER = ": ";
	private static String PREFIX = "$";
	/** name to be unique in a project. If we use */
	@FieldMetaData(isRequired = true)
	String name;
	/**
	 * text in English with place holders like $1 for run-time values. We will
	 * have these in multiple languages (like resource bundle)
	 */
	@FieldMetaData(isRequired = true)
	String text;

	MessageType messageType = MessageType.ERROR;

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public String getQualifiedName() {
		return this.name;
	}

	@Override
	public void getReady() {
		//
	}

	/** @return message type */
	public MessageType getMessageType() {
		return this.messageType;
	}

	/**
	 * Lots of cops around ask for identity!!
	 *
	 * @return message name
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name + Message.DELIMITER + this.text;
	}

	/**
	 * not yet implemented. You will get it in English
	 *
	 * @param language
	 *            ignored as of now
	 * @return text for the desired language
	 */
	public String toString(String language) {
		return this.name + Message.DELIMITER + this.text;
	}

	/**
	 * substitute parameter values into text
	 *
	 * @param params
	 *            parameters for message
	 * @return formatted text
	 */
	public String toString(String[] params) {
		String txt = this.text;
		if (params != null && params.length > 0) {
			for (int i = 0; i < params.length; i++) {
				String parm = Message.PREFIX + (i + 1);
				if (txt.indexOf(parm) == -1) {
					txt += params[i];
				} else {
					txt = txt.replace(parm, "" + params[i]);
				}
			}
		}
		return txt;
	}

	/**
	 * get message after substituting parameters. Language specific feature not
	 * yet implemented.
	 *
	 * @param language
	 *            ignored as of now
	 * @param params
	 *            parameters for message
	 * @return formatted text
	 */
	public String toString(String language, String[] params) {
		return this.toString(params);
	}

	/**
	 * substitute parameter values into text
	 *
	 * @param params
	 *            additional parameters
	 * @return formatted text
	 */
	public FormattedMessage getFormattedMessage(String[] params) {
		FormattedMessage msg = new FormattedMessage(this.getName(), this.messageType, this.toString(params));
		return msg;
	}

	/**
	 * get message after substituting parameters. Language specific feature not
	 * yet implemented.
	 *
	 * @param language
	 *            not yet implemented. ignored as of now.
	 * @param params
	 *            additional parameters for the message
	 * @return formatted text
	 */
	public FormattedMessage getFormattedMessage(String language, String[] params) {
		FormattedMessage msg = new FormattedMessage(this.getName(), this.messageType, this.toString(params));
		return msg;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#validate()
	 */
	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}
}
