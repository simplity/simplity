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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * our own scheduler that schedules job based on time-of-day rather than intervals
 *
 * @author simplity.org
 */
public class TimeOfDayScheduler implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(TimeOfDayScheduler.class);

  private static final int MAX_MINUTES = 24 * 60;

  /*
   * design: polls on each of the job. next poll is the least of the values
   * returned. Not tracking NEVER returned by poll, and we continue to poll
   *
   * polling is very short, and hence most of the time this thread would
   * sleep. wakeUp() designed to facilitate any change in the polling needs.
   * interrupt (without wake-up) in honored as a request to shut down
   */
  /** jobs to be scheduled */
  private ScheduledJob[] pollers;

  /** track a wake-up call */
  private boolean wakeupRequested;

  /** thread in which scheduler is running, or rather sleeping :-( */
  private Thread thread;

  /**
   * interrupt the infinite-loop.
   *
   * @param justWakeup true to request to wake-up. Typically when scheduler has been modified. false
   *     is to stop the agony of infinite work
   */
  public void interrupt(boolean justWakeup) {
    this.wakeupRequested = justWakeup;
    this.thread.interrupt();
  }

  /** @param jobs */
  public TimeOfDayScheduler(ScheduledJob[] jobs) {
    this.pollers = jobs;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    this.thread = Thread.currentThread();

    logger.info("TimeOfDay scheduler started.");

    while (true) {
      int nextDue = MAX_MINUTES;
      Calendar cal = Calendar.getInstance();
      int minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
      for (ScheduledJob job : this.pollers) {
        int n = job.poll(minutes);

        logger.info("Job " + job.scheduledJob.name + " can wait for " + n + " minutes");

        if (n > 0 && n < nextDue) {
          nextDue = n;
        }
      }
      try {

        logger.info("TimeOfDay scheduler taking a nap for " + nextDue + " minutes.");

        Thread.sleep(nextDue * 60000);
      } catch (InterruptedException e) {
        if (this.wakeupRequested) {
          this.wakeupRequested = false;
        } else {

          logger.info("TimeOfDayScheduler interrupted");

          return;
        }
      }
    }
  }
}
