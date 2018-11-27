/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.job.api;

import static java.util.Collections.unmodifiableList;
import static org.opencastproject.job.api.Job.FailureReason.NONE;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JobImpl implements Job {

  private long id;
  private String creator;
  private String organization;
  private long version;
  private String jobType;
  private String operation;
  private List<String> arguments = new ArrayList<>();
  private Status status;
  private FailureReason failureReason = NONE;
  private String createdHost;
  private String processingHost;
  private Date dateCreated;
  private Date dateStarted;
  private Date dateCompleted;
  private Long queueTime = 0L;
  private Long runTime = 0L;
  private String payload;
  private Long parentJobId = null;
  private Long rootJobId = null;
  private boolean dispatchable = true;
  private URI uri;
  private Float load = 1.0F;

  public JobImpl() { }

  public JobImpl(long id) {
    this.id = id;
  }

  public JobImpl(
          long id,
          String creator,
          String organization,
          long version,
          String jobType,
          String operation,
          List<String> arguments,
          Status status,
          String createdHost,
          String processingHost,
          Date dateCreated,
          Date dateStarted,
          Date dateCompleted,
          Long queueTime,
          Long runTime,
          String payload,
          Long parentJobId,
          Long rootJobId,
          boolean dispatchable,
          URI uri,
          Float load) {
    this.id = id;
    this.creator = creator;
    this.organization = organization;
    this.version = version;
    this.jobType = jobType;
    this.operation = operation;
    if (arguments != null)
      this.arguments.addAll(arguments);
    this.status = status;
    this.createdHost = createdHost;
    this.processingHost = processingHost;
    this.dateCreated = dateCreated;
    this.dateStarted = dateStarted;
    this.dateCompleted = dateCompleted;
    this.queueTime = queueTime;
    this.runTime = runTime;
    this.payload = payload;
    this.parentJobId = parentJobId;
    this.rootJobId = rootJobId;
    this.dispatchable = dispatchable;
    this.uri = uri;
    this.load = load;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getCreator() {
    return creator;
  }

  @Override
  public void setCreator(String creator) {
    this.creator = creator;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  @Override
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public String getJobType() {
    return jobType;
  }

  @Override
  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  @Override
  public String getOperation() {
    return operation;
  }

  @Override
  public void setOperation(String operation) {
    this.operation = operation;
  }

  @Override
  public List<String> getArguments() {
    return unmodifiableList(arguments);
  }

  @Override
  public void setArguments(List<String> arguments) {
    if (arguments != null)
      this.arguments = unmodifiableList(arguments);
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public void setStatus(Status status, FailureReason reason) {
    setStatus(status);
    this.failureReason = reason;
  }

  @Override
  public FailureReason getFailureReason() {
    return failureReason;
  }

  @Override
  public String getCreatedHost() {
    return createdHost;
  }

  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  @Override
  public void setProcessingHost(String processingHost) {
    this.processingHost = processingHost;
  }

  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  @Override
  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  @Override
  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  @Override
  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  @Override
  public Long getQueueTime() {
    return queueTime;
  }

  @Override
  public void setQueueTime(Long queueTime) {
    this.queueTime = queueTime;
  }

  @Override
  public Long getRunTime() {
    return runTime;
  }

  @Override
  public void setRunTime(Long runTime) {
    this.runTime = runTime;
  }

  @Override
  public String getPayload() {
    return payload;
  }

  @Override
  public void setPayload(String payload) {
    this.payload = payload;
  }

  @Override
  public int getSignature() {
    if (arguments == null)
      return jobType.hashCode();

    return jobType.hashCode() + arguments.hashCode();
  }

  @Override
  public Long getParentJobId() {
    return parentJobId;
  }

  @Override
  public void setParentJobId(Long parentJobId) {
    this.parentJobId = parentJobId;
  }

  @Override
  public Long getRootJobId() {
    return rootJobId;
  }

  @Override
  public boolean isDispatchable() {
    return dispatchable;
  }

  @Override
  public void setDispatchable(boolean dispatchable) {
   this.dispatchable = dispatchable;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public Float getJobLoad() {
    return load;
  }

  @Override
  public void setJobLoad(Float load) {
    this.load = load;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    JobImpl job = (JobImpl) o;

    return new EqualsBuilder().append(id, job.id).append(dispatchable, job.dispatchable)
            .append(creator, job.creator).append(organization, job.organization)
            .append(jobType, job.jobType).append(operation, job.operation).append(arguments, job.arguments)
            .append(status, job.status).append(failureReason, job.failureReason).append(createdHost, job.createdHost)
            .append(processingHost, job.processingHost).append(dateCreated, job.dateCreated)
            .append(dateStarted, job.dateStarted).append(dateCompleted, job.dateCompleted)
            .append(queueTime, job.queueTime).append(runTime, job.runTime).append(payload, job.payload)
            .append(parentJobId, job.parentJobId).append(rootJobId, job.rootJobId)
            .append(uri, job.uri).append(load, job.load).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(id).append(creator).append(organization).append(jobType)
            .append(operation).append(arguments).append(status).append(failureReason).append(createdHost)
            .append(processingHost).append(dateCreated).append(dateStarted).append(dateCompleted).append(queueTime)
            .append(runTime).append(payload).append(parentJobId).append(rootJobId).append(dispatchable).append(uri)
            .append(load).toHashCode();
  }

  @Override
  public String toString() {
    return String.format("Job {id:%d, operation:%s, status:%s}", id, operation, status.toString());
  }
}
