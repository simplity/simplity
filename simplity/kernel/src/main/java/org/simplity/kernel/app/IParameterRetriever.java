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

package org.simplity.kernel.app;

import org.simplity.kernel.service.ServiceContext;

/**
 * utility class that retrieves run time value for a parameter. Since such a
 * utility is app-specific, this class is designed to have app-specific
 * implementation as worker instance. the worker instance is set using
 * parameterRetrieverClassName attribute of application .
 *
 * @author simplity.org
 *
 */
public interface IParameterRetriever {
	/**
	 * get run time value for a parameter
	 *
	 * @param paramName
	 * @param ctx
	 *            service context in which this validation is required. null if
	 *            this is being executed outside of a service context
	 * @return value for this parameter, null if value could not be located
	 */
	public String getValue(String paramName, ServiceContext ctx);

}
