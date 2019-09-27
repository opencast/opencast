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
package org.opencastproject.transcription.nibity.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.MimeTypes.getMimeType;
import static org.opencastproject.util.RestUtil.fileResponse;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.nibity.NibityTranscriptionService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;


@Path("/")
@RestService(name = "NibityTranscriptionRestService", title = "Transcription Service REST Endpoint (uses Nibity services)", abstractText = "Uses external service to generate transcriptions of recordings.", notes = {
  "All paths above are relative to the REST endpoint base (something like http://your.server/transcripts)"})

public class NibityTranscriptionRestService extends AbstractJobProducerEndpoint {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(NibityTranscriptionRestService.class);

  /**
   * The transcription service
   */
  protected NibityTranscriptionService service;

  /**
   * The service registry
   */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * The WFR
   */
  protected WorkingFileRepository wfr;

  public void activate(ComponentContext cc) {
    logger.debug("activate()");
  }

  public void setTranscriptionService(NibityTranscriptionService service) {
    this.service = service;
  }

  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  public void setWorkingFileRepository(WorkingFileRepository wfr) {
    this.wfr = wfr;
  }


  @Override
  public JobProducer getService() {
    return service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @GET
  @Path("/submission/{fileName}")
  @RestQuery(name = "getSubmission", description = "Gets the file from the working repository under /collectionId/filename", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetSubmission(@PathParam("fileName") String fileName) throws NotFoundException {
    logger.debug("Submission media requested: {}", fileName);
    return fileResponse(wfr.getFileFromCollection(NibityTranscriptionService.SUBMISSION_COLLECTION, fileName), getMimeType(fileName), some(fileName))
            .build();
  }

}
