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

package org.simplity.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.AppUser;
import org.simplity.core.app.Application;
import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.app.internal.ServiceResponse;
import org.simplity.core.value.Value;
import org.simplity.json.JSONObject;

/**
 * @author simplity.org
 *
 */
public class CLI {
	/**
	 * command line utility to run any service. Since this is command line, we
	 * assume that security is already taken care of. (If user could do delete
	 * *.* what am I trying to stop him/her from doing??) We pick-up logged-in
	 * user name. Note that java command line has an option to specify the
	 * login-user. One can use this to run the application as that user
	 *
	 * @param args
	 *            comFolderName serviceName param1=value1 param2=value2 .....
	 */
	public static void main(String[] args) {
		doRun(args);
	}

	private static void doRun(String[] args) {
		int nbr = args.length;
		if (nbr < 2) {
			printUsage();
			return;
		}

		String resourceRoot = args[0];
		String serviceName = args[1];
		Map<String, String> params = new HashMap<>();
		params.put(AppConventions.Name.RESOURCE_ROOT, args[0]);
		List<String> messages = new ArrayList<>();
		Application app = new Application();
		boolean ok = app.openShop(params, messages);
		if (!ok) {
			System.err.println("Application failed to bootstrap with resource root = " + resourceRoot);
			return;
		}

		String user = System.getProperty("user.name");

		JSONObject json = new JSONObject();
		if (nbr > 2) {
			for (int i = 2; i < nbr; i++) {
				String[] parms = args[i].split("=");
				if (parms.length != 2) {
					printUsage();
					System.exit(-3);
				}
				json.put(parms[0], parms[1]);
			}
		}

		System.out.println("path:" + resourceRoot);
		System.out.println("userId:" + user);
		System.out.println("service:" + serviceName);
		System.out.println("request:" + json.toString());

		AppUser appUser = new AppUser(Value.newTextValue(user));
		ServiceRequest req = new ServiceRequest(serviceName, null, json);
		req.setUser(appUser);
		ServiceResponse resp = new ServiceResponse(false);
		app.serve(req, resp);

		System.out.println("response :" + resp.getPayloadText());
	}

	private static void printUsage() {
		System.out.println(
				"Usage : java  org.simplity.core.CLI componentFolderPath serviceName inputParam1=vaue1 ...");
		System.out.println(
				"example : java  org.simplity.core.CLI /user/data/ serviceName inputParam1=vaue1 ...");
	}

}
