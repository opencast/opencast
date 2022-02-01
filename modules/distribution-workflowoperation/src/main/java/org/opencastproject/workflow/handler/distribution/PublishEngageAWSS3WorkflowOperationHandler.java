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

package org.opencastproject.workflow.handler.distribution;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowOperationHandler;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The workflow definition for handling "engage publication" operations
 */
@Component(
    immediate = true,
    name = "org.opencastproject.workflow.handler.distribution.PublishEngageAWSS3WorkflowOperationHandler",
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Engage Publication Workflow Handler",
        "workflow.operation=publish-engage-aws"
    }
)
public class PublishEngageAWSS3WorkflowOperationHandler extends PublishEngageWorkflowOperationHandler {

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param downloadDistributionService
   *          the download distribution service
   */
  @Reference(
      name = "DownloadDistributionService",
      target = "(distribution.channel=aws.s3)"
  )
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
  @Reference(name = "SearchService")
  protected void setSearchService(SearchService searchService) {
    super.setSearchService(searchService);
  }

  @Reference(name = "organizationDirectoryService")
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    super.setOrganizationDirectoryService(organizationDirectoryService);
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
