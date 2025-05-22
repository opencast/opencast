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

package org.opencastproject.search.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResultList;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.StaticFileAuthorization;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An Opensearch-based {@link SearchService} implementation.
 */
@Component(
    immediate = true,
    service = { SearchService.class, SearchServiceImpl.class, StaticFileAuthorization.class },
    property = {
        "service.description=Search Service",
        "service.pid=org.opencastproject.search.impl.SearchServiceImpl"
    }
)
public final class SearchServiceImpl extends AbstractJobProducer implements SearchService, StaticFileAuthorization {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** The job type */
  public static final String JOB_TYPE = "org.opencastproject.search";

  /** The load introduced on the system by creating an add job */
  public static final float DEFAULT_ADD_JOB_LOAD = 0.1f;

  /** The load introduced on the system by creating a delete job */
  public static final float DEFAULT_DELETE_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the {@link #DEFAULT_ADD_JOB_LOAD} */
  public static final String ADD_JOB_LOAD_KEY = "job.load.add";

  /** The key to look for in the service configuration file to override the {@link #DEFAULT_DELETE_JOB_LOAD} */
  public static final String DELETE_JOB_LOAD_KEY = "job.load.delete";

  /** The load introduced on the system by creating an add job */
  private float addJobLoad = DEFAULT_ADD_JOB_LOAD;

  /** The load introduced on the system by creating a delete job */
  private float deleteJobLoad = DEFAULT_DELETE_JOB_LOAD;

  /** List of available operations on jobs */
  private enum Operation {
    Add, Delete, DeleteSeries
  }

  private SearchServiceIndex index;

  /** The security service */
  private SecurityService securityService;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  /** Persistent storage */
  private SearchServiceDatabase persistence;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectory = null;

  private final LoadingCache<Tuple<User, String>, Boolean> cache;

  private static final Pattern staticFilePattern =
      Pattern.compile("^/([^/]+)/(?:engage-player|engage-live)/([^/]+)/.*$");

  /**
   * Creates a new instance of the search service.
   */
  public SearchServiceImpl() {
    super(JOB_TYPE);

    cache = CacheBuilder.newBuilder()
        .maximumSize(2048)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<>() {
          @Override
          public Boolean load(Tuple<User, String> key) {
            return loadUrlAccess(key.getB());
          }
        });
  }

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  @Override
  @Activate
  public void activate(final ComponentContext cc) throws IllegalStateException {
    super.activate(cc);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  public Job add(MediaPackage mediaPackage) throws SearchException, IllegalArgumentException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Add.toString(),
          Collections.singletonList(MediaPackageParser.getAsXml(mediaPackage)), addJobLoad);
    } catch (ServiceRegistryException e) {
      throw new SearchException(e);
    }
  }

  @Override
  public void addSynchronously(MediaPackage mediaPackage)
          throws SearchException, IllegalArgumentException, UnauthorizedException {
    try {
      index.addSynchronously(mediaPackage);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(e);
    }
  }

  @Override
  public Collection<Pair<Organization, MediaPackage>> getSeries(String seriesId) {
    try {
      return persistence.getSeries(seriesId);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  public Job delete(String mediaPackageId) throws SearchException {
    try {
      return serviceRegistry.createJob(
        JOB_TYPE, Operation.Delete.toString(), Collections.singletonList(mediaPackageId), deleteJobLoad);
    } catch (ServiceRegistryException e) {
      throw new SearchException(e);
    }
  }

  public boolean deleteSynchronously(String mediaPackageId) throws SearchException {
    return index.deleteSynchronously(mediaPackageId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#deleteSeries(java.lang.String)
   */
  public Job deleteSeries(String seriesId) throws SearchException {
    try {
      return serviceRegistry.createJob(
          JOB_TYPE, Operation.DeleteSeries.toString(), Collections.singletonList(seriesId), deleteJobLoad);
    } catch (ServiceRegistryException e) {
      throw new SearchException(e);
    }
  }

  @Override
  public MediaPackage get(String mediaPackageId) throws NotFoundException, UnauthorizedException {
    try {
      return persistence.getMediaPackage(mediaPackageId);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(e);
    }
  }

  public SearchResultList search(SearchSourceBuilder searchSource) throws SearchException {
    SearchResponse idxResp = this.index.search(searchSource);
    return new SearchResultList(idxResp.getHits());
  }


  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      Organization org = organizationDirectory.getOrganization(job.getOrganization());
      User user = userDirectoryService.loadUser(job.getCreator());
      boolean[] deleted = new boolean[1];
      switch (op) {
        case Add:
          MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
          SecurityUtil.runAs(securityService, org, user, () -> {
            try {
              index.addSynchronously(mediaPackage);
            } catch (UnauthorizedException | SearchServiceDatabaseException e) {
              chuck(e);
            }
          });
          return null;
        case Delete:
          String mediapackageId = arguments.get(0);
          SecurityUtil.runAs(securityService, org, user, () -> {
            deleted[0] = index.deleteSynchronously(mediapackageId);
          });
          return Boolean.toString(deleted[0]);
        case DeleteSeries:
          String seriesId = arguments.get(0);
          SecurityUtil.runAs(securityService, org, user, () -> {
            deleted[0] = index.deleteSeriesSynchronously(seriesId);
          });
          return Boolean.toString(deleted[0]);
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  @Reference
  public void setSearchIndex(SearchServiceIndex ssi) {
    this.index = ssi;
  }

  @Reference
  public void setPersistence(SearchServiceDatabase persistence) {
    this.persistence = persistence;
  }


  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectory = organizationDirectory;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectory;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Modified
  public void modified(Map<String, Object> properties) {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      properties = Map.of();
    }

    logger.info("Configuring SearchServiceImpl");
    addJobLoad = LoadUtil.getConfiguredLoadValue(properties, ADD_JOB_LOAD_KEY, DEFAULT_ADD_JOB_LOAD, serviceRegistry);
    deleteJobLoad = LoadUtil.getConfiguredLoadValue(
        properties, DELETE_JOB_LOAD_KEY, DEFAULT_DELETE_JOB_LOAD, serviceRegistry);
  }

  @Override
  public List<Pattern> getProtectedUrlPattern() {
    return Collections.singletonList(staticFilePattern);
  }

  private boolean loadUrlAccess(final String mediaPackageId) {
    logger.debug("Check if user `{}` has access to media package `{}`", securityService.getUser(), mediaPackageId);
    try {
      persistence.getMediaPackage(mediaPackageId);
    } catch (NotFoundException | SearchServiceDatabaseException | UnauthorizedException e) {
      return false;
    }
    return true;
  }

  @Override
  public boolean verifyUrlAccess(final String path) {
    // Always allow access for admin
    final User user = securityService.getUser();
    if (user.hasRole(GLOBAL_ADMIN_ROLE)) {
      logger.debug("Allow access for admin `{}`", user);
      return true;
    }

    // Check pattern
    final Matcher m = staticFilePattern.matcher(path);
    if (!m.matches()) {
      logger.debug("Path does not match pattern. Preventing access.");
      return false;
    }

    // Check organization
    final String organizationId = m.group(1);
    if (!securityService.getOrganization().getId().equals(organizationId)) {
      logger.debug("The user's organization does not match. Preventing access.");
      return false;
    }

    // Check search index/cache
    final String mediaPackageId = m.group(2);
    final boolean access = cache.getUnchecked(Tuple.tuple(user, mediaPackageId));
    logger.debug("Check if user `{}` has access to media package `{}` using cache: {}", user, mediaPackageId, access);
    return access;
  }
}
