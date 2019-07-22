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

package org.opencastproject.serviceregistry.impl.jpa;

import org.opencastproject.serviceregistry.api.ServiceRegistration;
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

/**
 * A record of a service that creates and manages receipts.
 */
@Entity(name = "ServiceRegistration")
@Access(AccessType.FIELD)
@Table(name = "oc_service_registration", uniqueConstraints = @UniqueConstraint(columnNames = { "host_registration",
        "service_type" }))
@NamedQueries({
        @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT job.processorServiceRegistration.id as serviceRegistration, job.status, "
                + "count(job.status) as numJobs, "
                + "avg(job.queueTime) as meanQueue, "
                + "avg(job.runTime) as meanRun FROM Job job "
                + "where job.dateCreated >= :minDateCreated and job.dateCreated <= :maxDateCreated "
                + "group by job.processorServiceRegistration.id, job.status"),
        @NamedQuery(name = "ServiceRegistration.hostloads", query = "SELECT job.processorServiceRegistration.hostRegistration.baseUrl as host, job.status, sum(job.jobLoad), job.processorServiceRegistration.hostRegistration.maxLoad "
                + "FROM Job job "
                + "WHERE job.processorServiceRegistration.online=true and job.processorServiceRegistration.active=true and job.processorServiceRegistration.hostRegistration.maintenanceMode=false "
                + "AND job.status in :statuses "
                + "AND job.creatorServiceRegistration.serviceType != :workflow_type "
                + "GROUP BY job.processorServiceRegistration.hostRegistration.baseUrl, job.status"),
        @NamedQuery(name = "ServiceRegistration.getRegistration", query = "SELECT r from ServiceRegistration r "
                + "where r.hostRegistration.baseUrl = :host and r.serviceType = :serviceType"),
        @NamedQuery(name = "ServiceRegistration.getAll", query = "SELECT rh FROM ServiceRegistration rh WHERE rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getAllOnline", query = "SELECT rh FROM ServiceRegistration rh WHERE rh.hostRegistration.online=true AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getByHost", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.hostRegistration.baseUrl=:host AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getByType", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.serviceType=:serviceType AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning_error", query = "SELECT rh FROM ServiceRegistration rh "
                + "WHERE rh.serviceType = :serviceType AND (rh.serviceState = 1 OR rh.serviceState = 2)"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning", query = "SELECT rh FROM ServiceRegistration rh "
                + "WHERE rh.serviceType = :serviceType AND rh.serviceState = 1"),
        @NamedQuery(name = "ServiceRegistration.countNotNormal", query = "SELECT count(rh) FROM ServiceRegistration rh "
                + "WHERE rh.serviceState <> 0 AND rh.hostRegistration.active = true") })
public class ServiceRegistrationJpaImpl implements ServiceRegistration {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrationJpaImpl.class);

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "online_from")
  @Temporal(TemporalType.TIMESTAMP)
  private Date onlineFrom = new Date();

  @Column(name = "service_type", nullable = false, length = 255)
  private String serviceType;

  @ManyToOne
  @JoinColumn(name = "host_registration")
  private HostRegistrationJpaImpl hostRegistration;

  @Lob
  @Column(name = "path", nullable = false, length = 255)
  private String path;

  @Column(name = "service_state")
  private int serviceState = ServiceState.NORMAL.ordinal();

  @Column(name = "state_changed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date stateChanged = new Date();

  @Column(name = "warning_state_trigger")
  private int warningStateTrigger;

  @Column(name = "error_state_trigger")
  private int errorStateTrigger;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "online", nullable = false)
  private boolean online = true;

  @Column(name = "job_producer", nullable = false)
  private boolean isJobProducer;

  @Transient
  private boolean maintenanceMode = false;

  @Transient
  private String host;

  /**
   * Creates a new service registration which is online
   */
  public ServiceRegistrationJpaImpl() {
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
    this.hostRegistration = hostRegistration;
    this.serviceType = serviceType;
    this.path = path;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   *
   * @param hostRegistration
   *          the host registration
   * @param serviceType
   *          the type of job this service handles
   * @param path
   *          the URL path on this host to the service endpoint
   * @param jobProducer
   */
  public ServiceRegistrationJpaImpl(HostRegistrationJpaImpl hostRegistration, String serviceType, String path,
          boolean jobProducer) {
    this.hostRegistration = hostRegistration;
    this.host = hostRegistration.getBaseUrl();
    this.serviceType = serviceType;
    this.path = path;
    this.isJobProducer = jobProducer;
  }

  /**
   * Gets the primary key for this service registration.
   *
   * @return the primary key
   */
  public Long getId() {
    return id;
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

  @Override
  public Date getOnlineFrom() {
    return onlineFrom;
  }

  public void setOnlineFrom(Date onlineFrom) {
    this.onlineFrom = onlineFrom;
  }

  @Override
  public String getServiceType() {
    return serviceType;
  }

  @Override
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public ServiceState getServiceState() {
    return ServiceState.values()[serviceState];
  }

  public void setServiceState(ServiceState serviceState) {
    this.serviceState = serviceState.ordinal();
  }

  public void setServiceState(ServiceState state, int triggerJobSignature) {
    setServiceState(state);
    setStateChanged(new Date());
    if (state == ServiceState.WARNING) {
      setWarningStateTrigger(triggerJobSignature);
    } else if (state == ServiceState.ERROR) {
      setErrorStateTrigger(triggerJobSignature);
    }
  }

  @Override
  public Date getStateChanged() {
    return stateChanged;
  }

  public void setStateChanged(Date stateChanged) {
    this.stateChanged = stateChanged;
  }

  @Override
  public int getWarningStateTrigger() {
    return warningStateTrigger;
  }

  public void setWarningStateTrigger(int warningStateTrigger) {
    this.warningStateTrigger = warningStateTrigger;
  }

  @Override
  public int getErrorStateTrigger() {
    return errorStateTrigger;
  }

  public void setErrorStateTrigger(int errorStateTrigger) {
    this.errorStateTrigger = errorStateTrigger;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean isActive) {
    this.active = isActive;
  }

  @Override
  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    if (online && !isOnline())
      setOnlineFrom(new Date());
    this.online = online;
  }

  @Override
  public boolean isJobProducer() {
    return isJobProducer;
  }

  public void setJobProducer(boolean isJobProducer) {
    this.isJobProducer = isJobProducer;
  }

  @Override
  public boolean isInMaintenanceMode() {
    return maintenanceMode;
  }

  @Override
  public String getHost() {
    return host;
  }

  /**
   * Gets the associated {@link HostRegistrationJpaImpl}
   *
   * @return the host registration
   */
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
      host = hostRegistration.getBaseUrl();
      maintenanceMode = hostRegistration.isMaintenanceMode();
      if (!hostRegistration.isOnline())
        online = false;
      if (!hostRegistration.isActive())
        active = false;
    }
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ServiceRegistration))
      return false;
    ServiceRegistration registration = (ServiceRegistration) obj;
    return getHost().equals(registration.getHost()) && getServiceType().equals(registration.getServiceType());
  }

  @Override
  public String toString() {
    return getServiceType() + "@" + getHost();
  }

}
