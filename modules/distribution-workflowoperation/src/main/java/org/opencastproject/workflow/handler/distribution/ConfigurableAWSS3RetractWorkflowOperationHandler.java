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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowOperationHandler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * WOH that retracts elements from an internal distribution channel and removes the reflective publication elements from
 * the media package.
 */
@Component(
    immediate = true,
    name = "org.opencastproject.workflow.handler.distribution.ConfigurableAWSS3RetractWorkflowOperationHandler",
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Configurable Retraction Workflow Handler",
        "workflow.operation=retract-configure-aws"
    }
)
public class ConfigurableAWSS3RetractWorkflowOperationHandler extends ConfigurableRetractWorkflowOperationHandler {

  /** OSGi DI */
  @Reference(
      name = "DownloadDistributionService",
      target = "(distribution.channel=aws.s3)"
  )
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    super.setDownloadDistributionService(distributionService);
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
