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
package org.opencastproject.distribution.download;

import static java.lang.String.format;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * Distributes media to the local media delivery directory.
 */
public class DownloadDistributionServiceImpl extends AbstractJobProducer implements DistributionService,
        DownloadDistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  }

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.download";

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static";

  /** Timeout in millis for checking distributed file request */
  private static final long TIMEOUT = 10000L;

  /** Interval time in millis for checking distributed file request */
  private static final long INTERVAL = 300L;

  /** Path to the distribution directory */
  protected File distributionDirectory = null;

  /** this media download service's base URL */
  protected String serviceUrl = null;

  /** The remote service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The workspace reference */
  protected Workspace workspace = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The trusted HTTP client */
  private TrustedHttpClient trustedHttpClient;

  /**
   * Creates a new instance of the download distribution service.
   */
  public DownloadDistributionServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Activate method for this OSGi service implementation.
   * 
   * @param cc
   *          the OSGi component context
   */
  protected void activate(ComponentContext cc) {
    serviceUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
    if (serviceUrl == null)
      throw new IllegalStateException("Download url must be set (org.opencastproject.download.url)");

    String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    if (ccDistributionDirectory == null)
      throw new IllegalStateException("Distribution directory must be set (org.opencastproject.download.directory)");
    this.distributionDirectory = new File(ccDistributionDirectory);
    logger.info("Download distribution directory is {}", distributionDirectory);
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {
    return distribute(channelId, mediapackage, elementId, true);
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId, boolean checkAvailability)
          throws DistributionException, MediaPackageException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(
              JOB_TYPE,
              Operation.Distribute.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId,
                      Boolean.toString(checkAvailability)));
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
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement distributeElement(String channelId, MediaPackage mediapackage, String elementId,
          boolean checkAvailability) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");

    final String mediapackageId = mediapackage.getIdentifier().compact();
    final MediaPackageElement element = mediapackage.getElementById(elementId);

    // Make sure the element exists
    if (mediapackage.getElementById(elementId) == null)
      throw new IllegalStateException(format("No element %s found in mediapackage %s", elementId, mediapackageId));

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
      logger.info(format("Distributing %s@%s for publication channel %s to %s", elementId, mediapackageId, channelId,
              destination));

      try {
        FileSupport.link(source, destination, true);
      } catch (IOException e) {
        throw new DistributionException(format("Unable to copy %s tp %s", source, destination), e);
      }

      // Create a representation of the distributed file in the mediapackage
      MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      try {
        distributedElement.setURI(getDistributionUri(channelId, mediapackageId, element));
      } catch (URISyntaxException e) {
        throw new DistributionException("Distributed element produces an invalid URI", e);
      }
      distributedElement.setIdentifier(null);

      logger.info(format("Finished distributing element %s@%s for publication channel %s", elementId, mediapackageId,
              channelId));
      URI uri = distributedElement.getURI();
      long now = 0L;
      
      // Start itbwpdk
      // If the distribution channel is engage player
      // and the file is available locally
      // do check on file level for existence
      if ("engage-player".equals(channelId) && distributionDirectory.exists()) {
      
	  File xelement = null;
	  String buildpath = "";
	  boolean calc = false;
	  for (String t : uri.toString().split("/")) {
			if (calc) {
			buildpath = buildpath + "/" + t;
			}
			if ("static".equals(t)) {
				calc = true;
			}
	  }
	  xelement = new File(distributionDirectory.getPath().concat(buildpath));	      
      while (checkAvailability) {

      if (xelement.exists()) {
    	  logger.debug("Distributed file was created in download directory for engage player, " + xelement.getPath());  
        break;
      }
      if (now < TIMEOUT) {
        try {
          Thread.sleep(INTERVAL);
          now += INTERVAL;
          continue;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      logger.warn("Distributed file not created in download directory for engage player, " + xelement.getPath());
      throw new DistributionException("Distributed file not created, " + xelement.getPath());
      }
      
      } else {
      
      
        while (checkAvailability) {
        HttpResponse response = trustedHttpClient.execute(new HttpHead(uri));
        if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK)
          break;

        if (now < TIMEOUT) {
          try {
            Thread.sleep(INTERVAL);
            now += INTERVAL;
            continue;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        logger.warn("Status code of distributed file {}: {}", uri, response.getStatusLine().getStatusCode());
        throw new DistributionException("Unable to load distributed file " + uri.toString());
      }
      
      }
      
      
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

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
   * one given as parameter <code>elementId</code>. Instead, the element's distribution URI will be calculated. This way
   * you are able to retract elements by providing the "original" element here.
   * 
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param elementId
   *          the element identifier
   * @return the retracted element or <code>null</code> if the element was not retracted
   * @throws org.opencastproject.distribution.api.DistributionException
   *           in case of an error
   */
  protected MediaPackageElement retractElement(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");

    // Make sure the element exists
    MediaPackageElement element = mediapackage.getElementById(elementId);
    if (element == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");

    String mediapackageId = mediapackage.getIdentifier().compact();
    try {
      final File elementFile = getDistributionFile(channelId, mediapackage, element);
      final File mediapackageDir = getMediaPackageDirectory(channelId, mediapackage);
      // Does the file exist? If not, the current element has not been distributed to this channel
      // or has been removed otherwise
      if (!elementFile.exists()) {
        logger.info(format(
                "Element %s@%s has already been removed or has never been distributed for publication channel %s",
                elementId, mediapackageId, channelId));
        return element;
      }

      logger.info("Retracting element {} from {}", element, elementFile);

      // Try to remove the file and its parent folder representing the mediapackage element id
      FileUtils.forceDelete(elementFile.getParentFile());
      if (mediapackageDir.isDirectory() && mediapackageDir.list().length == 0)
        FileSupport.delete(mediapackageDir);

      logger.info(format("Finished retracting element %s@%s for publication channel %s", elementId, mediapackageId,
              channelId));
      return element;
    } catch (Exception e) {
      logger.warn(
              format("Error retracting element %s@%s for publication channel %s", elementId, mediapackageId, channelId),
              e);
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
          Boolean checkAvailability = Boolean.parseBoolean(arguments.get(3));
          MediaPackageElement distributedElement = distributeElement(channelId, mediapackage, elementId,
                  checkAvailability);
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
   * Gets the destination file to copy the contents of a mediapackage element.
   * 
   * @return The file to copy the content to
   */
  protected File getDistributionFile(String channelId, MediaPackage mp, MediaPackageElement element) {
    final String uriString = element.getURI().toString();
    final String directoryName = distributionDirectory.getAbsolutePath();
    if (uriString.startsWith(serviceUrl)) {
      String[] splitUrl = uriString.substring(serviceUrl.length() + 1).split("/");
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
  protected File getMediaPackageDirectory(String channelId, MediaPackage mp) {
    return new File(distributionDirectory, path(channelId, mp.getIdentifier().compact()));
  }

  /**
   * Gets the URI for the element to be distributed.
   * 
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param element
   *          The mediapackage element being distributed
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String channelId, String mediaPackageId, MediaPackageElement element)
          throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(serviceUrl, channelId, mediaPackageId, elementId, fileName);
    return new URI(destinationURI);
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
   * Callback for setting the trusted HTTP client.
   * 
   * @param trustedHttpClient
   *          the trusted HTTP client to set
   */
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
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
