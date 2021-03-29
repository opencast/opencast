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

package org.opencastproject.elasticsearch.index.event;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.Unmarshaller;

/**
 * Utility implementation to deal with the conversion of recording events and its corresponding index data structures.
 */
public final class EventIndexUtils {

  private static final Logger logger = LoggerFactory.getLogger(EventIndexUtils.class);

  // The number of attempts to get the series title in case it hasn't been added to the index.
  public static final int DEFAULT_ATTEMPTS = 10;
  // The amount of time in ms to wait before trying to get the series title again.
  public static final long DEFAULT_SLEEP = 100L;

  /**
   * This is a utility class and should therefore not be instantiated.
   */
  private EventIndexUtils() {
  }

  /**
   * Creates a search result item based on the data returned from the search index.
   *
   * @param metadata
   *          the search metadata
   * @param unmarshaller the unmarshaller to use
   * @return the search result item
   * @throws IOException
   *           if unmarshalling fails
   */
  public static Event toRecordingEvent(SearchMetadataCollection metadata, Unmarshaller unmarshaller)
          throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String eventJson = (String) metadataMap.get(EventIndexSchema.OBJECT).getValue();
    return Event.valueOf(IOUtils.toInputStream(eventJson, Charset.defaultCharset()), unmarshaller);
  }

  /**
   * Creates search metadata from a recording event such that the event can be stored in the search index.
   *
   * @param event
   *          the recording event
   * @return the set of metadata
   */
  public static SearchMetadataCollection toSearchMetadata(Event event) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(
            event.getIdentifier().concat(event.getOrganization()), Event.DOCUMENT_TYPE);
    metadata.addField(EventIndexSchema.UID, event.getIdentifier(), true);
    metadata.addField(EventIndexSchema.ORGANIZATION, event.getOrganization(), false);
    metadata.addField(EventIndexSchema.OBJECT, event.toXML(), false);
    if (StringUtils.isNotBlank(event.getTitle())) {
      metadata.addField(EventIndexSchema.TITLE, event.getTitle(), true);
    }
    if (StringUtils.isNotBlank(event.getDescription())) {
      metadata.addField(EventIndexSchema.DESCRIPTION, event.getDescription(), true);
    }
    if (StringUtils.isNotBlank(event.getLocation())) {
      metadata.addField(EventIndexSchema.LOCATION, event.getLocation(), true);
    }
    if (StringUtils.isNotBlank(event.getSeriesId())) {
      metadata.addField(EventIndexSchema.SERIES_ID, event.getSeriesId(), true);
    }
    if (StringUtils.isNotBlank(event.getSeriesName())) {
      metadata.addField(EventIndexSchema.SERIES_NAME, event.getSeriesName(), true);
    }
    if (StringUtils.isNotBlank(event.getLanguage())) {
      metadata.addField(EventIndexSchema.LANGUAGE, event.getLanguage(), true);
    }
    if (StringUtils.isNotBlank(event.getSubject())) {
      metadata.addField(EventIndexSchema.SUBJECT, event.getSubject(), true);
    }
    if (StringUtils.isNotBlank(event.getSource())) {
      metadata.addField(EventIndexSchema.SOURCE, event.getSource(), true);
    }
    if (StringUtils.isNotBlank(event.getCreated())) {
      metadata.addField(EventIndexSchema.CREATED, event.getCreated(), true);
    }
    if (StringUtils.isNotBlank(event.getCreator())) {
      metadata.addField(EventIndexSchema.CREATOR, event.getCreator(), true);
    }
    if (StringUtils.isNotBlank(event.getPublisher())) {
      metadata.addField(EventIndexSchema.PUBLISHER, event.getPublisher(), true);
    }
    if (StringUtils.isNotBlank(event.getLicense())) {
      metadata.addField(EventIndexSchema.LICENSE, event.getLicense(), true);
    }
    if (StringUtils.isNotBlank(event.getRights())) {
      metadata.addField(EventIndexSchema.RIGHTS, event.getRights(), true);
    }
    if (StringUtils.isNotBlank(event.getManagedAcl())) {
      metadata.addField(EventIndexSchema.MANAGED_ACL, event.getManagedAcl(), true);
    }
    if (StringUtils.isNotBlank(event.getWorkflowState())) {
      metadata.addField(EventIndexSchema.WORKFLOW_STATE, event.getWorkflowState(), true);
    }
    if (event.getWorkflowId() != null) {
      metadata.addField(EventIndexSchema.WORKFLOW_ID, event.getWorkflowId(), true);
    }
    if (StringUtils.isNotBlank(event.getWorkflowDefinitionId())) {
      metadata.addField(EventIndexSchema.WORKFLOW_DEFINITION_ID, event.getWorkflowDefinitionId(), true);
    }
    if (StringUtils.isNotBlank(event.getRecordingStartDate())) {
      metadata.addField(EventIndexSchema.START_DATE, event.getRecordingStartDate(), true);
    }
    if (StringUtils.isNotBlank(event.getRecordingEndDate())) {
      metadata.addField(EventIndexSchema.END_DATE, event.getRecordingEndDate(), true);
    }
    if (event.getDuration() != null) {
      metadata.addField(EventIndexSchema.DURATION, event.getDuration(), true);
    }
    if (event.getArchiveVersion() != null) {
      metadata.addField(EventIndexSchema.ARCHIVE_VERSION, event.getArchiveVersion(), true);
    }
    if (event.getRecordingStatus() != null) {
      metadata.addField(EventIndexSchema.RECORDING_STATUS, event.getRecordingStatus(), true);
    }

    metadata.addField(EventIndexSchema.EVENT_STATUS, event.getEventStatus(), true);

    metadata.addField(EventIndexSchema.HAS_COMMENTS, event.hasComments(), true);
    metadata.addField(EventIndexSchema.HAS_OPEN_COMMENTS, event.hasOpenComments(), true);
    metadata.addField(EventIndexSchema.NEEDS_CUTTING, event.needsCutting(), true);

    if (event.getPublications() != null) {
      List<Publication> publications = event.getPublications();
      HashMap<String, Object>[] publicationsArray = new HashMap[publications.size()];
      for (int i = 0; i < publications.size(); i++) {
        publicationsArray[i] = generatePublicationDoc(publications.get(i));
      }

      metadata.addField(EventIndexSchema.PUBLICATION, publicationsArray, true);
    }

    if (event.getPresenters() != null) {
      List<String> presenters = event.getPresenters();
      metadata.addField(EventIndexSchema.PRESENTER, presenters.toArray(new String[presenters.size()]), true);
    }

    if (event.getContributors() != null) {
      List<String> contributors = event.getContributors();
      metadata.addField(EventIndexSchema.CONTRIBUTOR, contributors.toArray(new String[contributors.size()]), true);
    }

    if (StringUtils.isNotBlank(event.getAccessPolicy())) {
      metadata.addField(EventIndexSchema.ACCESS_POLICY, event.getAccessPolicy(), false);
      addAuthorization(metadata, event.getAccessPolicy());
    }

    if (StringUtils.isNotBlank(event.getAgentId())) {
      metadata.addField(EventIndexSchema.AGENT_ID, event.getAgentId(), true);
    }

    if (StringUtils.isNotBlank(event.getTechnicalStartTime())) {
      metadata.addField(EventIndexSchema.TECHNICAL_START, event.getTechnicalStartTime(), true);
    }

    if (StringUtils.isNotBlank(event.getTechnicalEndTime())) {
      metadata.addField(EventIndexSchema.TECHNICAL_END, event.getTechnicalEndTime(), true);
    }

    if (event.getTechnicalPresenters() != null) {
      metadata.addField(EventIndexSchema.TECHNICAL_PRESENTERS,
              event.getTechnicalPresenters().toArray(new String[event.getTechnicalPresenters().size()]), true);
    }

    return metadata;
  }

  private static void addObjectStringtToMap(HashMap<String, Object> map, String key, Object value) {
    if (value == null) {
      map.put(key, "");
    } else {
      map.put(key, value.toString());
    }
  }

  /**
   * Generate the document structure for the publication element
   *
   * @param publication
   *          the source publication element
   * @return a map representing the ES document structure of the publication element
   */
  private static HashMap<String, Object> generatePublicationDoc(Publication publication) {
    HashMap<String, Object> pMap = new HashMap<String, Object>();

    // Add first level elements
    pMap.put(PublicationIndexSchema.CHANNEL, publication.getChannel());
    addObjectStringtToMap(pMap, PublicationIndexSchema.MIMETYPE, publication.getMimeType());

    // Attachments
    Attachment[] attachments = publication.getAttachments();
    HashMap<String, Object>[] attachmentsArray = new HashMap[attachments.length];
    for (int i = 0; i < attachmentsArray.length; i++) {
      Attachment attachment = attachments[i];
      HashMap<String, Object> element = new HashMap<String, Object>();
      element.put(PublicationIndexSchema.ELEMENT_ID, attachment.getIdentifier());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_MIMETYPE, attachment.getMimeType());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_TYPE, attachment.getElementType());
      element.put(PublicationIndexSchema.ELEMENT_TAG, attachment.getTags());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_URL, attachment.getURI());
      element.put(PublicationIndexSchema.ELEMENT_SIZE, attachment.getSize());
      attachmentsArray[i] = element;
    }
    pMap.put(PublicationIndexSchema.ATTACHMENT, attachmentsArray);

    // Catalogs
    Catalog[] catalogs = publication.getCatalogs();
    HashMap<String, Object>[] catalogsArray = new HashMap[catalogs.length];
    for (int i = 0; i < catalogsArray.length; i++) {
      Catalog catalog = catalogs[i];
      HashMap<String, Object> element = new HashMap<String, Object>();
      element.put(PublicationIndexSchema.ELEMENT_ID, catalog.getIdentifier());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_MIMETYPE, catalog.getMimeType());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_TYPE, catalog.getElementType());
      element.put(PublicationIndexSchema.ELEMENT_TAG, catalog.getTags());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_URL, catalog.getURI());
      element.put(PublicationIndexSchema.ELEMENT_SIZE, catalog.getSize());
      catalogsArray[i] = element;
    }
    pMap.put(PublicationIndexSchema.CATALOG, catalogsArray);

    // Tracks
    Track[] tracks = publication.getTracks();
    HashMap<String, Object>[] tracksArray = new HashMap[tracks.length];
    for (int i = 0; i < tracksArray.length; i++) {
      Track track = tracks[i];
      HashMap<String, Object> element = new HashMap<String, Object>();
      element.put(PublicationIndexSchema.ELEMENT_ID, track.getIdentifier());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_MIMETYPE, track.getMimeType());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_TYPE, track.getElementType());
      element.put(PublicationIndexSchema.ELEMENT_TAG, track.getTags());
      addObjectStringtToMap(element, PublicationIndexSchema.ELEMENT_URL, track.getURI());
      element.put(PublicationIndexSchema.ELEMENT_SIZE, track.getSize());
      element.put(PublicationIndexSchema.TRACK_DURATION, track.getDuration());
      tracksArray[i] = element;
    }
    pMap.put(PublicationIndexSchema.TRACK, tracksArray);

    return pMap;
  }

  /**
   * Adds authorization fields to the input document.
   *
   * @param doc
   *          the input document
   * @param aclString
   *          the access control list string
   */
  private static void addAuthorization(SearchMetadataCollection doc, String aclString) {
    Map<String, List<String>> permissions = new HashMap<String, List<String>>();

    // Define containers for common permissions
    for (Action action : Permissions.Action.values()) {
      permissions.put(action.toString(), new ArrayList<String>());
    }

    AccessControlList acl = AccessControlParser.parseAclSilent(aclString);
    for (AccessControlEntry entry : acl.getEntries()) {
      if (!entry.isAllow()) {
        logger.info("Event index does not support denial via ACL, ignoring {}", entry);
        continue;
      }
      List<String> actionPermissions = permissions.get(entry.getAction());
      if (actionPermissions == null) {
        actionPermissions = new ArrayList<String>();
        permissions.put(entry.getAction(), actionPermissions);
      }
      actionPermissions.add(entry.getRole());
    }

    // Write the permissions to the input document
    for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
      String fieldName = EventIndexSchema.ACL_PERMISSION_PREFIX.concat(entry.getKey());
      doc.addField(fieldName, entry.getValue(), false);
    }
  }

  /**
   * Loads the recording event from the search index or creates a new one that can then be persisted.
   *
   * @param mediapackageId
   *          the mediapackage identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the {@link AbstractSearchIndex} to search in
   * @return the recording event
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple recording events with the same identifier are found
   */
  public static Event getOrCreateEvent(String mediapackageId, String organization, User user,
          AbstractSearchIndex searchIndex) throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(organization, user).withoutActions().withIdentifier(mediapackageId);
    SearchResult<Event> searchResult = searchIndex.getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return new Event(mediapackageId, organization);
    } else if (searchResult.getDocumentCount() == 1) {
      return searchResult.getItems()[0].getSource();
    } else {
      throw new IllegalStateException(
              "Multiple recording events with identifier " + mediapackageId + " found in search index");
    }
  }

  /**
   * Loads the recording event from the search index
   *
   * @param mediapackageId
   *          the mediapackage identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the {@link AbstractSearchIndex} to search in
   * @return the recording event or <code>null</code> if not found
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple recording events with the same identifier are found
   */
  public static Event getEvent(String mediapackageId, String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(organization, user).withoutActions().withIdentifier(mediapackageId);
    SearchResult<Event> searchResult = searchIndex.getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return null;
    } else if (searchResult.getDocumentCount() == 1) {
      return searchResult.getItems()[0].getSource();
    } else {
      throw new IllegalStateException(
              "Multiple recording events with identifier " + mediapackageId + " found in search index");
    }
  }

  /**
   * Update the given {@link Event} with the given {@link DublinCore}.
   *
   * @param event
   *          the event to update
   * @param dc
   *          the catalog with the metadata for the update
   * @return the updated event
   */
  public static Event updateEvent(Event event, DublinCore dc) {
    event.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
    event.setDescription(dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
    event.setSubject(dc.getFirst(DublinCore.PROPERTY_SUBJECT));
    event.setLocation(dc.getFirst(DublinCore.PROPERTY_SPATIAL));
    event.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
    event.setSource(dc.getFirst(DublinCore.PROPERTY_SOURCE));
    event.setSeriesId(dc.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    event.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));
    event.setRights(dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
    event.setPublisher(dc.getFirst(DublinCore.PROPERTY_PUBLISHER));
    Date created;
    String encodedDate = dc.getFirst(DublinCore.PROPERTY_CREATED);
    if (StringUtils.isBlank(encodedDate)) {
      created = new Date();
    } else {
      created = EncodingSchemeUtils.decodeDate(encodedDate);
    }
    event.setCreated(DateTimeSupport.toUTC(created.getTime()));
    String strPeriod = dc.getFirst(DublinCore.PROPERTY_TEMPORAL);
    try {
      if (StringUtils.isNotBlank(strPeriod)) {
        DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(strPeriod);
        event.setRecordingStartDate(DateTimeSupport.toUTC(period.getStart().getTime()));
        event.setRecordingEndDate(DateTimeSupport.toUTC(period.getEnd().getTime()));
        event.setDuration(period.getEnd().getTime() - period.getStart().getTime());
      } else {
        event.setRecordingStartDate(DateTimeSupport.toUTC(created.getTime()));
      }
    } catch (Exception e) {
      logger.warn("Invalid start and end date/time for event {}: {}", event.getIdentifier(), strPeriod);
      event.setRecordingStartDate(DateTimeSupport.toUTC(created.getTime()));
    }

    updateTechnicalDate(event);

    // TODO: Add support for language
    event.setContributors(dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_ANY));
    event.setPresenters(dc.get(DublinCore.PROPERTY_CREATOR, DublinCore.LANGUAGE_ANY));
    return event;
  }

  public static Event updateTechnicalDate(Event event) {
    if (event.isScheduledEvent() && event.hasRecordingStarted()) {
      // Override technical dates from recording if already started
      event.setTechnicalStartTime(event.getRecordingStartDate());
      event.setTechnicalEndTime(event.getRecordingEndDate());
    } else {
      // If this is an upload where the start time is not set, set the start time to same as dublin core
      if (StringUtils.isBlank(event.getTechnicalStartTime())) {
        event.setTechnicalStartTime(event.getRecordingStartDate());
      }
      if (StringUtils.isBlank(event.getTechnicalEndTime())) {
        event.setTechnicalEndTime(event.getRecordingEndDate());
      }
    }
    return event;
  }

  /**
   * Update the given {@link Event} with the given {@link MediaPackage}.
   *
   * @param event
   *          the event to update
   * @param mp
   *          the mediapackage containing the metadata for the update
   * @return the updated event
   */
  public static Event updateEvent(Event event, MediaPackage mp) {
    event.setPublications(Arrays.asList(mp.getPublications()));
    event.setSeriesName(mp.getSeriesTitle());
    return event;
  }

  /**
   * A function to update the series title within an event. Uses the default number of attempts to get the series title
   * and the default amount of time to sleep between attempts.
   *
   * @param event
   *          The event to update the series name in
   * @param organization
   *          The organization for this event and series
   * @param user
   *          The user
   * @param searchIndex
   *          The index to search for the series
   */
  public static void updateSeriesName(Event event, String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException {
    updateSeriesName(event, organization, user, searchIndex, DEFAULT_ATTEMPTS, DEFAULT_SLEEP);
  }

  /**
   * A function to update the series title within an event.
   *
   * @param event
   *          The event to update the series name in
   * @param organization
   *          The organization for this event and series
   * @param user
   *          The user
   * @param searchIndex
   *          The index to search for the series
   * @param tries
   *          The number of attempts to try to get the series title
   * @param sleep
   *          The amount of time in ms to sleep between attempts to get the series title.
   */
  public static void updateSeriesName(Event event, String organization, User user, AbstractSearchIndex searchIndex,
          int tries, long sleep) throws SearchIndexException {
    if (event.getSeriesId() != null) {
      for (int i = 1; i <= tries; i++) {
        SearchResult<Series> result = searchIndex.getByQuery(
                new SeriesSearchQuery(organization, user).withoutActions().withIdentifier(event.getSeriesId()));
        if (result.getHitCount() > 0) {
          event.setSeriesName(result.getItems()[0].getSource().getTitle());
          break;
        } else {
          Integer triesLeft = tries - i;
          logger.debug("Not able to find the series {} in the search index for the event {}. Will try {} more times.",
                  event.getSeriesId(), event.getIdentifier(), triesLeft);
          try {
            Thread.sleep(sleep);
          } catch (InterruptedException e) {
            logger.warn("Interrupted while sleeping before checking for the series being added to the index", e);
          }
        }

      }
    }
  }

  /**
   * Update an event with the given has comments and has open comments status.
   *
   * @param eventId
   *          the event id
   * @param hasComments
   *          whether it has comments
   * @param hasOpenComments
   *          whether it has open comments
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the serach index
   * @throws SearchIndexException
   *           if error occurs
   * @throws NotFoundException
   *           if event has not been found
   */
  public static void updateComments(String eventId, boolean hasComments, boolean hasOpenComments, boolean needsCutting,
      String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException, NotFoundException {
    if (!hasComments && hasOpenComments) {
      throw new IllegalStateException(
              "Invalid comment update request: You can't have open comments without having any comments!");
    }
    if (!hasOpenComments && needsCutting) {
      throw new IllegalStateException(
          "Invalid comment update request: You can't have an needs cutting comment without having any open comments!");
    }
    Event event = getEvent(eventId, organization, user, searchIndex);
    if (event == null) {
      throw new NotFoundException("No event with id " + eventId + " found.");
    }

    event.setHasComments(hasComments);
    event.setHasOpenComments(hasOpenComments);
    event.setNeedsCutting(needsCutting);
    try {
      searchIndex.addOrUpdate(event);
    } catch (SearchIndexException e) {
      logger.warn("Unable to update event '{}'", event, e);
    }
  }

  /**
   * Update a managed acl name to a new one.
   *
   * @param currentManagedAcl
   *          The current unique managed acl name to look for in the events.
   * @param newManagedAcl
   *          The new managed acl name to update all of the events to.
   * @param organization
   *          The organization for the managed acl.
   * @param user
   *          The user.
   * @param searchIndex
   *          The index to update with the new managed acl name.
   */
  public static void updateManagedAclName(String currentManagedAcl, String newManagedAcl, String organization,
          User user, AbstractSearchIndex searchIndex) {
    SearchResult<Event> result = null;
    try {
      result = searchIndex
              .getByQuery(new EventSearchQuery(organization, user).withoutActions().withManagedAcl(currentManagedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the events in org '{}' with current managed acl name '{}' for event",
              organization, currentManagedAcl, e);
    }
    if (result != null && result.getHitCount() > 0) {
      for (SearchResultItem<Event> eventItem : result.getItems()) {
        Event event = eventItem.getSource();
        event.setManagedAcl(newManagedAcl);
        try {
          searchIndex.addOrUpdate(event);
        } catch (SearchIndexException e) {
          logger.warn(
                  "Unable to update event '{}' from current managed acl '{}' to new managed acl name '{}'",
                  event, currentManagedAcl, newManagedAcl, e);
        }
      }
    }
  }

  /**
   * Remove a managed acl from all events that have it.
   *
   * @param managedAcl
   *          The managed acl unique name to remove.
   * @param organization
   *          The organization for the managed acl
   * @param user
   *          The user
   * @param searchIndex
   *          The search index to remove the managed acl from.
   */
  public static void deleteManagedAcl(String managedAcl, String organization, User user,
          AbstractSearchIndex searchIndex) {
    SearchResult<Event> result = null;
    try {
      result = searchIndex
              .getByQuery(new EventSearchQuery(organization, user).withoutActions().withManagedAcl(managedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the events in org '{}' with current managed acl name '{}' for event",
              organization, managedAcl, e);
    }
    if (result != null && result.getHitCount() > 0) {
      for (SearchResultItem<Event> eventItem : result.getItems()) {
        Event event = eventItem.getSource();
        event.setManagedAcl(null);
        try {
          searchIndex.addOrUpdate(event);
        } catch (SearchIndexException e) {
          logger.warn("Unable to update event '{}' to remove managed acl '{}'", event, managedAcl, e);
        }
      }
    }
  }

  /**
   * Gets all of the MediaPackageElement's flavors.
   *
   * @param publications
   *          The list of publication elements to get the flavors from.
   * @return An array of {@link String} representation of the MediaPackageElementFlavors
   */
  private static String[] getPublicationFlavors(List<Publication> publications) {
    Set<String> allPublicationFlavors = new TreeSet<String>();
    for (Publication p : publications) {
      for (Attachment attachment : p.getAttachments()) {
        if (attachment.getFlavor() != null) {
          allPublicationFlavors.add(attachment.getFlavor().toString());
        }
      }
      for (Catalog catalog : p.getCatalogs()) {
        if (catalog.getFlavor() != null) {
          allPublicationFlavors.add(catalog.getFlavor().toString());
        }
      }
      for (Track track : p.getTracks()) {
        if (track.getFlavor() != null) {
          allPublicationFlavors.add(track.getFlavor().toString());
        }
      }
    }
    return allPublicationFlavors.toArray(new String[allPublicationFlavors.size()]);
  }

  /**
   * Returns <code>true</code> if the previewSubtype matches any of the publicationFlavors.
   *
   * @param publications
   * @param previewSubtype
   * @return
   */
  public static Boolean subflavorMatches(List<Publication> publications, String previewSubtype) {
    String[] publicationFlavors = getPublicationFlavors(publications);
    if (publicationFlavors != null && previewSubtype != null) {
      final String subtype = "/" + previewSubtype;
      for (String flavor : publicationFlavors) {
        if (flavor.endsWith(subtype)) {
          return true;
        }
      }
    }
    return false;
  }

}
