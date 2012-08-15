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
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.util.List;
import java.util.Map;

/**
 * The episode service is Matterhorn's media package archive. It also supports batch reprocessing
 */
public interface EpisodeService {
  /**
   * Identifier for service registration and location
   */
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
   * Adds the media package to the archive.
   *
   * @param mediaPackage
   *          the media package
   * @throws EpisodeServiceException
   *           if an error occurs while adding the media package
   * @throws MediaPackageException
   *           if an error occurs accessing the media package
   * @throws UnauthorizedException
   *           if the current user is not authorized to execute the operation
   * @throws ServiceRegistryException
   *           if the job for adding the mediapackage can not be created
   */
  void add(MediaPackage mediaPackage) throws EpisodeServiceException, MediaPackageException, UnauthorizedException,
          ServiceRegistryException;

  /**
   * Removes the media package identified by <code>mediaPackageId</code> from the archive.
   *
   * @param mediaPackageId
   *          id of the media package to remove
   * @return <code>true</code> if the episode was found and deleted
   * @throws EpisodeServiceException
   *           if an error occurs while removing the media package
   * @throws UnauthorizedException
   *           if the current user is not authorized to execute the operation
   */
  boolean delete(String mediaPackageId) throws EpisodeServiceException, UnauthorizedException;

  /**
   * Lock or unlock a media package.
   *
   * @param mediaPackageId
   *          id of the media package to (un)lock
   * @return <code>true</code> if the episode was found and (un)locked
   * @throws EpisodeServiceException
   *           if an error occurs while (un)locking the media package
   * @throws UnauthorizedException
   *           if the current user is not authorized to execute the operation
   */
  boolean lock(String mediaPackageId, boolean lock) throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages found by query <code>q</code> with workflow <code>workflowDefinition</code>.
   *
   * @param workflowDefinition
   *          the workflow to apply to all found media packages
   * @param q
   *          the query
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, EpisodeQuery q) throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages found by query <code>q</code> with workflow <code>workflowDefinition</code> and
   * workflow properties <code>properties</code>.
   *
   * @param workflowDefinition
   *          the workflow to apply to all found media packages
   * @param q
   *          the query
   * @param properties
   *          properties to configure the workflow
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, EpisodeQuery q, Map<String, String> properties)
          throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages with workflow <code>workflowDefinition</code>.
   *
   * @param mediaPackageIds
   *          list of media package ids
   * @param workflowDefinition
   *          the workflow to apply to all found media packages
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, List<String> mediaPackageIds)
          throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages with workflow <code>workflowDefinition</code> and
   * workflow properties <code>properties</code>.
   *
   * @param mediaPackageIds
   *          list of media package ids
   * @param workflowDefinition
   *          the workflow to apply to all found media packages
   * @param properties
   *          properties to configure the workflow
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, List<String> mediaPackageIds, Map<String, String> properties)
          throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages with workflow identified by <code>workflowDefinitionId</code>.
   *
   * @param mediaPackageIds
   *          list of media package ids
   * @param workflowDefinitionId
   *          the workflow to apply to all found media packages
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(String workflowDefinitionId, List<String> mediaPackageIds) throws EpisodeServiceException, UnauthorizedException;

  /**
   * Process all media packages with workflow identified by <code>workflowDefinitionId</code> and
   * workflow properties <code>properties</code>.
   *
   * @param mediaPackageIds
   *          list of media package ids
   * @param workflowDefinitionId
   *          the workflow to apply to all found media packages
   * @param properties
   *          properties to configure the workflow
   * @return a list of started workflows todo should report about failed workflow starts
   * @throws EpisodeServiceException
   * @throws UnauthorizedException
   */
  WorkflowInstance[] applyWorkflow(String workflowDefinitionId, List<String> mediaPackageIds, Map<String, String> properties)
          throws EpisodeServiceException, UnauthorizedException;

  /**
   * Find search results based on the specified query object
   *
   * @param q
   *          The {@link EpisodeQuery} containing the details of the desired results
   * @return The search result
   * @throws EpisodeServiceException
   *           if an error occurs while searching for media packages
   */
  SearchResult getByQuery(EpisodeQuery q) throws EpisodeServiceException;

  /**
   * Finds search results across any organization, protected by any access control. This should be used for
   * administrative purposes, such as bulk edits based on metadata updates.
   *
   * @param q
   *          The {@link EpisodeQuery} containing the details of the desired results
   * @return The search result
   * @throws EpisodeServiceException
   *           if an error occurs while searching for media packages
   * @throws UnauthorizedException
   *           if the user does not an administrative role
   */
  SearchResult getForAdministrativeRead(EpisodeQuery q) throws EpisodeServiceException, UnauthorizedException;

  /**
   * Sends a query to the search service. Depending on the service implementation, the query might be an sql statement a
   * solr query or something similar. In the future, a higher level query language might be a better solution.
   *
   * @param query
   *          the search query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws EpisodeServiceException
   */
  SearchResult getByQuery(String query, int limit, int offset) throws EpisodeServiceException;
}
