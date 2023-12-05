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
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowOperationHandler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * WOH that distributes selected elements to an internal distribution channel and adds reflective publication elements
 * to the media package.
 */
@Component(
    immediate = true,
    name = "org.opencastproject.workflow.handler.distribution.ConfigurableAWSS3PublishWorkflowOperationHandler",
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Configurable Publication Workflow Handler",
        "workflow.operation=publish-configure-aws"
    }
)
public class ConfigurableAWSS3PublishWorkflowOperationHandler extends ConfigurablePublishWorkflowOperationHandler {

  /** OSGi DI */
  @Reference(target = "(distribution.channel=aws.s3)")
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    super.setDownloadDistributionService(distributionService);
  }

  @Reference(target = "(distribution.channel=streaming)")
  void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    super.setStreamingDistributionService(streamingDistributionService);
  }

  /** OSGi DI */
  @Reference
  protected void setSecurityService(SecurityService securityService) {
    super.setSecurityService(securityService);
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
