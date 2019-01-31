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

package org.opencastproject.inspection.ffmpeg;

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.inspection.api.util.Options;
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
import org.opencastproject.util.LoadUtil;
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
import java.util.Map;

/** Inspects media via ffprobe. */
public class MediaInspectionServiceImpl extends AbstractJobProducer implements MediaInspectionService, ManagedService {

  /** The load introduced on the system by creating an inspect job */
  public static final float DEFAULT_INSPECT_JOB_LOAD = 0.2f;

  /** The load introduced on the system by creating an enrich job */
  public static final float DEFAULT_ENRICH_JOB_LOAD = 0.2f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_INSPECT_JOB_LOAD} */
  public static final String INSPECT_JOB_LOAD_KEY = "job.load.inspect";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_ENRICH_JOB_LOAD} */
  public static final String ENRICH_JOB_LOAD_KEY = "job.load.enrich";

  /** The load introduced on the system by creating an inspect job */
  private float inspectJobLoad = DEFAULT_INSPECT_JOB_LOAD;

  /** The load introduced on the system by creating an enrich job */
  private float enrichJobLoad = DEFAULT_ENRICH_JOB_LOAD;

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

  private volatile MediaInspector inspector;

  /** Creates a new media inspection service instance. */
  public MediaInspectionServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    /* Configure analyzer */
    final String path = cc.getBundleContext().getProperty(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG);
    final String ffprobeBinary;
    if (path == null) {
      logger.debug("DEFAULT " + FFmpegAnalyzer.FFPROBE_BINARY_CONFIG + ": " + FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT);
      ffprobeBinary = FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT;
    } else {
      logger.debug("FFprobe config binary: {}", path);
      ffprobeBinary = path;
    }
    inspector = new MediaInspector(workspace, ffprobeBinary);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null)
      return;

    inspectJobLoad = LoadUtil.getConfiguredLoadValue(properties, INSPECT_JOB_LOAD_KEY, DEFAULT_INSPECT_JOB_LOAD,
            serviceRegistry);
    enrichJobLoad = LoadUtil.getConfiguredLoadValue(properties, ENRICH_JOB_LOAD_KEY, DEFAULT_ENRICH_JOB_LOAD,
            serviceRegistry);
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
      Map<String, String> options = null;
      switch (op) {
        case Inspect:
          URI uri = URI.create(arguments.get(0));
          options = Options.fromJson(arguments.get(1));
          inspectedElement = inspector.inspectTrack(uri, options);
          break;
        case Enrich:
          MediaPackageElement element = MediaPackageElementParser.getFromXml(arguments.get(0));
          boolean overwrite = Boolean.parseBoolean(arguments.get(1));
          options = Options.fromJson(arguments.get(2));
          inspectedElement = inspector.enrich(element, overwrite, options);
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
    return inspect(uri, Options.NO_OPTION);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, java.util.Map)
   */
  @Override
  public Job inspect(URI uri, final Map<String,String> options) throws MediaInspectionException {
    assert (options != null);
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Inspect.toString(), Arrays.asList(uri.toString(),
              Options.toJson(options)), inspectJobLoad);
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
  public Job enrich(final MediaPackageElement element, final boolean override)
          throws MediaInspectionException, MediaPackageException {
    return enrich(element, override, Options.NO_OPTION);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.mediapackage.MediaPackageElement,
   *      boolean, java.util.Map)
   */
  @Override
  public Job enrich(final MediaPackageElement element, final boolean override, final Map<String,String> options)
          throws MediaInspectionException, MediaPackageException {
    assert (options != null);
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Enrich.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(element), Boolean.toString(override),
              Options.toJson(options)), enrichJobLoad);
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
