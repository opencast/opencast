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

package org.opencastproject.ingestdownloadservice.impl;

import org.opencastproject.ingestdownloadservice.api.IngestDownloadService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A simple tutorial class to learn about Opencast Services
 */
public class IngestDownloadServiceImpl extends AbstractJobProducer implements IngestDownloadService {

  public enum Operation {
    Download
  }

  /**
   * The module specific logger
   */
  private static final Logger logger = LoggerFactory.getLogger(IngestDownloadServiceImpl.class);

  /**
   * Reference to the receipt service
   */
  private ServiceRegistry serviceRegistry = null;

  /**
   * The security service
   */
  private SecurityService securityService = null;

  /**
   * The user directory service
   */
  private UserDirectoryService userDirectoryService = null;

  /**
   * The organization directory service
   */
  private OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * The workspace service
   */
  private Workspace workspace;

  /**
   * The http client to use when connecting to remote servers
   */
  private TrustedHttpClient client = null;

  /**
   * Creates a new abstract job producer for jobs of the given type.
   *
   */
  public IngestDownloadServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Sets the workspace to use.
   *
   * @param workspace the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   *
   * @param serviceRegistry the service registry
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
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
   * Callback for setting the security service.
   *
   * @param securityService the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
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

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  @Override
  public Job ingestDownload(MediaPackage mediaPackage, String sourceFlavors, String sourceTags, boolean deleteExternal,
          boolean tagsAndFlavor) throws ServiceRegistryException {

    final List<String> paramList = new ArrayList<>(5);
    paramList.add(MediaPackageParser.getAsXml(mediaPackage));
    paramList.add(sourceFlavors);
    paramList.add(sourceTags);
    paramList.add(Boolean.toString(deleteExternal));
    paramList.add(Boolean.toString(tagsAndFlavor));

    return serviceRegistry.createJob(JOB_TYPE, Operation.Download.toString(), paramList);

  }

  @Override
  protected String process(Job job) throws MediaPackageException, IOException {
    final List<String> arguments = new ArrayList<>(job.getArguments());

    final MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
    final String sourceFlavors = arguments.get(1);
    final String sourceTags = arguments.get(2);
    final boolean deleteExternal = Boolean.parseBoolean(arguments.get(3));
    final boolean tagsAndFlavor = Boolean.parseBoolean(arguments.get(4));

    // building elementSelector with tags and flavors
    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector = new SimpleElementSelector();
    for (String tag : StringUtils.split(sourceTags, ", ")) {
      elementSelector.addTag(tag);
    }
    for (String flavor : StringUtils.split(sourceFlavors, ", ")) {
      elementSelector.addFlavor(flavor);
    }

    final String baseUrl = workspace.getBaseUri().toString();

    List<URI> externalUris = new ArrayList<>();
    for (MediaPackageElement element : elementSelector.select(mediaPackage, tagsAndFlavor)) {
      if (element.getURI() == null) {
        continue;
      }

      if (element.getElementType() == MediaPackageElement.Type.Publication) {
        logger.debug("Skipping publication {} from media package {}", element.getIdentifier(),
                     mediaPackage.getIdentifier());
        continue;
      }

      if (element.getURI().toString().startsWith(baseUrl)) {
        logger.info("Skipping already existing element {}", element.getURI());
        continue;
      }

      // Download the external URI
      File file;
      try {
        file = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        logger.warn("Unable to download the external element {}", element.getURI());
        continue;
      }

      // Put to working file repository and rewrite URI on element
      final URI originalUri = element.getURI();
      try (InputStream in = new FileInputStream(file)) {
        final String filename = FilenameUtils.getName(element.getURI().getPath());
        final URI uri = workspace.put(mediaPackage.getIdentifier().compact(), element.getIdentifier(), filename, in);
        element.setURI(uri);
      } finally {
        try {
          workspace.delete(originalUri);
        } catch (Exception e) {
          logger.warn("Unable to delete ingest-downloaded element {}", element.getURI(), e);
        }
      }

      logger.info("Downloaded the external element {}", originalUri);

      // Store original URI for deletion
      externalUris.add(originalUri);
    }

    if (!deleteExternal || externalUris.size() == 0)
      return MediaPackageParser.getAsXml(mediaPackage);

    // Find all external working file repository base Urls
    logger.debug("Assembling list of external working file repositories");
    List<String> externalWfrBaseUrls = new ArrayList<>();
    try {
      final String wfrServiceType = WorkingFileRepository.SERVICE_TYPE;
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByType(wfrServiceType)) {
        if (baseUrl.startsWith(reg.getHost())) {
          logger.trace("Skipping local working file repository");
          continue;
        }
        externalWfrBaseUrls.add(UrlSupport.concat(reg.getHost(), reg.getPath()));
      }
      logger.debug("{} external working file repositories found", externalWfrBaseUrls.size());
    } catch (ServiceRegistryException e) {
      logger.error("Unable to load WFR services from service registry", e);
    }

    // try deleting files from external working file reposities
    for (URI uri : externalUris) {

      String elementUri = uri.toString();

      // Delete external working file repository URI's
      Optional<String> wfrBaseUrl = externalWfrBaseUrls.parallelStream().filter(elementUri::startsWith).findAny();

      if (!wfrBaseUrl.isPresent()) {
        logger.debug("Unable to delete {}, no working file repository found for this URI", elementUri);
        continue;
      }

      final String deleteUrl;
      if (uri.getPath().startsWith(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX)) {
        deleteUrl = elementUri.substring(0, elementUri.lastIndexOf("/"));
      } else if (uri.getPath().startsWith(WorkingFileRepository.COLLECTION_PATH_PREFIX)) {
        deleteUrl = elementUri;
      } else {
        logger.info("Unable to handle working file repository URI {}", elementUri);
        continue;
      }
      HttpDelete delete = new HttpDelete(deleteUrl);

      HttpResponse response = null;
      try {
        response = client.execute(delete);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_NO_CONTENT || statusCode == HttpStatus.SC_OK) {
          logger.info("Successfully deleted external URI {}", delete.getURI());
        } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
          logger.debug("External URI {} has already been deleted", delete.getURI());
        } else {
          logger.warn("Unable to delete external URI {}, status code '{}' returned", delete.getURI(), statusCode);
        }
      } catch (TrustedHttpClientException e) {
        logger.warn("Unable to execute DELETE request on external URI {}", delete.getURI());
      } finally {
        client.close(response);
      }
    }

    return MediaPackageParser.getAsXml(mediaPackage);

  }
}

