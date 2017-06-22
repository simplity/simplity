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

package org.simplity.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.rest.param.ObjectParameter;
import org.simplity.rest.param.Parameter;
import org.simplity.service.ServiceProtocol;

/**
 * represents a response from a swagger (open api) document
 *
 * @author simplity.org
 *
 */
public class Response {
	private static final int DEFAULT_ERROR_CODE = 422;
	private int responseCode;
	private Parameter[] headers;
	private Parameter bodyParameter;

	/**
	 * construct based on spec json
	 *
	 * @param code
	 * @param responseJson
	 */
	public Response(String code, JSONObject responseJson) {
		try {
			this.responseCode = Integer.parseInt(code);
		} catch (Exception e) {
			throw new ApplicationError("Invalid Http response code " + code);
		}
		JSONObject obj = responseJson.optJSONObject(Tags.HEADERS_ATTR);
		if (obj != null) {
			String[] keys = JSONObject.getNames(obj);
			this.headers = new Parameter[keys.length];
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				this.headers[i] = Parameter.parse(keys[i], obj.getJSONObject(key));
			}
		}

		obj = responseJson.optJSONObject(Tags.SCHEMA_ATTR);
		if (obj != null) {
			String fieldName = obj.optString(Tags.FIELD_NAME_ATTR, null);
			this.bodyParameter = new ObjectParameter(fieldName, obj);
		}
	}

	/**
	 * @return http response code to be used for this response
	 */
	public int getCode() {
		return this.responseCode;
	}

	/**
	 * writes response based on service output and service spec
	 *
	 * @param resp
	 * @param data
	 * @param bodyFieldName
	 *            name of the field that has the data for body. null if data
	 *            itself has field values for body
	 * @param sendAll
	 *            send everything from input data.DO not go by swagger spec.
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, JSONObject data, String bodyFieldName, boolean sendAll)
			throws IOException {
		if (this.responseCode == 0) {
			resp.setStatus(this.responseCode);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
		}
		if (this.headers != null) {
			for (Parameter hdr : this.headers) {
				hdr.setHeader(resp, data.opt(hdr.getName()));
			}
		}
		if (sendAll) {
			String txt;
			if (data == null) {
				txt = "{}";
			} else {
				txt = data.toString();
			}
			resp.getWriter().write(txt);
			return;
		}

		if (this.bodyParameter == null) {
			return;
		}

		if (data == null) {
			Tracer.trace("No response object suplied for response. sending an empty object");
			resp.getWriter().write("{}");
			return;
		}

		JSONWriter writer = new JSONWriter(resp.getWriter());
		writer.object();
		/*
		 * body parameter is always an ObjectParameter
		 */
		JSONObject bodyData = data;
		if (bodyFieldName != null) {
			bodyData = data.optJSONObject(bodyFieldName);
			if (bodyData == null) {
				Tracer.trace("We expected a JSON an attribute named " + bodyFieldName
						+ " with object value, but got null. We will use parent object itself as body object object ");
				bodyData = data;
			}
		}
		this.bodyParameter.toWriter(writer, bodyData, false);
		writer.endObject();
	}

	/**
	 * write a response when we have only messages, and no data
	 *
	 * @param resp
	 * @param messages
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, FormattedMessage[] messages) throws IOException {
		if (this.responseCode == 0) {
			resp.setStatus(this.responseCode);
		} else {
			resp.setStatus(DEFAULT_ERROR_CODE);
		}
		if (messages != null) {
			JSONWriter writer = new JSONWriter(resp.getWriter());
			writer.object();
			writer.key(ServiceProtocol.REQUEST_STATUS);
			writer.value(ServiceProtocol.STATUS_ERROR);
			writer.key(ServiceProtocol.MESSAGES);
			JsonUtil.addObject(writer, messages);
			writer.endObject();
		}
	}
}
