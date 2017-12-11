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
package org.opencastproject.event.handler;

import static org.opencastproject.job.api.Job.Status.FINISHED;
import static org.opencastproject.mediapackage.MediaPackageElementParser.getFromXml;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES;
import static org.opencastproject.publication.api.OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX;
import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.util.MediaPackageMetadataSupport;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.QueryBuilder;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

public class OaiPmhUpdatedEventHandler implements ManagedService {

  // config keys
  private static final String CFG_PROPAGATE_EPISODE = "propagate.episode";
  private static final String CFG_FLAVORS = "flavors";
  private static final String CFG_TAGS = "tags";

  /** Whether to propagate episode meta data changes to OAI-PMH or not */
  private boolean propagateEpisode = false;

  /** List of flavors to redistribute */
  private List<MediaPackageElementFlavor> flavors = new ArrayList<>();

  /** List of tags to redistribute */
  private List<String> tags = new ArrayList<>();

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(OaiPmhUpdatedEventHandler.class);

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The distribution service */
  protected DistributionService distributionService = null;

  /** The OAI-PMH persistence service */
  protected OaiPmhDatabase oaiPmhPersistence = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The organization directory */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Dublin core catalog service */
  protected DublinCoreCatalogService dublinCoreService = null;

  /** The workspace */
  protected Workspace workspace = null;

  /** The system account to use for running asynchronous events */
  protected String systemAccount = null;

  /**
   * OSGI callback for component activation.
   *
   * @param bundleContext
   *          the OSGI bundle context
   */
  protected void activate(BundleContext bundleContext) {
    this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user");
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * @param dublinCoreService
   *          the dublin core service to set
   */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreService) {
    this.dublinCoreService = dublinCoreService;
  }

  /**
   * @param distributionService
   *          the distributionService to set
   */
  public void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /**
   * @param oaiPmhPersistence
   *          the OAI-PMH persistence service to set
   */
  public void setOaiPmhPersistence(OaiPmhDatabase oaiPmhPersistence) {
    this.oaiPmhPersistence = oaiPmhPersistence;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param authorizationService
   *          the authorizationService to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void handleEvent(final SeriesItem seriesItem) {
    // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
    logger.debug("Handling {}", seriesItem);
    String seriesId = seriesItem.getSeriesId();

    // We must be an administrative user to make this query
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();
    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));

      SearchResult result = oaiPmhPersistence.search(QueryBuilder.query().seriesId(seriesId).build());

      for (SearchResultItem item : result.getItems()) {
        MediaPackage mp = item.getMediaPackage();

        Organization org = organizationDirectoryService.getOrganization(item.getOrganization());
        securityService.setOrganization(org);

        // If the security policy has been updated, make sure to distribute that change
        // to the distribution channels as well
        if (SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())) {
          // Build a new XACML file for this mediapackage
          authorizationService.setAcl(mp, AclScope.Series, seriesItem.getAcl());
          Attachment fileRepoCopy = mp.getAttachments(XACML_POLICY_SERIES)[0];

          // Distribute the updated XACML file
          Job distributionJob = distributionService.distribute(PUBLICATION_CHANNEL_PREFIX.concat(item.getRepository()),
                  mp, fileRepoCopy.getIdentifier());
          JobBarrier barrier = new JobBarrier(null, serviceRegistry, distributionJob);
          Result jobResult = barrier.waitForJobs();
          if (jobResult.getStatus().get(distributionJob).equals(FINISHED)) {
            mp.remove(fileRepoCopy);
            mp.add(getFromXml(serviceRegistry.getJob(distributionJob.getId()).getPayload()));
          } else {
            logger.error("Unable to distribute XACML {}", fileRepoCopy.getIdentifier());
            continue;
          }
        }

        // Update the series dublin core
        if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())
                || SeriesItem.Type.UpdateElement.equals(seriesItem.getType())) {
          DublinCoreCatalog seriesDublinCore = null;
          MediaPackageElementFlavor catalogType = null;
          if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())) {
            seriesDublinCore = seriesItem.getMetadata();
            mp.setSeriesTitle(seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE));
            catalogType = MediaPackageElements.SERIES;
          } else {
            seriesDublinCore = seriesItem.getExtendedMetadata();
            catalogType = MediaPackageElementFlavor.flavor(seriesItem.getElementType(), "series");
          }

          // Update the series dublin core
          Catalog[] seriesCatalogs = mp.getCatalogs(catalogType);
          if (seriesCatalogs.length == 1) {
            Catalog c = seriesCatalogs[0];
            String filename = FilenameUtils.getName(c.getURI().toString());
            URI uri = workspace.put(mp.getIdentifier().toString(), c.getIdentifier(), filename,
                    dublinCoreService.serialize(seriesDublinCore));
            c.setURI(uri);
            // setting the URI to a new source so the checksum will most like be invalid
            c.setChecksum(null);

            // Distribute the updated series dc
            Job distributionJob = distributionService.distribute(
                    PUBLICATION_CHANNEL_PREFIX.concat(item.getRepository()), mp, c.getIdentifier());
            JobBarrier barrier = new JobBarrier(null, serviceRegistry, distributionJob);
            Result jobResult = barrier.waitForJobs();
            if (jobResult.getStatus().get(distributionJob).equals(FINISHED)) {
              mp.remove(c);
              mp.add(getFromXml(serviceRegistry.getJob(distributionJob.getId()).getPayload()));
            } else {
              logger.error("Unable to distribute series catalog {}", c.getIdentifier());
              continue;
            }
          }
        }

        // Remove the series catalog and isPartOf from episode catalog
        if (SeriesItem.Type.Delete.equals(seriesItem.getType())) {
          mp.setSeries(null);
          mp.setSeriesTitle(null);

          boolean retractSeriesCatalog = retractSeriesCatalog(mp, item.getRepository());
          boolean updateEpisodeCatalog = updateEpisodeCatalog(mp, item.getRepository());

          if (!retractSeriesCatalog || !updateEpisodeCatalog)
            continue;
        }

        // Update the OAI-PMH persitence with the modified mediapackage
        oaiPmhPersistence.store(mp, item.getRepository());
      }
    } catch (ServiceRegistryException e) {
      logger.error(e.getMessage());
    } catch (NotFoundException e) {
      logger.error(e.getMessage());
    } catch (MediaPackageException e) {
      logger.error(e.getMessage());
    } catch (IOException e) {
      logger.error(e.getMessage());
    } catch (DistributionException e) {
      logger.error(e.getMessage());
    } catch (OaiPmhDatabaseException e) {
      logger.error(e.getMessage());
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }

  private boolean retractSeriesCatalog(MediaPackage mp, String repository) throws DistributionException {
    // Retract the series catalog
    for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
      Job retractJob = distributionService
              .retract(PUBLICATION_CHANNEL_PREFIX.concat(repository), mp, c.getIdentifier());
      JobBarrier barrier = new JobBarrier(null, serviceRegistry, retractJob);
      Result jobResult = barrier.waitForJobs();
      if (jobResult.getStatus().get(retractJob).equals(FINISHED)) {
        mp.remove(c);
      } else {
        logger.error("Unable to retract series catalog {}", c.getIdentifier());
        return false;
      }
    }
    return true;
  }

  private boolean updateEpisodeCatalog(MediaPackage mp, String repository) throws DistributionException,
      MediaPackageException, NotFoundException, ServiceRegistryException, IllegalArgumentException, IOException {
    // Update the episode catalog
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
      Job distributionJob = distributionService.distribute(PUBLICATION_CHANNEL_PREFIX.concat(repository), mp,
              episodeCatalog.getIdentifier());
      JobBarrier barrier = new JobBarrier(null, serviceRegistry, distributionJob);
      Result jobResult = barrier.waitForJobs();
      if (jobResult.getStatus().get(distributionJob).equals(FINISHED)) {
        mp.remove(episodeCatalog);
        mp.add(getFromXml(serviceRegistry.getJob(distributionJob.getId()).getPayload()));
      } else {
        logger.error("Unable to distribute episode catalog {}", episodeCatalog.getIdentifier());
        return false;
      }
    }
    return true;
  }

  public void handleEvent(AssetManagerItem.TakeSnapshot snapshotItem) {
    if (!propagateEpisode) {
      logger.trace("Skipping automatic propagation of episode meta data to OAI-PMH since it is turned off.");
      return;
    }

    //An episode or its ACL has been updated. Construct the MediaPackage and publish it to OAI-PMH.
    logger.debug("Handling {}", snapshotItem);

    // We must be an administrative user to make a query to the OaiPmhPublicationService
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();

    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));
      Query query = QueryBuilder.query().mediaPackageId(snapshotItem.getMediapackage()).build();
      SearchResult result = oaiPmhPersistence.search(query);
      if (result.getItems().size() > 1) {
        logger.error("Found multiple ({}) OAI-PMH records for media package: {}", result.getItems().size(),
            snapshotItem.getMediapackage().getIdentifier());
        return;
      }
      if (result.getItems().size() < 1) {
        logger.trace("There is no OAI-PMH record to update for media package {}. Skipping.",
            snapshotItem.getMediapackage().getIdentifier());
        return;
      }

      if (result.getItems().get(0).isDeleted()) {
        logger.trace("This OAI-PMH record has been deleted {}. Skipping.",
                snapshotItem.getMediapackage().getIdentifier());
        return;
      }

      SearchResultItem item = result.getItems().get(0);
      MediaPackage repoMp = item.getMediaPackage(); // This is the media package from OAI-PMH which has to be updated.

      // distribute all changed elements of media package.
      MediaPackage newMp = snapshotItem.getMediapackage();
      for (MediaPackageElement mpe : newMp.elements()) {
        if (mpe instanceof Publication || !containsFlavor(flavors, mpe.getFlavor()) || !containsTag(tags, mpe.getTags())) {
          continue;
        }
        Job distributionJob = distributionService.distribute(PUBLICATION_CHANNEL_PREFIX.concat(item.getRepository()),
                newMp, mpe.getIdentifier());
        JobBarrier barrier = new JobBarrier(null, serviceRegistry, distributionJob);
        Result jobResult = barrier.waitForJobs();
        if (jobResult.getStatus().get(distributionJob).equals(FINISHED)) {
          MediaPackageElement distributedElement = getFromXml(serviceRegistry.getJob(distributionJob.getId()).getPayload());
          MediaPackageElement toRemove = (MediaPackageElement) distributedElement.clone();
          toRemove.setIdentifier(null); // We cannot use id here because it differs when package is published initially.
          repoMp.remove(toRemove);
          repoMp.add(distributedElement);
        } else {
          logger.error("Unable to distribute media package element {}", mpe.getIdentifier());
        }
      }

      // We now apply the new meta data to the media package
      final MediaPackageMetadata metadata = dublinCoreService.getMetadata(newMp);
      MediaPackageMetadataSupport.populateMediaPackageMetadata(repoMp, metadata);
      repoMp.setSeries(newMp.getSeries());
      repoMp.setSeriesTitle(newMp.getSeriesTitle());

      // Update the OAI-PMH persistence with the updated mediapackage
      oaiPmhPersistence.store(repoMp, item.getRepository());
    } catch (DistributionException | MediaPackageException | ServiceRegistryException | OaiPmhDatabaseException
        | NotFoundException e) {
      logger.error(e.getMessage());
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }

  @Override
  public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
    this.flavors = new ArrayList<>();
    this.tags = new ArrayList<>();

    final Option<Boolean> propagateEpisode = getOptCfgAsBoolean(dictionary, CFG_PROPAGATE_EPISODE);
    if (propagateEpisode.isSome()) {
      this.propagateEpisode = propagateEpisode.get();
    }

    final Option<String> flavorsRaw = getOptCfg(dictionary, CFG_FLAVORS);
    if (flavorsRaw.isSome()) {
      final String[] flavorStrings = flavorsRaw.get().split("\\s*,\\s*");
      for (String flavorString : flavorStrings) {
        flavors.add(MediaPackageElementFlavor.parseFlavor(flavorString));
      }
    }

    final Option<String> tagsRaw = getOptCfg(dictionary, CFG_TAGS);
    if (tagsRaw.isSome()) {
      final String[] tags = tagsRaw.get().split("\\s*,\\s*");
      this.tags.addAll(Arrays.asList(tags));
    }
  }

  private static boolean containsFlavor(List<MediaPackageElementFlavor> flavors, MediaPackageElementFlavor flavor) {
    for (MediaPackageElementFlavor current : flavors) {
      if (current.matches(flavor)) return true;
    }
    return flavors.isEmpty();
  }

  private static boolean containsTag(List<String> tags, String[] mpeTags) {
    for (String current : mpeTags) {
      if (tags.contains(current)) return true;
    }
    return tags.isEmpty();
  }
}
