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

import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public final class EventUtils {
  private static final int CREATED_BY_UI_ORDER = 14;

  public static final Map<String, String> PUBLICATION_CHANNELS = new HashMap<String, String>();

  static {
    PUBLICATION_CHANNELS.put(EngagePublicationChannel.CHANNEL_ID, "EVENTS.EVENTS.DETAILS.GENERAL.ENGAGE");
    PUBLICATION_CHANNELS.put("youtube", "EVENTS.EVENTS.DETAILS.GENERAL.YOUTUBE");
  }

  private EventUtils() {

  }

  /**
   * Loads the metadata for the given event
   *
   * @param event
   *          the source {@link Event}
   * @return a {@link AbstractMetadataCollection} instance with all the event metadata
   */
  @SuppressWarnings("unchecked")
  public static AbstractMetadataCollection getEventMetadata(Event event, EventCatalogUIAdapter eventCatalogUIAdapter)
          throws Exception {
    AbstractMetadataCollection metadata = eventCatalogUIAdapter.getRawFields();

    MetadataField<?> title = metadata.getOutputFields().get("title");
    metadata.removeField(title);
    MetadataField<String> newTitle = MetadataUtils.copyMetadataField(title);
    newTitle.setValue(event.getTitle());
    metadata.addField(newTitle);

    MetadataField<?> subject = metadata.getOutputFields().get("subject");
    metadata.removeField(subject);
    MetadataField<String> newSubject = MetadataUtils.copyMetadataField(subject);
    newSubject.setValue(event.getSubject());
    metadata.addField(newSubject);

    MetadataField<?> description = metadata.getOutputFields().get("description");
    metadata.removeField(description);
    MetadataField<String> newDescription = MetadataUtils.copyMetadataField(description);
    newDescription.setValue(event.getDescription());
    metadata.addField(newDescription);

    MetadataField<?> language = metadata.getOutputFields().get("language");
    metadata.removeField(language);
    MetadataField<String> newLanguage = MetadataUtils.copyMetadataField(language);
    newLanguage.setValue(event.getLanguage());
    metadata.addField(newLanguage);

    MetadataField<?> rightsHolder = metadata.getOutputFields().get("rightsHolder");
    metadata.removeField(rightsHolder);
    MetadataField<String> newRightsHolder = MetadataUtils.copyMetadataField(rightsHolder);
    newRightsHolder.setValue(event.getRights());
    metadata.addField(newRightsHolder);

    MetadataField<?> license = metadata.getOutputFields().get("license");
    metadata.removeField(license);
    MetadataField<String> newLicense = MetadataUtils.copyMetadataField(license);
    newLicense.setValue(event.getLicense());
    metadata.addField(newLicense);

    MetadataField<?> series = metadata.getOutputFields().get("isPartOf");
    metadata.removeField(series);
    MetadataField<String> newSeries = MetadataUtils.copyMetadataField(series);
    newSeries.setValue(event.getSeriesId());
    metadata.addField(newSeries);

    MetadataField<?> presenters = metadata.getOutputFields().get("creator");
    metadata.removeField(presenters);
    MetadataField<String> newPresenters = MetadataUtils.copyMetadataField(presenters);
    newPresenters.setValue(StringUtils.join(event.getPresenters(), ", "));
    metadata.addField(newPresenters);

    MetadataField<?> contributors = metadata.getOutputFields().get("contributor");
    metadata.removeField(contributors);
    MetadataField<String> newContributors = MetadataUtils.copyMetadataField(contributors);
    newContributors.setValue(StringUtils.join(event.getContributors(), ", "));
    metadata.addField(newContributors);

    String recordingStartDate = event.getRecordingStartDate();
    if (StringUtils.isNotBlank(recordingStartDate)) {
      Date startDateTime = new Date(DateTimeSupport.fromUTC(recordingStartDate));

      MetadataField<?> startDate = metadata.getOutputFields().get("startDate");
      metadata.removeField(startDate);
      MetadataField<String> newStartDate = MetadataUtils.copyMetadataField(startDate);
      SimpleDateFormat sdf = new SimpleDateFormat(startDate.getPattern().get());
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      newStartDate.setValue(sdf.format(startDateTime));
      metadata.addField(newStartDate);

      MetadataField<?> startTime = metadata.getOutputFields().get("startTime");
      metadata.removeField(startTime);
      MetadataField<String> newStartTime = MetadataUtils.copyMetadataField(startTime);
      sdf = new SimpleDateFormat(startTime.getPattern().get());
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      newStartTime.setValue(sdf.format(startDateTime));
      metadata.addField(newStartTime);
    }

    if (event.getDuration() != null) {
      MetadataField<?> duration = metadata.getOutputFields().get("duration");
      metadata.removeField(duration);
      MetadataField<String> newDuration = MetadataUtils.copyMetadataField(duration);
      newDuration.setValue(event.getDuration().toString());
      metadata.addField(newDuration);
    }

    MetadataField<?> agent = metadata.getOutputFields().get("location");
    metadata.removeField(agent);
    MetadataField<String> newAgent = MetadataUtils.copyMetadataField(agent);
    newAgent.setValue(event.getLocation());
    metadata.addField(newAgent);

    MetadataField<?> source = metadata.getOutputFields().get("source");
    metadata.removeField(source);
    MetadataField<String> newSource = MetadataUtils.copyMetadataField(source);
    newSource.setValue(event.getSource());
    metadata.addField(newSource);

    // Admin UI only field
    MetadataField<String> createdBy = MetadataField.createTextMetadataField("createdBy", Opt.<String> none(),
            "EVENTS.EVENTS.DETAILS.METADATA.CREATED_BY", true, false, Opt.<Map<String, String>> none(),
            Opt.<String> none(), Opt.some(CREATED_BY_UI_ORDER), Opt.<String> none());
    createdBy.setValue(event.getCreator());
    metadata.addField(createdBy);

    MetadataField<?> created = metadata.getOutputFields().get("created");
    metadata.removeField(created);
    MetadataField<Date> newCreated = MetadataUtils.copyMetadataField(created);
    newCreated.setValue(new Date(DateTimeSupport.fromUTC(event.getCreated())));
    metadata.addField(newCreated);

    MetadataField<?> uid = metadata.getOutputFields().get("identifier");
    metadata.removeField(uid);
    MetadataField<String> newUID = MetadataUtils.copyMetadataField(uid);
    newUID.setValue(event.getIdentifier());
    metadata.addField(newUID);

    return metadata;
  }

  /**
   * A filter to remove all internal channel publications.
   */
  public static final Fn<Publication, Boolean> internalChannelFilter = new Fn<Publication, Boolean>() {
    @Override
    public Boolean ap(Publication a) {
      if (InternalPublicationChannel.CHANNEL_ID.equals(a.getChannel()))
        return false;
      return true;
    }
  };
}
