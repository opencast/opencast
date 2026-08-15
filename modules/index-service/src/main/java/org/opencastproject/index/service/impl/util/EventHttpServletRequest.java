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
package org.opencastproject.index.service.impl.util;

import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;

public class EventHttpServletRequest {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EventHttpServletRequest.class);

  private static final String ACTION_JSON_KEY = "action";
  private static final String ALLOW_JSON_KEY = "allow";
  private static final String METADATA_JSON_KEY = "metadata";
  private static final String ROLE_JSON_KEY = "role";

  private Optional<AccessControlList> acl = Optional.empty();
  private Optional<MediaPackage> mediaPackage = Optional.empty();
  private Optional<MetadataList> metadataList = Optional.empty();
  private Optional<JsonObject> processing = Optional.empty();
  private Optional<JsonObject> source = Optional.empty();
  private Optional<JsonObject> scheduling = Optional.empty();

  public void setAcl(AccessControlList acl) {
    this.acl = Optional.of(acl);
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = Optional.of(mediaPackage);
  }

  public void setMetadataList(MetadataList metadataList) {
    this.metadataList = Optional.of(metadataList);
  }

  public void setProcessing(JsonObject processing) {
    this.processing = Optional.of(processing);
  }

  public void setScheduling(JsonObject scheduling) {
    this.scheduling = Optional.of(scheduling);
  }

  public void setSource(JsonObject source) {
    this.source = Optional.of(source);
  }

  public Optional<AccessControlList> getAcl() {
    return acl;
  }

  public Optional<MediaPackage> getMediaPackage() {
    return mediaPackage;
  }

  public Optional<MetadataList> getMetadataList() {
    return metadataList;
  }

  public Optional<JsonObject> getProcessing() {
    return processing;
  }

  public Optional<JsonObject> getScheduling() {
    return scheduling;
  }

  public Optional<JsonObject> getSource() {
    return source;
  }

  /**
   * Create a {@link EventHttpServletRequest} from a {@link HttpServletRequest} to create a new {@link Event}.
   *
   * @param request
   *          The multipart request that should result in a new {@link Event}
   * @param ingestService
   *          The {@link IngestService} to use to ingest {@link Event} media.
   * @param eventCatalogUIAdapters
   *          The catalog ui adapters to use for getting the event metadata.
   * @param startDatePattern
   *          The pattern to use to parse the start date from the request.
   * @param startTimePattern
   *          The pattern to use to parse the start time from the request.
   * @return An {@link EventHttpServletRequest} populated from the request.
   * @throws IndexServiceException
   *           Thrown if unable to create the event for an internal reason.
   * @throws IllegalArgumentException
   *           Thrown if the multi part request doesn't have the necessary data.
   */
  public static EventHttpServletRequest createFromHttpServletRequest(
          HttpServletRequest request,
          IngestService ingestService,
          List<EventCatalogUIAdapter> eventCatalogUIAdapters,
          String startDatePattern,
          String startTimePattern)
                  throws IndexServiceException {
    EventHttpServletRequest eventHttpServletRequest = new EventHttpServletRequest();
    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        eventHttpServletRequest.setMediaPackage(ingestService.createMediaPackage());
        if (eventHttpServletRequest.getMediaPackage().isEmpty()) {
          throw new IndexServiceException("Unable to create a new mediapackage to store the new event's media.");
        }

        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            setFormField(eventCatalogUIAdapters, eventHttpServletRequest, item, fieldName, startDatePattern,
                startTimePattern);
          } else {
            if (!item.getName().isBlank()) {
              ingestFile(ingestService, eventHttpServletRequest, item);
            } else {
              logger.debug("Skipping field {} due to missing filename", item.getFieldName());
            }
          }
        }
      } else {
        throw new IllegalArgumentException("No multipart content");
      }

      return eventHttpServletRequest;

    } catch (Exception e) {
      throw new IndexServiceException("Unable to parse new event.", e);
    }
  }

  /**
   * Ingest a file from a multi part request for a new event.
   *
   * @param ingestService
   *          The {@link IngestService} to use to ingest the file.
   * @param eventHttpServletRequest
   *          The {@link EventHttpServletRequest} that has the ingest mediapackage.
   * @param item
   *          The representation of the file.
   * @throws MediaPackageException
   *           Thrown if unable to add the track to the mediapackage.
   * @throws IOException
   *           Thrown if unable to upload the file into the mediapackage.
   * @throws IngestException
   *           Thrown if unable to ingest the file.
   */
  private static void ingestFile(IngestService ingestService, EventHttpServletRequest eventHttpServletRequest,
          FileItemStream item) throws MediaPackageException, IOException, IngestException {
    MediaPackage mp = eventHttpServletRequest.getMediaPackage().get();
    if ("presenter".equals(item.getFieldName())) {
      eventHttpServletRequest.setMediaPackage(
              ingestService.addTrack(item.openStream(), item.getName(), MediaPackageElements.PRESENTER_SOURCE, mp));
    } else if ("presentation".equals(item.getFieldName())) {
      eventHttpServletRequest.setMediaPackage(
              ingestService.addTrack(item.openStream(), item.getName(), MediaPackageElements.PRESENTATION_SOURCE, mp));
    } else if ("audio".equals(item.getFieldName())) {
      eventHttpServletRequest.setMediaPackage(ingestService.addTrack(item.openStream(), item.getName(),
              new MediaPackageElementFlavor("presenter-audio", "source"), mp));
    } else {
      logger.warn("Unknown field name found {}", item.getFieldName());
    }
  }

  /**
   * Set a value for creating a new event from a form field.
   *
   * @param eventCatalogUIAdapters
   *          The list of event catalog ui adapters used for loading the metadata for the new event.
   * @param eventHttpServletRequest
   *          The current details of the request that have been loaded.
   * @param item
   *          The content of the field.
   * @param fieldName
   *          The key of the field.
   * @param startDatePattern
   *          The pattern to use to parse the start date from the request.
   * @param startTimePattern
   *          The pattern to use to parse the start time from the request.
   * @throws IOException
   *           Thrown if unable to laod the content of the field.
   * @throws NotFoundException
   *           Thrown if unable to find a metadata catalog or field that matches an input catalog or field.
   */
  private static void setFormField(List<EventCatalogUIAdapter> eventCatalogUIAdapters,
                                   EventHttpServletRequest eventHttpServletRequest,
                                   FileItemStream item,
                                   String fieldName,
                                   String startDatePattern,
                                   String startTimePattern)
                  throws IOException, NotFoundException {
    if (METADATA_JSON_KEY.equals(fieldName)) {
      String metadata = Streams.asString(item.openStream());
      if (StringUtils.isNotEmpty(metadata)) {
        try {
          MetadataList metadataList = deserializeMetadataList(metadata, eventCatalogUIAdapters, startDatePattern,
                  startTimePattern);
          eventHttpServletRequest.setMetadataList(metadataList);
        } catch (IllegalArgumentException e) {
          throw e;
        } catch (JsonParseException e) {
          throw new IllegalArgumentException(String.format("Unable to parse event metadata because: '%s'", e));
        } catch (NotFoundException e) {
          throw e;
        } catch (java.text.ParseException e) {
          throw new IllegalArgumentException(String.format("Unable to parse event metadata because: '%s'", e));
        }
      }
    } else if ("acl".equals(item.getFieldName())) {
      String access = Streams.asString(item.openStream());
      if (StringUtils.isNotEmpty(access)) {
        try {
          AccessControlList acl = deserializeJsonToAcl(access, true);
          eventHttpServletRequest.setAcl(acl);
        } catch (Exception e) {
          logger.warn("Unable to parse acl {}", access);
          throw new IllegalArgumentException("Unable to parse acl");
        }
      }
    } else if ("processing".equals(item.getFieldName())) {
      String processing = Streams.asString(item.openStream());
      if (StringUtils.isNotEmpty(processing)) {
        try {
          eventHttpServletRequest.setProcessing(JsonParser.parseString(processing).getAsJsonObject());
        } catch (Exception e) {
          logger.warn("Unable to parse processing configuration {}", processing);
          throw new IllegalArgumentException("Unable to parse processing configuration");
        }
      }
    } else if ("scheduling".equals(item.getFieldName())) {
      String scheduling = Streams.asString(item.openStream());
      if (StringUtils.isNotEmpty(scheduling)) {
        try {
          eventHttpServletRequest.setScheduling(JsonParser.parseString(scheduling).getAsJsonObject());
        } catch (Exception e) {
          logger.warn("Unable to parse scheduling information {}", scheduling);
          throw new IllegalArgumentException("Unable to parse scheduling information");
        }
      }
    }
  }

  /**
   * Load the details of updating an event.
   *
   * @param event
   *          The event to update.
   * @param request
   *          The multipart request that has the data to load the updated event.
   * @param eventCatalogUIAdapters
   *          The list of catalog ui adapters to use to load the event metadata.
   * @param startDatePattern
   *          The pattern to use to parse the start date from the request.
   * @param startTimePattern
   *          The pattern to use to parse the start time from the request.
   * @return The data for the event update
   * @throws IllegalArgumentException
   *           Thrown if the request to update the event is malformed.
   * @throws IndexServiceException
   *           Thrown if something is unable to load the event data.
   * @throws NotFoundException
   *           Thrown if unable to find a metadata catalog or field that matches an input catalog or field.
   */
  public static EventHttpServletRequest updateFromHttpServletRequest(
          Event event,
          HttpServletRequest request,
          List<EventCatalogUIAdapter> eventCatalogUIAdapters,
          String startDatePattern,
          String startTimePattern)
                  throws IllegalArgumentException, IndexServiceException, NotFoundException {
    EventHttpServletRequest eventHttpServletRequest = new EventHttpServletRequest();
    if (ServletFileUpload.isMultipartContent(request)) {
      try {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            setFormField(eventCatalogUIAdapters, eventHttpServletRequest, item, fieldName, startDatePattern,
                startTimePattern);
          }
        }
      } catch (IOException e) {
        throw new IndexServiceException("Unable to update event", e);
      } catch (FileUploadException e) {
        throw new IndexServiceException("Unable to update event", e);
      }
    } else {
      throw new IllegalArgumentException("No multipart content");
    }
    return eventHttpServletRequest;
  }

  /**
   * De-serialize an JSON into an {@link AccessControlList}.
   *
   * @param json
   *          The {@link AccessControlList} to serialize.
   * @param assumeAllow
   *          Assume that all entries are allows.
   * @return An {@link AccessControlList} representation of the Json
   * @throws JsonParseException
   */
  /**
   * Render a value as plain text rather than as JSON, i.e. without the quotes a JsonElement's toString() would add,
   * and as the empty string when the member is absent or null. This matches how the previous parser, which handed
   * back bare Java values, was used here.
   */
  private static String asPlainString(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return "";
    }
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }

  /** Join subject values with commas, using the plain text of each entry rather than its JSON representation. */
  private static String joinSubjects(JsonArray subjects) {
    return StreamSupport.stream(subjects.spliterator(), false)
        .map(EventHttpServletRequest::asPlainString)
        .collect(Collectors.joining(","));
  }

  protected static AccessControlList deserializeJsonToAcl(String json, boolean assumeAllow) {
    JsonArray aclJson = JsonParser.parseString(json).getAsJsonArray();
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    for (JsonElement element : aclJson) {
      JsonObject aceJson = element.getAsJsonObject();
      String action = asPlainString(aceJson.get(ACTION_JSON_KEY));
      String allow;
      if (assumeAllow) {
        allow = "true";
      } else {
        allow = asPlainString(aceJson.get(ALLOW_JSON_KEY));
      }
      String role = asPlainString(aceJson.get(ROLE_JSON_KEY));
      if (StringUtils.trimToNull(action) != null && StringUtils.trimToNull(allow) != null
              && StringUtils.trimToNull(role) != null) {
        AccessControlEntry ace = new AccessControlEntry(role, action, Boolean.parseBoolean(allow));
        entries.add(ace);
      } else {
        throw new IllegalArgumentException(String.format(
                "One of the access control elements is missing a property. The action was '%s', allow was '%s' and "
                    + "the role was '%s'",
                action, allow, role));
      }
    }
    return new AccessControlList(entries);
  }

  /**
   * Change the simplified fields of key values provided to the external api into a {@link MetadataList}.
   *
   * @param json
   *          The json string that contains an array of metadata field lists for the different catalogs.
   * @param startDatePattern
   *          The pattern to use to parse the start date from the json payload.
   * @param startTimePattern
   *          The pattern to use to parse the start time from the json payload.
   * @return A {@link MetadataList} with the fields populated with the values provided.
   * @throws JsonParseException
   *           Thrown if unable to parse the json string.
   * @throws NotFoundException
   *           Thrown if unable to find the catalog or field that the json refers to.
   */
  protected static MetadataList deserializeMetadataList(
          String json,
          List<EventCatalogUIAdapter> catalogAdapters,
          String startDatePattern,
          String startTimePattern)
          throws NotFoundException, java.text.ParseException {
    MetadataList metadataList = new MetadataList();
    JsonArray jsonCatalogs = JsonParser.parseString(json).getAsJsonArray();
    for (JsonElement catalogElement : jsonCatalogs) {
      JsonObject catalog = catalogElement.getAsJsonObject();
      if (StringUtils.isBlank(asPlainString(catalog.get("flavor")))) {
        throw new IllegalArgumentException(
                "Unable to create new event as no flavor was given for one of the metadata collections");
      }
      String flavorString = asPlainString(catalog.get("flavor"));
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

      DublinCoreMetadataCollection collection = null;
      EventCatalogUIAdapter adapter = null;
      for (EventCatalogUIAdapter eventCatalogUIAdapter : catalogAdapters) {
        if (eventCatalogUIAdapter.getFlavor().equals(flavor)) {
          adapter = eventCatalogUIAdapter;
          collection = eventCatalogUIAdapter.getRawFields();
        }
      }

      if (collection == null) {
        throw new IllegalArgumentException(
                String.format("Unable to find an EventCatalogUIAdapter with Flavor '%s'", flavorString));
      }

      String fieldsJson = asPlainString(catalog.get("fields"));
      if (StringUtils.trimToNull(fieldsJson) != null) {
        Map<String, String> fields = RequestUtils.getKeyValueMap(fieldsJson);
        for (String key : fields.keySet()) {
          if ("subjects".equals(key)) {
            // Handle the special case of allowing subjects to be an array.
            MetadataField field = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id 'subject' from Catalog with Flavor '%s'.", flavorString));
            }
            collection.removeField(field);
            try {
              JsonArray subjects = JsonParser.parseString(fields.get(key)).getAsJsonArray();
              collection.addField(MetadataJson
                      .copyWithDifferentJsonValue(field, joinSubjects(subjects)));
            } catch (JsonParseException e) {
              throw new IllegalArgumentException(
                      String.format("Unable to parse the 'subjects' metadata array field because: %s", e.toString()));
            }
          } else if ("startDate".equals(key)) {
            // Special handling for start date since in API v1 we expect start date and start time to be separate
            // fields.
            MetadataField field = collection.getOutputFields().get(key);
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(startDatePattern == null
                ? field.getPattern() : startDatePattern);
            SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern());
            DateTime newStartDate = new DateTime(apiSdf.parse(fields.get(key)), DateTimeZone.UTC);
            if (field.getValue() != null) {
              DateTime oldStartDate = new DateTime(sdf.parse((String) field.getValue()), DateTimeZone.UTC);
              newStartDate = oldStartDate.withDate(newStartDate.year().get(), newStartDate.monthOfYear().get(),
                  newStartDate.dayOfMonth().get());
            }
            collection.removeField(field);
            collection.addField(MetadataJson.copyWithDifferentJsonValue(field, sdf.format(newStartDate.toDate())));
          } else if ("startTime".equals(key)) {
            // Special handling for start time since in API v1 we expect start date and start time to be separate
            // fields.
            MetadataField field = collection.getOutputFields().get("startDate");
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", "startDate",
                  flavorString));
            }
            SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(startTimePattern == null
                ? "HH:mm" : startTimePattern);
            SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern());
            DateTime newStartDate = new DateTime(apiSdf.parse(fields.get(key)), DateTimeZone.UTC);
            if (field.getValue() != null) {
              DateTime oldStartDate = new DateTime(sdf.parse((String) field.getValue()), DateTimeZone.UTC);
              newStartDate = oldStartDate.withTime(
                      newStartDate.hourOfDay().get(),
                      newStartDate.minuteOfHour().get(),
                      newStartDate.secondOfMinute().get(),
                      newStartDate.millisOfSecond().get());
            }
            collection.removeField(field);
            collection.addField(MetadataJson.copyWithDifferentJsonValue(field, sdf.format(newStartDate.toDate())));
          } else {
            MetadataField field = collection.getOutputFields().get(key);
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            collection.removeField(field);
            collection.addField(MetadataJson.copyWithDifferentJsonValue(field, fields.get(key)));
          }
        }
      }
      metadataList.add(adapter, collection);
    }
    setStartDateAndTimeIfUnset(metadataList);
    return metadataList;
  }

  /**
   * Set the start date and time to the current date & time if it hasn't been set through the api call.
   *
   * @param metadataList
   *          The metadata list created from the json request to create a new event
   */
  private static void setStartDateAndTimeIfUnset(MetadataList metadataList) {
    final DublinCoreMetadataCollection commonEventCollection = metadataList
            .getMetadataByFlavor(MediaPackageElements.EPISODE.toString());
    if (commonEventCollection != null) {
      MetadataField startDate = commonEventCollection.getOutputFields().get("startDate");
      if (!startDate.isUpdated()) {
        SimpleDateFormat utcDateFormat = new SimpleDateFormat(startDate.getPattern());
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentDate = utcDateFormat.format(new DateTime(DateTimeZone.UTC).toDate());
        commonEventCollection.removeField(startDate);
        commonEventCollection.addField(MetadataJson.copyWithDifferentJsonValue(startDate, currentDate));
      }
    }
  }
}
