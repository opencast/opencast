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

import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.exception.InternalServerErrorException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.entwinemedia.fn.data.Opt;

import javax.servlet.http.HttpServletRequest;

public interface IndexService {

  public enum Source {
    ARCHIVE, WORKFLOW, SCHEDULE
  };

  public enum SourceType {
    UPLOAD, UPLOAD_LATER, SCHEDULE_SINGLE, SCHEDULE_MULTIPLE
  }

  /**
   * Creates a new event based on a request.
   *
   * @param request
   *          The request containing the data to create the event.
   * @return The event's id (or a comma seperated list of event ids if it was a group of events)
   * @throws InternalServerErrorException
   *           Thrown if there was an internal server error while creating the event.
   * @throws IllegalArgumentException
   *           Thrown if the provided request was inappropriate.
   */
  String createEvent(HttpServletRequest request) throws InternalServerErrorException, IllegalArgumentException;

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
   * @throws InternalServerErrorException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateCommonEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException;

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
   * @throws InternalServerErrorException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateAllEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException;

  // TODO remove when update has been removed from event endpoint as well.
  void updateMediaPackageMetadata(MediaPackage mp, MetadataList metadataList);

  // TODO remove when it is no longer needed by AbstractEventEndpoint.
  Opt<MediaPackage> getEventMediapackage(Event event) throws InternalServerErrorException;

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  Source getEventSource(Event event);

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  WorkflowInstance getCurrentWorkflowInstance(String mpId) throws InternalServerErrorException;

  // TODO remove when it is no longer needed by AbstractEventEndpoint
  void updateWorkflowInstance(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException;

  /**
   * Get a single event
   *
   * @param id
   *          the mediapackage id
   * @param index
   *          The index to get the event from.
   * @return an event or none if not found wrapped in an option
   * @throws SearchIndexException
   */
  Opt<Event> getEvent(String id, AbstractSearchIndex index) throws InternalServerErrorException;

  /**
   * Create a new series.
   *
   * @param metadata
   *          The json metadata to create the new series.
   * @return The series id.
   * @throws IllegalArgumentException
   *           Thrown if the metadata is incomplete or malformed.
   * @throws InternalServerErrorException
   *           Thrown if there are issues with processing the request.
   * @throws UnauthorizedException
   *           Thrown if the user cannot create a new series.
   */
  String createSeries(String metadata) throws IllegalArgumentException, InternalServerErrorException,
          UnauthorizedException;

  /**
   * Get a single series
   *
   * @param seriesId
   *          the series id
   * @param searchIndex
   *          the abstract search index
   * @return a series or none if not found wrapped in an option
   * @throws SearchIndexException
   */
  Opt<Series> getSeries(String seriesId, AbstractSearchIndex searchIndex) throws SearchIndexException;

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
   * @throws InternalServerErrorException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateCommonSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException;

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
   * @throws InternalServerErrorException
   *           Thrown if there was an error updating the event.
   * @throws NotFoundException
   *           Thrown if the {@link Event} could not be found.
   * @throws UnauthorizedException
   *           Thrown if the current user is unable to update the event.
   */
  MetadataList updateAllSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException;

}
