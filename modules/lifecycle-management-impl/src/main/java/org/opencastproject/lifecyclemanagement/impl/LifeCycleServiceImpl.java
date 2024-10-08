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
package org.opencastproject.lifecyclemanagement.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventIndexSchema;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQueryField;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCycleDatabaseException;
import org.opencastproject.lifecyclemanagement.api.LifeCycleDatabaseService;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.StartWorkflowParameters;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.requests.SortCriterion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component(
    property = {
        "service.description=LifeCycle Service",
        "service.pid=org.opencastproject.lifecyclemanagement.LifeCycleService"
    },
    immediate = true,
    service = LifeCycleService.class
)
public class LifeCycleServiceImpl implements LifeCycleService {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LifeCycleServiceImpl.class);
  private static final Gson gson = new Gson();

  /** Persistent storage */
  protected LifeCycleDatabaseService persistence;

  /** The security service */
  protected SecurityService securityService;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  protected ElasticsearchIndex index;

  @Reference
  public void setPersistence(LifeCycleDatabaseService persistence) {
    this.persistence = persistence;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Reference
  public void setElasticSearchIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Activate
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating LifeCycle Management Service");
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCyclePolicyById(String) 
   */
  @Override
  public LifeCyclePolicy getLifeCyclePolicyById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCyclePolicy policy = persistence.getLifeCyclePolicy(id);
      if (!checkPermission(policy, Permissions.Action.READ)) {
        throw new UnauthorizedException("User does not have read permissions");
      }
      return policy;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCyclePolicyById(String)
   */
  @Override
  public LifeCyclePolicy getLifeCyclePolicyById(String id, String orgId)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCyclePolicy policy = persistence.getLifeCyclePolicy(id, orgId);
      if (!checkPermission(policy, Permissions.Action.READ)) {
        throw new UnauthorizedException("User does not have read permissions");
      }
      return policy;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCyclePolicies(int, int, SortCriterion)
   */
  @Override
  public List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws IllegalStateException {
    try {
      List<LifeCyclePolicy> policies = persistence.getLifeCyclePolicies(limit, offset, sortCriterion);
      policies.removeIf(policy -> !checkPermission(policy, Permissions.Action.READ));
      return policies;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getActiveLifeCyclePolicies
   */
  @Override
  public List<LifeCyclePolicy> getActiveLifeCyclePolicies() throws IllegalStateException {
    try {
      String orgId = securityService.getOrganization().getId();
      List<LifeCyclePolicy> policies = persistence.getActiveLifeCyclePolicies(orgId);
      policies.removeIf(policy -> !checkPermission(policy, Permissions.Action.READ));
      return policies;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#createLifeCyclePolicy(LifeCyclePolicy)
   */
  @Override
  public LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy) throws UnauthorizedException {
    try {
      if (policy.getOrganization() != null) {
        policy = persistence.createLifeCyclePolicy(policy, policy.getOrganization());
      } else {
        policy = persistence.createLifeCyclePolicy(policy, securityService.getOrganization().getId());
      }
      return policy;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#updateLifeCyclePolicy(LifeCyclePolicy)
   */
  @Override
  public boolean updateLifeCyclePolicy(LifeCyclePolicy policy)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException {
    try {
      LifeCyclePolicy existingPolicy = persistence.getLifeCyclePolicy(policy.getId());
      if (!checkPermission(existingPolicy, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }
    } catch (NotFoundException | LifeCycleDatabaseException e) {
      throw new IllegalStateException("Could not get policy from database with id ");
    }

    if (policy.getOrganization() == null) {
      policy.setOrganization(securityService.getOrganization().getId());
    }

    try {
      return persistence.updateLifeCyclePolicy(policy, securityService.getOrganization().getId());
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#deleteLifeCyclePolicy(String)
   */
  @Override
  public boolean deleteLifeCyclePolicy(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCyclePolicy policy = persistence.getLifeCyclePolicy(id);
      if (!checkPermission(policy, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }
      return persistence.deleteLifeCyclePolicy(policy, securityService.getOrganization().getId());
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException("Could not delete policy from database with id " + id);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#deleteLifeCyclePolicy(String)
   */
  public void deleteAllLifeCyclePoliciesCreatedByConfig(String orgId)
          throws IllegalStateException {
    try {
      persistence.deleteAllLifeCyclePoliciesCreatedByConfig(orgId);
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException("Could not delete policies created from config from database. ");
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCycleTaskById(String)
   */
  @Override
  public LifeCycleTask getLifeCycleTaskById(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCycleTask task = persistence.getLifeCycleTask(id);
//      if (!checkPermission(task, Permissions.Action.READ)) {
//        throw new UnauthorizedException("User does not have read permissions");
//      }
      return task;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCycleTaskByTargetId(String)
   */
  @Override
  public LifeCycleTask getLifeCycleTaskByTargetId(String targetId)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCycleTask task = persistence.getLifeCycleTaskByTargetId(targetId);
      //      if (!checkPermission(task, Permissions.Action.READ)) {
      //        throw new UnauthorizedException("User does not have read permissions");
      //      }
      return task;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#getLifeCycleTasksWithStatus(Status) 
   */
  @Override
  public List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status) throws IllegalStateException {
    try {
      String orgId = securityService.getOrganization().getId();
      List<LifeCycleTask> tasks = persistence.getLifeCycleTasksWithStatus(status, orgId);
      //      if (!checkPermission(policies, Permissions.Action.READ)) {
      //        throw new UnauthorizedException("User does not have read permissions");
      //      }
      return tasks;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#createLifeCycleTask(LifeCycleTask) 
   */
  @Override
  public LifeCycleTask createLifeCycleTask(LifeCycleTask task) throws UnauthorizedException {
    try {
      task = persistence.createLifeCycleTask(task, securityService.getOrganization().getId());
      return task;
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#updateLifeCycleTask(LifeCycleTask)
   */
  @Override
  public boolean updateLifeCycleTask(LifeCycleTask task)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException {
    try {
      LifeCycleTask existingTask = persistence.getLifeCycleTask(task.getId());
//      if (!checkPermission(existingTask, Permissions.Action.WRITE)) {
//        throw new UnauthorizedException("User does not have write permissions");
//      }
    } catch (NotFoundException | LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }

    if (task.getOrganization() == null) {
      task.setOrganization(securityService.getOrganization().getId());
    }

    try {
      return persistence.updateLifeCycleTask(task, securityService.getOrganization().getId());
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#deleteLifeCycleTask(String) 
   */
  @Override
  public boolean deleteLifeCycleTask(String id)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      LifeCycleTask task = persistence.getLifeCycleTask(id);
//      if (!checkPermission(task, Permissions.Action.WRITE)) {
//        throw new UnauthorizedException("User does not have write permissions");
//      }
      return persistence.deleteLifeCycleTask(task, securityService.getOrganization().getId());
    } catch (LifeCycleDatabaseException e) {
      throw new IllegalStateException("Could not delete task from database with id " + id);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#checkValidity(LifeCyclePolicy)
   */
  public boolean checkValidity(LifeCyclePolicy policy) {
    if (policy.getTitle() == null || policy.getTitle().isEmpty()) {
      logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Missing title.");
      return false;
    }
    if (policy.getTargetType() == null) {
      logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Missing target type.");
      return false;
    }
    if (policy.getAction() == null) {
      logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Missing action.");
      return false;
    }
    if (policy.getTiming() == null) {
      logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Missing timing.");
      return false;
    }

    // Check if conditionally required fields are present
    if (policy.getTiming() == Timing.SPECIFIC_DATE) {
      if (policy.getActionDate() == null) {
        logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. A policy with the timing"
            + Timing.SPECIFIC_DATE + " must have an action date.");
        return false;
      }
    }
    if (policy.getTiming() == Timing.REPEATING) {
      if (policy.getCronTrigger() == null || policy.getCronTrigger().isEmpty()) {
        logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. A policy with the timing"
            + Timing.REPEATING + " must have an cron trigger.");
        return false;
      }
    }

    // Check if cron string is valid
    if (policy.getCronTrigger() != null && !policy.getCronTrigger().isEmpty()) {
      if (!org.quartz.CronExpression.isValidExpression(policy.getCronTrigger())) {
        logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Cron trigger "
            + policy.getCronTrigger() + " is not valid.");
        return false;
      }
    }

    // Check if action parameters are well formed
    try {
      if (policy.getAction() == Action.START_WORKFLOW) {
        StartWorkflowParameters actionParametersParsed = gson.fromJson(policy.getActionParameters(),
            StartWorkflowParameters.class);
        if (actionParametersParsed.getWorkflowId() == null) {
          logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Action parameters for the action"
              + Action.START_WORKFLOW + " must include workflowId.");
          return false;
        }
      }
    } catch (JsonSyntaxException e) {
      logger.warn("LifeCyclePolicy with id " + policy.getId() + " is not valid. Action parameters are not well formed"
          + ".");
      return false;
    }

    if (policy.getAccessControlEntries() == null || policy.getAccessControlEntries().isEmpty()) {
      policy.setAccessControlEntries(new ArrayList<>());
    }

    return true;
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleService#filterForEvents(Map)
   */
  public List<Event> filterForEvents(Map<String, EventSearchQueryField<String>> filters) throws SearchIndexException {
    try {
      SearchResult<Event> results = null;
      List<Event> eventsList = new ArrayList<>();
      final Organization organization = securityService.getOrganization();
      final User user = securityService.getUser();
      //      if (organization == null || user == null) {
      //        return Response.status(SC_SERVICE_UNAVAILABLE).build();
      //      }
      EventSearchQuery query = new EventSearchQuery(organization.getId(), user);

      addFiltersToQuery(query, filters);

      results = index.getByQuery(query);

      for (SearchResultItem<Event> item : results.getItems()) {
        Event source = item.getSource();
        eventsList.add(source);
      }

      return eventsList;
    } catch (SearchIndexException e) {
      logger.error("The Search Index was not able to get the events list:", e);
      throw e;
    }
  }

  /**
   * Add arguments to a query for the index
   * @param query The query the arguments should be added to
   * @param filters The arguments that should be added to the query
   */
  private void addFiltersToQuery(EventSearchQuery query, Map<String, EventSearchQueryField<String>> filters) {
    for (String name : filters.keySet()) {
      switch (name) {
        case EventIndexSchema.UID -> query.withIdentifier(filters.get(name).getValue());
        case EventIndexSchema.TITLE -> query.withTitle(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.DESCRIPTION -> query.withDescription(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.PRESENTER -> query.withPresenter(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.CONTRIBUTOR-> query.withContributor(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.SUBJECT -> query.withSubject(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.LOCATION -> query.withLocation(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.SERIES_ID -> query.withSeriesId(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.SERIES_NAME -> query.withSeriesName(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.LANGUAGE -> query.withLanguage(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.SOURCE -> query.withSource(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.CREATED -> query.withCreated(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.CREATOR -> query.withCreator(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.PUBLISHER -> query.withPublisher(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.LICENSE -> query.withLicense(
            filters.get(name).getValue(), filters.get(name).getType(), filters.get(name).isMust());
        case EventIndexSchema.RIGHTS -> query.withRights(filters.get(name).getValue());
        case EventIndexSchema.START_DATE -> query.withStartDate(
            filters.get(name).getValue(), filters.get(name).getType());

        case "start_date_range" -> {
          Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(filters.get(name).getValue());
          query.withStartFrom(fromAndToCreationRange.getA());
          query.withStartTo(fromAndToCreationRange.getB());
        }
        case "start_date_relative" -> {
          Period period = Period.parse(filters.get(name).getValue());
          LocalDateTime now = LocalDateTime.now();
          LocalDateTime relative = now.plus(period);
          query.withStartDate(relative.atZone(ZoneId.systemDefault()).toInstant().toString(),
              filters.get(name).getType());
        }

        default -> logger.debug("Filter " + name + " is not supported");
      }
    }
  }

  /**
   * Runs a permission check on the given policy for the given action
   * @param policy {@link LifeCyclePolicy} to check permission for
   * @param action Action to check permission for
   * @return True if action is permitted on the {@link LifeCyclePolicy}, else false
   */
  private boolean checkPermission(LifeCyclePolicy policy, Permissions.Action action) {
    User currentUser = securityService.getUser();
    Organization currentOrg = securityService.getOrganization();
    String currentOrgAdminRole = currentOrg.getAdminRole();
    String currentOrgId = currentOrg.getId();

    return currentUser.hasRole(GLOBAL_ADMIN_ROLE)
        || (currentUser.hasRole(currentOrgAdminRole) && currentOrgId.equals(policy.getOrganization()))
        || authorizationService.hasPermission(getAccessControlList(policy), action.toString());
  }

  /**
   * Parse the access information for a policy from its database format into an {@link AccessControlList}
   * @param policy The {@link LifeCyclePolicy} to get the {@link AccessControlList} for
   * @return The {@link AccessControlList} for the given {@link LifeCyclePolicy}
   */
  private AccessControlList getAccessControlList(LifeCyclePolicy policy) {
    List<AccessControlEntry> accessControlEntries = new ArrayList<>();
    for (LifeCyclePolicyAccessControlEntry entry : policy.getAccessControlEntries()) {
      accessControlEntries.add(entry.toAccessControlEntry());
    }
    return new AccessControlList(accessControlEntries);
  }
}
