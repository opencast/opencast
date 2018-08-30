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

package org.opencastproject.index.service.api;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventHttpServletRequest;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.entwinemedia.fn.data.Opt;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface IndexService {
  enum Source {
    ARCHIVE, WORKFLOW, SCHEDULE
  };

  enum SourceType {
    UPLOAD, UPLOAD_LATER, SCHEDULE_SINGLE, SCHEDULE_MULTIPLE
  }

  SearchResult<Group> getGroups(String filter, Opt<Integer> limit, Opt<Integer> offset, Opt<String> sort,
          AbstractSearchIndex index) throws SearchIndexException;

  /**
   * Get a single group
   *
   * @param id
   *          the group id
   * @param index
   *          the index to search
   * @return a group or none if not found wrapped in an option
   * @throws SearchIndexException
   *           Thrown if the index cannot be read
   */
  Opt<Group> getGroup(String id, AbstractSearchIndex index) throws SearchIndexException;

  Response removeGroup(String id) throws NotFoundException;

  /**
   * Update a {@link Group} with new data
   *
   * @param id
   *          The unique id for the group.
   * @param name
   *          The name to use for the group.
   * @param description
   *          The description of the group.
   * @param roles
   *          A comma separated list of roles to add to this group.
   * @param members
   *          A comma separated list of roles to add to this group.
   * @return The Response from the update
   * @throws NotFoundException
   *           Thrown if the group was not found
   */
  Response updateGroup(String id, String name, String description, String roles, String members)
          throws NotFoundException;

  /**
   * Create a new {@link Group}
   *
   * @param name
   *          The name of the group, also transformed to be the id for this group.
   * @param description
   *          The description of the group.
   * @param roles
   *          A comma separated list of roles to add to this group.
   * @param members
   *          A comma separated list of members to add to this group.
   * @return The Response from the update
   */
  Response createGroup(String name, String description, String roles, String members);

  /**
   * Get a single event
   *
   * @param id
   *          the mediapackage id
   * @param index
   *          The index to get the event from.
   * @return an event or none if not found wrapped in an option
   * @throws SearchIndexException
   *           Thrown if the index cannot be read
   */
  Opt<Event> getEvent(String id, AbstractSearchIndex index) throws SearchIndexException;

  /**
   * Creates a new event based on a request.
   *
   * @param request
   *          The request containing the data to create the event.
   * @return The event's id (or a comma seperated list of event ids if it was a group of events)
   * @throws IndexServiceException
   *           Thrown if there was an internal server error while creating the event.
   * @throws IllegalArgumentException
   *           Thrown if the provided request was inappropriate.
   */
  String createEvent(HttpServletRequest request) throws IndexServiceException, IllegalArgumentException;

  /**
   * Creates a new event based on a json string and a media package.
   *
   * @param metadataJson
   *          The json representing the metadata collection.
   * @param mp
   *          The mediapackage that will be used to create the event.
   * @return The event's id (or a comma seperated list of event ids if it was a group of events)
   * @throws ParseException
   *           Thrown if unable to parse the json metadata
   * @throws IOException
   *           Thrown if unable to create a dublin core catalog
   * @throws MediaPackageException
   *           Thrown if there is a problem with the mediapackage.
   * @throws IngestException
   *           Thrown if unable to ingest the new event.
   * @throws NotFoundException
   *           Thrown if the event is not found to be created.
   * @throws SchedulerException
   *           Thrown if there is a problem scheduling the event.
   * @throws UnauthorizedException
   *           Thrown if the user is unable to create events.
   */
  String createEvent(JSONObject metadataJson, MediaPackage mp) throws ParseException, IOException,
          MediaPackageException, IngestException, NotFoundException, SchedulerException, UnauthorizedException;

  /**
   * Create a new event using a {@link EventHttpServletRequest}.
   *
   * @param eventHttpServletRequest
   *          The {@link EventHttpServletRequest} containing all of the necessary information to create a new event.
   * @return The id or ids created by the {@link EventHttpServletRequest}.
   * @throws ParseException
   *           Thrown if unable to parse the start and end UTC date and time.
   * @throws IOException
   *           Thrown if unable to update the event's DublinCoreCatalog
   * @throws MediaPackageException
   *           Thrown if unable to update the event's {@link MediaPackage}
   * @throws IngestException
   *           Thrown if unable to update the ingest service with the new Event.
   * @throws NotFoundException
   *           Thrown if the specified workflow definition cannot be found.
   * @throws SchedulerException
   *           Thrown if unable to schedule the new event.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to create the new event.
   */
  String createEvent(EventHttpServletRequest eventHttpServletRequest) throws ParseException, IOException,
          MediaPackageException, IngestException, NotFoundException, SchedulerException, UnauthorizedException;

  /**
   * Removes an event.
   *
   * @param id
   *          The id for the event to remove.
   * @return true if the event was found and removed.
   * @throws NotFoundException
   *           Thrown if the group was not found
   * @throws UnauthorizedException
   *           Thrown if the action is unauthorized
   */
  boolean removeEvent(String id) throws NotFoundException, UnauthorizedException;

  /**
   * Create or Update an existing event asset.
   *
   * The request contains a JSON describing asset type,
   *  flavor, subflavor, the mpId, a workflow description id to initiate on the event,
   *  and the asset file streams in a multipart form.
   *  Existing type-flavor matches are updated, otherwise the asset is added.
   *
   * @param request
   *          The http servlet request containing metadata, workflow id, and file streams
   * @param mp
   *          The mediapackage to update
   * @return the workflow instance id that was started
   * @throws ParseException
   *           Thrown if unable to parse the start and end UTC date and time.
   * @throws IOException
   *           Thrown if unable to update the event's DublinCoreCatalog
   * @throws MediaPackageException
   *           Thrown if unable to update the event's {@link MediaPackage}
   * @throws NotFoundException
   *           Thrown if the specified workflow definition cannot be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to create the new event.
   * @throws IndexServiceException
   *            Thrown if the update assets workflow cannot be started
   */
  String updateEventAssets(MediaPackage mp, HttpServletRequest request) throws ParseException, IOException,
          MediaPackageException, NotFoundException, UnauthorizedException, IndexServiceException;

  /**
   * Update an event's metadata using a {@link MetadataList}
   *
   * @param id
   *          The id of the event.
   * @param metadataList
   *          The {@link MetadataList} with the new metadata.
   * @param index
   *          The index used to process this update.
   * @return The {@link MetadataList} with the updated fields.
   * @throws IndexServiceException
   *           Thrown if unable to update the event with the index.
   * @throws SearchIndexException
   *           Thrown if there was a problem getting the event.
   * @throws NotFoundException
   *           Thrown if unable to find the event.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to edit the event.
   */
  MetadataList updateEventMetadata(String id, MetadataList metadataList, AbstractSearchIndex index)
          throws IndexServiceException, SearchIndexException, NotFoundException, UnauthorizedException;

  /**
   * Update only the common default event metadata.
   *
   * @param id
   *          The id of the event to update.
   * @param metadataJSON
   *          The metadata to update in json format.
   * @param index
   *          The index to update the event in.
   * @return A metadata list of the updated fields.
   * @throws IllegalArgumentException
   *           Thrown if the metadata was not formatted correctly.
   * @throws IndexServiceException
   *           Thrown if there was an error updating the event.
   * @throws SearchIndexException
   *           Thrown if there was an error searching the search index.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateCommonEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, SearchIndexException, NotFoundException,
          UnauthorizedException;

  /**
   * Update the event metadata in all available catalogs.
   *
   * @param id
   *          The id of the event to update.
   * @param metadataJSON
   *          The metadata to update in json format.
   * @param index
   *          The index to update the event in.
   * @return A metadata list of the updated fields.
   * @throws IllegalArgumentException
   *           Thrown if the metadata was not formatted correctly.
   * @throws IndexServiceException
   *           Thrown if there was an error updating the event.
   * @throws SearchIndexException
   *           Thrown if there was an error searching the search index.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateAllEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, SearchIndexException, NotFoundException,
          UnauthorizedException;

  /**
   * Remove catalogs from the event with the given flavor.
   *
   * @param event
   *          The event who will have the catalog removed.
   * @param flavor
   *          The flavor to use to find the catalogs.
   * @throws IndexServiceException
   *           Thrown if there was a problem getting the catalog for the event.
   * @throws NotFoundException
   *           Thrown if unable to find a catalog that matches the flavor.
   * @throws UnauthorizedException
   *           Thrown if the action is unauthorized.
   */
  void removeCatalogByFlavor(Event event, MediaPackageElementFlavor flavor)
          throws IndexServiceException, NotFoundException, UnauthorizedException;

  /**
   * Update the event's {@link AccessControlList}.
   *
   * @param id
   *          The id of the event to update.
   * @param acl
   *          The {@link AccessControlList} that this event will get updated with.
   * @param index
   *          The index to update the event in.
   * @return The updated {@link AccessControlList}.
   * @throws IllegalArgumentException
   *           Thrown if the event is currently processing as a workflow so unable to update the ACL as we may have
   *           already distributed it.
   * @throws IndexServiceException
   *           Thrown if there was a problem updating the ACL for an event.
   * @throws SearchIndexException
   *           Thrown if there was an error searching the search index.
   * @throws NotFoundException
   *           Thrown if the event cannot be found to update.
   * @throws UnauthorizedException
   *           Thrown if the action is unauthorized.
   */
  AccessControlList updateEventAcl(String id, AccessControlList acl, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, SearchIndexException, NotFoundException,
          UnauthorizedException;

  // TODO remove when it is no longer needed by AbstractEventEndpoint.
  MediaPackage getEventMediapackage(Event event) throws IndexServiceException;

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  Source getEventSource(Event event);

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  Opt<WorkflowInstance> getCurrentWorkflowInstance(String mpId) throws IndexServiceException;

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  void updateWorkflowInstance(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException;

  void updateCommentCatalog(Event event, List<EventComment> comments) throws Exception;

  MetadataList getMetadataListWithAllSeriesCatalogUIAdapters();

  MetadataList getMetadataListWithAllEventCatalogUIAdapters();

  /**
   * @return A {@link List} of {@link EventCatalogUIAdapter} that provide the metadata to the front end.
   */
  List<EventCatalogUIAdapter> getEventCatalogUIAdapters();

  /**
   * @return the common {@link EventCatalogUIAdapter}
   */
  EventCatalogUIAdapter getCommonEventCatalogUIAdapter();

  /**
   * Get a single series
   *
   * @param seriesId
   *          the series id
   * @param searchIndex
   *          the abstract search index
   * @return a series or none if not found wrapped in an option
   * @throws SearchIndexException
   *           Thrown if there is an error when using the search index.
   */
  Opt<Series> getSeries(String seriesId, AbstractSearchIndex searchIndex) throws SearchIndexException;

  /**
   * Create a new series.
   *
   * @param metadata
   *          The json metadata to create the new series.
   * @return The series id.
   * @throws IllegalArgumentException
   *           Thrown if the metadata is incomplete or malformed.
   * @throws IndexServiceException
   *           Thrown if there are issues with processing the request.
   * @throws UnauthorizedException
   *           Thrown if the user cannot create a new series.
   */
  String createSeries(String metadata) throws IllegalArgumentException, IndexServiceException, UnauthorizedException;

  /**
   * Create a series from a set of metadata and options.
   *
   * @param metadataList
   *          The metadata for the series
   * @param options
   *          Options for the series
   * @param optAcl
   *          ACLs for the series
   * @param optThemeId
   *          Themes for the series
   * @return The series id.
   * @throws IndexServiceException
   *           Thrown if there are issues with processing the request.
   */
  String createSeries(MetadataList metadataList, Map<String, String> options, Opt<AccessControlList> optAcl,
          Opt<Long> optThemeId) throws IndexServiceException;

  /**
   * Remove a series.
   *
   * @param id
   *          The id of the series to remove.
   * @throws NotFoundException
   *           Thrown if the series is not found
   * @throws SeriesException
   *           Thrown if an error removing the series
   * @throws UnauthorizedException
   *           Thrown if the activity is unauthorized
   */
  void removeSeries(String id) throws NotFoundException, SeriesException, UnauthorizedException;

  /**
   * @return the common {@link SeriesCatalogUIAdapter}
   */
  SeriesCatalogUIAdapter getCommonSeriesCatalogUIAdapter();

  /**
   * @return A {@link List} of {@link SeriesCatalogUIAdapter} that provide the metadata to the front end.
   */
  List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters();

  /**
   * Changes the opt out status of a single event (by its mediapackage id)
   *
   * @param eventId
   *          The event's unique id formally the mediapackage id
   * @param optout
   *          Whether the event should be moved into opted out.
   * @param index
   *          The index to update the event in.
   * @throws NotFoundException
   *           Thrown if the event could not be found.
   * @throws SchedulerException
   *           Thrown if there was an error in the scheduler service
   * @throws SearchIndexException
   *           Thrown if there was and error in search index
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  void changeOptOutStatus(String eventId, boolean optout, AbstractSearchIndex index)
          throws NotFoundException, SchedulerException, SearchIndexException, UnauthorizedException;

  /**
   * Update only the common default series metadata.
   *
   * @param id
   *          The id of the series to update.
   * @param metadataJSON
   *          The metadata to update in json format.
   * @param index
   *          The index to update the series in.
   * @return A metadata list of the updated fields.
   * @throws IllegalArgumentException
   *           Thrown if the metadata was not formatted correctly.
   * @throws IndexServiceException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateCommonSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException;

  /**
   * Update the series metadata in all available catalogs.
   *
   * @param id
   *          The id of the series to update.
   * @param metadataJSON
   *          The metadata to update in json format.
   * @param index
   *          The index to update the series in.
   * @return A metadata list of the updated fields.
   * @throws IllegalArgumentException
   *           Thrown if the metadata was not formatted correctly.
   * @throws IndexServiceException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateAllSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException;

  /**
   * Update the series metadata in all available catalogs by providing a complete {@link MetadataList}
   *
   * @param id
   *          The id of the series
   * @param metadataList
   *          The complete {@link MetadataList}
   * @param index
   *          The index that will be used to find the series.
   * @return The updated {@link MetadataList}
   * @throws IndexServiceException
   *           Thrown if unable to query the index for the series.
   * @throws NotFoundException
   *           Thrown if unable to find the series to update the metadata for.
   * @throws UnauthorizedException
   *           Thrown if the user is unable to update the series.
   */
  MetadataList updateAllSeriesMetadata(String id, MetadataList metadataList, AbstractSearchIndex index)
          throws IndexServiceException, NotFoundException, UnauthorizedException;

  /**
   * Remove a catalog from the series that matches the given flavor.
   *
   * @param series
   *          The series to remove the catalog from.
   * @param flavor
   *          The flavor that will match the catalog.
   * @throws IndexServiceException
   *           Thrown if there was an error reading the given event.
   * @throws NotFoundException
   *           Thrown if the catalog cannot be found.
   */
  void removeCatalogByFlavor(Series series, MediaPackageElementFlavor flavor)
          throws IndexServiceException, NotFoundException;

  /**
   * Checks if the given event as an active transaction
   *
   * @param eventId
   *          the event to check
   * @return Whether the event has an active transaction or not
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to access the given event.
   * @throws IndexServiceException
   *           Thrown if there was an error reading the given event.
   */
  boolean hasActiveTransaction(String eventId) throws NotFoundException, UnauthorizedException, IndexServiceException;

  /**
   * Checks if the given event has snapshots
   * @param eventId the event to check
   * @return <code>true</code> if the event has snapshots, <code>false</code> otherwise
   */
  boolean hasSnapshots(String eventId);

  Map<String,Map<String,String>> getEventWorkflowProperties(List<String> eventIds);

}
