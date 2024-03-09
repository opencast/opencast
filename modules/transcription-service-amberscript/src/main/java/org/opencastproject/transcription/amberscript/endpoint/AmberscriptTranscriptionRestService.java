/*
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
package org.opencastproject.transcription.amberscript.endpoint;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.amberscript.AmberscriptTranscriptionService;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;


@Path("/transcripts/amberscript")
@RestService(
    name = "AmberscriptTranscriptionRestService",
    title = "Transcription Service REST Endpoint (uses Amberscript services)",
    abstractText = "Uses external service to generate transcriptions of recordings.",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/transcripts)",
    }
)

@Component(
    immediate = true,
    service = AmberscriptTranscriptionRestService.class,
    property = {
        "service.description=AmberScript Transcription REST Endpoint",
        "opencast.service.type=org.opencastproject.transcription.amberscript",
        "opencast.service.path=/transcripts/amberscript",
        "opencast.service.jobproducer=true"
    }
)
public class AmberscriptTranscriptionRestService extends AbstractJobProducerEndpoint {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(AmberscriptTranscriptionRestService.class);

  /**
   * The transcription service
   */
  protected AmberscriptTranscriptionService service;

  /**
   * The service registry
   */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * The WFR
   */
  protected WorkingFileRepository wfr;

  @Activate
  public void activate() {
    logger.debug("activate()");
  }

  @Reference
  public void setTranscriptionService(AmberscriptTranscriptionService service) {
    this.service = service;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  @Reference
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

}
