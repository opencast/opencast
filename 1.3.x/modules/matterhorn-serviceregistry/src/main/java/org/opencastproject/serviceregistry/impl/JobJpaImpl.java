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
package org.opencastproject.serviceregistry.impl;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.JaxbJobContext;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A long running, asynchronously executed job. This concrete implementations adds JPA annotations to {@link JaxbJob}.
 */
@Entity(name = "Job")
@Access(AccessType.PROPERTY)
@Table(name = "JOB")
@NamedQueries({
        // Job queries
        @NamedQuery(name = "Job", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType "
                + "order by j.dateCreated"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType order by j.dateCreated"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j "
                + "where j.status = :status order by j.dateCreated"),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j order by j.dateCreated"),
        @NamedQuery(name = "Job.dispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = true and "
                + "j.status=:status order by j.dateCreated"),
        @NamedQuery(name = "Job.processinghost.status", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.processorServiceRegistration.hostRegistration.baseUrl = :host order by j.dateCreated"),
        // Job count queries
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType"),
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
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host")

})
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "job", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org")
public class JobJpaImpl extends JaxbJob {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JobJpaImpl.class);

  /** The service that produced this job */
  protected ServiceRegistrationJpaImpl creatorServiceRegistration;

  /** The service that is processing, or processed, this job */
  protected ServiceRegistrationJpaImpl processorServiceRegistration;

  // @ManyToMany(mappedBy = "root_id", fetch = FetchType.EAGER)
  protected List<JobPropertyJpaImpl> properties;

  /** The job context, to be created after loading by JPA */
  protected JaxbJobContext context = null;

  @JoinColumn(name = "root_id", referencedColumnName = "id", updatable = false)
  protected JobJpaImpl rootJob = null;

  @JoinColumn(name = "parent_id", referencedColumnName = "id", updatable = false)
  protected JobJpaImpl parentJob = null;

  /** Default constructor needed by jaxb and jpa */
  public JobJpaImpl() {
    super();
  }

  /**
   * Constructor with everything needed for a newly instantiated job.
   */
  public JobJpaImpl(User user, Organization organization, ServiceRegistrationJpaImpl creatorServiceRegistration, String operation, List<String> arguments,
          String payload, boolean dispatchable) {
    this();
    this.creator = user.getUserName();
    this.organization = organization.getId();
    this.operation = operation;
    this.context = new JaxbJobContext();
    if (arguments != null) {
      this.arguments = new ArrayList<String>(arguments);
    }
    setPayload(payload);
    setDateCreated(new Date());
    setCreatedHost(creatorServiceRegistration.getHost());
    setJobType(creatorServiceRegistration.getServiceType());
    setDispatchable(dispatchable);
    setStatus(Status.INSTANTIATED);
    this.creatorServiceRegistration = creatorServiceRegistration;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getId()
   */
  @Id
  @GeneratedValue
  @XmlAttribute
  @Override
  public long getId() {
    return id;
  }

  @Override
  @Column(name = "creator", nullable = false)
  @XmlElement(name = "creator")
  public String getCreator() {
    return creator;
  }

  @Override
  @Column(name = "organization", nullable = false)
  @XmlElement(name = "organization")
  public String getOrganization() {
    return organization;
  }

  @Transient
  @XmlElement(name = "url")
  @Override
  public URI getUri() {
    return super.getUri();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getVersion()
   */
  @Column(name = "instance_version")
  @Version
  @XmlAttribute
  @Override
  public long getVersion() {
    return super.getVersion();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getStatus()
   */
  @Column
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getType()
   */
  @Transient
  @XmlAttribute(name = "type")
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getOperation()
   */
  @Column(name = "operation")
  @XmlAttribute
  @Override
  public String getOperation() {
    return operation;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getArguments()
   */
  @Lob
  @Column(name = "argument", length = 2147483647)
  @OrderColumn(name = "listindex")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "JOB_ARG", joinColumns = @JoinColumn(name = "ID", referencedColumnName = "ID"), uniqueConstraints = @UniqueConstraint(columnNames = {
          "ID", "LISTINDEX" }))
  @XmlElement(name = "arg")
  @XmlElementWrapper(name = "args")
  @Override
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getProcessingHost()
   */
  @Transient
  @XmlElement
  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getCreatedHost()
   */
  @Transient
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
  @Column
  @Temporal(TemporalType.TIMESTAMP)
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
  @Column
  @Temporal(TemporalType.TIMESTAMP)
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
  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /**
   * @return the queueTime
   */
  @Column
  @XmlElement
  @Override
  public Long getQueueTime() {
    return queueTime;
  }

  /**
   * @return the runTime
   */
  @Column
  @XmlElement
  @Override
  public Long getRunTime() {
    return runTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getPayload()
   */
  @Lob
  @Column(name = "PAYLOAD", length = 2147483647)
  @XmlElement
  @Override
  public String getPayload() {
    return super.getPayload();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#setPayload(java.lang.String)
   */
  @Override
  public void setPayload(String payload) {
    super.setPayload(payload);
  }

  @Column
  @XmlAttribute
  @Override
  public boolean isDispatchable() {
    return super.dispatchable;
  }

  @Override
  public void setDispatchable(boolean dispatchable) {
    super.setDispatchable(dispatchable);
  }

  /**
   * @return the serviceRegistration where this job was created
   */
  @ManyToOne
  @JoinColumn(name = "creator_svc")
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
      super.setCreatedHost(null);
    } else {
      super.setCreatedHost(creatorServiceRegistration.getHost());
    }
  }

  /**
   * @return the processorServiceRegistration
   */
  @ManyToOne
  @JoinColumn(name = "processor_svc")
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
      super.setProcessingHost(null);
    } else {
      super.setProcessingHost(processorServiceRegistration.getHost());
    }
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
      logger.warn("creator service registration is null");
    } else {
      super.createdHost = creatorServiceRegistration.getHost();
      super.jobType = creatorServiceRegistration.getServiceType();
    }
    if (processorServiceRegistration == null) {
      logger.debug("processor service registration is null");
    } else {
      super.processingHost = creatorServiceRegistration.getHost();
      super.jobType = creatorServiceRegistration.getServiceType();
    }
    context = new JaxbJobContext();
    if (rootJob != null) {
      context.setId(rootJob.getId());
    }
    if (properties != null) {
      for (JobPropertyJpaImpl property : properties) {
        context.getProperties().put(property.getKey(), property.getValue());
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getContext()
   */
  @Transient
  @Override
  public JaxbJobContext getContext() {
    return context;
  }

  /**
   * @return the properties
   */
  @Transient
  // TODO: remove to re-enable job context properties
  public List<JobPropertyJpaImpl> getProperties() {
    return properties;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties(List<JobPropertyJpaImpl> properties) {
    this.properties = properties;
  }

  /**
   * @return the parentJob
   */
  public JobJpaImpl getParentJob() {
    return parentJob;
  }

  /**
   * @param parentJob
   *          the parentJob to set
   */
  public void setParentJob(JobJpaImpl parentJob) {
    this.parentJob = parentJob;
  }

  /**
   * @return the rootJob
   */
  public JobJpaImpl getRootJob() {
    return rootJob;
  }

  /**
   * @param rootJob
   *          the rootJob to set
   */
  public void setRootJob(JobJpaImpl rootJob) {
    this.rootJob = rootJob;
  }
}
