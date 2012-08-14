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

import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A record of a service that creates and manages receipts.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "service", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "service", namespace = "http://serviceregistry.opencastproject.org")
@Entity(name = "ServiceRegistration")
@Access(AccessType.PROPERTY)
@Table(name = "service_registration", uniqueConstraints = @UniqueConstraint(columnNames = { "host_registration",
        "service_type" }))
@NamedQueries({
        @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT job.processorServiceRegistration as serviceRegistration, job.status, "
                + "count(job.status) as numJobs, "
                + "avg(job.queueTime) as meanQueue, "
                + "avg(job.runTime) as meanRun FROM Job job " + "group by job.processorServiceRegistration, job.status"),
        @NamedQuery(name = "ServiceRegistration.hostload", query = "SELECT job.processorServiceRegistration as serviceRegistration, job.status, count(job.status) as numJobs "
                + "FROM Job job "
                + "WHERE job.processorServiceRegistration.online=true and job.processorServiceRegistration.hostRegistration.maintenanceMode=false "
                + "GROUP BY job.processorServiceRegistration, job.status"),
        @NamedQuery(name = "ServiceRegistration.getRegistration", query = "SELECT r from ServiceRegistration r "
                + "where r.hostRegistration.baseUrl = :host and r.serviceType = :serviceType"),
        @NamedQuery(name = "ServiceRegistration.getAll", query = "SELECT rh FROM ServiceRegistration rh"),
        @NamedQuery(name = "ServiceRegistration.getByHost", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.hostRegistration.baseUrl=:host"),
        @NamedQuery(name = "ServiceRegistration.getByType", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.serviceType=:serviceType"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning_error", query = "SELECT rh FROM ServiceRegistration rh "
                + "WHERE rh.serviceType = :serviceType AND (rh.serviceState = org.opencastproject.serviceregistry.api.ServiceState.WARNING OR "
                + "rh.serviceState = org.opencastproject.serviceregistry.api.ServiceState.ERROR)"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning", query = "SELECT rh FROM ServiceRegistration rh "
                + "WHERE rh.serviceType = :serviceType AND rh.serviceState = org.opencastproject.serviceregistry.api.ServiceState.WARNING") })
public class ServiceRegistrationJpaImpl extends JaxbServiceRegistration {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrationJpaImpl.class);

  /** The primary key */
  private Long id;

  /** The host that provides this service */
  private HostRegistrationJpaImpl hostRegistration;

  /**
   * Creates a new service registration which is online
   */
  public ServiceRegistrationJpaImpl() {
    super();
  }

  /**
   * Creates a new service registration which is online
   * 
   * @param hostRegistration
   *          the host registration
   * @param serviceType
   *          the type of job this service handles
   * @param path
   *          the URL path on this host to the service endpoint
   */
  public ServiceRegistrationJpaImpl(HostRegistrationJpaImpl hostRegistration, String serviceType, String path) {
    super(serviceType, hostRegistration.getBaseUrl(), path);
    this.hostRegistration = hostRegistration;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   * 
   * @param processingHost
   *          the host
   * @param serviceId
   *          the job type
   * @param jobProducer
   */
  public ServiceRegistrationJpaImpl(HostRegistrationJpaImpl hostRegistration, String serviceType, String path,
          boolean jobProducer) {
    super(serviceType, hostRegistration.getBaseUrl(), path, jobProducer);
    this.hostRegistration = hostRegistration;
  }

  /**
   * Gets the primary key for this service registration.
   * 
   * @return the primary key
   */
  @Id
  @Column(name = "id")
  @GeneratedValue
  public Long getId() {
    return id;
  }

  @Column(name = "online_from")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getOnlineFrom() {
    return super.getOnlineFrom();
  }

  /**
   * Sets the primary key identifier.
   * 
   * @param id
   *          the identifier
   */
  public void setId(Long id) {
    this.id = id;
  }

  /** The length was chosen this short because MySQL complains when trying to create an index larger than this */
  @Column(name = "service_type", nullable = false, length = 255)
  @XmlElement(name = "type")
  @Override
  public String getServiceType() {
    return super.getServiceType();
  }

  @Lob
  @Column(name = "path", nullable = false, length = 65535)
  @XmlElement(name = "path")
  @Override
  public String getPath() {
    return super.getPath();
  }

  @Column(name = "service_state")
  @XmlElement(name = "service_state")
  @Override
  public ServiceState getServiceState() {
    return super.getServiceState();
  }

  @Column(name = "state_changed")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "state_changed")
  @Override
  public Date getStateChanged() {
    return super.getStateChanged();
  }

  @Column(name = "warning_state_trigger")
  @XmlElement(name = "warning_state_trigger")
  @Override
  public int getWarningStateTrigger() {
    return warningStateTrigger;
  }

  public void setWarningStateTrigger(int jobSignature) {
    this.warningStateTrigger = jobSignature;
  }

  @Column(name = "error_state_trigger")
  @XmlElement(name = "error_state_trigger")
  @Override
  public int getErrorStateTrigger() {
    return errorStateTrigger;
  }

  public void setErrorStateTrigger(int jobSignature) {
    this.errorStateTrigger = jobSignature;
  }

  @Column(name = "online", nullable = false)
  @XmlElement(name = "online")
  @Override
  public boolean isOnline() {
    return super.isOnline();
  }

  @Column(name = "job_producer", nullable = false)
  @XmlElement(name = "jobproducer")
  @Override
  public boolean isJobProducer() {
    return super.isJobProducer();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isInMaintenanceMode()
   */
  @Transient
  @Override
  public boolean isInMaintenanceMode() {
    return super.maintenanceMode;
  }

  /**
   * Gets the associated {@link HostRegistrationJpaImpl}
   * 
   * @return the host registration
   */
  @ManyToOne
  @JoinColumn(name = "host_registration")
  public HostRegistrationJpaImpl getHostRegistration() {
    return hostRegistration;
  }

  /**
   * @param hostRegistration
   *          the hostRegistration to set
   */
  public void setHostRegistration(HostRegistrationJpaImpl hostRegistration) {
    this.hostRegistration = hostRegistration;
  }

  @PostLoad
  public void postLoad() {
    if (hostRegistration == null) {
      logger.warn("host registration is null");
    } else {
      super.host = hostRegistration.getBaseUrl();
      super.maintenanceMode = hostRegistration.isMaintenanceMode();
      if (!hostRegistration.isOnline())
        super.online = false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return serviceType + "@" + host;
  }

}
