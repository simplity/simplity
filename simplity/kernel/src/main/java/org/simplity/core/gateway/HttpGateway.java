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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.IRequestReader;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.app.internal.JsonReqReader;
import org.simplity.core.app.internal.JsonRespWriter;
import org.simplity.core.app.internal.XmlReqReader;
import org.simplity.core.app.internal.XmlRespWriter;
import org.simplity.core.service.ExternalService;
import org.simplity.core.service.InputData;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.IoUtil;
import org.simplity.core.util.XmlUtil;
import org.simplity.core.value.Value;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Gateway to make HTTP requests for the specified URL.
 *
 * @author simplity.org
 *
 */
public class HttpGateway extends ServiceGateway {
	protected static final Logger logger = LoggerFactory.getLogger(HttpGateway.class);
	static final String DEFAULT_METHOD = "POST";

	/**
	 * base url of the server. for example https://www.simplity.org/thisApp/
	 * value of path received from client requests to be appended to this base
	 * url to make a connection
	 */
	String baseUrl;

	/**
	 * application/json, application/xml, and text/html are the common ones.
	 */
	String contentType;

	/**
	 *
	 */
	String method = DEFAULT_METHOD;
	/**
	 * Proxy url
	 */
	String proxyHostName;

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

	private boolean contentIsXml;
	/**
	 * instantiated in getReady() for performance
	 */
	private Authenticator authenticator;
	/**
	 * if the service name is to be sent as Http Header. null if it is not to be
	 * sent as part of header
	 */
	String headerNameForService;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Gateway#getAssistant(java.lang.String,
	 * org.simplity.service.ServiceContext)
	 */
	@Override
	public HttpGateway.Assistant getAssistant(String serviceName, ServiceContext ctx) {
		return new Assistant(serviceName);
	}

	Authenticator getAuth() {
		return this.authenticator;
	}

	boolean useXml() {
		return this.contentIsXml;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Gateway#getReady()
	 */
	@Override
	public void getReady() {
		if (this.contentType != null && this.contentType.toLowerCase().indexOf("xml") != -1) {
			this.contentIsXml = true;
		}
		if (this.proxyHostName != null) {
			/*
			 * using anonymous class as it is used here and nowhere else
			 */
			this.authenticator = new Authenticator() {
				private PasswordAuthentication auth = new PasswordAuthentication(HttpGateway.this.proxyUserName,
						HttpGateway.this.proxyPassword.toCharArray());

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return this.auth;
				}

			};
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Gateway#shutdown()
	 */
	@Override
	public void shutdown() {
		//
	}

	/**
	 * a worker inner-class that re-uses set-up time parameters from its parent
	 * instance and manages state across method invocations with its own
	 * attributes
	 *
	 * @author simplity.org
	 *
	 */
	public class Assistant implements IServiceAssistant {

		private String serviceName;
		/**
		 * path that is to be appended to the base path for making the http
		 * request. This may include query string as well. It does not have
		 * protocol, domain or port.
		 */
		private String path;

		private String methodToUse = HttpGateway.this.method;

		private String[] headerNames;

		private String[] headerValues;

		private String[] cookieNames;

		private String[] cookieValues;

		private HttpURLConnection conn;

		private ExternalService service;

		/**
		 * used internally.
		 */
		Assistant(String serviceName) {
			this.serviceName = serviceName;
			this.service = Application.getActiveInstance().getExternalService(HttpGateway.this.applicationName,
					serviceName);
		}

		/**
		 * append this path to the base url specified for the gateway
		 *
		 * @param path
		 *            only the specific path, possibly with query string, but
		 *            with no protocol, domain or port. this is appended with
		 *            the baseURL for the connection
		 */
		public void setPath(String path) {
			this.checkConnection();
			this.path = path;
		}

		/**
		 * use this method instead of the default method used by the gateway
		 *
		 * @param httpMethod
		 *            valid non-null http method.
		 */
		public void setMethod(String httpMethod) {
			if (httpMethod != null) {
				this.checkConnection();
				this.methodToUse = httpMethod;
			}
		}

		/**
		 * let these headers be sent along with the request please
		 *
		 * @param names
		 *            non-null non-empty array with non-null non-empty names
		 * @param values
		 *            corresponding values. null or empty strings are ok.
		 *
		 */
		public void setHeaders(String[] names, String[] values) {
			this.checkConnection();
			if (names == null || names.length == 0) {
				return;
			}

			if (values == null || values.length != names.length) {
				throw new ApplicationError("setHeader called with inconsistent names and values");
			}
			this.headerNames = names;
			this.headerValues = values;
		}

		/**
		 * let these headers be sent along with the request please
		 *
		 * @param names
		 *            non-null non-empty array with non-null non-empty names
		 * @param values
		 *            corresponding values. null or empty strings are ok.
		 *
		 */
		public void setHeaders(String[] names, Value[] values) {
			this.checkConnection();
			if (names == null || names.length == 0) {
				return;
			}

			if (values == null || values.length != names.length) {
				throw new ApplicationError("setHeader called with inconsistent names and values");
			}
			this.headerNames = names;
			this.headerValues = this.valueToText(values);
		}

		/**
		 * let these cookies be sent along with the request please
		 *
		 * @param names
		 *            non-null non-empty array with non-null non-empty names
		 * @param values
		 *            corresponding values. null or empty strings are ok.
		 *
		 */
		public void setCookies(String[] names, Value[] values) {
			this.checkConnection();
			if (names == null || names.length == 0) {
				return;
			}

			if (values == null || values.length != names.length) {
				throw new ApplicationError("setCookies called with inconsistent names and values");
			}
			this.cookieNames = names;
			this.cookieValues = this.valueToText(values);
		}

		/**
		 * let these cookies be sent along with the request please
		 *
		 * @param names
		 *            non-null non-empty array with non-null non-empty names
		 * @param values
		 *            corresponding values. null or empty strings are ok.
		 *
		 */
		public void setCookies(String[] names, String[] values) {
			this.checkConnection();
			if (names == null || names.length == 0) {
				return;
			}

			if (values == null || values.length != names.length) {
				throw new ApplicationError("setCookies called with inconsistent names and values");
			}
			this.cookieNames = names;
			this.cookieValues = values;
		}

		/**
		 * send request to the server and get response from it.
		 *
		 * @param ctx
		 *            service context
		 * @return true if all ok. false in case of any error. Error message
		 *         would have been put into service context
		 */
		public boolean sendAndReceive(ServiceContext ctx) {

			/*
			 * avoid the long HttpGateWay.this in each statement to improve
			 * readability
			 */
			HttpGateway gateway = HttpGateway.this;
			String fullPath = gateway.baseUrl;
			if (this.path != null) {
				fullPath += this.path;
			}

			Exception ex = null;
			try {
				URL url = new URL(fullPath);

				/*
				 * get connection
				 */
				Authenticator auth = gateway.getAuth();
				if (auth != null) {

					Proxy proxyCon = new Proxy(Proxy.Type.HTTP,
							new InetSocketAddress(gateway.proxyHostName, gateway.proxyPort));
					Authenticator.setDefault(auth);
					this.conn = (HttpURLConnection) url.openConnection(proxyCon);
				} else {
					this.conn = (HttpURLConnection) url.openConnection();
				}

				/*
				 * despatch request
				 */
				this.prepareToConnect();
				this.setPayload(ctx);
				/*
				 * send request and receive response
				 */
				int status = this.conn.getResponseCode();
				/*
				 * how do you know this is successful? 2xx series is safe
				 */
				if (status < 200 || status > 299) {
					logger.error(
							"Http call failed for application " + gateway.getApplicationName() + " with url " + fullPath
									+ " with status code " + status);
					return false;
				}
				this.getPayload(ctx);
			} catch (Exception e) {
				ex = e;
			}
			logger.error(" Http call failed for application " + gateway.getApplicationName() + " with url " + fullPath,
					ex);
			return false;
		}

		/**
		 * @return http response status code
		 */
		public long getStatus() {
			if (this.conn == null) {
				logger.error("Invalid call to getStatus() before calling serve() method");
				return 0;
			}
			try {
				return this.conn.getResponseCode();
			} catch (IOException e) {
				return 0;
			}
		}

		/**
		 * @param names
		 * @return array of values received for the header field names
		 */
		public String[] getHeaders(String[] names) {
			String[] values = new String[names.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = this.conn.getHeaderField(names[i]);
			}
			return values;
		}

		/**
		 * set all attributes before opening the connection
		 *
		 * @throws ProtocolException
		 */
		private void prepareToConnect() throws ProtocolException {
			if (HttpGateway.this.headerNameForService != null) {
				this.conn.setRequestProperty(HttpGateway.this.headerNameForService, this.serviceName);
			}
			this.conn.setRequestMethod(this.methodToUse);
			this.conn.setRequestProperty("Accept", HttpGateway.this.contentType);
			this.conn.setRequestProperty("Content-Type", HttpGateway.this.contentType);
			if (this.headerNames != null) {
				int i = 0;
				for (String nam : this.headerNames) {
					String value = this.headerValues[i];
					if (value != null) {
						this.conn.setRequestProperty(nam, value);
					}
					i++;
				}
			}

			if (this.cookieNames != null) {
				int i = 0;
				StringBuilder sbf = new StringBuilder();
				for (String nam : this.cookieNames) {
					String value = this.cookieValues[i];
					if (value != null) {
						if (sbf.length() != 0) {
							sbf.append("; ");
						}
						sbf.append(nam).append('=').append(value);
					}
					i++;
				}
				if (sbf.length() != 0) {
					this.conn.setRequestProperty("Cookie: ", sbf.toString());
				}
			}
		}

		private void setPayload(ServiceContext ctx) throws XMLStreamException, IOException {
			OutputData dataTobeSent = this.service.getRequestSpec();
			if (dataTobeSent == null) {
				return;
			}
			this.conn.setDoOutput(true);
			IResponseWriter respWriter = null;
			try (OutputStream os = this.conn.getOutputStream()) {
				if (HttpGateway.this.useXml()) {
					respWriter = new XmlRespWriter(os);
				} else {
					respWriter = new JsonRespWriter(os);
				}
				dataTobeSent.write(respWriter, ctx);
				respWriter.done();
			}
		}

		private void getPayload(ServiceContext ctx) throws IOException {
			InputData dataToBeReceived = this.service.getResponseSpec();
			if (dataToBeReceived == null) {
				return;
			}

			try (InputStream stream = this.conn.getInputStream()) {
				IRequestReader reqReader = null;
				if (HttpGateway.this.useXml()) {
					Document doc = XmlUtil.fromStream(stream);
					reqReader = new XmlReqReader(doc);
				} else {
					String json = IoUtil.streamToText(stream);
					if (json == null || json.isEmpty()) {
						json = "{}";
					}
					reqReader = new JsonReqReader(new JSONObject(json));
				}
				dataToBeReceived.read(reqReader, ctx);
			}
		}

		private void checkConnection() {
			if (this.conn != null) {
				throw new ApplicationError("request settings to be done before executing the service.");
			}

		}

		/**
		 * convert values to text
		 *
		 * @param values
		 * @return
		 */
		private String[] valueToText(Value[] values) {
			String[] result = new String[values.length];
			int i = 0;
			for (Value value : values) {
				if (Value.isNull(value) == false) {
					result[i] = value.toString();
				}
				i++;
			}
			return result;
		}

		@Override
		public boolean execute(ServiceContext ctx) {
			return this.sendAndReceive(ctx);
		}
	}
}
