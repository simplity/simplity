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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.rest;

import java.net.MalformedURLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.simplity.kernel.Application;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;

/**
 * to be configured to run as a startup servlet when Simplity application needs
 * to be exposed as servlet
 *
 * @author simplity.org
 *
 */
public class Startup extends HttpServlet {

	private static final long serialVersionUID = 1L;
	/**
	 * name of parameter in web.xml that has the root folder for resources.
	 * defaults to "WEB-INF/components"
	 */
	public static final String COMP_FOLDER = "comp-folder";
	/**
	 * context name to which web.xml would have set api-folder
	 */
	public static final String API_FOLDER = "api-folder";
	/**
	 * default resource folder relative to web-root.
	 */
	public static final String DEFAULT_COMP_FOLDER = "/WEB-INF/comp/";
	/**
	 * default folder for api
	 */
	public static final String DEFAULT_API_FOLDER = "/WEB-INF/api/";

	@Override
	public void init() throws ServletException {
		ServletContext ctx = this.getServletContext();
		bootStrap(ctx);
		loadApis(ctx);
	}

	/**
	 * made this a public static to get flexibility of calling this from another
	 * servlet
	 *
	 * @param ctx
	 *            Context
	 */
	public static void bootStrap(ServletContext ctx) {
		FileManager.setContext(ctx);
		String folder = ctx.getInitParameter(COMP_FOLDER);
		if (folder == null) {
			/*
			 * comp folder not set in web.xml. Let us look for that in context
			 */
			folder = DEFAULT_COMP_FOLDER;
			try {
				folder = ctx.getResource(folder).getPath();
				Tracer.trace("Root folder is set using recource to " + folder);
			} catch (MalformedURLException e) {
				Tracer.trace(e, "Error while getting root folder path from servlet context");
			}
		} else {
			Tracer.trace("Root folder is set to " + folder + " as a web parameter.");
		}
		Tracer.trace("Going to bootstrap Application with comp folder at " + folder);
		try {
			Application.bootStrap(folder);
		} catch (Exception e) {
			Tracer.trace(e,
					"Unable to bootstrap Application using resource folder " + folder + ". Application will not work.");
		}
	}
	/**
	 * made this a public static to get flexibility of calling this from another
	 * servlet
	 *
	 * @param ctx
	 *            Context
	 */
	public static void loadApis(ServletContext ctx) {
		String folder = ctx.getInitParameter(API_FOLDER);
		if (folder == null) {
			/*
			 * comp folder not set in web.xml. Let us look for that in context
			 */
			folder = DEFAULT_API_FOLDER;
			try {
				folder = ctx.getResource(folder).getPath();
				Tracer.trace("API folder is set to " + folder);
			} catch (MalformedURLException e) {
				Tracer.trace(e, "Error while getting folder path from servlet context");
			}
		} else {
			Tracer.trace("API folder is set to " + folder + " as a web parameter.");
		}
		Tracer.trace("Going to load apis from " + folder);
		try {
			Operations.loadAll(folder);
		} catch (Exception e) {
			Tracer.trace(e,
					"Unable to load APIs from folder " + folder + ". Application will not work.");
		}
	}
}
