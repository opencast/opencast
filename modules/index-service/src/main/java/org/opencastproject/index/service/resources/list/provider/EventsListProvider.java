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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventIndexSchema;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    immediate = true,
    service = ResourceListProvider.class,
    property = {
        "service.description=Events list provider",
        "opencast.service.type=org.opencastproject.index.service.resources.list.provider.EventsListProvider"
    }
)
public class EventsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "EVENTS";

  public static final String CONTRIBUTORS = PROVIDER_PREFIX + ".CONTRIBUTORS";
  public static final String PRESENTERS_BIBLIOGRAPHIC = PROVIDER_PREFIX + ".PRESENTERS_BIBLIOGRAPHIC";
  public static final String PRESENTERS_TECHNICAL = PROVIDER_PREFIX + ".PRESENTERS_TECHNICAL";
  public static final String SUBJECT = PROVIDER_PREFIX + ".SUBJECT";
  public static final String LOCATION = PROVIDER_PREFIX + ".LOCATION";
  public static final String PROGRESS = PROVIDER_PREFIX + ".PROGRESS";
  public static final String STATUS = PROVIDER_PREFIX + ".STATUS";
  public static final String COMMENTS = PROVIDER_PREFIX + ".COMMENTS";
  public static final String PUBLISHER = PROVIDER_PREFIX + ".PUBLISHER";
  public static final String ISPUBLISHED = PROVIDER_PREFIX + ".ISPUBLISHED";

  public enum Comments {
    NONE, OPEN, RESOLVED;
  }

  public enum IsPublished {
    YES, NO;
  }

  private static final String[] NAMES = { PROVIDER_PREFIX, CONTRIBUTORS, PRESENTERS_BIBLIOGRAPHIC, PRESENTERS_TECHNICAL,
          SUBJECT, LOCATION, PROGRESS, STATUS, COMMENTS, PUBLISHER, ISPUBLISHED };

  private static final Logger logger = LoggerFactory.getLogger(EventsListProvider.class);

  private ElasticsearchIndex index;

  @Activate
  protected void activate(BundleContext bundleContext) {
    logger.info("Events list provider activated!");
  }

  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> list = new HashMap<String, String>();

    if (CONTRIBUTORS.equals(listName)) {
      for (String contributor : index.getTermsForField(EventIndexSchema.CONTRIBUTOR, Event.DOCUMENT_TYPE))
        list.put(contributor, contributor);
    } else if (PRESENTERS_BIBLIOGRAPHIC.equals(listName)) {
      for (String presenter : index.getTermsForField(EventIndexSchema.PRESENTER, Event.DOCUMENT_TYPE))
        list.put(presenter, presenter);
    } else if (PRESENTERS_TECHNICAL.equals(listName)) {
      for (String presenter : index.getTermsForField(EventIndexSchema.TECHNICAL_PRESENTERS, Event.DOCUMENT_TYPE))
        list.put(presenter, presenter);
    } else if (LOCATION.equals(listName)) {
      for (String location : index.getTermsForField(EventIndexSchema.LOCATION, Event.DOCUMENT_TYPE))
        list.put(location, location);
    } else if (SUBJECT.equals(listName)) {
      for (String subject : index.getTermsForField(EventIndexSchema.SUBJECT, Event.DOCUMENT_TYPE))
        list.put(subject, subject);
    } else if (PROGRESS.equals(listName)) {
      for (WorkflowState progress : WorkflowState.values())
        list.put(progress.toString(), progress.toString());
    } else if (STATUS.equals(listName)) {
      list.put("EVENTS.EVENTS.STATUS.SCHEDULED", "EVENTS.EVENTS.STATUS.SCHEDULED");
      list.put("EVENTS.EVENTS.STATUS.RECORDING", "EVENTS.EVENTS.STATUS.RECORDING");
      list.put("EVENTS.EVENTS.STATUS.INGESTING", "EVENTS.EVENTS.STATUS.INGESTING");
      list.put("EVENTS.EVENTS.STATUS.PENDING", "EVENTS.EVENTS.STATUS.PENDING");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING", "EVENTS.EVENTS.STATUS.PROCESSING");
      list.put("EVENTS.EVENTS.STATUS.PAUSED", "EVENTS.EVENTS.STATUS.PAUSED");
      list.put("EVENTS.EVENTS.STATUS.PROCESSED", "EVENTS.EVENTS.STATUS.PROCESSED");
      list.put("EVENTS.EVENTS.STATUS.RECORDING_FAILURE", "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING_FAILURE", "EVENTS.EVENTS.STATUS.PROCESSING_FAILURE");
      list.put("EVENTS.EVENTS.STATUS.PROCESSING_CANCELLED", "EVENTS.EVENTS.STATUS.PROCESSING_CANCELLED");
    } else if (COMMENTS.equals(listName)) {
      for (Comments comments : Comments.values())
        list.put(comments.toString(), "FILTERS.EVENTS.COMMENTS." + comments.toString());
    } else if (PUBLISHER.equals(listName)) {
      for (String publisher : index.getTermsForField(EventIndexSchema.PUBLISHER, Event.DOCUMENT_TYPE))
        list.put(publisher, publisher);
    } else if (ISPUBLISHED.equals(listName)) {
      for (IsPublished isPublished : IsPublished.values())
        list.put(isPublished.toString(), "FILTERS.EVENTS.ISPUBLISHED." + isPublished.toString());
    }

    return list;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return STATUS.equals(listName) || COMMENTS.equals(listName);
  }

  @Override
  public String getDefault() {
    return null;
  }
}
