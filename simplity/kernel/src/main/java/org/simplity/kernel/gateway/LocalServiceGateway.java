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

package org.simplity.kernel.gateway;

import org.simplity.kernel.app.Application;
import org.simplity.kernel.service.ServiceContext;

/**
 * gateway to be used when this application is simplity-based, and has the
 * flexibility to be either bundled with this app/module, or deployed separately
 *
 * @author simplity.org
 *
 */
public class LocalServiceGateway extends ServiceGateway {

	@Override
	public IServiceAssistant getAssistant(String serviceName, ServiceContext ctx) {
		return new Assistant(serviceName);
	}

	@Override
	public void getReady() {
		//

	}

	@Override
	public void shutdown() {
		//
	}

	class Assistant implements IServiceAssistant {
		private String serviceName;

		Assistant(String serviceName) {
			this.serviceName = serviceName;
		}

		@Override
		public boolean execute(ServiceContext ctx) {
			return Application.getActiveInstance().getService(this.serviceName).executeAsSubProcess(ctx);
		}
	}

}
