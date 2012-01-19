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
package org.opencastproject.composer.gstreamer;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncoderListener;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.MimetypesFileTypeMap;

/**
 * Abstract base class for GStreamer encoder engines.
 */
public abstract class AbstractGSEncoderEngine implements EncoderEngine {

  /** Logging utility */
  private static Logger logger = LoggerFactory.getLogger(AbstractGSEncoderEngine.class);

  /** List of installed listeners */
  protected List<EncoderListener> listeners = new CopyOnWriteArrayList<EncoderListener>();

  /** Supported profiles for this engine */
  protected Map<String, EncodingProfile> supportedProfiles = new HashMap<String, EncodingProfile>();

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.api.EncoderEngine#addEncoderListener(org.opencastproject.composer.api.EncoderListener)
   */
  @Override
  public void addEncoderListener(EncoderListener listener) {
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.api.EncoderEngine#removeEncoderListener(org.opencastproject.composer.api.EncoderListener
   * )
   */
  @Override
  public void removeEncoderListener(EncoderListener listener) {
    listeners.remove(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#encode(java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File encode(File mediaSource, EncodingProfile format, Map<String, String> properties) throws EncoderException {
    return process(null, mediaSource, format, properties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#mux(java.io.File, java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File mux(File audioSource, File videoSource, EncodingProfile format, Map<String, String> properties)
          throws EncoderException {
    return process(audioSource, videoSource, format, properties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#trim(java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, long, long, java.util.Map)
   */
  @Override
  public File trim(File mediaSource, EncodingProfile format, long start, long duration, Map<String, String> properties)
          throws EncoderException {

    if (properties == null) {
      properties = new Hashtable<String, String>();
    }
    properties.put("trim.start", Long.toString(start * 1000000L));
    properties.put("trim.duration", Long.toString(duration * 1000000L));

    return process(null, mediaSource, format, properties);
  }
  
  /**
   * Substitutes template values from template with actual values from properties.
   * 
   * @param template
   *          String that represents template
   * @param properties
   *          Map that contains substitution for template values in template
   * @param cleanup
   *          if template values that were not matched should be removed
   * @return String built from template
   */
  protected String substituteTemplateValues(String template, Map<String, String> properties, boolean cleanup) {

    StringBuffer buffer = new StringBuffer();
    Pattern pattern = Pattern.compile("#\\{\\S+?\\}");
    Matcher matcher = pattern.matcher(template);
    while (matcher.find()) {
      String match = template.substring(matcher.start() + 2, matcher.end() - 1);
      if (properties.containsKey(match)) {
        matcher.appendReplacement(buffer, properties.get(match));
      }
    }
    matcher.appendTail(buffer);

    String processedTemplate = buffer.toString();

    if (cleanup) {
      // remove all property matches
      buffer = new StringBuffer();
      Pattern ppattern = Pattern.compile("\\S+?=#\\{\\S+?\\}");
      matcher = ppattern.matcher(processedTemplate);
      while (matcher.find()) {
        matcher.appendReplacement(buffer, "");
      }
      matcher.appendTail(buffer);
      processedTemplate = buffer.toString();

      // remove all other templates
      buffer = new StringBuffer();
      matcher = pattern.matcher(processedTemplate);
      while (matcher.find()) {
        matcher.appendReplacement(buffer, "");
      }
      matcher.appendTail(buffer);
      processedTemplate = buffer.toString();
    }

    return processedTemplate;
  }

  /**
   * Executes encoding job. At least one source has to be specified.
   * 
   * @param audioSource
   *          File that contains audio source (if used)
   * @param videoSource
   *          File that contains video source (if used)
   * @param profile
   *          EncodingProfile used for this encoding job
   * @param properties
   *          Map containing any additional properties
   * @return File created as result of this encoding job
   * @throws EncoderException
   *           if encoding fails
   */
  protected File process(File audioSource, File videoSource, EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    Map<String, String> params = new HashMap<String, String>();
    if (properties != null) {
      params.putAll(properties);
    }

    try {
      if (audioSource == null && videoSource == null) {
        throw new IllegalArgumentException("At least one source must be specified.");
      }

      // Set encoding parameters
      if (audioSource != null) {
        String audioInput = FilenameUtils.normalize(audioSource.getAbsolutePath());
        params.put("in.audio.path", audioInput);
        params.put("in.audio.name", FilenameUtils.getBaseName(audioInput));
        params.put("in.audio.suffix", FilenameUtils.getExtension(audioInput));
        params.put("in.audio.filename", FilenameUtils.getName(audioInput));
        params.put("in.audio.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(audioInput));
      }
      if (videoSource != null) {
        String videoInput = FilenameUtils.normalize(videoSource.getAbsolutePath());
        params.put("in.video.path", videoInput);
        params.put("in.video.name", FilenameUtils.getBaseName(videoInput));
        params.put("in.video.suffix", FilenameUtils.getExtension(videoInput));
        params.put("in.video.filename", FilenameUtils.getName(videoInput));
        params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput));
      }
      File parentFile;
      if (videoSource == null) {
        parentFile = audioSource;
      } else {
        parentFile = videoSource;
      }
      String outDir = parentFile.getAbsoluteFile().getParent();
      String outFileName = FilenameUtils.getBaseName(parentFile.getName());
      String outSuffix = substituteTemplateValues(profile.getSuffix(), params, false);

      if (new File(outDir, outFileName + outSuffix).exists()) {
        outFileName += "_" + UUID.randomUUID().toString();
      }
      
      params.put("out.dir", outDir);
      params.put("out.name", outFileName);
      params.put("out.suffix", outSuffix);
      
      
      File encodedFile = new File(outDir, outFileName + outSuffix);
      params.put("out.file.path", encodedFile.getAbsolutePath());

      // create and launch gstreamer pipeline
      createAndLaunchPipeline(profile, params);

      if (audioSource != null) {
        logger.info("Audio track {} and video track {} successfully encoded using profile '{}'",
                new String[] { (audioSource == null ? "N/A" : audioSource.getName()),
                        (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier() });
      } else {
        logger.info("Video track {} successfully encoded using profile '{}'", new String[] { videoSource.getName(),
                profile.getIdentifier() });
      }
      fireEncoded(this, profile, audioSource, videoSource);
      return encodedFile;
    } catch (EncoderException e) {
      if (audioSource != null) {
        logger.warn(
                "Error while encoding audio track {} and video track {} using '{}': {}",
                new String[] { (audioSource == null ? "N/A" : audioSource.getName()),
                        (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      } else {
        logger.warn("Error while encoding video track {} using '{}': {}", new String[] {
                (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      }
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw e;
    } catch (Exception e) {
      logger.warn("Error while encoding audio {} and video {} to {}:{}, {}",
              new Object[] { (audioSource == null ? "N/A" : audioSource.getName()),
                      (videoSource == null ? "N/A" : videoSource.getName()), profile.getName(), e.getMessage() });
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw new EncoderException(this, e.getMessage(), e);
    }
  }

  /**
   * Creates Pipeline from profile and additional properties and launches it.
   * 
   * @param profile
   *          EncodingProfile used for creating Pipeline
   * @param properties
   *          additional properties for creating Pipeline
   * @throws EncoderException
   *           if Pipeline creation or execution fails
   */
  protected abstract void createAndLaunchPipeline(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException;

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#extract(java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, java.util.Map, long[])
   */
  @Override
  public List<File> extract(File mediaSource, EncodingProfile profile, Map<String, String> properties, long... times)
          throws EncoderException {

    Map<String, String> params = new HashMap<String, String>();
    if (properties != null) {
      params.putAll(properties);
    }

    try {
      if (mediaSource == null) {
        throw new IllegalArgumentException("Media source must be specified.");
      }
      if (times.length == 0) {
        throw new IllegalArgumentException("At least one time has to be specified");
      }

      // build string definition
      String imageDimensions = profile.getExtension("gstreamer.image.dimensions");
      if (imageDimensions == null) {
        throw new EncoderException("Missing dimension definition in encoding profile");
      }
      StringBuilder definition = new StringBuilder();
      definition.append(Long.toString(times[0]) + ":" + imageDimensions);
      for (int i = 1; i < times.length; i++) {
        definition.append("," + Long.toString(times[i]) + ":" + imageDimensions);
      }
      params.put("gstreamer.image.extraction", definition.toString());

      // Set encoding parameters
      String mediaInput = FilenameUtils.normalize(mediaSource.getAbsolutePath());
      params.put("in.video.path", mediaInput);
      params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(mediaInput));

      String outDir = mediaSource.getAbsoluteFile().getParent();
      String outFileName = FilenameUtils.getBaseName(mediaSource.getName());
      String outSuffix = profile.getSuffix();

      // add time template
      if (!outSuffix.contains("#{time}")) {
        outFileName += "_#{time}";
      }
      
      File encodedFileTemplate = new File(outDir, outFileName + outSuffix);
      params.put("out.file.path", encodedFileTemplate.getAbsolutePath());

      // extract images
      List<File> outputImages = extractMultipleImages(profile, params);
      if (outputImages.size() == 0) {
        logger.warn("No images were extracted from video track {} using encoding profile '{}'", mediaSource.getName(),
                profile.getIdentifier());
      }

      logger.info("Images successfully extracted from video track {} using profile '{}'",
              new String[] { mediaSource.getName(), profile.getIdentifier() });
      fireEncoded(this, profile, mediaSource);
      return outputImages;
    } catch (EncoderException e) {
      logger.warn("Error while extracting images from video track {} using '{}': {}", new String[] {
              (mediaSource == null ? "N/A" : mediaSource.getName()), profile.getIdentifier(), e.getMessage() });
      fireEncodingFailed(this, profile, e, mediaSource);
      throw e;
    } catch (Exception e) {
      logger.warn("Error while extracting images from video track {} using '{}': {}", new String[] {
              (mediaSource == null ? "N/A" : mediaSource.getName()), profile.getIdentifier(), e.getMessage() });
      fireEncodingFailed(this, profile, e, mediaSource);
      throw new EncoderException(this, e.getMessage(), e);
    }
  }

  /**
   * Extracts multiple images from video stream. Profile is looked for the following template: &lt;time in
   * seconds&gt;:&lt;image width&gt;x&lt;image height&gt;. Multiple image definitions can be separated with comma. If
   * image width or image height is less or equal to zero, original image size will be retained.
   * 
   * @param profile
   *          EncodeingProfile used for image extraction
   * @param properties
   *          additional properties used in extraction
   * @return List of extracted image's files
   * @throws EncoderException
   *           if extraction fails
   */
  protected abstract List<File> extractMultipleImages(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException;

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#supportsMultithreading()
   */
  @Override
  public boolean supportsMultithreading() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#supportsProfile(java.lang.String,
   * org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  @Override
  public boolean supportsProfile(String profile, MediaType type) {
    if (supportedProfiles.containsKey(profile)) {
      EncodingProfile p = supportedProfiles.get(profile);
      return p.isApplicableTo(type);
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#needsLocalWorkCopy()
   */
  @Override
  public boolean needsLocalWorkCopy() {
    return false;
  }

  /**
   * This method is called to send the <code>formatEncoded</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param profile
   *          the media format
   * @param sourceFiles
   *          the source files encoded
   */
  protected void fireEncoded(EncoderEngine engine, EncodingProfile profile, File... sourceFiles) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncoded(engine, profile, sourceFiles);
      } catch (Throwable t) {
        logger.error("Encoder listener " + l + " threw exception while handling callback");
      }
    }
  }

  /**
   * This method is called to send the <code>trackEncodingFailed</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFiles
   *          the files that were encoded
   * @param profile
   *          the media format
   * @param cause
   *          the reason of failure
   */
  protected void fireEncodingFailed(EncoderEngine engine, EncodingProfile profile, Throwable cause, File... sourceFiles) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncodingFailed(engine, profile, cause, sourceFiles);
      } catch (Throwable t) {
        logger.error("Encoder listener {} threw exception while handling callback", l);
      }
    }
  }

  /**
   * This method is called to send the <code>trackEncodingProgressed</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFile
   *          the file that is being encoded
   * @param profile
   *          the media format
   * @param progress
   *          the progress value
   */
  protected void fireEncodingProgressed(EncoderEngine engine, File sourceFile, EncodingProfile profile, int progress) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncodingProgressed(engine, sourceFile, profile, progress);
      } catch (Throwable t) {
        logger.error("Encoder listener " + l + " threw exception while handling callback");
      }
    }
  }

  /**
   * Parses image extraction configuration in the following format: #{image_time_1}:#{image_width}x#{image_height}.
   * Multiple extraction configurations can be separated by comma.
   * 
   * @param configuration
   *          Configuration for image extraction
   * @param outputTemplate
   *          output path template. Should be in the form /some_file_name_#{time}.jpg so that each image will have it's
   *          unique path.
   * @return parsed List for image extraction
   */
  protected List<ImageExtractionProperties> parseImageExtractionConfiguration(String configuration,
          String outputTemplate) {

    LinkedList<ImageExtractionProperties> propertiesList = new LinkedList<AbstractGSEncoderEngine.ImageExtractionProperties>();
    Scanner scanner = new Scanner(configuration);
    scanner.useDelimiter(",");
    int counter = 0;

    while (scanner.hasNext()) {
      String nextToken = scanner.next().trim();
      if (!nextToken.matches("[0-9]+:[0-9]+[x|X][0-9]+")) {
        throw new IllegalArgumentException("Invalid token found: " + nextToken);
      }

      String[] properties = nextToken.split("[:|x|X]");
      String output = outputTemplate.replaceAll("#\\{time\\}", properties[0]);
      if (output.equals(outputTemplate)) {
        logger.warn("Output filename does not contain #{time} template: multiple images will overwrite");
      }
      
      if (new File(output).exists()) {
        String outputFile = FilenameUtils.removeExtension(output);
        String extension = FilenameUtils.getExtension(output);
        output = outputFile + "_reencode." + extension;
      }
      
      ImageExtractionProperties imageProperties = new ImageExtractionProperties(counter++,
              Long.parseLong(properties[0]), Integer.parseInt(properties[1]), Integer.parseInt(properties[2]), output);

      propertiesList.add(imageProperties);
    }

    Collections.sort(propertiesList, new Comparator<ImageExtractionProperties>() {
      @Override
      public int compare(ImageExtractionProperties o1, ImageExtractionProperties o2) {
        return (int) (o2.timeInSeconds - o1.timeInSeconds);
      }
    });

    return propertiesList;
  }

  /**
   * Reorder images to the same way as they were specified in profile and returns only list of filenames.
   * 
   * @param extractionProperties
   *          extraction properties for images
   * @return List of image filenames
   */
  protected List<File> reorder(List<ImageExtractionProperties> extractionProperties) {
    Collections.sort(extractionProperties, new Comparator<ImageExtractionProperties>() {
      @Override
      public int compare(ImageExtractionProperties o1, ImageExtractionProperties o2) {
        return o2.order - o1.order;
      }
    });
    List<File> outputImages = new LinkedList<File>();
    for (ImageExtractionProperties properties : extractionProperties) {
      outputImages.add(new File(properties.imageOutput));
    }
    return outputImages;
  }

  /**
   * Removes any existing file from image extraction properties.
   * 
   * @param extractionProperties
   */
  protected void cleanup(List<ImageExtractionProperties> extractionProperties) {
    for (ImageExtractionProperties properties : extractionProperties) {
      File file = new File(properties.imageOutput);
      if (file.exists() && !file.delete()) {
        logger.warn("Could not delete file: {}", properties.imageOutput);
      }
    }
  }

  /**
   * Class that holds information for image extraction.
   */
  protected class ImageExtractionProperties {
    /** sequence in template */
    private int order;
    /** extraction time */
    private long timeInSeconds;
    /** image width */
    private int imageWidth;
    /** image height */
    private int imageHeight;
    /** output path */
    private String imageOutput;

    public ImageExtractionProperties(int order, long timeInSeconds, int imageWidth, int imageHeight, String imageOutput) {
      this.order = order;
      this.timeInSeconds = timeInSeconds;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
      this.imageOutput = imageOutput;
    }

    public int getOrder() {
      return order;
    }

    public long getTimeInSeconds() {
      return timeInSeconds;
    }

    public int getImageWidth() {
      return imageWidth;
    }

    public int getImageHeight() {
      return imageHeight;
    }

    public String getImageOutput() {
      return imageOutput;
    }
  }
}
