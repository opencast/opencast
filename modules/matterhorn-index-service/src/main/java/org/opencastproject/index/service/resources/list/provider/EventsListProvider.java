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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.api.EventIndex;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.Organization;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EventsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "EVENTS";

  public static final String CONTRIBUTORS = PROVIDER_PREFIX + ".CONTRIBUTORS";
  public static final String PRESENTERS = PROVIDER_PREFIX + ".PRESENTERS";
  public static final String SUBJECT = PROVIDER_PREFIX + ".SUBJECT";
  public static final String LOCATION = PROVIDER_PREFIX + ".LOCATION";
  public static final String START_DATE = PROVIDER_PREFIX + ".START_DATE";
  public static final String PROGRESS = PROVIDER_PREFIX + ".PROGRESS";
  public static final String STATUS = PROVIDER_PREFIX + ".STATUS";
  public static final String REVIEW_STATUS = PROVIDER_PREFIX + ".REVIEW_STATUS";
  public static final String COMMENTS = PROVIDER_PREFIX + ".COMMENTS";

  public enum Comments {
    NONE, OPEN, RESOLVED;
  }

  private static final String[] NAMES = { PROVIDER_PREFIX, CONTRIBUTORS, PRESENTERS, SUBJECT, LOCATION, PROGRESS,
          STATUS, REVIEW_STATUS, COMMENTS };

  private static final Logger logger = LoggerFactory.getLogger(EventsListProvider.class);

  private EventIndex index;

  protected void activate(BundleContext bundleContext) {
    logger.info("Events list provider activated!");
  }

  public void setIndex(EventIndex index) {
    this.index = index;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> list = new HashMap<String, Object>();

    if (CONTRIBUTORS.equals(listName)) {
      for (String contributor : index.getEventContributors())
        list.put(contributor, contributor);
    } else if (PRESENTERS.equals(listName)) {
      for (String presenter : index.getEventPresenters())
        list.put(presenter, presenter);
    } else if (LOCATION.equals(listName)) {
      for (String location : index.getEventLocations())
        list.put(location, location);
    } else if (SUBJECT.equals(listName)) {
      for (String subject : index.getEventSubjects())
        list.put(subject, subject);
    } else if (PROGRESS.equals(listName)) {
      for (WorkflowState progress : WorkflowState.values())
        list.put(progress.toString(), progress.toString());
    } else if (STATUS.equals(listName)) {
      list.put("EVENTS.EVENTS.STATUS.SCHEDULED", "EVENTS.EVENTS.STATUS.SCHEDULED");
      list.put("EVENTS.EVENTS.STATUS.OPTEDOUT", "EVENTS.EVENTS.STATUS.OPTEDOUT");
      list.put("EVENTS.EVENTS.STATUS.BLACKLISTED", "EVENTS.EVENTS.STATUS.BLACKLISTED");
      list.put("EVENTS.EVENTS.STATUS.RECORDING", "EVENTS.EVENTS.STATUS.RECORDING");
      list.put("EVENTS.EVENTS.STATUS.INGESTING", "EVENTS.EVENTS.STATUS.INGESTING");
      list.put("EVENTS.EVENTS.STATUS.PENDING", "EVENTS.EVENTS.STATUS.PENDING");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING", "EVENTS.EVENTS.STATUS.PROCESSING");
      list.put("EVENTS.EVENTS.STATUS.PAUSED", "EVENTS.EVENTS.STATUS.PAUSED");
      list.put("EVENTS.EVENTS.STATUS.PROCESSED", "EVENTS.EVENTS.STATUS.PROCESSED");
      list.put("EVENTS.EVENTS.STATUS.RECORDING_FAILURE", "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING_FAILURE", "EVENTS.EVENTS.STATUS.PROCESSING_FAILURE");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING_CANCELED", "EVENTS.EVENTS.STATUS.PROCESSING_CANCELED");
    } else if (REVIEW_STATUS.equals(listName)) {
      for (ReviewStatus status : ReviewStatus.values())
        list.put(status.toString(), status.toString());
    } else if (COMMENTS.equals(listName)) {
      for (Comments comments : Comments.values())
        list.put(comments.toString(), comments.toString());
    }

    return list;
  }
}
