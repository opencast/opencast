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

package org.opencastproject.elasticsearch.index.objects.event;

import static org.opencastproject.security.util.SecurityUtil.getEpisodeRoleId;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.elasticsearch.index.objects.series.SeriesIndexSchema;
import org.opencastproject.elasticsearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;

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
import java.util.HashSet;
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
  public static SearchMetadataCollection toSearchMetadata(Event event, ListProvidersService listProviderService,
      boolean episodeIdRole) {
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

    if (event.comments() != null) {
      List<Comment> comments = event.comments();
      HashMap<String, Object>[] commentsArray = new HashMap[comments.size()];
      for (int i = 0; i < comments.size(); i++) {
        Comment comment = comments.get(i);
        HashMap<String, Object> myMap = new HashMap<String, Object>() {{
            put(CommentIndexSchema.ID, comment.getId());
            put(CommentIndexSchema.REASON, comment.getReason());
            put(CommentIndexSchema.TEXT, comment.getText());
            put(CommentIndexSchema.RESOLVED_STATUS, comment.isResolvedStatus());
          }};
        commentsArray[i] = myMap;
//        generatePublicationDoc(comments.get(i).getType());
      }

      metadata.addField(EventIndexSchema.COMMENTS, commentsArray, true);
    }

    metadata.addField(EventIndexSchema.NEEDS_CUTTING, event.needsCutting(), true);

    if (event.getPublications() != null) {
      List<Publication> publications = event.getPublications();
      HashMap<String, Object>[] publicationsArray = new HashMap[publications.size()];
      for (int i = 0; i < publications.size(); i++) {
        publicationsArray[i] = generatePublicationDoc(publications.get(i));
      }

      if (publications.size() == 1 && !publications.get(0).getChannel().equals("internal") || publications.size() > 1) {
        metadata.addField(EventIndexSchema.IS_PUBLISHED, true, true);
      } else {
        metadata.addField(EventIndexSchema.IS_PUBLISHED, false, true);
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

    if (!event.getExtendedMetadata().isEmpty()) {
      addExtendedMetadata(metadata, event.getExtendedMetadata());
    }

    if (StringUtils.isNotBlank(event.getAccessPolicy())) {
      metadata.addField(EventIndexSchema.ACCESS_POLICY, event.getAccessPolicy(), true);
      addAuthorization(metadata, event.getAccessPolicy(), event.getIdentifier(), listProviderService, episodeIdRole);
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
   * Adds extended metadata fields to the input document
   *
   * @param doc
   *          the input document
   * @param extendedMetadata
   *          the extended metadata map
   */
  private static void addExtendedMetadata(SearchMetadataCollection doc, Map<String, Map<String,
          List<String>>> extendedMetadata) {
    for (String type: extendedMetadata.keySet()) {
      Map<String, List<String>> extendedMetadataByType = extendedMetadata.get(type);
      for (String name: extendedMetadataByType.keySet()) {
        List<String> values = extendedMetadataByType.get(name);
        String fieldName = SeriesIndexSchema.EXTENDED_METADATA_PREFIX.concat(type + "_" + name);
        doc.addField(fieldName, values, true);
      }
    }
  }

  /**
   * Adds authorization fields to the input document.
   *
   * @param doc
   *          the input document
   * @param aclString
   *          the access control list string
   */
  private static void addAuthorization(SearchMetadataCollection doc, String aclString,
      String eventId, ListProvidersService listProvidersService, boolean episodeIdRole) {
    Map<String, List<String>> permissions = new HashMap<>();

    // Define containers for common permissions
    for (Action action : Permissions.Action.values()) {
      permissions.put(action.toString(), new ArrayList<>());
    }

    // Add roles from acl
    AccessControlList acl = AccessControlParser.parseAclSilent(aclString);
    List<AccessControlEntry> entries = acl.getEntries();

    // Add special action roles for episode id roles
    if (episodeIdRole) {
      Set<AccessControlEntry> customEntries = new HashSet<>();
      customEntries.add(new AccessControlEntry(getEpisodeRoleId(eventId, "READ"), "read", true));
      customEntries.add(new AccessControlEntry(getEpisodeRoleId(eventId, "WRITE"), "write", true));

      ResourceListQuery query = new ResourceListQueryImpl();
      if (listProvidersService.hasProvider("ACL.ACTIONS")) {
        Map<String, String> actions = new HashMap<>();
        try {
          actions = listProvidersService.getList("ACL.ACTIONS", query, true);
        } catch (ListProviderException e) {
          logger.error("Listproviders not loaded. " + e);
        }
        for (String action : actions.keySet()) {
          customEntries.add(new AccessControlEntry(getEpisodeRoleId(eventId, action), action, true));
        }
      }

      entries.addAll(customEntries);
    }

    // Convert roles to permission blocks
    for (AccessControlEntry entry : entries) {
      if (!entry.isAllow()) {
        logger.info("Event index does not support denial via ACL, ignoring {}", entry);
        continue;
      }
      List<String> actionPermissions = permissions.get(entry.getAction());
      if (actionPermissions == null) {
        actionPermissions = new ArrayList<>();
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
   * Update extended metadata for event from dublin core catalog.
   *
   * @param event
   *         The event
   * @param dc
   *         The dublin core catalog with extended metadata
   * @param flavor
   *         The flavor of the extended metadata
   * @return
   *         The updated event
   */
  public static Event updateEventExtendedMetadata(Event event, DublinCoreCatalog dc, MediaPackageElementFlavor flavor) {
    Map<String, List<String>> map = new HashMap();
    Set<EName> eNames = dc.getProperties();
    for (EName eName: eNames) {
      String name = eName.getLocalName();
      List<String> values = dc.get(eName, DublinCore.LANGUAGE_ANY);
      map.put(name, values);
    }
    event.setExtendedMetadata(flavor.toString(), map);
    return event;
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
  public static void updateSeriesName(Event event, String organization, User user, ElasticsearchIndex searchIndex)
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
  public static void updateSeriesName(Event event, String organization, User user, ElasticsearchIndex searchIndex,
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
