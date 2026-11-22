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

import static org.opencastproject.security.api.Permissions.Action.WRITE;
import static org.opencastproject.security.util.SecurityUtil.getEpisodeRoleId;

import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.opensearch.api.SearchIndexException;
import org.opencastproject.opensearch.impl.AbstractOpenSearchIndex;
import org.opencastproject.opensearch.index.OpenSearchIndex;
import org.opencastproject.opensearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.opensearch.index.rebuild.IndexProducer;
import org.opencastproject.opensearch.index.rebuild.IndexRebuildException;
import org.opencastproject.opensearch.index.rebuild.IndexRebuildService;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.api.SearchServiceQuery;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A OpenSearch-based {@link SearchService} implementation.
 */
@Component(
        immediate = true,
        service = { SearchServiceIndex.class, IndexProducer.class },
        property = {
                "service.description=Search Service Index",
                "service.pid=org.opencastproject.search.impl.SearchServiceIndex"
        }
)
public class SearchServiceIndex extends AbstractIndexProducer implements IndexProducer {

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Search;
  }

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceIndex.class);

  public static final String INDEX_NAME = "opencast_search";

  private static final String INDEX_ACL_READ = SearchResult.INDEX_ACL + ".read";
  private static final String DC_TITLE = SearchResult.DUBLINCORE + ".title";
  private static final String DC_IS_PART_OF = SearchResult.DUBLINCORE + ".isPartOf";

  /** Fields searched by free text queries */
  private static final List<String> TEXT_FIELDS = List.of(
      SearchResult.DUBLINCORE + ".*.search",
      SearchResult.DUBLINCORE + ".*.search._2gram",
      SearchResult.DUBLINCORE + ".*.search._3gram",
      SearchResult.DUBLINCORE + ".*.search._4gram");
  private static final float TEXT_BOOST = 1.2f;

  private OpenSearchIndex esIndex;

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

  private ListProvidersService listProvidersService;

  private String systemUserName = null;


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
    systemUserName = SecurityUtil.getSystemUserName(cc);
  }

  private void createIndex() {
    try {
      esIndex.createIndexIfMissing(INDEX_NAME, loadResource("/opensearch/indexSettings.json"),
          loadResource("/opensearch/search-mapping.json"));
    } catch (IOException | SearchIndexException e) {
      throw new SearchException("Could not create index " + INDEX_NAME, e);
    }
  }

  private String loadResource(String path) throws IOException {
    try (var in = getClass().getResourceAsStream(path)) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }

  /** Executes a request against OpenSearch, with retries as configured for the OpenSearch index. */
  private <T> T request(AbstractOpenSearchIndex.Request<T> request) throws IOException {
    try {
      return esIndex.withRetries(request);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting to retry a request", e);
    }
  }

  @Reference
  public void setEsIndex(OpenSearchIndex esIndex) {
    this.esIndex = esIndex;
  }

  @Override
  public void clear() throws IOException {
    try {
      if (!request(c -> c.indices().delete(d -> d.index(INDEX_NAME))).acknowledged()) {
        logger.error("Index '{}' could not be deleted", INDEX_NAME);
      }
    } catch (OpenSearchException e) {
      if (e.status() != 404) {
        throw e;
      }
      logger.info("Cannot clear non-existing index '{}'", INDEX_NAME);
    }
    createIndex();
  }

  /**
   * Documents found by a search and the total number of matching documents.
   *
   * @param documents
   *          the source documents of the hits on the requested page
   * @param total
   *          the total number of matching documents
   */
  public record Hits(List<Map<String, Object>> documents, long total) {
  }

  /**
   * Searches the index.
   *
   * @param query
   *          the query to run
   * @return the matching documents
   * @throws SearchException
   *          if querying the index fails
   */
  public Hits search(SearchServiceQuery query) throws SearchException {
    final SearchSourceBuilder searchSource = toSearchSource(query);
    logger.debug("Sending for query: {}", searchSource.query());
    try {
      final SearchResponse response = esIndex.getLegacyClient()
          .search(new SearchRequest(INDEX_NAME).source(searchSource), RequestOptions.DEFAULT);
      final List<Map<String, Object>> documents = Arrays.stream(response.getHits().getHits())
          .map(SearchHit::getSourceAsMap)
          .collect(Collectors.toList());
      return new Hits(documents, response.getHits().getTotalHits().value());
    } catch (IOException e) {
      throw new SearchException(e);
    }
  }

  /**
   * Renders the body of the search request for a query as JSON. Only meant for testing.
   */
  static String searchRequestBody(SearchServiceQuery query) {
    return toSearchSource(query).toString();
  }

  private static SearchSourceBuilder toSearchSource(SearchServiceQuery query) {
    final BoolQueryBuilder bool = QueryBuilders.boolQuery()
        .filter(QueryBuilders.termQuery(SearchResult.ORG, query.getOrganization()))
        .filter(QueryBuilders.termQuery(SearchResult.TYPE, query.getType().name()));
    if (!query.isIncludeDeleted()) {
      bool.filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(SearchResult.DELETED_DATE)));
    }
    query.getReadRoles().ifPresent(roles -> bool.filter(QueryBuilders.termsQuery(INDEX_ACL_READ, roles)));
    query.getModifiedSince().ifPresent(since -> bool.filter(
        QueryBuilders.rangeQuery(SearchResult.MODIFIED_DATE).gte(since.getTime())));
    query.getId().ifPresent(id -> bool.must(QueryBuilders.idsQuery().addIds(id)));
    query.getSeriesTitle().ifPresent(title -> bool.must(QueryBuilders.termQuery(DC_TITLE, title)));

    final List<String> seriesIds = query.getSeriesIds();
    if (seriesIds.size() == 1) {
      bool.must(QueryBuilders.termQuery(DC_IS_PART_OF, seriesIds.get(0)));
    } else if (seriesIds.size() > 1) {
      final BoolQueryBuilder seriesQuery = QueryBuilders.boolQuery();
      seriesIds.forEach(id -> seriesQuery.should(QueryBuilders.termQuery(DC_IS_PART_OF, id)));
      bool.must(seriesQuery);
    }

    query.getLive().ifPresent(live -> bool.must(QueryBuilders.termQuery(SearchResult.LIVE, live)));

    query.getText().ifPresent(text -> {
      final MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(text)
          .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX)
          .operator(Operator.AND)
          .fuzziness(Fuzziness.AUTO)
          .fuzzyTranspositions(true);
      // Search-as-you-type generates useful subfields for autocompletion, we should use it
      // https://docs.opensearch.org/latest/field-types/supported-field-types/search-as-you-type/
      TEXT_FIELDS.forEach(field -> multiMatch.field(field, TEXT_BOOST));
      bool.minimumShouldMatch(1);
      bool.should(multiMatch);
    });

    final SearchSourceBuilder searchSource = new SearchSourceBuilder().query(bool);
    if (query.getOffset() >= 0) {
      searchSource.from(query.getOffset());
    }
    if (query.getLimit() >= 0) {
      searchSource.size(query.getLimit());
    }
    query.getSortOrders().forEach((field, order) -> searchSource.sort(field,
        order == SortCriterion.Order.Descending ? SortOrder.DESC : SortOrder.ASC));
    searchSource.sort("_score", SortOrder.DESC);
    // Used to stabilize the sort order of results with the same score.
    searchSource.sort("_doc", SortOrder.ASC);
    return searchSource;
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

    checkSearchEntityWritePermission(mediaPackageId);

    logger.debug("Attempting to add media package {} to search index", mediaPackageId);
    final var acls = new AccessControlList[1];
    final var org = securityService.getOrganization();
    final var systemUser = SecurityUtil.createSystemUser(systemUserName, org);
    // Ensure we always get the actual acl by forcing access
    SecurityUtil.runAs(securityService, org, systemUser, () -> {
      acls[0] = authorizationService.getActiveAcl(mediaPackage).getA();
    });
    var acl = acls[0] == null ? new AccessControlList() : acls[0];
    var now = new Date();

    try {
      persistence.storeMediaPackage(mediaPackage, acl, now);
    } catch (SearchServiceDatabaseException e) {
      throw new SearchException(String.format("Could not store media package to search database %s", mediaPackageId),
          e);
    }

    indexMediaPackage(mediaPackage, acl);
  }

  public void indexMediaPackage(String mediaPackageId)
          throws SearchException, SearchServiceDatabaseException, UnauthorizedException, NotFoundException {
    if (!securityService.getUser().hasRole("ROLE_ADMIN")) {
      throw new UnauthorizedException("Only global administrators may trigger manual event updates.");
    }
    try {
      MediaPackage mp = persistence.getMediaPackage(mediaPackageId);
      AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
      Date modificationDate = persistence.getModificationDate(mediaPackageId);
      Date deletionDate = persistence.getDeletionDate(mediaPackageId);
      indexMediaPackage(mp, acl, modificationDate, deletionDate);
    } catch (RuntimeException e) {
      logSkippingElement(logger, "event", mediaPackageId, e);
    }
  }

  private void indexMediaPackage(MediaPackage mediaPackage, AccessControlList acl)
          throws SearchException, SearchServiceDatabaseException {
    indexMediaPackage(mediaPackage, acl, null, null);
  }

  private void indexMediaPackage(MediaPackage mediaPackage, AccessControlList acl, Date modDate, Date delDate)
          throws SearchException, SearchServiceDatabaseException {
    String mediaPackageId = mediaPackage.getIdentifier().toString();
    String orgId = securityService.getOrganization().getId();

    //If the entry has been deleted then there's *probably* no dc file to load.
    DublinCoreCatalog dc = null == delDate
        ? DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage).orElse(DublinCores.mkSimple())
        : DublinCores.mkSimple();

    List<DublinCoreCatalog> seriesList = Collections.emptyList();
    if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF)) {
      //Find the series (if any), filter for those which exist to prevent linking non-existent series
      seriesList = dc.get(DublinCore.PROPERTY_IS_PART_OF).stream().map(DublinCoreValue::getValue).map(s -> {
        try {
          return seriesService.getSeries(s);
        } catch (NotFoundException e) {
          logger.warn("Series {} not found during index of event {}, omitting the link from the indexed data", s,
              mediaPackageId);
        } catch (UnauthorizedException e) {
          logger.warn("Not authorized for series {} during index of event {}, omitting the link from the indexed data",
              s, mediaPackageId);
        } catch (SeriesException e) {
          throw new SearchException(e);
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // Add custom roles if enabled
    acl = addCustomAclRoles(mediaPackageId, acl);

    SearchResult item = new SearchResult(SearchService.IndexEntryType.Episode, dc, acl, orgId, mediaPackage,
        null != modDate ? modDate.toInstant() : Instant.now(),
        null != delDate ? delDate.toInstant() : null);
    Map<String, Object> metadata = item.dehydrateForIndex();
    try {
      request(c -> c.index(i -> i.index(INDEX_NAME).id(mediaPackageId).document(metadata)));
      logger.debug("Indexed episode {}", mediaPackageId);
    } catch (IOException e) {
      throw new SearchException(e);
    }

    // OpenSearch series
    for (DublinCoreCatalog seriesDc : seriesList) {
      String seriesId = seriesDc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
      AccessControlList seriesAcl = persistence.getAccessControlLists(seriesId, mediaPackageId).stream()
          .map(aclPair -> addCustomAclRoles(aclPair.getKey(), aclPair.getValue()))
          .reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
      item = new SearchResult(SearchService.IndexEntryType.Series, seriesDc, seriesAcl, orgId,
          null, Instant.now(), null);

      Map<String, Object> seriesData = item.dehydrateForIndex();
      try {
        request(c -> c.index(i -> i.index(INDEX_NAME).id(seriesId).document(seriesData)));
        logger.debug("Indexed series {} related to episode {}", seriesId, mediaPackageId);
      } catch (IOException e) {
        throw new SearchException(e);
      }
    }
  }

  /**
   * Add custom roles of the media package to the passed ACL
   *
   * @param mediaPackageId
   *          the media package
   * @param acl
   *          the existing access control list
   * @return {@link AccessControlList} containing the passed and the custom roles merged together
   *
   */
  private AccessControlList addCustomAclRoles(String mediaPackageId, AccessControlList acl) {
    // This allows users with a role of the form ROLE_EPISODE_<ID>_<ACTION> to access the event through the index
    Set<AccessControlEntry> customEntries = new HashSet<>();
    customEntries.add(new AccessControlEntry(getEpisodeRoleId(mediaPackageId, "READ"), "read", true));
    customEntries.add(new AccessControlEntry(getEpisodeRoleId(mediaPackageId, "WRITE"), "write", true));

    ResourceListQuery query = new DefaultResourceListQuery();
    if (listProvidersService.hasProvider("ACL.ACTIONS")) {
      Map<String, String> actions = new HashMap<>();
      try {
        actions = listProvidersService.getList("ACL.ACTIONS", query, true);
      } catch (ListProviderException e) {
        throw new SearchException("Listproviders not loaded. " + e);
      }
      for (String action : actions.keySet()) {
        customEntries.add(
            new AccessControlEntry(getEpisodeRoleId(mediaPackageId, action), action, true));
      }
    }

    AccessControlList customRoles = new AccessControlList(new ArrayList<>(customEntries));
    acl = customRoles.merge(acl);

    return acl;
  }

  private void checkSearchEntityWritePermission(final String mediaPackageId) throws SearchException {
    User user = securityService.getUser();
    try {
      if (!persistence.isAvailable(mediaPackageId)) {
        throw new NotFoundException();
      }
      AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
      if (!AccessControlUtil.isAuthorized(acl, user, securityService.getOrganization(), WRITE.toString(),
          mediaPackageId)) {
        throw new UnauthorizedException(user, "Write permission denied for " + mediaPackageId, acl);
      }
    } catch (NotFoundException e) {
      logger.debug("Mediapackage {} does not exist or was deleted, allowing writes for user {}", mediaPackageId, user);
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

    checkSearchEntityWritePermission(mediaPackageId);

    String deletionString = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    try {
      logger.info("Marking media package {} as deleted in search index", mediaPackageId);
      final Map<String, Object> update = Map.of(
          SearchResult.DELETED_DATE, deletionString,
          SearchResult.MODIFIED_DATE, deletionString);
      request(c -> c.update(u -> u.index(INDEX_NAME).id(mediaPackageId).doc(update), Map.class));
    } catch (OpenSearchException e) {
      if (e.status() != 404) {
        throw e;
      }
      logger.warn("Event {} is not in the search index. Skipping deletion", mediaPackageId);
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
                .map(aclPair -> addCustomAclRoles(aclPair.getKey(), aclPair.getValue()))
                .reduce(new AccessControlList(), AccessControlList::mergeActions);
            final Map<String, Object> update = Map.of(
                SearchResult.INDEX_ACL, SearchResult.dehydrateAclForIndex(seriesAcl),
                SearchResult.MODIFIED_DATE, deletionString);
            final String id = seriesId;
            try {
              request(c -> c.update(u -> u.index(INDEX_NAME).id(id).doc(update), Map.class));
            } catch (OpenSearchException e) {
              if (e.status() == 404) {
                logger.warn("Attempted to modify {}, but that series does not exist in the index.", seriesId);
              }
            }
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
      final Map<String, Object> update = Map.of(
          "deleted", Instant.now().getEpochSecond(),
          "modified", Instant.now().toString());
      try {
        final var response = request(c -> c.update(u -> u.index(INDEX_NAME).id(seriesId).doc(update), Map.class));
        //NB: We're marking things as deleted but *not actually deleting them**
        return Result.Updated == response.result();
      } catch (OpenSearchException e) {
        if (e.status() == 404) {
          logger.debug("Attempted to delete {}, but that series does not exist in the index.", seriesId);
          return true;
        }
        throw new SearchException(e);
      }
    } catch (IOException e) {
      throw new SearchException("Could not delete series " + seriesId + " from index", e);
    }
  }

  @Override
  public void repopulate(IndexRebuildService.DataType type) throws IndexRebuildException {
    final Organization originalOrg = securityService.getOrganization();
    final User originalUser = securityService.getUser();

    try {
      int total = persistence.countMediaPackages();
      int pageSize = 50;
      int pageOffset = 0;
      AtomicInteger current = new AtomicInteger(1);
      logIndexRebuildBegin(logger, total, "search");
      List<Tuple<MediaPackage, String>> page = null;

      do {
        page = persistence.getAllMediaPackages(pageSize, pageOffset).collect(Collectors.toList());
        page.forEach(tuple -> {
          try {
            MediaPackage mediaPackage = tuple.getA();
            Organization organization = organizationDirectory.getOrganization(tuple.getB());
            final var systemUser = SecurityUtil.createSystemUser(systemUserName, organization);
            securityService.setUser(systemUser);
            securityService.setOrganization(organization);

            String mediaPackageId = mediaPackage.getIdentifier().toString();

            AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
            Date modificationDate = persistence.getModificationDate(mediaPackageId);
            Date deletionDate = persistence.getDeletionDate(mediaPackageId);

            current.getAndIncrement();

            indexMediaPackage(mediaPackage, acl, modificationDate, deletionDate);
          } catch (SearchServiceDatabaseException e) {
            logIndexRebuildError(logger, total, current.get(), e);
            //NB: Runtime exception thrown to escape the functional interfacing
            throw new RuntimeException("Internal Index Rebuild Failure", e);
          } catch (RuntimeException | NotFoundException e) {
            logSkippingElement(logger, "event", tuple.getA().getIdentifier().toString(), e);
          }
        });
        //Current is the *page* index, so we remove one since each page only has pageSize entries
        logIndexRebuildProgress(logger, total, current.get() - 1, pageSize);
        pageOffset += 1;
      } while (pageOffset * pageSize <= total);
      //NB: Catching RuntimeException since it can be thrown inside the functional forEach here
    } catch (SearchServiceDatabaseException | RuntimeException e) {
      logIndexRebuildError(logger, e);
      throw new IndexRebuildException("Index Rebuild Failure", e);
    } finally {
      securityService.setUser(originalUser);
      securityService.setOrganization(originalOrg);
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

  @Reference
  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }
}
