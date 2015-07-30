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
package org.opencastproject.job.impl.jpa;

import static org.opencastproject.job.api.Job.FailureReason.NONE;

import org.opencastproject.job.api.JaxbJobContext;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * A long running, asynchronously executed job. This concrete implementations adds JPA annotations to {@link JaxbJob}.
 */
@Entity(name = "Job")
@Access(AccessType.FIELD)
@Table(name = "mh_job")
@NamedQueries({
        @NamedQuery(name = "Job", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType "
                + "order by j.dateCreated"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType order by j.dateCreated"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j "
                + "where j.status = :status order by j.dateCreated"),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j order by j.dateCreated"),
        @NamedQuery(name = "Job.dispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = true and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.undispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = false and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.processinghost.status", query = "SELECT j FROM Job j "
                + "where j.status in :statuses and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.processorServiceRegistration.hostRegistration.baseUrl = :host order by j.dateCreated"),
        @NamedQuery(name = "Job.root.children", query = "SELECT j FROM Job j WHERE j.rootJob.id = :id ORDER BY j.dateCreated"),
        @NamedQuery(name = "Job.children", query = "SELECT j FROM Job j WHERE j.parentJob.id = :id ORDER BY j.dateCreated"),
        @NamedQuery(name = "Job.withoutParent", query = "SELECT j FROM Job j WHERE j.parentJob IS NULL"),
        @NamedQuery(name = "Job.avgOperation", query = "SELECT j.operation, AVG(j.runTime), AVG(j.queueTime) FROM Job j GROUP BY j.operation"),

        // Job count queries
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.all", query = "SELECT COUNT(j) FROM Job j"),
        @NamedQuery(name = "Job.count.nullType", query = "SELECT COUNT(j) FROM Job j " + "where j.status = :status"),
        @NamedQuery(name = "Job.count.nullStatus", query = "SELECT COUNT(j) FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host"),
        @NamedQuery(name = "Job.countByOperation", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.operation = :operation and "
                + "j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.fullMonty", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.operation = :operation "
                + "and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host"),
        @NamedQuery(name = "Job.count.history.failed", query = "SELECT COUNT(j) FROM Job j "
                + "WHERE j.status = 4 AND j.processorServiceRegistration IS NOT NULL "
                + "AND j.processorServiceRegistration.serviceType = :serviceType AND j.processorServiceRegistration.hostRegistration.baseUrl = :host "
                + "AND j.dateCompleted >= j.processorServiceRegistration.stateChanged"),
        @NamedQuery(name = "Job.countPerHostService", query = "SELECT h.baseUrl, s.serviceType, j.status, count(j) "
                + "FROM Job j, ServiceRegistration s, HostRegistration h "
                + "WHERE ((j.processorServiceRegistration IS NOT NULL AND j.processorServiceRegistration = s) "
                + "OR (j.creatorServiceRegistration IS NOT NULL AND j.creatorServiceRegistration = s)) "
                + "AND s.hostRegistration = h GROUP BY h.baseUrl, s.serviceType, j.status") })
public class JobJpaImpl implements Job {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JobJpaImpl.class);

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Lob
  @Column(name = "creator", nullable = false, length = 65535)
  private String creator;

  @Lob
  @Column(name = "organization", nullable = false, length = 128)
  private String organization;

  @Transient
  private URI uri;

  @Version
  @Column(name = "instance_version")
  private long version;

  @Column(name = "status")
  private int status;

  @Transient
  private FailureReason failureReason = NONE;

  @Transient
  private String jobType;

  @Lob
  @Column(name = "operation", length = 65535)
  private String operation;

  @Lob
  @Column(name = "argument", length = 2147483647)
  @OrderColumn(name = "argument_index")
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "mh_job_argument", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
  private List<String> arguments;

  @Transient
  private String processingHost;

  @Transient
  private String createdHost;

  @Column(name = "date_completed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  @Column(name = "date_created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "date_started")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateStarted;

  @Column(name = "queue_time")
  private Long queueTime = 0L;

  @Column(name = "run_time")
  private Long runTime = 0L;

  @Lob
  @Column(name = "payload", length = 16777215)
  private String payload;

  @Column(name = "dispatchable")
  private boolean dispatchable;

  @Transient
  private Long parentJobId = -1L;

  @Transient
  private long rootJobId = -1L;

  @ManyToOne
  @JoinColumn(name = "creator_service")
  private ServiceRegistrationJpaImpl creatorServiceRegistration;

  @ManyToOne
  @JoinColumn(name = "processor_service")
  private ServiceRegistrationJpaImpl processorServiceRegistration;

  @Transient
  private List<JobPropertyJpaImpl> properties;

  @JoinColumn(name = "parent", referencedColumnName = "id", nullable = true)
  private JobJpaImpl parentJob = null;

  @Transient
  private JaxbJobContext context = null;

  @OneToOne(fetch = FetchType.LAZY, targetEntity = JobJpaImpl.class, optional = true)
  @JoinColumn(name = "root", referencedColumnName = "id", nullable = true)
  private JobJpaImpl rootJob = null;

  @OneToMany(mappedBy = "parentJob", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH,
          CascadeType.MERGE })
  private List<JobJpaImpl> childJobs = null;

  /** Default constructor needed by JPA */
  public JobJpaImpl() {
  }

  /** Constructor with everything needed for a newly instantiated job. */
  public JobJpaImpl(User user, Organization organization, ServiceRegistrationJpaImpl creatorServiceRegistration,
          String operation, List<String> arguments, String payload, boolean dispatchable) {
    this.creator = user.getUsername();
    this.organization = organization.getId();
    this.operation = operation;
    this.context = new JaxbJobContext();
    this.childJobs = new ArrayList<JobJpaImpl>();
    this.payload = payload;
    this.dateCreated = new Date();
    this.createdHost = creatorServiceRegistration.getHost();
    this.jobType = creatorServiceRegistration.getServiceType();
    this.dispatchable = dispatchable;
    this.status = Status.INSTANTIATED.ordinal();
    this.creatorServiceRegistration = creatorServiceRegistration;
    if (arguments != null) {
      this.arguments = new ArrayList<String>(arguments);
    }
  }

  public JobJpaImpl(User user, Organization organization, ServiceRegistrationJpaImpl creatorServiceRegistration,
          String operation, List<String> arguments, String payload, boolean dispatchable, JobJpaImpl rootJob,
          JobJpaImpl parentJob) {
    this(user, organization, creatorServiceRegistration, operation, arguments, payload, dispatchable);
    this.rootJobId = rootJob.getId();
    this.parentJobId = parentJob.getId();
    this.rootJob = rootJob;
    this.parentJob = parentJob;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public List<JobJpaImpl> getChildJobs() {
    return childJobs;
  }

  public void setChildJobs(List<JobJpaImpl> jobs) {
    this.childJobs = jobs;
  }

  @Override
  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  @Override
  public Status getStatus() {
    return Status.values()[status];
  }

  @Override
  public void setStatus(Status status) {
    this.status = status.ordinal();
  }

  @Override
  public void setStatus(Status status, FailureReason reason) {
    setStatus(status);
    if (reason == null)
      this.failureReason = NONE;
    else
      this.failureReason = reason;
  }

  @Override
  public FailureReason getFailureReason() {
    return failureReason;
  }

  @Override
  public String getJobType() {
    return jobType;
  }

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
    return arguments;
  }

  @Override
  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  public void setProcessingHost(String processingHost) {
    this.processingHost = processingHost;
  }

  @Override
  public String getCreatedHost() {
    return createdHost;
  }

  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  @Override
  public Long getQueueTime() {
    return queueTime;
  }

  public void setQueueTime(Long queueTime) {
    this.queueTime = queueTime;
  }

  @Override
  public Long getRunTime() {
    return runTime;
  }

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
  public boolean isDispatchable() {
    return dispatchable;
  }

  @Override
  public void setDispatchable(boolean dispatchable) {
    this.dispatchable = dispatchable;
  }

  public ServiceRegistrationJpaImpl getCreatorServiceRegistration() {
    return creatorServiceRegistration;
  }

  /**
   * @param serviceRegistration
   *          the serviceRegistration to set
   */
  public void setCreatorServiceRegistration(ServiceRegistrationJpaImpl serviceRegistration) {
    this.creatorServiceRegistration = serviceRegistration;
    if (creatorServiceRegistration == null) {
      this.createdHost = null;
    } else {
      this.createdHost = creatorServiceRegistration.getHost();
    }
  }

  public ServiceRegistrationJpaImpl getProcessorServiceRegistration() {
    return processorServiceRegistration;
  }

  /**
   * @param processorServiceRegistration
   *          the processorServiceRegistration to set
   */
  public void setProcessorServiceRegistration(ServiceRegistrationJpaImpl processorServiceRegistration) {
    this.processorServiceRegistration = processorServiceRegistration;
    if (processorServiceRegistration == null) {
      this.processingHost = null;
    } else {
      this.processingHost = processorServiceRegistration.getHost();
    }
  }

  public long getProcessorServiceRegistrationId() {
    return processorServiceRegistration.getId();
  }

  @PreUpdate
  public void preUpdate() {
    if (properties != null)
      properties.clear();
    else
      properties = new ArrayList<JobPropertyJpaImpl>();
    for (Map.Entry<String, String> entry : context.getProperties().entrySet()) {
      properties.add(new JobPropertyJpaImpl(rootJob, entry.getKey(), entry.getValue()));
    }
  }

  @PostLoad
  public void postLoad() {
    if (payload != null) {
      payload.getBytes(); // force the clob to load
    }
    if (creatorServiceRegistration == null) {
      logger.warn("creator service registration for job '{}' is null", id);
    } else {
      this.createdHost = creatorServiceRegistration.getHost();
      this.jobType = creatorServiceRegistration.getServiceType();
    }
    if (processorServiceRegistration == null) {
      logger.debug("processor service registration for job '{}' is null", id);
    } else {
      this.processingHost = processorServiceRegistration.getHost();
      this.jobType = processorServiceRegistration.getServiceType();
    }
    context = new JaxbJobContext();
    if (rootJob != null) {
      context.setId(rootJob.getId());
      rootJobId = rootJob.getId();
    }
    if (parentJob != null)
      parentJobId = parentJob.getId();
    if (properties != null) {
      for (JobPropertyJpaImpl property : properties) {
        context.getProperties().put(property.getName(), property.getValue());
      }
    }
    if (rootJob != null)
      rootJobId = rootJob.getId();
    if (parentJob != null)
      parentJobId = parentJob.getId();
  }

  @Override
  public JaxbJobContext getContext() {
    return context;
  }

  public List<JobPropertyJpaImpl> getProperties() {
    return properties;
  }

  public void setProperties(List<JobPropertyJpaImpl> properties) {
    this.properties = properties;
  }

  @Override
  public Long getParentJobId() {
    return parentJobId;
  }

  public JobJpaImpl getParentJob() {
    return parentJob;
  }

  public void setParentJob(JobJpaImpl parentJob) {
    if (parentJob == null)
      return;

    this.parentJobId = parentJob.getId();
    this.parentJob = parentJob;
  }

  @Override
  public Long getRootJobId() {
    return rootJobId;
  }

  public JobJpaImpl getRootJob() {
    return rootJob;
  }

  public void setRootJob(JobJpaImpl rootJob) {
    if (rootJob == null)
      return;

    this.rootJobId = rootJob.getId();
    this.rootJob = rootJob;
  }

  @Override
  public int getSignature() {
    if (arguments == null)
      return jobType.hashCode();

    return jobType.hashCode() + arguments.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Job) {
      return ((Job) obj).getId() == id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return "Job {id:" + this.id + ", version:" + version + "}";
  }

  public static JobJpaImpl from(Job job) {
    JobJpaImpl newJob = new JobJpaImpl();
    newJob.dateCompleted = job.getDateCompleted();
    newJob.dateCreated = job.getDateCreated();
    newJob.dateStarted = job.getDateStarted();
    newJob.queueTime = job.getQueueTime();
    newJob.runTime = job.getRunTime();
    newJob.version = job.getVersion();
    newJob.payload = job.getPayload();
    newJob.processingHost = job.getProcessingHost();
    newJob.createdHost = job.getCreatedHost();
    newJob.id = job.getId();
    newJob.jobType = job.getJobType();
    newJob.operation = job.getOperation();
    newJob.arguments = job.getArguments();
    newJob.status = job.getStatus().ordinal();
    newJob.failureReason = job.getFailureReason();
    if (job.getContext() != null)
      newJob.context = new JaxbJobContext(job.getContext());
    newJob.parentJobId = job.getParentJobId();
    newJob.rootJobId = job.getRootJobId();
    newJob.dispatchable = job.isDispatchable();
    newJob.uri = job.getUri();
    newJob.creator = job.getCreator();
    newJob.organization = job.getOrganization();
    return newJob;
  }

}
