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

package org.simplity.kernel.app;

import org.simplity.kernel.ApplicationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * static class to manage apps in a JVM
 *
 * @author simplity.org
 *
 */
public class AppManager {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	/**
	 * sole/default app running in this JVM
	 */

	private static IApp defaultInstance;

	/**
	 * provision for multiple apps. It is unlikely to be large number, and hence
	 * we think array would be better than map. Optimization is important
	 * because Application.getInstance() would be invoked quite frequently
	 */
	private static IApp[] instances;

	/**
	 * get the app with specific name
	 *
	 * @param appId
	 * @return application with the desired id, or null if there is no such app.
	 */
	public static IApp getApp(String appId) {
		if (defaultInstance != null && defaultInstance.getAppId().equals(appId)) {
			return defaultInstance;
		}
		if (instances == null) {
			logger.warn(
					"app named {} is requested, but a multi-app environment is not set up. Also, this app is not the default app.");
			return null;
		}
		for (IApp app : instances) {
			if (app.getAppId().equals(appId)) {
				return app;
			}
		}

		logger.warn(
				"app named {} is not running.", appId);
		return null;
	}

	/**
	 *
	 * @return sole/default application
	 */
	public static IApp getApp() {
		if (defaultInstance != null) {
			return defaultInstance;
		}
		throw new ApplicationError(
				"Design error: Applicaiton.getInstance() invoked before configuring any application.");
	}

	/**
	 * set this app the sole application to be run in this JVM
	 *
	 * @param app
	 */
	public static void setDefaultApp(IApp app) {
		String appId = app.getAppId();
		if (defaultInstance != null) {
			if (defaultInstance.getAppId().equals(appId)) {
				logger.info("{} is reloaded as default application", appId);
			} else {
				logger.info("{} is replaced with {} as default application", defaultInstance.getAppId(), appId);
			}
		}
		if (instances == null) {
			logger.info("{} is now running as defautl app", appId);
		} else {
			logger.info("A multi-app environment is set up with a default app of {} ", appId);
		}
		defaultInstance = app;

	}

	/**
	 * add this app to the multi-app environment. It could be the first one to
	 * be added
	 *
	 * @param app
	 */
	public static void addApp(IApp app) {
		if (defaultInstance == null) {
			defaultInstance = app;
			logger.info("{} is configured as default application", app.getAppId());
			return;
		}
		logger.info("Multi-app scenario with a default app is operational");
		if (instances != null) {
			instances = appendApp(app);
			return;
		}
		instances = new IApp[1];
		instances[0] = app;
		logger.info("{} added as the first app to a multi-app environment", app.getAppId());
	}

	/**
	 * remove an app from the multi-app environment
	 *
	 * @param appId
	 * @return removed app, or null if no such app ws running
	 */
	public static IApp removeApp(String appId) {
		IApp app = getApp(appId);
		if (app == null) {
			logger.warn("{} is not found as a running app. Not removed", appId);
			return null;
		}
		app.closeShop();

		if (defaultInstance != null && appId.equals(defaultInstance.getAppId())) {
			logger.info("Default app {} is shutting down", appId);
			defaultInstance = null;
			return app;
		}
		if (instances.length == 1) {
			instances = null;
			logger.info("Last app {} is removed. No apps running anymore", appId);
			return app;
		}
		instances = deleteApp(appId);
		logger.info("{} removed from multi-app environment. {} app/s running now", instances.length);
		return app;
	}

	private static IApp[] appendApp(IApp app) {
		String appId = app.getAppId();
		int nbr = instances.length;
		IApp[] apps = new IApp[nbr + 1];
		apps[nbr] = app;
		for (int i = 0; i < instances.length; i++) {
			IApp a = instances[i];
			if (a.getAppId().equals(appId)) {
				logger.info("An instance of {} was already running in the multi-app environment. It is reloaded");
				instances[i] = app;
				return instances;
			}
			apps[i] = instances[i];
		}
		return apps;
	}

	/**
	 * to be called only after ensuring that the app exists, and there is at
	 * least one more
	 *
	 * @param appId
	 * @return
	 */
	private static IApp[] deleteApp(String appId) {
		int nbr = instances.length;
		IApp[] apps = new IApp[nbr - 1];
		int j = 0;
		boolean toBeRemoved = true;
		for (int i = 0; i < instances.length; i++) {
			IApp a = instances[i];
			if (toBeRemoved && a.getAppId().equals(appId)) {
				toBeRemoved = false;
			} else {
				apps[j] = instances[i];
				j++;
			}
		}
		return apps;
	}
}
