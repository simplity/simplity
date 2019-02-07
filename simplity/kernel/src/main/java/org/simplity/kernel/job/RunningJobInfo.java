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

import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.data.MultiRowsSheet;

/**
 * this is a data-structure with information about a running job
 *
 * @author simplity.org
 */
public class RunningJobInfo {
  /** column names when this data structure is serialized as data sheet */
  public static String[] HEADER = {"jobName", "serviceName", "seqNo", "jobStatus", "serviceStatus"};

  /**
   * @param infoList
   * @return data sheet for all the info objects
   */
  public static IDataSheet toDataSheet(RunningJobInfo[] infoList) {
    int nbr = infoList.length;
    String[][] rows = new String[nbr][];
    for (int i = 0; i < infoList.length; i++) {
      rows[i] = infoList[i].toRow();
    }
    return new MultiRowsSheet(HEADER, rows);
  }

  /** @return a row of values, in the order as in HEADER */
  public String[] toRow() {
    JobStatus sts = JobStatus.SCHEDULED;
    if (this.jobStatus == null) {
      sts = JobStatus.SCHEDULED;
    } else {
      sts = this.jobStatus;
    }
    String[] row = {
      this.jobName,
      this.serviceName,
      "" + this.seqNo,
      sts.toString().toLowerCase(),
      this.serviceStatus
    };
    return row;
  }

  /** name of this job. */
  public final String jobName;
  /** service being executed */
  public final String serviceName;
  /** status of job */
  public final JobStatus jobStatus;
  /** if there is more than one thread running for this job this is the seq no. 0 otherwise. */
  public final int seqNo;
  /** any status emitted by the service. */
  public final String serviceStatus;

  /**
   * constructor to set all values of this data structure
   *
   * @param jobName
   * @param serviceName
   * @param jobStatus
   * @param seqNo
   * @param serviceStatus
   */
  public RunningJobInfo(
      String jobName, String serviceName, JobStatus jobStatus, int seqNo, String serviceStatus) {
    this.jobName = jobName;
    this.jobStatus = jobStatus;
    this.serviceName = serviceName;
    this.seqNo = seqNo;
    this.serviceStatus = serviceStatus;
  }
}
