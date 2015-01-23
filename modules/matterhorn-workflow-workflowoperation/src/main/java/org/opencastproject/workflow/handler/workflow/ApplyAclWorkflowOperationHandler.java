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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "distribute" operations
 */
public class ApplyAclWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ApplyAclWorkflowOperationHandler.class);

  /** The authorization service */
  private AuthorizationService authorizationService = null;

  /** The series service */
  private SeriesService seriesService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param authorizationService
   *          the authorization service
   */
  protected void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param seriesService
   *          the series service
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return new TreeMap<String, String>();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running apply-acl workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    String seriesId = mediaPackage.getSeries();
    if (seriesId == null) {
      return createResult(mediaPackage, Action.CONTINUE);
    }
    try {
      AccessControlList acl = seriesService.getSeriesAccessControl(seriesId);
      if (acl != null) {
        authorizationService.setAcl(mediaPackage, AclScope.Series, acl);
      }
      return createResult(mediaPackage, Action.CONTINUE);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

}
