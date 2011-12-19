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

package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.impl.api.AudioStreamMetadata;
import org.opencastproject.inspection.impl.api.VideoStreamMetadata;
import org.opencastproject.inspection.impl.api.util.CmdlineMediaAnalyzerSupport;
import org.opencastproject.mediapackage.track.BitRateMode;
import org.opencastproject.mediapackage.track.FrameRateMode;
import org.opencastproject.mediapackage.track.ScanType;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This MediaAnalyzer implementation leverages MediaInfo (<a href="http://mediainfo.sourceforge.net/"
 * >http://mediainfo.sourceforge.net</a>) for analysis.
 * <p/>
 * <strong>Please note</strong> that this implementation is stateful and cannot be shared or used multiple times.
 * <p/>
 * Also this implementation does not keep control-, text- or other non-audio or video streams and purposefully ignores
 * them during the <code>postProcess()</code> step.
 */
public class MediaInfoAnalyzer extends CmdlineMediaAnalyzerSupport {

  public static final String MEDIAINFO_BINARY_CONFIG = "mediainfopath";
  public static final String MEDIAINFO_BINARY_DEFAULT = "mediainfo";

  private static final Logger logger = LoggerFactory.getLogger(MediaInfoAnalyzer.class);

  private static final Map<String, Setter> CommonStreamProperties = new HashMap<String, Setter>();

  private static final Map<StreamSection, Map<String, Setter>> Parser = new HashMap<StreamSection, Map<String, Setter>>();

  private StreamSection streamSection;

  /** Holds the current metadata set. */
  private Object currentMetadata;

  static {
    CommonStreamProperties.put("Format", new Setter("format", "string"));
    CommonStreamProperties.put("Format_Profile", new Setter("formatProfile", "string"));
    CommonStreamProperties.put("Format/Info", new Setter("formatInfo", "string"));
    CommonStreamProperties.put("Format/Url", new Setter("formatURL", "url"));
    CommonStreamProperties.put("Duration", new Setter("duration", "duration"));
    CommonStreamProperties.put("BitRate", new Setter("bitRate", "float"));
    CommonStreamProperties.put("BitRate_Mode", new Setter("bitRateMode", "bitRateMode"));
    CommonStreamProperties.put("BitRate_Minimum", new Setter("bitRateMinimum", "float"));
    CommonStreamProperties.put("BitRate_Maximum", new Setter("bitRateMaximum", "float"));
    CommonStreamProperties.put("BitRate_Nominal", new Setter("bitRateNominal", "float"));
    CommonStreamProperties.put("Resolution", new Setter("resolution", "int"));
    CommonStreamProperties.put("Encoded_Date", new Setter("encodedDate", "date"));

    Map<String, Setter> generalStreamSection = new HashMap<String, Setter>();
    generalStreamSection.put("FileName", new Setter("fileName", "string"));
    generalStreamSection.put("FileExtension", new Setter("fileExtension", "string"));
    generalStreamSection.put("FileSize", new Setter("size", "long"));
    generalStreamSection.put("Duration", new Setter("duration", "duration"));
    generalStreamSection.put("OverallBitRate", new Setter("bitRate", "float"));
    generalStreamSection.put("Encoded_Date", new Setter("encodedDate", "date"));

    Map<String, Setter> videoStreamSection = new HashMap<String, Setter>();
    videoStreamSection.putAll(CommonStreamProperties);
    videoStreamSection.put("Width", new Setter("frameWidth", "int"));
    videoStreamSection.put("Height", new Setter("frameHeight", "int"));
    videoStreamSection.put("PixelAspectRatio", new Setter("pixelAspectRatio", "float"));
    videoStreamSection.put("FrameRate", new Setter("frameRate", "float"));
    videoStreamSection.put("FrameRate_Mode", new Setter("frameRateMode", "frameRateMode"));
    videoStreamSection.put("ScanType", new Setter("scanType", "scanType"));
    videoStreamSection.put("ScanOrder", new Setter("scanOrder", "scanOrder"));

    Map<String, Setter> audioStreamSection = new HashMap<String, Setter>();
    audioStreamSection.putAll(CommonStreamProperties);
    audioStreamSection.put("Channel(s)", new Setter("channels", "int"));
    audioStreamSection.put("ChannelPositions", new Setter("channelPositions", "string"));
    audioStreamSection.put("SamplingRate", new Setter("samplingRate", "int"));
    audioStreamSection.put("SamplingCount", new Setter("samplingCount", "long"));

    Parser.put(StreamSection.general, generalStreamSection);
    Parser.put(StreamSection.video, videoStreamSection);
    Parser.put(StreamSection.audio, audioStreamSection);
  }

  public MediaInfoAnalyzer() {
    // instantiated using MediaAnalyzerFactory via newInstance()
    super(MEDIAINFO_BINARY_DEFAULT);
  }

  /**
   * Allows configuration {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.impl.api.MediaAnalyzer#setConfig(java.util.Map)
   */
  public void setConfig(Map<String, Object> config) {
    if (config != null) {
      if (config.containsKey(MEDIAINFO_BINARY_CONFIG)) {
        String binary = (String) config.get(MEDIAINFO_BINARY_CONFIG);
        setBinary(binary);
        logger.debug("MediaInfoAnalyzer config binary: " + binary);
      }
    }
  }

  protected String[] getAnalysisOptions(File media) {
    String mediaPath = media.getAbsolutePath().replaceAll(" ", "\\ ");
    return new String[] { "--Language=raw", "--Full", mediaPath};
  }

  protected void onAnalysis(String line) {
    // Detect section
    for (StreamSection section : StreamSection.values()) {
      if (isNewSection(section, line)) {
        streamSection = section;
        logger.debug("New section " + streamSection);
        switch (streamSection) {
          case general:
            currentMetadata = metadata;
            break;
          case video:
            currentMetadata = new VideoStreamMetadata();
            metadata.getVideoStreamMetadata().add((VideoStreamMetadata) currentMetadata);
            break;
          case audio:
            currentMetadata = new AudioStreamMetadata();
            metadata.getAudioStreamMetadata().add((AudioStreamMetadata) currentMetadata);
            break;
          default:
            logger.warn("Bug: Unknown stream section {}", streamSection);
        }
        return; // LEAVE
      }
    }

    // Parse data line
    if (currentMetadata != null) {
      // Split the line into key and value
      String[] kv = line.split("\\s*:\\s*", 2);
      Setter setter = Parser.get(streamSection).get(kv[0]);
      if (setter != null) {
        // A setter for this key is registered
        setter.set(currentMetadata, kv[1]);
      }
    }
  }

  private boolean isNewSection(StreamSection section, String line) {
    // Match case insenstive (?i)
    return Pattern.compile("(?i)" + section.name() + "( \\#[\\d]+)?").matcher(line).matches();
  }

  /**
   * Filter out any non-video or audio streams.
   */
  @Override
  protected void postProcess() {
    // Filter out "strange" streams. These can be e.g. media control streams.
    for (Iterator<VideoStreamMetadata> i = metadata.getVideoStreamMetadata().iterator(); i.hasNext();) {
      VideoStreamMetadata videoMetadata = i.next();
      if (videoMetadata.getBitRate() == null && BitRateMode.ConstantBitRate.equals(videoMetadata.getBitRateMode()))
        i.remove();
    }
    for (Iterator<AudioStreamMetadata> i = metadata.getAudioStreamMetadata().iterator(); i.hasNext();) {
      AudioStreamMetadata audioMetadata = i.next();
      if (audioMetadata.getBitRate() == null && BitRateMode.ConstantBitRate.equals(audioMetadata.getBitRateMode()))
        i.remove();
    }
  }

  static String convertString(String value) {
    return value;
  }

  static Long convertLong(String value) {
    return new Long(value);
  }

  static Integer convertInt(String value) {
    return new Integer(value);
  }

  static Float convertFloat(String value) {
    return new Float(value);
  }

  static Long convertDuration(String value) {
    return new Long(value);
  }

  static URL convertUrl(String value) throws MalformedURLException {
    return new URL(value);
  }

  static FrameRateMode convertFrameRateMode(String value) throws MediaInspectionException {
    if ("cfr".equalsIgnoreCase(value))
      return FrameRateMode.ConstantFrameRate;
    if ("vfr".equalsIgnoreCase(value))
      return FrameRateMode.VariableFrameRate;
    throw new MediaInspectionException("Cannot parse FrameRateMode " + value);
  }

  static ScanType convertScanType(String value) throws MediaInspectionException {
    if ("interlaced".equalsIgnoreCase(value))
      return ScanType.Interlaced;
    if ("progressive".equalsIgnoreCase(value))
      return ScanType.Progressive;
    throw new MediaInspectionException("Cannot parse ScanType " + value);
  }

  static BitRateMode convertBitRateMode(String value) throws MediaInspectionException {
    if ("vbr".equalsIgnoreCase(value))
      return BitRateMode.VariableBitRate;
    if ("cbr".equalsIgnoreCase(value))
      return BitRateMode.ConstantBitRate;
    throw new MediaInspectionException("Cannot parse BitRateMode " + value);
  }

  static Date convertDate(String value) throws ParseException {
    return new SimpleDateFormat("z yyyy-MM-dd hh:mm:ss").parse(value);
  }

  private static final class Setter {

    private static final String CONVERTER_METHOD_PREFIX = "convert";

    private String property;
    private String converterMethodName;

    private Setter(String property, String type) {
      this.property = property;
      this.converterMethodName = CONVERTER_METHOD_PREFIX + StringUtils.capitalize(type);
    }

    public void set(Object target, String value) {
      try {
        Method converter = MediaInfoAnalyzer.class.getDeclaredMethod(converterMethodName, String.class);
        Object converted = converter.invoke(null, value);
        BeanUtils.setProperty(target, property, converted);
      } catch (NoSuchMethodException e) {
        // throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        // throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        // throw new RuntimeException(e);
      }
    }

  }

  private enum StreamSection {

    general, video, audio
  }

}
