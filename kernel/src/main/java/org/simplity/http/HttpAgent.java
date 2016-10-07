/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
package org.simplity.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * servlet that is the agent to expose all services on http. receives requests
 * for service and responds back with response given by the service. As you can
 * see, this class can be easily converted into a servlet and configured to be
 * the target URL for client requests. However, there would be several
 * infrastructure related issues to be addressed in this layer. Hence we
 * recommend that you take care of all that for your project, and call this
 * class, rather than we making provisions for all of that
 *
 * @author simplity.org
 *
 */
public class HttpAgent {
	/*
	 * session parameter name with which user token is saved. This token is the
	 * name under which our global parameters are saved. This indirection is
	 * used to keep flexibility allow a client to haev multiple active sessions.
	 * sessions, we make this a set of tokens.
	 */
	private static final String GET = "GET";
	private static final String TAG = "<htppTrace at=\"";
	private static final String ELAPSED = "\" elapsed=\"";
	private static final String SERVICE = "\" serviceName=\"";
	private static final String USER = "\" userId=\"";
	private static final String TAG_CLOSE = "\n]]>\n</httpTrace>";
	private static final String CLOSE = "\" >\n<![CDATA[\n";
	private static final String TRACE = "trace";

	private static final Logger myLogger = Logger.getLogger(TRACE);
	private static final FormattedMessage INTERNAL_ERROR = new FormattedMessage(
			"internalError",
			MessageType.ERROR,
			"We are sorry. There was an internal error on server. Support team has been notified.");
	private static final FormattedMessage NO_LOGIN = new FormattedMessage(
			"notLoggedIn",
			MessageType.ERROR,
			"You are not logged into the server, or server may have logged-you out as a safety measure after a period of no activity.");
	private static final FormattedMessage DATA_ERROR = new FormattedMessage(
			"invalidDataFormat",
			MessageType.ERROR,
			"Data text sent from client is not formatted properly. Unable to extract data from the text.");
	private static final FormattedMessage NO_SERVICE = new FormattedMessage(
			"noService", MessageType.ERROR,
			"No service name was specified for this request.");

	/**
	 * parameter name with which userId is to be saved in session. made this
	 * public filters to observe this convention. Value in session with this
	 * name implies that the user has logged-in. HttpAgent does not serve unless
	 * there is a userId associated with the session
	 */
	public static final String SESSION_NAME_FOR_USER_ID = "_userIdInSession";

	private static final String SESSION_NAME_FOR_MAP = "userSessionMap";

	/**
	 * set at set-up time in case we are in development mode, and we use a
	 * default dummyLogin id
	 */
	private static Value autoLoginUserId;

	/**
	 * serve this service. Main entrance to the server from an http client.
	 *
	 * @param req
	 *            http request
	 * @param resp
	 *            http response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             IO exception
	 *
	 */
	public static void serve(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		/*
		 * serviceName and CSRF headers are expected in header in AJAX mode with
		 * post
		 */
		String serviceName = req.getHeader(ServiceProtocol.SERVICE_NAME);
		HttpSession session = req.getSession(true);
		boolean isGet = GET.equals(req.getMethod());

		/*
		 * serviceName is a parameter in GET mode, and CSRF would be in session
		 */
		if (isGet) {
			if (serviceName == null) {
				serviceName = req.getParameter(ServiceProtocol.SERVICE_NAME);
			}
		}

		long startedAt = new Date().getTime();
		long elapsed = 0;
		Value userId = null;
		ServiceData outData = null;
		Tracer.startAccumulation();
		Tracer.trace("Request received for service " + serviceName);
		FormattedMessage message = null;
		int errorStatus = ServiceProtocol.STATUS_FAILED;
		/*
		 * let us earnestly try to serve now :-) this do{} is not a loop, but a
		 * block that helps in handling errors in an elegant way
		 */
		do {
			try {
				if (serviceName == null) {
					message = NO_SERVICE;
					break;
				}

				ServiceData inData = createServiceData(session);
				if (inData == null) {
					message = NO_LOGIN;
					errorStatus = ServiceProtocol.STATUS_NO_LOGIN;
					break;
				}
				userId = inData.getUserId();
				String payLoad = null;
				if (isGet) {
					payLoad = queryToJson(req);
				} else {
					/*
					 * try-catch specifically for any possible I/O errors
					 */
					try {
						payLoad = readInput(req);
					} catch (Exception e) {
						message = DATA_ERROR;
						break;
					}
				}
				inData.setPayLoad(payLoad);
				inData.setServiceName(serviceName);
				outData = ServiceAgent.getAgent().executeService(inData);
				/*
				 * by our convention, server may send data in inData to be set
				 * to session
				 */
				if (outData.hasErrors() == false) {
					setSessionData(session, outData);
				}
			} catch (Exception e) {
				Tracer.trace(e, "Internal error");
				message = INTERNAL_ERROR;
			}
		} while (false);

		elapsed = new Date().getTime() - startedAt;
		resp.setHeader(ServiceProtocol.SERVICE_EXECUTION_TIME, elapsed + "");
		String response = null;
		if (outData == null) {
			/*
			 * we got error
			 */
			Tracer.trace("Error on web teir : "
					+ (message == null ? "Unknow error" : message.text));
			FormattedMessage[] messages = { message };
			response = JsonUtil.toJson(messages);
			resp.setStatus(errorStatus);
		} else {
			if (outData.hasErrors()) {
				Tracer.trace("Service returned with errors");
				resp.setStatus(ServiceProtocol.STATUS_FAILED);
				response = JsonUtil.toJson(outData.getMessages());
			} else {
				/*
				 * all OK
				 */
				resp.setStatus(ServiceProtocol.STATUS_OK);
				response = outData.getPayLoad();
				Tracer.trace("Service has no errors and has "
						+ (response == null ? "no " : (response.length())
								+ " chars ") + " payload");
			}
		}
		if (response != null) {
			ServletOutputStream out = resp.getOutputStream();
			out.print(response);
			out.close();
		}
		String trace = Tracer.stopAccumulation();
		String log = TAG + startedAt + ELAPSED + elapsed + SERVICE
				+ serviceName + USER + userId + CLOSE + trace + TAG_CLOSE;
		myLogger.info(log);
	}

	/**
	 * This method can be used in two scenarios.
	 *
	 * 1. SSO or some other login mechanism is used that is outside of this
	 * application. In this case, the registered loginService() inside this
	 * application does not do any authentication, but it will extract user-data
	 * for this session. It may also keep track of the SSO token etc..
	 *
	 * 2. login is designed as service within this application. In this case,
	 * securityToekn is the password supplied by the user. registered
	 * loginService authenticates this password before extracting use data.
	 *
	 * In case you use SSO, or any other common utility outside this
	 * application, we assume that you have authenticated the user, and we will
	 * be calling the spec
	 *
	 * @param loginId
	 *            Login ID
	 * @param securityToken
	 *            Security Token, optional.
	 * @param session
	 *            for accessing session
	 * @return token for this session that needs to be supplied for any service
	 *         under this sessions. null implies that we could not login.
	 */
	public static String login(String loginId, String securityToken,
			HttpSession session) {
		/*
		 * ask serviceAgent to login.
		 */
		ServiceData inData = new ServiceData();
		inData.put(ServiceProtocol.USER_ID, Value.newTextValue(loginId));
		if (securityToken != null) {
			inData.put(ServiceProtocol.USER_TOKEN,
					Value.newTextValue(securityToken));
		}
		ServiceData outData = ServiceAgent.getAgent().login(inData);
		if (outData == null) {
			return null;
		}
		Value userId = outData.getUserId();
		if (userId == null) {
			/*
			 * possible that loginService is a custom one. Let us try to fish in
			 * the Map
			 */
			Object uid = outData.get(ServiceProtocol.USER_ID);
			if (uid == null) {
				Tracer.trace("Server came back with no userId and hence HttpAgent assumes that the login did not succeed");
				return null;
			}
			if (uid instanceof Value) {
				userId = (Value) uid;
			} else {
				userId = Value.parseObject(uid);
			}
		}

		/*
		 * create and save new session data
		 */
		newSession(session, userId);
		Tracer.trace("Login succeeded for loginId " + loginId);
		String result = outData.getPayLoad();
		if (result == null || result.length() == 0) {
			result = "{}";
		}
		return result;
	}

	/**
	 * logout after executing desired service
	 *
	 * @param session
	 *            http session
	 * @param timedOut
	 *            is this triggered on session time-out? false means a specific
	 *            logout request from user
	 *
	 */
	public static void logout(HttpSession session, boolean timedOut) {
		if (session == null) {
			return;
		}
		ServiceData inData = createServiceData(session);
		if (inData == null) {
			Tracer.trace("No active session found, and hence logout not called");
			return;
		}
		if (timedOut) {
			inData.put(ServiceProtocol.TIMED_OUT, Value.VALUE_TRUE);
		}

		ServiceAgent.getAgent().logout(inData);

		removeSession(session);
	}

	/**
	 * read input stream into a string
	 *
	 * @param req
	 * @throws IOException
	 */
	private static String readInput(HttpServletRequest req) throws IOException {
		BufferedReader reader = null;
		StringBuilder sbf = new StringBuilder();
		try {
			reader = req.getReader();
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

	private static String queryToJson(HttpServletRequest req) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		Map<String, String[]> fields = req.getParameterMap();
		if (fields != null) {
			for (Map.Entry<String, String[]> entry : fields.entrySet()) {
				writer.key(entry.getKey()).value(entry.getValue()[0]);
			}
		}
		writer.endObject();
		return writer.toString();
	}

	/**
	 * @param autoUserId
	 *            in case login is disabled and a default loginId is to be used
	 *            for all services
	 */
	public static void setUp(Value autoUserId) {
		autoLoginUserId = autoUserId;

	}

	/**
	 *
	 * @param session
	 * @param token
	 * @param inData
	 * @return
	 */
	private static ServiceData createServiceData(HttpSession session) {
		Value userId = (Value) session.getAttribute(SESSION_NAME_FOR_USER_ID);
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
		.getAttribute(SESSION_NAME_FOR_MAP);
		if (userId == null) {
			Tracer.trace("Request by non-logged-in session detected.");
			if (autoLoginUserId == null) {
				Tracer.trace("Login is required.");
				return null;
			}
			userId = autoLoginUserId;
			Tracer.trace("User " + userId + " auto logged-in");
			sessionData = newSession(session, userId);
		}

		if (sessionData == null) {
			throw new ApplicationError(
					"Unexpected situation. UserId is located in session, but not map");
		}
		ServiceData data = new ServiceData();
		data.setUserId(userId);
		for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
			data.put(entry.getKey(), entry.getValue());
		}

		return data;
	}

	/**
	 *
	 * @param session
	 * @param token
	 *            - future use
	 */
	private static void removeSession(HttpSession session) {
		Object obj = session.getAttribute(SESSION_NAME_FOR_USER_ID);
		if (obj == null) {
			Tracer.trace("Remove session : No sesion to remove");
			return;
		}
		Tracer.trace("Session removed for " + obj);
		session.removeAttribute(SESSION_NAME_FOR_USER_ID);
		session.removeAttribute(SESSION_NAME_FOR_MAP);
	}

	/**
	 *
	 * @param session
	 * @param data
	 */
	private static void setSessionData(HttpSession session, ServiceData data) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
		.getAttribute(SESSION_NAME_FOR_MAP);

		if (sessionData == null) {
			Tracer.trace("Unexpected situation. setSession invoked with no active session. Action ignored");
		} else {
			for (String key : data.getFieldNames()) {
				sessionData.put(key, data.get(key));
			}
		}
	}

	/**
	 * create a new session for this user. To be used by filter/SSO etc.. if
	 * there is no specific login process for Simplity application. Note; If a
	 * loginService is to be executed, then caller should use login() instead of
	 * this method
	 *
	 * @param session
	 * @param userId
	 * @return map of global fields that is maintained by Simplity. Any
	 *         parameter in this map is made available to every service request
	 */
	public static Map<String, Object> newSession(HttpSession session,
			Value userId) {

		Map<String, Object> sessionData = new HashMap<String, Object>();
		session.setAttribute(SESSION_NAME_FOR_USER_ID, userId);
		session.setAttribute(SESSION_NAME_FOR_MAP, sessionData);
		Tracer.trace("New session data created for " + userId);
		return sessionData;
	}
}