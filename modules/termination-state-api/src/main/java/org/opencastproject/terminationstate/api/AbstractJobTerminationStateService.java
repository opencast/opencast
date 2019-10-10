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
package org.opencastproject.terminationstate.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.Log;

import org.slf4j.LoggerFactory;

/** AbstractJobTerminationStateService.
 * Abstract implementation of of TerminationStateService API which checks
 * whether there are any jobs running before changing the termination state from
 * WAIT to READY once a termination notification has been received.
 */
public abstract class AbstractJobTerminationStateService implements TerminationStateService {

  private TerminationState state = TerminationState.NONE;
  private ServiceRegistry serviceRegistry;

  private final Log logger = new Log(LoggerFactory.getLogger(AbstractJobTerminationStateService.class.getName()));

  /**
   * {@inheritDoc}
   */
  @Override
  public void setState(TerminationState state) {
    this.state = state;
    logger.info("Termination state set to {}", state.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TerminationState getState() {
    return state;
  }

  /**
   * Count the current number of jobs this node is processing
   * @return number jobs
   */
  protected long countJobs() throws ServiceRegistryException {
    String host = "";
    long nJobs = 0;

    try {
      host = serviceRegistry.getRegistryHostname();
      nJobs = serviceRegistry.countByHost(null, host, Job.Status.RUNNING);
    } catch (ServiceRegistryException ex) {
      logger.error("Cannot count jobs running on {}", host, ex);
      throw ex;
    }

    return nJobs;
  }

  /**
   * If waiting and no jobs running change the state to ready
   * @return ready to terminate
   */
  protected boolean readyToTerminate() {
    if (state == TerminationState.WAIT) {
      try {
        if (countJobs() == 0) {
          state = TerminationState.READY;
          return true;
        }
      } catch (ServiceRegistryException ex) {
        // Ready to terminate else node state could become permanently stuck as WAITING
        logger.warn("Can't determine number of running Jobs, setting Termination State to READ");
        state = TerminationState.READY;
        return true;
      }
    } else if (state == TerminationState.READY) {
      return true;
    }

    return false;
  }

  /**
   * OSGI dependency injection of service registry
   * @param service ServiceRegistry instance
   */
  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }
}
