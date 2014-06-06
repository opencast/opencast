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

import java.util.Date;

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
public class JaxbServiceRegistration implements ServiceRegistration {

  @XmlElement(name = "type")
  protected String serviceType;

  @XmlElement(name = "host")
  protected String host;

  @XmlElement(name = "path")
  protected String path;

  @XmlElement(name = "active")
  protected boolean active;

  @XmlElement(name = "online")
  protected boolean online;

  @XmlElement(name = "maintenance")
  protected boolean maintenanceMode;

  @XmlElement(name = "jobproducer")
  protected boolean jobProducer;

  /** The last time the service has been declared online */
  @XmlElement(name = "onlinefrom")
  protected Date onlineFrom;

  @XmlElement(name = "service_state")
  protected ServiceState serviceState;

  @XmlElement(name = "state_changed")
  protected Date stateChanged;

  @XmlElement(name = "error_state_trigger")
  protected int errorStateTrigger;

  @XmlElement(name = "warning_state_trigger")
  protected int warningStateTrigger;

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   */
  public JaxbServiceRegistration() {
    this.online = true;
    this.active = true;
    this.maintenanceMode = false;
    this.onlineFrom = new Date();
    this.serviceState = ServiceState.NORMAL;
    this.stateChanged = new Date();
  }

  /**
   * Creates a new JAXB annotated service registration based on an existing service registration
   *
   * @param serviceRegistration
   */
  public JaxbServiceRegistration(ServiceRegistration serviceRegistration) {
    this.host = serviceRegistration.getHost();
    this.jobProducer = serviceRegistration.isJobProducer();
    this.maintenanceMode = serviceRegistration.isInMaintenanceMode();
    this.active = serviceRegistration.isActive();
    this.online = serviceRegistration.isOnline();
    this.onlineFrom = serviceRegistration.getOnlineFrom();
    this.path = serviceRegistration.getPath();
    this.serviceType = serviceRegistration.getServiceType();
    this.serviceState = serviceRegistration.getServiceState();
    this.stateChanged = serviceRegistration.getStateChanged();
    this.warningStateTrigger = serviceRegistration.getWarningStateTrigger();
    this.errorStateTrigger = serviceRegistration.getErrorStateTrigger();
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   *
   * @param host
   *          the host
   * @param serviceId
   *          the job type
   */
  public JaxbServiceRegistration(String serviceType, String host, String path) {
    this();
    this.serviceType = serviceType;
    this.host = host;
    this.path = path;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   *
   * @param host
   *          the host
   * @param serviceId
   *          the job type
   * @param jobProducer
   */
  public JaxbServiceRegistration(String serviceType, String host, String path, boolean jobProducer) {
    this();
    this.serviceType = serviceType;
    this.host = host;
    this.path = path;
    this.jobProducer = jobProducer;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getHost()
   */
  @Override
  public String getHost() {
    return host;
  }

  /**
   * @param host
   *          the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getServiceType()
   */
  @Override
  public String getServiceType() {
    return serviceType;
  }

  /**
   * @param serviceType
   *          the serviceType to set
   */
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isInMaintenanceMode()
   */
  @Override
  public boolean isInMaintenanceMode() {
    return maintenanceMode;
  }

  /**
   * Sets the maintenance status of this service registration
   *
   * @param maintenanceMode
   */
  public void setInMaintenanceMode(boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isActive()
   */
  @Override
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isOnline()
   */
  @Override
  public boolean isOnline() {
    return online;
  }

  /**
   * Sets the online status of this service registration
   *
   * @param online
   */
  public void setOnline(boolean online) {
    if (online && !isOnline())
      setOnlineFrom(new Date());
    this.online = online;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getPath()
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * @param path
   *          the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isJobProducer()
   */
  @Override
  public boolean isJobProducer() {
    return jobProducer;
  }

  /**
   * Sets whether this service registration is a job producer.
   *
   * @param jobProducer
   *          the jobProducer to set
   */
  public void setJobProducer(boolean jobProducer) {
    this.jobProducer = jobProducer;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getOnlineFrom()
   */
  @Override
  public Date getOnlineFrom() {
    return onlineFrom;
  }

  /**
   * Sets the last time the service has been declared online
   *
   * @param onlineFrom
   *          the onlineFrom to set
   */
  public void setOnlineFrom(Date onlineFrom) {
    this.onlineFrom = onlineFrom;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getServiceState()
   */
  @Override
  public ServiceState getServiceState() {
    return serviceState;
  }

  /**
   * Sets the current state of the service.
   *
   * @param state
   *          current state
   */
  public void setServiceState(ServiceState state) {
    this.serviceState = state;
  }

  /**
   * Sets the current state of the service and the trigger Job. If the state is set to {@link ServiceState#WARNING} or
   * {@link ServiceState#ERROR} the triggered job will be set.
   *
   * @param state
   *          the service state
   * @param triggerJobSignature
   *          the triggered job signature
   */
  public void setServiceState(ServiceState state, int triggerJobSignature) {

    setServiceState(state);
    setStateChanged(new Date());
    if (state == ServiceState.WARNING) {
      setWarningStateTrigger(triggerJobSignature);
    } else if (state == ServiceState.ERROR) {
      setErrorStateTrigger(triggerJobSignature);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getStateChanged()
   */
  @Override
  public Date getStateChanged() {
    return stateChanged;
  }

  /**
   * Sets the last date when the state was changed
   *
   * @param stateChanged
   *          last date
   */
  public void setStateChanged(Date stateChanged) {
    this.stateChanged = stateChanged;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getErrorStateTrigger()
   */
  @Override
  public int getErrorStateTrigger() {
    return errorStateTrigger;
  }

  /**
   * Sets the job which triggered the last error state
   *
   * @param job
   *          the job
   */
  public void setErrorStateTrigger(int jobSignature) {
    this.errorStateTrigger = jobSignature;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getWarningStateTrigger()
   */
  @Override
  public int getWarningStateTrigger() {
    return warningStateTrigger;
  }

  /**
   * Sets the job which triggered the last warning state
   *
   * @param job
   *          the job
   */
  public void setWarningStateTrigger(int jobSignature) {
    this.warningStateTrigger = jobSignature;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ServiceRegistration))
      return false;
    ServiceRegistration registration = (ServiceRegistration) obj;
    return getHost().equals(registration.getHost()) && getServiceType().equals(registration.getServiceType());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getServiceType() + "@" + getHost();
  }

}
