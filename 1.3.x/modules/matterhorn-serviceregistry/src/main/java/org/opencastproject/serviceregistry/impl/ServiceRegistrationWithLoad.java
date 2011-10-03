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

import java.util.Set;

/**
 * A wrapper around the service registration that is able to keep track of the current load of the host that is running
 * the service.
 */
class ServiceRegistrationWithLoad extends ServiceRegistrationJpaImpl implements Comparable<ServiceRegistrationWithLoad> {

  /** The wrapped instance */
  protected ServiceRegistrationJpaImpl serviceRegistration = null;

  /** The load */
  protected int hostLoad = 0;

  /**
   * Creates a wrapper around the service registration.
   * 
   * @param serviceRegistration
   *          the service registration
   */
  public ServiceRegistrationWithLoad(ServiceRegistrationJpaImpl serviceRegistration) {
    this.serviceRegistration = serviceRegistration;
  }

  /**
   * Sets the load that is currently on the host running this service.
   * 
   * @param load
   *          the load
   */
  public void setHostLoad(int load) {
    this.hostLoad = load;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(ServiceRegistrationWithLoad other) {
    return this.hostLoad - other.hostLoad;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getId()
   */
  @Override
  public Long getId() {
    return serviceRegistration.getId();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    serviceRegistration.setId(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getServiceType()
   */
  @Override
  public String getServiceType() {
    return serviceRegistration.getServiceType();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getPath()
   */
  @Override
  public String getPath() {
    return serviceRegistration.getPath();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#isOnline()
   */
  @Override
  public boolean isOnline() {
    return serviceRegistration.isOnline();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#isJobProducer()
   */
  @Override
  public boolean isJobProducer() {
    return serviceRegistration.isJobProducer();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getCreatorJobs()
   */
  @Override
  public Set<JobJpaImpl> getCreatorJobs() {
    return serviceRegistration.getCreatorJobs();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#setCreatorJobs(java.util.Set)
   */
  @Override
  public void setCreatorJobs(Set<JobJpaImpl> creatorJobs) {
    serviceRegistration.setCreatorJobs(creatorJobs);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getProcessorJobs()
   */
  @Override
  public Set<JobJpaImpl> getProcessorJobs() {
    return serviceRegistration.getProcessorJobs();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#setProcessorJobs(java.util.Set)
   */
  @Override
  public void setProcessorJobs(Set<JobJpaImpl> processorJobs) {
    serviceRegistration.setProcessorJobs(processorJobs);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#isInMaintenanceMode()
   */
  @Override
  public boolean isInMaintenanceMode() {
    return serviceRegistration.isInMaintenanceMode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#getHostRegistration()
   */
  @Override
  public HostRegistration getHostRegistration() {
    return serviceRegistration.getHostRegistration();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#setHostRegistration(org.opencastproject.serviceregistry.impl.HostRegistration)
   */
  @Override
  public void setHostRegistration(HostRegistration hostRegistration) {
    serviceRegistration.setHostRegistration(hostRegistration);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#postLoad()
   */
  @Override
  public void postLoad() {
    serviceRegistration.postLoad();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl#toString()
   */
  @Override
  public String toString() {
    return serviceRegistration.toString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#getHost()
   */
  @Override
  public String getHost() {
    return serviceRegistration.getHost();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setHost(java.lang.String)
   */
  @Override
  public void setHost(String host) {
    serviceRegistration.setHost(host);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setServiceType(java.lang.String)
   */
  @Override
  public void setServiceType(String serviceType) {
    serviceRegistration.setServiceType(serviceType);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setInMaintenanceMode(boolean)
   */
  @Override
  public void setInMaintenanceMode(boolean maintenanceMode) {
    serviceRegistration.setInMaintenanceMode(maintenanceMode);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setOnline(boolean)
   */
  @Override
  public void setOnline(boolean online) {
    serviceRegistration.setOnline(online);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setPath(java.lang.String)
   */
  @Override
  public void setPath(String path) {
    serviceRegistration.setPath(path);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.JaxbServiceRegistration#setJobProducer(boolean)
   */
  @Override
  public void setJobProducer(boolean jobProducer) {
    serviceRegistration.setJobProducer(jobProducer);
  }

}