/*
 * Copyright (c) 2019 simplity.org
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
package org.simplity.fm;

/**
 * represents a validation error while accepting data from a client for a field
 * 
 * @author simplity.org.
 *
 */
public class Message {
	/**
	 * create a message that is not associated with any field
	 * 
	 * @param messageType
	 * @param messageId
	 * @param fieldName
	 *            null if this message does not refer to any field
	 * @param gridName
	 *            null if this message does not refer to any field in a grid
	 * @param rowNumber
	 *            zero based row in the grid,if grid is relevant
	 * @return a message that is not associated with any field
	 */
	public static Message getGenericMessage(MessageType messageType, String messageId, String fieldName,
			String gridName, int rowNumber) {
		return new Message(messageType, messageId, fieldName, gridName, rowNumber);
	}

	/**
	 * create a validation error message for a field
	 * 
	 * @param fieldName
	 * @param messageId
	 * @return validation error message
	 */
	public static Message getValidationMessage(String fieldName, String messageId) {
		return new Message(MessageType.Error, messageId, fieldName, null, 0);
	}

	/**
	 * create a validation error message for a field
	 * 
	 * @param fieldName
	 * @param messageId
	 * @param gridName 
	 * @param rowNumber 
	 * @return validation error message
	 */
	public static Message getValidationMessage(String fieldName, String messageId, String gridName, int rowNumber) {
		return new Message(MessageType.Error, messageId, fieldName, gridName, rowNumber);
	}
	/**
	 * name of the field/column that is in error. null if the error is not
	 * specific to a field
	 */
	public final String fieldName;

	/**
	 * error message id for this error. non-null;
	 */
	public final String messageId;

	/**
	 * name of the data grid in error. Null , if this is a field, and not a grid
	 */
	public final String gridName;

	/**
	 * zero based row number in case this is a data grid
	 */
	public final int rowNumber;

	/**
	 * message type/severity.
	 */
	public MessageType messageType;


	private Message(MessageType messageType, String messageId, String fieldName, String gridName, int rowNumber) {
		this.messageType = messageType;
		this.messageId = messageId;
		this.rowNumber = rowNumber;
		this.fieldName = fieldName;
		this.gridName = gridName;
	}
}
