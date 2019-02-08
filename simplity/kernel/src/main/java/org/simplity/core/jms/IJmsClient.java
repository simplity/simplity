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

package org.simplity.core.jms;

import org.simplity.core.service.ServiceContext;

/**
 * interface to be implemented by a class that wants to process a message that
 * is consumed by a queue.
 *
 * @author simplity.org
 */
public interface IJmsClient {
	/**
	 * process a message. Data content of the message is already extracted into
	 * the ctx.
	 *
	 * @param ctx
	 *            service context where this is all happening. Data in the
	 *            incoming message is extracted into this.
	 * @return true if all ok, and the session should be committed. false if
	 *         something went wrong, and the session is to be rolled back.
	 */
	public boolean process(ServiceContext ctx);

	/**
	 *
	 * @return after do the desired processing (before for producing, after for
	 *         consuming) the worker signals whether to continue, or stop.
	 */
	public boolean toContinue();
}
