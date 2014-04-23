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
package org.opencastproject.videoeditor.gstreamer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.lowlevel.MainLoop;
import org.opencastproject.videoeditor.gstreamer.exceptions.PipelineBuildException;
import org.opencastproject.videoeditor.gstreamer.sources.SourceBinsFactory;
import org.opencastproject.videoeditor.impl.VideoEditorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gstreamer pipeline factory for VideoEditorService.
 */
public class VideoEditorPipeline {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(VideoEditorPipeline.class);

  private static final boolean DEBUG_OUTPUT_ONLY = false;

  public static final String DEFAULT_AUDIO_ENCODER = GstreamerElements.FAAC;
  public static final String DEFAULT_AUDIO_ENCODER_PROPERTIES = "bitrate=128000";

  public static final String DEFAULT_VIDEO_ENCODER = GstreamerElements.X264ENC;
  public static final String DEFAULT_VIDEO_ENCODER_PROPERTIES = "";

  public static final String DEFAULT_MUX = GstreamerElements.MP4MUX;
  public static final String DEFAULT_MUX_PROPERTIES = "";
  public static final String DEFAULT_OUTPUT_FILE_EXTENSION = ".mp4";

  private Properties properties;
  private Pipeline pipeline;
  private MainLoop mainLoop = new MainLoop();
  private String lastErrorMessage = null;

  public VideoEditorPipeline(Properties properties) {
    this.properties = properties != null ? properties : new Properties();

    pipeline = new Pipeline();
  }

  /**
   * Run Gstreamer pipeline. This method blocks until pipline is finished.
   */
  public void run() {
    logger.debug("starting pipeline...");
    pipeline.play();
    mainLoop.run();
    logger.debug("main loop quit!");
    stop();
  }

  /**
   * Stop Gstreamer pipeline.
   *
   * @return
   */
  public void stop() {
    if (pipeline == null) {
      return;
    }
    pipeline.setState(State.NULL);
  }

  /**
   * Add Gstreamer event listeners to pipeline.
   */
  protected void addListener() {
    pipeline.getBus().connect(new Bus.INFO() {
      /**
       * {@inheritDoc}
       *
       * @see org.gstreamer.Bus.INFO#infoMessage(org.gstreamer.GstObject, int,
       * java.lang.String)
       */
      @Override
      public void infoMessage(GstObject source, int code, String message) {
        logger.debug("INFO from {}: ", source.getName(), message);
      }
    });

    pipeline.getBus().connect(new Bus.ERROR() {
      /**
       * {@inheritDoc}
       *
       * @see org.gstreamer.Bus.ERROR#errorMessage(org.gstreamer.GstObject, int,
       * java.lang.String)
       */
      @Override
      public void errorMessage(GstObject source, int code, String message) {
        logger.warn("ERROR from {}: ", source.getName(), message);
        lastErrorMessage = String.format("%s: %s", source.getName(), message);
        mainLoop.quit();
        Gst.quit();
      }
    });

    pipeline.getBus().connect(new Bus.EOS() {
      /**
       * {@inheritDoc}
       *
       * @see org.gstreamer.Bus.EOS#endOfStream(org.gstreamer.GstObject)
       */
      @Override
      public void endOfStream(GstObject source) {
        logger.debug("EOS from {}: stop pipeline", new String[]{source.getName()});
        mainLoop.quit();
        Gst.quit();
      }
    });

    pipeline.getBus().connect(new Bus.WARNING() {
      /**
       * {@inheritDoc}
       *
       * @see org.gstreamer.Bus.WARNING#warningMessage(org.gstreamer.GstObject,
       * int, java.lang.String)
       */
      @Override
      public void warningMessage(GstObject source, int code, String message) {
        logger.warn("WARNING from {}: ", source.getName(), message);
      }
    });

    pipeline.getBus().connect(new Bus.STATE_CHANGED() {
      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {
        if (source instanceof Pipeline) {
          logger.debug("{} changed state to {}", new String[]{
            source.getName(), current.toString()
          });
          if (current == State.READY || current == State.PLAYING) {
            pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS | Pipeline.DEBUG_GRAPH_SHOW_STATES,
                    "videoeditor-pipeline-" + current.name(), true);
          }
        }
      }
    });
  }

  /**
   * Returns Gstreamer pipeline state.
   *
   * @return pipline state
   */
  public State getState() {
    return getState(0);
  }

  /**
   * Returns Gstreamer pipeline state with an timeout (in milliseconds) for
   * Gstreamer elements performed the async state change.
   *
   * @param timeoutMillis timeout in milliseconds
   * @return pipeline state
   */
  public State getState(long timeoutMillis) {
    if (pipeline == null) {
      return State.NULL;
    }
    return pipeline.getState(TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
  }

  /**
   * Create an Gstreamer pipeline with all source bins from {
   *
   * @see SourceBinsFactory} inside.
   * @param sourceBins source bins factory
   * @throws PipelineBuildException if pipeline build fails
   */
  public void addSourceBinsAndCreatePipeline(SourceBinsFactory sourceBins)
          throws PipelineBuildException {

    if (DEBUG_OUTPUT_ONLY) {
      addSourceBinsAndCreateDebugPipeline(sourceBins);
      return;
    }

    // create and link muxer and filesink
    Element muxer = createMux();
    Element fileSink = ElementFactory.make(GstreamerElements.FILESINK, null);
    pipeline.addMany(muxer, fileSink);

    if (!muxer.link(fileSink)) {
      throw new PipelineBuildException();
    }

    fileSink.set("location", sourceBins.getOutputFilePath());
    fileSink.set("sync", false);

    Bin sourceBin;
    Element capsfilter;
    Element encoder;

    if (sourceBins.hasAudioSource()) {
      // create and link audio bin and audio encoder
      sourceBin = sourceBins.getAudioSourceBin();
      capsfilter = ElementFactory.make(GstreamerElements.CAPSFILTER, "audiocaps");
      encoder = createAudioEncoder();
      pipeline.addMany(sourceBin, capsfilter, encoder);
      if (!Element.linkMany(sourceBin, capsfilter, encoder, muxer)) {
        throw new PipelineBuildException();
      }

      if (properties.containsKey(VideoEditorProperties.AUDIO_CAPS)) {
        capsfilter.setCaps(Caps.fromString(properties.getProperty(VideoEditorProperties.AUDIO_CAPS)));
      }
      // remove java references to elements
      capsfilter.disown();
      encoder.disown();
      sourceBin.disown();
    }

    if (sourceBins.hasVideoSource()) {
      // create and link video bin and audio encoder
      sourceBin = sourceBins.getVideoSourceBin();
      capsfilter = ElementFactory.make(GstreamerElements.CAPSFILTER, "videocaps");
      encoder = createVideoEncoder();
      pipeline.addMany(sourceBin, capsfilter, encoder);
      if (!Element.linkMany(sourceBin, capsfilter, encoder, muxer)) {
        throw new PipelineBuildException();
      }

      if (properties.containsKey(VideoEditorProperties.VIDEO_CAPS)) {
        capsfilter.setCaps(Caps.fromString(properties.getProperty(VideoEditorProperties.VIDEO_CAPS)));
      }
      // remove java references to elements
      capsfilter.disown();
      encoder.disown();
      sourceBin.disown();
    }

    // add Gstreamer event listener
    addListener();

    // remove java references to elements
    muxer.disown();
    fileSink.disown();
  }

  /**
   * Returns last error message.
   *
   * @return last error message
   */
  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  /**
   * Create and setup audio encoder.
   *
   * @return Gstreamer audio encoder
   */
  protected Element createAudioEncoder() {
    String encoder = properties.getProperty(VideoEditorProperties.AUDIO_ENCODER, DEFAULT_AUDIO_ENCODER);
    String encoderProperties = properties.getProperty(VideoEditorProperties.AUDIO_ENCODER_PROPERTIES, DEFAULT_AUDIO_ENCODER_PROPERTIES);

    Element encoderElem = ElementFactory.make(encoder, null);
    Map<String, String> encoderPropertiesDict = getPropertiesFromString(encoderProperties);

    for (String key : encoderPropertiesDict.keySet()) {
      encoderElem.set(key, encoderPropertiesDict.get(key));
    }

    return encoderElem;
  }

  /**
   * Create and setup video encoder.
   *
   * @return Gstreamer video encoder
   */
  protected Element createVideoEncoder() {
    String encoder = properties.getProperty(VideoEditorProperties.VIDEO_ENCODER, DEFAULT_VIDEO_ENCODER);
    String encoderProperties = properties.getProperty(VideoEditorProperties.VIDEO_ENCODER_PROPERTIES, DEFAULT_VIDEO_ENCODER_PROPERTIES);

    Element encoderElem = ElementFactory.make(encoder, null);
    Map<String, String> encoderPropertiesDict = getPropertiesFromString(encoderProperties);

    for (String key : encoderPropertiesDict.keySet()) {
      encoderElem.set(key, encoderPropertiesDict.get(key));
    }

    return encoderElem;
  }

  /**
   * Create and setup muxer.
   *
   * @return Gstreamer muxer
   */
  protected Element createMux() {
    String mux = properties.getProperty(VideoEditorProperties.MUX, DEFAULT_MUX);
    String muxProperties = properties.getProperty(VideoEditorProperties.MUX_PROPERTIES, DEFAULT_MUX_PROPERTIES);

    Element muxElem = ElementFactory.make(mux, null);
    Map<String, String> muxPropertiesDict = getPropertiesFromString(muxProperties);

    for (String key : muxPropertiesDict.keySet()) {
      muxElem.set(key, muxPropertiesDict.get(key));
    }

    return muxElem;
  }

  /**
   * Parse element properties from string.
   *
   * @param encoderProperties element properties
   * @return element properties as map
   */
  private static Map<String, String> getPropertiesFromString(String encoderProperties) {
    Map<String, String> properties = new HashMap<String, String>();

    for (String prop : encoderProperties.trim().split(" ")) {
      if (prop.isEmpty()) {
        break;
      }

      if (prop.trim().split("=").length == 2) {
        properties.put(prop.trim().split("=")[0], prop.trim().split("=")[1]);
      }
    }

    return properties;
  }

  private void addSourceBinsAndCreateDebugPipeline(SourceBinsFactory sourcebins) throws PipelineBuildException {

    if (sourcebins.hasAudioSource()) {
      Element audiosink = ElementFactory.make(GstreamerElements.AUTOAUDIOSINK, null);
      pipeline.addMany(sourcebins.getAudioSourceBin(), audiosink);

      if (!sourcebins.getAudioSourceBin().link(audiosink)) {
        throw new PipelineBuildException();
      }
    }

    if (sourcebins.hasVideoSource()) {
      Element videosink = ElementFactory.make(GstreamerElements.AUTOVIDEOSINK, null);
      pipeline.addMany(sourcebins.getVideoSourceBin(), videosink);

      if (!sourcebins.getVideoSourceBin().link(videosink)) {
        throw new PipelineBuildException();
      }
    }
  }
}
