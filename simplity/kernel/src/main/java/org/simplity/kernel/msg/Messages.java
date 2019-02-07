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

import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works as a wrapper on top of component manager to deal with
 * messages. It also serves as a place to define al internally used messages
 *
 * @author simplity.org
 */
public class Messages {
	private static final Logger logger = LoggerFactory.getLogger(Messages.class);

	/*
	 * messages used internally
	 *
	 * these are defined in kernel.xml under msg folder. we have to ensure that
	 * this list and that file are in synch.
	 */
	/** a min number of rows expected */
	public static final String INVALID_VALUE = "kernel.invalidValue";
	/** a max nbr of rows expected */
	public static final String INVALID_DATA = "kernel.invalidData";
	/** */
	public static final String INVALID_FIELD = "kernel.invalidField";
	/** */
	public static final String INVALID_COMPARATOR = "kernel.invalidComparator";

	/** */
	public static final String INVALID_ENTITY_LIST = "kernel.invalidEntityList";

	/** */
	public static final String VALUE_REQUIRED = "kernel.valueRequired";
	/** */
	public static final String INVALID_FROM_TO = "kernel.invalidFromTo";
	/** */
	public static final String INVALID_OTHER_FIELD = "kernel.invalidOtherField";
	/** */
	public static final String INVALID_SORT_ORDER = "kernel.invalidSortOrder";
	/** */
	public static final String INVALID_BASED_ON_FIELD = "kernel.invalidBasedOnField";
	/** */
	public static final String INTERNAL_ERROR = "kernel.internalError";

	/** */
	public static final String INVALID_TABLE_ACTION = "kernel.invalidTableAction";

	/** */
	public static final String INVALID_SERIALIZED_DATA = "kernel.invalidInputStream";
	/** */
	public static final String NO_SERVICE = "kernel.noService";
	/** */
	public static final String NO_ACCESS = "kernel.noAccess";
	/** */
	public static final String SUCCESS = "kernel.success";

	/** */
	public static final String ERROR = "kernel.error";
	/** warning */
	public static final String WARNING = "kernel.warning";

	/** info */
	public static final String INFO = "kernel.info";

	/** no rows were selected */
	public static final String NO_ROWS = "kernel.noRows";

	/** no rows were inserted/updated */
	public static final String NO_UPDATE = "kernel.noUpdate";

	/**
	 * client has sent an invalid attachment key. Client should send only the
	 * key that it would have received after a successful upload of attachment
	 */
	public static final String INVALID_ATTACHMENT_KEY = "kernel.invalidAttachmentKey";

	/**
	 * get message text for this message after formatting based on parameters
	 *
	 * @param messageName
	 * @param parameters
	 * @return formatted message text
	 */
	public static FormattedMessage getMessage(String messageName, String... parameters) {
		Message message = (Message) Application.getActiveInstance().getComponentOrNull(ComponentType.MSG, messageName);
		if (message == null) {
			message = defaultMessage(messageName);
		}
		return message.getFormattedMessage(parameters);
	}

	private static Message defaultMessage(String messageName) {
		Message msg = new Message();
		msg.name = messageName;
		msg.text = messageName + " : description for this message is not found.";

		logger.info("Missing message : " + messageName);

		return msg;
	}
}
