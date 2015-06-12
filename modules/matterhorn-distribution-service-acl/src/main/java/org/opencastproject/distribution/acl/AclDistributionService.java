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


package org.opencastproject.distribution.acl;

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
import java.util.Arrays;
import java.util.List;

import static org.opencastproject.util.RequireUtil.notNull;

/**
 * Distributes an access control list to control media to the local media delivery directory.
 */
public class AclDistributionService extends AbstractJobProducer implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AclDistributionService.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.acl";

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
  public AclDistributionService() {
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
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(),
                                       Arrays.asList(channelId,
                                                     MediaPackageParser.getAsXml(mediapackage),
                                                     elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  protected MediaPackageElement distributeElement(String channelId, MediaPackage mediaPackage, String elementId)
          throws DistributionException, MediaPackageException {
    try {
      File source = getAclXmlAttachmentFile(mediaPackage);
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

      return (MediaPackageElement)getAclXmlAttachment(mediaPackage);
    } catch (Exception e) {
      logger.warn("Could not distribute acl XML with " + elementId + " from " + mediaPackage.getIdentifier().compact(), e);
      return null;
    }
  }

  /**
   * @param mediaPackage The media package to find the attachment that has an ACL.
   * @return Returns the attachment if possible or null.
   */
  private Attachment getAclXmlAttachment(MediaPackage mediaPackage) {
    Attachment[] aclAttachments = mediaPackage.getAttachments(new MediaPackageElementFlavor("text", "acl"));
    if (aclAttachments.length > 0) {
      return aclAttachments[0];
    } else
    {
      return null;
    }
  }


  /**
   * @param mediaPackage
   *          The media package to find the acl xml.
   * @return Returns a File object representing the attachment acl xml
   * @throws DistributionException
   *           Thrown if it is unable to access the acl xml.
   */
  private File getAclXmlAttachmentFile(MediaPackage mediaPackage) throws DistributionException {
    File aclSourceFile = null;
    Attachment acl = getAclXmlAttachment(mediaPackage);
    if (acl != null) {
      try {
        aclSourceFile = workspace.get(acl.getURI());
      } catch (NotFoundException e) {
        throw new DistributionException("Unable to find " + acl.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new DistributionException("Error loading " + acl.getURI() + " from the workspace", e);
      }
    }
    return aclSourceFile;
  }

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementId, "elementId");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
                                       Arrays.asList(channelId,
                                                     MediaPackageParser.getAsXml(mediapackage),
                                                     elementId));
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
   return null;
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
