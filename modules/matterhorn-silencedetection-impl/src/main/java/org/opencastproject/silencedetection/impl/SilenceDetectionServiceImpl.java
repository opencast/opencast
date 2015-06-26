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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
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
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SilenceDetectionService using FFmpeg.
 */
public class SilenceDetectionServiceImpl extends AbstractJobProducer implements SilenceDetectionService, ManagedService {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionServiceImpl.class);

  private static enum Operation {

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
              arguments);

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
   * {@see org.opencastproject.silencedetection.api.MediaSegments}
   * XML as string. Source track should have an audio stream. All detected
   * {@see org.opencastproject.silencedetection.api.MediaSegment}s
   * (one or more) are non silent sequences.
   *
   * @param track track where to run silence detection
   * @return {@see MediaSegments} Xml as String
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
    Track[] referenceTracksArr = referenceTracks.toArray(new Track[referenceTracks.size()]);

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

  protected void activate(ComponentContext context) {
    logger.debug("activating...");
  }

  protected void deactivate(ComponentContext context) {
    logger.debug("deactivating...");
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = new Properties();
    Enumeration keys = properties.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      this.properties.put(key, properties.get(key));
    }
    logger.debug("Properties updated!");
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }
}
