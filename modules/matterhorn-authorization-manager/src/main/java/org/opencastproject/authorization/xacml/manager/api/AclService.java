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
package org.opencastproject.authorization.xacml.manager.api;

import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import java.util.Date;
import java.util.List;

/**
 * ACL service API for managing and scheduling ACLs.
 */
public interface AclService {

  /**
   * Adds an episode ACL to schedule at the given date
   *
   * @param episodeId
   *          the episode id
   * @param managedAclId
   *          The managed acl id or none if to delete. This implies fallback to the series ACL.
   * @param at
   *          the date when the acl applies or gets deleted respectively
   * @param workflow
   *          the workflow to apply
   * @return the episode transition
   * @throws AclServiceException
   *           if exception occurred
   */
  EpisodeACLTransition addEpisodeTransition(String episodeId, Option<Long> managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException;

  /**
   * Adds a series ACL to schedule at the given date
   *
   * @param seriesId
   *          the series id
   * @param managedAclId
   *          the managed acl identifier
   * @param at
   *          the date when the acl applies
   * @param workflow
   *          the workflow to apply
   * @param override
   *          if true the series ACL will take precedence over any existing episode ACL
   * @return the series transition
   * @throws AclServiceException
   *           if exception occurred
   */
  SeriesACLTransition addSeriesTransition(String seriesId, long managedAclId, Date at, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException;

  /**
   * Updates an existing episode transition
   *
   * @param transitionId
   *          the transition id
   * @param managedAclId
   *          The managed acl id or none if to delete. This implies fallback to the series ACL.
   * @param at
   *          the date when the acl applies or gets deleted respectively
   * @param workflow
   *          the workflow to apply
   * @return the episode transition
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the scheduled episode ACL was not found
   */
  EpisodeACLTransition updateEpisodeTransition(long transitionId, Option<Long> managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException, NotFoundException;

  /**
   * Updates an existing series transition
   *
   * @param transitionId
   *          the transition id
   * @param managedAclId
   *          the managed acl identifier
   * @param at
   *          the date when the acl applies
   * @param workflow
   *          the workflow to apply
   * @param override
   *          if true the series ACL will take precedence over any existing episode ACL
   * @return the series transition
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the scheduled series ACL was not found
   */
  SeriesACLTransition updateSeriesTransition(long transitionId, long managedAclId, Date at,
          Option<ConfiguredWorkflowRef> workflow, boolean override) throws AclServiceException, NotFoundException;

  /**
   * Marks a series transition as completed
   *
   * @param transitionId
   *          the transition identifier
   * @return the completed series ACL transition
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the scheduled series ACL was not found
   */
  SeriesACLTransition markSeriesTransitionAsCompleted(long transitionId) throws AclServiceException, NotFoundException;

  /**
   * Marks an episode transition as completed
   *
   * @param transitionId
   *          the transition identifier
   * @return the completed episode ACL transition
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the scheduled episode ACL was not found
   */
  EpisodeACLTransition markEpisodeTransitionAsCompleted(long transitionId) throws AclServiceException,
          NotFoundException;

  /**
   * Deletes a episode transition by it's transition id
   *
   * @param transitionId
   *          the transition id
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the episode transition could not be found
   */
  void deleteEpisodeTransition(long transitionId) throws AclServiceException, NotFoundException;

  /**
   * Deletes a series transition by it's transition id
   *
   * @param transitionId
   *          the transition id
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if the series transition could not be found
   */
  void deleteSeriesTransition(long transitionId) throws AclServiceException, NotFoundException;

  /**
   * Deletes all transitions for a given episode.
   *
   * @param episodeId
   *          the episode id
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if series id could not be found
   */
  void deleteEpisodeTransitions(String episodeId) throws AclServiceException, NotFoundException;

  /**
   * Delete all transitions for a given series.
   *
   * @param seriesId
   *          the series id
   * @throws AclServiceException
   *           if exception occurred
   * @throws NotFoundException
   *           if series id could not be found
   */
  void deleteSeriesTransitions(String seriesId) throws AclServiceException, NotFoundException;

  /**
   * Returns the transition result by the given transition query.
   *
   * @param query
   *          the transition query
   * @return the transition result
   * @throws AclServiceException
   *           if exception occurred
   */
  TransitionResult getTransitions(TransitionQuery query) throws AclServiceException;

  /**
   * Immediate ACL transition application to an episode.
   *
   * @throws AclServiceException
   *           in case of any error
   */
  void applyEpisodeAclTransition(final EpisodeACLTransition t) throws AclServiceException;

  /**
   * Immediate ACL application to an episode.
   *
   * @param episodeId
   *          the episode id
   * @param managedAcl
   *          the ACL to apply, <code>none</code> to delete the episode ACL from the media package to cause a fallback
   *          to the series ACL
   * @param workflow
   *          an optional workflow to apply to the episode afterwards
   * @return true if the episode exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToEpisode(String episodeId, Option<ManagedAcl> managedAcl, Option<ConfiguredWorkflowRef> workflow)
          throws AclServiceException;

  /**
   * Immediate ACL application to an episode.
   *
   * @param episodeId
   *          the episode id
   * @param acl
   *          the ACL to apply, <code>null</code> to delete the episode ACL from the media package to cause a fallback
   *          to the series ACL
   * @param workflow
   *          an optional workflow to apply to the episode afterwards
   * @return true if the episode exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToEpisode(String episodeId, AccessControlList acl, Option<ConfiguredWorkflowRef> workflow)
          throws AclServiceException;

  /**
   * Immediate ACL transition application to a series.
   *
   * @throws AclServiceException
   *           in case of any error
   */
  void applySeriesAclTransition(final SeriesACLTransition t) throws AclServiceException;

  /**
   * Immediate ACL application to a series.
   *
   * @param seriesId
   *          the series id
   * @param managedAcl
   *          the ACL to apply
   * @param override
   *          if true it will force the use the series ACL for all the episodes in the series. Otherwise, only episodes
   *          that don't have a custom access control list defined will be adjusted to the new series ACL.
   * @param workflow
   *          an optional workflow to apply to the episode afterwards
   * @return false if the series doesn't exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToSeries(String seriesId, ManagedAcl managedAcl, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException;

  /**
   * Immediate ACL application to a series.
   *
   * @param seriesId
   *          the series id
   * @param acl
   *          the ACL to apply
   * @param override
   *          if true it will force the use the series ACL for all the episodes in the series. Otherwise, only episodes
   *          that don't have a custom access control list defined will be adjusted to the new series ACL.
   * @param workflow
   *          an optional workflow to apply to the episode afterwards
   * @return false if the series doesn't exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToSeries(String seriesId, AccessControlList acl, boolean override,
          Option<ConfiguredWorkflowRef> workflow) throws AclServiceException;

  /**
   * Return all ACLs of this organization.
   */
  List<ManagedAcl> getAcls();

  /**
   * Return an ACL of an organization by its ID.
   *
   * @return <code>some</code> if the ACL could be found, <code>none</code> if the ACL with the given ID does not exist.
   */
  Option<ManagedAcl> getAcl(long id);

  /**
   * Update an existing ACL.
   *
   * @return true on a successful update, false if no ACL exists with the given ID.
   */
  boolean updateAcl(ManagedAcl acl);

  /**
   * Create a new ACL.
   *
   * @return <code>some</code> if the new ACL could be created successfully, <code>none</code> if an ACL with the same
   *         name already exists
   */
  Option<ManagedAcl> createAcl(AccessControlList acl, String name);

  /**
   * Delete an ACL by its ID.
   *
   * @return <code>true</code> if the ACL existed and could be deleted successfully, <code>false</code> if the ACL can't
   *         be deleted because it still has active references on it.
   * @throws NotFoundException
   *           if the managed acl could not be found
   * @throws AclServiceException
   *           if exception occurred
   */
  boolean deleteAcl(long id) throws AclServiceException, NotFoundException;
}
