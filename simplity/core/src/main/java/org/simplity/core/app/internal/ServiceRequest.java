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

import java.io.InputStream;
import java.util.Map;

import org.simplity.core.MessageBox;
import org.simplity.core.app.AppUser;
import org.simplity.core.app.IRequestReader;
import org.simplity.core.app.IServiceRequest;
import org.simplity.core.util.IoUtil;
import org.simplity.core.util.XmlUtil;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author simplity.org
 *
 */
public class ServiceRequest implements IServiceRequest {
	private static final Logger logger = LoggerFactory.getLogger(ServiceRequest.class);
	private String serviceName;
	private AppUser appUser;
	/*
	 * only one of the three payload would be non-null
	 */
	/**
	 * if the pyload is xml.
	 */
	private Document xmlPayload;
	/**
	 * if the payload if json.
	 */
	private JSONObject jsonPayload;
	/**
	 * fields that are meant to be from client, but are not in payload. like the
	 * fields in REST url, query strings, cookies and session fields
	 */
	private Map<String, Object> fields;

	/**
	 * special arrangement for service to communicate its progress to the
	 * requester. Used in batch case
	 */
	private MessageBox messageBox = null;

	/**
	 * context maintained on client layer (typically web/servlet). This is a big
	 * NO from our side because it holds us back from hosting the two layers on
	 * different JVMs. Provisioned for us to co-exist with Apps that need this
	 * feature.
	 */
	private Object clientContext;

	/**
	 * get an instance when there is no payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param fields
	 *            can be null
	 */
	public ServiceRequest(String serviceName, Map<String, Object> fields) {
		this.serviceName = serviceName;
		this.fields = fields;
	}

	/**
	 * request object for an xml payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param fields
	 *            can be null;
	 * @param xmlPayload
	 *            non-null document that is the payload
	 */
	public ServiceRequest(String serviceName, Map<String, Object> fields, Document xmlPayload) {
		this(serviceName, fields);
		this.xmlPayload = xmlPayload;
	}

	/**
	 * request object for a json payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param fields
	 *            can be null
	 * @param jsonPayload
	 *            non-null JSON that is the payload
	 */
	public ServiceRequest(String serviceName, Map<String, Object> fields, JSONObject jsonPayload) {
		this(serviceName, fields);
		this.jsonPayload = jsonPayload;
	}

	/**
	 * get an instance when there is no payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param fields
	 *            can be null
	 * @param inStream
	 *            non-null stream from which to read payload
	 * @param isXml
	 *            we use only json and xml. hence this boolean
	 * @throws Exception
	 *             in case the input is not a valid xml/json
	 */
	public ServiceRequest(String serviceName, Map<String, Object> fields, InputStream inStream, boolean isXml)
			throws Exception {
		this(serviceName, fields);
		if (isXml) {
			Document doc = XmlUtil.fromStream(inStream);
			this.xmlPayload = doc;
			logger.info("Input XML = {}", XmlUtil.prettyPrint(doc.getDocumentElement()));
		} else {

			String str = IoUtil.streamToText(inStream);
			if (str == null || str.isEmpty()) {
				logger.info("No input in payload. EMpty payload created.");
				str = "{}";
			}
			logger.info("Iput JSON = {}", str);
			this.jsonPayload = new JSONObject(str);
		}
	}

	/* ************ interface methods ************ */
	/**
	 * @return the serviceName
	 */
	@Override
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * @return the userId
	 */
	@Override
	public AppUser getUser() {
		return this.appUser;
	}

	/**
	 *
	 * @param appUser
	 */
	public void setUser(AppUser appUser) {
		this.appUser = appUser;
	}

	/**
	 *
	 * @param fieldName
	 * @return value of the named field, or null if no such field
	 */
	@Override
	public Object getFieldValue(String fieldName) {
		if (this.fields == null) {
			return null;
		}
		return this.fields.get(fieldName);
	}

	/**
	 *
	 * @return set containing all the fields. empty set if there are no fields
	 */
	@Override
	public Map<String, Object> getFieldValues() {
		return this.fields;
	}

	/**
	 * @return the messageBox
	 */
	@Override
	public MessageBox getMessageBox() {
		return this.messageBox;
	}

	/**
	 * @return the clientContext
	 */
	@Override
	public Object getClientContext() {
		return this.clientContext;
	}

	@Override
	public IRequestReader getPayloadReader() {
		if (this.jsonPayload != null) {
			return new JsonReqReader(this.jsonPayload, this.fields);
		}
		if (this.xmlPayload != null) {
			return new XmlReqReader(this.xmlPayload);
		}
		logger.error("null reader returned as there is no payload");
		return null;
	}

	/**
	 * @param messageBox
	 *            the messageBox to set
	 */
	public void setMessageBox(MessageBox messageBox) {
		this.messageBox = messageBox;
	}

	/**
	 * @param clientContext
	 *            context maintained on client layer (typically web/servlet).
	 *            This is a big NO from our side because it holds us back from
	 *            hosting the two layers on different JVMs. Provisioned for us
	 *            to co-exist with Apps that need this feature. This will be
	 *            available in service context with getClientContext() method
	 */
	public void setClientContext(Object clientContext) {
		this.clientContext = clientContext;
	}

	/**
	 *
	 * @return get the payload as it is
	 */
	public Object getPayload() {
		if (this.jsonPayload != null) {
			return this.jsonPayload;
		}
		if (this.xmlPayload != null) {
			return this.xmlPayload;
		}
		logger.error("No payload is set. returning null as payload");
		return null;
	}
}
