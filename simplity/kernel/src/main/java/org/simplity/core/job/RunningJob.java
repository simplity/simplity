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

package org.simplity.core.job;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.app.internal.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a thread that runs a service with given input data
 *
 * @author simplity.org
 */
public class RunningJob implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(RunningJob.class);

	private final ServiceRequest request;
	private JobStatus jobStatus = JobStatus.SCHEDULED;

	/**
	 * create a job thread to run a service with the input data
	 *
	 * @param req
	 *            input request for the service
	 */
	public RunningJob(ServiceRequest req) {
		this.request = req;
	}

	@Override
	public void run() {
		/*
		 * remember : this is the thread that would run as long as the service
		 * wants it.The service may be designed to run for ever, or it may be a
		 * batch job and return once known set of work is finished. It is
		 * possible that getJobStatus() be invoked from another thread, and
		 * hence we keep that field updated
		 */
		this.jobStatus = JobStatus.RUNNING;
		String serviceName = this.request.getServiceName();
		logger.info("Job for service {} started", serviceName);
		Application app = Application.getActiveInstance();
		try {
			app.serve(this.request, new ServiceResponse(false));
			logger.info("Service " + serviceName + " is done..");
			this.jobStatus = JobStatus.DONE;
		} catch (Exception e) {
			this.jobStatus = JobStatus.FAILED;
			String msg = "Error while running service " + serviceName + " as a batch job.";
			app.reportApplicationError(this.request, new ApplicationError(e, msg));
		}
	}

	/** @return current job status */
	public JobStatus getJobStatus() {
		return this.jobStatus;
	}

	/** @return current job status */
	public String getServiceStatus() {
		Object msg = this.request.getMessageBox().getMessage();
		if (msg == null) {
			return "unknown";
		}
		return msg.toString();
	}
}
