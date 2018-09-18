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
package org.opencastproject.publication.configurable;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.publication.api.ConfigurablePublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.JobUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ConfigurablePublicationServiceImpl extends AbstractJobProducer implements ConfigurablePublicationService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurablePublicationServiceImpl.class);

  /* Gson is thread-safe so we use a single instance */
  private Gson gson = new Gson();

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
  }

  public ConfigurablePublicationServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public String getJobType() {
    return super.getJobType();
  }

  public enum Operation {
    Replace
  }

  private DownloadDistributionService distributionService;

  private SecurityService securityService;

  private UserDirectoryService userDirectoryService;

  private OrganizationDirectoryService organizationDirectoryService;

  private ServiceRegistry serviceRegistry;

  public void setDownloadDistributionService(final DownloadDistributionService distributionService) {
    this.distributionService = distributionService;
  }

  public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return this.securityService;
  }

  public void setSecurityService(final SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setUserDirectoryService(final UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public void setOrganizationDirectoryService(final OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return this.userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return this.organizationDirectoryService;
  }

  @Override
  public Job replace(final MediaPackage mediaPackage, final String channelId,
          final Collection<? extends MediaPackageElement> addElements, final Set<String> retractElementIds)
          throws PublicationException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Replace.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), channelId,
                      MediaPackageElementParser.getArrayAsXml(addElements), gson.toJson(retractElementIds)));
    } catch (final ServiceRegistryException e) {
      throw new PublicationException("Unable to create job", e);
    }
  }

  @Override
  public Publication replaceSync(
      MediaPackage mediaPackage, String channelId, Collection<? extends MediaPackageElement> addElements,
      Set<String> retractElementIds) throws PublicationException, MediaPackageException {
    try {
      return doReplaceSync(mediaPackage, channelId, addElements, retractElementIds);
    } catch (DistributionException e) {
      throw new PublicationException(e);
    }
  }

  @Override
  protected String process(final Job job) throws Exception {
    final List<String> arguments = job.getArguments();
    final MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
    final String channelId = arguments.get(1);
    final Collection<? extends MediaPackageElement> addElements = MediaPackageElementParser
            .getArrayFromXml(arguments.get(2));
    Set<String> retractElementIds = gson.fromJson(arguments.get(3), new TypeToken<Set<String>>() { }.getType());

    Publication result = null;
    switch (Operation.valueOf(job.getOperation())) {
      case Replace:
        result = doReplace(mediaPackage, channelId, addElements, retractElementIds);
        break;
      default:
        break;
    }
    if (result != null) {
      return MediaPackageElementParser.getAsXml(result);
    } else {
      return null;
    }
  }

  private void distributeMany(final MediaPackage mp, final String channelId,
          final Collection<? extends MediaPackageElement> elements)
          throws DistributionException, MediaPackageException {

    final Optional<Publication> publicationOpt = getPublication(mp, channelId);

    if (publicationOpt.isPresent()) {

      final Publication publication = publicationOpt.get();

      // Add all the elements top-level so the distribution service knows what to do
      elements.forEach(mp::add);

      Set<String> elementIds = new HashSet<>();
      for (final MediaPackageElement mpe : elements) {
        elementIds.add(mpe.getIdentifier());
      }

      try {
        Job job = distributionService.distribute(channelId, mp, elementIds, false);

        if (!JobUtil.waitForJob(serviceRegistry, job).isSuccess()) {
          throw new DistributionException("At least one of the publication jobs did not complete successfully");
        }
        List<? extends MediaPackageElement> distributedElements = MediaPackageElementParser.getArrayFromXml(job.getPayload());
        for (MediaPackageElement mpe : distributedElements) {
           PublicationImpl.addElementToPublication(publication, mpe);
        }
      } finally {
        // Remove our changes
        elements.stream().map(MediaPackageElement::getIdentifier).forEach(mp::removeElementById);
      }
    }
  }

  private void distributeManySync(final MediaPackage mp, final String channelId,
          final Collection<? extends MediaPackageElement> elements) throws DistributionException {

    final Optional<Publication> publicationOpt = getPublication(mp, channelId);

    if (publicationOpt.isPresent()) {

      final Publication publication = publicationOpt.get();

      // Add all the elements top-level so the distribution service knows what to do
      elements.forEach(mp::add);

      Set<String> elementIds = new HashSet<>();
      for (final MediaPackageElement mpe : elements) {
        elementIds.add(mpe.getIdentifier());
      }

      try {
        List<? extends MediaPackageElement> distributedElements = distributionService.distributeSync(channelId, mp,
            elementIds, false);
        for (MediaPackageElement mpe : distributedElements) {
          PublicationImpl.addElementToPublication(publication, mpe);
        }
      } finally {
        // Remove our changes
        elements.stream().map(MediaPackageElement::getIdentifier).forEach(mp::removeElementById);
      }
    }
  }

  private Publication doReplace(final MediaPackage mp, final String channelId,
          final Collection<? extends MediaPackageElement> addElementIds, final Set<String> retractElementIds)
          throws DistributionException, MediaPackageException {
    // Retract old elements
    final Job retractJob = distributionService.retract(channelId, mp, retractElementIds);

    if (!JobUtil.waitForJobs(serviceRegistry, retractJob).isSuccess()) {
      throw new DistributionException("At least one of the retraction jobs did not complete successfully");
    }

    final Optional<Publication> priorPublication = getPublication(mp, channelId);

    final Publication publication;

    if (priorPublication.isPresent()) {
      publication = priorPublication.get();
    } else {
      final String publicationUUID = UUID.randomUUID().toString();
      publication = PublicationImpl.publication(publicationUUID, channelId, null, null);
      mp.add(publication);
    }

    retractElementIds.forEach(publication::removeAttachmentById);

    distributeMany(mp, channelId, addElementIds);

    return publication;
  }

  private Publication doReplaceSync(final MediaPackage mp, final String channelId,
          final Collection<? extends MediaPackageElement> addElementIds, final Set<String> retractElementIds)
          throws DistributionException {
    // Retract old elements
    distributionService.retractSync(channelId, mp, retractElementIds);

    final Optional<Publication> priorPublication = getPublication(mp, channelId);

    final Publication publication;

    if (priorPublication.isPresent()) {
      publication = priorPublication.get();
    } else {
      final String publicationUUID = UUID.randomUUID().toString();
      publication = PublicationImpl.publication(publicationUUID, channelId, null, null);
      mp.add(publication);
    }

    retractElementIds.forEach(publication::removeAttachmentById);

    distributeManySync(mp, channelId, addElementIds);

    return publication;
  }

  private Optional<Publication> getPublication(final MediaPackage mp, final String channelId) {
    return Arrays.stream(mp.getPublications()).filter(p -> p.getChannel().equalsIgnoreCase(channelId)).findAny();
  }
}
