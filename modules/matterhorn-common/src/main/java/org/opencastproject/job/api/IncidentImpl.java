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
package org.opencastproject.job.api;

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class IncidentImpl implements Incident {
  private final long id;
  private final long jobId;
  private final String serviceType;
  private final String processingHost;
  private final Date timestamp;
  private final Severity severity;
  private final String code;
  private final List<Tuple<String, String>> details;
  private final Map<String, String> parameters;

  public IncidentImpl(long id,
                      long jobId,
                      String serviceType,
                      String processingHost,
                      Date timestamp,
                      Severity severity,
                      String code,
                      List<Tuple<String, String>> details,
                      Map<String, String> parameters) {
    this.id = id;
    this.jobId = jobId;
    this.serviceType = serviceType;
    this.processingHost = processingHost;
    this.timestamp = new Date(timestamp.getTime());
    this.severity = severity;
    this.code = code;
    this.details = Immutables.mk(details);
    this.parameters = Immutables.mk(parameters);
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public long getJobId() {
    return jobId;
  }

  @Override
  public String getServiceType() {
    return serviceType;
  }

  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public Severity getSeverity() {
    return severity;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public List<Tuple<String, String>> getDetails() {
    return details;
  }

  @Override
  public Map<String, String> getDescriptionParameters() {
    return parameters;
  }

  @Override
  public int hashCode() {
    return hash(id, jobId, serviceType, processingHost, timestamp, severity, code, details, parameters);
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Incident && eqFields((Incident) that));
  }

  private boolean eqFields(Incident that) {
    return eq(id, that.getId())
            && eq(jobId, that.getJobId())
            && eq(serviceType, that.getServiceType())
            && eq(processingHost, that.getProcessingHost())
            && eq(timestamp, that.getTimestamp())
            && eq(severity, that.getSeverity())
            && eq(code, that.getCode())
            && eq(details, that.getDetails())
            && eq(parameters, that.getDescriptionParameters());
  }
}
