/*
 * Copyright (c) 2019 simplity.org
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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import javax.naming.InitialContext;

import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class to manage batch jobs. sub-component of <code>Application</code>.
 * Designed as an independent public class only to put it in the right package.
 * Should not be considered as an API
 *
 * @author simplity.org
 *
 */
public class BatchSetup {
	private static Logger logger = LoggerFactory.getLogger(BatchSetup.class);
	/**
	 * batch job to fire after bootstrapping.
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.JOBS)
	String jobsToRunOnStartup;

	/*
	 * for batch jobs
	 */
	/**
	 * jndi name the container has created to get a threadFactory instance
	 */
	String threadFactoryJndiName;

	/**
	 * jndi name the container has created to get a managed schedule thread
	 * pooled executor instance
	 */
	String scheduledExecutorJndiName;

	/** number of threads to keep in the pool even if they are idle */
	int corePoolSize;
	/*
	 * batch and thread management
	 */
	private ThreadFactory threadFactory;
	private ScheduledExecutorService threadPoolExecutor;
	private int batchPoolSize;

	/**
	 * configure is called by <code>Application</code> after loading the
	 * attributes
	 *
	 * @return null if all ok. Error message in case of any error
	 */
	public String configure() {
		/*
		 * batch job, thread pools etc..
		 */
		if (this.corePoolSize == 0) {
			this.batchPoolSize = 1;
		} else {
			this.batchPoolSize = this.corePoolSize;
		}

		if (this.threadFactoryJndiName != null) {
			try {
				this.threadFactory = (ThreadFactory) new InitialContext().lookup(this.threadFactoryJndiName);
				logger.info("Thread factory instantiated as " + this.threadFactory.getClass().getName());
			} catch (Exception e) {
				return this.failed(
						"Error while looking up " + this.threadFactoryJndiName + ". " + e.getLocalizedMessage());
			}
		}

		if (this.scheduledExecutorJndiName != null) {
			try {
				this.threadPoolExecutor = (ScheduledExecutorService) new InitialContext()
						.lookup(this.scheduledExecutorJndiName);
				logger.info(
						"ScheduledThreadPoolExecutor instantiated as "
								+ this.threadPoolExecutor.getClass().getName());
			} catch (Exception e) {
				return this.failed("Error while looking up " + this.scheduledExecutorJndiName + ". "
						+ e.getLocalizedMessage());
			}
		}
		if (this.jobsToRunOnStartup != null) {
			BatchJobs.startJobs(this.jobsToRunOnStartup);
			logger.info("Scheduler started for Batch " + this.jobsToRunOnStartup);
		}
		return null;
	}

	private String failed(String msg) {
		logger.error(msg);
		/*
		 * we run the background batch job only if everything has gone well.
		 */
		if (this.jobsToRunOnStartup != null) {
			logger.error("Scheduler NOT started for batch " + this.jobsToRunOnStartup
					+ " because of issues with applicaiton set up.");
		}
		return msg;

	}

	/**
	 * get a managed thread as per the container
	 *
	 * @param runnable
	 * @return thread
	 */
	public Thread createThread(Runnable runnable) {
		if (this.threadFactory == null) {
			return new Thread(runnable);
		}
		return this.threadFactory.newThread(runnable);
	}

	/**
	 * get a managed thread as per the container
	 *
	 * @return executor
	 */
	public ScheduledExecutorService getScheduledExecutor() {
		if (this.threadPoolExecutor != null) {
			return this.threadPoolExecutor;
		}
		int nbr = this.batchPoolSize;
		if (nbr == 0) {
			nbr = 2;
		}
		if (this.threadFactory == null) {
			return new ScheduledThreadPoolExecutor(nbr);
		}
		this.threadPoolExecutor = new ScheduledThreadPoolExecutor(nbr, this.threadFactory);
		return this.threadPoolExecutor;
	}

}
