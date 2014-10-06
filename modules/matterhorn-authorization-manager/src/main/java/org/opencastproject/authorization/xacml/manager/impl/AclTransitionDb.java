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
package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import java.util.Date;
import java.util.List;

/** API that defines persistent storage of ACL transitions. */
public interface AclTransitionDb {
  /**
   * Store a scheduled episode ACL.
   *
   * @param episodeId
   *         the episode identifier
   * @param applicationDate
   *         the date of application
   * @param managedAclId
   *         the managed access control list id
   * @param workflow
   *         the workflow and its parameters
   * @return the stored episode ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   */
  EpisodeACLTransition storeEpisodeAclTransition(Organization organization,
                                                 String episodeId,
                                                 Date applicationDate,
                                                 Option<Long> managedAclId,
                                                 Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException;

  /**
   * Store a scheduled series ACL.
   *
   * @param seriesId
   *         the series identifier
   * @param applicationDate
   *         the date of application
   * @param managedAclId
   *         the managed access control list id
   * @param workflow
   *         the workflow and its parameters
   * @param override
   *         the override flag
   * @return the stored series ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   */
  SeriesACLTransition storeSeriesAclTransition(Organization organization,
                                               String seriesId,
                                               Date applicationDate,
                                               long managedAclId,
                                               boolean override,
                                               Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException;

  /**
   * Update a scheduled episode transition.
   *
   * @param transitionId
   *         the transition identifier
   * @param applicationDate
   *         the date of application
   * @param managedAclId
   *         the managed access control list id
   * @param workflow
   *         the workflow and its parameters
   * @return the updated episode ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled ACL was not found
   */
  EpisodeACLTransition updateEpisodeAclTransition(Organization organization,
                                                  long transitionId,
                                                  Date applicationDate,
                                                  Option<Long> managedAclId,
                                                  Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException, NotFoundException;

  /**
   * Update a scheduled series transition.
   *
   * @param transitionId
   *         the transition identifier
   * @param applicationDate
   *         the date of application
   * @param managedAclId
   *         the managed access control list id
   * @param workflow
   *         the workflow and its parameters
   * @param override
   *         the override flag
   * @return the updated series ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled ACL was not found
   */
  SeriesACLTransition updateSeriesAclTransition(Organization organization,
                                                long transitionId,
                                                Date applicationDate,
                                                long managedAclId,
                                                boolean override,
                                                Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException, NotFoundException;

  /**
   * Marks a series transition as completed
   *
   * @param transitionId
   *         the transition identifier
   * @return the completed series ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled series ACL was not found
   */
  SeriesACLTransition markSeriesTransitionAsCompleted(Organization organization, long transitionId)
          throws AclTransitionDbException, NotFoundException;

  /**
   * Marks an episode transition as completed
   *
   * @param transitionId
   *         the transition identifier
   * @return the completed episode ACL transition
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled episode ACL was not found
   */
  EpisodeACLTransition markEpisodeTransitionAsCompleted(Organization organization,
                                                        long transitionId) throws AclTransitionDbException,
          NotFoundException;

  /**
   * Delete a scheduled episode ACL.
   *
   * @param transitionId
   *         the transition identifier
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled ACL was not found
   */
  void deleteEpisodeAclTransition(Organization organization,
                                  long transitionId) throws AclTransitionDbException, NotFoundException;

  /**
   * Delete a scheduled series ACL.
   *
   * @param transitionId
   *         the transition identifier
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   * @throws NotFoundException
   *         if the scheduled ACL was not found
   */
  void deleteSeriesAclTransition(Organization organization,
                                 long transitionId) throws AclTransitionDbException, NotFoundException;

  /**
   * Returns a list of all scheduled episode ACL entries
   *
   * @param episodeId
   *         the episode identifier
   * @return the list of all scheduled episode ALC entries
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   */
  List<EpisodeACLTransition> getEpisodeAclTransitions(Organization organization,
                                                      String episodeId) throws AclTransitionDbException;

  /**
   * Returns a list of all scheduled series ACL entries
   *
   * @param seriesId
   *         the series identifier
   * @return the list of all scheduled series ACL entries
   * @throws AclTransitionDbException
   *         if exception occurs during reading/storing from the persistence layer
   */
  List<SeriesACLTransition> getSeriesAclTransitions(Organization organization,
                                                    String seriesId) throws AclTransitionDbException;

  /**
   * Returns the transition result by the given transition query
   *
   * @param query
   *         the transition query
   * @return the transition result
   */
  TransitionResult getByQuery(Organization organization, TransitionQuery query) throws AclTransitionDbException;
}
