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
package org.opencastproject.publication.oaipmh;

import static java.lang.String.format;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.ofChannel;
import static org.opencastproject.util.JobUtil.waitForJobs;
import static org.opencastproject.util.data.Collections.set;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.QueryBuilder;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.oaipmh.server.OaiPmhServerInfo;
import org.opencastproject.oaipmh.server.OaiPmhServerInfoUtil;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes a recording to an OAI-PMH publication repository.
 */
public class OaiPmhPublicationServiceImpl extends AbstractJobProducer implements OaiPmhPublicationService {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhPublicationServiceImpl.class);

  /** The publication element mime type */
  private static final String MIME_TYPE = "text/xml";

  /** The element id separator */
  private static final String SEPARATOR = ";;";

  /** List of available operations on jobs */
  private enum Operation {
    Publish, Retract
  }

  /** The remote service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The security service */
  private SecurityService securityService = null;

  /** The user directory service */
  private UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService = null;

  /** The download distribution service */
  private DownloadDistributionService downloadDistributionService = null;

  /** The streaming distribution service */
  private StreamingDistributionService streamingDistributionService = null;

  /** The OAI-PMH persistence */
  private OaiPmhDatabase persistence = null;

  private OaiPmhServerInfo oaiPmhServerInfo;

  /**
   * Callback for the OSGi environment to set the service registry reference.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param distributionService
   *          the distribution service
   */
  public void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.downloadDistributionService = distributionService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param distributionService
   *          the distribution service
   */
  public void setStreamingDistributionService(StreamingDistributionService distributionService) {
    this.streamingDistributionService = distributionService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param persistence
   *          the OAI-PMH persistence
   */
  public void setPersistence(OaiPmhDatabase persistence) {
    this.persistence = persistence;
  }

  /** OSGi DI. */
  public void setOaiPmhServerInfo(OaiPmhServerInfo oaiPmhServerInfo) {
    this.oaiPmhServerInfo = oaiPmhServerInfo;
  }

  /**
   * Creates a new instance of the OAI-PMH publication service.
   */
  public OaiPmhPublicationServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job publish(MediaPackage mediaPackage, String repository, Set<String> downloadIds, Set<String> streamingIds,
          boolean checkAvailability) throws PublicationException, MediaPackageException {
    if (mediaPackage == null)
      throw new MediaPackageException("Media package must be specified");
    if (StringUtils.isEmpty(repository))
      throw new IllegalStateException("Repository must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Publish.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), // 0
                      repository, // 1
                      StringUtils.join(downloadIds, SEPARATOR), // 2
                      StringUtils.join(streamingIds, SEPARATOR), // 3
                      Boolean.toString(checkAvailability))); // 4
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create a job", e);
    }
  }

  protected Publication publishInternal(Job job, MediaPackage mp, String repository, Set<String> downloadIds,
          Set<String> streamingIds, boolean checkAvailability) throws PublicationException, MediaPackageException {
    if (!oaiPmhServerInfo.hasRepo(repository)) {
      final String msg = format("OAI-PMH repository %s does not exist", repository);
      logger.error(msg);
      throw new PublicationException(msg);
    }
    try {
      logger.info("Publishing media package {} to OAI-PMH", mp.getIdentifier());
      // => HIWEST-1692 avoid duplication of distribution artifacts
      // - Retract an existing publication.
      // - Check if media package has been published before before running a retract.
      //   Otherwise error messages will be logged.
      if (hasBeenPublished(mp.getIdentifier().toString(), repository)) {
        retractInternal(job, mp, repository);
      }
      final Publication publication = createPublicationElement(mp.getIdentifier().compact(), repository);
      final MediaPackage mpPublication = publishElementsToDownload(job, mp, repository, downloadIds, streamingIds,
              checkAvailability);
      if (mpPublication == null) {
        return null;
      }
      mpPublication.add(publication);
      try {
        persistence.store(mpPublication, repository);
        logger.info("Published {} to OAI-PMH repository {}", mpPublication, repository);
      } catch (OaiPmhDatabaseException e) {
        logger.error("Unable to store '{}' to OAI-PMH repository '{}'", mp, repository);
        throw new PublicationException(e);
      }
      logger.debug("Publish operation complete");
      return publication;

    } catch (PublicationException e) {
      throw e;
    } catch (Exception e) {
      throw new PublicationException(e);
    }
  }

  /**
   * Check if media package {@code mpId} has already been published to {@code repository}.
   */
  private boolean hasBeenPublished(String mpId, String repository) {
    return persistence.search(
        QueryBuilder
            .queryRepo(repository)
            .mediaPackageId(mpId)
            .build())
        .size() > 0;
  }

  /** Create a new publication element. */
  private Publication createPublicationElement(String mpId, String repository) throws PublicationException {
    for (String hostUrl : OaiPmhServerInfoUtil.oaiPmhServerUrlOfCurrentOrganization(securityService)) {
      final URI engageUri = URIUtils.resolve(
              URI.create(UrlSupport.concat(hostUrl, oaiPmhServerInfo.getMountPoint(), repository)),
              "?verb=ListMetadataFormats&identifier=" + mpId);
      return PublicationImpl.publication(UUID.randomUUID().toString(), publicationChannelId(repository), engageUri,
              MimeTypes.parseMimeType(MIME_TYPE));
    }
    // no host URL
    final String msg = format("No host url for oai-pmh server configured for organization %s " + "("
            + OaiPmhServerInfoUtil.ORG_CFG_OAIPMH_SERVER_HOSTURL + ")", securityService.getOrganization().getId());
    logger.error(msg);
    throw new PublicationException(msg);
  }

  @Override
  public Job retract(MediaPackage mediaPackage, String repository) throws PublicationException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Media package must be specified");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), // 0
                      repository)); // 1
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create a job", e);
    }
  }

  protected MediaPackage publishElementsToDownload(Job parentJob, MediaPackage mediaPackage, String repository,
          Set<String> downloadIds, Set<String> streamingIds, boolean checkAvailability)
          throws PublicationException, MediaPackageException {
    // Distribute to download
    final List<Job> jobs = new ArrayList<>();
    final String pubChannelId = publicationChannelId(repository);
    try {
        Job job = downloadDistributionService.distribute(pubChannelId, mediaPackage, downloadIds, checkAvailability, true);
        jobs.add(job);
        if (streamingIds.size() > 0) {
          job = streamingDistributionService.distribute(pubChannelId, mediaPackage, streamingIds);
          if (job != null) jobs.add(job);
        }
    } catch (DistributionException e) {
      throw new PublicationException(e);
    }

    if (jobs.size() < 1) {
      logger.info("No media package element was found for distribution to engage");
      return null;
    }

    // Wait until all distribution jobs have returned
    if (!waitForJobs(parentJob, serviceRegistry, jobs).isSuccess())
      throw new PublicationException("One of the distribution jobs did not complete successfully");

    logger.debug("Distribute operation completed");

    try {
      return getMediaPackageForOaiPmh(mediaPackage, jobs);
    } catch (Throwable t) {
      throw new PublicationException(t);
    }
  }

  /**
   * Retracts the mediapackage with the given identifier from the OAI-PMH repository.
   *
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  protected Publication retractInternal(Job parentJob, MediaPackage mediapackage, String repository)
          throws PublicationException {
    final String mediapackageId = mediapackage.getIdentifier().toString();
    logger.info("Trying to retract media package '{}' from OAI-PMH repository '{}'", mediapackageId, repository);
    // get _publicized_ media package
    final SearchResult sr = persistence.search(QueryBuilder.queryRepo(repository).mediaPackageId(mediapackageId)
            .build());
    final String pubChannelId = publicationChannelId(repository);
    // iterate the result set (which has at most 1 element)
    for (SearchResultItem item : sr.getItems()) {
      final MediaPackage mp = item.getMediaPackage();
      // remove from database
      try {
        persistence.delete(mediapackageId, repository);
      } catch (OaiPmhDatabaseException e) {
        logger.error("Unable to delete mediapackage '{}' from OAI-PMH repository '{}'", mediapackageId, repository);
        throw new PublicationException(e);
      } catch (NotFoundException e) {
        logger.warn("Unable to remove mediapackage '{}'from OAI-PMH repository '{}' since it does not exist",
                mediapackageId, repository);
      }
      // retract all media package elements from their distribution location
      Set<String> retractElements = new HashSet<>();
      for (MediaPackageElement element : mp.getElements()) {
        retractElements.add(element.getIdentifier());
      }
      final List<Job> jobs = new ArrayList<>();
      try {
        Job job = downloadDistributionService.retract(pubChannelId, mp, retractElements);
        if (job != null) {
          jobs.add(job);
        }
        job = streamingDistributionService.retract(pubChannelId, mp, retractElements);
        if (job != null) {
          jobs.add(job);
        }
      } catch (DistributionException ex) {
        throw new PublicationException(ex);
      }
       if (!waitForJobs(parentJob, serviceRegistry, jobs).isSuccess())
        throw new PublicationException(format("Unable to retract an element of mediapackage '%s' from the "
                + "distribution for publication repository '%s'", mediapackageId, pubChannelId));
      // use the media package parameter to determine the publication element since
      // the stored one does not contain it.
      // todo: design flaw
      for (Publication p : mlist(mediapackage.getPublications()).find(ofChannel(pubChannelId)))
        return p;
      logger.warn(format("Database query for mediapackage %s, publication repository %s yielded a mediapackage"
              + " but it does not contain a matching publication element", mediapackageId, pubChannelId));
    }
    return null;
  }

  private static String publicationChannelId(String repository) {
    return PUBLICATION_CHANNEL_PREFIX.concat(repository);
  }

  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
      String repository = arguments.get(1);
      switch (op) {
        case Publish:
          final Set<String> downloadIds = set(StringUtils.split(arguments.get(2), SEPARATOR));
          final Set<String> streamingIds = set(StringUtils.split(arguments.get(3), SEPARATOR));
          boolean checkAvailability = BooleanUtils.toBoolean(arguments.get(4));
          MediaPackageElement publishedElement = publishInternal(job, mediaPackage, repository, downloadIds,
                  streamingIds, checkAvailability);
          return (publishedElement != null) ? MediaPackageElementParser.getAsXml(publishedElement) : null;
        case Retract:
          MediaPackageElement retractedElement = retractInternal(job, mediaPackage, repository);
          return (retractedElement != null) ? MediaPackageElementParser.getAsXml(retractedElement) : null;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      logger.error("Error processing OAI-PMH operation job {}: {}", job.getId(), ExceptionUtils.getStackTrace(e));
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Returns a media package that only contains elements that are marked for distribution.
   *
   * @param current
   *          the current mediapackage
   * @param jobs
   *          list of distribution jobs
   * @return the new mediapackage
   * @throws org.opencastproject.mediapackage.MediaPackageException
   * @throws org.opencastproject.util.NotFoundException
   * @throws org.opencastproject.serviceregistry.api.ServiceRegistryException
   */
  protected MediaPackage getMediaPackageForOaiPmh(MediaPackage current, List <Job> jobs) throws MediaPackageException,
          NotFoundException, ServiceRegistryException {
    MediaPackage mp = (MediaPackage) current.clone();

    // All the jobs have passed, let's update the mediapackage with references to the distributed elements
    List<String> elementsToPublish = new ArrayList<>();
    Map<String, String> distributedElementIds = new HashMap<>();

    for (Job job : jobs) {
     try {
       final List <MediaPackageElement> distributedElements = (List <MediaPackageElement>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
       for (MediaPackageElement distributedElement: distributedElements) {
         // If there is no payload, then the item has not been distributed.
          if (job.getPayload() == null)
            continue;

          // If the job finished successfully, but returned no new element, the channel simply doesn't support this
          // kind of element. So we just keep on looping.
          if (distributedElement == null)
            continue;

          // Make sure the mediapackage is prompted to create a new identifier for this element
          distributedElement.setIdentifier(null);

          // Add the new element to the mediapackage
          mp.add(distributedElement);
          elementsToPublish.add(distributedElement.getIdentifier());
       }
     } catch (Exception e) {
       logger.error("Exception" + e);
     }

    }

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!elementsToPublish.contains(element.getIdentifier())) {
        removals.add(element);
      }
    }

    // Translate references to the distributed artifacts
    for (MediaPackageElement element : mp.getElements()) {

      if (removals.contains(element))
        continue;

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference == null)
        continue;

      // See if the element has been distributed
      String distributedElementId = null;
      if (elementsToPublish.contains(reference.getIdentifier())) {
        distributedElementId =  elementsToPublish.get(elementsToPublish.indexOf(reference.getIdentifier()));
      } else {
        continue;
      }

      MediaPackageReference translatedReference = new MediaPackageReferenceImpl(mp.getElementById(distributedElementId));
      if (reference.getProperties() != null) {
        translatedReference.getProperties().putAll(reference.getProperties());
      }

      // Set the new reference
      element.setReference(translatedReference);

    }

    // Remove everything we don't want to add to publish except the publications
    for (MediaPackageElement element : removals) {
      if (MediaPackageElement.Type.Publication.equals(element.getElementType()))
        continue;
      mp.remove(element);
    }
    return mp;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
