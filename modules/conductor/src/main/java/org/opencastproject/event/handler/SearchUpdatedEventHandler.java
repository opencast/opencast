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

package org.opencastproject.event.handler;

import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;
import static org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Responds to series events by re-distributing metadata and security policy files for published mediapackages. */
@Component(
    immediate = true,
    service = {
        SearchUpdatedEventHandler.class
    },
    property = {
        "service.description=Search Updated Event Handler"
    }
)
public class SearchUpdatedEventHandler {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(SearchUpdatedEventHandler.class);

  // config keys
  protected static final String DISTRIBUTION_CHECK_AVAILABILITY = "distribution.check.availability";

  /** Whether to propagate episode meta data changes to OAI-PMH or not */
  private boolean checkAvailability;

  /** The distribution service */
  protected DownloadDistributionService downloadDistributionService = null;

  /** The search service */
  protected SearchService searchService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** Dublin core catalog service */
  protected DublinCoreCatalogService dublinCoreService = null;

  /** The workspace */
  protected Workspace workspace = null;

  /** The system account to use for running asynchronous events */
  protected String systemAccount = null;

  /**
   * OSGI callback for component activation.
   *
   * @param componentContext
   *          the OSGI component context
   */
  @Activate
  protected void activate(ComponentContext componentContext) {
    this.systemAccount = componentContext.getBundleContext().getProperty("org.opencastproject.security.digest.user");
    updated(componentContext);
  }

  @Modified
  protected void updated(ComponentContext componentContext) {
    checkAvailability = BooleanUtils.toBoolean(Objects.toString(componentContext.getProperties()
            .get(DISTRIBUTION_CHECK_AVAILABILITY), "true"));
    logger.info("Check-availability flag set to {}", checkAvailability);
  }

  /**
   * @param workspace
   *          the workspace to set
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * @param dublinCoreService
   *          the dublin core service to set
   */
  @Reference
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreService) {
    this.dublinCoreService = dublinCoreService;
  }

  /**
   * @param downloadDistributionService
   *          the downloadDstributionService to set
   */
  @Reference(target = "(distribution.channel=download)")
  public void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    this.downloadDistributionService = downloadDistributionService;
  }

  /**
   * @param searchService
   *          the searchService to set
   */
  @Reference
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param authorizationService
   *          the authorizationService to set
   */
  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void handleEvent(final SeriesItem seriesItem) {
    // A series or its ACL has been updated. Find any media packages with that series, and update them.
    logger.debug("Handling {}", seriesItem);
    String seriesId = seriesItem.getSeriesId();

    // We must be an administrative user to make this query
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();
    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));

      for (var seriesData: searchService.getSeries(seriesId)) {
        var mp = seriesData.getRight();
        Organization org = seriesData.getLeft();
        securityService.setOrganization(org);

        // If the security policy has been updated, make sure to distribute that change
        // to the distribution channels as well
        if (SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())) {
          try {
            if (Boolean.TRUE.equals(seriesItem.getOverrideEpisodeAcl())) {

              MediaPackageElement[] distributedEpisodeAcls = mp.getElementsByFlavor(XACML_POLICY_EPISODE);
              for (MediaPackageElement distributedEpisodeAcl : distributedEpisodeAcls) {
                downloadDistributionService.retractSync(CHANNEL_ID, mp, distributedEpisodeAcl.getIdentifier());
                authorizationService.removeAcl(mp, AclScope.Episode);
              }
            }

            Attachment fileRepoCopy = authorizationService.setAcl(mp, AclScope.Series, seriesItem.getAcl()).getB();

            // Distribute the updated XACML file
            List<MediaPackageElement> mpes = downloadDistributionService.distributeSync(CHANNEL_ID, mp,
                fileRepoCopy.getIdentifier(), checkAvailability);
            if (mpes != null && mpes.size() == 1) {
              mp.remove(fileRepoCopy);
              mp.add(mpes.getFirst());
            } else {
              throw new DistributionException("Unable to distribute series XACML " + fileRepoCopy.getIdentifier());
            }
          } catch (DistributionException | MediaPackageException e) {
            logger.error("Could not update series ACL in search for event {} of series {}", mp.getIdentifier(),
                seriesId, e);
            continue;
          }
        }

        // Update the series dublin core
        if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())) {
          try {
            DublinCoreCatalog seriesDublinCore = seriesItem.getMetadata();
            mp.setSeriesTitle(seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE));

            // Update the series dublin core
            Catalog[] seriesCatalogs = mp.getCatalogs(MediaPackageElements.SERIES);
            if (seriesCatalogs.length == 1) {
              Catalog c = seriesCatalogs[0];
              String filename = FilenameUtils.getName(c.getURI().toString());
              URI uri = workspace.put(mp.getIdentifier().toString(), c.getIdentifier(), filename,
                  dublinCoreService.serialize(seriesDublinCore));
              c.setURI(uri);
              // setting the URI to a new source so the checksum will most like be invalid
              c.setChecksum(null);

              // Distribute the updated series dc
              List<MediaPackageElement> mpes = downloadDistributionService.distributeSync(
                  CHANNEL_ID, mp, c.getIdentifier(), checkAvailability);
              if (mpes != null && mpes.size() == 1) {
                mp.remove(c);
                mp.add(mpes.getFirst());
              } else {
                throw new DistributionException("Unable to distribute series catalog " + c.getIdentifier());
              }
            }
          } catch (DistributionException | IOException e) {
            logger.error("Could not update series catalog in search for event {} of series {}", mp.getIdentifier(),
                seriesId, e);
            continue;
          }
        }

        // Remove the series catalog and isPartOf from episode catalog
        if (SeriesItem.Type.Delete.equals(seriesItem.getType())) {
          try {
            mp.setSeries(null);
            mp.setSeriesTitle(null);

            // retract the series catalog
            for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
              downloadDistributionService.retractSync(CHANNEL_ID, mp, c.getIdentifier());
              mp.remove(c);
            }

            // update episode catalog
            for (Catalog episodeCatalog : mp.getCatalogs(MediaPackageElements.EPISODE)) {
              DublinCoreCatalog episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog);
              episodeDublinCore.remove(DublinCore.PROPERTY_IS_PART_OF);
              String filename = FilenameUtils.getName(episodeCatalog.getURI().toString());
              URI uri = workspace.put(mp.getIdentifier().toString(), episodeCatalog.getIdentifier(), filename,
                  dublinCoreService.serialize(episodeDublinCore));
              episodeCatalog.setURI(uri);
              // setting the URI to a new source so the checksum will most like be invalid
              episodeCatalog.setChecksum(null);

              // Distribute the updated episode dublincore
              List<MediaPackageElement> mpes = downloadDistributionService.distributeSync(CHANNEL_ID, mp,
                  episodeCatalog.getIdentifier(), checkAvailability);

              if (mpes != null && mpes.size() == 1) {
                mp.remove(episodeCatalog);
                mp.add(mpes.getFirst());
              } else {
                throw new DistributionException(
                    "Unable to distribute episode catalog " + episodeCatalog.getIdentifier());
              }
            }
          } catch (DistributionException | IOException e) {
            logger.error("Could remove series {} from search for event {}", seriesId, mp.getIdentifier(), e);
            continue;
          }
        }

        // Update the search index with the modified mediapackage
        try {
          searchService.addSynchronously(mp);
        } catch (SearchException e) {
          logger.error("Unable to update media package {} in search for series {}", mp.getIdentifier(), seriesId, e);
        }
      }
      //We remove the episode->series links above, which effectively orphaned the series in the index, now we remove it
      if (SeriesItem.Type.Delete.equals(seriesItem.getType())) {
        try {
          searchService.deleteSeries(seriesId);
        } catch (NotFoundException e) {
          // that's fine
        } catch (SearchException e) {
          logger.error("Could not delete series {} from search", seriesId, e);
        }
      }
    } catch (UnauthorizedException e) {
      logger.error("Unoauthorized for system user - this should never happen!");
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }
}
