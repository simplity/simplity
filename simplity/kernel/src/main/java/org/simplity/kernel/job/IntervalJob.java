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

package org.simplity.kernel.job;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.simplity.kernel.value.Value;

/**
 * A job that is added to a scheduler. manages running jobs for the job that is
 * scheduled
 *
 * @author simplity.org
 */
public class IntervalJob extends ScheduledJob {

	/*
	 * we have only one job
	 */
	protected RunningJob runningJob;
	protected Future<?> future;

	IntervalJob(Job job, Value uid) {
		super(job, uid);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#schedule(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public boolean scheduleJobs(ScheduledExecutorService executor) {
		this.runningJob = this.scheduledJob.createRunningJob(this.userId);
		this.future = executor.scheduleAtFixedRate(this.runningJob, 0, this.scheduledJob.runInterval, TimeUnit.SECONDS);
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.job.ScheduledJob#shutDownGracefully(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void cancel() {
		if (this.future != null) {
			this.future.cancel(true);
		}
		this.isScheduled = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#incrmentThread(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void incrmentThread(ScheduledExecutorService executor) {
		this.noChange();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#decrmentThread(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void decrmentThread(ScheduledExecutorService executor) {
		this.noChange();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#putStatus(java.util.List)
	 */
	@Override
	public void putJobStatusStub(JobStatus sts, List<RunningJobInfo> infoList) {
		this.putJobStatus(sts, this.runningJob, infoList, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#poll(int)
	 */
	@Override
	public int poll(int referenceMinutes) {
		return ScheduledJob.NEVER;
	}
}
