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
package org.simplity.tp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.InputData;
import org.simplity.kernel.data.OutputData;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Get response from a server using rest call
 *
 * @author infosys.com
 *
 */
public class HttpClient extends Action {
	private static final char DOLLAR = '$';
	private static final String JSON = "/json";
	private static final String XML = "/xml";

	/**
	 * Complete HTTP URL string starting. Example
	 * http://app.acnecorp.com/a/b/c.html?b=s&j=3
	 * this string may contain ${variableName} in which case the string is
	 * created at run time after substituting values for these at run time. If
	 * no value is found, we treat it as empty string and still go ahead.
	 */
	String urlString;
	String parsedUrlString;
	/**
	 * The HTTP method, GET, POST etc..
	 */
	String httpMethod;

	/**
	 * application/json, application/xml, and text/html are the common ones.
	 *
	 */

	String contentType;
	/**
	 * By default, we do not send any additional data as part of request. That
	 * is, we send only header for request. You may send data using a
	 * specification that is similar to OutputData of a service.
	 *
	 */
	OutputData requestData;
	/**
	 * In case the data to be sent for request is prepared using some logic into
	 * a field, we just send the value of that field
	 */
	String requestFieldName;
	/**
	 * By default, we extract all fields from response json/xml into service
	 * context. You may specify expected fields on the lines of
	 * inputSpecification for a service.
	 *
	 */
	InputData responseData;
	/**
	 * in case you have logic that processes the response, we set the response
	 * to this field
	 */
	String responseFieldName;
	/**
	 * Proxy details
	 */
	String proxy;
	/**
	 * proxy port. Required if proxy is specified.
	 */
	int proxyPort;
	/**
	 * Proxy username
	 */
	String proxyUserName;
	/**
	 * Proxy pwd
	 */
	String proxyPassword;

	/**
	 * in case url has variables in it, cache its parts for efficiency at run
	 * time. into an array which has its
	 * odd-idex (0-based) has names and other
	 */
	private String[] urlParts;

	/**
	 * if user and password are known at design time, we cache an instance of
	 * authentication
	 */
	private Authenticator authenticator;
	/**
	 * in case proxy user is a fieldName whose value is to be taken at run time
	 */
	private String proxyUserField;

	/**
	 * in case proxy pwd is a fieldName whose value is to be taken at run time
	 */
	private String proxyPwdField;
	/**
	 * handy boolean
	 */
	private boolean isJson;
	/**
	 * handy boolean
	 */
	private boolean isXml;

	/**
	 * default constructor
	 */
	public HttpClient() {

	}

	/**
	 * creating a client at run time
	 *
	 * @param urlString
	 * @param httpMethod
	 * @param contentType
	 * @param requestData
	 * @param requestFieldName
	 * @param responseData
	 * @param responseFieldName
	 * @param isJson
	 * @param isXml
	 */
	public HttpClient(String urlString, String httpMethod, String contentType, OutputData requestData,
			String requestFieldName, InputData responseData, String responseFieldName, boolean isJson, boolean isXml) {
		this.urlString = urlString;
		this.httpMethod = httpMethod;
		this.contentType = contentType;
		this.requestData = requestData;
		this.requestFieldName = requestFieldName;
		this.responseData = responseData;
		this.responseFieldName = responseFieldName;
		this.isJson = isJson;
		this.isXml = isXml;
	}

	@Override
	public Value doAct(ServiceContext ctx) {
		String txt;
		if (this.parsedUrlString != null) {
			this.urlString = ctx.getTextValue(this.parsedUrlString);
		}
		if (this.urlParts == null) {
			txt = this.urlString;
		} else {
			txt = TextUtil.substituteFields(this.urlParts, ctx);
		}
		String responseText;
		if (txt.equals(".")) {
			/*
			 * special case for loop back
			 */
			responseText = this.getRequestText(ctx);
		} else {
			responseText = this.getHttpResponse(txt, ctx);
		}
		if (this.isJson) {
			if (this.responseData != null) {
				this.responseData.extractFromJson(responseText, ctx);
			} else {
				JsonUtil.extractAll(new JSONObject(responseText), ctx);
			}
		} else if (this.isXml) {
			if (this.responseData != null) {
				throw new ApplicationError("We are not yet ready with xml based extraction of data.");
			}
			XmlUtil.extractAll(responseText, ctx);
		} else {
			ctx.setTextValue(this.responseFieldName, responseText);
		}
		return Value.VALUE_TRUE;
	}

	/**
	 * @param txt
	 * @param ctx
	 * @return
	 */
	private String getHttpResponse(String txt, ServiceContext ctx) {
		try {
			URL url = new URL(txt);
			HttpURLConnection conn = null;

			/*
			 * get connection
			 */
			if (this.proxy != null) {
				Authenticator auth = this.authenticator == null ? this.getAuth(ctx) : this.authenticator;

				Proxy proxyCon = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxy, this.proxyPort));
				Authenticator.setDefault(auth);
				conn = (HttpURLConnection) url.openConnection(proxyCon);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}

			/*
			 * despatch request
			 */
			conn.setRequestMethod(this.httpMethod);
			conn.setRequestProperty("Accept", this.contentType);
			conn.setRequestProperty("Conent-Type", this.contentType);
			conn.setDoOutput(true);
			String req = this.getRequestText(ctx);
			if (req != null) {
				conn.getOutputStream().write(req.getBytes("UTF-8"));
			}
			/*
			 * receive response
			 */
			int resp = conn.getResponseCode();
			if (resp != 200) {
				throw new ApplicationError("Http call for url " + url + " returned with a non200 status " + resp);
			}
			return readResponse(conn);
		} catch (ApplicationError e) {
			throw e;
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while rest call using url " + txt);
		}
	}

	/**
	 * @param ctx
	 * @return
	 */
	private String getRequestText(ServiceContext ctx) {
		if (this.requestFieldName != null) {
			String req = ctx.getTextValue(this.requestFieldName);
			if (req == null) {
				Tracer.trace("No value for field " + this.requestFieldName
						+ " in context, and hence no data is sent with the http request.");
			}
			return req;
		}
		if (this.requestData != null) {
			if (!this.isJson) {
				throw new ApplicationError("We are not ready with an xml output formatter.");

			}
			JSONWriter writer = new JSONWriter();
			writer.object();
			this.requestData.dataToJson(writer, ctx);
			writer.endObject();
			return writer.toString();
		}
		return null;
	}

	/**
	 * get an authenticator using our user name and password from ctx;
	 *
	 * @param ctx
	 * @return
	 */
	private Authenticator getAuth(ServiceContext ctx) {
		/*
		 * by our design, either userName or password is to be extracted from
		 * context
		 */
		String user;
		String pwd;
		if (this.proxyUserField == null) {
			user = this.proxyUserName;
		} else {
			Value value = ctx.getValue(this.proxyUserField);
			if (value == null) {
				throw new ApplicationError("Field " + this.proxyUserField
						+ " not found in service context at run time. This is required for a rest call through proxy.");
			}
			user = value.toString();
		}
		if (this.proxyPwdField == null) {
			pwd = this.proxyPassword;
		} else {
			Value value = ctx.getValue(this.proxyPwdField);
			if (value == null) {
				throw new ApplicationError("Field " + this.proxyPwdField
						+ " not found in service context at run time. This is required for a rest call through proxy.");
			}
			pwd = value.toString();
		}
		return new MyAuthenticator(user, pwd);
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		BufferedReader reader = null;
		StringBuilder sbf = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			int ch;
			while ((ch = reader.read()) > -1) {
				sbf.append((char) ch);
			}
			reader.close();
			return sbf.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady()
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		this.urlParts = TextUtil.parseToParts(this.urlString);
		if (this.proxyUserName != null && this.proxyUserName.charAt(0) == DOLLAR) {
			this.proxyUserField = this.proxyUserName.substring(1);
		}
		if (this.proxyPassword != null && this.proxyPassword.charAt(0) == DOLLAR) {
			this.proxyPwdField = this.proxyPassword.substring(1);
		}
		if (this.proxyPwdField != null && this.proxyUserField != null) {
			this.authenticator = new MyAuthenticator(this.proxyUserName, this.proxyPassword);
		}
		if (this.contentType.endsWith(JSON)) {
			this.isJson = true;
		} else if (this.contentType.endsWith(XML)) {
			this.isXml = true;
		} else if (this.responseFieldName == null) {
			throw new ApplicationError(
					"responseFieldName is requried for a restClient action that has its content type set to "
							+ this.contentType);
		}
		if (this.urlString != null) {
			this.parsedUrlString = TextUtil.getFieldName(this.urlString);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		/*
		 * mandatory fields
		 */
		count += ctx.checkMandatoryField("urlString", this.urlString);
		count += ctx.checkMandatoryField("restMethod", this.httpMethod);
		count += ctx.checkMandatoryField("contentType", this.contentType);

		/*
		 * mandatory fields for proxy
		 */
		if (this.proxy == null) {
			if (this.proxyPort != 0 || this.proxyUserName != null || this.proxyPassword != null) {
				ctx.addError("proxy port, user, password are not relavant when proxy is not specified.");
				count++;
			}
		} else if (this.proxyPort == 0) {
			ctx.addError("proxyPort is required if proxy is to be used.");
			count++;
		}

		/*
		 * handling response
		 */
		boolean responseIsJson = false;
		boolean responseIsXml = false;
		if (this.contentType != null) {
			responseIsJson = this.contentType.endsWith(JSON);
			responseIsXml = this.contentType.endsWith(XML);
		}
		/*
		 * we are not ready with input/output specification for xml
		 */
		if (responseIsXml) {
			if (this.requestData != null || this.responseData != null) {
				ctx.addError(
						"We are yet to implement input/output data specification for xml. Please manage on your own with requestFieldName/responseFieldName for the time being.");
				count++;
			}
		}
		/*
		 * fieldName or output data, but not both..
		 */
		if (this.responseFieldName == null) {
			if (responseIsJson == false && responseIsXml == false) {
				ctx.addError("You should specify responseFieldName when content-type is non-json and non-xml.");
				count++;
			}
		} else {
			if (this.responseData != null) {
				ctx.addError(
						"You should specify either responseFieldName or responseData, but not both, to handle the response.");
				count++;
			}
		}
		return count;
	}
}

/**
 * authenticator instance used only in this class.
 *
 */
class MyAuthenticator extends Authenticator {
	private final PasswordAuthentication auth;

	MyAuthenticator(String userName, String userPassword) {
		this.auth = new PasswordAuthentication(userName, userPassword.toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.net.Authenticator#getPasswordAuthentication()
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return this.auth;
	}
}