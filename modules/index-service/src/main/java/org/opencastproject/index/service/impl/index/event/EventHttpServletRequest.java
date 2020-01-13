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

import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

public class EventHttpServletRequest {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EventHttpServletRequest.class);

  private static final String ACTION_JSON_KEY = "action";
  private static final String ALLOW_JSON_KEY = "allow";
  private static final String ID_JSON_KEY = "id";
  private static final String METADATA_JSON_KEY = "metadata";
  private static final String ROLE_JSON_KEY = "role";
  private static final String VALUE_JSON_KEY = "value";

  private Opt<AccessControlList> acl = Opt.none();
  private Opt<MediaPackage> mediaPackage = Opt.none();
  private Opt<MetadataList> metadataList = Opt.none();
  private Opt<JSONObject> processing = Opt.none();
  private Opt<JSONObject> source = Opt.none();
  private Opt<JSONObject> scheduling = Opt.none();

  public void setAcl(AccessControlList acl) {
    this.acl = Opt.some(acl);
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = Opt.some(mediaPackage);
  }

  public void setMetadataList(MetadataList metadataList) {
    this.metadataList = Opt.some(metadataList);
  }

  public void setProcessing(JSONObject processing) {
    this.processing = Opt.some(processing);
  }

  public void setScheduling(JSONObject scheduling) {
    this.scheduling = Opt.some(scheduling);
  }

  public void setSource(JSONObject source) {
    this.source = Opt.some(source);
  }

  public Opt<AccessControlList> getAcl() {
    return acl;
  }

  public Opt<MediaPackage> getMediaPackage() {
    return mediaPackage;
  }

  public Opt<MetadataList> getMetadataList() {
    return metadataList;
  }

  public Opt<JSONObject> getProcessing() {
    return processing;
  }

  public Opt<JSONObject> getScheduling() {
    return scheduling;
  }

  public Opt<JSONObject> getSource() {
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
          Opt<String> startDatePattern,
          Opt<String> startTimePattern)
                  throws IndexServiceException {
    EventHttpServletRequest eventHttpServletRequest = new EventHttpServletRequest();
    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        eventHttpServletRequest.setMediaPackage(ingestService.createMediaPackage());
        if (eventHttpServletRequest.getMediaPackage().isNone()) {
          throw new IndexServiceException("Unable to create a new mediapackage to store the new event's media.");
        }

        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            setFormField(eventCatalogUIAdapters, eventHttpServletRequest, item, fieldName, startDatePattern, startTimePattern);
          } else {
            ingestFile(ingestService, eventHttpServletRequest, item);
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
                                   Opt<String> startDatePattern,
                                   Opt<String> startTimePattern)
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
        } catch (ParseException e) {
          throw new IllegalArgumentException(String.format("Unable to parse event metadata because: '%s'", e.toString()));
        } catch (NotFoundException e) {
          throw e;
        } catch (java.text.ParseException e) {
          throw new IllegalArgumentException(String.format("Unable to parse event metadata because: '%s'", e.toString()));
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
        JSONParser parser = new JSONParser();
        try {
          eventHttpServletRequest.setProcessing((JSONObject) parser.parse(processing));
        } catch (Exception e) {
          logger.warn("Unable to parse processing configuration {}", processing);
          throw new IllegalArgumentException("Unable to parse processing configuration");
        }
      }
    } else if ("scheduling".equals(item.getFieldName())) {
      String scheduling = Streams.asString(item.openStream());
      if (StringUtils.isNotEmpty(scheduling)) {
        JSONParser parser = new JSONParser();
        try {
          eventHttpServletRequest.setScheduling((JSONObject) parser.parse(scheduling));
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
          Opt<String> startDatePattern,
          Opt<String> startTimePattern)
                  throws IllegalArgumentException, IndexServiceException, NotFoundException {
    EventHttpServletRequest eventHttpServletRequest = new EventHttpServletRequest();
    if (ServletFileUpload.isMultipartContent(request)) {
      try {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            setFormField(eventCatalogUIAdapters, eventHttpServletRequest, item, fieldName, startDatePattern, startTimePattern);
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
   * @throws ParseException
   */
  protected static AccessControlList deserializeJsonToAcl(String json, boolean assumeAllow) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONArray aclJson = (JSONArray) parser.parse(json);
    @SuppressWarnings("unchecked")
    ListIterator<Object> iterator = aclJson.listIterator();
    JSONObject aceJson;
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    while (iterator.hasNext()) {
      aceJson = (JSONObject) iterator.next();
      String action = aceJson.get(ACTION_JSON_KEY) != null ? aceJson.get(ACTION_JSON_KEY).toString() : "";
      String allow;
      if (assumeAllow) {
        allow = "true";
      } else {
        allow = aceJson.get(ALLOW_JSON_KEY) != null ? aceJson.get(ALLOW_JSON_KEY).toString() : "";
      }
      String role = aceJson.get(ROLE_JSON_KEY) != null ? aceJson.get(ROLE_JSON_KEY).toString() : "";
      if (StringUtils.trimToNull(action) != null && StringUtils.trimToNull(allow) != null
              && StringUtils.trimToNull(role) != null) {
        AccessControlEntry ace = new AccessControlEntry(role, action, Boolean.parseBoolean(allow));
        entries.add(ace);
      } else {
        throw new IllegalArgumentException(String.format(
                "One of the access control elements is missing a property. The action was '%s', allow was '%s' and the role was '%s'",
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
   * @throws ParseException
   *           Thrown if unable to parse the json string.
   * @throws NotFoundException
   *           Thrown if unable to find the catalog or field that the json refers to.
   */
  protected static MetadataList deserializeMetadataList(String json, List<EventCatalogUIAdapter> catalogAdapters, Opt<String> startDatePattern, Opt<String> startTimePattern)
          throws ParseException, NotFoundException, java.text.ParseException {
    MetadataList metadataList = new MetadataList();
    JSONParser parser = new JSONParser();
    JSONArray jsonCatalogs = (JSONArray) parser.parse(json);
    for (int i = 0; i < jsonCatalogs.size(); i++) {
      JSONObject catalog = (JSONObject) jsonCatalogs.get(i);
      if (catalog.get("flavor") == null || StringUtils.isBlank(catalog.get("flavor").toString())) {
        throw new IllegalArgumentException(
                "Unable to create new event as no flavor was given for one of the metadata collections");
      }
      String flavorString = catalog.get("flavor").toString();
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

      MetadataCollection collection = null;
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

      String fieldsJson = catalog.get("fields").toString();
      if (StringUtils.trimToNull(fieldsJson) != null) {
        Map<String, String> fields = RequestUtils.getKeyValueMap(fieldsJson);
        for (String key : fields.keySet()) {
          if ("subjects".equals(key)) {
            // Handle the special case of allowing subjects to be an array.
            MetadataField<?> field = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id 'subject' from Catalog with Flavor '%s'.", flavorString));
            }
            collection.removeField(field);
            try {
              JSONArray subjects = (JSONArray) parser.parse(fields.get(key));
              collection.addField(
                      MetadataField.copyMetadataFieldWithValue(field, StringUtils.join(subjects.iterator(), ",")));
            } catch (ParseException e) {
              throw new IllegalArgumentException(
                      String.format("Unable to parse the 'subjects' metadata array field because: %s", e.toString()));
            }
          } else if ("startDate".equals(key)) {
            // Special handling for start date since in API v1 we expect start date and start time to be separate fields.
            MetadataField<String> field = (MetadataField<String>) collection.getOutputFields().get(key);
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(startDatePattern.getOr(field.getPattern().get()));
            SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern().get());
            DateTime newStartDate = new DateTime(apiSdf.parse(fields.get(key)), DateTimeZone.UTC);
            if (field.getValue().isSome()) {
              DateTime oldStartDate = new DateTime(sdf.parse(field.getValue().get()), DateTimeZone.UTC);
              newStartDate = oldStartDate.withDate(newStartDate.year().get(), newStartDate.monthOfYear().get(), newStartDate.dayOfMonth().get());
            }
            collection.removeField(field);
            collection.addField(MetadataField.copyMetadataFieldWithValue(field, sdf.format(newStartDate.toDate())));
          } else if ("startTime".equals(key)) {
            // Special handling for start time since in API v1 we expect start date and start time to be separate fields.
            MetadataField<String> field = (MetadataField<String>) collection.getOutputFields().get("startDate");
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", "startDate", flavorString));
            }
            SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(startTimePattern.getOr("HH:mm"));
            SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern().get());
            DateTime newStartDate = new DateTime(apiSdf.parse(fields.get(key)), DateTimeZone.UTC);
            if (field.getValue().isSome()) {
              DateTime oldStartDate = new DateTime(sdf.parse(field.getValue().get()), DateTimeZone.UTC);
              newStartDate = oldStartDate.withTime(
                      newStartDate.hourOfDay().get(),
                      newStartDate.minuteOfHour().get(),
                      newStartDate.secondOfMinute().get(),
                      newStartDate.millisOfSecond().get());
            }
            collection.removeField(field);
            collection.addField(MetadataField.copyMetadataFieldWithValue(field, sdf.format(newStartDate.toDate())));
          } else {
            MetadataField<?> field = collection.getOutputFields().get(key);
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            collection.removeField(field);
            collection.addField(MetadataField.copyMetadataFieldWithValue(field, fields.get(key)));
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
    Opt<MetadataCollection> optCommonEventCollection = metadataList
            .getMetadataByFlavor(MediaPackageElements.EPISODE.toString());
    if (optCommonEventCollection.isSome()) {
      MetadataCollection commonEventCollection = optCommonEventCollection.get();

      MetadataField<?> startDate = commonEventCollection.getOutputFields().get("startDate");
      if (!startDate.isUpdated()) {
        SimpleDateFormat utcDateFormat = new SimpleDateFormat(startDate.getPattern().get());
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentDate = utcDateFormat.format(new DateTime(DateTimeZone.UTC).toDate());
        commonEventCollection.removeField(startDate);
        commonEventCollection.addField(MetadataField.copyMetadataFieldWithValue(startDate, currentDate));
      }
    }
  }
}
