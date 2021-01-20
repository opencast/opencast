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

package org.opencastproject.videogrid.impl;

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
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videogrid.api.VideoGridService;
import org.opencastproject.videogrid.api.VideoGridServiceException;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/** Create video grids */
public class VideoGridServiceImpl extends AbstractJobProducer implements VideoGridService, ManagedService {

  /** Configuration key for this operation's job load */
  private static final String JOB_LOAD_CONFIG = "job.load.videogrid";

  /** The load introduced on the system by creating a job */
  private static final float JOB_LOAD_DEFAULT = 1.5f;

  /** The load introduced on the system by creating a job */
  private float jobLoad = JOB_LOAD_DEFAULT;

  private static final Logger logger = LoggerFactory.getLogger(VideoGridServiceImpl.class);

  /** List of available operations on jobs */
  private static final String OPERATION = "createPartialTracks";

  /** Services */
  private Workspace workspace;
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;
  private OrganizationDirectoryService organizationDirectoryService;

  /** For JSON serialization */
  private static final Gson gson = new Gson();
  private static final Type stringListOfListType = new TypeToken<List<List<String>>>() { }.getType();

  /** Creates a new videogrid service instance. */
  public VideoGridServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.debug("Activated videogrid service");
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null)
      return;
    logger.debug("Start updating videogrid service");

    jobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_CONFIG, JOB_LOAD_DEFAULT, serviceRegistry);
    logger.debug("Set videogrid job load to {}", jobLoad);

    logger.debug("Finished updating videogrid service");
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

    // Parse arguments
    List<String> arguments = job.getArguments();
    List<List<String>> commands = gson.fromJson(arguments.get(0), stringListOfListType);
    List<Track> tracks = new ArrayList<>();
    for (int i = 1; i < arguments.size(); i++) {
      tracks.add(i - 1, (Track) MediaPackageElementParser.getFromXml(arguments.get(i)));
    }

    String outputDirPath = String.format("%s/videogrid/%d/", workspace.rootDirectory(), job.getId());
    FileUtils.forceMkdir(new File(outputDirPath));

    // Execute all commands
    List<String> outputPaths = new ArrayList<>();
    int index = 0;

    for (List<String> command : commands) {
      // Replace placeholders in command with track paths
      for (int i = 0; i < command.size(); i++) {
        String[] trackIds = StringUtils.substringsBetween(command.get(i), "#{","}");
        if (trackIds != null) {
          for (String trackId: trackIds) {
            Track replaceTrack = tracks.stream()
                    .filter(track -> track.getIdentifier().equals(trackId))
                    .findAny()
                    .orElse(null);
            if (replaceTrack == null)
              throw new VideoGridServiceException(String.format("Track with id %s could not be found!", trackId));
            command.set(i, command.get(i).replaceAll("#\\{" + trackId + "\\}", getTrackPath(replaceTrack)));
          }
        }
      }

      // Add output path to command
      String outputFile = outputDirPath + "videogrid_part_" + index + "_" + job.getId() + ".mp4";
      outputPaths.add(outputFile);
      command.add(outputFile);
      index++;

      logger.info("Running command: {}", command);

      // Run ffmpeg
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);
      Process ffmpegProcess = null;
      int exitCode = 1;
      BufferedReader errStream = null;
      try {
        ffmpegProcess = pb.start();

        errStream = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
        String line = errStream.readLine();
        while (line != null) {
          logger.info(line);
          line = errStream.readLine();
        }

        exitCode = ffmpegProcess.waitFor();
      } catch (IOException ex) {
        throw new VideoGridServiceException("Start ffmpeg process failed", ex);
      } catch (InterruptedException ex) {
        throw new VideoGridServiceException("Waiting for encoder process exited was interrupted unexpectedly", ex);
      } finally {
        IoSupport.closeQuietly(ffmpegProcess);
        IoSupport.closeQuietly(errStream);
        if (exitCode != 0) {
          try {
            logger.warn("FFMPEG process exited with errorcode: " + exitCode);
            FileUtils.forceDelete(new File(outputDirPath));
          } catch (IOException e) {
            // it is ok, no output file was generated by ffmpeg
          }
        }
      }

      if (exitCode != 0)
        throw new Exception(String.format("The encoder process exited abnormally with exit code %s "
                + "using command\n%s", exitCode, String.join(" ", command)));
    }

    // Put each generated video into workspace
    List<URI> uris = new ArrayList<>();
    for (String outputPath : outputPaths) {

      FileInputStream outputFileInputStream = null;
      URI videoFileUri;
      try {
        outputFileInputStream = new FileInputStream(outputPath);
        videoFileUri = workspace.putInCollection("videogrid",
                FilenameUtils.getName(outputPath), outputFileInputStream);
        uris.add(videoFileUri);
        logger.info("Copied the created video to the workspace {}", videoFileUri);
      } catch (FileNotFoundException ex) {
        throw new VideoGridServiceException(String.format("Video file '%s' not found", outputPath), ex);
      } catch (IOException ex) {
        throw new VideoGridServiceException(String.format(
                "Can't write video file '%s' to workspace", outputPath), ex);
      } catch (IllegalArgumentException ex) {
        throw new VideoGridServiceException(ex);
      } finally {
        IoSupport.closeQuietly(outputFileInputStream);
      }
    }

    FileUtils.deleteQuietly(new File(workspace.rootDirectory(), String.format("videogrid/%d", job.getId())));

    // Return URIs to the videos;
    return gson.toJson(uris);
  }

  @Override
  public Job createPartialTracks(List<List<String>> commands, Track... tracks)
          throws VideoGridServiceException, MediaPackageException {
    List<String> jobArguments = new ArrayList<>(Arrays.asList(gson.toJson(commands)));
    for (int i = 0; i < tracks.length; i++) {
      jobArguments.add(i + 1, MediaPackageElementParser.getAsXml(tracks[i]));
    }
    try {
      logger.debug("Create videogrid service job");
      return serviceRegistry.createJob(JOB_TYPE, OPERATION, jobArguments, jobLoad);
    } catch (ServiceRegistryException e) {
      throw new VideoGridServiceException(e);
    }
  }

  /**
   * Returns the absolute path of the track
   *
   * @param track
   *          Track whose path you want
   * @return {@String} containing the absolute path of the given track
   * @throws VideoGridServiceException
   */
  private String getTrackPath(Track track) throws VideoGridServiceException {
    File mediaFile;
    try {
      mediaFile = workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new VideoGridServiceException(
              "Error finding the media file in the workspace", e);
    } catch (IOException e) {
      throw new VideoGridServiceException(
              "Error reading the media file in the workspace", e);
    }
    return mediaFile.getAbsolutePath();
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

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setServiceRegistry(ServiceRegistry jobManager) {
    this.serviceRegistry = jobManager;
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
}
