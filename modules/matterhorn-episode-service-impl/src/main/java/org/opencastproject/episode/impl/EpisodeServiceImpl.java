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
package org.opencastproject.episode.impl;

import static java.lang.String.format;
import static org.opencastproject.episode.api.EpisodeQuery.query;
import static org.opencastproject.episode.impl.PartialMediaPackage.partialMp;
import static org.opencastproject.episode.impl.Protected.granted;
import static org.opencastproject.episode.impl.Protected.rejected;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.episode.impl.Util.filterItems;
import static org.opencastproject.episode.impl.elementstore.DeletionSelector.delAll;
import static org.opencastproject.episode.impl.elementstore.Source.source;
import static org.opencastproject.mediapackage.MediaPackageSupport.copy;
import static org.opencastproject.mediapackage.MediaPackageSupport.getMediaPackageElementId;
import static org.opencastproject.mediapackage.MediaPackageSupport.updateElement;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.hasNoChecksum;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.JobUtil.waitForJob;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Functions.constant0;
import static org.opencastproject.util.data.functions.Functions.toPredicate;

import org.opencastproject.episode.api.ArchivedMediaPackageElement;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.UriRewriter;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.elementstore.ElementStore;
import org.opencastproject.episode.impl.persistence.Asset;
import org.opencastproject.episode.impl.persistence.Episode;
import org.opencastproject.episode.impl.persistence.EpisodeServiceDatabase;
import org.opencastproject.episode.impl.persistence.EpisodeServiceDatabaseException;
import org.opencastproject.episode.impl.solr.SolrIndexManager;
import org.opencastproject.episode.impl.solr.SolrRequester;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;
import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EpisodeServiceImpl implements EpisodeService {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(EpisodeServiceImpl.class);

  private final SolrRequester solrRequester;
  private final SolrIndexManager solrIndex;
  private final SecurityService secSvc;
  private final AuthorizationService authSvc;
  private final OrganizationDirectoryService orgDir;
  private final ServiceRegistry svcReg;
  private final WorkflowService workflowSvc;
  private final Workspace workspace;
  private final EpisodeServiceDatabase persistence;
  private final ElementStore elementStore;
  private final MediaInspectionService mediaInspectionSvc;
  private final String systemUserName;

  public EpisodeServiceImpl(SolrRequester solrRequester, SolrIndexManager solrIndex, SecurityService secSvc,
          AuthorizationService authSvc, OrganizationDirectoryService orgDir, ServiceRegistry svcReg,
          WorkflowService workflowSvc, Workspace workspace, MediaInspectionService mediaInspectionSvc,
          EpisodeServiceDatabase persistence, ElementStore elementStore, String systemUserName) {
    this.solrRequester = solrRequester;
    this.solrIndex = solrIndex;
    this.secSvc = secSvc;
    this.authSvc = authSvc;
    this.orgDir = orgDir;
    this.svcReg = svcReg;
    this.workflowSvc = workflowSvc;
    this.workspace = workspace;
    this.persistence = persistence;
    this.elementStore = elementStore;
    this.mediaInspectionSvc = mediaInspectionSvc;
    this.systemUserName = systemUserName;
  }

  @Override
  /*
   * todo adding to the archive needs to be synchronized because of the index. It resets the oc_latest_version flag and
   * this must not happen concurrently. This approach works as long as the archive is not distributed.
   */
  public synchronized void add(final MediaPackage mp) throws EpisodeServiceException {
    handleException(new Effect0.X() {
      @Override
      protected void xrun() throws Exception {
        logger.debug("Attempting to add mediapackage {} to archive", mp.getIdentifier());
        final AccessControlList acl = authSvc.getActiveAcl(mp).getA();
        protect(acl, list(WRITE_PERMISSION), new Effect0.X() {
          @Override
          public void xrun() throws Exception {
            addInternal(copy(mp), acl);
          }
        });
      }
    });
  }

  // todo make archiving transactional
  /** Mutates mp and its elements, so make sure to work on a copy. */
  private void addInternal(final MediaPackage mp, final AccessControlList acl) throws Exception {
    final Date now = new Date();
    // claim a new version for the mediapackage
    final String mpId = mp.getIdentifier().toString();
    final Version version = persistence.claimVersion(mpId);
    logger.info("Creating new version {} of mediapackage {}", version, mp);
    final PartialMediaPackage pmp = mkPartial(mp);
    // make sure they have a checksum
    enrichAssetChecksums(pmp);
    // download and archive elements
    storeAssets(pmp, version);
    // store mediapackage in index
    /*
     * todo: Url-rewritten mediapackages cannot be stored in solr since the solr index accesses metadata catalogs via
     * StaticMetadataService which in turn uses the workspace to download them. If the URL is already a URN this does
     * not work.
     */
    solrIndex.add(mp, acl, now, version);
    // store mediapackage in db
    try {
      rewriteAssetsForArchival(pmp, version);
      persistence.storeEpisode(pmp, acl, now, version);
    } catch (EpisodeServiceDatabaseException e) {
      logger.error("Could not store episode {}: {}", mpId, e);
      throw new EpisodeServiceException(e);
    }
    // save manifest to element store
    // this is done at the end after the media package element ids have been rewritten to neutral URNs
    storeManifest(pmp, version);
  }

  // todo if the user is allowed to delete the whole set of versions is checked against the
  // acl of the latest version. That's probably not the best approach.
  @Override
  public boolean delete(final String mediaPackageId) throws EpisodeServiceException {
    return handleException(new Function0.X<Boolean>() {
      @Override
      public Boolean xapply() throws Exception {
        for (Protected<Episode> p : persistence.getLatestEpisode(mediaPackageId).map(protectEpisode(WRITE_PERMISSION))) {
          if (p.isGranted()) {
            logger.info("Removing mediapackage {} from the archive", mediaPackageId);
            final Date now = new Date();
            return elementStore.delete(delAll(getOrgId(), mediaPackageId))
                    && persistence.deleteEpisode(mediaPackageId, now) && solrIndex.delete(mediaPackageId, now);
          } else {
            // rejected
            throw new EpisodeServiceException(p.getRejected());
          }
        }
        // mediapackage not found
        return false;
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final EpisodeQuery q) throws EpisodeServiceException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        // todo only the latest version is used, this should change when the UI supports versions
        q.onlyLastVersion();
        return mlist(solrRequester.find(q).getItems()).bind(filterUnprotectedItemsForWrite)
                .bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final List<String> mediaPackageIds) throws EpisodeServiceException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        return mlist(mediaPackageIds).bind(queryLatestMediaPackageById).bind(filterUnprotectedItemsForWrite)
                .bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  // todo Maybe the resulting list should just be filtered by means of the solr query.
  // The current approach has low performance since all result items are fetched from the index and filtered
  // afterwards. This is due to moving security checks from the sub-components towards the EpisodeService.
  @Override
  public SearchResult find(final EpisodeQuery q, final UriRewriter rewriter) throws EpisodeServiceException {
    return handleException(new Function0.X<SearchResult>() {
      @Override
      public SearchResult xapply() throws Exception {
        final SearchResult r = filterItems(solrRequester.find(q), filterUnprotectedItemsForRead);
        for (SearchResultItem item : r.getItems()) {
          // rewrite URIs in place
          rewriteAssetsForDelivery(rewriter, item);
        }
        return r;
      }
    });
  }

  @Override
  public Option<ArchivedMediaPackageElement> get(final String mpId, final String mpElemId, final Version version)
          throws EpisodeServiceException {
    return handleException(new Function0.X<Option<ArchivedMediaPackageElement>>() {
      @Override
      public Option<ArchivedMediaPackageElement> xapply() throws Exception {
        for (final Protected<Episode> p : persistence.getEpisode(mpId, version).map(protectEpisode(READ_PERMISSION))) {
          if (p.isGranted()) {
            final MediaPackage mp = p.getGranted().getMediaPackage();
            for (MediaPackageElement mpe : option(mp.getElementById(mpElemId))) {
              for (InputStream stream : elementStore.get(spath(getOrgId(), mpId, version, mpElemId))) {
                return some(new ArchivedMediaPackageElement(stream, mpe.getMimeType(), mpe.getSize()));
              }
            }
            // mediapackage element does not exist
            return none();
          } else {
            // episode is protected
            throw new EpisodeServiceException(p.getRejected());
          }
        }
        // episode cannot be found
        return none();
      }
    });
  }

  @Override
  public SearchResult findForAdministrativeRead(final EpisodeQuery q, final UriRewriter rewriter)
          throws EpisodeServiceException {
    return handleException(new Function0.X<SearchResult>() {
      @Override
      public SearchResult xapply() throws Exception {
        User user = secSvc.getUser();
        if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole()))
          throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

        final SearchResult r = solrRequester.find(q);
        for (SearchResultItem item : r.getItems()) {
          // rewrite URIs in place
          rewriteAssetsForDelivery(rewriter, item);
        }
        return r;
      }
    });
  }

  //

  /**
   * Since the index is populated from persistent storage where media package element URIs have been transformed to
   * location independend URNs these have to be rewritten to point to actual locations.
   * 
   * @param uriRewriter
   *          facility used to point mediapackage element URIs to the episode service
   * @param waitForRestService
   *          <code>true</code> to add 10s wait in order to make sure the episode REST service is online and working
   */
  void populateIndex(final UriRewriter uriRewriter, final boolean waitForRestService) {

    long instancesInSolr;
    try {
      instancesInSolr = solrIndex.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr > 0L) {
      logger.debug("No need to populate episode search index");
      return;
    }

    logger.info("Start populating episode search index");

    new Thread(new Runnable() {

      @Override
      public void run() {
        // The episode service REST methods are being published by the REST publisher service, and currently there is
        // no way to figure out when that registration was done, hence the stupid "sleep 5000".

        if (waitForRestService) {
          try {
            logger.debug("Waiting 10s for episode REST service to be available");
            Thread.sleep(10000);
          } catch (InterruptedException e1) {
            logger.warn("Error waiting 5s");
          }
        }
        populateIndex(uriRewriter);
      }

    }).start();

  }

  /**
   * Since the index is populated from persistent storage where media package element URIs have been transformed to
   * location independend URNs these have to be rewritten to point to actual locations.
   * 
   * @param uriRewriter
   *          facility used to point mediapackage element URIs to the episode service
   * @param waitForRestService
   *          <code>true</code> to add 10s wait in order to make sure the episode REST service is online and working
   */
  void populateTestIndex(final UriRewriter uriRewriter, final boolean waitForRestService) {

    long instancesInSolr;
    try {
      instancesInSolr = solrIndex.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr > 0L) {
      logger.debug("No need to populate episode search index");
      return;
    }

    new Thread(new Runnable() {

      @Override
      public void run() {
        // The episode service REST methods are being published by the REST publisher service, and currently there is
        // no way to figure out when that registration was done, hence the stupid "sleep 5000".

        if (waitForRestService) {
          try {
            logger.debug("Waiting 10s for episode REST service to be available");
            Thread.sleep(10000);
          } catch (InterruptedException e1) {
            logger.warn("Error waiting 5s");
          }
        }

        // Finally get the work done
        populateIndex(uriRewriter);
      }

    }).start();
  }

  /**
   * Since the index is populated from persistent storage where media package element URIs have been transformed to
   * location independend URNs these have to be rewritten to point to actual locations.
   * 
   * @param uriRewriter
   *          facility used to point mediapackage element URIs to the episode service
   */
  void populateIndex(final UriRewriter uriRewriter) {
    logger.info("Start populating episode search index");

    Map<String, Version> maps = new HashMap<String, Version>();
    Iterator<Episode> episodes;
    try {
      episodes = persistence.getAllEpisodes();
    } catch (EpisodeServiceDatabaseException e) {
      logger.error("Unable to load the archive entries: {}", e);
      throw new ServiceException(e.getMessage());
    }
    int errors = 0;
    while (episodes.hasNext()) {
      try {
        final Episode episode = episodes.next();
        String episodeId = episode.getMediaPackage().getIdentifier().toString();

        Version latestVersion = maps.get(episodeId);
        if (latestVersion == null) {
          Option<Episode> latestEpisode = persistence.getLatestEpisode(episodeId);
          if (latestEpisode.isNone())
            throw new EpisodeServiceException("Latest episode from existing episode identifier not found!");
          latestVersion = latestEpisode.get().getVersion();
          maps.put(episodeId, latestVersion);
        }

        boolean isLatestVersion = episode.getVersion().equals(latestVersion);

        final Organization organization = orgDir.getOrganization(episode.getOrganization());
        secSvc.setOrganization(organization);
        secSvc.setUser(SecurityUtil.createSystemUser(systemUserName, organization));
        // mediapackage URIs need to be rewritten to concrete URLs for indexation to work
        final PartialMediaPackage pmp = mkPartial(episode.getMediaPackage());
        rewriteAssetUris(uriRewriter.curry(episode.getVersion()), pmp);
        solrIndex.add(pmp.getMediaPackage(), episode.getAcl(), episode.getVersion(), episode.getDeletionDate(),
                episode.getModificationDate(), isLatestVersion);
      } catch (Exception e) {
        logger.error("Unable to index series instances: {}", e.getMessage());
        errors++;
      } finally {
        secSvc.setOrganization(null);
        secSvc.setUser(null);
      }
    }
    if (errors > 0)
      logger.error("Skipped {} erroneous archive entries while populating the episode search index", errors);
    logger.info("Finished populating episode search index");
  }

  /**
   * Returns a single element list containing the SearchResultItem if access is granted for write action or an empty
   * list if rejected.
   */
  private final Function<SearchResultItem, List<SearchResultItem>> filterUnprotectedItemsForWrite = Protected
          .<SearchResultItem> getPassedAsListf().o(protectResultItem(WRITE_PERMISSION));

  /**
   * Returns a single element list containing the SearchResultItem if access is granted for read action or an empty list
   * if rejected.
   */
  private final Function<SearchResultItem, List<SearchResultItem>> filterUnprotectedItemsForRead = Protected
          .<SearchResultItem> getPassedAsListf().o(protectResultItem(READ_PERMISSION));

  public static Option<EpisodeServiceException> unwrapEpisodeServiceException(Throwable e) {
    if (e == null)
      return none();
    else if (e instanceof EpisodeServiceException)
      return some((EpisodeServiceException) e);
    else
      return unwrapEpisodeServiceException(e.getCause());
  }

  /**
   * Unify exception handling by wrapping any occuring exception in an
   * {@link org.opencastproject.episode.api.EpisodeServiceException}.
   */
  public static <A> A handleException(final Function0<A> f) throws EpisodeServiceException {
    try {
      return f.apply();
    } catch (Exception e) {
      logger.error("An error occured", e);
      throw unwrapEpisodeServiceException(e).getOrElse(new EpisodeServiceException(e));
    }
  }

  /** Store all assets of <code>mp</code> under the given version. */
  private void storeAssets(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getOrgId();
    for (final MediaPackageElement e : pmp.getPartial()) {
      logger.info(format("Archiving %s %s %s", e.getFlavor(), e.getMimeType(), e.getURI()));
      final StoragePath storagePath = spath(orgId, mpId, version, e.getIdentifier());
      findAssetInVersions(e.getIdentifier(), e.getChecksum().toString()).fold(new Option.EMatch<StoragePath>() {
        @Override
        public void esome(StoragePath found) {
          if (!elementStore.copy(found, storagePath))
            throw new EpisodeServiceException("An asset with checksum " + e.getChecksum().toString() + " has"
                    + " already been archived but trying to copy or link asset " + found + " failed");
        }

        @Override
        public void enone() {
          Option<Long> size = null;
          if (e.getSize() > 0) {
            size = Option.some(e.getSize());
          } else {
            size = Option.<Long> none();
          }
          elementStore.put(storagePath, source(e.getURI(), size, option(e.getMimeType())));
        }
      });
    }
  }

  private void storeManifest(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getOrgId();
    // store the manifest.xml
    // todo make use of checksums
    logger.info(format("Archiving manifest of mediapackage %s", mpId));
    final String manifestFileName = UUID.randomUUID().toString() + ".xml";
    final URI manifestTmpUri = workspace.putInCollection("episode-service", manifestFileName,
            IOUtils.toInputStream(MediaPackageParser.getAsXml(pmp.getMediaPackage()), "UTF-8"));
    elementStore.put(spath(orgId, mpId, version, manifestAssetId(pmp, "manifest")),
            source(manifestTmpUri, Option.<Long> none(), Option.some(MimeTypes.XML)));
    workspace.deleteFromCollection("episode-service", manifestFileName);
  }

  /**
   * Create a unique id for the manifest xml. This is to avoid an id collision in the rare case that the mediapackage
   * contains an XML element with the id used for the manifest. A UUID could also be used but this is far less readable.
   * 
   * @param seedId
   *          the id to start with
   */
  private String manifestAssetId(PartialMediaPackage pmp, String seedId) {
    if (mlist(pmp.getPartial()).map(getMediaPackageElementId).value().contains(seedId)) {
      return manifestAssetId(pmp, seedId + "_");
    } else {
      return seedId;
    }
  }

  private String getOrgId() {
    return secSvc.getOrganization().getId();
  }

  /** Check if element <code>e</code> is already part of the history. */
  private Option<StoragePath> findAssetInVersions(final String assetId, final String checksum) throws Exception {
    return persistence.findAssetByChecksum(checksum).map(new Function<Asset, StoragePath>() {
      @Override
      public StoragePath apply(Asset asset) {
        logger.info("Content of {} with checksum {} has been archived before", assetId, checksum);
        return asset.getStoragePath();
      }
    });
  }

  /** Make sure each of the asset elements has a checksum. */
  public void enrichAssetChecksums(PartialMediaPackage pmp) {
    mlist(pmp.getPartial()).bind(newEnrichJob(mediaInspectionSvc, hasNoChecksum))
            .map(payloadAsMediaPackageElement(svcReg)).each(updateElement(pmp.getMediaPackage()));
  }

  /** Create a media inspection job for a mediapackage element if predicate <code>p</code> is true. */
  public static Function<MediaPackageElement, Option<Job>> newEnrichJob(final MediaInspectionService svc,
          final Function<MediaPackageElement, Boolean> p) {
    return new Function.X<MediaPackageElement, Option<Job>>() {
      @Override
      public Option<Job> xapply(MediaPackageElement e) throws Exception {
        if (p.apply(e))
          return some(svc.enrich(e, true));
        else
          return none();
      }
    };
  }

  /**
   * Interpret the payload of a completed Job as a MediaPackageElement. Wait for the job to complete if necessary.
   * 
   * @throws MediaPackageException
   *           in case the payload is not a mediapackage element
   */
  public static Function<Job, MediaPackageElement> payloadAsMediaPackageElement(final ServiceRegistry reg) {
    return new Function.X<Job, MediaPackageElement>() {
      @Override
      public MediaPackageElement xapply(Job job) throws MediaPackageException {
        waitForJob(reg, none(0L), job);
        return MediaPackageElementParser.getFromXml(job.getPayload());
      }
    };
  }

  public static Function<MediaPackageElement, URI> createUrn(final String mpId, final Version version) {
    return new Function<MediaPackageElement, URI>() {
      @Override
      public URI apply(MediaPackageElement mpe) {
        try {
          return new URI("urn", "matterhorn:" + mpId + ":" + version + ":" + mpe.getIdentifier(), null);
        } catch (URISyntaxException e) {
          throw new EpisodeServiceException(e);
        }
      }
    };
  }

  /**
   * Clears the complete solr index.
   * 
   * @throws EpisodeServiceException
   *           if clearing the index fails
   */
  public void clear() throws EpisodeServiceException {
    // todo security
    try {
      logger.info("Clearing the search index");
      solrIndex.clear();
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  /**
   * Create a function to apply a workflow on the mediapackage inside a {@link SearchResultItem}. The function either
   * returns a single element list or the empty list.
   */
  private Function<SearchResultItem, List<WorkflowInstance>> applyWorkflow(final ConfiguredWorkflow workflow,
          final UriRewriter rewriter) {
    return new Function<SearchResultItem, List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> apply(SearchResultItem item) {
        try {
          rewriteAssetsForDelivery(rewriter, item);
          return list(workflowSvc.start(workflow.getWorkflowDefinition(), item.getMediaPackage(),
                  workflow.getParameters()));
        } catch (WorkflowDatabaseException e) {
          logger.error("Error starting workflow", e);
        } catch (WorkflowParsingException e) {
          logger.error("Error starting workflow", e);
        }
        return nil();
      }
    };
  }

  /**
   * Return a partial mediapackage filtering assets. Assets are elements the archive is going to manager, i.e. all
   * non-publication elements.
   */
  public static PartialMediaPackage mkPartial(MediaPackage mp) {
    return partialMp(mp, isAsset);
  }

  public static final Predicate<MediaPackageElement> isAsset = toPredicate(isNotPublication);

  /** Rewrite URIs of all asset elements of a mediapackage. */
  public static void rewriteAssetUris(Function<MediaPackageElement, URI> uriCreator, PartialMediaPackage pmp) {
    for (MediaPackageElement mpe : pmp.getPartial()) {
      mpe.setURI(uriCreator.apply(mpe));
    }
  };

  /** Rewrite URIs of assets of mediapackage elements. */
  public static void rewriteAssetsForArchival(PartialMediaPackage pmp, Version version) {
    rewriteAssetUris(createUrn(pmp.getMediaPackage().getIdentifier().toString(), version), pmp);
  }

  /** Rewrite URIs of assets of a mediapackage contained in a search result item. */
  public static void rewriteAssetsForDelivery(UriRewriter uriRewriter, SearchResultItem item) {
    rewriteAssetUris(uriRewriter.curry(item.getOcVersion()), mkPartial(item.getMediaPackage()));
  }

  /** Query the latest version of a media package. */
  private final Function<String, List<SearchResultItem>> queryLatestMediaPackageById = new Function.X<String, List<SearchResultItem>>() {
    @Override
    public List<SearchResultItem> xapply(String id) throws Exception {
      return solrRequester.find(query(secSvc).id(id).onlyLastVersion()).getItems();
    }
  };

  /** Apply a function if the current user is authorized to perform the given actions. */
  private <A> Protected<A> protect(final AccessControlList acl, List<String> actions, Function0<A> f) {
    final User user = secSvc.getUser();
    final Organization org = secSvc.getOrganization();
    final Function<String, Boolean> isAuthorized = new Function<String, Boolean>() {
      @Override
      public Boolean apply(String action) {
        return AccessControlUtil.isAuthorized(acl, user, org, action);
      }
    };
    final boolean authorized = mlist(actions).map(isAuthorized).foldl(false, Booleans.or);
    if (authorized)
      return granted(f.apply());
    else
      return rejected(new UnauthorizedException(user, mkString(actions, ",")));
  }

  /** Protect access to the contained media package. */
  private Function<SearchResultItem, Protected<SearchResultItem>> protectResultItem(final String action) {
    return new Function.X<SearchResultItem, Protected<SearchResultItem>>() {
      @Override
      public Protected<SearchResultItem> xapply(SearchResultItem item) throws Exception {
        return protect(AccessControlParser.parseAcl(item.getOcAcl()), list(action), constant0(item));
      }
    };
  }

  /** Protect access to the contained media package. */
  private Function<Episode, Protected<Episode>> protectEpisode(final String action) {
    return new Function<Episode, Protected<Episode>>() {
      @Override
      public Protected<Episode> apply(Episode e) {
        return protect(e.getAcl(), list(action), constant0(e));
      }
    };
  }
}
