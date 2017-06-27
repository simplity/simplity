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

package org.simplity.kernel.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.simplity.json.JSONObject;

/**
 * utility routines around HTTP protocol
 *
 * @author simplity.org
 *
 */
public class HttpUtil {
	/**
	 * read input stream into a string
	 *
	 * @param req
	 * @return body of HTTP request as a string. empty string if there is no
	 *         body. null in case of error.
	 * @throws IOException
	 */
	public static String readBody(HttpServletRequest req) throws IOException {
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

	/**
	 * @param req
	 * @param existingJson
	 *            if you want form fields to be copied to an existing json.null if
	 *            you want a new one to be created for this.
	 * @return json to which form fields, if any are copied to. it is
	 *         existingJson if it were non-null. it is a new json object if
	 *         existingJson was null
	 * @throws IOException
	 */
	public static JSONObject getFormAsJson(HttpServletRequest req, JSONObject existingJson) throws IOException {
		JSONObject params = existingJson == null ? new JSONObject() : existingJson;

		parseQueryString(req, params);
		return params;
	}

	/**
	 * extract query string parameters into json
	 *
	 * @param req
	 * @param existingJson
	 *            null if you want a new json to be created and returned for
	 *            this.
	 * @return json to which query string parameters, if any are copied. always
	 *         non-null.
	 */
	public static JSONObject parseQueryString(HttpServletRequest req, JSONObject existingJson) {
		JSONObject params = existingJson;
		if (params == null) {
			params = new JSONObject();
		}
		parseFields(req.getQueryString(), params);
		return params;
	}

	/**
	 * extract query string parameters into json
	 *
	 * @param uriEncodedString
	 * @param params
	 *            to which fields are to be extracted
	 */
	public static void parseFields(String uriEncodedString, JSONObject params) {
		if (uriEncodedString == null) {
			return;
		}
		String text = uriEncodedString.trim();
		if (text.isEmpty()) {
			return;
		}
		String[] parts = text.split("&");
		try {
			for (String part : parts) {
				if (part.isEmpty()) {
					continue;
				}
				String[] pair = part.split("=");
				String field = URLDecoder.decode(pair[0], "UTF-8");
				String value;
				if (pair.length == 1) {
					params.accumulate(field, "");
					params.accumulate("_query", field);
				} else {
					value = URLDecoder.decode(pair[1], "UTF-8");
					params.accumulate(field, value);
				}
			}
		} catch (UnsupportedEncodingException e) {
			// we know that this is supported
		}
	}
}
