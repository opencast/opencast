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
package org.opencastproject.publication.engage;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = EngagePublicationServiceImpl.class, property = {
    "service.description=Publication Service (Engage)", "distribution.channel=aws.s3" })
public class AWSS3EngagePublicationServiceImpl extends EngagePublicationServiceImpl {
  @Override
  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
  }

  @Override
  public void setSearchService(SearchService searchService) {
    super.setSearchService(searchService);
  }

  @Override
  @Reference(target = "(distribution.channel=aws.s3)")
  public void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    super.setDownloadDistributionService(downloadDistributionService);
  }

  @Override
  @Reference(target = "(distribution.channel=streaming)")
  public void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    super.setStreamingDistributionService(streamingDistributionService);
  }

  @Override
  @Reference
  public void setWorkspace(Workspace workspace) {
    super.setWorkspace(workspace);
  }

  @Override
  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }
}
