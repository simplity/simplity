/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.app.internal;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.app.IServiceResponse;
import org.simplity.core.app.ServiceResult;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.util.XmlUtil;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author simplity.org
 *
 */
public class ServiceResponse implements IServiceResponse {
	private static final Logger logger = LoggerFactory.getLogger(ServiceResponse.class);

	private Map<String, Object> sessionFields;
	private ServiceResult result;
	private FormattedMessage[] messages;
	private int executionTime;
	private boolean isXml;
	private Writer writer;
	private StringWriter StringWriter;

	/**
	 * a service response that does not provide a writer, but expects the
	 * payload to be ready as an object
	 *
	 * @param isXml
	 *            should be other than JSON_STREAM and XML_STREAM
	 */
	public ServiceResponse(boolean isXml) {
		this.isXml = isXml;
	}

	/**
	 * a service response that expects the service to write response payload to
	 * its writer
	 *
	 * @param writer
	 *            writer that would provide the response payload
	 * @param isXml
	 *            we use either json or xml. so, false implies json
	 */
	public ServiceResponse(Writer writer, boolean isXml) {
		this.writer = writer;
		this.isXml = isXml;
	}

	@Override
	public void setResult(ServiceResult result, int millis) {
		this.result = result;
		this.executionTime = millis;

	}

	@Override
	public boolean isStreaming() {
		return this.writer != null;
	}

	@Override
	public boolean isXml() {
		return this.isXml;
	}

	/**
	 * set messages. If the result is not ALL_OK, then at least one error
	 * message explaining the failure must be included.
	 *
	 * @param messages
	 */
	@Override
	public void setMessages(FormattedMessage[] messages) {
		this.messages = messages;
	}

	@Override
	public void setSessionField(String key, String value) {
		this.ensureMap();
		this.sessionFields.put(key, value);
	}

	@Override
	public IResponseWriter getPayloadWriter(boolean responseIsAnArray) {
		Writer riter = this.writer;
		if (riter == null) {
			riter = this.StringWriter = new StringWriter();
		}
		if (this.isXml) {
			try {
				return new XmlRespWriter(riter);
			} catch (XMLStreamException e) {
				throw new ApplicationError(e, "Error while creating a resp writer using xml writer");
			}
		}
		return new JsonRespWriter(riter, responseIsAnArray);
	}

	/**
	 *
	 * @return service result
	 */
	@Override
	public ServiceResult getServiceResult() {
		return this.result;
	}

	/**
	 *
	 * @return payload as an xml document. throws ApplicationError if this is
	 *         not appropriater
	 */
	public Document getPayloadXml() {
		this.checkState(true);
		if (this.StringWriter == null) {
			logger.warn(
					"getPayloadXml invoked before the writer was even opened for writing. returning empty document");
			return XmlUtil.newEmptyDocument();
		}

		return XmlUtil.textToDoc(this.StringWriter.toString());
	}

	private void checkState(boolean forXml) {
		if (forXml) {
			if (!this.isXml) {
				throw new ApplicationError("Design Error: getPayloadXml invoked when payload type is json");
			}
		} else if (this.isXml) {
			throw new ApplicationError("Design Error: getPayloadJson invoked when payload type is xml");
		}

		if (this.writer != null) {
			throw new ApplicationError("Design Error: getPayload when payload is streamed directly into a writer");
		}

	}

	/**
	 *
	 * @return payload json. ApplicationErorr is thrown if it is not meant to
	 *         generate a json object as response
	 */
	public JSONObject getPayloadJson() {
		this.checkState(false);
		if (this.StringWriter == null) {
			logger.warn("getPayloadJson invoked before the writer was even opened for writing. returning empty JSON");
			return new JSONObject();
		}

		return new JSONObject(this.StringWriter.toString());
	}

	/**
	 *
	 * @return payload text. ApplicationError is thrown if this response is
	 *         directly streamed,
	 */
	public String getPayloadText() {
		if (this.writer != null) {
			throw new ApplicationError("getPayloadText() not possible for streamed response");
		}
		if (this.StringWriter != null) {
			return this.StringWriter.toString();
		}
		logger.error("No payload created. returning empty string");
		return "";
	}

	@Override
	public Map<String, Object> getSessionFields() {
		return this.sessionFields;
	}

	private void ensureMap() {
		if (this.sessionFields == null) {
			this.sessionFields = new HashMap<>();
		}
	}

	/**
	 *
	 * @return execution time claimed by the service in milliseconds
	 */
	@Override
	public int getExecutionTime() {
		return this.executionTime;
	}

	@Override
	public FormattedMessage[] getMessages() {
		return this.messages;
	}
}
