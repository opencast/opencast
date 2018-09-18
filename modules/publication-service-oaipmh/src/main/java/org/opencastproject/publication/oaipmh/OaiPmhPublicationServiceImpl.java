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
import static org.opencastproject.util.JobUtil.waitForJobs;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageRuntimeException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
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
import org.opencastproject.util.data.Collections;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Publishes a recording to an OAI-PMH publication repository.
 */
public class OaiPmhPublicationServiceImpl extends AbstractJobProducer implements OaiPmhPublicationService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhPublicationServiceImpl.class);

  public enum Operation {
    Publish, Retract, UpdateMetadata, Replace
  }

  private DownloadDistributionService downloadDistributionService;
  private OaiPmhServerInfo oaiPmhServerInfo;
  private OaiPmhDatabase oaiPmhDatabase;
  private OrganizationDirectoryService organizationDirectoryService;
  private SecurityService securityService;
  private ServiceRegistry serviceRegistry;
  private StreamingDistributionService streamingDistributionService;
  private UserDirectoryService userDirectoryService;

  public OaiPmhPublicationServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  protected String process(Job job) throws Exception {
    if (!StringUtils.equalsIgnoreCase(JOB_TYPE, job.getJobType()))
      throw new IllegalArgumentException("Can not handle job type " + job.getJobType());

    Publication publication = null;
    MediaPackage mediaPackage = MediaPackageParser.getFromXml(job.getArguments().get(0));
    String repository = job.getArguments().get(1);
    boolean checkAvailability = false;
    switch (Operation.valueOf(job.getOperation())) {
      case Publish:
        String[] downloadElementIds = StringUtils.split(job.getArguments().get(2), SEPARATOR);
        String[] streamingElementIds = StringUtils.split(job.getArguments().get(3), SEPARATOR);
        checkAvailability = BooleanUtils.toBoolean(job.getArguments().get(4));
        publication = publish(job, mediaPackage, repository,
                Collections.set(downloadElementIds), Collections.set(streamingElementIds), checkAvailability);
        break;
      case Replace:
        final Set<? extends MediaPackageElement> downloadElements =
            Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.getArguments().get(2)));
        final Set<? extends MediaPackageElement> streamingElements =
            Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.getArguments().get(3)));
        final Set<MediaPackageElementFlavor> retractDownloadFlavors = Arrays.stream(
            StringUtils.split(job.getArguments().get(4), SEPARATOR))
            .filter(s -> !s.isEmpty())
            .map(MediaPackageElementFlavor::parseFlavor)
            .collect(Collectors.toSet());
        final Set<MediaPackageElementFlavor> retractStreamingFlavors = Arrays.stream(
            StringUtils.split(job.getArguments().get(5), SEPARATOR))
            .filter(s -> !s.isEmpty())
            .map(MediaPackageElementFlavor::parseFlavor)
            .collect(Collectors.toSet());
        final Set<? extends MediaPackageElement> publications =
            Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.getArguments().get(6)));
        checkAvailability = BooleanUtils.toBoolean(job.getArguments().get(7));
        publication = replace(job, mediaPackage, repository, downloadElements, streamingElements,
            retractDownloadFlavors, retractStreamingFlavors, publications, checkAvailability);
        break;
      case Retract:
        publication = retract(job, mediaPackage, repository);
        break;
      case UpdateMetadata:
        checkAvailability = BooleanUtils.toBoolean(job.getArguments().get(4));
        String[] flavors = StringUtils.split(job.getArguments().get(2), SEPARATOR);
        String[] tags = StringUtils.split(job.getArguments().get(3), SEPARATOR);
        publication = updateMetadata(job, mediaPackage, repository,
                Collections.set(flavors), Collections.set(tags), checkAvailability);
        break;
      default:
        throw new IllegalArgumentException("Can not handle this type of operation: " + job.getOperation());
    }
    return publication != null ? MediaPackageElementParser.getAsXml(publication) : null;
  }

  @Override
  public Job replace(MediaPackage mediaPackage, String repository, Set<? extends MediaPackageElement> downloadElements,
         Set<? extends MediaPackageElement> streamingElements, Set<MediaPackageElementFlavor> retractDownloadFlavors,
         Set<MediaPackageElementFlavor> retractStreamingFlavors, Set<? extends Publication> publications,
         boolean checkAvailability) throws PublicationException {
    checkInputArguments(mediaPackage, repository);
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Replace.name(),
          Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), // 0
              repository, // 1
              MediaPackageElementParser.getArrayAsXml(Collections.toList(downloadElements)), // 2
              MediaPackageElementParser.getArrayAsXml(Collections.toList(streamingElements)), // 3
              StringUtils.join(retractDownloadFlavors, SEPARATOR), // 4
              StringUtils.join(retractStreamingFlavors, SEPARATOR), // 5
              MediaPackageElementParser.getArrayAsXml(Collections.toList(publications)), // 6
              Boolean.toString(checkAvailability))); // 7
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create job", e);
    } catch (MediaPackageException e) {
      throw new PublicationException("Unable to serialize media package elements", e);
    }
  }

  @Override
  public Publication replaceSync(
      MediaPackage mediaPackage, String repository, Set<? extends MediaPackageElement> downloadElements,
      Set<? extends MediaPackageElement> streamingElements, Set<MediaPackageElementFlavor> retractDownloadFlavors,
      Set<MediaPackageElementFlavor> retractStreamingFlavors, Set<? extends Publication> publications,
      boolean checkAvailability) throws PublicationException, MediaPackageException {
    return replace(null, mediaPackage, repository, downloadElements, streamingElements, retractDownloadFlavors,
        retractStreamingFlavors, publications, checkAvailability);
  }

  @Override
  public Job publish(MediaPackage mediaPackage, String repository, Set<String> downloadElementIds,
          Set<String> streamingElementIds, boolean checkAvailability)
          throws PublicationException, MediaPackageException {
    checkInputArguments(mediaPackage, repository);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Publish.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), // 0
                      repository, // 1
                      StringUtils.join(downloadElementIds, SEPARATOR), // 2
                      StringUtils.join(streamingElementIds, SEPARATOR), // 3
                      Boolean.toString(checkAvailability))); // 4
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create job", e);
    }
  }

  @Override
  public Job retract(MediaPackage mediaPackage, String repository) throws PublicationException, NotFoundException {
    checkInputArguments(mediaPackage, repository);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), repository));
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create job", e);
    }
  }

  @Override
  public Job updateMetadata(MediaPackage mediaPackage, String repository, Set<String> flavors, Set<String> tags,
          boolean checkAvailability) throws PublicationException, MediaPackageException {
    checkInputArguments(mediaPackage, repository);
    if ((flavors == null || flavors.isEmpty()) && (tags == null || tags.isEmpty()))
      throw new IllegalArgumentException("Flavors or tags must be set");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.UpdateMetadata.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), // 0
                      repository, // 1
                      StringUtils.join(flavors, SEPARATOR), // 2
                      StringUtils.join(tags, SEPARATOR), // 3
                      Boolean.toString(checkAvailability))); // 4
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create job", e);
    }
  }

  protected Publication publish(Job job, MediaPackage mediaPackage, String repository, Set<String> downloadElementIds,
          Set<String> streamingElementIds, boolean checkAvailability)
          throws PublicationException, MediaPackageException {
    String mpId = mediaPackage.getIdentifier().compact();
    SearchResult searchResult = oaiPmhDatabase.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
            .isDeleted(false).build());
    // retract oai-pmh if published
    if (searchResult.size() > 0) {
      try {
        Publication p = retract(job, mediaPackage, repository);
        if (mediaPackage.contains(p))
          mediaPackage.remove(p);
      } catch (NotFoundException e) {
        logger.debug("No OAI-PMH publication found for media package {}.", mpId, e);
        // this is ok
      }
    }
    List<Job> distributionJobs = new ArrayList<>(2);
    if (downloadElementIds != null && !downloadElementIds.isEmpty()) {
      // select elements for download distribution
      MediaPackage mpDownloadDist = (MediaPackage) mediaPackage.clone();
      for (MediaPackageElement mpe : mpDownloadDist.getElements()) {
        if (downloadElementIds.contains(mpe.getIdentifier()))
          continue;
        mpDownloadDist.remove(mpe);
      }
      // publish to download
      if (mpDownloadDist.getElements().length > 0) {
        try {
          Job downloadDistributionJob = downloadDistributionService
                  .distribute(getPublicationChannelName(repository), mpDownloadDist, downloadElementIds,
                          checkAvailability);
          if (downloadDistributionJob != null) {
            distributionJobs.add(downloadDistributionJob);
          }
        } catch (DistributionException e) {
          throw new PublicationException(format("Unable to distribute media package %s to download distribution.", mpId),
                  e);
        }
      }
    }
    if (streamingElementIds != null && !streamingElementIds.isEmpty()) {
      // select elements for streaming distribution
      MediaPackage mpStreamingDist = (MediaPackage) mediaPackage.clone();
      for (MediaPackageElement mpe : mpStreamingDist.getElements()) {
        if (streamingElementIds.contains(mpe.getIdentifier()))
          continue;
        mpStreamingDist.remove(mpe);
      }
      // publish to streaming
      if (mpStreamingDist.getElements().length > 0) {
        try {
          Job streamingDistributionJob = streamingDistributionService
                  .distribute(getPublicationChannelName(repository), mpStreamingDist, streamingElementIds);
          if (streamingDistributionJob != null) {
            distributionJobs.add(streamingDistributionJob);
          }
        } catch (DistributionException e) {
          throw new PublicationException(format("Unable to distribute media package %s to streaming distribution.", mpId),
                  e);
        }
      }
    }
    if (distributionJobs.isEmpty()) {
      throw new IllegalStateException(format(
              "The media package %s does not contain any elements for publishing to OAI-PMH", mpId));
    }
    // wait for distribution jobs
    if (!waitForJobs(job, serviceRegistry, distributionJobs).isSuccess()) {
      throw new PublicationException(format(
              "Unable to distribute elements of media package %s to distribution channels.", mpId));
    }

    List<MediaPackageElement> distributedElements = new ArrayList<>();
    for (Job distributionJob : distributionJobs) {
      String distributedElementsXml = distributionJob.getPayload();
      if (StringUtils.isNotBlank(distributedElementsXml)) {
        for (MediaPackageElement distributedElement : MediaPackageElementParser.getArrayFromXml(distributedElementsXml)) {
          distributedElements.add(distributedElement);
        }
      }
    }
    MediaPackage oaiPmhDistMp = (MediaPackage) mediaPackage.clone();
    // cleanup media package elements
    for (MediaPackageElement mpe : oaiPmhDistMp.getElements()) {
      // keep publications
      if (MediaPackageElement.Type.Publication == mpe.getElementType())
        continue;
      oaiPmhDistMp.remove(mpe);
    }
    // ...add the distributed elements
    for (MediaPackageElement mpe : distributedElements) {
      oaiPmhDistMp.add(mpe);
    }

    // publish to oai-pmh
    try {
      oaiPmhDatabase.store(oaiPmhDistMp, repository);
    } catch (OaiPmhDatabaseException e) {
      // todo: should we retract the elements from download and streaming here?
      throw new PublicationException(format("Unable to distribute media package %s to OAI-PMH repository %s", mpId, repository), e);
    }
    return createPublicationElement(mpId, repository);
  }

  private Publication replace(Job job, MediaPackage mediaPackage, String repository,
          Set<? extends MediaPackageElement> downloadElements, Set<? extends MediaPackageElement> streamingElements,
          Set<MediaPackageElementFlavor> retractDownloadFlavors, Set<MediaPackageElementFlavor> retractStreamingFlavors,
          Set<? extends MediaPackageElement> publications, boolean checkAvailable) throws MediaPackageException,
      PublicationException {
    final String mpId = mediaPackage.getIdentifier().compact();
    final String channel = getPublicationChannelName(repository);

    try {
      final SearchResult search = oaiPmhDatabase.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
          .isDeleted(false).build());
      if (search.size() > 1) throw new PublicationException("Found multiple OAI-PMH records for id " + mpId);
      final Optional<MediaPackage> existingMp = search.getItems().stream().findFirst().map(
              SearchResultItem::getMediaPackage);

      // Collect Ids of elements to distribute
      final Set<String> addDownloadElementIds = downloadElements.stream()
          .map(MediaPackageElement::getIdentifier)
          .collect(Collectors.toSet());
      final Set<String> addStreamingElementIds = streamingElements.stream()
          .map(MediaPackageElement::getIdentifier)
          .collect(Collectors.toSet());

      // Use retractFlavors to search for existing elements to retract
      final Set<MediaPackageElement> removeDownloadElements = existingMp.map(mp ->
          Arrays.stream(mp.getElements())
          .filter(e -> retractDownloadFlavors.stream().anyMatch(f -> f.matches(e.getFlavor())))
          .collect(Collectors.toSet())
      ).orElse(java.util.Collections.emptySet());
      final Set<MediaPackageElement> removeStreamingElements = existingMp.map(mp ->
          Arrays.stream(mp.getElements())
              .filter(e -> retractStreamingFlavors.stream().anyMatch(f -> f.matches(e.getFlavor())))
              .collect(Collectors.toSet())
      ).orElse(java.util.Collections.emptySet());

      // Element IDs to retract. Elements identified by flavor and elements to re-distribute
      final Set<String> removeDownloadElementIds = Stream
          .concat(removeDownloadElements.stream(), downloadElements.stream())
          .map(MediaPackageElement::getIdentifier)
          .collect(Collectors.toSet());
      final Set<String> removeStreamingElementIds = Stream
          .concat(removeStreamingElements.stream(), streamingElements.stream())
          .map(MediaPackageElement::getIdentifier)
          .collect(Collectors.toSet());

      if (removeDownloadElementIds.isEmpty() && removeStreamingElementIds.isEmpty()
          && addDownloadElementIds.isEmpty() && addStreamingElementIds.isEmpty()) {
        // Nothing to do
        return Arrays.stream(mediaPackage.getPublications())
            .filter(p -> channel.equals(p.getChannel()))
            .findFirst()
            .orElse(null);
      }

      final MediaPackage temporaryMediaPackage = (MediaPackage) mediaPackage.clone();
      downloadElements.forEach(temporaryMediaPackage::add);
      streamingElements.forEach(temporaryMediaPackage::add);
      removeDownloadElements.forEach(temporaryMediaPackage::add);
      removeStreamingElements.forEach(temporaryMediaPackage::add);

      final List<MediaPackageElement> retractedElements = new ArrayList<>();
      final List<MediaPackageElement> distributedElements = new ArrayList<>();
      if (job != null) {
        retractedElements
            .addAll(retract(job, channel, temporaryMediaPackage, removeDownloadElementIds, removeStreamingElementIds));
        distributedElements
            .addAll(distribute(job, channel, temporaryMediaPackage, addDownloadElementIds, addStreamingElementIds,
                checkAvailable));
      } else {
        retractedElements
            .addAll(retractSync(channel, temporaryMediaPackage, removeDownloadElementIds, removeStreamingElementIds));
        distributedElements
            .addAll(distributeSync(channel, temporaryMediaPackage, addDownloadElementIds, addStreamingElementIds,
                checkAvailable));
      }

      final MediaPackage oaiPmhDistMp = (MediaPackage) existingMp.orElse(mediaPackage).clone();

      // Remove OAI-PMH publication
      Arrays.stream(oaiPmhDistMp.getPublications())
          .filter(p -> channel.equals(p.getChannel()))
          .forEach(oaiPmhDistMp::remove);

      // Remove retracted elements
      retractedElements.stream()
          .map(MediaPackageElement::getIdentifier)
          .forEach(oaiPmhDistMp::removeElementById);
      // Add new distributed elements
      distributedElements.forEach(oaiPmhDistMp::add);

      // Remove old publications
      publications.stream()
          .map(p -> ((Publication) p).getChannel())
          .forEach(c -> Arrays.stream(oaiPmhDistMp.getPublications())
              .filter(p -> c.equals(p.getChannel()))
              .forEach(oaiPmhDistMp::remove));

      // Add updated publications
      publications.forEach(oaiPmhDistMp::add);

      // publish to oai-pmh
      oaiPmhDatabase.store(oaiPmhDistMp, repository);

      return Arrays.stream(mediaPackage.getPublications())
          .filter(p -> channel.equals(p.getChannel()))
          .findFirst()
          .orElse(createPublicationElement(mpId, repository));
    } catch (OaiPmhDatabaseException e) {
      throw new PublicationException(format("Unable to update media package %s in OAI-PMH repository %s", mpId,
          repository), e);
    } catch (DistributionException e) {
      throw new PublicationException(format("Unable to update OAI-PMH distributions of media package %s.", mpId), e);
    } catch (MediaPackageRuntimeException e) {
      throw e.getWrappedException();
    }
  }

  private List<MediaPackageElement> retract(
          Job job, String channel, MediaPackage mp, Set<String> removeDownloadElementIds,
          Set<String> removeStreamingElementIds) throws PublicationException, DistributionException {
    final List<Job> retractJobs = new ArrayList<>(2);
    if (!removeDownloadElementIds.isEmpty()) {
      retractJobs.add(downloadDistributionService.retract(channel, mp, removeDownloadElementIds));
    }
    if (!removeStreamingElementIds.isEmpty()) {
      retractJobs.add(streamingDistributionService.retract(channel, mp, removeStreamingElementIds));
    }

    // wait for retract jobs
    if (!waitForJobs(job, serviceRegistry, retractJobs).isSuccess()) {
      throw new PublicationException(format("Unable to retract OAI-PMH distributions of media package %s",
          mp.getIdentifier().toString()));
    }

    return retractJobs.stream()
        .filter(j -> StringUtils.isNotBlank(j.getPayload()))
        .map(Job::getPayload)
        .flatMap(p -> MediaPackageElementParser.getArrayFromXmlUnchecked(p).stream())
        .collect(Collectors.toList());
  }

  private List<MediaPackageElement> retractSync(String channel, MediaPackage mp, Set<String> removeDownloadElementIds,
      Set<String> removeStreamingElementIds) throws DistributionException {
    final List<MediaPackageElement> retracted = new ArrayList<>();
    if (!removeDownloadElementIds.isEmpty()) {
      retracted.addAll(downloadDistributionService.retractSync(channel, mp, removeDownloadElementIds));
    }
    if (!removeStreamingElementIds.isEmpty()) {
      retracted.addAll(streamingDistributionService.retractSync(channel, mp, removeStreamingElementIds));
    }
    return retracted;
  }

  private List<MediaPackageElement> distribute(
      Job job, String channel, MediaPackage mp, Set<String> addDownloadElementIds,
      Set<String> addStreamingElementIds, boolean checkAvailable) throws PublicationException, MediaPackageException,
      DistributionException {
    final List<Job> distributeJobs = new ArrayList<>(2);
    if (!addDownloadElementIds.isEmpty()) {
      distributeJobs.add(downloadDistributionService.distribute(channel, mp, addDownloadElementIds,
          checkAvailable));
    }
    if (!addStreamingElementIds.isEmpty()) {
      distributeJobs.add(streamingDistributionService.distribute(channel, mp, addStreamingElementIds));
    }

    // wait for distribute jobs
    if (!waitForJobs(job, serviceRegistry, distributeJobs).isSuccess()) {
      throw new PublicationException(format("Unable to distribute OAI-PMH distributions of media package %s",
          mp.getIdentifier().toString()));
    }

    return distributeJobs.stream()
        .filter(j -> StringUtils.isNotBlank(j.getPayload()))
        .map(Job::getPayload)
        .flatMap(p -> MediaPackageElementParser.getArrayFromXmlUnchecked(p).stream())
        .collect(Collectors.toList());
  }

  private List<MediaPackageElement> distributeSync(
      String channel, MediaPackage mp, Set<String> addDownloadElementIds, Set<String> addStreamingElementIds,
      boolean checkAvailable) throws DistributionException {
    final List<MediaPackageElement> distributed = new ArrayList<>();
    if (!addDownloadElementIds.isEmpty()) {
      distributed.addAll(downloadDistributionService.distributeSync(channel, mp, addDownloadElementIds,
          checkAvailable));
    }
    if (!addStreamingElementIds.isEmpty()) {
      distributed.addAll(streamingDistributionService.distributeSync(channel, mp, addStreamingElementIds));
    }
    return distributed;
  }


  protected Publication retract(Job job, MediaPackage mediaPackage, String repository)
          throws PublicationException, NotFoundException {
    String mpId = mediaPackage.getIdentifier().compact();

    // track elements for retraction
    MediaPackage oaiPmhMp = null;
    SearchResult searchResult = oaiPmhDatabase.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
            .isDeleted(false).build());
    for (SearchResultItem searchResultItem : searchResult.getItems()) {
      if (oaiPmhMp == null) {
        oaiPmhMp = searchResultItem.getMediaPackage();
      } else {
        for (MediaPackageElement mpe : searchResultItem.getMediaPackage().getElements()) {
          oaiPmhMp.add(mpe);
        }
      }
    }

    // retract oai-pmh
    try {
      oaiPmhDatabase.delete(mpId, repository);
    } catch (OaiPmhDatabaseException e) {
      throw new PublicationException(format("Unable to retract media package %s from OAI-PMH repository %s",
              mpId, repository), e);
    } catch (NotFoundException e) {
      logger.debug(format("Skip retracting media package %s from OIA-PMH repository %s as it isn't published.",
              mpId, repository), e);
    }

    if (oaiPmhMp != null && oaiPmhMp.getElements().length > 0) {
      // retract files from distribution channels
      Set<String> mpeIds = new HashSet<>();
      for (MediaPackageElement mpe : oaiPmhMp.elements()) {
        if (MediaPackageElement.Type.Publication == mpe.getElementType())
          continue;

        mpeIds.add(mpe.getIdentifier());
      }
      if (!mpeIds.isEmpty()) {
        List<Job> retractionJobs = new ArrayList<>();
        // retract download
        try {
          Job retractDownloadJob = downloadDistributionService
                  .retract(getPublicationChannelName(repository), oaiPmhMp, mpeIds);
          if (retractDownloadJob != null) {
            retractionJobs.add(retractDownloadJob);
          }
        } catch (DistributionException e) {
          throw new PublicationException(format("Unable to create retraction job from distribution channel download for the media package %s ",
                  mpId), e);
        }

        // retract streaming
        try {
          Job retractDownloadJob = streamingDistributionService
                  .retract(getPublicationChannelName(repository), oaiPmhMp, mpeIds);
          if (retractDownloadJob != null) {
            retractionJobs.add(retractDownloadJob);
          }
        } catch (DistributionException e) {
          throw new PublicationException(format("Unable to create retraction job from distribution channel streaming for the media package %s ",
                  mpId), e);
        }
        if (retractionJobs.size() > 0) {
          // wait for distribution jobs
          if (!waitForJobs(job, serviceRegistry, retractionJobs).isSuccess())
            throw new PublicationException(
                    format("Unable to retract elements of media package %s from distribution channels.", mpId));
        }
      }
    }

    String publicationChannel = getPublicationChannelName(repository);
    for (Publication p : mediaPackage.getPublications()) {
      if (StringUtils.equals(publicationChannel, p.getChannel()))
        return p;
    }
    return null;
  }

  protected Publication updateMetadata(Job job, MediaPackage mediaPackage, String repository, Set<String> flavors, Set<String> tags,
          boolean checkAvailability) throws PublicationException {
    final Set<MediaPackageElementFlavor> parsedFlavors = new HashSet<>();
    for (String flavor : flavors) {
      parsedFlavors.add(MediaPackageElementFlavor.parseFlavor(flavor));
    }

    final MediaPackage filteredMp;
    final SearchResult result = oaiPmhDatabase.search(QueryBuilder.queryRepo(repository).mediaPackageId(mediaPackage)
            .isDeleted(false).build());
    if (result.size() == 1) {
      // apply tags and flavors to the current media package
      try {
        logger.debug("filter elements with flavors {} and tags {} on media package {}",
                StringUtils.join(flavors, ", "), StringUtils.join(tags, ", "),
                MediaPackageParser.getAsXml(mediaPackage));

        filteredMp = filterMediaPackage(mediaPackage, parsedFlavors, tags);
      } catch (MediaPackageException e) {
        throw new PublicationException("Error filtering media package", e);
      }
    } else if (result.size() == 0) {
      logger.info(format("Skipping update of media package %s since it is not currently published to %s",
              mediaPackage, repository));
      return null;
    } else {
      final String msg = format("More than one media package with id %s found", mediaPackage.getIdentifier().compact());
      logger.warn(msg);
      throw new PublicationException(msg);
    }
    // re-distribute elements to download
    Set<String> elementIdsToDistribute = new HashSet<>();
    for (MediaPackageElement mpe : filteredMp.getElements()) {
      // do not distribute publications
      if (MediaPackageElement.Type.Publication == mpe.getElementType())
        continue;
      elementIdsToDistribute.add(mpe.getIdentifier());
    }
    if (elementIdsToDistribute.isEmpty()) {
      logger.debug("The media package {} does not contain any elements to update. "
                      + "Skip OAI-PMH metadata update operation for repository {}",
              mediaPackage.getIdentifier().compact(), repository);
      return null;
    }
    logger.debug("distribute elements {}", StringUtils.join(elementIdsToDistribute, ", "));
    final List<MediaPackageElement> distributedElements = new ArrayList<>();
    try {
      Job distJob = downloadDistributionService
              .distribute(getPublicationChannelName(repository), filteredMp, elementIdsToDistribute, checkAvailability);
      if (job == null)
        throw new PublicationException("The distribution service can not handle this type of media package elements.");
      if (!waitForJobs(job, serviceRegistry, distJob).isSuccess()) {
        throw new PublicationException(format(
                "Unable to distribute updated elements from media package %s to the download distribution service",
                mediaPackage.getIdentifier().compact()));
      }
      if (distJob.getPayload() != null) {
        for (MediaPackageElement mpe : MediaPackageElementParser.getArrayFromXml(distJob.getPayload())) {
          distributedElements.add(mpe);
        }
      }
    } catch (DistributionException | MediaPackageException e) {
      throw new PublicationException(format(
              "Unable to distribute updated elements from media package %s to the download distribution service",
              mediaPackage.getIdentifier().compact()), e);
    }

    // update elements (URLs)
    for (MediaPackageElement e : filteredMp.getElements()) {
      if (MediaPackageElement.Type.Publication.equals(e.getElementType()))
        continue;
      filteredMp.remove(e);
    }
    for (MediaPackageElement e : distributedElements) {
      filteredMp.add(e);
    }
    MediaPackage publishedMp = merge(filteredMp, removeMatchingNonExistantElements(filteredMp,
            (MediaPackage) result.getItems().get(0).getMediaPackage().clone(), parsedFlavors, tags));
    // Does the media package have a title and track?
    if (!MediaPackageSupport.isPublishable(publishedMp)) {
      throw new PublicationException("Media package does not meet criteria for publication");
    }
    // Publish the media package to OAI-PMH
    try {
      logger.debug(format("Updating metadata of media package %s in %s",
              publishedMp.getIdentifier().compact(), repository));
      oaiPmhDatabase.store(publishedMp, repository);
    } catch (OaiPmhDatabaseException e) {
      throw new PublicationException(format("Media package %s could not be updated",
              publishedMp.getIdentifier().compact()));
    }
    // retract orphaned elements from download distribution
    // orphaned elements are all those elements to which the updated media package no longer refers (in terms of element uri)
    Map<URI, MediaPackageElement> elementUriMap = new Hashtable<>();
    for (SearchResultItem oaiPmhSearchResultItem : result.getItems()) {
      for (MediaPackageElement mpe : oaiPmhSearchResultItem.getMediaPackage().getElements()) {
        if (MediaPackageElement.Type.Publication == mpe.getElementType() || null == mpe.getURI())
          continue;
        elementUriMap.put(mpe.getURI(), mpe);
      }
    }
    for (MediaPackageElement publishedMpe : publishedMp.getElements()) {
      if (MediaPackageElement.Type.Publication == publishedMpe.getElementType())
        continue;
      if (elementUriMap.containsKey(publishedMpe.getURI()))
        elementUriMap.remove(publishedMpe.getURI());
    }
    Set<String> orphanedElementIds = new HashSet<>();
    for (MediaPackageElement orphanedMpe : elementUriMap.values()) {
      orphanedElementIds.add(orphanedMpe.getIdentifier());
    }
    if (!orphanedElementIds.isEmpty()) {
      for (SearchResultItem oaiPmhSearchResultItem : result.getItems()) {
        try {
          Job retractJob = downloadDistributionService.retract(getPublicationChannelName(repository),
                  oaiPmhSearchResultItem.getMediaPackage(), orphanedElementIds);
          if (retractJob != null) {
            if (!waitForJobs(job, serviceRegistry, retractJob).isSuccess())
              logger.warn("The download distribution retract job for the orphaned elements from media package {} does not end successfully",
                      oaiPmhSearchResultItem.getMediaPackage().getIdentifier().compact());
          }
        } catch (DistributionException e) {
          logger.warn("Unable to retract orphaned elements from download distribution service for the media package {} channel {}",
                  oaiPmhSearchResultItem.getMediaPackage().getIdentifier().compact(), getPublicationChannelName(repository), e);
        }
      }
    }

    // return the publication
    String publicationChannel = getPublicationChannelName(repository);
    for (Publication p : mediaPackage.getPublications()) {
      if (StringUtils.equals(publicationChannel, p.getChannel()))
        return p;
    }
    return null;
  }

  protected void checkInputArguments(MediaPackage mediaPackage, String repository) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Media package must be specified");
    if (StringUtils.isEmpty(repository))
      throw new IllegalArgumentException("Repository must be specified");
    if (!oaiPmhServerInfo.hasRepo(repository))
      throw new IllegalArgumentException("OAI-PMH repository '" + repository + "' does not exist");
  }

  protected String getPublicationChannelName(String repository) {
    return PUBLICATION_CHANNEL_PREFIX.concat(repository);
  }

  /** Create a new publication element. */
  protected Publication createPublicationElement(String mpId, String repository) throws PublicationException {
    for (String hostUrl : OaiPmhServerInfoUtil.oaiPmhServerUrlOfCurrentOrganization(securityService)) {
      final URI engageUri = URIUtils.resolve(
              URI.create(UrlSupport.concat(hostUrl, oaiPmhServerInfo.getMountPoint(), repository)),
              "?verb=ListMetadataFormats&identifier=" + mpId);
      return PublicationImpl.publication(UUID.randomUUID().toString(), getPublicationChannelName(repository), engageUri,
              MimeTypes.parseMimeType(MimeTypes.XML.toString()));
    }
    // no host URL
    final String msg = format("No host url for oai-pmh server configured for organization %s " + "("
            + OaiPmhServerInfoUtil.ORG_CFG_OAIPMH_SERVER_HOSTURL + ")", securityService.getOrganization().getId());
    throw new PublicationException(msg);
  }

  private static MediaPackage updateMediaPackageFields(MediaPackage oaiPmhMp, MediaPackage mediaPackage) {
    oaiPmhMp.setTitle(mediaPackage.getTitle());
    oaiPmhMp.setDate(mediaPackage.getDate());
    oaiPmhMp.setLanguage(mediaPackage.getLanguage());
    oaiPmhMp.setLicense(mediaPackage.getLicense());
    oaiPmhMp.setSeries(mediaPackage.getSeries());
    oaiPmhMp.setSeriesTitle(mediaPackage.getSeriesTitle());
    for (String contributor : oaiPmhMp.getContributors())
      oaiPmhMp.removeContributor(contributor);
    for (String contributor : mediaPackage.getContributors())
      oaiPmhMp.addContributor(contributor);
    for (String creator : oaiPmhMp.getCreators())
      oaiPmhMp.removeCreator(creator);
    for (String creator : mediaPackage.getCreators())
      oaiPmhMp.addCreator(creator);
    for (String subject : oaiPmhMp.getSubjects())
      oaiPmhMp.removeSubject(subject);
    for (String subject : mediaPackage.getSubjects())
      oaiPmhMp.addSubject(subject);
    return oaiPmhMp;
  }

  /**
   * Creates a clone of the media package and removes those elements that do not match the flavor and tags filter
   * criteria.
   *
   * @param mediaPackage
   *          the media package
   * @param flavors
   *          the flavors
   * @param tags
   *          the tags
   * @return the filtered media package
   */
  private MediaPackage filterMediaPackage(MediaPackage mediaPackage, Set<MediaPackageElementFlavor> flavors,
          Set<String> tags) throws MediaPackageException {
    if (flavors.isEmpty() && tags.isEmpty())
      throw new IllegalArgumentException("Flavors or tags parameter must be set");

    MediaPackage filteredMediaPackage = (MediaPackage) mediaPackage.clone();

    // The list of elements to keep
    List<MediaPackageElement> keep = new ArrayList<>();

    SimpleElementSelector selector = new SimpleElementSelector();
    // Filter elements
    for (MediaPackageElementFlavor flavor : flavors) {
      selector.addFlavor(flavor);
    }
    for (String tag : tags) {
      selector.addTag(tag);
    }
    keep.addAll(selector.select(mediaPackage, true));

    // Keep publications
    for (Publication p : filteredMediaPackage.getPublications())
      keep.add(p);

    // Fix references and flavors
    for (MediaPackageElement element : filteredMediaPackage.getElements()) {

      if (!keep.contains(element)) {
        logger.debug("Removing {} '{}' from media package '{}'", element.getElementType().toString().toLowerCase(),
                element.getIdentifier(), filteredMediaPackage.getIdentifier().toString());
        filteredMediaPackage.remove(element);
        continue;
      }

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        Map<String, String> referenceProperties = reference.getProperties();
        MediaPackageElement referencedElement = mediaPackage.getElementByReference(reference);

        // if we are distributing the referenced element, everything is fine. Otherwise...
        if (referencedElement != null && !keep.contains(referencedElement)) {

          // Follow the references until we find a flavor
          MediaPackageElement parent = null;
          while ((parent = mediaPackage.getElementByReference(reference)) != null) {
            if (parent.getFlavor() != null && element.getFlavor() == null) {
              element.setFlavor(parent.getFlavor());
            }
            if (parent.getReference() == null)
              break;
            reference = parent.getReference();
          }

          // Done. Let's cut the path but keep references to the mediapackage itself
          if (reference != null && reference.getType().equals(MediaPackageReference.TYPE_MEDIAPACKAGE))
            element.setReference(reference);
          else if (reference != null && (referenceProperties == null || referenceProperties.size() == 0))
            element.clearReference();
          else {
            // Ok, there is more to that reference than just pointing at an element. Let's keep the original,
            // you never know.
            referencedElement.setURI(null);
            referencedElement.setChecksum(null);
          }
        }
      }
    }

    return filteredMediaPackage;
  }

  /**
   * Remove all these elements from {@code publishedMp}, that matches the given flavors and tags
   * but are not in the {@code updatedMp}.
   *
   * @param updatedMp the updated media package
   * @param publishedMp the media package that is currently published
   * @param flavors flavors of elements to update
   * @param tags tags of elements to update
   * @return published media package without elements, that matches the flavors and tags
   *     but are not in the updated media package
   */
  public static MediaPackage removeMatchingNonExistantElements(MediaPackage updatedMp, MediaPackage publishedMp,
          Set<MediaPackageElementFlavor> flavors, Set<String> tags) {
    SimpleElementSelector selector = new SimpleElementSelector();
    // Filter elements
    for (MediaPackageElementFlavor flavor : flavors) {
      selector.addFlavor(flavor);
    }
    for (String tag : tags) {
      selector.addTag(tag);
    }
    for (MediaPackageElement publishedMpe : selector.select(publishedMp, true)) {
      boolean foundInUpdatedMp = false;
      for (MediaPackageElement updatedMpe : updatedMp.getElementsByFlavor(publishedMpe.getFlavor())) {
        if (!updatedMpe.containsTag(tags)) {
          // todo: this case shouldn't happen!
        }
        foundInUpdatedMp = true;
        break;
      }

      if (!foundInUpdatedMp) {
        publishedMp.remove(publishedMpe);
      }
    }
    return publishedMp;
  }

  /**
   * Merges the updated media package with the one that is currently published in a way where the updated elements
   * replace existing ones in the published media package based on their flavor.
   * <p>
   * If <code>publishedMp</code> is <code>null</code>, this method returns the updated media package without any
   * modifications.
   *
   * @param updatedMp
   *          the updated media package
   * @param publishedMp
   *          the media package that is currently published
   * @return the merged media package
   */
  public static MediaPackage merge(MediaPackage updatedMp, MediaPackage publishedMp) {
    if (publishedMp == null)
      return updatedMp;

    final MediaPackage mergedMp = MediaPackageSupport.copy(publishedMp);

    // Merge the elements
    for (final MediaPackageElement updatedElement : updatedMp.elements()) {
      for (final MediaPackageElementFlavor flavor : Opt.nul(updatedElement.getFlavor())) {
        for (final MediaPackageElement outdated : mergedMp.getElementsByFlavor(flavor)) {
          mergedMp.remove(outdated);
        }
        logger.debug(format("Update %s %s of type %s", updatedElement.getElementType().toString().toLowerCase(),
                updatedElement.getIdentifier(), updatedElement.getElementType()));
        mergedMp.add(updatedElement);
      }
    }

    // Remove publications
    for (final Publication p : mergedMp.getPublications())
      mergedMp.remove(p);

    // Add updated publications
    for (final Publication updatedPublication : updatedMp.getPublications())
      mergedMp.add(updatedPublication);

    // Merge media package fields
    updateMediaPackageFields(mergedMp, updatedMp);
    return mergedMp;
  }

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

  /** OSGI DI */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** OSGI DI */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGI DI */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /** OSGI DI */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGI DI */
  public void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    this.downloadDistributionService = downloadDistributionService;
  }

  /** OSGI DI */
  public void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    this.streamingDistributionService = streamingDistributionService;
  }

  /** OSGI DI */
  public void setOaiPmhServerInfo(OaiPmhServerInfo oaiPmhServerInfo) {
    this.oaiPmhServerInfo = oaiPmhServerInfo;
  }

  /** OSGI DI */
  public void setOaiPmhDatabase(OaiPmhDatabase oaiPmhDatabase) {
    this.oaiPmhDatabase = oaiPmhDatabase;
  }
}
