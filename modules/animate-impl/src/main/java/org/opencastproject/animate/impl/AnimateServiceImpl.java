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

package org.opencastproject.animate.impl;

import org.opencastproject.animate.api.AnimateService;
import org.opencastproject.animate.api.AnimateServiceException;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/** Create video animations using Synfig */
@Component(
    immediate = true,
    service = {
        AnimateService.class,
        ManagedService.class
    },
    property = {
        "service.description=Animation Service",
        "service.pid=org.opencastproject.animate.impl.AnimateServiceImpl"
    }
)
public class AnimateServiceImpl extends AbstractJobProducer implements AnimateService, ManagedService {

  /** Configuration key for setting a custom synfig path */
  private static final String SYNFIG_BINARY_CONFIG = "synfig.path";

  /** Default path to the synfig binary */
  public static final String SYNFIG_BINARY_DEFAULT = "synfig";

  /** Path to the synfig binary */
  private String synfigBinary = SYNFIG_BINARY_DEFAULT;

  /** Configuration key for this operation's job load */
  private static final String JOB_LOAD_CONFIG = "job.load.animate";

  /** The load introduced on the system by creating an inspect job */
  private static final float JOB_LOAD_DEFAULT = 0.8f;

  /** The load introduced on the system by creating an inspect job */
  private float jobLoad = JOB_LOAD_DEFAULT;

  private static final Logger logger = LoggerFactory.getLogger(AnimateServiceImpl.class);

  /** List of available operations on jobs */
  private static final String OPERATION = "animate";

  private Workspace workspace;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;
  private OrganizationDirectoryService organizationDirectoryService;

  private static final Type stringMapType = new TypeToken<Map<String, String>>() { }.getType();
  private static final Type stringListType = new TypeToken<List<String>>() { }.getType();

  /** Creates a new animate service instance. */
  public AnimateServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.debug("Activated animate service");
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Start updating animate service");

    synfigBinary = StringUtils.defaultIfBlank((String) properties.get(SYNFIG_BINARY_CONFIG), SYNFIG_BINARY_DEFAULT);
    logger.debug("Set synfig binary path to {}", synfigBinary);

    jobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_CONFIG, JOB_LOAD_DEFAULT, serviceRegistry);
    logger.debug("Set animate job load to {}", jobLoad);

    logger.debug("Finished updating animate service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    logger.debug("Started processing job {}", job.getId());
    if (!OPERATION.equals(job.getOperation())) {
      throw new ServiceRegistryException(String.format("This service can't handle operations of type '%s'",
              job.getOperation()));
    }

    List<String> arguments = job.getArguments();
    URI animation = new URI(arguments.get(0));
    Gson gson = new Gson();
    Map<String, String> metadata = gson.fromJson(arguments.get(1), stringMapType);
    List<String> options = gson.fromJson(arguments.get(2), stringListType);

    // filter animation and get new, custom input file
    File input = customAnimation(job, animation, metadata);

    // prepare output file
    File output = new File(workspace.rootDirectory(), String.format("animate/%d/%s.%s", job.getId(),
            FilenameUtils.getBaseName(animation.getPath()), "mkv"));
    FileUtils.forceMkdirParent(output);

    // create animation process.
    final List<String> command = new ArrayList<>();
    command.add(synfigBinary);
    command.add("-i");
    command.add(input.getAbsolutePath());
    command.add("-o");
    command.add(output.getAbsolutePath());
    command.addAll(options);
    logger.info("Executing animation command: {}", command);

    Process process = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      process = processBuilder.start();

      // print synfig (+ffmpeg) output
      try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) {
          logger.debug("Synfig: {}", line);
        }
      }

      // wait until the task is finished
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new AnimateServiceException(String.format("Synfig exited abnormally with status %d (command: %s)",
                exitCode, command));
      }
      if (!output.isFile()) {
        throw new AnimateServiceException("Synfig produced no output");
      }
      logger.info("Animation generated successfully: {}", output);
    } catch (Exception e) {
      // Ensure temporary data are removed
      FileUtils.deleteQuietly(output.getParentFile());
      logger.debug("Removed output directory of failed animation process: {}", output.getParentFile());
      throw new AnimateServiceException(e);
    } finally {
      IoSupport.closeQuietly(process);
      FileUtils.deleteQuietly(input);
    }

    URI uri = workspace.putInCollection("animate-" + job.getId(), output.getName(),
            new FileInputStream(output));
    FileUtils.deleteQuietly(new File(workspace.rootDirectory(), String.format("animate/%d", job.getId())));

    return uri.toString();
  }


  private File customAnimation(final Job job, final URI input, final Map<String, String> metadata)
          throws IOException, NotFoundException {
    logger.debug("Start customizing the animation");
    File output = new File(workspace.rootDirectory(), String.format("animate/%d/%s.%s", job.getId(),
            FilenameUtils.getBaseName(input.getPath()), FilenameUtils.getExtension(input.getPath())));
    FileUtils.forceMkdirParent(output);
    String animation;
    try {
      animation = FileUtils.readFileToString(new File(input), "UTF-8");
    } catch (IOException e) {
      // Maybe no local file?
      logger.debug("Falling back to workspace to read {}", input);
      try (InputStream in = workspace.read(input)) {
        animation = IOUtils.toString(in, "UTF-8");
      }
    }

    // replace all metadata
    for (Map.Entry<String, String> entry: metadata.entrySet()) {
      String value = StringEscapeUtils.escapeXml11(entry.getValue());
      animation = animation.replaceAll("\\{\\{" + entry.getKey() + "\\}\\}", value);
    }

    // write new animation file
    FileUtils.write(output, animation, "utf-8");

    return output;
  }


  @Override
  public Job animate(URI animation, Map<String, String> metadata, List<String> arguments)
          throws AnimateServiceException {
    Gson gson = new Gson();
    List<String> jobArguments = Arrays.asList(animation.toString(), gson.toJson(metadata), gson.toJson(arguments));
    try {
      logger.debug("Create animate service job");
      return serviceRegistry.createJob(JOB_TYPE, OPERATION, jobArguments, jobLoad);
    } catch (ServiceRegistryException e) {
      throw new AnimateServiceException(e);
    }
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry jobManager) {
    this.serviceRegistry = jobManager;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }
}
