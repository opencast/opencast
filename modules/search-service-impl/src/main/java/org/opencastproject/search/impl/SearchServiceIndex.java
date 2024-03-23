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

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A Elasticsearch-based {@link SearchService} implementation.
 */
@Component(
        immediate = true,
        service = { SearchServiceIndex.class, IndexProducer.class },
        property = {
                "service.description=Search Service Index",
                "service.pid=org.opencastproject.search.impl.SearchServiceIndex"
        }
)
public final class SearchServiceIndex extends AbstractIndexProducer implements IndexProducer {

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Search;
  }

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceIndex.class);

  public static final String INDEX_NAME = "opencast_search";

  private final Gson gson = new Gson();

  private ElasticsearchIndex esIndex;

  private SeriesService seriesService;

  /** The local workspace */
  private Workspace workspace;

  /** The security service */
  private SecurityService securityService;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** Persistent storage */
  private SearchServiceDatabase persistence;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectory = null;

  /**
   * Creates a new instance of the search service index.
   */
  public SearchServiceIndex() {
  }

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  @Activate
  public void activate(final ComponentContext cc) throws IllegalStateException {
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
      var request = new CreateIndexRequest(INDEX_NAME).mapping(mapping, XContentType.JSON);
      var response = esIndex.getClient().indices().create(request, RequestOptions.DEFAULT);
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
  public void setEsIndex(ElasticsearchIndex esIndex) {
    this.esIndex = esIndex;
  }


  public SearchResponse search(SearchSourceBuilder searchSource) throws SearchException {
    SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
    logger.debug("Sending for query: {}", searchSource.query());
    searchRequest.source(searchSource);
    try {
      return esIndex.getClient().search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new SearchException(e);
    }
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

    checkMPWritePermission(mediaPackageId);

    logger.debug("Attempting to add media package {} to search index", mediaPackageId);
    var acl = authorizationService.getActiveAcl(mediaPackage).getA();
    var now = new Date();

    try {
      persistence.storeMediaPackage(mediaPackage, acl, now);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(String.format("Could not store media package to search database %s", mediaPackageId),
          e);
    }

    indexMediaPackage(mediaPackage, acl);
  }

  private void indexMediaPackage(MediaPackage mediaPackage, AccessControlList acl)
          throws SearchException, UnauthorizedException, SearchServiceDatabaseException {
    indexMediaPackage(mediaPackage, acl, null, null);
  }

  private void indexMediaPackage(MediaPackage mediaPackage, AccessControlList acl, Date modDate, Date delDate)
          throws SearchException, UnauthorizedException, SearchServiceDatabaseException {
    String mediaPackageId = mediaPackage.getIdentifier().toString();
    //If the entry has been deleted then there's *probably* no dc file to load.
    DublinCoreCatalog dc = null == delDate
        ? DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage).orElse(DublinCores.mkSimple())
        : DublinCores.mkSimple();
    String orgId = securityService.getOrganization().getId();
    SearchResult item = new SearchResult(SearchService.IndexEntryType.Episode, dc, acl, orgId, mediaPackage,
        null != modDate ? modDate.toInstant() : Instant.now(),
        null != delDate ? delDate.toInstant() : null);
    Map<String, Object> metadata = item.dehydrateForIndex();
    try {
      var request = new IndexRequest(INDEX_NAME);
      request.id(mediaPackageId);
      request.source(metadata);
      esIndex.getClient().index(request, RequestOptions.DEFAULT);
      logger.debug("Indexed episode {}", mediaPackageId);
    } catch (IOException e) {
      throw new SearchException(e);
    }

    // Elasticsearch series
    for (DublinCoreValue seriesId : item.getDublinCore().get(DublinCoreCatalog.PROPERTY_IS_PART_OF)) {
      try {
        DublinCoreCatalog seriesDc = seriesService.getSeries(seriesId.getValue());
        AccessControlList seriesAcl = persistence.getAccessControlLists(seriesId.getValue(), mediaPackageId).stream()
            .reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
        item = new SearchResult(SearchService.IndexEntryType.Series, seriesDc, seriesAcl, orgId,
            null, Instant.now(), null);

        Map<String, Object> seriesData = item.dehydrateForIndex();
        try {
          var request = new IndexRequest(INDEX_NAME);
          request.id(seriesId.getValue());
          request.source(seriesData);
          esIndex.getClient().index(request, RequestOptions.DEFAULT);
          logger.debug("Indexed series {} related to episode {}", seriesId.getValue(), mediaPackageId);
        } catch (IOException e) {
          throw new SearchException(e);
        }
      } catch (NotFoundException | SeriesException e) {
        logger.warn("Could not get series {} from series service. Skipping its publication", seriesId);
      }
    }
  }

  private void checkMPWritePermission(final String mediaPackageId) throws SearchException {
    try {
      MediaPackage mp = persistence.getMediaPackage(mediaPackageId);
      if (!authorizationService.hasPermission(mp, Permissions.Action.WRITE.toString())) {
        boolean isAdmin = securityService.getUser().getRoles().stream()
            .map(Role::getName)
            .anyMatch(r -> r.equals(SecurityConstants.GLOBAL_ADMIN_ROLE));
        if (!isAdmin) {
          throw new UnauthorizedException(securityService.getUser(), "Write permission denied for " + mediaPackageId,
              authorizationService.getActiveAcl(mp).getA());
        } else {
          logger.debug("Write for {} is not allowed by ACL, but user has {}",
              mediaPackageId, SecurityConstants.GLOBAL_ADMIN_ROLE);
        }
      }
    } catch (NotFoundException e) {
      logger.debug("Mediapackage {} not found, allowing writes", mediaPackageId);
    } catch (SearchServiceDatabaseException | UnauthorizedException e) {
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

    checkMPWritePermission(mediaPackageId);

    String deletionString = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    try {
      logger.info("Marking media package {} as deleted in search index", mediaPackageId);
      JsonElement json = gson.toJsonTree(Map.of(
          SearchResult.DELETED_DATE, deletionString,
          SearchResult.MODIFIED_DATE, deletionString));
      var updateRequst = new UpdateRequest(INDEX_NAME, mediaPackageId)
          .doc(gson.toJson(json), XContentType.JSON);
      esIndex.getClient().update(updateRequst, RequestOptions.DEFAULT);

    } catch (IOException e) {
      throw new SearchException("Could not delete episode " + mediaPackageId + " from index", e);
    }

    try {
      logger.info("Marking media package {} as deleted in search database", mediaPackageId);

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
        throw new SearchException(
            String.format("Could not delete media package with id %s from persistence storage", mediaPackageId), e);
      }

      // Update series
      if (seriesId != null) {
        try {
          if (!persistence.getSeries(seriesId).isEmpty()) {
            // Update series acl if there are still episodes in the series
            final AccessControlList seriesAcl = persistence.getAccessControlLists(seriesId).stream()
                .reduce(new AccessControlList(), AccessControlList::mergeActions);
            JsonElement json = gson.toJsonTree(Map.of(
                SearchResult.INDEX_ACL, SearchResult.dehydrateAclForIndex(seriesAcl),
                SearchResult.MODIFIED_DATE, deletionString));
            var updateRequest = new UpdateRequest(INDEX_NAME, seriesId).doc(gson.toJson(json), XContentType.JSON);
            esIndex.getClient().update(updateRequest, RequestOptions.DEFAULT);
          } else {
            // Remove series if there are no episodes in the series any longer
            deleteSeriesSynchronously(seriesId);
          }
        } catch (IOException e) {
          throw new SearchException(e);
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
  public boolean deleteSeriesSynchronously(String seriesId) throws SearchException {
    try {
      logger.info("Marking {} as deleted in the search index", seriesId);
      JsonElement json = gson.toJsonTree(Map.of(
          "deleted", Instant.now().getEpochSecond(),
          "modified", Instant.now().toString()));
      var updateRequest = new UpdateRequest(INDEX_NAME, seriesId).doc(gson.toJson(json), XContentType.JSON);
      UpdateResponse response = esIndex.getClient().update(updateRequest, RequestOptions.DEFAULT);
      //NB: We're marking things as deleted but *not actually deleting them**
      return DocWriteResponse.Result.UPDATED == response.getResult();
    } catch (IOException e) {
      throw new SearchException("Could not delete series " + seriesId + " from index", e);
    }
  }

  @Override
  public void repopulate() throws IndexRebuildException {
    try {
      int total = persistence.countMediaPackages();
      int pageSize = 50;
      int offset = 0;
      AtomicInteger current = new AtomicInteger(0);
      logIndexRebuildBegin(logger, esIndex.getIndexName(), total, "search");
      List<Tuple<MediaPackage, String>> page = null;

      do {
        page = persistence.getAllMediaPackages(pageSize, offset).collect(Collectors.toList());
        page.forEach(tuple -> {
          try {
            MediaPackage mediaPackage = tuple.getA();
            String mediaPackageId = mediaPackage.getIdentifier().toString();

            AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
            Date modificationDate = persistence.getModificationDate(mediaPackageId);
            Date deletionDate = persistence.getDeletionDate(mediaPackageId);

            AccessControlList seriesAcl = persistence.getAccessControlLists(mediaPackage.getSeries(), mediaPackageId)
                .stream().reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
            logger.debug("Updating series ACL with merged access control list: {}", seriesAcl);

            logIndexRebuildProgress(logger, esIndex.getIndexName(), total, current.getAndIncrement(), pageSize);
            indexMediaPackage(mediaPackage, acl, modificationDate, deletionDate);
          } catch (SearchServiceDatabaseException | UnauthorizedException | NotFoundException e) {
            logIndexRebuildError(logger, "search", total, current.get(), e);
            //NB: Runtime exception thrown to escape the functional interfacing
            throw new RuntimeException("Internal Index Rebuild Failure", e);
          }
        });
        offset += pageSize;
      } while (page.size() == pageSize);
      //NB: Catching RuntimeException since it can be thrown inside the functional forEach here
    } catch (SearchServiceDatabaseException | RuntimeException e) {
      logIndexRebuildError(logger, "search", e);
      throw new IndexRebuildException("Index Rebuild Failure", e);
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
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectory = organizationDirectory;
  }
}
