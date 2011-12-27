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

import org.opencastproject.job.api.Job.Status;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class serves as a convenience for services that implement the {@link JobProducer} api to deal with handling long
 * running, asynchronous operations.
 */
public abstract class AbstractJobProducer implements JobProducer {

  /** The logger */
  static final Logger logger = LoggerFactory.getLogger(AbstractJobProducer.class);

  /** The types of job that this producer can handle */
  protected String jobType = null;

  /** To enable threading when dispatching jobs */
  protected ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Creates a new abstract job producer for jobs of the given type.
   * 
   * @param jobType
   *          the job type
   */
  public AbstractJobProducer(String jobType) {
    this.jobType = jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("Status must not be null");
    return getServiceRegistry().count(getJobType(), status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job)
   */
  @Override
  public boolean acceptJob(Job job) throws ServiceRegistryException {
    if (isReadyToAccept(job)) {
      try {
        job.setStatus(Job.Status.RUNNING);
        getServiceRegistry().updateJob(job);
      } catch (NotFoundException e) {
        throw new IllegalStateException(e);
      }
      executor.submit(new JobRunner(job));
      return true;
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
   */
  @Override
  public boolean isReadyToAccept(Job job) throws ServiceRegistryException {
    return true;
  }

  /**
   * Returns a reference to the service registry.
   * 
   * @return the service registry
   */
  protected abstract ServiceRegistry getServiceRegistry();

  /**
   * Returns a reference to the security service
   * 
   * @return the security service
   */
  protected abstract SecurityService getSecurityService();

  /**
   * Returns a reference to the user directory service
   * 
   * @return the user directory service
   */
  protected abstract UserDirectoryService getUserDirectoryService();

  /**
   * Returns a reference to the organization directory service.
   * 
   * @return the organization directory service
   */
  protected abstract OrganizationDirectoryService getOrganizationDirectoryService();

  /**
   * Asks the overriding class to process the arguments using the given operation. The result will be added to the
   * associated job as the payload.
   * 
   * @param job
   *          the job to process
   * 
   * @return the operation result
   * @throws Exception
   */
  protected abstract String process(Job job) throws Exception;

  /**
   * A utility class to run jobs
   */
  class JobRunner implements Callable<Void> {

    /** The job */
    private final Job job;

    /**
     * Constructs a new job runner
     * 
     * @param job
     *          the job to run
     */
    JobRunner(Job job) {
      this.job = job;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public Void call() throws Exception {
      SecurityService securityService = getSecurityService();
      Organization organization = getOrganizationDirectoryService().getOrganization(job.getOrganization());
      try {
        securityService.setOrganization(organization);
        User user = getUserDirectoryService().loadUser(job.getCreator());
        securityService.setUser(user);
        String payload = process(job);
        if (job.getStatus() == Status.FAILED) {
          logger.warn("Error handling operation '{}' of job {}", job.getOperation(), job.getId());
          return null;
        }
        job.setPayload(payload);
        job.setStatus(Status.FINISHED);
      } catch (Exception e) {
        job.setStatus(Status.FAILED);
        if (e instanceof ServiceRegistryException)
          throw (ServiceRegistryException) e;
        logger.warn("Error handling operation '{}': {}", job.getOperation(), e.getMessage());
      } finally {
        try {
          getServiceRegistry().updateJob(job);
        } catch (NotFoundException e) {
          throw new ServiceRegistryException(e);
        } finally {
          securityService.setUser(null);
          securityService.setOrganization(null);
        }
      }
      return null;
    }
  }

}
