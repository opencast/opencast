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
package org.opencastproject.distribution.streaming;

import static java.lang.String.format;
import static org.opencastproject.util.PathSupport.path;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * Distributes media to the local media delivery directory.
 */
public class StreamingDistributionService extends AbstractJobProducer implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionService.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.streaming";

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator;

  /** The workspace reference */
  protected Workspace workspace = null;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The distribution directory */
  protected File distributionDirectory = null;

  /** The base URL for streaming */
  protected String streamingUrl = null;

  /**
   * Creates a new instance of the streaming distribution service.
   */
  public StreamingDistributionService() {
    super(JOB_TYPE);
  }

  protected void activate(ComponentContext cc) {
    // Get the configured streaming and server URLs
    if (cc != null) {
      streamingUrl = StringUtils.trimToNull(cc.getBundleContext().getProperty("org.opencastproject.streaming.url"));
      if (streamingUrl == null)
        logger.warn("Stream url was not set (org.opencastproject.streaming.url)");
      else
        logger.info("streaming url is {}", streamingUrl);

      String distributionDirectoryPath = StringUtils.trimToNull(cc.getBundleContext().getProperty(
              "org.opencastproject.streaming.directory"));
      if (distributionDirectoryPath == null)
        logger.warn("Streaming distribution directory must be set (org.opencastproject.streaming.directory)");
      else {
        distributionDirectory = new File(distributionDirectoryPath);
        if (!distributionDirectory.isDirectory()) {
          try {
            FileUtils.forceMkdir(distributionDirectory);
          } catch (IOException e) {
            throw new IllegalStateException("Distribution directory does not exist and can't be created", e);
          }
        }
      }

      logger.info("Streaming distribution directory is {}", distributionDirectory);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {
    if (mediapackage == null)
      throw new MediaPackageException("Mediapackage must be specified");
    if (elementId == null)
      throw new MediaPackageException("Element ID must be specified");
    if (channelId == null)
      throw new MediaPackageException("Channel ID must be specified");

    if (StringUtils.isBlank(streamingUrl))
      throw new IllegalStateException("Stream url must be set (org.opencastproject.streaming.url)");
    if (distributionDirectory == null)
      throw new IllegalStateException(
              "Streaming distribution directory must be set (org.opencastproject.streaming.directory)");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Distribute a Mediapackage element to the download distribution service.
   * 
   * @param mediapackage
   *          The media package that contains the element to distribute.
   * @param elementId
   *          The id of the element that should be distributed contained within the media package.
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement distributeElement(String channelId, final MediaPackage mediapackage, String elementId)
          throws DistributionException {
    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");

    final MediaPackageElement element = mediapackage.getElementById(elementId);

    // Streaming servers only deal with tracks
    if (!MediaPackageElement.Type.Track.equals(element.getElementType())) {
      logger.debug("Skipping {} {} for distribution to the streaming server", element.getElementType().toString()
              .toLowerCase(), element.getIdentifier());
      return null;
    }

    // Make sure the element exists
    if (mediapackage.getElementById(elementId) == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");

    try {
      File source;
      try {
        source = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        throw new DistributionException("Unable to find " + element.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new DistributionException("Error loading " + element.getURI() + " from the workspace", e);
      }
      File destination = getDistributionFile(channelId, mediapackage, element);

      // Put the file in place
      try {
        FileUtils.forceMkdir(destination.getParentFile());
      } catch (IOException e) {
        throw new DistributionException("Unable to create " + destination.getParentFile(), e);
      }
      logger.info("Distributing {} to {}", elementId, destination);

      try {
        FileSupport.link(source, destination, true);
      } catch (IOException e) {
        throw new DistributionException("Unable to copy " + source + " to " + destination, e);
      }

      // Create a representation of the distributed file in the mediapackage
      final MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      try {
        distributedElement.setURI(getDistributionUri(channelId, mediapackage, element));
      } catch (URISyntaxException e) {
        throw new DistributionException("Distributed element produces an invalid URI", e);
      }
      distributedElement.setIdentifier(null);

      logger.info("Finished distribution of {}", element);
      return distributedElement;

    } catch (Exception e) {
      logger.warn("Error distributing " + element, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(String,
   *      org.opencastproject.mediapackage.MediaPackage, String) java.lang.String)
   */
  @Override
  public Job retract(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");

    if (distributionDirectory == null)
      throw new IllegalStateException(
              "Streaming distribution directory must be set (org.opencastproject.streaming.directory)");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Retracts the mediapackage with the given identifier from the distribution channel.
   * 
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param elementId
   *          the element identifier
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  protected MediaPackageElement retractElement(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException {

    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

    // Make sure the element exists
    final MediaPackageElement element = mediapackage.getElementById(elementId);
    if (element == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");

    // Find the element that has been created as part of the distribution process
    final URI distributedURI;
    MediaPackageElement distributedElement = null;
    try {
      distributedURI = getDistributionUri(channelId, mediapackage, element);
      for (MediaPackageElement e : mediapackage.getElements()) {
        if (distributedURI.equals(e.getURI())) {
          distributedElement = e;
          break;
        }
      }
    } catch (URISyntaxException e) {
      throw new DistributionException("Retracted element produces an invalid URI", e);
    }

    // Has this element been distributed?
    if (distributedElement == null)
      return null;

    try {
      final File elementFile = getDistributionFile(channelId, mediapackage, element);
      final File mediapackageDir = getMediaPackageDirectory(channelId, mediapackage);

      // Does the file exist? If not, the current element has not been distributed to this channel
      // or has been removed otherwise
      if (!elementFile.exists())
        return distributedElement;

      // Try to remove the file and - if possible - the parent folder
      FileUtils.forceDelete(elementFile);
      if (mediapackageDir.list().length == 0) {
        FileSupport.delete(mediapackageDir.getParentFile());
      }

      logger.info("Finished rectracting element {} of media package {}", elementId, mediapackage);

      return distributedElement;
    } catch (Exception e) {
      logger.warn("Error retracting element " + elementId + " of mediapackage " + mediapackage, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }

  }

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   * 
   * @return The file to copy the content to
   */
  protected File getDistributionFile(String channelId, MediaPackage mp, MediaPackageElement element) {
    final String uriString = element.getURI().toString();
    final String directoryName = distributionDirectory.getAbsolutePath();
    if (uriString.startsWith(streamingUrl)) {
      String[] splitUrl = uriString.substring(streamingUrl.length() + 1).split("/");
      if (splitUrl.length < 4) {
        logger.warn(format(
                "Malformed URI %s. Must be of format .../{channelId}/{mediapackageId}/{elementId}/{fileName}."
                        + " Trying URI without channelId", uriString));
        return new File(path(directoryName, splitUrl[0], splitUrl[1], splitUrl[2]));
      } else {
        return new File(path(directoryName, splitUrl[0], splitUrl[1], splitUrl[2], splitUrl[3]));
      }
    }
    return new File(path(directoryName, channelId, mp.getIdentifier().compact(), element.getIdentifier(),
            FilenameUtils.getName(uriString)));
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   * 
   * @return the filesystem directory
   */
  protected File getMediaPackageDirectory(String channelId, MediaPackage mediaPackage) {
    return new File(distributionDirectory, path(channelId, mediaPackage.getIdentifier().compact()));
  }

  /**
   * Gets the URI for the element to be distributed.
   * 
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String channelId, MediaPackage mp, MediaPackageElement element)
          throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getBaseName(element.getURI().toString());
    String tag = FilenameUtils.getExtension(element.getURI().toString()) + ":";

    // removes the tag for flv files, but keeps it for all others (mp4 needs it)
    if ("flv:".equals(tag))
      tag = "";

    return new URI(UrlSupport.concat(streamingUrl, tag + channelId, mp.getIdentifier().compact(), elementId, fileName));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      String channelId = arguments.get(0);
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(1));
      String elementId = arguments.get(2);
      switch (op) {
        case Distribute:
          MediaPackageElement distributedElement = distributeElement(channelId, mediapackage, elementId);
          return (distributedElement != null) ? MediaPackageElementParser.getAsXml(distributedElement) : null;
        case Retract:
          MediaPackageElement retractedElement = retractElement(channelId, mediapackage, elementId);
          return (retractedElement != null) ? MediaPackageElementParser.getAsXml(retractedElement) : null;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Callback for the OSGi environment to set the workspace reference.
   * 
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
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
