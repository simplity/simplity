/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.core.app;

import java.util.List;
import java.util.Map;

import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.MessageType;
import org.simplity.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * app that is to be used in a situation of no-app. Idea is to respond-back to a
 * service request, more like an echo.
 *
 * @author simplity.org
 *
 */
public class StandInApp implements IApp {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StandInApp.class);

	private String initMessage = "";

	/**
	 *
	 * @param initMessage
	 *            message to be logged whenever a service request is made. This
	 *            is typically the reason why this stand-in app is put to
	 *            service instead of a real app. For example if the designated
	 *            app failed to load. null if the stand-in is by design, and not
	 *            because of an error.
	 */
	public StandInApp(String initMessage) {
		this.initMessage = initMessage;
	}

	@Override
	public String getAppId() {
		return "No-App";
	}

	@Override
	public boolean openShop(Map<String, String> params, List<String> messages) {
		return true;
	}

	@Override
	public void serve(IServiceRequest request, IServiceResponse response) {
		if (this.initMessage == null) {
			logger.info("This is a stand-in app that just echoes the request back to response");
		} else {
			logger.error(this.initMessage);
		}
		String serviceName = request.getServiceName();
		String userName = null;
		AppUser user = request.getUser();
		if (user == null) {
			userName = "no-user";
		} else {
			userName = user.getUserId().toString();
		}
		logger.info("going to echo service {} for user {}", serviceName, userName);
		IResponseWriter writer = response.getPayloadWriter(false);
		writer.setField("serviceName", serviceName);
		writer.setField("userName", userName);
		Map<String, Object> fields = request.getFieldValues();
		writer.beginObject("fields");
		if (fields == null || fields.isEmpty()) {
			logger.info("Input has no fields");
		} else {
			logger.info("Input has {} fields", fields.size());
			for (Map.Entry<String, Object> entry : fields.entrySet()) {
				String fn = entry.getKey();
				Object val = entry.getValue();
				if (val instanceof String) {
					logger.info("{}={}", fn, val.getClass().getName());
				} else {
					logger.info("{}={}", fn, val);
					writer.setField(fn, val);
				}
			}
		}
		writer.endObject();

		ServiceRequest req = (ServiceRequest) request;
		Object payload = req.getPayload();
		if (payload instanceof JSONObject) {
			writer.setObject("payload", payload);
		} else {
			writer.setField("payload-non-json", payload.toString());
		}
		FormattedMessage[] messages = { new FormattedMessage("noApp", MessageType.ERROR,
				"NO application is configured to provide response to service requests") };
		response.setMessages(messages);
		response.setResult(ServiceResult.INTERNAL_ERROR, 0);
	}

	@Override
	public void closeShop() {
		// ok
	}

}
