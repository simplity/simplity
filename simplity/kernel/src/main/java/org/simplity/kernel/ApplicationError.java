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
package org.simplity.kernel;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents an application design error. We have decided to make it an
 * unchecked exception because
 * there is no reason for this error to occur at run time, except that the
 * programmer has not run
 * design-time validations. This is like syntax error in a script language. We
 * expect that the
 * project has put reasonable build procedure to catch such errors. Examples are
 * missing components,
 * incompatible data-types etc..
 *
 * <p>
 * Even sql exceptions are not to be handled at the component level, they are
 * actually re-thrown
 * as ApplicationExcption We would like to catch all such exceptions at the
 * highest level and deal
 * with them rather than each component worrying about it.
 *
 * @author simplity.org
 */
public class ApplicationError extends RuntimeException {
	private static final Logger logger = LoggerFactory.getLogger(ApplicationError.class);

	protected static final long serialVersionUID = 1L;
	protected String msg;

	/**
	 * construct with cause of this error
	 *
	 * @param error
	 *            error message
	 */
	public ApplicationError(String error) {
		logger.error(error);
		this.msg = error;
	}

	/**
	 * error to be raised when an enum type is not handled. Useful as default case in switch-case
	 * @param e
	 */
	public ApplicationError(Enum<?> e) {
		this("Design Error: Ebun value of " + e + " for type " + e.getClass().getName() + " not yet implemented");
	}

	/**
	 * handles SqlException that is likely to be chained to get all messages
	 *
	 * @param e
	 *            exception being caught
	 * @param msg
	 *            additional error message
	 */
	public ApplicationError(Exception e, String msg) {

		logger.error(msg, e);

		if (e instanceof SQLException) {
			this.msg = this.getSqlMessage((SQLException) e);
		} else {
			this.msg = e.getMessage();
		}
	}

	private String getSqlMessage(SQLException e) {
		StringBuilder sbf = new StringBuilder();
		for (Throwable t : e) {
			sbf.append(t.getMessage()).append('\n');

			logger.error(e.getMessage());
		}
		return sbf.toString();
	}

	@Override
	public String getMessage() {
		return this.msg;
	}
}
