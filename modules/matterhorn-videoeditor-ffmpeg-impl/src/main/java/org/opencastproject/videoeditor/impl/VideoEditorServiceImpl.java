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
package org.opencastproject.videoeditor.impl;

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.api.VideoEditorService;
import org.opencastproject.videoeditor.ffmpeg.FFmpegEdit;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;

/**
 * Implementation of VideoeditorService using FFMPEG
 */
public class VideoEditorServiceImpl extends AbstractJobProducer implements VideoEditorService, ManagedService {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(VideoEditorServiceImpl.class);
  private static final String JOB_TYPE = "org.opencastproject.videoeditor";
  private static final String COLLECTION_ID = "videoeditor";
  private static final String SINK_FLAVOR_SUBTYPE = "trimmed";

  private static enum Operation {
    PROCESS_SMIL
  }
  /**
   * Reference to the media inspection service
   */
  private MediaInspectionService inspectionService = null;
  /**
   * Reference to the workspace service
   */
  private Workspace workspace = null;
  /**
   * Id builder used to create ids for encoded tracks
   */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();
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
  /**
   * The smil service.
   */
  protected SmilService smilService = null;
  /**
   * Bundle properties
   */
  private Properties properties = new Properties();
  /**
   * Temp storage directory
   */
  private String storageDir;

  public VideoEditorServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Splice segments given by smil document for the given track to the new one.
   *
   * @param job processing job
   * @param smil smil document with media segments description
   * @param track source track
   * @return processed track
   * @throws ProcessFailedException if an error occured
   */
  protected synchronized Track processSmil(Job job, Smil smil, String trackParamGroupId) throws ProcessFailedException {

    SmilMediaParamGroup trackParamGroup;
    ArrayList<String> inputfile = new ArrayList<String>();
    ArrayList<VideoClip> videoclips = new ArrayList<VideoClip>();
    try {
      trackParamGroup = (SmilMediaParamGroup) smil.get(trackParamGroupId);
    } catch (SmilException ex) {
      // can't be thrown, because we found the Id in processSmil(Smil)
      throw new ProcessFailedException("Smil does not contain a paramGroup element with Id " + trackParamGroupId);
    }
    String sourceTrackId = null;
    MediaPackageElementFlavor sourceTrackFlavor = null;
    String sourceTrackUri = null;
    // get source track metadata
    for (SmilMediaParam param : trackParamGroup.getParams()) {
      if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.getName())) {
        sourceTrackId = param.getValue();
      } else if (SmilMediaParam.PARAM_NAME_TRACK_SRC.equals(param.getName())) {
        sourceTrackUri = param.getValue();
      } else if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR.equals(param.getName())) {
        sourceTrackFlavor = MediaPackageElementFlavor.parseFlavor(param.getValue());
      }
    }
    File sourceFile = null;
    try {
      sourceFile = workspace.get(new URI(sourceTrackUri));
    } catch (IOException ex) {
      throw new ProcessFailedException("Can't read " + sourceTrackUri);
    } catch (NotFoundException ex) {
      throw new ProcessFailedException("Workspace does not contain a track " + sourceTrackUri);
    } catch (URISyntaxException ex) {
      throw new ProcessFailedException("Source URI " + sourceTrackUri + " is not valid.");
    }
    // get output file extension
    String outputFileExtension = properties.getProperty(VideoEditorProperties.DEFAULT_EXTENSION, ".mp4");
    outputFileExtension = properties.getProperty(VideoEditorProperties.OUTPUT_FILE_EXTENSION, outputFileExtension);

    if (!outputFileExtension.startsWith(".")) {
      outputFileExtension = '.' + outputFileExtension;
    }

    // create working directory
    File outputPath = new File(storageDir + "/" + job.getId(),
            sourceTrackFlavor + "_" + sourceFile.getName() + outputFileExtension);

    if (!outputPath.getParentFile().exists()) {
      outputPath.getParentFile().mkdirs();
    }
    URI newTrackURI = null;
    inputfile.add(sourceFile.getAbsolutePath()); // default source - add to source table as 0
    int srcIndex = inputfile.indexOf(sourceFile.getAbsolutePath()); // index = 0
    logger.info("Start processing srcfile {}", sourceFile.getAbsolutePath());
    try {
      // parse body elements
      for (SmilMediaObject element : smil.getBody().getMediaElements()) {
        // body should contain par elements
        if (element.isContainer()) {
          SmilMediaContainer container = (SmilMediaContainer) element;
          if (SmilMediaContainer.ContainerType.PAR == container.getContainerType()) {
            // par element should contain media elements
            for (SmilMediaObject elementChild : container.getElements()) {
              if (!elementChild.isContainer()) {
                SmilMediaElement media = (SmilMediaElement) elementChild;
                 //logger.debug("Start processing smilMedia {}", media.toString());
                if (trackParamGroupId.equals(media.getParamGroup())) {
                  long begin = media.getClipBeginMS();
                  long end = media.getClipEndMS();
                  URI clipTrackURI = media.getSrc();
                  File clipSourceFile = null;
                  if (clipTrackURI != null) {
                    try {
                      clipSourceFile = workspace.get(clipTrackURI);
                    } catch (IOException ex) {
                      throw new ProcessFailedException("Can't read " + clipTrackURI);
                    } catch (NotFoundException ex) {
                      throw new ProcessFailedException("Workspace does not contain a track " + clipTrackURI);
                    }
                  }
                  int index = -1;

                  if (clipSourceFile != null) {      // clip has different source
                    index = inputfile.indexOf(clipSourceFile.getAbsolutePath()); // Look for known tracks
                    if (index == -1) {
                      inputfile.add(clipSourceFile.getAbsolutePath()); // add new track
                     //TODO: inspect each new video file, bad input will throw exc
                    }
                    index = inputfile.indexOf(clipSourceFile.getAbsolutePath());
                  } else {
                    index = srcIndex; // default src
                  }

                  videoclips.add(new VideoClip(index, begin / 1000.0, end / 1000.0));
                }
              } else {
                throw new ProcessFailedException("Smil container '"
                        + ((SmilMediaContainer) elementChild).getContainerType().toString()
                        + "'is not supportet yet");
              }
            }
          } else {
            throw new ProcessFailedException("Smil container '"
                    + container.getContainerType().toString() + "'is not supportet yet");
          }
        }
      }
      List<VideoClip> cleanclips = sortSegments(videoclips);    // remove very short cuts that will look bad
      String error = null;
      String outputResolution = "";    //TODO: fetch the largest output resolution from SMIL.head.layout.root-layout
      // When outputResolution is set to WxH, all clips are scaled to that size in the output video.
      // TODO: Each clips could have a region id, relative to the root-layout
      // Then each clip is zoomed/panned/padded to WxH befor concatenation
      FFmpegEdit ffmpeg = new FFmpegEdit(properties);
      error = ffmpeg.processEdits(inputfile, outputPath.getAbsolutePath(), outputResolution, cleanclips);

      if (error != null) {
        FileUtils.deleteQuietly(outputPath.getParentFile());
        throw new ProcessFailedException("Editing pipeline exited abnormaly! Error: " + error);
      }

      // create Track for edited file
      String newTrackId = idBuilder.createNew().toString();
      InputStream in = new FileInputStream(outputPath);
      try {
        newTrackURI = workspace.putInCollection(COLLECTION_ID,
                String.format("%s-%s%s", sourceTrackFlavor.getType(), newTrackId, outputFileExtension), in);
      } catch (IllegalArgumentException ex) {
        throw new ProcessFailedException("Copy track into workspace failed! " + ex.getMessage());
      } finally {
        IOUtils.closeQuietly(in);
        FileUtils.deleteQuietly(outputPath.getParentFile());
      }
      //logger.debug("Copied the edited file from " + outputPath.toString() + " to workspace at " + String.format("%s-%s%s", sourceTrackFlavor.getType(), newTrackId, outputFileExtension) + " returns  " + newTrackURI.toString());

      // inspect new Track
      Job inspectionJob = null;
      try {
          inspectionJob = inspect(job,newTrackURI);
      } catch (MediaInspectionException e) {
          throw new ProcessFailedException("Media inspection of " + newTrackURI + " failed", e);
      }
      Track editedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      logger.info("edited FILE " + inspectionJob.getPayload());
      editedTrack.setIdentifier(newTrackId);
      editedTrack.setFlavor(new MediaPackageElementFlavor(sourceTrackFlavor.getType(), SINK_FLAVOR_SUBTYPE));

      return editedTrack;

    } catch (MediaInspectionException ex) {
      throw new ProcessFailedException("Inspecting encoded Track failed with: " + ex.getMessage());
    } catch (MediaPackageException ex) {
      throw new ProcessFailedException("Unable to serialize edited Track! " + ex.getMessage());
    } catch (Exception ex) {
      throw new ProcessFailedException(ex.getMessage());
    } finally {
      FileUtils.deleteQuietly(outputPath.getParentFile());
    }
  }

  /*
   * Inspect the output file
   */
  protected Job inspect(Job job, URI workspaceURI) throws MediaInspectionException, ProcessFailedException {
    Job inspectionJob;
    try {
        inspectionJob = inspectionService.inspect(workspaceURI);
    } catch (MediaInspectionException e) {
      incident().recordJobCreationIncident(job, e);
      throw new MediaInspectionException("Media inspection of " + workspaceURI + " failed", e);
    }
    JobBarrier barrier = new JobBarrier(serviceRegistry, inspectionJob);
    if (!barrier.waitForJobs().isSuccess()) {
      throw new ProcessFailedException("Media inspection of " + workspaceURI + " failed");
    }
    return inspectionJob;
  }

  /* Clean up the edit points, make sure they are at least 2 seconds apart (default fade duration)
   * Otherwise it can be very slow to run and output will be ugly because of the cross fades
   */
  private static List<VideoClip> sortSegments(List<VideoClip> edits) {
    LinkedList<VideoClip> ll = new LinkedList<VideoClip>();
    List<VideoClip> clips = new ArrayList<VideoClip>();
    Iterator<VideoClip> it = edits.iterator();
    VideoClip clip;
    VideoClip nextclip;
    while (it.hasNext()) {     // Check for legal durations
      clip = it.next();
      if (clip.getDuration() > 2) { // Keep segments at least 2 seconds long
        ll.add(clip);
      }
    }
    clip = ll.pop();        // initialize
    while (!ll.isEmpty()) { // Check that 2 consecutive segments from same src are at least 2 secs apart
      if (ll.peek() != null) {
        nextclip = ll.pop();  // check next consecutive segment
        if ((nextclip.getSrc() == clip.getSrc()) && (nextclip.getStart() - clip.getEnd()) < 2) { // collapse two segments into one
          clip.setEnd(nextclip.getEnd());                             // by using inpt of seg 1 and outpoint of seg 2
        } else {
          clips.add(clip);   // keep last segment
          clip = nextclip;   // check next segment
        }
      }
    }
    clips.add(clip); // add last segment
    return clips;
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.videoeditor.api.VideoEditorService#processSmil(org.opencastproject.smil.entity.Smil)
   */
  @Override
  public List<Job> processSmil(Smil smil) throws ProcessFailedException {
    if (smil == null) {
      throw new ProcessFailedException("Smil document is null!");
    }

    List<Job> jobs = new LinkedList<Job>();
    try {
      for (SmilMediaParamGroup paramGroup : smil.getHead().getParamGroups()) {
        for (SmilMediaParam param : paramGroup.getParams()) {
          if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.getName())) {
            jobs.add(serviceRegistry.createJob(getJobType(), Operation.PROCESS_SMIL.toString(),
                    Arrays.asList(smil.toXML(), paramGroup.getId())));
          }
        }
      }
      return jobs;
    } catch (JAXBException ex) {
      throw new ProcessFailedException("Failed to serialize smil " + smil.getId());
    } catch (ServiceRegistryException ex) {
      throw new ProcessFailedException("Failed to create job: " + ex.getMessage());
    } catch (Exception ex) {
      throw new ProcessFailedException(ex.getMessage());
    }
  }

  @Override
  protected String process(Job job) throws Exception {
    if (Operation.PROCESS_SMIL.toString().equals(job.getOperation())) {
      Smil smil = smilService.fromXml(job.getArguments().get(0)).getSmil();
      if (smil == null) {
        throw new ProcessFailedException("Smil document is null!");
      }

      Track editedTrack = processSmil(job, smil, job.getArguments().get(1));
      return MediaPackageElementParser.getAsXml(editedTrack);
    }

    throw new ProcessFailedException("Can't handle this operation: " + job.getOperation());
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
    storageDir = context.getBundleContext().getProperty("org.opencastproject.storage.dir");
    FFmpegEdit.init(context.getBundleContext());
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

  public void setMediaInspectionService(MediaInspectionService inspectionService) {
    this.inspectionService = inspectionService;
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
