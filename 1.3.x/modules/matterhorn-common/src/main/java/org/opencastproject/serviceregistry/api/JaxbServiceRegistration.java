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

  @XmlElement(name = "online")
  protected boolean online;

  @XmlElement(name = "maintenance")
  protected boolean maintenanceMode;

  @XmlElement(name = "jobproducer")
  protected boolean jobProducer;

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   */
  public JaxbServiceRegistration() {
    this.online = true;
    this.maintenanceMode = false;
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
    this.online = serviceRegistration.isOnline();
    this.path = serviceRegistration.getPath();
    this.serviceType = serviceRegistration.getServiceType();
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
    ServiceRegistration registration = (ServiceRegistration)obj;
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
