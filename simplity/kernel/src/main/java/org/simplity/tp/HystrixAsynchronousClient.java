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
package org.simplity.tp;

import java.util.concurrent.Future;
 
import org.simplity.kernel.data.InputData;
import org.simplity.kernel.data.OutputData;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Get response from a server using rest call
 *
 * @author infosys.com
 *
 */
public class HystrixAsynchronousClient extends Action {
	
	OutputData requestData;
	/**
	 * In case the data to be sent for request is prepared using some logic into
	 * a field, we just send the value of that field
	 */
	String requestFieldName;
	/**
	 * By default, we extract all fields from response json/xml into service
	 * context. You may specify expected fields on the lines of
	 * inputSpecification for a service.
	 *
	 */
	InputData responseData;
	/**
	 * in case you have logic that processes the response, we set the response
	 * to this field
	 */
	String responseFieldName;
	
	public HystrixAsynchronousClient() {
		
	}
	
	@Override
	public Value doAct(ServiceContext ctx) {
		try {
			String inputData = ctx.getTextValue("inputData");
			HystrixCommandHelloWorld hystrixCommandHelloWorld = new HystrixCommandHelloWorld(inputData);
			Future<String> responseFuture = hystrixCommandHelloWorld.queue();
			String responseText = responseFuture.get();
			boolean isResponseFromCache = hystrixCommandHelloWorld.isResponseFromCache();
			
			ctx.setTextValue("response", responseText);
			ctx.setTextValue("isResponseFromCache", String.valueOf(isResponseFromCache));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return Value.VALUE_TRUE;
	}

}
