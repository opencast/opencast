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

import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;

import com.entwinemedia.fn.Fn;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public final class EventUtils {
  /** The logging facility */
  static final Logger logger = LoggerFactory.getLogger(EventUtils.class);

  public static final Map<String, String> PUBLICATION_CHANNELS = new HashMap<String, String>();

  static {
    PUBLICATION_CHANNELS.put(EngagePublicationChannel.CHANNEL_ID, "EVENTS.EVENTS.DETAILS.GENERAL.ENGAGE");
    PUBLICATION_CHANNELS.put("youtube", "EVENTS.EVENTS.DETAILS.GENERAL.YOUTUBE");
    PUBLICATION_CHANNELS.put("engage-live", "EVENTS.EVENTS.DETAILS.GENERAL.ENGAGE_LIVE");
  }

  private EventUtils() {

  }

  /**
   * Loads the metadata for the given event
   *
   * @param event
   *          the source {@link Event}
   * @return a {@link MetadataCollection} instance with all the event metadata
   */
  @SuppressWarnings("unchecked")
  public static MetadataCollection getEventMetadata(Event event, EventCatalogUIAdapter eventCatalogUIAdapter)
          throws Exception {
    MetadataCollection metadata = eventCatalogUIAdapter.getRawFields();

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_TITLE.getLocalName())) {
      MetadataField<?> title = metadata.getOutputFields().get(DublinCore.PROPERTY_TITLE.getLocalName());
      metadata.removeField(title);
      MetadataField<String> newTitle = MetadataUtils.copyMetadataField(title);
      newTitle.setValue(event.getTitle());
      metadata.addField(newTitle);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_SUBJECT.getLocalName())) {
      MetadataField<?> subject = metadata.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
      metadata.removeField(subject);
      MetadataField<String> newSubject = MetadataUtils.copyMetadataField(subject);
      newSubject.setValue(event.getSubject());
      metadata.addField(newSubject);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_DESCRIPTION.getLocalName())) {
      MetadataField<?> description = metadata.getOutputFields().get(DublinCore.PROPERTY_DESCRIPTION.getLocalName());
      metadata.removeField(description);
      MetadataField<String> newDescription = MetadataUtils.copyMetadataField(description);
      newDescription.setValue(event.getDescription());
      metadata.addField(newDescription);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_LANGUAGE.getLocalName())) {
      MetadataField<?> language = metadata.getOutputFields().get(DublinCore.PROPERTY_LANGUAGE.getLocalName());
      metadata.removeField(language);
      MetadataField<String> newLanguage = MetadataUtils.copyMetadataField(language);
      newLanguage.setValue(event.getLanguage());
      metadata.addField(newLanguage);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_RIGHTS_HOLDER.getLocalName())) {
      MetadataField<?> rightsHolder = metadata.getOutputFields().get(DublinCore.PROPERTY_RIGHTS_HOLDER.getLocalName());
      metadata.removeField(rightsHolder);
      MetadataField<String> newRightsHolder = MetadataUtils.copyMetadataField(rightsHolder);
      newRightsHolder.setValue(event.getRights());
      metadata.addField(newRightsHolder);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_LICENSE.getLocalName())) {
      MetadataField<?> license = metadata.getOutputFields().get(DublinCore.PROPERTY_LICENSE.getLocalName());
      metadata.removeField(license);
      MetadataField<String> newLicense = MetadataUtils.copyMetadataField(license);
      newLicense.setValue(event.getLicense());
      metadata.addField(newLicense);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_IS_PART_OF.getLocalName())) {
      MetadataField<?> series = metadata.getOutputFields().get(DublinCore.PROPERTY_IS_PART_OF.getLocalName());
      metadata.removeField(series);
      MetadataField<String> newSeries = MetadataUtils.copyMetadataField(series);
      newSeries.setValue(event.getSeriesId());
      metadata.addField(newSeries);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_CREATOR.getLocalName())) {
      MetadataField<?> presenters = metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName());
      metadata.removeField(presenters);
      MetadataField<String> newPresenters = MetadataUtils.copyMetadataField(presenters);
      newPresenters.setValue(StringUtils.join(event.getPresenters(), ", "));
      metadata.addField(newPresenters);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_CONTRIBUTOR.getLocalName())) {
      MetadataField<?> contributors = metadata.getOutputFields().get(DublinCore.PROPERTY_CONTRIBUTOR.getLocalName());
      metadata.removeField(contributors);
      MetadataField<String> newContributors = MetadataUtils.copyMetadataField(contributors);
      newContributors.setValue(StringUtils.join(event.getContributors(), ", "));
      metadata.addField(newContributors);
    }

    String recordingStartDate = event.getRecordingStartDate();
    if (StringUtils.isNotBlank(recordingStartDate)) {
      Date startDateTime = new Date(DateTimeSupport.fromUTC(recordingStartDate));

      if (metadata.getOutputFields().containsKey("startDate")) {
        MetadataField<?> startDate = metadata.getOutputFields().get("startDate");
        metadata.removeField(startDate);
        MetadataField<String> newStartDate = MetadataUtils.copyMetadataField(startDate);
        SimpleDateFormat sdf = new SimpleDateFormat(startDate.getPattern().get());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        newStartDate.setValue(sdf.format(startDateTime));
        metadata.addField(newStartDate);
      }
    }

    if (event.getDuration() != null) {
      MetadataField<?> duration = metadata.getOutputFields().get("duration");
      metadata.removeField(duration);
      MetadataField<String> newDuration = MetadataUtils.copyMetadataField(duration);
      newDuration.setValue(event.getDuration().toString());
      metadata.addField(newDuration);
    }

    if (metadata.getOutputFields().containsKey("location")) {
      MetadataField<?> agent = metadata.getOutputFields().get("location");
      metadata.removeField(agent);
      MetadataField<String> newAgent = MetadataUtils.copyMetadataField(agent);
      newAgent.setValue(event.getLocation());
      metadata.addField(newAgent);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_SOURCE.getLocalName())) {
      MetadataField<?> source = metadata.getOutputFields().get(DublinCore.PROPERTY_SOURCE.getLocalName());
      metadata.removeField(source);
      MetadataField<String> newSource = MetadataUtils.copyMetadataField(source);
      newSource.setValue(event.getSource());
      metadata.addField(newSource);
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_CREATED.getLocalName())) {
      String createdDate = event.getCreated();
      if (StringUtils.isNotBlank(createdDate)) {
        MetadataField<?> created = metadata.getOutputFields().get(DublinCore.PROPERTY_CREATED.getLocalName());
        metadata.removeField(created);
        MetadataField<Date> newCreated = MetadataUtils.copyMetadataField(created);
        newCreated.setValue(new Date(DateTimeSupport.fromUTC(createdDate)));
        metadata.addField(newCreated);
      }
    }

    if (metadata.getOutputFields().containsKey(DublinCore.PROPERTY_IDENTIFIER.getLocalName())) {
      MetadataField<?> uid = metadata.getOutputFields().get(DublinCore.PROPERTY_IDENTIFIER.getLocalName());
      metadata.removeField(uid);
      MetadataField<String> newUID = MetadataUtils.copyMetadataField(uid);
      newUID.setValue(event.getIdentifier());
      metadata.addField(newUID);
    }

    return metadata;
  }

  /**
   * A filter to remove all internal channel publications.
   */
  public static final Fn<Publication, Boolean> internalChannelFilter = new Fn<Publication, Boolean>() {
    @Override
    public Boolean apply(Publication a) {
      if (InternalPublicationChannel.CHANNEL_ID.equals(a.getChannel()))
        return false;
      return true;
    }
  };
}
