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

package org.simplity.core.batch;

import org.simplity.core.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Wrapper on a job when it is scheduled
 *
 * @author simplity.org
 */
public abstract class ScheduledJob {
	protected static final Logger logger = LoggerFactory.getLogger(ScheduledJob.class);

  /** due at value to denote that this job will nt submit again */
  public static final int NEVER = -1;

  protected final Job scheduledJob;
  protected boolean isScheduled;
  protected Value userId;

  ScheduledJob(Job job, Value uid) {
    this.scheduledJob = job;
    this.userId = uid;
  }

  /**
   * schedule running jobs using the executor
   *
   * @param executor
   * @return true if this needs polling. false if executor can manage its scheduling
   */
  public boolean schedule(ScheduledExecutorService executor) {
    if (this.isScheduled) {

      logger.info(this.scheduledJob.name + " is already scheduled");

      return false;
    }
    this.isScheduled = true;
    return this.scheduleJobs(executor);
  }

  abstract boolean scheduleJobs(ScheduledExecutorService executor);
  /** cancel this job */
  abstract void cancel();

  /**
   * add another thread to this job. ignored if this is a batch job, or if there is only one thread
   * at this time
   *
   * @param executor
   */
  public void incrmentThread(ScheduledExecutorService executor) {
    this.noChange();
  }

  /**
   * reduce a thread from this job. ignored if this is a batch job, or if there is only one thread
   * at this time
   *
   * @param executor
   */
  public void decrmentThread(ScheduledExecutorService executor) {
    this.noChange();
  }

  protected void noChange() {

    logger.info(
        "Job {} is a batch, and hence we can not add/remove thread",this.scheduledJob.name);
  }

  /**
   * add status of running jobs into the list
   *
   * @param infoList
   */
  public void putStatus(List<RunningJobInfo> infoList) {
    JobStatus sts = null;
    if (this.isScheduled == false) {
      sts = JobStatus.CANCELLED;
    }
    this.putJobStatusStub(sts, infoList);
  }

  /**
   * @param sts
   * @param infoList
   */
  protected abstract void putJobStatusStub(JobStatus sts, List<RunningJobInfo> infoList);

  /**
   * @param sts
   * @param runningJob2
   * @param infoList
   */
  protected void putJobStatus(
      JobStatus sts, RunningJob rj, List<RunningJobInfo> infoList, int seq) {
    JobStatus status = sts;
    String serviceStatus = "unknown";
    if (rj != null) {
      if (sts == null) {
        status = rj.getJobStatus();
      }
      serviceStatus = rj.getServiceStatus();
    }
    RunningJobInfo info =
        new RunningJobInfo(
            this.scheduledJob.name, this.scheduledJob.serviceName, status, seq, serviceStatus);
    infoList.add(info);
  }

  /**
   * poll wake-up for the scheduled job to check whether it should submit returned value is the
   * number of minutes remaining for this job. This can be used by the caller to optimize polling,
   * if at all required. Since we are talking about minutes, blind polling itself should be fine
   *
   * @param referenceMinutes
   * @return number of minutes. NEVER to indicate that this job need not be
   */
  public int poll(int referenceMinutes) {
    return ScheduledJob.NEVER;
  }
}
