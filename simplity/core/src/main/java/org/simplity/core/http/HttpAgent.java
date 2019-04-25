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

package org.simplity.core.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.AppManager;
import org.simplity.core.app.Application;
import org.simplity.core.app.IApp;
import org.simplity.core.app.IServiceRequest;
import org.simplity.core.app.IServiceResponse;
import org.simplity.core.app.ServiceResult;
import org.simplity.core.app.StandInApp;
import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.app.internal.ServiceResponse;
import org.simplity.core.util.IoUtil;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent (or controller) that receives any requests for a service over HTTP.
 * Each application should have a concrete class that extends this class with
 * app specific code put into the abstract methods
 *
 *
 * This has the init() to bootstrap Simplity application as well.
 *
 * @author simplity.org
 *
 */
public abstract class HttpAgent extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static final Logger logger = LoggerFactory.getLogger(HttpAgent.class);

	private static final String XML_CONTENT = "application/xml";
	private static final String JSON_CONTENT = "application/json";
	private static final String AUTH_HEADER = "Authorization";
	private static final String USER_ID = AppConventions.Name.USER_ID;
	private static final String[] HDR_NAMES = { "Access-Control-Allow-Methods", "Access-Control-Allow-Headers",
			"Access-Control-Max-Age", "Access-Control-Allow-Origin", "Connection", "Cache-Control", "Expires" };
	private static final String[] HDR_TEXTS = { "POST, GET, OPTIONS", "authorization,content-type", "1728000", "*",
			"Keep-Alive", "no-cache, no-store, must-revalidate", "0" };
	// private static final String REQ_ORIGIN = "Origin";
	private static final int STATUS_ALL_OK = 200;
	private static final int STATUS_AUTH_REQUIRED = 401;
	private static final int STATUS_METHOD_NOT_ALLOWED = 405;

	/**
	 * path-to-service mappings
	 */
	protected Paths mappedPaths;
	/**
	 * in case we are to extract some data from cookies,header and session..
	 */
	protected Set<String> cookieFields;
	/**
	 * standard header fields to be extracted
	 */
	protected String[] headerFields = null;
	/**
	 * standard session fields to be extracted
	 */
	protected String[] sessionFields = null;

	/**
	 * objects from requestAttributes to be extracted
	 */
	protected String[] requestAttributes = null;

	/**
	 * root path that is mapped for this REST module. e.g /api/ for the url
	 * http://a.b.c/site/api/customer/{custId}
	 */
	protected String rootFolder = null;

	/**
	 * map of service names used by client to the one used by server.
	 */
	protected JSONObject serviceAliases = null;

	/**
	 * login service name
	 */
	protected String loginServiceName;

	/**
	 * logout service name
	 */
	protected String logoutServiceName;
	/**
	 * length of root folder is all that we use during execution. keeping it
	 * rather than keep checking it.
	 */
	private int rootFolderLength = 0;

	/**
	 * use streaming payload if the service layer is inside the same JVM as
	 * this. if this is set to false, request payload is read into object before
	 * passing to the service layer. Similarly, service layer passes the
	 * response object back which is written out to response stream
	 */
	protected boolean useStreamingPayload = false;

	/**
	 * http client can either specify service name explicitly (non-rest) or can
	 * use the REST sway of specifying resource-specific-path and operation
	 */
	protected boolean cleintCanSpecifyServiceName = true;

	/**
	 * during development, we may want to simplify it by not requiring a login
	 */
	protected boolean allowGuests = false;
	/**
	 * many a times, developers do not notice the error message thrown by
	 * set-up. They look at only the error message thrown by service request and
	 * get to a wrong path for debugging. Better to keep throwing the config
	 * error each time. This field captures the error, if any
	 */
	private String errorMessage;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletContext ctx = this.getServletContext();
		this.rootFolder = ctx.getInitParameter(HttpConventions.Resource.REST_ROOT);
		if (this.rootFolder != null) {
			this.rootFolderLength = this.rootFolder.length();
		}
		String resourceRoot = ctx.getInitParameter(AppConventions.Name.RESOURCE_ROOT);
		if (resourceRoot == null) {
			logger.warn("{} is not set to identify root folder for components. Default vale of {} is tried.",
					AppConventions.Name.RESOURCE_ROOT, AppConventions.Value.RESOURCE_ROOT_DEFAULT);
			resourceRoot = AppConventions.Value.RESOURCE_ROOT_DEFAULT;
		}

		this.bootstrap(resourceRoot);
		this.loadPaths(resourceRoot);
		this.loadServiceAliases(resourceRoot);
		this.setHttpParams(ctx);
		this.appSpecificInit(ctx);
	}

	@Override
	public void destroy() {
		super.destroy();
		AppManager.removeApp(AppManager.getApp().getAppId());
	}

	/**
	 * we expect OPTIONS method only as a pre-flight request in a CORS
	 * environment. We have a ready response
	 */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		for (int i = 0; i < HDR_NAMES.length; i++) {
			resp.setHeader(HDR_NAMES[i], HDR_TEXTS[i]);
		}
		resp.setStatus(STATUS_ALL_OK);
	}

	/*
	 * we allow get, post and options. Nothing else
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(STATUS_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(STATUS_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(STATUS_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(STATUS_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.serve(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.serve(req, resp);
	}

	/**
	 * serve an in-bound request.
	 *
	 * @param req
	 *            http request
	 * @param resp
	 *            http response
	 * @throws IOException
	 *             IO exception
	 *
	 */
	public void serve(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (this.errorMessage != null) {
			/*
			 * app was not configured properly
			 */
			logger.error(this.errorMessage);
			return;
		}
		String serviceName = null;
		long bigin = System.currentTimeMillis();
		String ct = req.getContentType();
		boolean isXml = ct != null && ct.indexOf("xml") != -1;
		if (isXml) {
			resp.setContentType(XML_CONTENT);
		} else {
			resp.setContentType(JSON_CONTENT);
		}

		try (InputStream ins = req.getInputStream(); Writer writer = new PrintWriter(resp.getOutputStream())) {
			try {
				this.setResponseHeaders(resp);
				IApp iapp = AppManager.getApp();
				if (iapp == null || iapp instanceof Application == false) {
					String msg = "No service app is running. All requests are responded back as internal error";
					logger.error(msg);
					this.respondWithError(resp, writer, msg);
					return;
				}

				Application app = (Application) iapp;
				/*
				 * data from non-payload sources, like header and cookies is in
				 * this map
				 */
				Map<String, Object> fields = new HashMap<>();
				/*
				 * path-data is extracted during path-parsing for serviceName
				 */
				serviceName = this.getServiceName(req, fields);
				if (serviceName == null) {
					logger.warn("No service name is inferred from request.");
					this.respondWithError(resp, writer, "Sorry, that request is beyond us!!");
					return;
				}
				this.mineFields(req, fields);
				String authToken = this.getSessionFields(req, fields);
				if (authToken == null) {
					if (this.allowGuests == false) {
						if (this.loginServiceName == null || this.loginServiceName.equals(serviceName) == false) {
							resp.setStatus(STATUS_AUTH_REQUIRED);
							this.respondWithError(resp, writer,
									"We do not serve guests. Please come back with authenticated token");
							return;
						}
					}
				}
				/*
				 * no washing dirty linen in public. try-catch to ensure that we
				 * ALWAYS return in a controlled manner
				 */
				IServiceRequest request = new ServiceRequest(serviceName, fields, ins, isXml);
				IServiceResponse response = new ServiceResponse(writer, isXml);
				Object userId = fields.get(USER_ID);
				if (userId != null) {
					request.setUser(app.createAppUser(userId.toString()));
				}
				/*
				 * app specific code to copy anything from client-layer to
				 * request as well as set anything to response
				 */
				boolean okToProceed = this.prepareRequestAndResponse(serviceName, req, resp, request, response, fields);

				if (okToProceed) {
					app.serve(request, response);
					/*
					 * app-specific hook to do anything before responding back
					 */
					this.postProcess(req, resp, request, response);
				}

				ServiceResult result = response.getServiceResult();
				logger.info("Service {} ended with result={} ", serviceName, result);

				if (result != ServiceResult.ALL_OK) {
					this.respondWithError(resp, writer, "Sorry, your request failed to execute : " + result);
					return;
				}
				logger.info("Server-layer reported {} ms as time taken to execute service {}",
						response.getExecutionTime(), serviceName);
				if (this.loginServiceName != null && this.loginServiceName.equals(serviceName)) {
					authToken = this.createSession(req, response.getSessionFields());
					this.respondToLogin(resp, writer, authToken);
				} else if (this.logoutServiceName != null && this.logoutServiceName.equals(serviceName)) {
					this.destroySession(req, authToken);
				}
			} catch (Exception e) {
				String msg = "Error occured while serving the request";
				logger.error(msg, e);
				this.respondWithError(resp, writer, msg);

			} finally {
				logger.info("Http server took {} ms to deliver service {}", System.currentTimeMillis() - bigin,
						serviceName);
			}
		}
	}

	/**
	 * response headers are typically set using filters. However, to cater to
	 * different deployment scenarios, we provide this method that the
	 * app-specific implementation can over-ride
	 *
	 * @param resp
	 */
	protected void setResponseHeaders(HttpServletResponse resp) {
		for (int i = 0; i < HDR_NAMES.length; i++) {
			resp.setHeader(HDR_NAMES[i], HDR_TEXTS[i]);
		}
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setDateHeader("Expires", 0);
	}

	/**
	 * app-specific implementations can use templates to respond back
	 *
	 * @param resp
	 * @param message
	 *            error message
	 * @param writer
	 *            response writer used for this request
	 * @throws IOException
	 */
	protected void respondWithError(HttpServletResponse resp, Writer writer, String message) throws IOException {
		resp.setStatus(500);
		writer.write("{\"status\":\"error\",\"message\":\"");
		writer.write(message);
		writer.write("\"}");

	}

	/**
	 * default session management is to save it under app-scoped map with token
	 * as the key. May be over-ridden by app-specific controller
	 *
	 * @param req
	 * @param fields
	 * @return
	 */
	protected String getSessionFields(HttpServletRequest req, Map<String, Object> fields) {
		String token = req.getHeader(AUTH_HEADER);
		if (token == null) {
			logger.info("No auth token recd in header {}", AUTH_HEADER);
			return null;
		}
		ServletContext ctx = req.getServletContext();
		Object obj = ctx.getAttribute(token);
		if (obj == null) {
			logger.info("Auth token recd in header={} this is not valid", token);
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) obj;
		Object userId = sessionData.get(USER_ID);
		if (userId == null) {
			logger.info("Session data is found for token {} but userId is not found with name {}. Session invalidated.",
					token, USER_ID);
			ctx.removeAttribute(token);
			return null;
		}
		fields.put(USER_ID, userId);

		if (this.sessionFields != null) {
			for (String name : this.sessionFields) {
				Object value = sessionData.get(name);
				if (value != null) {
					fields.put(name, value);
				}
			}
		}
		return token;
	}

	/**
	 * clients may send data via query-string, cookies and header fields
	 *
	 * @param req
	 * @param fields
	 */
	private void mineFields(HttpServletRequest req, Map<String, Object> fields) {
		String qry = req.getQueryString();
		if (qry != null) {
			for (String part : qry.split("&")) {
				String[] pair = part.split("=");
				String val;
				if (pair.length == 1) {
					val = "";
				} else {
					val = this.decode(pair[1]);
				}
				fields.put(pair[0], val);
			}
		}

		if (this.cookieFields != null) {
			Cookie[] cookies = req.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					String name = cookie.getName();
					if (this.cookieFields.contains(name)) {
						fields.put(name, cookie.getValue());
					}
				}
			}
		}

		if (this.headerFields != null) {
			for (String name : this.headerFields) {
				String value = req.getHeader(name);
				if (value != null) {
					fields.put(name, value);
				}
			}
		}

		if (this.requestAttributes != null) {
			for (String name : this.requestAttributes) {
				Object value = req.getAttribute(name);
				if (value != null) {
					fields.put(name, value);
				}
			}
		}
	}

	private String decode(String text) {
		try {
			return URLDecoder.decode(text, AppConventions.CHAR_ENCODING);
		} catch (UnsupportedEncodingException e) {
			logger.error("How come {} is not a valid encoding?", AppConventions.CHAR_ENCODING);
			/*
			 * we do know that this is supported. so, this is unreachable code.
			 */
			return text;
		}
	}

	private String getServiceName(HttpServletRequest req, Map<String, Object> fields) {
		String serviceName = null;
		if (this.cleintCanSpecifyServiceName) {
			serviceName = req.getHeader(HttpConventions.FieldNames.SERVICE_NAME);
			if (serviceName == null) {
				serviceName = (String) fields.get(HttpConventions.FieldNames.SERVICE_NAME);
			}
			if (serviceName != null) {
				logger.info("Service name = {} extracted from header/query", serviceName);
				if (this.serviceAliases != null) {
					Object alias = this.serviceAliases.opt(serviceName);
					if (alias != null) {
						logger.info("client-requested service name {} is mapped {} ", serviceName, alias);
						serviceName = alias.toString();
					}
				}
				return serviceName;
			}
		}

		if (this.mappedPaths == null) {
			if (this.cleintCanSpecifyServiceName) {
				logger.info(
						"Request header has no service name, and we are not set-up to translate path to serviceName");
			} else {
				logger.info(
						"This instance is not set-up properly. It does not allow clients to specify srevice names, nor has it configured RSET paths");
			}
			return null;
		}

		String uri = this.decode(req.getRequestURI());
		/*
		 * assuming http://www.simplity.org:8020/app1/subapp/a/b/c?a=b&c=d uri
		 * would be set to /app1/subapp/a/b/c we need to get a/b/c as RESTful
		 * path
		 */

		int idx = req.getContextPath().length() + this.rootFolderLength;
		String path = uri.substring(idx);
		serviceName = this.mappedPaths.parse(path, req.getMethod(), fields);
		logger.info("uri {} has the REST path {}. This is mapped to service {} ", uri, path, serviceName);
		return serviceName;
	}

	private void loadPaths(String resourceRoot) {
		String resName = resourceRoot + HttpConventions.Resource.REST_PATHS;
		String text = IoUtil.readResource(resName);
		if (text == null) {
			logger.error("Resource {} could not be read. Paths will not be mapped to service names for REST calls.",
					resName);
			return;
		}
		this.mappedPaths = new Paths();
		this.mappedPaths.addPaths(new JSONObject(text));
	}

	private void loadServiceAliases(String resourceRoot) {
		String resName = resourceRoot + HttpConventions.Resource.SERVICE_ALIASES;
		String text = IoUtil.readResource(resName);
		if (text == null) {
			logger.warn("Service aliases resource {} could not be read. No service alaises set for this app.", resName);
			return;
		}

		try {
			JSONObject json = new JSONObject(text);
			this.serviceAliases = json;
			logger.info("{} service aliases loaded.", json.length());
		} catch (Exception e) {
			logger.error("Contents of resource {} is not a valid json. Error : {}", resName, e.getMessage());
		}
	}

	private void bootstrap(String resourceRoot) {
		Map<String, String> params = new HashMap<>();
		params.put(AppConventions.Name.RESOURCE_ROOT, resourceRoot);
		List<String> messages = new ArrayList<>();
		IApp app = new Application();
		boolean allOk = app.openShop(params, messages);
		if (!allOk) {
			StringBuilder sbf = new StringBuilder("Application configuration failed. Error message:\n");
			if (messages.size() == 0) {
				sbf.append(
						"application.xml is probably missing or has syntax errors. Please verify that you have set resourceRoot to the right folder/path and that application.xml is located there");
			} else {
				for (String msg : messages) {
					sbf.append(msg).append('\n');
				}
			}
			String error = sbf.toString();
			logger.error(error);
			app = new StandInApp(error);
		}
		AppManager.setDefaultApp(app);
	}

	/**
	 * @param ctx
	 */
	private void setHttpParams(ServletContext ctx) {
		String text = ctx.getInitParameter(HttpConventions.Http.COOKIES);
		if (text == null) {
			logger.info("{} is not set. No cookie will be used.", HttpConventions.Http.COOKIES);
		} else {
			logger.info("{} is used as set of cookie name/s to be extracted as fields for each request.", text);
			this.cookieFields = new HashSet<>();
			for (String fieldName : text.split(HttpConventions.FIELD_NAME_SEPARATOR)) {
				this.cookieFields.add(fieldName);
			}
		}

		text = ctx.getInitParameter(HttpConventions.Http.HEADERS);
		if (text == null) {
			logger.info("{} is not set. No header fields will be used as data.", HttpConventions.Http.HEADERS);
		} else {
			logger.info("{} is/are the header fields to be extracted as data.", text);
			this.headerFields = text.split(HttpConventions.FIELD_NAME_SEPARATOR);
		}

		text = ctx.getInitParameter(HttpConventions.Http.SESSION_FIELDS);
		if (text == null) {
			logger.info("{} is not set. No session fields will be used as data.", HttpConventions.Http.SESSION_FIELDS);
		} else {
			logger.info("{} is/are the session fields to be extracted as data.", text);
			this.sessionFields = text.split(HttpConventions.FIELD_NAME_SEPARATOR);
		}

		text = ctx.getInitParameter(HttpConventions.Http.REQUEST_ATTRIBUTES);
		if (text == null) {
			logger.info("{} is not set. No attributes from HttpSevletRequest will be used as data.",
					HttpConventions.Http.REQUEST_ATTRIBUTES);
		} else {
			logger.info("{} is/are the request attributes that will be used as input data", text);
			this.requestAttributes = text.split(HttpConventions.FIELD_NAME_SEPARATOR);
		}
		text = ctx.getInitParameter(HttpConventions.FieldNames.LOGIN_SERVICE);
		if (text == null) {
			logger.info(
					"{} is not set. Login process is not managed by the app. We assume that it is handled outside of the app.",
					HttpConventions.FieldNames.LOGIN_SERVICE);
		} else {
			logger.info("{} is the login service", text);
			this.loginServiceName = text;
		}
		text = ctx.getInitParameter(HttpConventions.FieldNames.LOGOUT_SERVICE);
		if (text == null) {
			logger.info(
					"{} is not set. Login process is not managed by the app. We assume that it is handled outside of the app.",
					HttpConventions.FieldNames.LOGOUT_SERVICE);
		} else {
			logger.info("{} is the logout service", text);
			this.logoutServiceName = text;
		}
	}

	/**
	 * default is to use HttpSession that is created in need basis, and saved
	 * with a map in the app context. App-specific controller can over-ride this
	 * to use mem-cache solutions like redis
	 *
	 * @param req
	 * @param sessionData
	 * @return
	 */
	protected String createSession(HttpServletRequest req, Object sessionData) {
		String token = UUID.randomUUID().toString();
		req.getServletContext().setAttribute(token, sessionData);
		return token;
	}

	/**
	 * @param req
	 */
	protected void destroySession(HttpServletRequest req, String token) {
		req.getServletContext().removeAttribute(token);
	}

	/**
	 *
	 * @param resp
	 * @param writer
	 * @param token
	 * @throws IOException
	 */
	protected void respondToLogin(HttpServletResponse resp, Writer writer, String token) throws IOException {
		writer.write("{\"status\":\"ok\", \"token\":\"");
		writer.write(token.replace("\"", "\"\""));
		writer.write("\"}");
	}

	/**
	 * Called before requesting for this service from server agent. Typically,
	 * appUser and fields are set. clientContext may also be set. Refer to
	 * <code>ExampleClient</code>
	 *
	 * @param req
	 *            service request
	 * @param resp
	 *            service response
	 * @param request
	 *            http request
	 * @param response
	 *            http response
	 * @param fields
	 *            fields picked-up from all request/session as per standard
	 * @return true if all ok, nad service should be called. false in case some
	 *         error is detected.
	 */
	protected abstract boolean prepareRequestAndResponse(String serviceName, HttpServletRequest req,
			HttpServletResponse resp, IServiceRequest request, IServiceResponse response, Map<String, Object> fields);

	/**
	 * application specific functionality in web-layer after service layer
	 * returns with success.
	 *
	 * @param httpRequest
	 * @param httpResponse
	 * @param serviceRequest
	 * @param serviceResponse
	 */
	protected abstract void postProcess(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			IServiceRequest serviceRequest, IServiceResponse serviceResponse);

	/**
	 * app specific aspects to be handled at init() time
	 *
	 * @param ctx
	 */
	protected abstract void appSpecificInit(ServletContext ctx);
}
