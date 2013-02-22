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

import static org.opencastproject.episode.api.EpisodeQuery.query;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.episode.impl.elementstore.DeletionSelector.delAll;
import static org.opencastproject.episode.impl.elementstore.Source.source;
import static org.opencastproject.mediapackage.MediaPackageSupport.modify;
import static org.opencastproject.mediapackage.MediaPackageSupport.rewriteUris;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.JobUtil.waitForJob;
import static org.opencastproject.util.data.Collections.array;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Functions.constant;
import static org.opencastproject.util.data.functions.Misc.chuck;

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
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

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
  private final EpisodeServiceDatabase persistence;
  private final ElementStore elementStore;
  private final MediaInspectionService mediaInspectionSvc;
  private final String systemUserName;

  public EpisodeServiceImpl(SolrRequester solrRequester, SolrIndexManager solrIndex, SecurityService secSvc,
          AuthorizationService authSvc, OrganizationDirectoryService orgDir, ServiceRegistry svcReg,
          WorkflowService workflowSvc, MediaInspectionService mediaInspectionSvc, EpisodeServiceDatabase persistence,
          ElementStore elementStore, String systemUserName) {
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
    this.systemUserName = systemUserName;
  }

  @Override
  // todo adding to the archive needs to be synchronized because of the index.
  // It resets the oc_latest_version flag and this must not happen concurrently.
  // This approach works as long as the archive is not distributed.
  public synchronized void add(final MediaPackage mediaPackage) throws EpisodeServiceException {
    handleException(new Effect0.X() {
      @Override
      protected void xrun() throws Exception {
        logger.debug("Attempting to add mediapackage {} to archive", mediaPackage.getIdentifier());
        final AccessControlList acl = authSvc.getAccessControlList(mediaPackage);
        protect(acl, list(WRITE_PERMISSION), new Effect0.X() {
          @Override
          public void xrun() throws Exception {
            final Date now = new Date();

            // FIXME error handling ?
            final Version version = persistence.claimVersion(mediaPackage.getIdentifier().toString());
            logger.info("Creating new version {} of mediapackage {}", version, mediaPackage);

            // archive elements
            final Function<MediaPackage, MediaPackage> archiveElements = new Function.X<MediaPackage, MediaPackage>() {
              @Override
              public MediaPackage xapply(MediaPackage mediaPackage) throws Exception {
                archiveElements(mediaPackage, version);
                return mediaPackage;
              }
            };

            // persist in solr
            final Function<MediaPackage, MediaPackage> index = new Function.X<MediaPackage, MediaPackage>() {
              @Override
              protected MediaPackage xapply(MediaPackage mediaPackage) throws Exception {
                if (solrIndex.add(mediaPackage, acl, now, version)) {
                  logger.info("Added mediapackage {} to the archive", mediaPackage.getIdentifier());
                } else {
                  logger.warn("Failed to add mediapackage {} to the archive", mediaPackage.getIdentifier());
                }
                return mediaPackage;
              }
            };

            // persist in db
            final Effect<MediaPackage> persist = new Effect.X<MediaPackage>() {
              @Override
              protected void xrun(MediaPackage mediaPackage) throws Exception {
                try {
                  persistence.storeEpisode(mediaPackage, acl, now, version);
                } catch (EpisodeServiceDatabaseException e) {
                  logger.error("Could not store episode {}: {}", mediaPackage.getIdentifier(), e);
                  throw new EpisodeServiceException(e);
                }
              }
            };

            // todo url-rewritten media package cannot be stored in solr since the solrIndex
            // accesses metadata catalogs via StaticMetadataService which in turn uses the workspace to download
            // them. If the URL is already a URN this does not work.
            // todo make archiving transactional
            persist.o(rewriteForArchival(version)).o(index).o(archiveElements).apply(enrichChecksums(mediaPackage));
          }
        });
      }
    });
  }

  // todo if the user is allowed to delete the whole set of versions is checked against the
  // acl of the latest version. That's probably not the best approach.
  @Override
  public boolean delete(final String mediaPackageId) throws EpisodeServiceException {
    return handleException(new Function0.X<Boolean>() {
      @Override
      public Boolean xapply() throws Exception {
        for (Episode e : persistence.getLatestEpisode(mediaPackageId)) {
          return protect(e.getAcl(), list(WRITE_PERMISSION), new Function0.X<Boolean>() {
            @Override
            public Boolean xapply() throws Exception {
              logger.info("Removing mediapackage {} from the archive", mediaPackageId);
              final Date now = new Date();
              return elementStore.delete(delAll(getOrgId(), mediaPackageId))
                      && persistence.deleteEpisode(mediaPackageId, now) && solrIndex.delete(mediaPackageId, now);
            }
          });
        }
        // none
        return false;
      }
    });
  }

  // todo workflows are started until user is not authorized
  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final EpisodeQuery q) throws EpisodeServiceException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        // todo only the latest version is used, this should change when the UI supports versions
        q.onlyLastVersion();
        return mlist(solrRequester.find(q).getItems()).bind(
                applyWorkflow(workflow, rewriteUri).o(protectResultItem(WRITE_PERMISSION))).value();
      }
    });
  }

  // todo workflows are started until user is not authorized
  @Override
  public List<WorkflowInstance> applyWorkflow(final ConfiguredWorkflow workflow, final UriRewriter rewriteUri,
          final List<String> mediaPackageIds) throws EpisodeServiceException {
    return handleException(new Function0.X<List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> xapply() throws Exception {
        return mlist(mediaPackageIds).bind(queryForMediaPackage)
                .bind(applyWorkflow(workflow, rewriteUri).o(protectResultItem(WRITE_PERMISSION))).value();
      }
    });
  }

  // todo method only returns if _all_ media packages can be read by the current user.
  // maybe the resulting list should just be filterered
  // This approach has low performance since all result items are fetched from the index and filtered
  // afterwards. This is due to moving security checks from the subcomponents towards the EpisodeService.
  @Override
  public SearchResult find(final EpisodeQuery q) throws EpisodeServiceException {
    return handleException(new Function0.X<SearchResult>() {
      @Override
      public SearchResult xapply() throws Exception {
        final SearchResult r = solrRequester.find(q);
        // check if user is allowed to read each media package
        mlist(r.getItems()).each(protectResultItem(READ_PERMISSION).toEffect());
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
        for (final Episode e : persistence.getEpisode(mpId, version).map(protectEpisode(READ_PERMISSION))) {
          final MediaPackage mp = e.getMediaPackage();
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
    return handleException(new Function0.X<SearchResult>() {
      @Override
      public SearchResult xapply() throws Exception {
        User user = secSvc.getUser();
        Organization organization = orgDir.getOrganization(user.getOrganization());
        if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(organization.getAdminRole()))
          throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

        return solrRequester.find(q);
      }
    });
  }

  //

  /**
   * Since the index is populated from persitent storage where media package element URIs have been transformed to
   * location independend URNs these have to be rewritten to point to actual locations.
   */
  void populateIndex(UriRewriter uriRewriter) {
    long instancesInSolr = 0L;
    try {
      instancesInSolr = solrIndex.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      logger.info("Start populating episode search index");
      try {
        Map<String, Version> maps = new HashMap<String, Version>();
        Iterator<Episode> episodes = persistence.getAllEpisodes();
        while (episodes.hasNext()) {
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
          boolean isLatestVersion = episode.getVersion().equals(latestVersion) ? true : false;

          final Organization organization = orgDir.getOrganization(episode.getOrganization());
          secSvc.setOrganization(organization);
          secSvc.setUser(SecurityUtil.createSystemUser(systemUserName, organization));
          // The whole media package gets rewritten here. This is not the best approach since then
          // the media package is stored in the index with concrete URLs.
          // Just rewriting URLs on a per element basis does not work either since a media package element clone loses
          // the reference to its parent media package. Some rewriters like the one provided by the
          // AbstractEpisodeServiceRestEndpoint need this reference though.
          //
          // However storing the concrete URLs in the index is not that problematic because
          // URLs get rewritten on delivery anyway. See #add(..)
          solrIndex.add(rewriteUris(episode.getMediaPackage(), uriRewriter.curry(episode.getVersion())),
                  episode.getAcl(), episode.getVersion(), episode.getDeletionDate(), episode.getModificationDate(),
                  isLatestVersion);
        }
        logger.info("Finished populating episode search index");
      } catch (Exception e) {
        logger.warn("Unable to index series instances: {}", e);
        throw new ServiceException(e.getMessage());
      } finally {
        secSvc.setOrganization(null);
        secSvc.setUser(null);
      }
    } else {
      logger.debug("No need to populate episode search index");
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

  /**
   * Unify exception handling by wrapping any occuring exception in an
   * {@link org.opencastproject.episode.api.EpisodeServiceException}.
   */
  public static <A> A handleException(final Function0<A> f) throws EpisodeServiceException {
    try {
      return f.apply();
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw unwrapEpisodeServiceException(e).getOrElse(new EpisodeServiceException(e));
    }
  }

  /** Store all elements of <code>mp</code> under the given version if they match the filter. */
  private void archiveElements(MediaPackage mp, final Version version) throws Exception {
    final String mpId = mp.getIdentifier().toString();
    final String orgId = getOrgId();
    for (final MediaPackageElement e : mp.getElements()) {
      logger.info("Archiving {} {} {}", array(e.getFlavor(), e.getMimeType(), e.getURI()));
      findElementInVersions(e).fold(new Option.Match<StoragePath, Void>() {
        @Override
        public Void some(StoragePath found) {
          if (!elementStore.copy(found, spath(orgId, mpId, version, e.getIdentifier())))
            throw new EpisodeServiceException("Could not copy asset " + found);
          return null;
        }

        @Override
        public Void none() {
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
      @Override
      public StoragePath apply(Asset asset) {
        logger.info("Content of {} with checksum {} has been archived before", e.getIdentifier(), e.getChecksum());
        return asset.getStoragePath();
      }
    });
  }

  /** Make sure each of the elements has a checksum available. */
  public MediaPackage enrichChecksums(MediaPackage mp) {
    return modify(mp, new Effect<MediaPackage>() {
      @Override
      public void run(final MediaPackage mp) {
        mlist(mp.getElements()).bind(enrichIfNecessary(mediaInspectionSvc)).each(
                updateElement(mp).o(payloadAsMediaPackageElement(svcReg)));
      }
    });
  }

  /** Update a mediapackage element of a mediapackage. */
  public static Effect<MediaPackageElement> updateElement(final MediaPackage mp) {
    return new Effect<MediaPackageElement>() {
      @Override
      protected void run(MediaPackageElement e) {
        mp.removeElementById(e.getIdentifier());
        mp.add(e);
      }
    };
  }

  /** Enrich a mediapackage element if it does not have a checksum. */
  public static Function<MediaPackageElement, List<Job>> enrichIfNecessary(final MediaInspectionService svc) {
    return new Function.X<MediaPackageElement, List<Job>>() {
      @Override
      public List<Job> xapply(MediaPackageElement element) throws Exception {
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

  /** Create a function to rewrite all media package element URIs for archival. */
  public static Function<MediaPackage, MediaPackage> rewriteForArchival(final Version version) {
    return new Function<MediaPackage, MediaPackage>() {
      @Override
      public MediaPackage apply(MediaPackage mp) {
        return rewriteUris(mp, new Function<MediaPackageElement, URI>() {
          @Override
          public URI apply(MediaPackageElement mpe) {
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
          final UriRewriter rewriteUri) {
    return new Function<SearchResultItem, List<WorkflowInstance>>() {
      @Override
      public List<WorkflowInstance> apply(SearchResultItem item) {
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

  /** Apply a function if the current user is authorized to perform the given actions. */
  public <A> A protect(final AccessControlList acl, List<String> actions, Function0<A> f) {
    final User user = secSvc.getUser();
    final Organization org = secSvc.getOrganization();
    final boolean authorized = mlist(actions).map(new Function<String, Boolean>() {
      @Override
      public Boolean apply(String action) {
        return AccessControlUtil.isAuthorized(acl, user, org, action);
      }
    }).foldl(false, Booleans.or);
    if (authorized)
      return f.apply();
    else
      return chuck(new UnauthorizedException(user, mkString(actions, ",")));
  }

  /** Protect access to the contained media package. */
  private Function<SearchResultItem, SearchResultItem> protectResultItem(final String action) {
    return new Function.X<SearchResultItem, SearchResultItem>() {
      @Override
      public SearchResultItem xapply(SearchResultItem item) throws Exception {
        return protect(AccessControlParser.parseAcl(item.getOcAcl()), list(action), constant(item));
      }
    };
  }

  /** Protect access to the contained media package. */
  private Function<Episode, Episode> protectEpisode(final String action) {
    return new Function.X<Episode, Episode>() {
      @Override
      public Episode xapply(Episode e) throws Exception {
        return protect(e.getAcl(), list(action), constant(e));
      }
    };
  }

  /** Query for the latest version of a media package. */
  private final Function<String, List<SearchResultItem>> queryForMediaPackage = new Function.X<String, List<SearchResultItem>>() {
    @Override
    public List<SearchResultItem> xapply(String id) throws Exception {
      return solrRequester.find(query(secSvc).id(id).onlyLastVersion()).getItems();
    }
  };
}
