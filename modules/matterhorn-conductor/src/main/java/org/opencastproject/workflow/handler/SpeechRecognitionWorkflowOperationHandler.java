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

package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.speechrecognition.api.SpeechRecognitionService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics.ListMonadic;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencastproject.util.data.Monadics.mlist;

/**
 * {@link org.opencastproject.workflow.api.WorkflowOperationHandler} to let a service like <a
 * href="http://www.koemei.com/">Koemei Speech Recognition</a> transcribe audio tracks in the media package.
 */
public class SpeechRecognitionWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionWorkflowOperationHandler.class);

  public static final MediaPackageElementFlavor AUDIO_FLAVOR = new MediaPackageElementFlavor("speech", "work");

  private SpeechRecognitionService speechRecognitionService;

  /**
   * Injected by OSGi environment.
   */
  public void setSpeechRecognitionService(SpeechRecognitionService speechRecognitionService) {
    this.speechRecognitionService = speechRecognitionService;
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    // check media package for suitable audio track and submit it to the SpeechRecognitionService

    ListMonadic<Track> mTracks = mlist(workflowInstance.getMediaPackage().getTracks()).filter(filterByFlavor);
    List<Track> tracks = mTracks.value();

    if (tracks.isEmpty()) {
      // No audio track available - log a message
      logger.info("media package contains no processable audio track");
      return createResult(WorkflowOperationResult.Action.CONTINUE);
    }

    // Send all tracks to the transcription service
    List<Job> transcriptionJobs = new ArrayList<Job>();
    for (Track track : tracks) {
      // Found a track - send it to speech recognition service
      logger.info("Sending track {} to SpeechRecognitionService", track);
      Job job = speechRecognitionService.transcribe(track, workflowInstance.getMediaPackage().getLanguage());
      transcriptionJobs.add(job);
    }

    // Wait for all jobs to be finished (or failed)
    logger.info("SpeechRecognitionService is waiting for {} transcription job(s) to return", transcriptionJobs.size());
    final JobBarrier.Result result = waitForStatus(transcriptionJobs.toArray(new Job[transcriptionJobs.size()]));

    for (Job job : transcriptionJobs) {
      if (result.isSuccess()) {
        Job newJob;
        try {
          newJob = serviceRegistry.getJob(job.getId());
        } catch (NotFoundException e) {
          throw new WorkflowOperationException(e);
        } catch (ServiceRegistryException e) {
          throw new WorkflowOperationException(e);
        }
        Catalog speechCatalog;
        try {
          speechCatalog = (Catalog) MediaPackageElementParser.getFromXml(newJob.getPayload());
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException(e);
        }
        workflowInstance.getMediaPackage().add(speechCatalog);
        return createResult(WorkflowOperationResult.Action.CONTINUE);
      } else
        throw new RuntimeException("Job terminated unsuccessfully");
    }

    return createResult(WorkflowOperationResult.Action.CONTINUE);
  }

  /**
   * Filter tracks of flavor {@link #AUDIO_FLAVOR}.
   */
  private static final Function<Track, Boolean> filterByFlavor = new Function<Track, Boolean>() {
    @Override
    public Boolean apply(Track track) {
      return AUDIO_FLAVOR.equals(track.getFlavor());
    }
  };

}
