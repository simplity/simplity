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

package org.simplity.kernel.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle Oauth2 authentication
 *
 * @author simplity.org
 *
 */
public class OAuth2Agent implements ISecurityAgent {
	private static Logger logger = LoggerFactory.getLogger(OAuth2Agent.class);
	private static final String ACCESS_TOKEN = "access_token";
	private static final String SCOPES_ATTR = "scopes";
	/**
	 * scopes for this authentication.
	 */
	private String[] scopes;

	/**
	 * initialize with specifications
	 *
	 * @param specs
	 */
	public OAuth2Agent(JSONObject specs) {
		JSONObject scopesLocal = specs.optJSONObject(SCOPES_ATTR);
		if (scopesLocal != null) {
			this.scopes = JSONObject.getNames(scopesLocal);
		} else {
			this.scopes = new String[0];
		}
	}

	/**
	 * initialize with specifications
	 *
	 * @param scopes
	 */
	public OAuth2Agent(String[] scopes) {
		if (scopes != null) {
			this.scopes = scopes;
		} else {
			this.scopes = new String[0];
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.auth.SecurityAgent#securityCleared(java.lang.Object[])
	 */
	@Override
	public boolean securityCleared(Object... params) {
		if (params != null && params.length == 2 && params[0] != null && params[0] instanceof HttpServletRequest
				&& params[1] != null && params[1] instanceof HttpServletResponse) {
			try {
				return this.securityCleared((HttpServletRequest) params[0], (HttpServletResponse) params[1]);
			} catch (Exception e) {
				throw new ApplicationError(e, "Error while invoking Oauth2 security)");
			}
		}
		throw new ApplicationError(
				"Oauth2Agent.securityCleared() requires HttpServletRequest and HttpServletResponse as parameters, both of which have to be non-null");
	}

	/**
	 *
	 * @param req
	 * @param resp
	 * @return true if cleared. false if response is redirected to
	 *         authentication site. Caller should end this request.
	 * @throws IOException
	 */
	public boolean securityCleared(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String accesstoken = this.parseToken(req, ACCESS_TOKEN);
		if (accesstoken == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is required");
			return false;
		}
		return this.checkForValidToken(accesstoken);
	}

	private boolean checkForValidToken(String accesstoken) {
		OAuthParameters oAuthParameters = Application.getActiveInstance().getOAuthParameters();
		String url = oAuthParameters.getCheckTokenURL();
		url += "?token=" + accesstoken;
		HttpURLConnection conn = null;
		logger.info("Checking token " + url);
		try {
			String userPassword = oAuthParameters.getClientId() + ":" + oAuthParameters.getClientSecret();
			String encoding = DatatypeConverter.printBase64Binary(userPassword.getBytes());

			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic " + encoding);

			if (conn.getResponseCode() == HttpServletResponse.SC_OK) {
				return true;
			}
		} catch (IOException e) {
			logger.error("Error with token {}", e);
		}
		return false;
	}

	/**
	 * parse token form header/query string
	 *
	 * @param req
	 * @param tokenType
	 * @return
	 */
	private String parseToken(HttpServletRequest req, String tokenType) {
		String qry = req.getHeader(tokenType);
		if (qry == null) {
			qry = req.getParameter(tokenType);
		}
		return qry;
	}

	/**
	 *
	 * @return scopes
	 */
	public String[] getScopes() {
		return this.scopes;
	}

}
