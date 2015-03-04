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
package org.opencastproject.archive.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.util.List;

/**
 * The episode service is Matterhorn's media package archive and re-processing hub.
 * Each method may throw an {@link ArchiveException} indicating an error situation.
 *
 * @param <RS> type of returned the result set
 */
public interface Archive<RS extends ResultSet> {
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

  /** The Archive job type */
  String JOB_TYPE = "org.opencastproject.archive";

  /**
   * Adds the media package to the archive.
   *
   * @param mediaPackage
   *         the media package
   * @throws ArchiveException
   *          in case of any internal error
   */
  void add(MediaPackage mediaPackage) throws ArchiveException;

  /**
   * Removes the media package identified by <code>mediaPackageId</code> from the archive.
   *
   * @param mediaPackageId
   *         id of the media package to remove
   * @return <code>true</code> if the episode was found and deleted
   * @throws ArchiveException
   *          in case of any internal error
   */
  boolean delete(String mediaPackageId) throws ArchiveException;

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
   * @throws ArchiveException
   *          in case of any internal error
   */
  // todo change return value to report about failed workflow starts
  List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                       UriRewriter rewriteUri,
                                       Query q) throws ArchiveException;

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
   * @throws ArchiveException
   *          in case of any internal error
   */
  // todo change return value to report about failed workflow starts
  List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                       UriRewriter rewriteUri,
                                       List<String> mediaPackageIds) throws ArchiveException;

  /**
   * Find search results based on the specified query object.
   *
   * @param q
   *         The {@link Query} containing the details of the desired results
   * @param rewriteUri
   *         a function to rewrite media package element URLs
   * @return The search result
   * @throws ArchiveException
   *          in case of any internal error
   */
  RS find(Query q, UriRewriter rewriteUri) throws ArchiveException;

  /**
   * Finds search results across any organization, protected by any access control. This should be used for
   * administrative purposes, such as bulk edits based on metadata updates.
   *
   * @param q
   *         The {@link Query} containing the details of the desired results
   * @param rewriteUri
   *         a function to rewrite media package element URLs
   * @return The search result
   * @throws ArchiveException
   *          in case of any internal error
   */
  // todo remove in favour of an enhanced query
  RS findForAdministrativeRead(Query q, UriRewriter rewriteUri) throws ArchiveException;

  /**
   * Get an archived media package element. The element is uniquely addressed by the triple
   * <code>(mediaPackageId, mediaPackageElementId, version)</code>
   *
   * @throws ArchiveException
   *          in case of any internal error
   */
  Option<ArchivedMediaPackageElement> get(String mediaPackageId, String mediaPackageElementId, Version version)
          throws ArchiveException;
}
