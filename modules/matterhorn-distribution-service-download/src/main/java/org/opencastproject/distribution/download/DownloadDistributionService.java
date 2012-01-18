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

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
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
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Distributes media to the local media delivery directory.
 */
public class DownloadDistributionService extends AbstractJobProducer implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionService.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, DistributeWithAcl, Retract 
  };

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.download";

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static";

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

  /**
   * Creates a new instance of the download distribution service.
   */
  public DownloadDistributionService() {
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  public Job distribute(MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {

    if (mediapackage == null)
      throw new MediaPackageException("Mediapackage must be specified");
    if (elementId == null)
      throw new MediaPackageException("Element ID must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Distributes the mediapackage's element to the location that is returned by the concrete implementation. In
   * addition, a representation of the distributed element is added to the mediapackage.
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String, MediaPackageElement)
   */
  protected MediaPackageElement distribute(Job job, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException {
    return distributeElement(mediapackage, elementId);
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
  public MediaPackageElement distributeElement(MediaPackage mediapackage, String elementId) 
          throws DistributionException {
    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

    String mediaPackageId = mediapackage.getIdentifier().compact();
    MediaPackageElement element = mediapackage.getElementById(elementId);

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
      File destination = getDistributionFile(mediapackage, element);

      // Put the file in place
      try {
        FileUtils.forceMkdir(destination.getParentFile());
      } catch (IOException e) {
        throw new DistributionException("Unable to create " + destination.getParentFile(), e);
      }
      logger.info("Distributing {} to {}", elementId, destination);

      try {
        FileSupport.copy(source, destination);
      } catch (IOException e) {
        throw new DistributionException("Unable to copy " + source + " to " + destination, e);
      }

      // Create a representation of the distributed file in the mediapackage
      MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      try {
        distributedElement.setURI(getDistributionUri(mediaPackageId, element));
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
  
  /* (non-Javadoc)
   * @see org.opencastproject.distribution.api.DistributionService#distributeWithAclXml(org.opencastproject.mediapackage.MediaPackage, java.lang.String, java.lang.String)
   */
  @Override
  public Job distributeWithAclXml(MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {
    if (mediapackage == null)
      throw new MediaPackageException("Mediapackage must be specified");
    if (elementId == null)
      throw new MediaPackageException("Element ID must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.DistributeWithAcl.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }
  
  /**
   * Distributes a MediaPackageElement and an ACL in an XML document to control it to the download distribution service.
   * 
   * @param job
   *          The job trying to distribute a MediaPackageElement and an XML document with an ACL.
   * @param mediaPackage
   *          The MediaPackage containing the MediaPackageElement
   * @param elementId
   *          The unique id of the MediaPackageElement to distribute.
   * @return Returns a reference to the distributed MediaPackageElement.
   */
  protected MediaPackageElement distributeWithAclXml(Job job, MediaPackage mediaPackage, String elementId)
          throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

    distributeAclXml(mediaPackage, elementId);
    return distributeElement(mediaPackage, elementId);
  }
  
  private void distributeAclXml(MediaPackage mediaPackage, String elementId) {
    try {
      File source = getAclXmlAttachment(mediaPackage);
      if (source == null) {
        throw new DistributionException("No available acl for mediapackage " + mediaPackage);
      }

      File destination = new File(PathSupport.concat(new String[] { distributionDirectory.getAbsolutePath(),
              mediaPackage.getIdentifier().compact(), elementId, elementId + ".acl" }));

      // Create the parent directory if it doesn't exist.
      try {
        FileUtils.forceMkdir(destination.getParentFile());
      } catch (IOException e) {
        throw new DistributionException("Unable to create " + destination.getParentFile(), e);
      }
      logger.info("Distributing {} to {}", elementId, destination);
      // Copy the acl from source to destination.
      try {
        FileSupport.copy(source, destination);
      } catch (IOException e) {
        throw new DistributionException("Unable to copy " + source + " to " + destination, e);
      }
    } catch (Exception e) {
      logger.warn("Could not distribute acl XML with " + elementId + " from " + mediaPackage.getIdentifier().compact(), e);
    }
  }
  
  private File getAclXmlAttachment(MediaPackage mediaPackage) throws DistributionException {
    File aclSourceFile = null;
    Attachment[] aclAttachments = mediaPackage.getAttachments(new MediaPackageElementFlavor("text", "acl"));
    if (aclAttachments.length > 0) {
      Attachment acl = aclAttachments[0];
      if (acl != null) {
        try {
          aclSourceFile = workspace.get(acl.getURI());
        } catch (NotFoundException e) {
          throw new DistributionException("Unable to find " + acl.getURI() + " in the workspace", e);
        } catch (IOException e) {
          throw new DistributionException("Error loading " + acl.getURI() + " from the workspace", e);
        }
      }
    }
    return aclSourceFile;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  public Job retract(MediaPackage mediaPackage, String elementId) throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    try {
      List<String> arguments = new ArrayList<String>();
      arguments.add(MediaPackageParser.getAsXml(mediaPackage));
      arguments.add(elementId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Retracts the mediapackage with the given identifier from the distribution channel.
   * 
   * @param job
   *          the associated job
   * @param mediapackage
   *          the mediapackage
   * @param elementId
   *          the element identifier
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  protected MediaPackageElement retract(Job job, MediaPackage mediapackage, String elementId)
          throws DistributionException {

    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

    // Make sure the element exists
    MediaPackageElement element = mediapackage.getElementById(elementId);
    if (element == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");

    // Find the element that has been created as part of the distribution process
    String mediaPackageId = mediapackage.getIdentifier().compact();
    URI distributedURI = null;
    MediaPackageElement distributedElement = null; 
    try {
      distributedURI = getDistributionUri(mediaPackageId, element);
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
        
    String mediapackageId = mediapackage.getIdentifier().compact();
    try {

      File mediapackageDir = getMediaPackageDirectory(mediapackageId);
      File elementDir = getDistributionFile(mediapackage, element);

      logger.info("Retracting element {} from {}", distributedElement, elementDir);

      // Does the file exist? If not, the current element has not been distributed to this channel
      // or has been removed otherwise
      if (!elementDir.exists()) {
        logger.warn("Unable to delete element from {}", elementDir);
        return distributedElement;
      }

      retractAclXmlFile();
      
      // Try to remove the file and - if possible - the parent folder
      FileUtils.forceDelete(elementDir.getParentFile());
      if (mediapackageDir.list().length == 0) {
        FileSupport.delete(mediapackageDir);
      }

      logger.info("Finished rectracting element {} of media package {}", elementId, mediapackageId);

      return distributedElement;
    } catch (Exception e) {
      logger.warn("Error retracting element " + elementId + " of mediapackage " + mediapackageId, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }

  }

  private void retractAclXmlFile() {
    
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
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(0));
      String elementId = arguments.get(1);
      switch (op) {
        case Distribute:
          MediaPackageElement distributedElement = distribute(job, mediapackage, elementId);
          return (distributedElement != null) ? MediaPackageElementParser.getAsXml(distributedElement) : null;
        case DistributeWithAcl:
          MediaPackageElement distributedAclElement = distributeWithAclXml(job, mediapackage, elementId);
          return (distributedAclElement != null) ? MediaPackageElementParser.getAsXml(distributedAclElement) : null;
        case Retract:
          MediaPackageElement retractedElement = retract(job, mediapackage, elementId);
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
   * @param mediaPackage
   *          the media package
   * @param element
   *          The mediapackage element being distributed
   * @return The file to copy the content to
   */
  protected File getDistributionFile(MediaPackage mediaPackage, MediaPackageElement element) {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String directoryName = distributionDirectory.getAbsolutePath();
    String destinationFileName = PathSupport.concat(new String[] { directoryName,
            mediaPackage.getIdentifier().compact(), elementId, fileName });
    return new File(destinationFileName);
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
  protected URI getDistributionUri(String mediaPackageId, MediaPackageElement element) throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { serviceUrl, mediaPackageId, elementId, fileName });
    return new URI(destinationURI);
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   * 
   * @param mediaPackageId
   *          the mediapackage ID
   * @return the filesystem directory
   */
  protected File getMediaPackageDirectory(String mediaPackageId) {
    return new File(distributionDirectory, mediaPackageId);
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
