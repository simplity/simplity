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
package org.simplity.rest;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.PayloadType;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

/**
 * internal servlet that handles all requests coming in as open-api. To be
 * called by the designated servlet that receives request from the web server
 *
 * @author simplity.org
 *
 */
public class RestAgent {
	private static final String UTF = "UTF-8";

	/**
	 * message to be sent to client if there is any internal error
	 */
	private static final FormattedMessage INTERNAL_ERROR = new FormattedMessage("internalError", MessageType.ERROR,
			"We are sorry. There was an internal error on server. Support team has been notified.");

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

	public static void serve(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/json");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setDateHeader("Expires", 0);

		/*
		 * assuming http://www.simplity.org:8020/app1/subapp/a/b/c?a=b&c=d
		 *
		 * we need to get a/b/c as RESTful path
		 */
		String path = URLDecoder.decode(req.getRequestURI(), UTF);
		Tracer.trace("Started serving URI=" + path );
		/*
		 * path now is set to /app1/subapp/a/b/c
		 */

		int idx = req.getContextPath().length();
		/*
		 * contextPath is set to /app1/subapp
		 */
		if(idx >= path.length()){
			/*
			 * this should never happen though..
			 */
			Tracer.trace("oooooops. URI is shorter than contextpath ???");
			path = "";
		}else{
			path = path.substring(idx);
			Tracer.trace("Going to use path=" + path + " for mapping this request to an operation/service");
		}
		/*
		 * parse body and query string into json object
		 */
		JSONObject pathJson = new JSONObject();
		Operation operation = Operations.getServiceSpec(path, req.getMethod().toLowerCase(), pathJson);
		if (operation == null) {
			respondWithError(resp, "We do not serve that request path");
			return;
		}

		/*
		 * using operation specification, get service name and input data
		 */
		JSONObject inJson = new JSONObject();
		List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
		String serviceName = operation.prepareService(req, inJson, pathJson, messages);

		if(messages.size() > 0){
			Tracer.trace("Input data has validation errors. Responding back without clliing the service");
			operation.writeResponse(resp, messages.toArray(new FormattedMessage[0]));
			return;
		}

		Tracer.trace("Request received for service " + serviceName);
		FormattedMessage message = null;
		//TODO : get user ID
		Value userId = Value.newTextValue("100");
		ServiceData inData = new ServiceData(userId, serviceName);
		inData.setPayLoad(inJson.toString());
		/*
		 * discard heavy objects as early as possible
		 */
		pathJson = null;
		inJson = null;

		ServiceData outData = null;
		try {
			outData = ServiceAgent.getAgent().executeService(inData, PayloadType.JSON);
		} catch (ApplicationError e) {
			Application.reportApplicationError(inData, e);
			message = INTERNAL_ERROR;
		} catch (Exception e) {
			Application.reportApplicationError(inData, new ApplicationError(e, "Error while processing request"));
			message = INTERNAL_ERROR;
		}

		if (outData == null) {
			outData = inData;
			if (message == null) {
				message = INTERNAL_ERROR;
			}
			outData.addMessage(message);
		}
		operation.writeResponse(resp, outData);
	}

	/**
	 * @param resp
	 * @param message
	 * @throws IOException
	 */
	public static void respondWithError(HttpServletResponse resp, String message) throws IOException {
		resp.setStatus(500);
		resp.getWriter().write(message);
	}
}
