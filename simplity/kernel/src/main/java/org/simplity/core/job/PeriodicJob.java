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

import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;

import org.simplity.core.value.Value;

/**
 * A job that is executed at predetermined hours on the clock.
 *
 * @author simplity.org
 */
public class PeriodicJob extends IntervalJob {

	private static final int MINUTES = 24 * 60;

	/*
	 * number of elapsed minute for the day, sorted asc
	 */
	private final int[] timesOfDay;

	private ScheduledExecutorService scheduleExecutor;
	/*
	 * which is the next run-time?
	 */
	private int nextIdx;

	PeriodicJob(Job job, Value userId) {
		super(job, userId);
		int[] dummy = { 1 };
		this.timesOfDay = dummy;
	}

	PeriodicJob(Job job, Value userId, int[] times) {
		super(job, userId);
		this.timesOfDay = times;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.BatchJob#schedule(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public boolean scheduleJobs(ScheduledExecutorService executor) {
		this.runningJob = this.scheduledJob.createRunningJob(this.userId);
		this.scheduleExecutor = executor;
		this.nextIdx = this.getTimeIdx();
		return true;
	}

	private int getTimeIdx() {
		Calendar cal = Calendar.getInstance();
		int minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
		if (logger.isInfoEnabled()) {
			logger.info("{} hr, {} mn  {}",cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),minutes);
		}
		for (int i = 0; i < this.timesOfDay.length; i++) {
			logger.info("{}  =  {}",i,this.timesOfDay[i]);
			if (this.timesOfDay[i] >= minutes) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * submit a job if it is time to do so.
	 *
	 * @param minutes
	 * @return number of minutes remaining for the next run
	 */
	@Override
	public int poll(int minutes) {
		if (this.scheduleExecutor == null) {
			/*
			 * probably cancelled..
			 */
			return NEVER;
		}
		if (this.nextIdx < 0) {
			/*
			 * last job for day is done, and waiting for morning..
			 *
			 * is it morning?
			 */
			int n = this.timesOfDay[0] - minutes;
			if (n >= 0) {
				this.nextIdx = 0;
				return n;
			}
			return MINUTES + n;
		}
		int n = minutes - this.timesOfDay[this.nextIdx];
		if (n > 0) {
			return n;
		}
		/*
		 * time has come to submit
		 */
		boolean submitted = this.submit();
		if (!submitted) {
			return 1;
		}

		/*
		 * move to next run-time
		 */
		this.nextIdx++;
		if (this.nextIdx == this.timesOfDay.length) {
			/*
			 * done for the day.
			 */
			this.nextIdx = -1;

			logger.info(
					"Job {} has run for the last time for the day. It will be run tomorrow {} minutes into the day.",this.scheduledJob.name,this.timesOfDay[0]);

			return MINUTES - minutes + this.timesOfDay[0];
		}
		n = this.timesOfDay[this.nextIdx] - minutes;
		/*
		 * it is possible that we are terribly running behind schedule and the
		 * next one is already overdue
		 */
		if (n < 0) {
			return 1;
		}
		return n;
	}

	private boolean submit() {
		if (this.future != null && this.future.isCancelled() == false && this.future.isDone() == false) {
			logger.info("Job {} is still running when it is time to run it again.. Will wait for next poll",this.scheduledJob.name);
			return false;
		}

		this.future = this.scheduleExecutor.submit(this.runningJob);
		return true;
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
		super.cancel();
		this.scheduleExecutor = null;
	}
}
