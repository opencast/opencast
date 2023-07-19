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

package org.opencastproject.workflow.handler.distribution;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowOperationHandler;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Workflow operation for retracting a media package from the engage player.
 */

@Component(
    immediate = true,
    name = "org.opencastproject.workflow.handler.distribution.RetractEngageAWSS3WorkflowOperationHandler",
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=AWS Retraction Workflow Operation Handler",
        "workflow.operation=retract-engage-aws"
    }
)
public class RetractEngageAWSS3WorkflowOperationHandler extends RetractEngageWorkflowOperationHandler {

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param downloadDistributionService
   *          the download distribution service
   */
  @Reference(target = "(distribution.channel=aws.s3)")
  protected void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    super.setDownloadDistributionService(downloadDistributionService);
  }

  /**
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   *
   * @param searchService
   *          an instance of the search service
   */
  @Reference
  protected void setSearchService(SearchService searchService) {
    super.setSearchService(searchService);
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

  @Override
  @Activate
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }



}
