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
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

/**
 * A service that creates jobs for long-running operations.
 */
public interface JobProducer {

  /**
   * The type of jobs that this producer creates.
   * 
   * @return the job type
   */
  String getJobType();

  /**
   * Get the number of jobs in a current status on all nodes.
   * 
   * @return Number of jobs in this state
   * @throws ServiceRegistryException
   *           if an error occurs while communicating with the backing data source
   */
  long countJobs(Status status) throws ServiceRegistryException;

  /**
   * Asks the job producer to handle the given job using the provided operation and list of arguments. The
   * implementation of this method <b>must</b> be asynchronous if the processing takes more than a few seconds.
   * 
   * @param job
   *          the job being dispatched
   * @return <code>true</code> if the job was accepted
   * @throws ServiceRegistryException
   *           if the producer was unable to start work as requested
   */
  boolean acceptJob(Job job) throws ServiceRegistryException;

  /**
   * Whether the job can be accepted.
   * 
   * @param job
   *          the job being dispatched
   * @throws ServiceRegistryException
   *           if the producer was unable to start work as requested
   * @return whether the service is ready to accept the job
   */
  boolean isReadyToAccept(Job job) throws ServiceRegistryException;

}
