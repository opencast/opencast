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

package org.opencastproject.index.service.impl.index.event;

import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class RetractionListener implements WorkflowListener {
  private final Logger logger = LoggerFactory.getLogger(RetractionListener.class);
  private final SecurityService securityService;
  private final Map<Long, Retraction> retractions;
  private final IndexService indexService;

  public RetractionListener(IndexService indexService, SecurityService securityService, Map<Long, Retraction> retractions) {
    this.indexService = indexService;
    this.securityService = securityService;
    this.retractions = retractions;
  }

  @Override
  public void stateChanged(WorkflowInstance workflow) {
    if (workflow.getState() != WorkflowInstance.WorkflowState.SUCCEEDED) {
      return;
    }
    if (!retractions.containsKey(workflow.getId())) {
      return;
    }
    MediaPackage mediaPackage = workflow.getMediaPackage();
    if (mediaPackage == null) {
      logger.warn("The retract workflow \"{}\" (id: {}, created by: {}) does not have a media package.",
              workflow.getTitle(), workflow.getId(), workflow.getCreatorName());
    } else if (mediaPackage.getPublications() != null && mediaPackage.getPublications().length > 0) {
      logger.warn("The retract workflow \"{}\" (id: {}, created by: {}, media package {}) "
                      + "has some non-retracted publications, refusing to orphan them.",
              workflow.getTitle(), workflow.getId(), workflow.getCreatorName(), mediaPackage.getIdentifier().compact());
    } else {
      final Retraction retraction = retractions.get(workflow.getId());
      SecurityUtil.runAs(securityService, retraction.getOrganization(), retraction.getUser(), () -> {
        final String mpId = mediaPackage.getIdentifier().compact();
        try {
          if (!indexService.removeEvent(mpId)) {
            logger.warn("Could not delete retracted media package {}. removeEvent returned false.", mpId);
          }
        } catch (UnauthorizedException e) {
          logger.warn("Not authorized to delete retracted media package {}", mpId);
        } catch (NotFoundException e) {
          logger.warn("Unable to delete retracted media package {} because it could not be found", mpId);
          retraction.getDoOnNotFound().run();
        } catch (Exception e) {
          logger.warn("Unable to delete retracted media package {}:", mpId, e);
        }
      });
    }
    retractions.remove(workflow.getId());
  }
}

