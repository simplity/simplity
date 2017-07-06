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

package org.simplity.job;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.value.Value;

/**
 * list of all jobs to be managed as batch/background services.
 *
 * @author simplity.org
 */
public class Jobs implements Component {
  static final Logger logger = Logger.getLogger(Jobs.class.getName());

  private static Jobs jobsInstance;

  /** @return current instance that can be used for managing running jobs */
  public static Jobs getCurrentInstance() {
    return jobsInstance;
  }

  /**
   * start an empty one. Jobs may be added later..
   *
   * @return instance of jobs that can be used for managing running jobs
   */
  public static Jobs startEmptyJobs() {
    jobsInstance = new Jobs();
    jobsInstance.jobs = new Job[0];
    jobsInstance.name = "dummy";
    jobsInstance.getReady();
    jobsInstance.start();
    return jobsInstance;
  }

  /**
   * schedule batch from a non-default resource
   *
   * @param jobName name of the jobs resource under Jobs folder to be used.
   * @return instance of jobs that can be used for managing running jobs
   */
  public static Jobs startJobs(String jobName) {
    if (jobName == null || jobName.isEmpty()) {
      return startEmptyJobs();
    }
    return load(jobName);
  }

  /** stop the scheduler after bringing down all running jobs */
  public static void stopJobs() {
    if (jobsInstance != null) {
      jobsInstance.stop();
      jobsInstance = null;
    }
  }

  private static Jobs load(String jobName) {
    if (jobsInstance != null) {
      throw new ApplicationError(
          "Jobs are already running. Bring them down before re-running, or incrmentally add ad-hoc jobs");
    }
    jobsInstance = ComponentManager.getJobs(jobName);
    jobsInstance.start();
    return jobsInstance;
  }

  /** name of this batch. Should match the file name. */
  String name;

  /** module name */
  String moduleName;

  /** default user id */
  String defaultUserId;

  /** jobs to be executed */
  Job[] jobs;

  private ScheduledExecutorService executor;

  private Map<String, ScheduledJob> scheduledJobs = new HashMap<String, ScheduledJob>();

  private ScheduledJob[] polledJobs;
  /** our scheduler for time-of-day jobs */
  private TimeOfDayScheduler scheduler;

  /** execute this batch */
  private void start() {
    if (this.executor != null) {
      throw new ApplicationError(
          "Jobs are already getting executed while another attempt is being made to execute them.");
    }
    this.executor = Application.getScheduledExecutor();
    /*
     * we want jobs to run only when the executor is active. That is,
     * executor is not just a submitter, but manager
     */
    //this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    //this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    //this.executor.setRemoveOnCancelPolicy(true);
    Value userId = this.getUserId();
    List<ScheduledJob> pollers = new ArrayList<ScheduledJob>();
    for (Job job : this.jobs) {
      ScheduledJob sj = job.createScheduledJob(userId);
      this.scheduledJobs.put(job.name, sj);
      boolean needPolling = sj.schedule(this.executor);
      if (needPolling) {
        pollers.add(sj);
      }
    }
    if (pollers.size() > 0) {
      this.polledJobs = pollers.toArray(new ScheduledJob[0]);
      this.scheduler = new TimeOfDayScheduler(this.polledJobs);
      Application.createThread(this.scheduler).start();
    }
  }

  /** bring down all running jobs and shutdown the scheduler */
  private void stop() {
    if (this.scheduler != null) {
      this.scheduler.interrupt(false);
    }
    this.cancelAll();
    if (this.executor != null) {
      try {
        this.executor.shutdownNow();
      } catch (IllegalStateException ignore) {
        //known issue with JBOSS
      }
    }
  }

  /**
   * cancel a job. interrupt it if it is running
   *
   * @param jobName
   */
  public void cancelJob(String jobName) {
    if (jobName == null || jobName.isEmpty()) {
      this.cancelAll();
      return;
    }
    ScheduledJob job = this.scheduledJobs.get(jobName);
    if (job == null) {

      logger.log(Level.INFO, "No job named " + jobName);
      Tracer.trace("No job named " + jobName);
      return;
    }
    job.cancel();
  }

  /** @param jobName */
  public void reschedule(String jobName) {
    if (jobName == null || jobName.isEmpty()) {

      logger.log(Level.INFO, "No job name specified for rescheduling");
      Tracer.trace("No job name specified for rescheduling");
      return;
    }
    ScheduledJob job = this.scheduledJobs.get(jobName);
    if (job == null) {

      logger.log(Level.INFO, "No job named " + jobName);
      Tracer.trace("No job named " + jobName);
      return;
    }
    job.schedule(this.executor);
  }

  /** cancel all jobs */
  public void cancelAll() {
    for (ScheduledJob job : this.scheduledJobs.values()) {
      job.cancel();
    }
  }

  /**
   * add another thread to this job. ignored if this is a batch job
   *
   * @param jobName
   */
  public void incrmentThread(String jobName) {
    ScheduledJob job = this.scheduledJobs.get(jobName);
    if (job == null) {

      logger.log(Level.INFO, "No job named " + jobName);
      Tracer.trace("No job named " + jobName);
      return;
    }
    job.incrmentThread(this.executor);
  }

  /**
   * reduce a thread from this job. ignored if this is a batch job, or if there is only one thread
   * at this time
   *
   * @param jobName
   */
  public void decrmentThread(String jobName) {
    ScheduledJob job = this.scheduledJobs.get(jobName);
    if (job == null) {

      logger.log(Level.INFO, "No job named " + jobName);
      Tracer.trace("No job named " + jobName);
      return;
    }
    job.decrmentThread(this.executor);
  }

  /**
   * get status of all running jobs
   *
   * @return status for all running jobs
   */
  public RunningJobInfo[] getStatus() {
    List<RunningJobInfo> infoList = new ArrayList<RunningJobInfo>();
    for (ScheduledJob job : this.scheduledJobs.values()) {
      job.putStatus(infoList);
    }
    return infoList.toArray(new RunningJobInfo[0]);
  }

  /**
   * @param jobName
   * @return status info for this job
   */
  public RunningJobInfo[] getStatus(String jobName) {
    RunningJobInfo[] inf = new RunningJobInfo[0];
    ScheduledJob job = this.scheduledJobs.get(jobName);
    if (job == null) {

      logger.log(Level.INFO, "No job named " + jobName);
      Tracer.trace("No job named " + jobName);
      return inf;
    }
    List<RunningJobInfo> infoList = new ArrayList<RunningJobInfo>();
    job.putStatus(infoList);
    return infoList.toArray(inf);
  }

  /**
   * ad-hoc running of a job.
   *
   * @param job to be also run. Ensure that its name should not clash with existing jobs
   * @return error message if add failed. null if all OK.
   */
  public String scheduleJob(Job job) {
    if (this.scheduledJobs.containsKey(job.name)) {
      return ("Job named "
          + job.name
          + " is already running. Choose a different name for your job if you insist on running it");
    }
    this.appendJob(job);
    ScheduledJob sjob = job.createScheduledJob(this.getUserId());
    boolean isPolled = sjob.schedule(this.executor);
    this.scheduledJobs.put(job.name, sjob);
    if (isPolled) {
      this.restartScheduler(sjob);
    }
    return null;
  }

  private void appendJob(Job job) {
    int nbr = this.jobs.length;
    Job[] newJobs = new Job[nbr + 1];
    for (int i = 0; i < nbr; i++) {
      newJobs[i] = this.jobs[i];
    }
    newJobs[nbr] = job;
    this.jobs = newJobs;
  }

  /** @param job */
  private void restartScheduler(ScheduledJob job) {
    /*
     * add this job to the polled jobs array
     */
    if (this.polledJobs != null) {
      int nbr = this.polledJobs.length;
      ScheduledJob[] newOnes = new ScheduledJob[nbr + 1];
      newOnes[nbr] = job;
      for (int i = 0; i < this.polledJobs.length; i++) {
        newOnes[i] = this.polledJobs[i];
      }
      this.polledJobs = newOnes;
      this.scheduler.interrupt(false);
    } else {
      this.polledJobs = new ScheduledJob[1];
      this.polledJobs[0] = job;
    }
    this.scheduler = new TimeOfDayScheduler(this.polledJobs);
    Application.createThread(this.scheduler).start();
  }

  /*
   * get default user id
   */
  private Value getUserId() {
    if (this.defaultUserId != null) {
      if (Application.userIdIsNumeric()) {
        try {
          return Value.newIntegerValue(Long.parseLong(this.defaultUserId));
        } catch (Exception e) {
          throw new ApplicationError(
              e,
              " Jobs has specified a non-numeric defaultUserId while application.xml states that userId is to be numeric");
        }
      }
      return Value.newTextValue(this.defaultUserId);
    }
    Value userId = Application.getDefaultUserId();
    if (userId != null) {
      return userId;
    }

    logger.log(
        Level.INFO,
        "Default User Id is not specified either at app level or at Jobs level. If they are indeed required in a service, and the job has not speicified, we will end up using a dummy value of 100");
    Tracer.trace(
        "Default User Id is not specified either at app level or at Jobs level. If they are indeed required in a service, and the job has not speicified, we will end up using a dummy value of 100");
    if (Application.userIdIsNumeric()) {
      return Value.newIntegerValue(100);
    }
    return Value.newTextValue("100");
  }

  /** */
  @Override
  public void getReady() {
    for (Job job : this.jobs) {
      job.getReady();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.comp.Component#getSimpleName()
   */
  @Override
  public String getSimpleName() {
    return this.name;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.comp.Component#getQualifiedName()
   */
  @Override
  public String getQualifiedName() {
    if (this.moduleName == null) {
      return this.name;
    }
    return this.name + '.' + this.moduleName;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
   * ValidationContext)
   */
  @Override
  public int validate(ValidationContext vtx) {
    vtx.beginValidation(ComponentType.JOBS, this.getQualifiedName());
    int count = 0;

    count += vtx.checkMandatoryField("name", this.name);
    if (this.jobs == null || this.jobs.length == 0) {
      vtx.addError("Batch hs no jobs to run");
      count++;
    } else {
      for (Job job : this.jobs) {
        count += job.validate(vtx);
      }
    }
    vtx.endValidation();
    return count;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.comp.Component#getComponentType()
   */
  @Override
  public ComponentType getComponentType() {
    return ComponentType.JOBS;
  }
}
