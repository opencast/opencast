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

import org.opencastproject.job.api.JobProducer;

import java.util.Date;

/**
 * Simple implementation of a service registration.
 */
public class ServiceRegistrationInMemoryImpl implements ServiceRegistration {

  /** Service type */
  protected String serviceType = null;

  /** Host that is running the service */
  protected String host = null;

  /** Path to the service */
  protected String path = null;

  /** The service instance */
  protected JobProducer service = null;

  /** True if this service produces jobs */
  protected boolean isJobProducer = true;

  /** True if this service is online */
  protected boolean isOnline = true;

  /** True if this service in in maintenance mode */
  protected boolean isInMaintenanceMode = true;

  /** Date from the last time the service has been put online */
  private Date onlineFrom;

  private ServiceState serviceState;

  /**
   * Creates a new service registration. The service is initially online and not in maintenance mode.
   * 
   * @param type
   *          the service type
   * @param host
   *          the service host
   * @param path
   *          the path to the service
   * @param jobProducer
   *          <code>true</code> if the service is a job producer
   */
  public ServiceRegistrationInMemoryImpl(String type, String host, String path, boolean jobProducer) {
    this.serviceType = type;
    this.host = host;
    this.path = path;
    this.isJobProducer = jobProducer;
  }

  /**
   * Creates a new service registration. The service is initially online and not in maintenance mode.
   * 
   * @param service
   *          the local service instance
   */
  public ServiceRegistrationInMemoryImpl(JobProducer service) {
    this.service = service;
    this.serviceType = service.getJobType();
    this.isJobProducer = true;
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getHost()
   */
  @Override
  public String getHost() {
    return host;
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isJobProducer()
   */
  @Override
  public boolean isJobProducer() {
    return isJobProducer;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isOnline()
   */
  @Override
  public boolean isOnline() {
    return isOnline;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isInMaintenanceMode()
   */
  @Override
  public boolean isInMaintenanceMode() {
    return isInMaintenanceMode;
  }

  /**
   * Sets the service's maintenance mode.
   * 
   * @param maintenance
   *          <code>true</code> if the service is in maintenance mode
   */
  public void setMaintenance(boolean maintenance) {
    this.isInMaintenanceMode = maintenance;
  }

  /**
   * Returns the actual service instance.
   * 
   * @return the service
   */
  public JobProducer getService() {
    return service;
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

  @Override
  public ServiceState getServiceState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Date getStateChanged() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getErrorStateTrigger() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getWarningStateTrigger() {
    // TODO Auto-generated method stub
    return 0;
  }

}
