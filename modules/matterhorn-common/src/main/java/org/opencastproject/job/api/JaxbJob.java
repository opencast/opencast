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

import static org.opencastproject.job.api.Job.FailureReason.NONE;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A long running, asynchronously executed job.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "job", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org")
public class JaxbJob implements Job {

  /** The job ID */
  protected long id;

  /** The user that created this job */
  protected String creator;

  /** The creator's organization */
  protected String organization;

  /** The version, used for optimistic locking */
  protected long version;

  /** The job type */
  protected String jobType;

  /** The job location */
  protected URI uri;

  /** The operation type */
  protected String operation;

  /** The arguments passed to the service operation */
  protected List<String> arguments;

  /** The server that created this job. */
  protected String createdHost;

  /** The server that is or was processing this job. Null if the job has not yet started. */
  protected String processingHost;

  /** The job status */
  protected Status status;

  /** The failure reason */
  protected FailureReason failureReason = NONE;

  /** The date this job was created */
  protected Date dateCreated;

  /** The date this job was started */
  protected Date dateStarted;

  /** The date this job was completed */
  protected Date dateCompleted;

  /** The parent job identifier */
  protected Long parentJobId = -1L;

  /** The root job identifier */
  protected Long rootJobId = -1L;

  /** The job context */
  protected JaxbJobContext context;

  /** The queue time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long queueTime = 0L;

  /** The run time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long runTime = 0L;

  /** The output produced by this job, or null if it has not yet been generated (or was not due to an exception) */
  // @XmlJavaTypeAdapter(value = CdataAdapter.class)
  protected String payload;

  /** Whether this job is queueable */
  protected boolean dispatchable;

  /** Default constructor needed by jaxb */
  public JaxbJob() {
    this.context = new JaxbJobContext();
  }

  /**
   * Constructs a JaxbJob with a specific identifier
   * 
   * @param id
   *          the job id
   */
  public JaxbJob(Long id) {
    this();
    this.id = id;
  }

  /**
   * Constructs a JaxbJob from an existing job
   * 
   * @param job
   *          the job to use as a template for constructing this JaxbJob
   */
  public JaxbJob(Job job) {
    this();
    this.dateCompleted = job.getDateCompleted();
    this.dateCreated = job.getDateCreated();
    this.dateStarted = job.getDateStarted();
    this.queueTime = job.getQueueTime();
    this.runTime = job.getRunTime();
    this.version = job.getVersion();
    this.payload = job.getPayload();
    this.processingHost = job.getProcessingHost();
    this.createdHost = job.getCreatedHost();
    this.id = job.getId();
    this.jobType = job.getJobType();
    this.operation = job.getOperation();
    this.arguments = job.getArguments();
    this.status = job.getStatus();
    this.failureReason = job.getFailureReason();
    if (job.getContext() != null)
      this.context = new JaxbJobContext(job.getContext());
    this.parentJobId = job.getParentJobId();
    this.rootJobId = job.getRootJobId();
    this.dispatchable = job.isDispatchable();
    this.uri = job.getUri();
    this.creator = job.getCreator();
    this.organization = job.getOrganization();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getId()
   */
  @XmlAttribute
  @Override
  public long getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getVersion()
   */
  @XmlAttribute
  @Override
  public long getVersion() {
    return version;
  }

  /**
   * @param version
   *          the version to set
   */
  public void setVersion(long version) {
    this.version = version;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setId(long)
   */
  @Override
  public void setId(long id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getStatus()
   */
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setStatus(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setStatus(org.opencastproject.job.api.Job.Status,
   *      org.opencastproject.job.api.Job.FailureReason)
   */
  @Override
  public void setStatus(Status status, FailureReason reason) {
    setStatus(status);
    if (reason == null)
      this.failureReason = NONE;
    else
      this.failureReason = reason;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getFailureReason()
   */
  @XmlTransient
  @Override
  public FailureReason getFailureReason() {
    return failureReason;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getType()
   */
  @XmlAttribute(name = "type")
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * Sets the job type
   * 
   * @param jobType
   *          the job type
   */
  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getOperation()
   */
  @XmlElement
  @Override
  public String getOperation() {
    return operation;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setOperation(java.lang.String)
   */
  @Override
  public void setOperation(String operation) {
    this.operation = operation;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getArguments()
   */
  @XmlElement(name = "arg")
  @XmlElementWrapper(name = "args")
  @Override
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * @param arguments
   *          the arguments to set
   */
  @Override
  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getProcessingHost()
   */
  @XmlElement
  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  /**
   * Sets the host url
   * 
   * @param processingHost
   *          the host's base URL
   */
  public void setProcessingHost(String processingHost) {
    this.processingHost = processingHost;
  }

  /**
   * @param createdHost
   *          the createdHost to set
   */
  public void setCreatedHost(String createdHost) {
    this.createdHost = createdHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getCreatedHost()
   */
  @XmlElement
  @Override
  public String getCreatedHost() {
    return createdHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateCompleted()
   */
  @XmlElement
  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateCreated()
   */
  @XmlElement
  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateStarted()
   */
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /**
   * @return the queueTime
   */
  @XmlElement
  @Override
  public Long getQueueTime() {
    return queueTime;
  }

  /**
   * @param queueTime
   *          the queueTime to set
   */
  public void setQueueTime(Long queueTime) {
    this.queueTime = queueTime;
  }

  /**
   * @return the runTime
   */
  @XmlElement
  @Override
  public Long getRunTime() {
    return runTime;
  }

  /**
   * @param runTime
   *          the runTime to set
   */
  public void setRunTime(Long runTime) {
    this.runTime = runTime;
  }

  /**
   * @param dateCreated
   *          the dateCreated to set
   */
  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  /**
   * @param dateStarted
   *          the dateStarted to set
   */
  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  /**
   * @param dateCompleted
   *          the dateCompleted to set
   */
  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getPayload()
   */
  @XmlElement
  @Override
  public String getPayload() {
    return payload;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setPayload(java.lang.String)
   */
  @Override
  public void setPayload(String payload) {
    this.payload = payload;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getContext()
   */
  @XmlElement
  @Override
  public JaxbJobContext getContext() {
    return context;
  }

  /**
   * Sets the job context.
   * 
   * @param context
   *          the context to set
   */
  public void setContext(JaxbJobContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getParentJobId()
   */
  @XmlElement
  @Override
  public Long getParentJobId() {
    return parentJobId;
  }

  /**
   * @param parentJobId
   *          the parentJobId to set
   */
  public void setParentJobId(Long parentJobId) {
    this.parentJobId = parentJobId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getRootJobId()
   */
  @XmlElement
  @Override
  public Long getRootJobId() {
    return rootJobId;
  }

  /**
   * @param rootJobId
   *          the rootJobId to set
   */
  public void setRootJobId(Long rootJobId) {
    this.rootJobId = rootJobId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#isDispatchable()
   */
  @Override
  @XmlAttribute
  public boolean isDispatchable() {
    return dispatchable;
  }

  @Override
  public void setDispatchable(boolean dispatchable) {
    this.dispatchable = dispatchable;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getUri()
   */
  @Override
  @XmlElement(name = "url")
  public URI getUri() {
    return uri;
  }

  /**
   * Sets the URI.
   * 
   * @param uri
   *          the uri to set
   */
  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getCreator()
   */
  @Override
  @XmlElement(name = "creator")
  public String getCreator() {
    return creator;
  }

  /**
   * Sets the user that created this job.
   * 
   * @param creator
   *          the creator
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getOrganization()
   */
  @Override
  @XmlElement(name = "organization")
  public String getOrganization() {
    return organization;
  }

  /**
   * Sets the organization that this job is associated with.
   * 
   * @param organization
   *          the organization
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public int getSignature() {
    if (arguments == null)
      return jobType.hashCode();

    return jobType.hashCode() + arguments.hashCode();

  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Job) {
      return ((Job) obj).getId() == id;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int) id >> 32;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Job {id:" + this.id + ", version:" + version + "}";
  }

}
