/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.episode.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.util.List;

/**
 * The episode service is Matterhorn's media package archive and re-processing hub.
 * Each method may throw an {@link org.opencastproject.episode.api.EpisodeServiceException} indicating
 * an error situation.
 */
public interface EpisodeService {
  /** Identifier for service registration and location */
  String JOB_TYPE = "org.opencastproject.episode";

  /**
   * The {@link org.opencastproject.security.api.AccessControlEntry#getAction()} that allows a user in a particular role
   * to see a mediapackage in the search index.
   */
  String READ_PERMISSION = "read";

  /**
   * The {@link org.opencastproject.security.api.AccessControlEntry#getAction()} that allows a user in a particular role
   * to update or remove a mediapackage from the search index.
   */
  String WRITE_PERMISSION = "write";

  /**
   * The {@link org.opencastproject.security.api.AccessControlEntry#getAction()} that allows a user in a particular role
   * to contribute a mediapackage from the search index.
   */
  String CONTRIBUTE_PERMISSION = "contribute";

  /**
   * Adds the media package to the archive.
   *
   * @param mediaPackage
   *         the media package
   * @throws EpisodeServiceException
   *         if an error occurs while adding the media package
   */
  void add(MediaPackage mediaPackage) throws EpisodeServiceException;

  /**
   * Removes the media package identified by <code>mediaPackageId</code> from the archive.
   *
   * @param mediaPackageId
   *         id of the media package to remove
   * @return <code>true</code> if the episode was found and deleted
   */
  boolean delete(String mediaPackageId) throws EpisodeServiceException;

  /**
   * Process all media packages found by query <code>q</code> with workflow <code>workflowDefinition</code>.
   *
   * @param workflow
   *         the configured workflow to apply to media packages
   * @param rewriteUri
   *         a function to rewrite media package element URLs
   * @param q
   *         the query
   * @return a list of started workflows
   */
  // todo change return value to report about failed workflow starts
  List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                       UriRewriter rewriteUri,
                                       EpisodeQuery q) throws EpisodeServiceException;

  /**
   * Process all media packages with workflow <code>workflowDefinition</code>.
   *
   * @param workflow
   *         the configured workflow to apply to media packages
   * @param rewriteUri
   *         a function to rewrite media package element URLs
   * @param mediaPackageIds
   *         list of media package ids
   * @return a list of started workflows
   * @throws EpisodeServiceException
   */
  // todo change return value to report about failed workflow starts
  List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                       UriRewriter rewriteUri,
                                       List<String> mediaPackageIds) throws EpisodeServiceException;

  /**
   * Find search results based on the specified query object.
   *
   * @param q
   *         The {@link EpisodeQuery} containing the details of the desired results
   * @return The search result
   * @throws EpisodeServiceException
   *         if an error occurs while searching for media packages
   */
  SearchResult find(EpisodeQuery q) throws EpisodeServiceException;

  /**
   * Finds search results across any organization, protected by any access control. This should be used for
   * administrative purposes, such as bulk edits based on metadata updates.
   *
   * @param q
   *         The {@link EpisodeQuery} containing the details of the desired results
   * @return The search result
   * @throws EpisodeServiceException
   *         if an error occurs while searching for media packages
   */
  SearchResult findForAdministrativeRead(EpisodeQuery q) throws EpisodeServiceException;

  /** Get an archived media package element. */
  Option<ArchivedMediaPackageElement> get(String mediaPackageId, String mediaPackageElementId, Version version)
          throws EpisodeServiceException;
}
