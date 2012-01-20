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
package org.opencastproject.composer.gstreamer.engine;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.gstreamer.AbstractGSEncoderEngine;

import org.apache.commons.io.FilenameUtils;
import org.gstreamer.Buffer;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

/**
 * Encoder engine that uses GStreamer for encoding.
 */
public class GStreamerEncoderEngine extends AbstractGSEncoderEngine {

  /** Suffix for gstreamer pipeline template and image extraction */
  protected static final String GS_SUFFIX = "gstreamer.pipeline";
  protected static final String GS_IMAGE_TEMPLATE = "gstreamer.image.extraction";

  /** Logger utility */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerEncoderEngine.class);

  // constants used in gstreamer
  private static final String START_TIME = "start-time";
  // private static final String END_TIME = "end-time";
  private static final int GS_SEEK_FLAGS = SeekFlags.FLUSH;

  /** Rendering hints for resizing images */
  private static final Map<Key, Object> imageRenderingHints = new HashMap<RenderingHints.Key, Object>();
  static {
    imageRenderingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.gstreamer.AbstractGSEncoderEngine#createAndLaunchPipeline(org.opencastproject.composer
   * .api.EncodingProfile, java.util.Map)
   */
  @Override
  protected void createAndLaunchPipeline(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    logger.info("Creating pipeline definition from: {}", profile.getIdentifier());
    String pipelineDefinition = buildGStreamerPipelineDefinition(profile, properties);
    logger.debug("Creating pipeline from: {}", pipelineDefinition);
    GSPipeline pipeline = createPipeline(pipelineDefinition);
    logger.debug("Executing pipeline built from: {}", pipelineDefinition);
    if (properties.containsKey(START_TIME)) {
      launchPipeline(pipeline, Integer.parseInt(properties.get(START_TIME)));
    } else {
      launchPipeline(pipeline, 0);
    }
    logger.info("Execution successful");
  }

  /**
   * Builds string representation of gstreamer pipeline by substituting templates from pipeline template with actual
   * values from properties. Template format is #{property.name}. All unmatched properties are removed.
   * 
   * @param profile
   *          EncodingProfile used for this encoding job
   * @param properties
   *          Map that contains substitutions for templates
   * @return String representation of gstreamer pipeline
   * @throws EncoderException
   *           if profile does not contain gstreamer template
   */
  private String buildGStreamerPipelineDefinition(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    String pipelineTemplate = profile.getExtension(GS_SUFFIX);
    if (pipelineTemplate == null) {
      logger.warn("Profile {} does not contain gstreamer pipeline template.", profile.getName());
      throw new EncoderException("Profile " + profile.getName() + " does not contain gstreamer pipeline template");
    }

    // substitute templates for actual values
    String tmpPipelineDefinition = substituteTemplateValues(pipelineTemplate, properties, false);
    String pipelineDefinition = substituteTemplateValues(tmpPipelineDefinition, properties, true);

    return pipelineDefinition;
  }

  /**
   * Creates GSPipeline that contains gstreamer Pipeline with its MonitorObject from string representation of pipeline.
   * Syntax is equivalent to the gstreamer command line syntax.
   * 
   * @param pipelineDefinition
   *          String representation of gstreamer pipeline
   * @return GSPipeline that contains built Pipeline and MonitorObject
   * @throws EncoderException
   *           if Pipeline cannot be constructed from pipeline definition
   */
  private GSPipeline createPipeline(String pipelineDefinition) throws EncoderException {

    if (pipelineDefinition == null || "".equals(pipelineDefinition)) {
      logger.warn("No pipeline definition specified.");
      throw new EncoderException("Pipeline definition is null");
    }

    Pipeline pipeline;
    try {
      logger.debug("processing pipeline: {}", new String[] { pipelineDefinition });
      pipeline = Pipeline.launch(pipelineDefinition);
    } catch (Throwable t) {
      logger.warn("Could not create pipeline from definition \"{}\": {}", pipelineDefinition, t.getMessage());
      throw new EncoderException("Unable to create pipeline from: " + pipelineDefinition, t);
    }
    if (pipeline == null) {
      logger.warn("No pipeline was created from \"{}\"", pipelineDefinition);
      throw new EncoderException("No pipeline was created from: " + pipelineDefinition);
    }

    MonitorObject monitorObject = createNewMonitorObject();
    installListeners(pipeline, monitorObject);

    return createNewGSPipeline(pipeline, monitorObject);
  }

  /**
   * Executes GSPipeline. Blocks until either exception occurs in processing pipeline or EOS is reached. Optionally you
   * can specify start position from where pipeline should start playing.
   * 
   * @param gspipeline
   *          GSPipeline used for execution
   * @param startPosition
   *          start position in seconds
   * @throws EncoderException
   *           if current thread is interrupted, exception occurred in processing pipeline or pipeline could not play
   *           from specified time position
   */
  private void launchPipeline(GSPipeline gspipeline, int startPosition) throws EncoderException {

    // start position is in seconds
    if (startPosition < 0) {
      logger.warn("Starting position is not a positive number");
      throw new EncoderException("Invalid start position: " + startPosition);
    }

    if (Thread.interrupted()) {
      logger.warn("Failed to start processing pipeline: Thread interrupted");
      throw new EncoderException("Failed to start processing pipeline: Thread interrupted");
    }

    // FIXME revise and test code
    if (startPosition > 0) {
      gspipeline.getPipeline().pause();
      gspipeline.getPipeline().getState();
      if (!gspipeline.getPipeline().seek(1.0, Format.TIME, SeekFlags.FLUSH | SeekFlags.ACCURATE, SeekType.SET,
              startPosition * 1000 * 1000, SeekType.NONE, -1)) {
        gspipeline.getPipeline().stop();
        logger.warn("Could not start pipeline from: {} s", startPosition);
        throw new EncoderException("Failed to set " + startPosition + " as new start position");
      }
    }

    gspipeline.getPipeline().play();
    synchronized (gspipeline.getMonitorObject().getMonitorLock()) {
      try {
        while (!gspipeline.getMonitorObject().getEOSReached())
          gspipeline.getMonitorObject().getMonitorLock().wait();
      } catch (InterruptedException e) {
        logger.warn("Thread interrupted while processing");
        throw new EncoderException("Could not finish processing", e);
      } finally {
        gspipeline.getPipeline().stop();
      }
    }

    String errorMessage = gspipeline.getMonitorObject().getFirstErrorMessage();
    if (errorMessage != null) {
      logger.warn("Errors in processing pipeline");
      throw new EncoderException("Error occurred in processing pipeline: " + errorMessage);
    }
  }
  
  /**
   * Install various listeners to Pipeline, such as: ERROR, WARNING, INFO, STATE_CHANGED and EOS.
   * 
   * @param pipeline
   *          Pipeline to which listeners will be installed
   * @param monitorObject
   *          MonitorObject used for monitoring state of pipeline: errors and EOS
   */
  private void installListeners(Pipeline pipeline, final MonitorObject monitorObject) {
    pipeline.getBus().connect(new Bus.ERROR() {
      @Override
      public void errorMessage(GstObject source, int code, String message) {
        String errorMessage = source.getName() + ": " + code + " - " + message;
        monitorObject.addErrorMessage(errorMessage);
        logger.error("Error in processing pipeline: {}", errorMessage);
        // terminate pipeline immediately
        monitorObject.setStopPipeline(true);
        synchronized (monitorObject.getMonitorLock()) {
          monitorObject.getMonitorLock().notifyAll();
        }
      }
    });
    pipeline.getBus().connect(new Bus.WARNING() {
      @Override
      public void warningMessage(GstObject source, int code, String message) {
        logger.warn("Warning in processing pipeline: {}: {} - {}",
                new String[] { source.getName(), Integer.toString(code), message });
      }
    });
    pipeline.getBus().connect(new Bus.INFO() {
      @Override
      public void infoMessage(GstObject source, int code, String message) {
        logger.info("{}: {} - {}", new String[] { source.getName(), Integer.toString(code), message });
      }
    });
    pipeline.getBus().connect(new Bus.STATE_CHANGED() {
      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {
        logger.debug("{}: State changed from {} to {}",
                new String[] { source.getName(), old.toString(), current.toString() });
      }
    });
    pipeline.getBus().connect(new Bus.EOS() {
      @Override
      public void endOfStream(GstObject source) {
        logger.info("{}: End of stream reached.", source.getName());
        monitorObject.setStopPipeline(true);
        synchronized (monitorObject.getMonitorLock()) {
          monitorObject.getMonitorLock().notifyAll();
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.gstreamer.AbstractGSEncoderEngine#extractMultipleImages(org.opencastproject.composer
   * .api.EncodingProfile, java.util.Map)
   */
  @Override
  protected List<File> extractMultipleImages(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    String imageExtractionTemplate = properties.get(GS_IMAGE_TEMPLATE);
    String outputTemplate = properties.get("out.file.path");
    String videoPath = properties.get("in.video.path");

    // TODO generalize token substitution and write token cleanup
    String configuration = substituteTemplateValues(imageExtractionTemplate, properties, false);
    List<ImageExtractionProperties> extractionProperties = parseImageExtractionConfiguration(configuration,
            outputTemplate);

    Pipeline pipeline = createFixedPipelineForImageExtraction(videoPath);
    AppSink appsink = (AppSink) pipeline.getElementByName("appsink");

    // install listeners
    MonitorObject monitorObject = createNewMonitorObject();
    installListeners(pipeline, monitorObject);

    switch (pipeline.setState(State.PAUSED)) {
    case FAILURE:
      logger.warn("Could not change pipeline state to PAUSED");
      throw new EncoderException("Could not change state");
    case NO_PREROLL:
      pipeline.setState(State.NULL);
      logger.warn("Live sources are not supported");
      throw new EncoderException("Live sources not supported");
    default:
      break;
    }

    // loop through all image definitions and extract them
    // if one extraction fails, remove all and throw exception
    for (ImageExtractionProperties imageProperties : extractionProperties) {

      // state should be set to paused before we attempt to seek
      if (pipeline.getState() == State.NULL) {
        logger.warn("Exception occured while trying to play file {}", videoPath);
        cleanup(extractionProperties);
        throw new EncoderException("Failed to play file " + videoPath);
      }

      // seek
      if (!pipeline.seek(1.0, Format.TIME, GS_SEEK_FLAGS, SeekType.SET,
              imageProperties.getTimeInSeconds() * 1000 * 1000 * 1000, SeekType.NONE, -1)) {
        pipeline.setState(State.NULL);
        logger.warn("Could not seek to position {}s", imageProperties.getTimeInSeconds());
        cleanup(extractionProperties);
        throw new EncoderException("Failed to seek to position " + imageProperties.getTimeInSeconds() + "s");
      }

      // get buffer
      pipeline.setState(State.PLAYING);
      Buffer buffer = appsink.pullBuffer();
      pipeline.setState(State.PAUSED);

      if (buffer != null) {
        try {
          createImageOutOfBuffer(buffer, imageProperties.getImageWidth(), imageProperties.getImageHeight(),
                  imageProperties.getImageOutput());
        } catch (Exception e) {
          logger.warn("Could not create image out of buffer at time {}: {}", imageProperties.getTimeInSeconds(),
                  e.getMessage());
          cleanup(extractionProperties);
          throw new EncoderException("Failed to create image", e);
        } finally {
          buffer.dispose();
        }
      } else {
        // check if EOS was reached
        if (monitorObject.getEOSReached()) {
          logger.warn("Image extraction time {}s exceeds stream duration", imageProperties.getTimeInSeconds());
          cleanup(extractionProperties);
          throw new EncoderException("Invalid image extraction time: " + imageProperties.getTimeInSeconds() + "s");
        } else {
          logger.warn("Could not retrieve buffer from pipeline for time {}s", imageProperties.getTimeInSeconds());
          cleanup(extractionProperties);
          throw new EncoderException("Failed to retrieve buffer for time " + imageProperties.getTimeInSeconds() + "s");
        }
      }
    }

    pipeline.setState(State.NULL);
    return reorder(extractionProperties);
  }

  /**
   * Creates fixed pipeline for image extraction. Pipeline consists of filesrc, decodebin, ffmpegcolorspace and appsink
   * elements. Appsink element is named 'appsink' and by that name reference to appsink element can be obtained from the
   * pipeline.
   * 
   * @param videoPath
   *          video on which image extraction will be performed
   * @return built and linked pipeline
   * @throws EncoderException
   *           if linking fails
   */
  private Pipeline createFixedPipelineForImageExtraction(String videoPath) throws EncoderException {
    // create pipeline
    Element filesrc = ElementFactory.make("filesrc", null);
    filesrc.set("location", videoPath);
    Element decodebin = ElementFactory.make("decodebin2", null);
    final Element ffmpegcs = ElementFactory.make("ffmpegcolorspace", null);
    AppSink appSink = (AppSink) ElementFactory.make("appsink", "appsink");
    Pipeline pipeline = new Pipeline("Image extraction");
    pipeline.addMany(filesrc, decodebin, ffmpegcs, appSink);

    // link pipeline
    if (!filesrc.link(decodebin)) {
      throw new EncoderException("Failed linking filesrc with decodebin");
    }

    decodebin.connect(new Element.PAD_ADDED() {
      @Override
      public void padAdded(Element element, Pad pad) {
        pad.link(ffmpegcs.getStaticPad("sink"));
      }
    });
    Pad pad = new Pad(null, PadDirection.SRC);
    decodebin.addPad(pad);

    Caps caps = new Caps("video/x-raw-rgb, bpp=32, depth=24");
    if (!Element.linkPadsFiltered(ffmpegcs, "src", appSink, "sink", caps)) {
      throw new EncoderException("Failed linking ffmpegcolorspace with appsink");
    }

    return pipeline;
  }

  /**
   * Creates image out of gstreamer buffer. Buffer should have following properties: 32 bits/pixel and color depth of 24
   * bits. Output image format is chosen based on the output file name. If width or height are equal or less than 0,
   * original image size is retained.
   * 
   * @param buffer
   *          gstreamer buffer from which image will be constructed
   * @param width
   *          width of the new image
   * @param height
   *          height of the new image
   * @param output
   *          output file name
   * @throws IOException
   *           if writing image fails
   */
  private void createImageOutOfBuffer(Buffer buffer, int width, int height, String output) throws IOException {
    // get buffer information
    Structure structure = buffer.getCaps().getStructure(0);
    int origHeight = structure.getInteger("height");
    int origWidth = structure.getInteger("width");

    // create original image
    IntBuffer intBuf = buffer.getByteBuffer().asIntBuffer();
    int[] imageData = new int[intBuf.capacity()];
    intBuf.get(imageData, 0, imageData.length);
    BufferedImage originalImage = new BufferedImage(origWidth, origHeight, BufferedImage.TYPE_INT_RGB);
    originalImage.setRGB(0, 0, origWidth, origHeight, imageData, 0, origWidth);

    BufferedImage image;
    if (height <= 0 || width <= 0) {
      logger.info("Retaining image of original size {}x{}", origWidth, origHeight);
      image = originalImage;
    } else {
      logger.info("Resizing image from {}x{} to {}x{}", new Object[] { origWidth, origHeight, width, height });
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      graphics.setRenderingHints(imageRenderingHints);
      graphics.drawImage(originalImage, 0, 0, width, height, null);
      graphics.dispose();
    }

    // write image
    File outputFile = new File(output);
    ImageIO.write(image, FilenameUtils.getExtension(output), outputFile);
  }

  /**
   * Creates new MonitorObject, which keeps information of pipelines errors, and if pipeline should be stopped.
   * 
   * @return new MonitorObject
   */
  private MonitorObject createNewMonitorObject() {
    MonitorObject monitorObject = new MonitorObject() {

      /** Lock object for notification between threads */
      private final Object lock = new Object();
      /** If pipeline should be stopped (EOS or error) */
      private AtomicBoolean stopPipeline = new AtomicBoolean(false);
      /** List of errors */
      private LinkedList<String> errors = new LinkedList<String>();

      @Override
      public void setStopPipeline(boolean stop) {
        stopPipeline.set(stop);
      }

      @Override
      public Object getMonitorLock() {
        return lock;
      }

      @Override
      public String getFirstErrorMessage() {
        return errors.isEmpty() ? null : errors.getFirst();
      }

      @Override
      public boolean getEOSReached() {
        return stopPipeline.get();
      }

      @Override
      public void addErrorMessage(String message) {
        errors.add(message);
      }
    };
    return monitorObject;
  }

  /**
   * Creates new GSPipeline object that holds Pipeline and its MonitorObject.
   * 
   * @param pipeline
   * @param monitorObject
   * @return new GSPipeline object
   */
  private GSPipeline createNewGSPipeline(final Pipeline pipeline, final MonitorObject monitorObject) {
    GSPipeline gspipeline = new GSPipeline() {
      @Override
      public Pipeline getPipeline() {
        return pipeline;
      }

      @Override
      public MonitorObject getMonitorObject() {
        return monitorObject;
      }
    };
    return gspipeline;
  }

  /**
   * Used for monitoring pipeline state.
   */
  private interface MonitorObject {
    Object getMonitorLock();

    boolean getEOSReached();

    void setStopPipeline(boolean reached);

    void addErrorMessage(String message);

    String getFirstErrorMessage();
  }

  /**
   * Container for Pipeline and MonitorObject
   */
  private interface GSPipeline {
    Pipeline getPipeline();

    MonitorObject getMonitorObject();
  }
}
