/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.serviceregistry.api;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.fn.juc.Immutables;
import org.opencastproject.fn.juc.Mutables;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.Job;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** Create and record job incidents. Facade for {@link IncidentService}. */
public final class Incidents {
  private static final Log log = Log.mk(Incident.class);

  /**
   * System error codes
   */
  private static final String SYSTEM_UNHANDLED_EXCEPTION = "org.opencastproject.system.unhandled-exception";
  private static final String SYSTEM_JOB_CREATION_EXCEPTION = "org.opencastproject.system.job-creation-exception";
  private static final String SYSTEM_MIGRATED_ERROR = "org.opencastproject.system.migrated-error";

  public static final Map<String, String> NO_PARAMS = Immutables.emtpyMap();
  public static final List<Tuple<String, String>> NO_DETAILS = Immutables.nil();

  private final IncidentService is;
  private final ServiceRegistry sr;

  public Incidents(ServiceRegistry sr, IncidentService is) {
    this.is = is;
    this.sr = sr;
  }

  /**
   * Record an incident for a given job. This method is intended to record client incidents, i.e. incidents crafted by
   * the programmer.
   * 
   * @param code
   *          A code number. This incident factory method enforces an incident code schema of <code>job_type.code</code>
   *          , e.g. <code>org.opencastproject.service.1511</code> . So instead of aligning
   *          <code>job.getJobType()</code> and the incident's code prefix manually this is done automatically for you
   *          by this method. See {@link org.opencastproject.job.api.Incident#getCode()}.
   * @see org.opencastproject.job.api.Incident
   */
  public void record(Job job, Severity severity, int code, Map<String, String> params,
          List<Tuple<String, String>> details) {
    try {
      is.storeIncident(job, new Date(), job.getJobType() + "." + code, severity, params, details);
    } catch (IncidentServiceException e) {
      logException(e);
    }
  }

  /**
   * Record an incident for a given job. This method is intended to record client incidents, i.e. incidents crafted by
   * the programmer.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void record(Job job, Severity severity, int code) {
    record(job, severity, code, NO_PARAMS, NO_DETAILS);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code) {
    record(job, Severity.FAILURE, code, NO_PARAMS, NO_DETAILS);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code, Map<String, String> params) {
    record(job, Severity.FAILURE, code, params, NO_DETAILS);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code, List<Tuple<String, String>> details) {
    record(job, Severity.FAILURE, code, NO_PARAMS, details);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code, Map<String, String> params, List<Tuple<String, String>> details) {
    record(job, Severity.FAILURE, code, params, details);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code, Throwable t, List<Tuple<String, String>> details) {
    recordFailure(job, code, t, NO_PARAMS, details);
  }

  /**
   * Record a failure incident for a given job.
   * 
   * @see #record(org.opencastproject.job.api.Job, org.opencastproject.job.api.Incident.Severity, int, java.util.Map,
   *      java.util.List)
   * @see org.opencastproject.job.api.Incident
   */
  public void recordFailure(Job job, int code, Throwable t, Map<String, String> params,
          List<Tuple<String, String>> details) {
    List<Tuple<String, String>> detailList = Mutables.list(details);
    detailList.add(tuple("stack-trace", ExceptionUtils.getStackTrace(t)));
    record(job, Severity.FAILURE, code, params, detailList);
  }

  public void recordMigrationIncident(Job job, String error) {
    try {
      is.storeIncident(job, new Date(), SYSTEM_MIGRATED_ERROR, Severity.FAILURE, Immutables.map(tuple("error", error)),
              NO_DETAILS);
    } catch (IncidentServiceException e) {
      logException(e);
    }
  }

  public void recordJobCreationIncident(Job job, Throwable t) {
    unhandledException(job, SYSTEM_JOB_CREATION_EXCEPTION, Severity.FAILURE, t);
  }

  /**
   * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
   * the job system itself, e.g. the job dispatcher.
   */
  public void unhandledException(Job job, Severity severity, Throwable t) {
    unhandledException(job, SYSTEM_UNHANDLED_EXCEPTION, severity, t);
  }

  /**
   * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
   * the job system itself, e.g. the job dispatcher. Please note that an incident will <em>only</em> be recorded if none
   * of severity {@link org.opencastproject.job.api.Incident.Severity#FAILURE} has already been recorded by the job or
   * one of its child jobs. If no job with the given job id exists nothing happens.
   */
  public void unhandledException(long jobId, Severity severity, Throwable t) {
    try {
      unhandledException(sr.getJob(jobId), severity, t);
    } catch (NotFoundException ignore) {
    } catch (ServiceRegistryException e) {
      logException(e);
    }
  }

  /**
   * Record an incident for a given job caused by an uncatched exception. This method is intended to record incidents by
   * the job system itself, e.g. the job dispatcher.
   */
  private void unhandledException(Job job, String code, Severity severity, Throwable t) {
    if (!alreadyRecordedFailureIncident(job.getId())) {
      try {
        is.storeIncident(
                job,
                new Date(),
                code,
                severity,
                Immutables.map(tuple("exception", ExceptionUtils.getMessage(t))),
                Immutables.list(tuple("job-type", job.getJobType()), tuple("job-operation", job.getOperation()),
                        tuple("stack-trace", ExceptionUtils.getStackTrace(t))));
      } catch (IncidentServiceException e) {
        logException(e);
      }
    }
  }

  private void logException(Throwable t) {
    log.error(t, "Error recording job incident. Log exception and move on.");
  }

  public boolean alreadyRecordedFailureIncident(long jobId) {
    try {
      return findFailure(is.getIncidentsOfJob(jobId, true));
    } catch (Exception e) {
      return false;
    }
  }

  static boolean findFailure(IncidentTree r) {
    return mlist(r.getIncidents()).exists(isFailure) || mlist(r.getDescendants()).exists(findFailureFn);
  }

  static final Function<IncidentTree, Boolean> findFailureFn = new Function<IncidentTree, Boolean>() {
    @Override
    public Boolean apply(IncidentTree r) {
      return findFailure(r);
    }
  };

  static final Function<Incident, Boolean> isFailure = new Function<Incident, Boolean>() {
    @Override
    public Boolean apply(Incident i) {
      return i.getSeverity() == Severity.FAILURE;
    }
  };
}
