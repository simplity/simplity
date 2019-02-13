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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	private Document xmlPayload;
	private JSONObject jsonPayload;
	/**
	 * actual payload. null if reader is non-null, or there is no payload at
	 * all. generally json or document, except for internal services when
	 * service context is used as input source.
	 */
	private Object payload;
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
	 * request object for an xml payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param xmlPayload
	 *            non-null document that is the payload
	 */
	public ServiceRequest(String serviceName, Document xmlPayload) {
		this.serviceName = serviceName;
		this.xmlPayload = xmlPayload;
	}

	/**
	 * request object for a json payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param jsonPayload
	 *            non-null JSON that is the payload
	 */
	public ServiceRequest(String serviceName, JSONObject jsonPayload) {
		this.serviceName = serviceName;
		this.jsonPayload = jsonPayload;
	}

	/**
	 * get an instance when there is no payload
	 *
	 * @param serviceName
	 *            non-null string
	 */
	public ServiceRequest(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * get an instance when there is no payload
	 *
	 * @param serviceName
	 *            non-null string
	 * @param inStream
	 *            non-null stream from which to read payload
	 * @param isXml
	 *            we use only json and xml. hence this boolean
	 * @throws Exception
	 *             in case the input is not a valid xml/json
	 */
	public ServiceRequest(String serviceName, InputStream inStream, boolean isXml) throws Exception {
		this.serviceName = serviceName;
		if (isXml) {
			this.xmlPayload = XmlUtil.fromStream(inStream);
		} else {

			String str = IoUtil.streamToText(inStream);
			if (str == null || str.isEmpty()) {
				str = "{}";
			}
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
	public Set<String> getFieldNames() {
		if (this.fields == null) {
			return new HashSet<>();
		}
		return this.fields.keySet();
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
		if (this.xmlPayload == null) {
			if (this.jsonPayload == null) {
				logger.info("null reader returned as there is no payload");
				return null;
			}
			return new JsonReqReader((JSONObject) this.payload);
		}
		return new XmlReqReader((Document) this.payload);
	}

	/**
	 * @param fields
	 *            the fields to set
	 */
	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}

	/**
	 * set/remove value for a field
	 *
	 * @param fieldName
	 *            name of the field
	 * @param fieldValue
	 *            value of the field to set. if null, current value, removed.
	 * @return current value of this field
	 */
	public Object setFieldValue(String fieldName, Object fieldValue) {
		if (fieldValue == null) {
			return this.fields.remove(fieldName);
		}
		return this.fields.put(fieldName, fieldValue);
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
		return this.payload;
	}
}
