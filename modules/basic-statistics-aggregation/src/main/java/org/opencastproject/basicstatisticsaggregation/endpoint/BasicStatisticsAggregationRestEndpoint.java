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
package org.opencastproject.basicstatisticsaggregation.endpoint;

import org.opencastproject.basicstatisticsaggregation.BasicStatisticsAggregationService;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

/**
 * For internal communication between nodes
 */

@Component(
    property = {
        "service.description=Basic Statistics Aggregation REST Endpoint",
        "opencast.service.type=org.opencastproject.basicstatisticssecret",
        "opencast.service.path=/basicstatistics-aggregation",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = BasicStatisticsAggregationRestEndpoint.class
)
@Path("/basicstatistics-aggregation")
@RestService(
    name = "BasicStatisticsAggregationEndpoint",
    title = "Basic Statistics Aggregation Endpoint",
    abstractText = "For internal communication between nodes for Opencasts basic statistics",
    notes = { "" }
)
@JaxrsResource
public class BasicStatisticsAggregationRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsAggregationRestEndpoint.class);

  /** The service */
  protected BasicStatisticsAggregationService basicStatisticsAggregationService;



  @Reference
  public void setBasicStatisticsAggregationService(BasicStatisticsAggregationService service) {
    this.basicStatisticsAggregationService = service;
  }
}
