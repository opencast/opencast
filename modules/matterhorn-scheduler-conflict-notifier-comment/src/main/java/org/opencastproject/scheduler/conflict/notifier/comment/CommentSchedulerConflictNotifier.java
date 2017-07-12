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
package org.opencastproject.scheduler.conflict.notifier.comment;

import static org.opencastproject.scheduler.impl.SchedulerUtil.eventOrganizationFilter;
import static org.opencastproject.scheduler.impl.SchedulerUtil.toHumanReadableString;
import static org.opencastproject.scheduler.impl.SchedulerUtil.uiAdapterToFlavor;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.scheduler.api.ConflictNotifier;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.ConflictingEvent;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Comment implementation of a scheduler conflict notifier
 */
public class CommentSchedulerConflictNotifier implements ConflictNotifier {

  private static final Logger logger = LoggerFactory.getLogger(CommentSchedulerConflictNotifier.class);

  private static final String COMMENT_REASON = "conflict";

  /** The event comment service */
  private EventCommentService eventCommentService;

  /** The security service */
  private SecurityService securityService;

  /** The workspace */
  private Workspace workspace;

  /** The list of registered event catalog UI adapters */
  private List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<>();

  /** OSGi callback to add {@link EventCommentService} instance. */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  /** OSGi callback to add {@link SecurityService} instance. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to add {@link Workspace} instance. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi callback to add {@link EventCatalogUIAdapter} instance. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link EventCatalogUIAdapter} instance. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  @Override
  public void notifyConflicts(List<ConflictingEvent> conflicts) {
    for (ConflictingEvent c : conflicts) {
      StringBuilder sb = new StringBuilder(
              "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). ");
      if (Strategy.OLD.equals(c.getConflictStrategy())) {
        sb.append("Find below the new version:").append(CharUtils.LF);
        sb.append(CharUtils.LF);
        sb.append(toHumanReadableString(workspace, getEventCatalogUIAdapterFlavors(), c.getNewEvent()));
      } else {
        sb.append("Find below the preceding version:").append(CharUtils.LF);
        sb.append(CharUtils.LF);
        sb.append(toHumanReadableString(workspace, getEventCatalogUIAdapterFlavors(), c.getOldEvent()));
      }
      try {
        EventComment comment = EventComment.create(Option.<Long> none(), c.getNewEvent().getEventId(),
                securityService.getOrganization().getId(), sb.toString(), securityService.getUser(), COMMENT_REASON,
                false);
        eventCommentService.updateComment(comment);
      } catch (EventCommentException e) {
        logger.error("Unable to create a comment on the event {}: {}", c.getOldEvent().getEventId(),
                ExceptionUtils.getStackTrace(e));
      }
    }
  }

  private List<MediaPackageElementFlavor> getEventCatalogUIAdapterFlavors() {
    final String organization = securityService.getOrganization().getId();
    return Stream.$(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).map(uiAdapterToFlavor)
            .toList();
  }

}
