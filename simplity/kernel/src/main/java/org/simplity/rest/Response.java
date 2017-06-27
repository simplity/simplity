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

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.rest.param.ArrayParameter;
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
	/*
	 * default success and failure responses in case of emergency!!
	 */
	private static final int DEFAULT_ERROR_CODE = 422;
	private static final String EMPTY_RESPONSE = "{}";
	private static Response defaultFailueResponse = createDefFail();
	private static Response defaultSuccessResponse = createDefSuccess();

	private static Response createDefFail() {
		Response resp = new Response();
		resp.sendAllData = true;
		resp.responseCode = RestContext.getContext().getDefaultFailureResponseCode();
		return resp;
	}

	private static Response createDefSuccess() {
		Response resp = new Response();
		resp.responseCode = RestContext.getContext().getDefaultSuccessResponseCode();
		resp.sendAllData = true;
		return resp;
	}

	/**
	 * @return a response object instance that is suitable to respond back when
	 *         there is some error
	 */
	public static Response getDefaultForFailure() {
		return defaultFailueResponse;
	}

	/**
	 * @return a response object instance that is suitable to respond back when
	 *         all-ok
	 */
	public static Response getDefaultForSuccess() {
		return defaultSuccessResponse;
	}

	private boolean sendAllData;
	private int responseCode;
	private Parameter[] headers;
	private Parameter bodyParameter;
	private String bodyFieldName;

	private Response() {
		// for private use only
	}

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
				this.headers[i] = Parameter.parse(keys[i], null, obj.getJSONObject(key));
			}
		}

		this.bodyFieldName = responseJson.optString(Tags.FIELD_NAME_ATTR, null);
		obj = responseJson.optJSONObject(Tags.SCHEMA_ATTR);
		if (obj != null) {
			this.bodyParameter = Parameter.parse(obj);
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
	 *            name of the field that has the data for body. null if data
	 *            itself has field values for body
	 * @param sendAll
	 *            send everything from input data.DO not go by swagger spec.
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, JSONObject data, boolean sendAll)
			throws IOException {
		if (this.responseCode != 0) {
			resp.setStatus(this.responseCode);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
		}
		if (this.headers != null) {
			for (Parameter hdr : this.headers) {
				hdr.setHeader(resp, data.opt(hdr.getFieldName()));
			}
		}

		if (sendAll || this.sendAllData) {
			if (data == null) {
				resp.getWriter().write(EMPTY_RESPONSE);
			} else {
				resp.getWriter().write(data.toString());
			}
			return;
		}

		if (this.bodyParameter == null) {
			return;
		}

		if (data == null) {
			Tracer.trace("No response object suplied for response. no body response.");
			return;
		}

		Object bodyData = data;
		if(this.bodyFieldName != null){
			bodyData = data.opt(this.bodyFieldName);
			if(bodyData == null){
				Tracer.trace("Response field " + this.bodyFieldName + " has no value. No response sent.");
				return;
			}
		}
		if(this.bodyParameter instanceof ObjectParameter){
			if(bodyData instanceof JSONObject){
				this.bodyParameter.toWriter(new JSONWriter(resp.getWriter()), bodyData, false);
			}else{
				Tracer.trace("We expected a JSON Object as response value with bodyFieldName=" + this.bodyFieldName + " but we got " + bodyData.getClass().getName() + " as value. Null value assumed for response.");
			}
			return;
		}

		if(this.bodyParameter instanceof ArrayParameter){
			ArrayParameter ap = (ArrayParameter)this.bodyParameter;
			if(bodyData instanceof JSONArray){
				if(ap.expectsTextValue() == false){
					this.bodyParameter.toWriter(new JSONWriter(resp.getWriter()), bodyData, false);
					return;
				}
				bodyData = ap.serialize((JSONArray)bodyData);
			}
		}
		resp.getWriter().write(bodyData.toString());
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
