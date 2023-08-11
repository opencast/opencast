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

package org.opencastproject.workflow.handler.coverimage;

import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of {@link CoverImageWorkflowOperationHandlerBase} for usage in an OSGi context
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Cover Image Workflow Operation Handler",
        "workflow.operation=cover-image"
    }
)
public class CoverImageWorkflowOperationHandler extends CoverImageWorkflowOperationHandlerBase {

  /** The cover image service */
  private CoverImageService coverImageService;

  /** The workspace service */
  private Workspace workspace;

  /** Reference to the static metadata service */
  private StaticMetadataService metadataService;

  /** The dublin core catalog service */
  private DublinCoreCatalogService dcService;

  private String karafEtc = null;

  @Activate
  public void activate(BundleContext bundleContext) {
    karafEtc = bundleContext.getProperty("karaf.etc");
  }

  /**
   * OSGi callback to set the cover image service
   *
   * @param coverImageService
   *          an instance of the cover image service
   */
  @Reference
  protected void setCoverImageService(CoverImageService coverImageService) {
    this.coverImageService = coverImageService;
  }

  /**
   * OSGi callback to set the workspace service
   *
   * @param workspace
   *          an instance of the workspace service
   */
  @Reference
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi callback to set the static metadata service
   *
   * @param srv
   *          an instance of the static metadata service
   */
  @Reference(target = "(metadata.source=dublincore)")
  protected void setStaticMetadataService(StaticMetadataService srv) {
    this.metadataService = srv;
  }

  /**
   * OSGi callback to set the dublin core catalog service
   *
   * @param dcService
   *          an instance of the dublin core catalog service
   */
  @Reference
  protected void setDublinCoreCatalogService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

  @Override
  protected CoverImageService getCoverImageService() {
    return coverImageService;
  }

  @Override
  protected Workspace getWorkspace() {
    return workspace;
  }

  @Override
  protected StaticMetadataService getStaticMetadataService() {
    return metadataService;
  }

  @Override
  protected DublinCoreCatalogService getDublinCoreCatalogService() {
    return dcService;
  }

  @Override
  protected String getKarafEtc() {
    return karafEtc;
  }
}
