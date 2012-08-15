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

import org.apache.solr.client.solrj.SolrServerException;
import org.opencastproject.episode.api.ArchivedMediaPackageElement;
import org.opencastproject.episode.api.ConfiguredWorkflow;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.elementstore.ElementStore;
import org.opencastproject.episode.impl.persistence.Asset;
import org.opencastproject.episode.impl.persistence.EpisodeDto;
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
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple3;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.osgi.framework.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.episode.impl.elementstore.DeletionSelector.delAll;
import static org.opencastproject.episode.impl.elementstore.Source.source;
import static org.opencastproject.mediapackage.MediaPackageSupport.modify;
import static org.opencastproject.mediapackage.MediaPackageSupport.rewriteUris;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.JobUtil.waitForJob;
import static org.opencastproject.util.data.Collections.array;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

public final class EpisodeServiceImpl implements EpisodeService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(EpisodeServiceImpl.class);

  /** The add operation */
  public static final String OPERATION_ADD = "add";

  /** The delete operation */
  public static final String OPERATION_DELETE = "delete";

  private final SolrRequester solrRequester;
  private final SolrIndexManager solrIndex;
  private final SecurityService secSvc;
  private final AuthorizationService authSvc;
  private final OrganizationDirectoryService orgDir;
  private final ServiceRegistry svcReg;
  private final WorkflowService workflowSvc;
  private final EpisodeServiceDatabase persistence;
  private final ElementStore elementStore;
  private final MediaInspectionService mediaInspectionSvc;

  public EpisodeServiceImpl(SolrRequester solrRequester,
                            SolrIndexManager solrIndex,
                            SecurityService secSvc,
                            AuthorizationService authSvc,
                            OrganizationDirectoryService orgDir,
                            ServiceRegistry svcReg,
                            WorkflowService workflowSvc,
                            MediaInspectionService mediaInspectionSvc,
                            EpisodeServiceDatabase persistence,
                            ElementStore elementStore) {
    this.solrRequester = solrRequester;
    this.solrIndex = solrIndex;
    this.secSvc = secSvc;
    this.authSvc = authSvc;
    this.orgDir = orgDir;
    this.svcReg = svcReg;
    this.workflowSvc = workflowSvc;
    this.persistence = persistence;
    this.elementStore = elementStore;
    this.mediaInspectionSvc = mediaInspectionSvc;
  }

  @Override
  public void add(final MediaPackage mediaPackage) throws EpisodeServiceException {
    handleException(new Effect0.X() {
      @Override protected void xrun() throws Exception {
        User currentUser = secSvc.getUser();
        String orgAdminRole = secSvc.getOrganization().getAdminRole();
        if (!currentUser.hasRole(orgAdminRole) && !currentUser.hasRole(GLOBAL_ADMIN_ROLE)
                && !authSvc.hasPermission(mediaPackage, WRITE_PERMISSION)) {
          throw new UnauthorizedException(currentUser, EpisodeService.WRITE_PERMISSION);
        }

        logger.debug("Attempting to add mediapackage {} to archive", mediaPackage.getIdentifier());
        final AccessControlList acl = authSvc.getAccessControlList(mediaPackage);

        final Date now = new Date();

        // FIXME error handling ?
        final Version version = persistence.claimVersion(mediaPackage.getIdentifier().toString());
        logger.info("Creating new version {} of mediapackage {}", version, mediaPackage);

        // archive elements
        final Function<MediaPackage, MediaPackage> archiveElements = new Function.X<MediaPackage, MediaPackage>() {
          @Override public MediaPackage xapply(MediaPackage mediaPackage) throws Exception {
            archiveElements(mediaPackage, version);
            return mediaPackage;
          }
        };

        // persist in db
        final Effect<MediaPackage> persist = new Effect.X<MediaPackage>() {
          @Override protected void xrun(MediaPackage mediaPackage) throws Exception {
            try {
              persistence.storeEpisode(mediaPackage, acl, now, version);
            } catch (EpisodeServiceDatabaseException e) {
              logger.error("Could not store episode {}: {}", mediaPackage.getIdentifier(), e);
              throw new EpisodeServiceException(e);
            }
          }
        };

        // persist in solr
        final Function<MediaPackage, MediaPackage> index = new Function.X<MediaPackage, MediaPackage>() {
          @Override protected MediaPackage xapply(MediaPackage mediaPackage) throws Exception {
            if (solrIndex.add(mediaPackage, acl, now, version)) {
              logger.info("Added mediapackage {} to the archive", mediaPackage.getIdentifier());
            } else {
              logger.warn("Failed to add mediapackage {} to the archive", mediaPackage.getIdentifier());
            }
            return mediaPackage;
          }
        };

        // todo url-rewritten media package cannot be stored in solr since the solrIndex
        //   accesses metadata catalogs via StaticMetadataService which in turn uses the workspace to download
        //   them. If the URL is already a URN this does not work.
        persist.o(rewriteForArchival(version)).o(index).o(archiveElements).apply(enrichChecksums(mediaPackage));
      }
    });
  }

  /** Store all elements of <code>mp</code> under the given version if they match the filter. */
  private void archiveElements(MediaPackage mp, final Version version) throws Exception {
    final String mpId = mp.getIdentifier().toString();
    final String orgId = getOrgId();
    for (final MediaPackageElement e : mp.getElements()) {
      logger.info("Archiving {} {} {}", array(e.getFlavor(), e.getMimeType(), e.getURI()));
      findElementInVersions(e).fold(new Option.Match<StoragePath, Void>() {
        @Override public Void some(StoragePath found) {
          if (!elementStore.copy(found, spath(orgId, mpId, version, e.getIdentifier())))
            throw new EpisodeServiceException("Could not copy asset " + found);
          return null;
        }

        @Override public Void none() {
          elementStore.put(spath(orgId, mpId, version, e.getIdentifier()),
                           source(e.getURI(), Option.some(e.getSize()), option(e.getMimeType())));
          return null;
        }
      });
    }
  }

  private String getOrgId() {
    return secSvc.getOrganization().getId();
  }

  /** Check if element <code>e</code> is already part of the history. */
  private Option<StoragePath> findElementInVersions(final MediaPackageElement e) throws Exception {
    return persistence.findAssetByChecksum(e.getChecksum().toString()).map(new Function<Asset, StoragePath>() {
      @Override public StoragePath apply(Asset asset) {
        logger.info("Content of {} with checksum {} has been archived before", e.getIdentifier(), e.getChecksum());
        return asset.getStoragePath();
      }
    });
  }

  /** Make sure each of the elements has a checksum available. */
  public MediaPackage enrichChecksums(MediaPackage mp) {
    return modify(mp, new Effect<MediaPackage>() {
      @Override public void run(final MediaPackage mp) {
        mlist(mp.getElements())
                .bind(enrichIfNecessary(mediaInspectionSvc))
                .each(updateElement(mp).o(payloadAsMediaPackageElement(svcReg)));
      }
    });
  }

  /** Update a mediapackage element of a mediapackage. */
  public static Effect<MediaPackageElement> updateElement(final MediaPackage mp) {
    return new Effect<MediaPackageElement>() {
      @Override protected void run(MediaPackageElement e) {
        mp.removeElementById(e.getIdentifier());
        mp.add(e);
      }
    };
  }

  /** Enrich a mediapackage element if it does not have a checksum. */
  public static Function<MediaPackageElement, List<Job>> enrichIfNecessary(final MediaInspectionService svc) {
    return new Function.X<MediaPackageElement, List<Job>>() {
      @Override public List<Job> xapply(MediaPackageElement element) throws Exception {
        if (element.getChecksum() == null)
          return list(svc.enrich(element, true));
        return nil();
      }
    };
  }

  /**
   * Interpret the payload of a completed Job as a MediaPackageElement. Wait for the job to complete if necessary.
   *
   * @throws MediaPackageException
   *         in case the payload is not a mediapackage element
   */
  public static Function<Job, MediaPackageElement> payloadAsMediaPackageElement(final ServiceRegistry reg) {
    return new Function.X<Job, MediaPackageElement>() {
      @Override public MediaPackageElement xapply(Job job) throws MediaPackageException {
        waitForJob(reg, none(0L), job);
        return MediaPackageElementParser.getFromXml(job.getPayload());
      }
    };
  }


  /** Create a function to rewrite all media package element URIs for archival. */
  public static Function<MediaPackage, MediaPackage> rewriteForArchival(final Version version) {
    return new Function<MediaPackage, MediaPackage>() {
      @Override public MediaPackage apply(MediaPackage mp) {
        return rewriteUris(mp, new Function<MediaPackageElement, URI>() {
          @Override public URI apply(MediaPackageElement mpe) {
            return createUrn(mpe.getMediaPackage().getIdentifier().toString(), mpe.getIdentifier(), version);
          }
        });
      }
    };
  }

  public static URI createUrn(String mpId, String mpElemId, Version version) {
    try {
      return new URI("urn", "matterhorn:" + mpId + ":" + version + ":" + mpElemId, null);
    } catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  @Override
  public boolean delete(final String mediaPackageId) throws EpisodeServiceException {
    // todo security
    return handleException(new Function0.X<Boolean>() {
      @Override public Boolean xapply() throws Exception {
        SearchResult result = solrRequester.getForWrite(new EpisodeQuery().withId(mediaPackageId));
        if (result.getItems().isEmpty()) {
          logger.warn("Can not delete mediapackage {}, which is not available for the current user to delete from the archive",
                      mediaPackageId);
          return false;
        }
        logger.info("Removing mediapackage {} from the archive", mediaPackageId);

        // FIXME error handling ?
        if (!elementStore.delete(delAll(getOrgId(), mediaPackageId))) {
          throw new EpisodeServiceException("Failed to delete mediapackage " + mediaPackageId);
        }
        final Date now = new Date();
        persistence.deleteEpisode(mediaPackageId, now);
        return solrIndex.delete(mediaPackageId, now);
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow,
                                              final Function2<Version, MediaPackageElement, URI> rewriteUri,
                                              final EpisodeQuery q) throws EpisodeServiceException {
    // todo security
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override public List<WorkflowInstance> xapply() throws Exception {
        // never include locked packages
        // todo it's bad to manipulate the query object... immutability!
        q.includeLocked(false);
        // always use the last version of a media package
        q.withOnlyLastVersion();
        return mlist(solrRequester.getForRead(q).getItems()).bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow,
                                              final Function2<Version, MediaPackageElement, URI> rewriteUri,
                                              final List<String> mediaPackageIds) throws EpisodeServiceException {
    // todo security
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override public List<WorkflowInstance> xapply() throws Exception {
        return mlist(mediaPackageIds).bind(queryForMediaPackage).bind(applyWorkflow(workflow, rewriteUri)).value();
      }
    });
  }

  /**
   * Create a function to apply a workflow on the mediapackage inside a {@link SearchResultItem}.
   * The function either returns a single element list or the empty list.
   */
  private Function<SearchResultItem, List<WorkflowInstance>> applyWorkflow(final ConfiguredWorkflow workflow,
                                                                           final Function2<Version, MediaPackageElement, URI> rewriteUri) {
    return new Function<SearchResultItem, List<WorkflowInstance>>() {
      @Override public List<WorkflowInstance> apply(SearchResultItem item) {
        try {
          final MediaPackage rewritten = rewriteUris(item.getMediaPackage(), rewriteUri.curry(item.getOcVersion()));
          return list(workflowSvc.start(workflow.getWorkflowDefinition(), rewritten, workflow.getParameters()));
        } catch (WorkflowDatabaseException e) {
          logger.error("Error starting workflow", e);
        } catch (WorkflowParsingException e) {
          logger.error("Error starting workflow", e);
        }
        return nil();
      }
    };
  }

  /** Query for the latest version of a media package. */
  private final Function<String, List<SearchResultItem>> queryForMediaPackage = new Function.X<String, List<SearchResultItem>>() {
    @Override public List<SearchResultItem> xapply(String id) throws Exception {
      return solrRequester.getForWrite(new EpisodeQuery().withId(id).withOnlyLastVersion()).getItems();
    }
  };

  /**
   * Clears the complete solr index.
   *
   * @throws EpisodeServiceException
   *         if clearing the index fails
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

  @Override
  public SearchResult find(final EpisodeQuery q) throws EpisodeServiceException {
    // todo security must be handled here
    return handleException(new Function0.X<SearchResult>() {
      @Override public SearchResult xapply() throws Exception {
        return solrRequester.getForRead(q);
      }
    });
  }

  @Override
  public Option<ArchivedMediaPackageElement> get(final String mpId, final String mpElemId, final Version version)
          throws EpisodeServiceException {
    // todo security must be handled here
    return handleException(new Function0.X<Option<ArchivedMediaPackageElement>>() {
      @Override public Option<ArchivedMediaPackageElement> xapply() throws Exception {
        for (EpisodeDto dto : persistence.getEpisode(mpId, version)) {
          final MediaPackage mp = MediaPackageParser.getFromXml(dto.getMediaPackageXML());
          for (MediaPackageElement mpe : option(mp.getElementById(mpElemId))) {
            for (InputStream stream : elementStore.get(spath(getOrgId(), mpId, version, mpElemId))) {
              return some(new ArchivedMediaPackageElement(stream, mpe.getMimeType(), mpe.getSize()));
            }
          }
        }
        return none();
      }
    });
  }

  @Override
  public SearchResult findForAdministrativeRead(final EpisodeQuery q) throws EpisodeServiceException {
    // todo security must be handled here
    return handleException(new Function0.X<SearchResult>() {
      @Override public SearchResult xapply() throws Exception {
        User user = secSvc.getUser();
        if (!user.hasRole(GLOBAL_ADMIN_ROLE)) {
          throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");
        }
        return solrRequester.getForAdministrativeRead(q);
      }
    });
  }

  void populateIndex() {
    long instancesInSolr = 0L;
    try {
      instancesInSolr = solrIndex.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        Iterator<Tuple3<MediaPackage, Version, String>> episodes = persistence.getAllEpisodes();
        while (episodes.hasNext()) {
          Tuple3<MediaPackage, Version, String> episode = episodes.next();

          Version version = episode.getB();
          String mediaPackageId = episode.getA().getIdentifier().toString();

          Organization organization = orgDir.getOrganization(episode.getC());
          secSvc.setOrganization(organization);
          secSvc.setUser(new User(organization.getName(), organization.getId(), new String[]{organization
                  .getAdminRole()}));

          boolean latestVersion = persistence.isLatestVersion(mediaPackageId, version);
          AccessControlList acl = persistence.getAccessControlList(mediaPackageId, version);
          Date deletionDate = persistence.getDeletionDate(mediaPackageId);
          Date modificationDate = persistence.getModificationDate(mediaPackageId, version);
          boolean lockState = persistence.getLockState(mediaPackageId);

          solrIndex.add(episode.getA(), acl, version, latestVersion, lockState, deletionDate, modificationDate);
        }
        logger.info("Finished populating episode search index");
      } catch (Exception e) {
        logger.warn("Unable to index series instances: {}", e);
        throw new ServiceException(e.getMessage());
      } finally {
        secSvc.setOrganization(null);
        secSvc.setUser(null);
      }
    }
  }

  public static Option<EpisodeServiceException> unwrapEpisodeServiceException(Throwable e) {
    if (e == null)
      return none();
    else if (e instanceof EpisodeServiceException)
      return some((EpisodeServiceException) e);
    else
      return unwrapEpisodeServiceException(e.getCause());
  }

  /** Unify exception handling by wrapping any occuring exception in an {@link org.opencastproject.episode.api.EpisodeServiceException}. */
  public static <A> A handleException(final Function0<A> f) throws EpisodeServiceException {
    try {
      return f.apply();
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw unwrapEpisodeServiceException(e).getOrElse(new EpisodeServiceException(e));
    }
  }
}
