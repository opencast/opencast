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
package org.opencastproject.job.jpa;

import static org.opencastproject.job.api.Job.FailureReason.NONE;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.FailureReason;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;

import com.entwinemedia.fn.Fn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * A long running, asynchronously executed job.
 */
@Entity(name = "Job")
@Access(AccessType.FIELD)
@Table(name = "oc_job")
@NamedQueries({
        @NamedQuery(name = "Job", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType "
                + "order by j.dateCreated"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType order by j.dateCreated"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j "
                + "where j.status = :status order by j.dateCreated"),
        @NamedQuery(name = "Job.statuses", query = "SELECT j FROM Job j "
                + "where j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j order by j.dateCreated"),
        @NamedQuery(name = "Job.dispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = true and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.dispatchable.status.idfilter", query = "SELECT j.id FROM Job j "
                + "WHERE j.dispatchable = true AND j.status IN :statuses AND j.id IN :jobids ORDER BY j.dateCreated"),
        @NamedQuery(name = "Job.undispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = false and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.payload", query = "SELECT j.payload FROM Job j where j.operation = :operation "
                + "order by j.dateCreated"),
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
        @NamedQuery(name = "Job.countByOperationOnly", query = "SELECT COUNT(j) FROM Job j "
                + "where j.operation = :operation"),
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
public class JpaJob {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaJob.class);

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

  @Version
  @Column(name = "instance_version")
  private long version;

  @Column(name = "status")
  private int status;

  @Lob
  @Column(name = "operation", length = 65535)
  private String operation;

  @Lob
  @Column(name = "argument", length = 2147483647)
  @OrderColumn(name = "argument_index")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "oc_job_argument", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
  private List<String> arguments;

  @Column(name = "date_completed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  @Column(name = "date_created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  public Date getDateStarted() {
    return dateStarted;
  }

  @Column(name = "date_started")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateStarted;

  @Column(name = "queue_time")
  private Long queueTime = 0L;

  @Column(name = "run_time")
  private Long runTime = 0L;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "payload", length = 16777215)
  private String payload;

  @Column(name = "dispatchable")
  private boolean dispatchable;

  @Column(name = "job_load")
  private Float jobLoad;

  @ManyToOne
  @JoinColumn(name = "creator_service")
  private ServiceRegistrationJpaImpl creatorServiceRegistration;

  @ManyToOne
  @JoinColumn(name = "processor_service")
  private ServiceRegistrationJpaImpl processorServiceRegistration;

  @JoinColumn(name = "parent", referencedColumnName = "id", nullable = true)
  private JpaJob parentJob = null;

  @OneToOne(fetch = FetchType.LAZY, targetEntity = JpaJob.class, optional = true)
  @JoinColumn(name = "root", referencedColumnName = "id", nullable = true)
  private JpaJob rootJob = null;

  @OneToMany(mappedBy = "parentJob", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH,
          CascadeType.MERGE })
  private List<JpaJob> childJobs;

  @Transient
  private String createdHost;

  @Transient
  private String processingHost;

  @Transient
  private Long parentJobId = null;

  @Transient
  private Long rootJobId = null;

  @Transient
  private FailureReason failureReason = NONE;

  @Transient
  private String jobType;

  @Transient
  private URI uri;

  public JpaJob() {
  }

  public JpaJob(User currentUser, Organization organization, ServiceRegistrationJpaImpl creatingService,
          String operation, List<String> arguments, String payload, boolean dispatchable, float load) {
    this.creator = currentUser.getUsername();
    this.organization = organization.getId();
    this.creatorServiceRegistration = creatingService;
    this.jobType = creatingService.getServiceType();
    this.operation = operation;
    this.arguments = arguments;
    this.payload = payload;
    this.dispatchable = dispatchable;
    this.jobLoad = load;
    this.status = Status.INSTANTIATED.ordinal();
  }

  public static JpaJob from(Job job) {
    JpaJob newJob = new JpaJob();
    newJob.id = job.getId();
    newJob.dateCompleted = job.getDateCompleted();
    newJob.dateCreated = job.getDateCreated();
    newJob.dateStarted = job.getDateStarted();
    newJob.queueTime = job.getQueueTime();
    newJob.runTime = job.getRunTime();
    newJob.version = job.getVersion();
    newJob.payload = job.getPayload();
    newJob.jobType = job.getJobType();
    newJob.operation = job.getOperation();
    newJob.arguments = job.getArguments();
    newJob.status = job.getStatus().ordinal();
    newJob.parentJobId = job.getParentJobId();
    newJob.rootJobId = job.getRootJobId();
    newJob.dispatchable = job.isDispatchable();
    newJob.uri = job.getUri();
    newJob.creator = job.getCreator();
    newJob.organization = job.getOrganization();
    newJob.jobLoad = job.getJobLoad();
    return newJob;
  }

  public Job toJob() {
    return new JobImpl(id, creator, organization, version, jobType, operation, arguments, Status.values()[status],
            createdHost, processingHost, dateCreated, dateStarted, dateCompleted, queueTime, runTime, payload,
            parentJobId, rootJobId, dispatchable, uri, jobLoad);
  }

  public static Fn<JpaJob, Job> fnToJob() {
    return new Fn<JpaJob, Job>() {
      @Override
      public Job apply(JpaJob jobJpa) {
        return jobJpa.toJob();
      }
    };
  }

  @PostLoad
  public void postLoad() {
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

    if (rootJob != null)
      rootJobId = rootJob.id;
    if (parentJob != null)
      parentJobId = parentJob.id;
  }

  public void setProcessorServiceRegistration(ServiceRegistrationJpaImpl processorServiceRegistration) {
    this.processorServiceRegistration = processorServiceRegistration;
    if (processorServiceRegistration == null) {
      this.processingHost = null;
    } else {
      this.processingHost = processorServiceRegistration.getHost();
    }
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setStatus(Status status) {
    this.status = status.ordinal();
  }

  public void setDispatchable(boolean dispatchable) {
    this.dispatchable = dispatchable;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  public void setQueueTime(long queueTime) {
    this.queueTime = queueTime;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  public void setRunTime(long runTime) {
    this.runTime = runTime;
  }

  public void setParentJob(JpaJob parentJob) {
    this.parentJob = parentJob;
    this.parentJobId = parentJob.id;
  }

  public void setRootJob(JpaJob rootJob) {
    this.rootJob = rootJob;
    this.rootJobId = rootJob.id;
  }

  public void setStatus(Status status, FailureReason failureReason) {
    this.status = status.ordinal();
    this.failureReason = failureReason;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public long getId() {
    return id;
  }

  public ServiceRegistrationJpaImpl getProcessorServiceRegistration() {
    return processorServiceRegistration;
  }

  public String getJobType() {
    return jobType;
  }

  public String getOperation() {
    return operation;
  }

  public Float getJobLoad() {
    return jobLoad;
  }

  public Status getStatus() {
    return Status.values()[status];
  }

  public boolean isDispatchable() {
    return dispatchable;
  }

  public JpaJob getRootJob() {
    return rootJob;
  }

  public JpaJob getParentJob() {
    return parentJob;
  }

  public List<JpaJob> getChildJobs() {
    return childJobs;
  }

  public String getChildJobsString() {
    StringBuilder sb = new StringBuilder();
    for (JpaJob job : getChildJobs()) {
      sb.append(job.getId());
      sb.append(" ");
    }
    return sb.toString();
  }

  public FailureReason getFailureReason() {
    return failureReason;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public Date getDateCompleted() {
    return dateCompleted;
  }

  public String getCreator() {
    return creator;
  }

  public String getOrganization() {
    return organization;
  }

  @Override
  public String toString() {
    return String.format("Job {id:%d, operation:%s, status:%s}", id, operation, getStatus().toString());
  }

}
