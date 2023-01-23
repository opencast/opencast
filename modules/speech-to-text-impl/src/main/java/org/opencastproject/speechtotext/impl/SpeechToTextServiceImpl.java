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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
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

    URI subtitleFilesURI;
    File subtitlesFile = null;
    String vttFileName = String.format("%s%d_%s.%s", TMP_PREFIX,
            job.getId(), FilenameUtils.getBaseName(mediaFile.getPath()), "vtt");

    try {
      // prepare the output file
      subtitlesFile = new File(String.format("%s/collection/%s/%s",
              workspace.rootDirectory(), COLLECTION, vttFileName));
      subtitlesFile.deleteOnExit();
      FileUtils.forceMkdirParent(subtitlesFile);

      subtitlesFile = speechToTextEngine.generateSubtitlesFile(
              workspace.get(mediaFile), subtitlesFile, language);

      // we need to call the "putInCollection" method to get
      // a URI, that can be used in the following processes
      try (FileInputStream subtitlesFileIS = new FileInputStream(subtitlesFile)) {
        subtitleFilesURI = workspace.putInCollection(COLLECTION,
                vttFileName.replaceFirst(TMP_PREFIX, ""), subtitlesFileIS);
      }
    } catch (Exception e) {
      throw new SpeechToTextServiceException("Error while generating subtitle from " + mediaFile, e);
    } finally {
      if (subtitlesFile != null && subtitlesFile.exists()) {
        FileUtils.deleteQuietly(subtitlesFile);
      }
    }
    return subtitleFilesURI.toString();
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

  @Reference(
          name = "SpeechToTextEngine",
          target = "(engineType=vosk)"
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
