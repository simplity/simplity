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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.value.Value;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a service to be executed in the background. That is, it is not
 * attached to a client-request. It can be configured to run every so often, or
 * at scheduled time-of-day.
 *
 * @author simplity.org
 *
 */
public class Job {
	private static final Logger logger = LoggerFactory.getLogger(Job.class);

	/** name of the job, unique within a jobs collection */
	@FieldMetaData(isRequired = true)
	String name;

	/** service to be run as a job */
	@FieldMetaData(isRequired = true)
	String serviceName;

	/**
	 * this job is to be fired at these times on a 24 hour clock. null if this
	 * job is
	 */
	String[] runAtTheseTimes;

	/** this job is to be run every so many seconds */
	int runInterval;

	/**
	 * is this a job that runs for ever left to itself? In that case specify
	 * number of such instances. In this case, runInterval and runAtTheseTimes
	 * are ignored.
	 */
	int nbrDedicatedThreads;

	/**
	 * parameters that this service expects as input are supplied with this
	 * mechanism
	 */
	String inputJson;

	/**
	 * if this job is to be fired with a specific userId. Defaults to scheduler
	 * level setting
	 */
	String userId;

	/** input fields */
	InputField[] inputFields;

	/** cached during getReady(); */
	private Value userIdValue;

	/** number of minutes elapsed for the day */
	private int[] timesOfDay;

	/**
	 * @param jobName
	 * @param serviceName
	 * @param intervalInSeconds
	 * @param nbrThreads
	 * @param timesOfDayToExecute
	 *            comma separated list of time-of-day at which th ejob is to be
	 *            fired. for example 01:10,11:30,14:60,23:30
	 */
	public Job(String jobName, String serviceName, int intervalInSeconds, int nbrThreads, String timesOfDayToExecute) {
		this.name = jobName;
		this.serviceName = serviceName;
		this.runInterval = intervalInSeconds;
		this.nbrDedicatedThreads = nbrThreads;
		if (timesOfDayToExecute != null) {
			this.runAtTheseTimes = timesOfDayToExecute.split(",");
		}
	}

	/** */
	public Job() {
		// default
	}

	/** */
	public void getReady() {
		if (this.runInterval > 0 && this.nbrDedicatedThreads > 0) {
			throw new ApplicationError("Job " + this.name
					+ " has set both runInterval and nbrDedicatedThreads. You shoudl specify one of them : either to run as batch every so often, or as a background job");
		}
		if (this.runInterval == 0 && this.nbrDedicatedThreads == 0) {

			logger.info("Job " + this.name + " will be run once");

			this.nbrDedicatedThreads = 1;
		}
		if (this.userId != null) {
			if (Application.getActiveInstance().userIdIsNumeric()) {
				try {
					this.userIdValue = Value.newIntegerValue(Long.parseLong(this.userId));
				} catch (Exception e) {
					throw new ApplicationError(
							"Job " + this.name + " has a wrong numeric value of " + this.userId + " as user id");
				}
			} else {
				this.userIdValue = Value.newTextValue(this.userId);
			}
		}
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.getReady();
			}
		}
		if (this.runAtTheseTimes != null) {
			this.timesOfDay = this.getTimes(this.runAtTheseTimes);
		}
	}

	/**
	 * @param uid
	 * @return instance of a scheduled job
	 */
	public ScheduledJob createScheduledJob(Value uid) {
		Value val = this.userIdValue;
		if (val == null) {
			val = uid;
		}
		if (this.timesOfDay != null) {
			return new PeriodicJob(this, val, this.timesOfDay);
		}
		if (this.runInterval > 0) {
			return new IntervalJob(this, val);
		}
		return new ListenerJob(this, val);
	}

	/**
	 * @param uid
	 * @return a running job
	 */
	public RunningJob createRunningJob(Value uid) {
		Value val = this.userIdValue;
		if (val == null) {
			val = uid;
		}
		Map<String, Object> fields = null;
		if (this.inputFields != null) {
			fields = new HashMap<>();
			for (InputField field : this.inputFields) {
				field.addToMap(fields);
			}
		}

		ServiceRequest req = null;
		if (this.inputJson != null) {
			req = new ServiceRequest(this.serviceName, fields, new JSONObject(this.inputJson));
		} else {
			req = new ServiceRequest(this.serviceName, fields);
		}

		return new RunningJob(req);
	}

	private int[] getTimes(String[] texts) {

		int times[] = new int[texts.length];
		for (int i = 0; i < texts.length; i++) {
			String[] pair = texts[i].split(":");
			if (pair.length != 2) {
				this.wrongOne(i);
			}
			try {
				int hh = Integer.parseInt(pair[0].trim(), 10);
				int mm = Integer.parseInt(pair[1].trim(), 10);
				if (hh < 0 || mm < 0 || hh > 23 || mm > 59) {
					this.wrongOne(i);
				}
				times[i] = hh * 60 + mm;
			} catch (Exception e) {
				this.wrongOne(i);
			}
		}
		Arrays.sort(times);
		return times;
	}

	private void wrongOne(int i) {
		throw new ApplicationError("Job " + this.name + " has an invalied time-of-day " + this.runAtTheseTimes[i]
				+ ". hh:mm, hh:mm,..  format is expected.");
	}

	/**
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.runInterval == 0 && this.nbrDedicatedThreads == 0 && this.runAtTheseTimes == null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
					"Job " + this.name + " has not specified any attributes for running. Assumed nbrDedicatedThread=1",
					"nbrDedicatedThread"));
		}
		if (this.runAtTheseTimes != null) {
			if (this.runInterval > 0) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"Job " + this.name + " has specified runAtTheseTimes, and hence runInterval="
								+ this.runInterval + " ignored.",
						"runInterval"));
			}
			if (this.nbrDedicatedThreads > 0) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"Job " + this.name + " has specified runAtTheseTimes, and hence nbrDedicatedThreads="
								+ this.nbrDedicatedThreads + " ignored",
						"nbrDedicatedThreads"));
			}
		} else if (this.runInterval > 0 && this.nbrDedicatedThreads > 0) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
					"Job " + this.name + " has specified nbrDedicatedThreads, and hence runInterval="
							+ this.runInterval + " ignored",
					"runInterval"));
		}
	}
}
