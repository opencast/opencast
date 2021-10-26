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

package org.opencastproject.speechtotext.impl;

import static java.util.stream.Collectors.joining;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.speechtotext.api.SpeechToTextEngine;
import org.opencastproject.speechtotext.api.SpeechToTextService;
import org.opencastproject.speechtotext.api.SpeechToTextServiceException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/** Creates a subtitles file for a video. */
@Component(
    immediate = true,
    service = {
        SpeechToTextService.class,
        ManagedService.class
    },
    property = {
        "service.description=Speech to Text Service",
        "service.pid=org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl"
})
public class SpeechToTextServiceImpl extends AbstractJobProducer implements SpeechToTextService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceImpl.class);

  /** Configuration key for the name of the engine that shall be used. */
  private static final String CONFIGURED_ENGINE_NAME_KEY = "speechtotext.engine";

  /** Contains a list of all speech-to-text engines modules that are implemented. */
  private static List<SpeechToTextEngine> availableEngines;

  /** The current used engine. */
  private static SpeechToTextEngine selectedEngine;

  /** Name of the default engine, when nothing configured. */
  private static final String DEFAULT_ENGINE_NAME = "Vosk";

  /** Configuration key for this operation's job load */
  private static final String JOB_LOAD_CONFIG = "job.load.speechtotext";

  /** The load introduced on the system by creating an inspect job */
  private static final float JOB_LOAD_DEFAULT = 0.8f;

  /** The load introduced on the system by creating an inspect job */
  private float jobLoad = JOB_LOAD_DEFAULT;

  /** List of available operations on jobs */
  private static final String OPERATION = "speechtotext";

  /** Prefix of the temporary working directory */
  private static final String COLLECTIONID_PREFIX = "subtitles-";

  private static final String TMP_PREFIX = "tmp_";


  //================================================================================
  // OSGi service instances
  //================================================================================

  /** The workspace service */
  private Workspace workspace;

  /** The registry service */
  private ServiceRegistry serviceRegistry;

  /** The security service */
  private SecurityService securityService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  //================================================================================


  /** Creates a new speech-to-text service instance. */
  public SpeechToTextServiceImpl() {
    super(JOB_TYPE);
  }


  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.debug("Activated speech to text service");
  }

  /**
   * OSGI callback when the configuration is updated. This method is only here to prevent the
   * configuration admin service from calling the service deactivate and activate methods
   * for a config update. It does not have to do anything as the updates are handled by updated().
   */
  @Modified
  public void modified(Map<String, Object> config) throws ConfigurationException {
    logger.debug("Modified speech-to-text service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    logger.debug("Start updating speech-to-text service");

    // we currently just use vosk as engine, so we hardcode it here to prevent config errors and confusion
    // if an another engine will be added, use the "updateUsedEngine" method
    if (availableEngines == null || availableEngines.size() <= 0) {
      throw new ConfigurationException("No speech to text engines are available");
    }
    for (SpeechToTextEngine engine : availableEngines) {
      if (engine.getEngineName().equals(DEFAULT_ENGINE_NAME)) { // hardcoded "vosk" as default
        selectedEngine = engine;
      }
    }

    jobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_CONFIG, JOB_LOAD_DEFAULT, serviceRegistry);
    logger.debug("Set speech-to-text job load to {}", jobLoad);

    logger.debug("Finished updating speech-to-text service");
  }

  /**
   * If configuration changes, this will change the currently used speech-to-text engine.
   *
   * @param properties Contains the configuration properties.
   */
  private void updateUsedEngine(Dictionary properties) {
    String availableEnginesAsStr = availableEngines.stream().map(
            SpeechToTextEngine::getEngineName).collect(joining(", "));
    logger.debug("Available engines: {}", availableEnginesAsStr);

    String configuredEngineName = StringUtils.defaultIfBlank(
            (String) properties.get(CONFIGURED_ENGINE_NAME_KEY), DEFAULT_ENGINE_NAME);

    for (SpeechToTextEngine engine : availableEngines) {
      if (engine.getEngineName().equals(configuredEngineName)) {
        selectedEngine = engine;
      }
    }
    if (selectedEngine == null) {
      logger.error("Couldn't find speak-to-text engine with name '{}'.", configuredEngineName);
    } else {
      logger.debug("Set speech-to-text engine to {}", selectedEngine.getEngineName());
    }
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
      throw new ServiceRegistryException(
              String.format("This service can't handle operations of type '%s'", job.getOperation()));
    }

    List<String> arguments = job.getArguments();
    String language = arguments.get(1);
    URI mediaFile = new URI(arguments.get(0));

    File tmpSubtitlesFile = null;
    URI subtitleFilesURI;
    String vttFileName = String.format("%s.%s", FilenameUtils.getBaseName(mediaFile.getPath()), "vtt");
    try {
      // prepare the output vtt file
      tmpSubtitlesFile = new File(String.format("%s/%s/%s/%s",
              workspace.rootDirectory(), "collection", getCollectionId(job), TMP_PREFIX + vttFileName));
      FileUtils.forceMkdirParent(tmpSubtitlesFile);
      // even that the mediafile is of type URI.. we use the "workspace.get()" method to get the absolute path
      tmpSubtitlesFile = selectedEngine.generateSubtitlesFile(workspace.get(mediaFile).toURI(),
              tmpSubtitlesFile, language);
      // we need to call the "putInCollection" method at this point to get a
      // proper URI that opencast can use in the following processes
      try (FileInputStream tmpSubtitlesFileIS = new FileInputStream(tmpSubtitlesFile)) {
        subtitleFilesURI = workspace.putInCollection(getCollectionId(job), vttFileName, tmpSubtitlesFileIS);
      }
      workspace.delete(workspace.getCollectionURI(getCollectionId(job), TMP_PREFIX + vttFileName));
    } catch (Exception e) {
      logger.error("Error while creating necessary subtitles tmp files and folders for mediafile '{}'", mediaFile);
      if (tmpSubtitlesFile != null) {
        FileUtils.deleteDirectory(tmpSubtitlesFile.getParentFile());
      }
      throw e;
    }
    return subtitleFilesURI.toString();
  }

  private String getCollectionId(Job job) {
    return COLLECTIONID_PREFIX + job.getId();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextService#transcribe(URI, String)
   */
  @Override
  public Job transcribe(URI mediaFile, String language) throws SpeechToTextServiceException {
    try {
      logger.debug("Creating speechToText service job");
      List<String> jobArguments = Arrays.asList(mediaFile.toString(), language);
      return serviceRegistry.createJob(JOB_TYPE, OPERATION, jobArguments, jobLoad);
    } catch (ServiceRegistryException e) {
      throw new SpeechToTextServiceException(e);
    }
  }


  //================================================================================
  // OSGi - Getter and Setter
  //================================================================================

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

  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE)
  public void addSpeechToTextEngine(SpeechToTextEngine engine) {
    if (availableEngines == null) {
      availableEngines = new ArrayList<>();
    }
    availableEngines.add(engine);
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
