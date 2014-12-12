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
package org.opencastproject.inspection.ffmpeg;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.parser.Parser;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/** Inspects media via ffprobe. */
public class MediaInspectionServiceImpl extends AbstractJobProducer implements MediaInspectionService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Inspect, Enrich
  }

  private Workspace workspace;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService = null;
  private UserDirectoryService userDirectoryService = null;
  private OrganizationDirectoryService organizationDirectoryService = null;
  private Parser tikaParser;

  private volatile MediaInspector inspector;

  /**
   * Sets the Apache Tika parser.
   *
   * @param tikaParser
   */
  public void setTikaParser(Parser tikaParser) {
    this.tikaParser = tikaParser;
  }

  /** Creates a new media inspection service instance. */
  public MediaInspectionServiceImpl() {
    super(JOB_TYPE);
  }

  public void activate(ComponentContext cc) {
    /* Configure analyzer */
    final String path = cc.getBundleContext().getProperty(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG);
    final String ffprobeBinary;
    if (path == null) {
      logger.debug("DEFAULT " + FFmpegAnalyzer.FFPROBE_BINARY_CONFIG + ": "
          + FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT);
      ffprobeBinary = FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT;
    } else {
      logger.debug("FFprobe config binary: {}", path);
      ffprobeBinary = path;
    }
    inspector = new MediaInspector(workspace, tikaParser, ffprobeBinary);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null)
      return;
    final String path = StringUtils.trimToNull((String) properties.get(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG));
    if (path != null) {
      logger.info("Setting the path to ffprobe to " + path);
      inspector = new MediaInspector(workspace, tikaParser, path);
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
      MediaPackageElement inspectedElement = null;
      switch (op) {
        case Inspect:
          URI uri = URI.create(arguments.get(0));
          inspectedElement = inspector.inspectTrack(uri);
          break;
        case Enrich:
          MediaPackageElement element = MediaPackageElementParser.getFromXml(arguments.get(0));
          boolean overwrite = Boolean.parseBoolean(arguments.get(1));
          inspectedElement = inspector.enrich(element, overwrite);
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
      return MediaPackageElementParser.getAsXml(inspectedElement);
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI)
   */
  @Override
  public Job inspect(URI uri) throws MediaInspectionException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Inspect.toString(), Arrays.asList(uri.toString()));
    } catch (ServiceRegistryException e) {
      throw new MediaInspectionException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.mediapackage.MediaPackageElement,
   *      boolean)
   */
  @Override
  public Job enrich(final MediaPackageElement element, final boolean override) throws MediaInspectionException,
         MediaPackageException {
           try {
             return serviceRegistry.createJob(JOB_TYPE, Operation.Enrich.toString(),
                 Arrays.asList(MediaPackageElementParser.getAsXml(element), Boolean.toString(override)));
           } catch (ServiceRegistryException e) {
             throw new MediaInspectionException(e);
           }
  }

  protected void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  protected void setServiceRegistry(ServiceRegistry jobManager) {
    this.serviceRegistry = jobManager;
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
   *         the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *         the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *         the organization directory
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
