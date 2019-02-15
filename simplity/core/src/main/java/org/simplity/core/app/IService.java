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

package org.simplity.core.app;

import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.InputData;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.ServiceContext;

/**
 * <p>
 * A service is the highest granular component of an app. Services make an app.
 * That is, objective of an app is to deliver the services allocated/defined.
 * </p>
 *
 * <p>
 * Core of Simplity is the notion of a service as just an API with three
 * important aspect to it
 * </p>
 * <li>Input Specification. <code>InputData</code></li>
 * <li>Description of what it does from client's perspective. This is described
 * in plain words. No formal specification.</li>
 * <li>Output specification in the form of <code>OutputData</code></li>
 * <p>
 * A service is completely unaware of the client, channel, method, protocol etc.
 * If a service has to behave differently based the way it is requested, for
 * example channel, then it has to be put as part of input data. Alternatively,
 * an aspect-driven design may be used to handle them outside the service
 * </p>
 * <p>
 * Service is normally invoked by <code>ServiceAgent</code>
 * </p>
 *
 *
 * @author simplity.org
 *
 */
public interface IService {

	/**
	 * While a service specifies its input requirement, it is not required to
	 * actually manage the data extraction from input source. This is to ensure
	 * that the service is unaware of the way the input data is received. It is
	 * up to the caller to extract data from the incoming request and put them
	 * into service context
	 *
	 * @return data expected as input from client/requester. null if no input is
	 *         expected.
	 */
	public InputData getInputSpecification();

	/**
	 * serve this request. input data is available in the context, and the
	 * output data should be made available in the context. That is, this method
	 * is meant for service to do its main processing, without worrying about
	 * input and output data
	 *
	 * @param ctx
	 */
	public void serve(ServiceContext ctx);

	/**
	 * execute this service as an action from another service
	 *
	 * @param ctx
	 *            any input data requirement of this service is assumed to be
	 *            already made available here.
	 * @return true if all OK. false if the service did not execute, and an
	 *         error message is added to the ctx
	 */
	public boolean executeAsSubProcess(ServiceContext ctx);

	/**
	 *
	 * @return data returned as response from this service. null if no data is
	 *         output.
	 */
	public OutputData getOutputSpecification();

	/**
	 *
	 * @return unique name of this service
	 */
	public String getServiceName();

	/**
	 *
	 * @return true if the response from this service can be cached, possibly by
	 *         caching-keys. false if this service should always be called.
	 */
	public boolean okToCache();

	/**
	 * relevant only if okToCache() returns true;
	 *
	 * @return list of key field names. Meaning is that this response for this
	 *         service depends only on these fields. Response can be cached, and
	 *         re-used based on values for these fields. for example, response
	 *         from service named getStates can be cached by countrCode.
	 */
	public String[] getCacheKeyNames();

	/**
	 * should the service be fired in the background (in a separate thread)?
	 *
	 * @return true if this service is marked for background execution
	 */
	public boolean toBeRunInBackground();

	/**
	 * this service may be called to work as an action in another service. In
	 * that case, the main service that calls this service wants to ensure that
	 * the right dbHandle is passed
	 *
	 * @return non-null dbUsage
	 */
	public DbUsage getDbUsage();

	/**
	 * is this one of those special service that optimizes the output process by
	 * directly writing to the response rather than preparing data in the
	 * service context? This is to be used only if such an optimization is
	 * required at the cost of making the service inflexible for different type
	 * of clients
	 *
	 * @return true if the service needs a writer to be initiated in the service
	 *         context to which it would write data as part of its processing.
	 *         false otherwise
	 */
	public boolean directlyWritesDataToResponse();

	/**
	 * Swagger/OpenAPIdesigners are asking response to be any json. So response
	 * need not be an object. While it is possible that the response be just a
	 * field, we are making provision only to an array at this time
	 *
	 * @return true if the response is an array and not an object. false if it
	 *         is an object
	 */
	public boolean responseIsAnArray();
}
