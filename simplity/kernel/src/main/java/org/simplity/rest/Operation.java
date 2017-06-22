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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.HttpUtil;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.rest.param.Parameter;
import org.simplity.service.ServiceProtocol;

/**
 * service specification based on Open API for a given operation.
 * This is a dummy wrapper on the JSON object returned by api
 *
 * @author simplity.org
 *
 */
public class Operation {
	/**
	 * service name that this operation maps to.
	 */
	private String serviceName;
	/**
	 * accept whatever has come from client. No parameter specification to
	 * parse/validate. Service will take care of validations.
	 */
	private boolean acceptAll;
	/*
	 * input parameters are split by type for run-time efficiency. Relevant only
	 * if accpetAll is false
	 */
	private Parameter bodyParameter;
	private Parameter[] qryParameters;
	private Parameter[] headerParameters;
	private Parameter[] pathParameters;
	private boolean bodyIsFormData;
	private String bodyFieldName;
	private ServiceTranslator translator;

	/*
	 * those were the fields for input. Rest of the fields are for output
	 */
	/**
	 * service has taken care of all data requirements. Just send the response
	 * with all that the service has responded with.
	 */
	private boolean sendAll;
	private Response[] successResponses;
	private Response[] failureResponses;
	private Response defaultResponse;

	/**
	 *
	 * @param operationSpec
	 * @param serviceName
	 */
	public Operation(JSONObject operationSpec, String serviceName) {
		this.serviceName = serviceName;
		this.setCustomAttributes(operationSpec);
		JSONArray params = operationSpec.optJSONArray(Tags.PARAMS_ATTR);
		/*
		 * organize input parameters
		 */
		if (params != null) {
			this.parseParams(params);
		}
		/*
		 * organize responses
		 */
		JSONObject resps = operationSpec.optJSONObject(Tags.RESP_ATTR);
		if (resps == null || resps.length() == 0) {
			Tracer.trace("Response spec is missing for operation " + serviceName
					+ ". Applicaiton level defult will be used for formatting response.");
			return;
		}
		this.parseResponses(resps);
	}

	/**
	 * set custom attributes based on this spec and defaults set at by the
	 * context
	 *
	 * @param spec
	 */
	private void setCustomAttributes(JSONObject spec) {
		RestContext ctx = RestContext.getContext();
		String cls = spec.optString(Tags.TRANSLATOR_ATTR, null);
		if (cls == null) {
			this.translator = ctx.getServiceTranslator();
		} else {
			try {
				this.translator = (ServiceTranslator) Class.forName(cls).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						cls + " could not be used to get an instance of " + ServiceTranslator.class.getName());
			}
		}
		this.acceptAll = spec.optBoolean(Tags.ACCEPT_ALL_ATTR, false);
		this.sendAll = spec.optBoolean(Tags.SEND_ALL_ATTR, false);

	}

	/**
	 * parse input parameters and populate corresponding input related fields
	 *
	 * @param params
	 */
	private void parseParams(JSONArray params) {
		List<Parameter> qry = null;
		List<Parameter> path = null;
		List<Parameter> header = null;

		for (Object obj : params) {
			JSONObject json = (JSONObject) obj;
			String pin = json.optString(Tags.IN_ATTR);
			Parameter parm = Parameter.parse(json);
			if (Tags.IN_BODY.equals(pin) || Tags.IN_FORM.equals(pin)) {
				if (this.bodyParameter == null) {
					this.bodyIsFormData = Tags.IN_FORM.equals(pin);
					if (RestContext.getContext().retainBodyAsObject()) {
						this.bodyFieldName = parm.getName();
					}

					this.bodyParameter = parm;
				} else {
					throw new ApplicationError("More than one body field defined for operation " + this.serviceName);
				}
			} else if (Tags.IN_QUERY.equals(pin)) {
				if (qry == null) {
					qry = new ArrayList<Parameter>();
				}
				qry.add(parm);

			} else if (Tags.IN_PATH.equals(pin)) {
				if (path == null) {
					path = new ArrayList<Parameter>();
				}
				path.add(parm);

			} else if (Tags.IN_HEADER.equals(pin)) {
				if (header == null) {
					header = new ArrayList<Parameter>();
				}
				header.add(parm);
			} else {
				Tracer.trace("Parameter " + parm.getName() + " ignored as it has an invalid 'in' attribute of " + pin);
			}
		}
		Parameter[] empty = new Parameter[0];
		if (qry != null) {
			this.qryParameters = qry.toArray(empty);
		}
		if (path != null) {
			this.pathParameters = path.toArray(empty);
		}
		if (header != null) {
			this.headerParameters = header.toArray(empty);
		}
	}

	/**
	 * parse responses
	 *
	 * @param resps
	 */
	private void parseResponses(JSONObject resps) {
		/*
		 * we are assuming that 2xx code is success and 5xx is failure. Other
		 * codes are generally handled by the controller/agent and are not
		 * relevant for the operation/service
		 */
		List<Response> successes = new ArrayList<Response>();
		List<Response> failures = new ArrayList<Response>();
		for (String code : resps.keySet()) {
			JSONObject obj = resps.optJSONObject(code);
			if (obj == null) {
				throw new ApplicationError("Response object is null for " + code);
			}
			char c = code.charAt(0);
			if (c == '5' || c == '4') {
				failures.add(new Response(code, obj));
			} else if (c == '2') {
				successes.add(new Response(code, obj));
			} else if (code.equals("default")) {
				this.defaultResponse = new Response(null, obj);
			} else {
				Tracer.trace("Operation " + this.serviceName + "will ignore response code " + code
						+ ", assuming that it will be managed by the controller/agent.");
			}
		}
		int nbr = successes.size();
		Response[] empty = new Response[0];
		if (nbr > 0) {
			this.successResponses = successes.toArray(empty);
		}
		nbr = failures.size();
		if (nbr > 0) {
			this.failureResponses = successes.toArray(empty);
		}
	}

	/**
	 * @return the serviceName
	 */

	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * input has data received, except from header. validate and copy based on
	 * parameter specifications
	 *
	 * @param req
	 *            request
	 * @param serviceData
	 *            non-null, into which all data is copied to. Typically this is
	 *            an empty JSON, but it is upto the caller.
	 * @param pathData
	 *            can be null.
	 *            field values from path templating.
	 * @param messages
	 *            non-null. Any error during parsing/validation is added to this
	 *            list
	 * @return serviceName to be used for this operation. null in case of any
	 *         error.
	 * @throws IOException
	 */
	public String prepareRequest(HttpServletRequest req, JSONObject serviceData, JSONObject pathData,
			List<FormattedMessage> messages) throws IOException {
		/*
		 * get body/form data into a json first. Of course it would be empty if
		 * there is no body
		 */
		JSONObject bodyData = null;
		if (this.bodyParameter == null) {
			bodyData = new JSONObject();
		} else if (this.bodyIsFormData) {
			bodyData = HttpUtil.getFormAsJson(req, null);
		} else {
			bodyData = HttpUtil.getPayloadAsJson(req, null);
		}

		if (this.acceptAll) {
			Tracer.trace("We are to accept all data. we assume there are no data in hrader");
			HttpUtil.parseQueryString(req, serviceData);
			if (pathData != null) {
				JsonUtil.copyAll(serviceData, pathData);
			}
		} else {
			if (this.bodyParameter != null) {
				bodyData = (JSONObject) this.bodyParameter.validate(bodyData, messages);
			}
			if (this.qryParameters != null) {
				this.parseAndValidate(this.qryParameters, HttpUtil.parseQueryString(req, null), serviceData, messages);
			}
			if (this.pathParameters != null) {
				if (pathData == null) {
					this.parseAndValidate(this.pathParameters, new JSONObject(), serviceData, messages);
				} else {
					this.parseAndValidate(this.pathParameters, bodyData, serviceData, messages);
				}
			}
			if (this.headerParameters != null) {
				this.parseAndValidateHeaderFields(req, serviceData, messages);
			}
			/*
			 * do we have errors?
			 */
			if (messages.size() > 0) {
				return null;
			}
		}
		if (this.bodyFieldName == null) {
			JsonUtil.copyAll(serviceData, bodyData);
		} else {
			serviceData.put(this.bodyFieldName, serviceData);
		}

		if (this.translator == null) {
			return this.serviceName;
		}
		return this.translator.translateInput(this.serviceName, serviceData);
	}

	/**
	 * extract data from header and validate as per spec
	 *
	 * @param req
	 * @param outData
	 *            to which data is to be extracted into
	 * @param messages
	 */
	private void parseAndValidateHeaderFields(HttpServletRequest req, JSONObject outData,
			List<FormattedMessage> messages) {
		for (Parameter parm : this.headerParameters) {
			String fieldName = parm.getName();
			Object obj = parm.validate(req.getHeader(fieldName), messages);
			if (obj != null) {
				outData.put(fieldName, obj);
			}
		}

	}

	/**
	 * parse fields from inData into outData as per parameter specifications
	 *
	 * @param parms
	 *            non-null array of parameters
	 * @param inData
	 *            non-null data as received from client
	 * @param outData
	 *            non-null json object to which validated input fields are
	 *            copied to
	 *
	 * @param messages
	 *            to which any error is added. Once an error is added,
	 *            outputData is to be treated as unusable.
	 */
	private void parseAndValidate(Parameter[] parms, JSONObject inData, JSONObject outData,
			List<FormattedMessage> messages) {
		for (Parameter parm : parms) {
			String fieldName = parm.getName();
			Object obj = parm.validate(inData.opt(fieldName), messages);
			if (obj != null) {
				outData.put(fieldName, obj);
			}
		}
	}

	/**
	 * writes response based on service output and service spec
	 *
	 * @param resp
	 * @param data
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, JSONObject data) throws IOException {
		Response response = this.selectResponse(this.successResponses, data);
		response.writeResponse(resp, data, this.bodyFieldName, this.sendAll);
	}

	private Response selectResponse(Response[] responses, JSONObject data) {
		if (responses == null) {
			return this.defaultResponse;
		}

		if (responses.length == 1) {
			return responses[0];
		}
		if (data != null) {
			int code = data.optInt(ServiceProtocol.HTTP_RESP_CODE_FIELD_NAME, 0);
			if (code != 0) {
				for (Response response : responses) {
					if (response.getCode() == code) {
						return response;
					}
				}
			}
		}
		Tracer.trace("We are unable to pick a response from multiple choices, and hence choosing the first one");
		return responses[0];
	}

	/**
	 * @param resp
	 * @param messages
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, FormattedMessage[] messages) throws IOException {
		Response response = this.selectResponse(this.failureResponses, null);
		response.writeResponse(resp, messages);
	}

}
