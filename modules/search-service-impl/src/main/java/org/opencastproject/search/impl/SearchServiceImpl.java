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

package org.opencastproject.search.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.StaticFileAuthorization;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Solr-based {@link SearchService} implementation.
 */
@Component(
    immediate = true,
    service = { SearchService.class,SearchServiceImpl.class,ManagedService.class,StaticFileAuthorization.class },
    property = {
        "service.description=Search Service",
        "service.pid=org.opencastproject.search.impl.SearchServiceImpl"
    }
)
public final class SearchServiceImpl extends AbstractJobProducer implements SearchService, ManagedService,
    StaticFileAuthorization {

  public enum IndexEntryType {
    Episode, Series
  }

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** The job type */
  public static final String JOB_TYPE = "org.opencastproject.search";

  /** The load introduced on the system by creating an add job */
  public static final float DEFAULT_ADD_JOB_LOAD = 0.1f;

  /** The load introduced on the system by creating a delete job */
  public static final float DEFAULT_DELETE_JOB_LOAD = 0.1f;

  public static final String ADD_JOB_LOAD_KEY = "job.load.add";

  public static final String INDEX_NAME = "opencast_search";

  public static final String DELETE_JOB_LOAD_KEY = "job.load.delete";

  /** The load introduced on the system by creating an add job */
  private float addJobLoad = DEFAULT_ADD_JOB_LOAD;

  /** The load introduced on the system by creating a delete job */
  private float deleteJobLoad = DEFAULT_DELETE_JOB_LOAD;

  /** List of available operations on jobs */
  private enum Operation {
    Add, Delete
  }

  private final Gson gson = new Gson();

  private ElasticsearchIndex elasticsearchIndex;

  private SeriesService seriesService;

  /** The local workspace */
  private Workspace workspace;

  /** The security service */
  private SecurityService securityService;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  /** Persistent storage */
  private SearchServiceDatabase persistence;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectory = null;

  private final LoadingCache<Tuple<User, String>, Boolean> cache;

  private static final Pattern staticFilePattern = Pattern.compile("^/([^/]+)/engage-player/([^/]+)/.*$");

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
   * Service activator, called via declarative services configuration. If the
   * solr server url is configured, we try to connect to it. If not, the solr
   * data directory with an embedded Solr server is used.
   *
   * @param cc
   *          the component context
   */
  @Override
  @Activate
  public void activate(final ComponentContext cc) throws IllegalStateException {
    super.activate(cc);
    createIndex();
  }

  private void createIndex() {
    var mapping = "";
    try (var in = this.getClass().getResourceAsStream("/search-mapping.json")) {
      mapping = IOUtils.toString(in, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new SearchException("Could not read mapping.", e);
    }
    try {
      logger.debug("Trying to create index for '{}'", INDEX_NAME);
      var request = new CreateIndexRequest(INDEX_NAME)
          .mapping(mapping, XContentType.JSON);
      var response = elasticsearchIndex.getClient().indices().create(request, RequestOptions.DEFAULT);
      if (!response.isAcknowledged()) {
        throw new SearchException("Unable to create index for '" + INDEX_NAME + "'");
      }
    } catch (ElasticsearchStatusException e) {
      if (e.getDetailedMessage().contains("already_exists_exception")) {
        logger.info("Detected existing index '{}'", INDEX_NAME);
      } else {
        throw e;
      }
    } catch (IOException e) {
      throw new SearchException(e);
    }
  }

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
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

  /**
   * Simplify ACL structure, so we can easily search by action.
   * @param acl The access control List to restructure
   * @return Restructured ACL
   */
  Map<String, Set<String>> searchableAcl(AccessControlList acl) {
    var result = new HashMap<String, Set<String>>();
    for (var entry: acl.getEntries()) {
      var action = entry.getAction();
      if (!result.containsKey(action)) {
        result.put(action, new HashSet<>());
      }
      result.get(action).add(entry.getRole());
    }
    return  result;
  }

  public SearchResponse search(SearchSourceBuilder searchSource) throws SearchException {
    SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
    logger.debug("Sending for query: {}", searchSource.query());
    searchRequest.source(searchSource);
    try {
      return elasticsearchIndex.getClient().search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new SearchException(e);
    }
  }

  private Map<String, List<String>> mapDublinCore(DublinCoreCatalog dublinCoreCatalog) {
    var metadata = new HashMap<String, List<String>>();
    for (var entry : dublinCoreCatalog.getValues().entrySet()){
      var key = entry.getKey().getLocalName();
      var values = entry.getValue().stream()
          .map(DublinCoreValue::getValue)
          .collect(Collectors.toList());
      metadata.put(key, values);
    }
    return metadata;

  }

  /**
   * Immediately adds the mediapackage to the search index.
   *
   * @param mediaPackage
   *          the media package
   * @throws SearchException
   *           if the media package cannot be added to the search index
   * @throws IllegalArgumentException
   *           if the media package is <code>null</code>
   * @throws UnauthorizedException
   *           if the user does not have the rights to add the mediapackage
   */
  public void addSynchronously(MediaPackage mediaPackage)
          throws SearchException, IllegalArgumentException, UnauthorizedException, SearchServiceDatabaseException {
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    var mediaPackageId = mediaPackage.getIdentifier().toString();
    logger.debug("Attempting to add media package {} to search index", mediaPackageId);
    var acl = authorizationService.getActiveAcl(mediaPackage).getA();
    var now = new Date();

    try {
      persistence.storeMediaPackage(mediaPackage, acl, now);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(
          String.format("Could not store media package to search database %s", mediaPackageId), e);
    }

    // Elasticsearch
    var metadata = DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage)
        .map(this::mapDublinCore)
        .orElse(Collections.emptyMap());
    metadata.put("modified", Collections.singletonList(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())));
    var mediaPackageJson = gson.fromJson(MediaPackageParser.getAsJSON(mediaPackage), Map.class).get("mediapackage");
    Map<String, Object> data = Map.of(
        "mediapackage", mediaPackageJson,
        "mediapackage_xml", MediaPackageParser.getAsXml(mediaPackage),
        "org", getSecurityService().getOrganization().getId(),
        "dc", metadata,
        "acl", searchableAcl(acl),
        "type", IndexEntryType.Episode.name()
    );
    try {
      var request = new IndexRequest(INDEX_NAME);
      request.id(mediaPackageId);
      request.source(data);
      elasticsearchIndex.getClient().index(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new SearchException(e);
    }

    // Elasticsearch series
    for (var seriesId: metadata.getOrDefault("isPartOf", Collections.emptyList())) {
      try {
        var series = mapDublinCore(seriesService.getSeries(seriesId));
        series.put("modified", Collections.singletonList(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())));
        var seriesAcl = persistence.getAccessControlLists(seriesId, mediaPackageId).stream()
            .reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
        Map<String, Object> seriesData = Map.of(
            "org", getSecurityService().getOrganization().getId(),
            "dc", series,
            "acl", searchableAcl(seriesAcl),
            "type", IndexEntryType.Series.name()
        );
        try {
          var request = new IndexRequest(INDEX_NAME);
          request.id(seriesId);
          request.source(seriesData);
          elasticsearchIndex.getClient().index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
          throw new SearchException(e);
        }
      } catch (NotFoundException | SeriesException e) {
        logger.warn("Could not get series {} from series service. Skipping its publication", seriesId);
      }
    }
  }

  @Override
  public Collection<Pair<Organization, MediaPackage>> getSeries(String seriesId) {
    var result = new ArrayList<Pair<Organization, MediaPackage>>();
    try {
      for (var entry: persistence.getSeries(seriesId)) {
        result.add(Pair.of(
            entry.getOrganization(),
            MediaPackageParser.getFromXml(entry.getMediaPackageXML())));
      }
    } catch (SearchServiceDatabaseException | MediaPackageException e) {
      throw new SearchException(e);
    }
    return result;
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

  /**
   * Immediately removes the given mediapackage from the search service.
   *
   * @param mediaPackageId
   *          the media package identifier
   * @return <code>true</code> if the mediapackage was deleted
   * @throws SearchException
   *           if deletion failed
   */
  public boolean deleteSynchronously(final String mediaPackageId) throws SearchException {

    // TODO: Permission checks
    // TODO: Maybe don't delete, but mark as deleted?
    try {
      var deleteRequest = new DeleteRequest(INDEX_NAME, mediaPackageId);
      elasticsearchIndex.getClient().delete(deleteRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new SearchException("Could not delete episode " + mediaPackageId + " from index", e);
    }

    try {
      logger.info("Removing media package {} from search index", mediaPackageId);

      String seriesId = null;
      Date now = new Date();
      try {
        seriesId = persistence.getMediaPackage(mediaPackageId).getSeries();
        persistence.deleteMediaPackage(mediaPackageId, now);
        logger.info("Removed media package {} from search persistence", mediaPackageId);
      } catch (NotFoundException e) {
        // even if mp not found in persistence, it might still exist in search index.
        logger.info("Could not find media package with id {} in persistence, but will try remove it from index anyway.",
                mediaPackageId);
      } catch (SearchServiceDatabaseException | UnauthorizedException e) {
        throw new SearchException(String.format("Could not delete media package with id %s from persistence storage",
            mediaPackageId), e);
      }

      // Update series
      if (seriesId != null) {
        if (persistence.getSeries(seriesId).size() > 0) {
          // Update series acl if there are still episodes in the series
          final AccessControlList seriesAcl = persistence.getAccessControlLists(seriesId).stream()
              .reduce(new AccessControlList(), AccessControlList::mergeActions);
          // TODO: Update series ACL in Elasticsearch
        } else {
          // Remove series if there are no episodes in the series any longer
          deleteSeriesSynchronously(seriesId);
        }
      }

      return true;
    } catch (SearchServiceDatabaseException e) {
      logger.info("Could not delete media package with id {} from search index", mediaPackageId);
      throw new SearchException(e);
    }
  }

  /**
   * Immediately removes the given series from the search service.
   *
   * @param seriesId
   *          the series
   * @throws SearchException
   */
  public void deleteSeriesSynchronously(String seriesId) throws SearchException {
    // TODO: Maybe don't delete, but mark as deleted?
    try {
      var deleteRequest = new DeleteRequest(INDEX_NAME, seriesId);
      elasticsearchIndex.getClient().delete(deleteRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new SearchException("Could not delete series " + seriesId + " from index", e);
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
      switch (op) {
        case Add:
          MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
          addSynchronously(mediaPackage);
          return null;
        case Delete:
          String mediapackageId = arguments.get(0);
          boolean deleted = deleteSynchronously(mediapackageId);
          return Boolean.toString(deleted);
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
  public void setPersistence(SearchServiceDatabase persistence) {
    this.persistence = persistence;
  }

  @Reference
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
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

  @Override
  public void updated(Dictionary properties) {
    // TODO: Move to modified()
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
