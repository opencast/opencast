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

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.DecodeBin;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * Splits video streams from NCast cards.
 * 
 * Video streams from NCast cards can come with two streams in a single file. The entire
 * video is a 1792x1028 stream containing two smaller streams, one of size 1024x768 on the
 * upper right side, and one 768x576 on the upper left side. This operation handler splits
 * the original stream into two new streams using GStreamer and injects them back into the
 * MediaPackage.
 * 
 */
public class NCASTVideoSplitWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** Gstreamer file source. */
	private Element fileSrc;
	/** Gstreamer video stream demuxer. */
	private Element videoStreamDemuxer;
	/** Muxer element for the right video stream. */
	private final Element rightVideoMuxer;
	/** Muxer element for the left video stream. */
	private final Element leftVideoMuxer;
	/** File sink for the left video stream. */
	private Element leftVideoFileSink;
	/** File sink for the right video stream. */
	private Element rightVideoFileSink; 
	/** Decode element for the original video stream. */
	private final DecodeBin videoDecoder; 
	/** Tee for the original video stream. */
	private final Element videoTee; 
	/** Tee for the original audio stream. */
	private final Element audioTee; 
	/** Crop element for the right video stream. */
	private final Element videoCropRight;
	/** Crop element for the left video stream. */
	private final Element videoCropLeft; 
	/** x264 encoder for the left video stream. */
	private final Element videoEncoder; 
	/** x264 encoder for the right video stream. */
	private final Element videoEncoder2; 
	/** Video and audio queues. */
	private final Element videoQueue1; 
	private final Element videoQueue2; 
	private final Element audioQueue; 
	private final Element audioQueue2; 
	private Bin bin; 
	
	/** Locations to write intermediate files obtained after splitting. */
	private String localStorageDirectory;
	private String localSplitTrackOutputFileName1;
	private String localSplitTrackOutputFileName2;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NCASTVideoSplitWorkflowOperationHandler.class);
	private Workspace workspace;
	protected ComponentContext componentContext;
	protected BundleContext bundleContext;
	private MediaInspectionService inspectionService;
	
	/**
	 * Create all of the GStreamer elements.
	 */
	public NCASTVideoSplitWorkflowOperationHandler() {
	  super();
	  Gst.init();
	  fileSrc = ElementFactory.make("filesrc", "file");
	  videoStreamDemuxer = ElementFactory.make("qtdemux", "demux"); 
	  rightVideoMuxer = ElementFactory.make("mp4mux", "mux0"); 
	  leftVideoMuxer = ElementFactory.make("mp4mux", "mux1"); 
	  leftVideoFileSink = ElementFactory.make("filesink", "sink1"); 
  	rightVideoFileSink = ElementFactory.make("filesink", "sink2"); 
  	videoDecoder = new DecodeBin("decodebin"); 
  	videoTee = ElementFactory.make("tee", "videoTee"); 
  	audioTee = ElementFactory.make("tee", "audioTee"); 
  	videoCropRight = ElementFactory.make("videocrop", "cropRight");
  	videoCropLeft = ElementFactory.make("videocrop", "cropLeft"); 
  	videoEncoder = ElementFactory.make("x264enc", "encode1"); 
  	videoEncoder2 = ElementFactory.make("x264enc", "encode2"); 
  	videoQueue1 = ElementFactory.make("queue", "videoQueue"); 
  	videoQueue2 = ElementFactory.make("queue", "videoQueue2"); 
  	audioQueue = ElementFactory.make("queue", "audioQueue"); 
  	audioQueue2 = ElementFactory.make("queue", "audioQueue1"); 
  	bin = new Bin(); 
	}
   
	/**
	 * Set the local inspection service.
	 * 
	 * @param service
	 */
	public void setInspectionService(MediaInspectionService service) {
	  inspectionService = service;
	}
	
	/**
	 * Set the workspace.
	 * @param workspace The new workspace.
	 */
   public void setWorkspace(Workspace workspace) {
      this.workspace = workspace;
   }
   
   /**
    * {@inheritDoc}
    * 
    * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
    */
   public void activate(ComponentContext componentContext) {
     this.componentContext = componentContext;
     bundleContext = componentContext.getBundleContext();
   }
   
   /**
    * Set the paths for the location for the intermediate files to write to.
    * 
    * @param oldTrackFileName The name of the original track file.
    */
   public void setIntermediateOutputPaths(String oldTrackFileName) {
     localStorageDirectory = bundleContext.getProperty("org.opencastproject.storage.dir");
     localSplitTrackOutputFileName1 = localStorageDirectory + "/" + "split1_" + oldTrackFileName;
     localSplitTrackOutputFileName2 = localStorageDirectory + "/" + "split2_" + oldTrackFileName;
   }
   
   /**
    * {@inheritDoc}
    */
   public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
      MediaPackage originalPackage = workflowInstance.getMediaPackage();
      logger.debug("Running NCAST Video Split");
      try {
        WorkflowOperationResult result = split(originalPackage, workflowInstance.getCurrentOperation());
    	  return result;
      } catch (Exception e) {
    	  throw new WorkflowOperationException(e);
      }
   }

   /**
    * Split any video tracks inside the given MediaPackage into two separate tracks.
    * 
    * @param src The input media package.
    * @param operation The operation instance.
    * @return An operation result containing the MediaPackage with the new, split tracks added.
    * @throws Exception
    */
   private WorkflowOperationResult split(MediaPackage src, WorkflowOperationInstance operation) throws Exception {
      MediaPackage mp = (MediaPackage) src.clone();
      String sourceMediaFlavor = operation.getConfiguration("source-media-flavor");
      logger.info("Getting all Quicktime tracks from the MediaPackage.");
      Track[] qtTracks = getQuickTimeTracks(mp, MediaPackageElementFlavor.parseFlavor(sourceMediaFlavor));
      if (qtTracks.length == 0) {
        logger.info("Skipping split as no suitable video tracks were found.");
        throw new Exception("No suitable tracks found.");
      }
      for (Track t : qtTracks) {
    	  Track[] returnedTracks = splitTrack(mp, t);
    	  for (Track derived : returnedTracks) {
    		  mp.addDerived(derived, t);
    	  }
      }
      logger.info("Returning modified media package.");
      return createResult(mp, Action.CONTINUE);
   }

   /** Split the given NCast Track into two tracks, each containing their own video feed.
    *
    * @param mp
    * The media package that the original track belongs too.
    *
    * @param qtTrack
    * The offered track for splitting.
    *
    * @return Array of length two containing the split tracks.
    * @throws IOException 
    * @throws FileNotFoundException 
    * @throws NotFoundException 
    * @throws WorkflowOperationException 
    */
   private Track[] splitTrack(MediaPackage mp, Track qtTrack) throws FileNotFoundException, IOException, NotFoundException, WorkflowOperationException {
     File file = workspace.get(qtTrack.getURI());
     setIntermediateOutputPaths(file.getName());
     // pair of local files returned from splitting the original file
     File[] returned = null;
     try {
       returned = splitFiles(file);
     } catch (RuntimeException exc) { // Error ocurred in the gstreamer pipeline.
       throw new WorkflowOperationException(exc);
     }
     // Upload the new files to the workspace.
     URI remoteFileURI1 = workspace.put(mp.getIdentifier().toString(), qtTrack.getIdentifier(), "tmp1", new FileInputStream(returned[0]));
     URI remoteFileURI2 = workspace.put(mp.getIdentifier().toString(), qtTrack.getIdentifier(), "tmp2", new FileInputStream(returned[1]));
     Track[] splitTracks = new Track[2];
     // Create new tracks for each of the files, and enrich the metadata.
     logger.info("Building the new tracks.");
     MediaPackageElementBuilder builder = new MediaPackageElementBuilderImpl();
     splitTracks[0] = (Track) builder.elementFromURI(remoteFileURI1, Type.Track, MediaPackageElements.PRESENTER_SOURCE);
     splitTracks[0].setMimeType(qtTrack.getMimeType());
     splitTracks[0] = enrichNewTrackMetaData(splitTracks[0]);
     splitTracks[1] = (Track) builder.elementFromURI(remoteFileURI2, Type.Track, MediaPackageElements.PRESENTER_SOURCE);
     splitTracks[1].setMimeType(qtTrack.getMimeType());
     splitTracks[1] = enrichNewTrackMetaData(splitTracks[1]);
     return splitTracks;
   }

   /**
    * Given an input track, use the inspection service to enrich it's metadata.
    * @param inputTrack The output track.
    * @return The enriched track.
    * 
    * @throws WorkflowOperationException
    */
   private Track enrichNewTrackMetaData(Track inputTrack) throws WorkflowOperationException {
     Job trackInspectionJob = null;
     Track outputTrack = null;
     try {
       trackInspectionJob = inspectionService.inspect(inputTrack.getURI());
       if (!waitForStatus(trackInspectionJob).isSuccess()) {
         throw new WorkflowOperationException("Error while inspecting new track " + inputTrack);
       }
       String in = trackInspectionJob.getPayload();
       outputTrack = (Track) MediaPackageElementParser.getFromXml(in);
     } catch (MediaInspectionException exc) {
       logger.error("Error while inspecting track " + inputTrack);
       logger.error(exc.getMessage());
       throw new WorkflowOperationException(exc);
     } catch (MediaPackageException exc) {
       logger.error("Error while inspecting track " + inputTrack);
       logger.error(exc.getMessage());
       throw new WorkflowOperationException(exc);
     }
     return outputTrack;
   }
   
   /**
    * Initialise the properties of the gstreamer elements.
    */
   private void initializeGStreamerElements() {
     leftVideoFileSink.set("location", localSplitTrackOutputFileName1); 
     rightVideoFileSink.set("location", localSplitTrackOutputFileName2);
     videoCropLeft.set("left", 1024); 
     videoCropLeft.set("bottom", 192);
     videoCropRight.set("right", 768);
     videoQueue1.set("max-size-buffers", 0); 
     videoQueue1.set("max-size-time", 0); 
     videoQueue1.set("max-size-bytes", 0); 
     videoQueue2.set("max-size-buffers", 0); 
     videoQueue2.set("max-size-time", 0); 
     videoQueue2.set("max-size-bytes", 0); 
     audioQueue.set("max-size-buffers", 0); 
     audioQueue.set("max-size-time", 0); 
     audioQueue.set("max-size-bytes", 0); 
     audioQueue2.set("max-size-buffers", 0); 
     audioQueue2.set("max-size-time", 0); 
     audioQueue2.set("max-size-bytes", 0); 
   }
   
   /**
    * Add all of the defined elements to the bin.
    */
   private void addElementsToBin() { 
	   bin.addMany(fileSrc, videoStreamDemuxer, rightVideoMuxer, leftVideoMuxer, rightVideoFileSink, videoTee, audioTee, 
			   videoDecoder, videoCropRight, videoCropLeft, videoEncoder, 
			   videoEncoder2, videoQueue2, audioQueue, videoQueue1, leftVideoFileSink, 
			   audioQueue2); 
   } 
   
   /**
    * Create the static links inside the bin.
    * @return true if the elements were linked successfully.
    */
   private boolean createStaticLinks() { 
	   boolean connected = true; 
	   // filesrc ! demux 
	   if (!fileSrc.link(videoStreamDemuxer)) { 
		   connected = false; 
	   } 
	   // mp4mux ! filesink 
	   if (!rightVideoMuxer.link(leftVideoFileSink)) { 
		   connected = false; 
	   } 
	   if (!leftVideoMuxer.link(rightVideoFileSink)) { 
		   connected = false; 
	   } 
	   return connected; 
   } 
   
   /**
    * Create and link the pads on the qtdemux element.
    */
   private void createDemuxLink() { 
	   videoStreamDemuxer.connect(new Element.PAD_ADDED() { 
		   public void padAdded(Element element, Pad pad) { 
			   if ("video_00".equals(pad.getName())) { 
				   // demux.video_00 ! decodebin 
				   pad.link(videoDecoder.getStaticPad("sink")); 
			   } else if ("audio_00".equals(pad.getName())) { 
				   // demux.audio_00 ! queue 
				   pad.link(audioTee.getStaticPad("sink")); 
				   audioTee.getRequestPad("src0").link(audioQueue.getStaticPad("sink")); 
				   audioTee.getRequestPad("src1").link(audioQueue2.getStaticPad("sink")); 
				   // queue ! mp4mux 
				   audioQueue.getStaticPad("src").link(rightVideoMuxer.getRequestPad("audio_0")); 
				   audioQueue2.getStaticPad("src").link(leftVideoMuxer.getRequestPad("audio_0")); 
			   } 
		   } 
	   }); 
   } 
   
   /**
    * Create and link the pads on the decodebin element.
    */
   private void createDecoderLink() { 
	   videoDecoder.connect(new DecodeBin.NEW_DECODED_PAD() { 
		   public void newDecodedPad(Element element, Pad pad, boolean last) { 
			   // decodebin ! videocrop right=480 
	    		  pad.link(videoTee.getStaticPad("sink")); 
	    		  videoTee.getRequestPad("src0").link(videoCropRight.getStaticPad("sink")); 
	    		  videoTee.getRequestPad("src1").link(videoCropLeft.getStaticPad("sink")); 
	    		  // videocrop right=480 ! x264enc 
	    		  videoCropRight.link(videoEncoder); 
	    		  videoCropLeft.link(videoEncoder2); 
	    		  // x264enc ! queue 
	    		  videoEncoder.link(videoQueue1); 
	    		  videoEncoder2.link(videoQueue2); 
	    		  // queue ! mp4mux 
	    		  videoQueue1.getStaticPad("src").link(rightVideoMuxer.getRequestPad("video_0")); 
	    		  videoQueue2.getStaticPad("src").link(leftVideoMuxer.getRequestPad("video_0")); 
	    	  } 
	      }); 
   } 
   
   /**
    * Given a video file ingested from an NCast card, split the video file
    * into two separate video files, one for each stream present in the original
    * file. 
    * 
    * @param filename The input video file.
    * @return A file array containing the two new video files.
    */
   private File[] splitFiles(File filename) throws RuntimeException {
     logger.info("Beginning GStreamer split on " + filename);
     fileSrc.set("location", filename.toString());
     initializeGStreamerElements();
     addElementsToBin();
     createStaticLinks();
     createDemuxLink();
     createDecoderLink();
     Pipeline pipeline = new Pipeline();
     Bus pipelineBus = pipeline.getBus(); 
     pipelineBus.connect(new Bus.ERROR() { 
       public void errorMessage(GstObject source, int code, String message) {
         logger.error("Error in pipeline. Source object: " + source.toString() + ".\n Message: " + message);
         Gst.quit();
         Gst.deinit();
         throw new RuntimeException("Error in GStreamer pipeline during splitting. Source object: " + source + ".\n Message: " + message);
       } 
     });
     pipelineBus.connect(new Bus.EOS() {
       public void endOfStream(GstObject source) {
           // Stream is finished playing. QUIT!
           Gst.quit();
       }
     });
     pipeline.add(bin); 
     pipeline.pause(); 
     try { 
    	 Thread.sleep(5000); 
     } catch (InterruptedException e) { 
    	 e.printStackTrace();
     } 
     pipeline.play(); 
     Gst.main(); 
     pipeline.setState(State.NULL); 
     Gst.deinit();
     logger.info("Pipeline finished. Creating tracks.");
     File[] returnList = new File[2];
     returnList[0] = new File(localSplitTrackOutputFileName1);
     returnList[1] = new File(localSplitTrackOutputFileName2);
     return returnList;
   }

   /**
    * Searches for QuickTime files with specified flavor.
    * 
    * @param mediaPackage
    *          media package to be searched
    * @param flavor
    *          track flavor to be searched for
    * @return array of suitable tracks
    */
   private Track[] getQuickTimeTracks(MediaPackage mediaPackage, MediaPackageElementFlavor flavor) {
     Track[] tracks = mediaPackage.getTracks(flavor);
     List<Track> qtTrackList = new LinkedList<Track>();
     for (Track t : tracks) {
       if (t.getMimeType().isEquivalentTo("video", "quicktime") && t.hasVideo()) {
         qtTrackList.add(t);
       }
     }
     return qtTrackList.toArray(new Track[qtTrackList.size()]);
   }
}
