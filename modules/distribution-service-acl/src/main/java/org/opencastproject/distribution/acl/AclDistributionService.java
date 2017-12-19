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

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.AbstractDistributionService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.PathSupport;

import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Distributes an access control list to control media to the local media delivery directory.
 */
public class AclDistributionService extends AbstractDistributionService implements DistributionService, ManagedService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AclDistributionService.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.acl";

  /** The load on the system introduced by creating a distribute job */
  public static final float DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f;

  /** The load on the system introduced by creating a retract job */
  public static final float DEFAULT_RETRACT_JOB_LOAD = 1.0f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_DISTRIBUTE_JOB_LOAD} */
  public static final String DISTRIBUTE_JOB_LOAD_KEY = "job.load.acl.distribute";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_RETRACT_JOB_LOAD} */
  public static final String RETRACT_JOB_LOAD_KEY = "job.load.acl.retract";

  /** The load on the system introduced by creating a distribute job */
  private float distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD;

  /** The load on the system introduced by creating a retract job */
  private float retractJobLoad = DEFAULT_RETRACT_JOB_LOAD;

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
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    serviceUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
    if (serviceUrl == null)
      throw new IllegalStateException("Download url must be set (org.opencastproject.download.url)");

    String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    if (ccDistributionDirectory == null)
      throw new IllegalStateException("Distribution directory must be set (org.opencastproject.download.directory)");
    this.distributionDirectory = new File(ccDistributionDirectory);
    logger.info("Download distribution directory is {}", distributionDirectory);
    this.distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE);
  }

  public String getDistributionType() {
    return this.distributionChannel;
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
                                                     elementId), distributeJobLoad);
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
                                                     elementId), retractJobLoad);
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

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    distributeJobLoad = LoadUtil.getConfiguredLoadValue(properties, DISTRIBUTE_JOB_LOAD_KEY, DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry);
    retractJobLoad = LoadUtil.getConfiguredLoadValue(properties, RETRACT_JOB_LOAD_KEY, DEFAULT_RETRACT_JOB_LOAD, serviceRegistry);
  }

}
