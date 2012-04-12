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
package org.opencastproject.rest;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.ws.rs.FormParam;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Base implementation for job producer REST endpoints.
 */
public abstract class AbstractJobProducerEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AbstractJobProducerEndpoint.class);

  /**
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job)
   */
  @POST
  @Path("/dispatch")
  public Response dispatchJob(@FormParam("job") String jobXml) throws ServiceRegistryException {
    final JobProducer service = getService();
    if (service == null)
      throw new WebApplicationException(Status.PRECONDITION_FAILED);

    final Job job;
    try {
      job = JobParser.parseJob(jobXml);
    } catch (IOException e) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }

    // Try to execute the job.
    if (service.isReadyToAccept(job)) {
      service.acceptJob(job);
      return Response.noContent().build();
    } else {
      logger.debug("Service {} refused to accept job {}", service, job);
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
    }
  }

  @HEAD
  @Path("/dispatch")
  public Response checkHeartbeat() {
    return Response.ok().build();
  }

  /**
   * Returns the job producer that is backing this REST endpoint.
   * 
   * @return the job producer
   */
  public abstract JobProducer getService();

  /**
   * 
   * 
   * @return
   */
  public abstract ServiceRegistry getServiceRegistry();

}
