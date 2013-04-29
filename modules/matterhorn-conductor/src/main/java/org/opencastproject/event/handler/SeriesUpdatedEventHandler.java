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
package org.opencastproject.event.handler;

import static org.opencastproject.event.EventAdminConstants.ID;
import static org.opencastproject.event.EventAdminConstants.PAYLOAD;
import static org.opencastproject.event.EventAdminConstants.SERIES_ACL_TOPIC;
import static org.opencastproject.job.api.Job.Status.FINISHED;
import static org.opencastproject.mediapackage.MediaPackageElementParser.getFromXml;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.workflow.handler.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responds to series events by re-distributing metadata and security policy files for published mediapackages.
 */
public class SeriesUpdatedEventHandler implements EventHandler {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(SeriesUpdatedEventHandler.class);

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The distribution service */
  protected DistributionService distributionService = null;

  /** The series service */
  protected SeriesService seriesService = null;

  /** The search service */
  protected SearchService searchService = null;

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

  /** The executor */
  protected ExecutorService executor = null;

  /**
   * OSGI callback for component activation.
   * 
   * @param bundleContext
   *          the OSGI bundle context
   */
  protected void activate(BundleContext bundleContext) {
    this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user");
    this.executor = Executors.newCachedThreadPool();
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
   * @param seriesService
   *          the series service to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
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
   * @param searchService
   *          the searchService to set
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
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

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  @Override
  public void handleEvent(final Event event) {
    logger.debug("Queuing {}", event);

    // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
    executor.execute(new Runnable() {
      @Override
      public void run() {
        logger.debug("Handling {}", event);
        String seriesId = (String) event.getProperty(ID);

        // We must be an administrative user to make this query
        try {
          DefaultOrganization defaultOrg = new DefaultOrganization();
          securityService.setOrganization(defaultOrg);
          securityService.setUser(new User(systemAccount, defaultOrg.getId(), new String[] { GLOBAL_ADMIN_ROLE }));

          SearchQuery q = new SearchQuery().withId(seriesId);
          SearchResult result = searchService.getForAdministrativeRead(q);

          for (SearchResultItem item : result.getItems()) {
            MediaPackage mp = item.getMediaPackage();
            Organization org = organizationDirectoryService.getOrganization(item.getOrganization());
            securityService.setOrganization(org);

            // If the security policy has been updated, make sure to distribute that change
            // to the distribution channels as well
            if (SERIES_ACL_TOPIC.equals(event.getTopic())) {
              // Build a new XACML file for this mediapackage
              AccessControlList acl = AccessControlParser.parseAcl((String) event.getProperty(PAYLOAD));
              authorizationService.setAccessControl(mp, acl);
              Attachment fileRepoCopy = mp.getAttachments(XACML_POLICY)[0];

              // Distribute the updated XACML file
              Job distributionJob = distributionService.distribute(CHANNEL_ID, mp, fileRepoCopy.getIdentifier());
              JobBarrier barrier = new JobBarrier(serviceRegistry, distributionJob);
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
            DublinCoreCatalog seriesDublinCore = seriesService.getSeries(seriesId);
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
              Job distributionJob = distributionService.distribute(CHANNEL_ID, mp, c.getIdentifier());
              JobBarrier barrier = new JobBarrier(serviceRegistry, distributionJob);
              Result jobResult = barrier.waitForJobs();
              if (jobResult.getStatus().get(distributionJob).equals(FINISHED)) {
                mp.remove(c);
                mp.add(getFromXml(serviceRegistry.getJob(distributionJob.getId()).getPayload()));
              } else {
                logger.error("Unable to distribute series catalog {}", c.getIdentifier());
                continue;
              }
            }

            // Update the search index with the modified mediapackage
            searchService.add(mp);
            Job searchJob = searchService.add(mp);
            JobBarrier barrier = new JobBarrier(serviceRegistry, searchJob);
            barrier.waitForJobs();
          }

        } catch (SearchException e) {
          logger.warn("Unable to find mediapackages in search: ", e.getMessage());
        } catch (UnauthorizedException e) {
          logger.warn(e.getMessage());
        } catch (MediaPackageException e) {
          logger.warn(e.getMessage());
        } catch (ServiceRegistryException e) {
          logger.warn(e.getMessage());
        } catch (NotFoundException e) {
          logger.warn(e.getMessage());
        } catch (IOException e) {
          logger.warn(e.getMessage());
        } catch (AccessControlParsingException e) {
          logger.warn(e.getMessage());
        } catch (DistributionException e) {
          logger.warn(e.getMessage());
        } catch (SeriesException e) {
          logger.warn(e.getMessage());
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }
      }
    });
  }
}
