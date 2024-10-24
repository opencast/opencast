/*
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
import org.opencastproject.util.LoadUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/** Creates a subtitles file for a video. */
@Component(
    immediate = true,
    service = {
        SpeechToTextService.class
    },
    property = {
        "service.description=Speech to Text Service",
        "service.pid=org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl"
    })
public class SpeechToTextServiceImpl extends AbstractJobProducer implements SpeechToTextService {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceImpl.class);

  /** The current used engine. */
  private SpeechToTextEngine speechToTextEngine;

  /** Configuration key for this operation's job load */
  private static final String JOB_LOAD_CONFIG = "job.load.speechtotext";

  /** The load introduced on the system by creating an inspect job */
  private static final float JOB_LOAD_DEFAULT = 0.8f;

  /** The load introduced on the system by creating an inspect job */
  private float jobLoad = JOB_LOAD_DEFAULT;

  /** List of available operations on jobs */
  private static final String OPERATION = "speechtotext";

  /** The workspace collection name */
  private static final String COLLECTION = "subtitles";

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
  @Modified
  public void activate(ComponentContext cc) {
    logger.debug("Activated/Modified speech to text service");
    Dictionary<String, Object> properties = cc.getProperties();
    jobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_CONFIG, JOB_LOAD_DEFAULT, serviceRegistry);
    logger.debug("Finished activating/updating speech-to-text service");
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
    Boolean translate = BooleanUtils.toBooleanObject(arguments.get(2));
    if (translate == null) {
      translate = false;
    }
    URI subtitleFilesURI;
    var name = String.format("job-%d", job.getId());
    var jobDir  = Path.of(workspace.rootDirectory(), "collection", COLLECTION, name).toFile();

    try {
      // prepare the output file
      jobDir.mkdirs();
      SpeechToTextEngine.Result result = speechToTextEngine.generateSubtitlesFile(
              workspace.get(mediaFile), jobDir, language, translate);
      language = result.getLanguage();

      // we need to call the "putInCollection" method to get
      // a URI, that can be used in the following processes
      final var outputName = String.format("%d-%s.vtt", job.getId(), FilenameUtils.getBaseName(mediaFile.getPath()));
      try (FileInputStream in = new FileInputStream(result.getSubtitleFile())) {
        subtitleFilesURI = workspace.putInCollection(COLLECTION, outputName, in);
      }
    } catch (Exception e) {
      throw new SpeechToTextServiceException("Error while generating subtitle from " + mediaFile, e);
    } finally {
      FileUtils.deleteQuietly(jobDir);
    }
    return subtitleFilesURI.toString() + "," + language + "," + speechToTextEngine.getEngineName();
  }


  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextService#transcribe(URI, String, Boolean)
   */
  @Override
  public Job transcribe(URI mediaFile, String language, Boolean translate) throws SpeechToTextServiceException {
    try {
      logger.debug("Creating speechToText service job");
      List<String> jobArguments = Arrays.asList(mediaFile.toString(), language, translate.toString());
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

  @Reference(
          name = "SpeechToTextEngine",
          target = "(enginetype=whispercpp)"
  )
  public void setSpeechToTextEngine(SpeechToTextEngine engine) {
    this.speechToTextEngine = engine;
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
