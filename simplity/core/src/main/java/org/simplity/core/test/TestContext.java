/*
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.simplity.core.app.AppUser;
import org.simplity.core.app.Application;
import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.app.internal.ServiceResponse;
import org.simplity.core.util.JsonUtil;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Context that holds name-value pairs and test results during a test run. */
public class TestContext {
	private static final Logger logger = LoggerFactory.getLogger(TestContext.class);

	private AppUser appUser;
	private Map<String, Object> values = new HashMap<String, Object>();
	private List<TestResult> results = new ArrayList<TestResult>();
	private Application app;
	private int nbrFailed = 0;

	/**
	 * start a context for testing. you MUST start() before firing test();
	 *
	 * @param userId
	 */
	public void start(String userId) {
		this.app = Application.getActiveInstance();
		this.appUser = this.app.createAppUser(userId, null, null);
	}

	/**
	 * add a test result to the context
	 *
	 * @param result
	 */
	public void addResult(TestResult result) {
		if (result.cleared() == false) {
			this.nbrFailed++;
		}
		this.results.add(result);
	}

	/** @return number of failed test */
	public int getNbrFailed() {
		return this.nbrFailed;
	}

	/**
	 * get a report of all tests run.
	 *
	 * @return first row is header. One row per test.
	 */
	public String[][] getReport() {
		int n = this.results.size() + 1;
		String[][] result = new String[n][];
		result[0] = TestResult.HEADR;
		for (int i = 1; i < result.length; i++) {
			result[i] = this.results.get(i - 1).toRow();
		}
		return result;
	}

	/**
	 * save a key-value pair
	 *
	 * @param key
	 * @param value
	 */
	public void setValue(String key, Object value) {
		this.values.put(key, value);
	}

	/**
	 * get a value for the key
	 *
	 * @param key
	 * @return value, or null if no such key was added earlier
	 */
	public Object getValue(String key) {
		return this.values.get(key);
	}

	/**
	 * @param serviceName
	 * @param input
	 *            JSON to service
	 * @return output JSON from service
	 */
	public String runService(String serviceName, String input) {
		if (this.app == null) {
			throw new ApplicationError("TestContext has to be started before running");
		}
		ServiceRequest request = new ServiceRequest(serviceName, null, new JSONObject(input));
		request.setUser(this.appUser);
		ServiceResponse response = new ServiceResponse(false);
		this.app.serve(request, response);
		return response.getPayloadText();
	}

	/**
	 * run a test run
	 *
	 * @param testRun
	 * @return number of failures
	 */
	public int runTest(TestRun testRun) {
		return testRun.run(this);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String root = "c:/repos/simplity/test/WebContent/WEB-INF/comp/";
		Application app = new Application();
		Map<String, String> params = new HashMap<>();
		params.put(AppConventions.Name.RESOURCE_ROOT, root);
		List<String> messages = new ArrayList<>();
		app.openShop(params, messages);
		TestContext ctx = new TestContext();
		ctx.start("100");
		TestRun testRun = app.getTestRun("service.input");
		testRun.run(ctx);
		JSONWriter writer = new JSONWriter();
		writer.object();
		writer.key("report");
		JsonUtil.addObject(writer, ctx.getReport());
		writer.endObject();

		logger.info(writer.toString());
	}
}
