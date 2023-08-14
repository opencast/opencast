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

package org.opencastproject.silencedetection.impl;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.silencedetection.api.MediaSegment;
import org.opencastproject.silencedetection.api.MediaSegments;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.api.SilenceDetectionService;
import org.opencastproject.silencedetection.ffmpeg.FFmpegSilenceDetector;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Implementation of SilenceDetectionService using FFmpeg.
 */
@Component(
    property = {
        "service.description=Silence Detection Service"
    },
    immediate = true,
    service = SilenceDetectionService.class
)
public class SilenceDetectionServiceImpl extends AbstractJobProducer implements SilenceDetectionService {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionServiceImpl.class);

  public static final String JOB_LOAD_KEY = "job.load.videoeditor.silencedetection";

  private static final float DEFAULT_JOB_LOAD = 0.2f;

  private float jobload = DEFAULT_JOB_LOAD;

  private enum Operation {
    SILENCE_DETECTION
  }
  /**
   * Reference to the workspace service
   */
  private Workspace workspace = null;
  /**
   * Reference to the receipt service
   */
  private ServiceRegistry serviceRegistry;
  /**
   * The organization directory service
   */
  protected OrganizationDirectoryService organizationDirectoryService = null;
  /**
   * The security service
   */
  protected SecurityService securityService = null;
  /**
   * The user directory service
   */
  protected UserDirectoryService userDirectoryService = null;
  protected SmilService smilService = null;
  private Properties properties;

  public SilenceDetectionServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.silencedetection.api.SilenceDetectionService#detect(
   * org.opencastproject.mediapackage.Track)
   */
  @Override
  public Job detect(Track sourceTrack) throws SilenceDetectionFailedException {
    return detect(sourceTrack, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.silencedetection.api.SilenceDetectionService#detect(
   * org.opencastproject.mediapackage.Track, org.opencastproject.mediapackage.Track[])
   */
  @Override
  public Job detect(Track sourceTrack, Track[] referenceTracks) throws SilenceDetectionFailedException {
    try {
      if (sourceTrack == null) {
        throw new SilenceDetectionFailedException("Source track is null!");
      }
      List<String> arguments = new LinkedList<String>();
      // put source track as job argument
      arguments.add(0, MediaPackageElementParser.getAsXml(sourceTrack));

      // put reference tracks as second argument
      if (referenceTracks != null) {
        arguments.add(1, MediaPackageElementParser.getArrayAsXml(Arrays.asList(referenceTracks)));
      }

      return serviceRegistry.createJob(
              getJobType(),
              Operation.SILENCE_DETECTION.toString(),
              arguments,
              jobload);

    } catch (ServiceRegistryException ex) {
      throw new SilenceDetectionFailedException("Unable to create job! " + ex.getMessage());
    } catch (MediaPackageException ex) {
      throw new SilenceDetectionFailedException("Unable to serialize track!");
    }
  }

  @Override
  protected String process(Job job) throws SilenceDetectionFailedException, SmilException, MediaPackageException {
    if (Operation.SILENCE_DETECTION.toString().equals(job.getOperation())) {
      // get source track
      String sourceTrackXml = StringUtils.trimToNull(job.getArguments().get(0));
      if (sourceTrackXml == null) {
        throw new SilenceDetectionFailedException("Track not set!");
      }
      Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);

      // run detection on source track
      MediaSegments segments = runDetection(sourceTrack);

      // get reference tracks if any
      List<Track> referenceTracks = null;
      if (job.getArguments().size() > 1) {
        String referenceTracksXml = StringUtils.trimToNull(job.getArguments().get(1));
        if (referenceTracksXml != null) {
          referenceTracks = (List<Track>) MediaPackageElementParser.getArrayFromXml(referenceTracksXml);
        }
      }

      if (referenceTracks == null) {
        referenceTracks = Arrays.asList(sourceTrack);
      }

      // create smil XML as result
      try {
        return generateSmil(segments, referenceTracks).toXML();
      } catch (Exception ex) {
        throw new SmilException("Failed to create smil document.", ex);
      }
    }

    throw new SilenceDetectionFailedException("Can't handle this operation: " + job.getOperation());
  }

  /**
   * Run silence detection on the source track and returns
   * {@link org.opencastproject.silencedetection.api.MediaSegments}
   * XML as string. Source track should have an audio stream. All detected
   * {@link org.opencastproject.silencedetection.api.MediaSegment}s
   * (one or more) are non silent sequences.
   *
   * @param track track where to run silence detection
   * @return {@link MediaSegments} Xml as String
   * @throws SilenceDetectionFailedException if an error occures
   */
  protected MediaSegments runDetection(Track track) throws SilenceDetectionFailedException {
    try {
      FFmpegSilenceDetector silenceDetector = new FFmpegSilenceDetector(properties, track, workspace);
      return silenceDetector.getMediaSegments();
    } catch (Exception ex) {
      throw new SilenceDetectionFailedException(ex.getMessage());
    }
  }

  /**
   * Create a smil from given parameters.
   *
   * @param segments media segment list with timestamps
   * @param referenceTracks tracks to put as media segment source files
   * @return generated smil
   * @throws SmilException if smil creation failed
   */
  protected Smil generateSmil(MediaSegments segments, List<Track> referenceTracks) throws SmilException {
    SmilResponse smilResponse = smilService.createNewSmil();
    Track[] referenceTracksArr = referenceTracks.toArray(new Track[0]);

    for (MediaSegment segment : segments.getMediaSegments()) {
      smilResponse = smilService.addParallel(smilResponse.getSmil());
      String parId = smilResponse.getEntity().getId();

      smilResponse = smilService.addClips(smilResponse.getSmil(), parId, referenceTracksArr,
              segment.getSegmentStart(), segment.getSegmentStop() - segment.getSegmentStart());
    }
    return smilResponse.getSmil();
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

  @Activate
  @Modified
  public void activate(BundleContext bundleContext, ComponentContext context) {
    logger.debug("Loading configuration");
    super.activate(context);

    this.properties = new Properties();
    if (bundleContext == null) {
      logger.info("No configuration available, using defaults");
      return;
    }
    FFmpegSilenceDetector.init(bundleContext);
    Dictionary<String, Object> properties = context.getProperties();
    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      final String key = keys.nextElement();
      logger.debug("{} = {}", key, properties.get(key));
      this.properties.put(key, properties.get(key));
    }
    logger.debug("Properties updated!");

    jobload = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_KEY, DEFAULT_JOB_LOAD, serviceRegistry);
  }

  @Deactivate
  protected void deactivate(ComponentContext context) {
    logger.debug("deactivating...");
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
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

  @Reference
  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }
}
