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
package org.opencastproject.archive.base;

import static java.lang.String.format;
import static org.opencastproject.archive.base.PartialMediaPackage.partialMp;
import static org.opencastproject.archive.base.Protected.granted;
import static org.opencastproject.archive.base.Protected.rejected;
import static org.opencastproject.archive.base.QueryBuilder.query;
import static org.opencastproject.archive.base.StoragePath.spath;
import static org.opencastproject.archive.base.storage.DeletionSelector.delAll;
import static org.opencastproject.archive.base.storage.Source.source;
import static org.opencastproject.mediapackage.MediaPackageSupport.copy;
import static org.opencastproject.mediapackage.MediaPackageSupport.getMediaPackageElementId;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.hasNoChecksum;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Functions.constant0;
import static org.opencastproject.util.data.functions.Functions.toPredicate;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.ArchivedMediaPackageElement;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDbException;
import org.opencastproject.archive.base.persistence.Asset;
import org.opencastproject.archive.base.persistence.Episode;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.index.IndexProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.archive.ArchiveItem;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect;
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
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Base implementation of the archive abstracting over search and index. */
public abstract class ArchiveBase<RS extends ResultSet> extends AbstractIndexProducer implements Archive<RS> {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(ArchiveBase.class);

  private final SecurityService secSvc;
  private final AuthorizationService authSvc;
  private final OrganizationDirectoryService orgDir;
  private final ServiceRegistry svcReg;
  private final WorkflowService workflowSvc;
  private final Workspace workspace;
  private final ArchiveDb persistence;
  private final ElementStore elementStore;
  private final String systemUserName;
  private final MessageSender messageSender;
  private final MessageReceiver messageReceiver;
  private UriRewriter uriRewriter = null;

  public ArchiveBase(SecurityService secSvc, AuthorizationService authSvc, OrganizationDirectoryService orgDir,
          ServiceRegistry svcReg, WorkflowService workflowSvc, Workspace workspace, ArchiveDb persistence,
          ElementStore elementStore, String systemUserName, MessageSender messageSender, MessageReceiver messageReceiver) {
    this.secSvc = secSvc;
    this.authSvc = authSvc;
    this.orgDir = orgDir;
    this.svcReg = svcReg;
    this.workflowSvc = workflowSvc;
    this.workspace = workspace;
    this.persistence = persistence;
    this.elementStore = elementStore;
    this.systemUserName = systemUserName;
    this.messageSender = messageSender;
    this.messageReceiver = messageReceiver;
  }

  protected abstract void index(MediaPackage mp, AccessControlList acl, Date timestamp, Version version);

  protected abstract void index(MediaPackage mediaPackage, AccessControlList acl, Version version, boolean deleted,
          Date modificationDate, boolean latestVersion);

  protected abstract boolean indexDelete(String mediaPackageId, Date timestamp);

  protected abstract RS indexFind(Query q);

  protected abstract long indexSize();

  // todo constructor method can be removed when security filtering has been moved to query
  protected abstract RS newResultSet(List<? extends ResultItem> rs, String query, long totalSize, long offset,
          long limit, long searchTime);

  @Override
  /*
   * todo adding to the archive needs to be synchronized because of the index. It resets the oc_latest_version flag and
   * this must not happen concurrently. This approach works as long as the archive is not distributed.
   */
  public synchronized void add(final MediaPackage mp) throws ArchiveException {
    handleException(new Effect0.X() {
      @Override
      public void xrun() throws Exception {
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
    messageSender.sendObjectMessage(ArchiveItem.ARCHIVE_QUEUE, MessageSender.DestinationType.Queue,
            ArchiveItem.update(mp, acl, version, now));
    // store mediapackage in index
    /*
     * todo: Url-rewritten mediapackages cannot be stored in solr since the solr index accesses metadata catalogs via
     * StaticMetadataService which in turn uses the workspace to download them. If the URL is already a URN this does
     * not work.
     */
    index(mp, acl, now, version);
    // store mediapackage in db
    try {
      rewriteAssetsForArchival(pmp, version);
      persistence.storeEpisode(pmp, acl, now, version);
    } catch (ArchiveDbException e) {
      logger.error("Could not store episode {}: {}", mpId, e);
      throw new ArchiveException(e);
    }
    // save manifest to element store
    // this is done at the end after the media package element ids have been rewritten to neutral URNs
    storeManifest(pmp, version);
  }

  // todo if the user is allowed to delete the whole set of versions is checked against the
  // acl of the latest version. That's probably not the best approach.
  @Override
  public boolean delete(final String mediaPackageId) throws ArchiveException {
    return handleException(new Function0.X<Boolean>() {
      @Override
      public Boolean xapply() throws Exception {
        for (Protected<Episode> p : persistence.getLatestEpisode(mediaPackageId).map(protectEpisode(WRITE_PERMISSION))) {
          if (p.isGranted()) {
            logger.info("Removing mediapackage {} from the archive", mediaPackageId);
            final Date now = new Date();
            // Using bitwise AND operator to make sure all deletion methods are executed even if one fails
            boolean deleted = elementStore.delete(delAll(getOrgId(), mediaPackageId))
                    & persistence.deleteEpisode(mediaPackageId, now) & indexDelete(mediaPackageId, now);
            messageSender.sendObjectMessage(ArchiveItem.ARCHIVE_QUEUE, MessageSender.DestinationType.Queue,
                    ArchiveItem.delete(mediaPackageId, now));
            return deleted;
          } else {
            // rejected
            throw new ArchiveException(p.getRejected());
          }
        }
        // mediapackage not found
        return false;
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final Query q) throws ArchiveException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        // todo only the latest version is used, this should change when the UI supports versions
        return mlist(indexFind(query(q).onlyLastVersion(true)).getItems()).bind(filterUnprotectedItemsForWrite)
                .bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final List<String> mediaPackageIds) throws ArchiveException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        return mlist(mediaPackageIds).bind(queryLatestMediaPackageById).bind(filterUnprotectedItemsForWrite)
                .bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  // todo maybe the resulting list should just be filtered directly by the query
  // The current approach has low performance since all result items are fetched from the index and filtered
  // afterwards. This is due to moving security checks from the sub-components towards the EpisodeService.
  @SuppressWarnings("unchecked")
  @Override
  public RS find(final Query q, final UriRewriter rewriter) throws ArchiveException {
    return handleException(new Function0.X<RS>() {
      @Override
      public RS xapply() throws Exception {
        final RS rs = indexFind(q);
        // filter and rewrite items
        final List<ResultItem> filtered = mlist(rs.getItems()).bind(filterUnprotectedItemsForRead)
                .each(rewriteAssetsForDelivery(rewriter)).value();
        final long newTotal = rs.getTotalSize() - (rs.size() - filtered.size());
        return newResultSet(filtered, rs.getQuery(), newTotal, rs.getOffset(), rs.getLimit(), rs.getSearchTime());
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public RS findForAdministrativeRead(final Query q, final UriRewriter rewriter) throws ArchiveException {
    return handleException(new Function0.X<RS>() {
      @Override
      public RS xapply() throws Exception {
        User user = secSvc.getUser();
        Organization organization = user.getOrganization();
        if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(organization.getAdminRole())) {
          throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");
        }
        final RS r = indexFind(q);
        for (ResultItem item : r.getItems()) {
          // rewrite URIs in place
          rewriteAssetsForDelivery(rewriter, item);
        }
        return r;
      }
    });
  }

  @Override
  public Option<ArchivedMediaPackageElement> get(final String mpId, final String mpElemId, final Version version)
          throws ArchiveException {
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
            throw p.getRejected();
          }
        }
        // episode cannot be found
        return none();
      }
    });
  }

  //

  /**
   * Since the index is populated from persistent storage where media package element URIs have been transformed to
   * location independent URNs these have to be rewritten to point to actual locations.
   */
  public void populateIndex(UriRewriter uriRewriter) {
    this.uriRewriter = uriRewriter;
    long instancesInSolr;
    try {
      instancesInSolr = indexSize();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      logger.info("Start populating episode search index");
      Map<String, Version> maps = new HashMap<String, Version>();
      Iterator<Episode> episodes;
      try {
        episodes = persistence.getAllEpisodes();
      } catch (ArchiveDbException e) {
        logger.error("Unable to load the archive entries: {}", e);
        throw new ServiceException(e.getMessage());
      }
      int errors = 0;
      while (episodes.hasNext()) {
        final Episode episode = episodes.next();
        try {
          String episodeId = episode.getMediaPackage().getIdentifier().toString();

          Version latestVersion = maps.get(episodeId);
          if (latestVersion == null) {
            Option<Episode> latestEpisode = persistence.getLatestEpisode(episodeId);
            if (latestEpisode.isNone())
              throw new ArchiveException("Latest episode from existing episode identifier not found!");
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
          index(pmp.getMediaPackage(), episode.getAcl(), episode.getVersion(), episode.isDeleted(),
                  episode.getModificationDate(), isLatestVersion);
        } catch (Exception e) {
          logger.error(
                  "Unable to index episode {} version {} instances: {}",
                  new String[] { episode.getMediaPackage().toString(), episode.getVersion().toString(),
                          ExceptionUtils.getStackTrace(e) });
          errors++;
        } finally {
          secSvc.setOrganization(null);
          secSvc.setUser(null);
        }
      }
      if (errors > 0)
        logger.error("Skipped {} erroneous archive entries while populating the episode search index", errors);
      logger.info("Finished populating episode search index");
    } else {
      logger.debug("No need to populate episode search index");
    }
  }

  /**
   * Returns a single element list containing the SearchResultItem if access is granted for write action or an empty
   * list if rejected.
   */
  private final Function<ResultItem, List<ResultItem>> filterUnprotectedItemsForWrite = Protected
          .<ResultItem> getPassedAsListf().o(protectResultItem(WRITE_PERMISSION));

  /**
   * Returns a single element list containing the SearchResultItem if access is granted for read action or an empty list
   * if rejected.
   */
  private final Function<ResultItem, List<ResultItem>> filterUnprotectedItemsForRead = Protected
          .<ResultItem> getPassedAsListf().o(protectResultItem(READ_PERMISSION));

  public static Option<ArchiveException> unwrapEpisodeServiceException(Throwable e) {
    if (e == null)
      return none();
    else if (e instanceof ArchiveException)
      return some((ArchiveException) e);
    else
      return unwrapEpisodeServiceException(e.getCause());
  }

  /**
   * Unify exception handling by wrapping any occuring exception in an
   * {@link org.opencastproject.archive.api.ArchiveException}.
   */
  public static <A> A handleException(final Function0<A> f) throws ArchiveException {
    try {
      return f.apply();
    } catch (Exception e) {
      logger.error("An error occurred", e);
      throw unwrapEpisodeServiceException(e).getOrElse(new ArchiveException(e));
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
            throw new ArchiveException("An asset with checksum " + e.getChecksum().toString() + " has"
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
    final URI manifestTmpUri = workspace.putInCollection("archive", manifestFileName,
            IOUtils.toInputStream(MediaPackageParser.getAsXml(pmp.getMediaPackage()), "UTF-8"));
    elementStore.put(spath(orgId, mpId, version, manifestAssetId(pmp, "manifest")),
            source(manifestTmpUri, Option.<Long> none(), Option.some(MimeTypes.XML)));
    workspace.deleteFromCollection("archive", manifestFileName);
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
    mlist(pmp.getPartial()).filter(hasNoChecksum).each(addChecksum);
  }

  private final Effect<MediaPackageElement> addChecksum = new Effect<MediaPackageElement>() {
    @Override
    protected void run(MediaPackageElement mpe) {
      try {
        logger.info("Calculate checksum for " + mpe.getURI());
        mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, getFileFromWorkspace(mpe.getURI())));
      } catch (IOException e) {
        throw new ArchiveException(format("Cannot calculate checksum for media package element %s", mpe.getURI()), e);
      }
    }
  };

  public File getFileFromWorkspace(URI uri) {
    try {
      return workspace.get(uri);
    } catch (NotFoundException e) {
      throw new ArchiveException(format("Cannot find file at URI %s", uri), e);
    } catch (IOException e) {
      throw new ArchiveException(format("Cannot access file at URI %s", uri), e);
    }
  }

  public static Function<MediaPackageElement, URI> createUrn(final String mpId, final Version version) {
    return new Function<MediaPackageElement, URI>() {
      @Override
      public URI apply(MediaPackageElement mpe) {
        try {
          return new URI("urn", "matterhorn:" + mpId + ":" + version + ":" + mpe.getIdentifier(), null);
        } catch (URISyntaxException e) {
          throw new ArchiveException(e);
        }
      }
    };
  }

  /**
   * Create a function to apply a workflow on the mediapackage inside a
   * {@link org.opencastproject.archive.api.ResultItem}. The function either returns a single element list or the empty
   * list.
   */
  private Function<ResultItem, List<WorkflowInstance>> applyWorkflow(final ConfiguredWorkflow workflow,
          final UriRewriter rewriter) {
    return new Function<ResultItem, List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> apply(ResultItem item) {
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
  }

  /** Rewrite URIs of assets of mediapackage elements. */
  public static void rewriteAssetsForArchival(PartialMediaPackage pmp, Version version) {
    rewriteAssetUris(createUrn(pmp.getMediaPackage().getIdentifier().toString(), version), pmp);
  }

  /** Rewrite URIs of assets of a mediapackage contained in a search result item. */
  public static void rewriteAssetsForDelivery(UriRewriter uriRewriter, ResultItem item) {
    rewriteAssetUris(uriRewriter.curry(item.getVersion()), mkPartial(item.getMediaPackage()));
  }

  /**
   * {@link #rewriteAssetsForDelivery(org.opencastproject.archive.api.UriRewriter, org.opencastproject.archive.api.ResultItem)}
   * as a function.
   */
  public static Effect<ResultItem> rewriteAssetsForDelivery(final UriRewriter uriRewriter) {
    return new Effect<ResultItem>() {
      @Override
      protected void run(ResultItem item) {
        rewriteAssetsForDelivery(uriRewriter, item);
      }
    };
  }

  /** Query the latest version of a media package. */
  private final Function<String, List<ResultItem>> queryLatestMediaPackageById = new Function.X<String, List<ResultItem>>() {
    @Override
    public List<ResultItem> xapply(String id) throws Exception {
      final Query q = query().currentOrganization(secSvc).mediaPackageId(id).onlyLastVersion(true);
      return indexFind(q).getItems();
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
  private Function<ResultItem, Protected<ResultItem>> protectResultItem(final String action) {
    return new Function.X<ResultItem, Protected<ResultItem>>() {
      @Override
      public Protected<ResultItem> xapply(ResultItem item) throws Exception {
        return protect(item.getAcl(), list(action), constant0(item));
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

  @Override
  public void repopulate(final String indexName) throws Exception {
    if (uriRewriter == null)
      throw new ArchiveException("No HttpMediaPackageElementProvider available!");

    final String destinationId = ArchiveItem.ARCHIVE_QUEUE_PREFIX + WordUtils.capitalize(indexName);

    logger.info("Start populating archive for index '{}'", indexName);
    final int total;
    final int[] current = new int[1];
    current[0] = 1;
    Map<String, Version> maps = new HashMap<String, Version>();
    Iterator<Episode> episodes;
    try {
      episodes = persistence.getAllEpisodes();
      total = persistence.countAllEpisodes();
    } catch (ArchiveDbException e) {
      logger.error("Unable to load the archive entries: {}", e);
      throw new ServiceException(e.getMessage());
    }
    int errors = 0;
    while (episodes.hasNext()) {
      final Episode episode = episodes.next();
      try {
        String episodeId = episode.getMediaPackage().getIdentifier().toString();

        Version latestVersion = maps.get(episodeId);
        if (latestVersion == null) {
          Option<Episode> latestEpisode = persistence.getLatestEpisode(episodeId);
          if (latestEpisode.isNone())
            throw new ArchiveException("Latest episode from existing episode identifier not found!");
          latestVersion = latestEpisode.get().getVersion();
          maps.put(episodeId, latestVersion);
        }

        final Organization organization = orgDir.getOrganization(episode.getOrganization());
        SecurityUtil.runAs(secSvc, organization, SecurityUtil.createSystemUser(systemUserName, organization),
                new Effect0() {
                  @Override
                  protected void run() {
                    // mediapackage URIs need to be rewritten to concrete URLs for indexation to work
                    final PartialMediaPackage pmp = mkPartial(episode.getMediaPackage());
                    rewriteAssetUris(uriRewriter.curry(episode.getVersion()), pmp);
                    messageSender.sendObjectMessage(
                            destinationId,
                            MessageSender.DestinationType.Queue,
                            ArchiveItem.update(pmp.getMediaPackage(), episode.getAcl(), episode.getVersion(),
                                    episode.getModificationDate()));
                    messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                            IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Archive, total,
                                    current[0]));
                    current[0] += 1;
                  }
                });
      } catch (Exception e) {
        logger.error(
                "Unable to index archive {} version {} instances: {}",
                new String[] { episode.getMediaPackage().toString(), episode.getVersion().toString(),
                        ExceptionUtils.getStackTrace(e) });
        errors++;
      }
    }
    if (errors > 0)
      logger.error("Skipped {} erroneous archive entries while populating the archive index", errors);
    logger.info("Finished populating archive for index {}", indexName);

    final Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(secSvc, organization, SecurityUtil.createSystemUser(systemUserName, organization),
            new Effect0() {
              @Override
              protected void run() {
                messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                        IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Archive));
              }
            });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Archive;
  }

  @Override
  public String getClassName() {
    return ArchiveBase.class.getName();
  }

}
