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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.handler.WaveformWorkflowOperationHandler.WaveUtils.WaveException;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaveformWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  static final Logger logger = LoggerFactory.getLogger(WaveformWorkflowOperationHandler.class);

  /**
   * Source flavor configuration property name.
   */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";
  /**
   * Target flavor configuration property name.
   */
  private static final String TARGET_FLAVOR_PROPERTY = "target-flavor";
  /**
   * The configuration options for this handler
   */
  private static final SortedMap<String, String> CONFIG_OPTIONS;
  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(SOURCE_FLAVOR_PROPERTY, "The source wave file flavor.");
    CONFIG_OPTIONS.put(TARGET_FLAVOR_PROPERTY, "The target png file output flavor.");
  }

  /**
   * Waveform image width.
   */
  static final int IMAGE_WIDTH = 5000;
  /**
   * Waveform image height.
   */
  static final int IMAGE_HEIGHT = 500;
  /**
   * The workspace.
   */
  private Workspace workspace;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering waveform workflow operation handler");
  }

  /**
   * {@inheritDoc}
   *
   * @see
   * org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    String sourceFlavorProperty = workflowInstance.getCurrentOperation().getConfiguration(SOURCE_FLAVOR_PROPERTY);
    MediaPackageElementFlavor sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorProperty);

    String targetFlavorProperty = workflowInstance.getCurrentOperation().getConfiguration(TARGET_FLAVOR_PROPERTY);
    MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty);

    Track[] tracks = mediaPackage.getTracks(sourceFlavor);
    if (tracks.length == 0) {
      logger.info("Skipping Waveform generation because no wave file is present in the mediapackage {}",
              mediaPackage.getIdentifier().compact());
      return createResult(Action.SKIP);
    }
    logger.info("Generating waveform png from {}", tracks[0].getURI().toString());

    WaveUtils waveUtils = null;
    BufferedImage bufferedImage = null;
    ByteArrayOutputStream os = null;
    InputStream is = null;
    try {
      File waveFile = workspace.get(tracks[0].getURI());
      waveUtils = new WaveUtils(waveFile);
      bufferedImage = waveUtils.generateWaveformImage(true);

      // finally save the image to file
      logger.debug("putting bufferedImage in ByteArrayOutputstream");
      os = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "png", os);
      is = new ByteArrayInputStream(os.toByteArray());
      logger.debug("adding waveform png to mediapackage");
      String elementId = UUID.randomUUID().toString();
      URI waveformUri = workspace.put(mediaPackage.getIdentifier().compact(), elementId, "waveform.png", is);
      Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(waveformUri, MediaPackageElement.Type.Attachment, targetFlavor);
      mediaPackage.add(attachment);

      logger.info("Generation waveform png from {} finished.", tracks[0].getURI().toString());
      return createResult(mediaPackage, Action.CONTINUE);

    } catch (WaveException ex) {
      logger.error("creating waveform image failed", ex);
    } catch (NotFoundException ex) {
      logger.error("wave file not found", ex);
    } catch (IOException ex) {
      logger.error("io exception occures while creating waveform image", ex);
    } finally {
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
    }

    return createResult(Action.SKIP);
  }

  /**
   * Set the workspace.
   *
   * @param workspace the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * This class provides wave-file operations like waveform extraction.
   */
  public class WaveUtils {

    private Wave wave;
    private float max = Float.MIN_VALUE;
    private float[][] pos = null;
    private float[][] neg = null;

    public WaveUtils(String filePath) throws IOException, WaveException {
      wave = new Wave(filePath);
      logger.debug("{}:\n{}", filePath, wave.getWaveHeader().toString());
    }

    public WaveUtils(File file) throws IOException, WaveException {
      wave = new Wave(file);
      logger.debug("{}:\n{}", file.getAbsolutePath(), wave.getWaveHeader().toString());
    }

    /**
     * Extract waveform from audio wave file.
     *
     * @param verticalScale if true, the wave will be streched vertically
     * @return waveform
     * @throws IOException if reading wave audio file failed
     */
    public BufferedImage generateWaveformImage(boolean verticalScale) throws IOException {
      readWaveFile();

      BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphic = image.createGraphics();
      graphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphic.setColor(Color.WHITE);
      graphic.fill(new Rectangle(IMAGE_WIDTH, IMAGE_HEIGHT));

      int centerLine = IMAGE_HEIGHT / 2;
      if (wave.getWaveHeader().getBlockAlign() == 1) {
        centerLine = IMAGE_HEIGHT;
      }

      // plott first channel
      graphic.setColor(Color.BLACK);
      for (int w = 0; w < IMAGE_WIDTH; w++) {
        int y1;
        int y2;
        if (verticalScale) {
          y1 = (int) (centerLine - ((pos[w][0] / max) * centerLine));
          y2 = (int) (centerLine - ((neg[w][0] / max) * centerLine));
        } else {
          y1 = (int) (centerLine - (pos[w][0] * centerLine));
          y2 = (int) (centerLine - (neg[w][0] * centerLine));
        }

        graphic.drawLine(w, y1, w, y2);
      }
      return image;
    }

    private void readWaveFile() throws IOException {
      int framesCount = 0;

      WaveHeader header = wave.getWaveHeader();
      int channels = wave.getWaveHeader().getChannels();
      // audio length in seconds
      long sec = header.getSubChunk2Size() / header.getByteRate();
      // n Samples per pixel
      int nSamples = (int) (header.getSampleRate() * sec / IMAGE_WIDTH);
      logger.debug("sec: {}, nSamples: {}", sec, nSamples);

      float[][] frames;
      pos = new float[IMAGE_WIDTH][channels];
      neg = new float[IMAGE_WIDTH][channels];

      for (int chunk = 0; chunk < IMAGE_WIDTH; chunk++) {
        frames = wave.getNFrames(nSamples);
        if (frames == null) {
          break;
        }
        framesCount += frames.length;

        double[] posTmp = new double[channels];
        double[] negTmp = new double[channels];

        for (int f = 0; f < frames.length; f++) {
          for (int c = 0; c < channels; c++) {
            if (frames[f][0] > 0) {
              posTmp[c] += frames[f][c];
            } else {
              negTmp[c] += frames[f][c];
            }
          }
        }

        if (frames.length > 0) {
          for (int c = 0; c < channels; c++) {
            pos[chunk][c] = (float) (posTmp[c] / frames.length);
            neg[chunk][c] = (float) (negTmp[c] / frames.length);
            max = max > pos[chunk][c] ? max : pos[chunk][c];
            max = max > -1 * neg[chunk][c] ? max : -1 * neg[chunk][c];
          }
        }
      }
      wave.closeDataStream();

      logger.debug("read: {} of {}", framesCount, header.getSubChunk2Size() / (header.getBlockAlign()));
    }

    /**
     * Exception can occure by processing wave data.
     */
    public class WaveException extends Exception {

      public WaveException(String message) {
        super(message);
      }
    }

    /**
     * Wave class encapsulates the wave data in a wave header and have some
     * methods to read content.
     */
    class Wave {

      private WaveUtils.WaveHeader waveHeader;
      private InputStream inputStream;

      /**
       * Constructor.
       *
       * @param filename Wave file
       * @throws IOException if reading from input stream failed
       * @throws WaveException if wave file has invalid or unsupported header format
       */
      public Wave(String filename) throws IOException, WaveException {
        this(new File(filename));
      }

      /**
       * Constructor.
       *
       * @param f the wav file
       * @throws IOException if reading from input stream failed
       * @throws WaveException if wave file has invalid or unsupported header format
       */
      public Wave(File f) throws IOException, WaveException {
        this(new FileInputStream(f));
      }

      /**
       * Constructor.
       * On any thrown exception the input stream will be closed.
       *
       * @param inputStream Wave file input stream
       * @throws IOException if reading from input stream failed
       * @throws WaveException if wave file has invalid or unsupported header format
       */
      protected Wave(InputStream inputStream) throws IOException, WaveException  {
        this.inputStream = inputStream;
        waveHeader = new WaveUtils.WaveHeader(inputStream);

        if (!waveHeader.isValid()) {
          IOUtils.closeQuietly(inputStream);
          throw new WaveException("Invalid wave header");
        }
      }

      /**
       * Get the wave header
       *
       * @return waveHeader
       */
      public WaveUtils.WaveHeader getWaveHeader() {
        return waveHeader;
      }

      /**
       * Returns true if not EOF.
       *
       * @return true if not EOF
       */
      public boolean hasMoreSamples() {
        try {
          return inputStream.available() > 0;
        } catch (IOException ex) {
          // stream closed
          return false;
        }
      }

      /**
       * Get n samples. First index specifies sample index and the second a
       * channel.
       *
       * @param n samples to read
       * @return samples readed, the length can be different from n
       * @throws IOException if an I/O error occurs.
       */
      public float[][] getNFrames(int n) throws IOException {
        // read raw audio data for n samples
        byte[] rawSamples = new byte[n * getWaveHeader().getBlockAlign()];
        int bytesRead = -1;

        try {
          bytesRead = inputStream.read(rawSamples);
        } catch (IOException ex) {
          closeDataStream();
          throw ex;
        }

        if (bytesRead == -1) {
          closeDataStream();
          return null;
        }
        int channels = getWaveHeader().getChannels();
        int bytesPerSample = getWaveHeader().getBlockAlign();
        float[][] frames = new float[bytesRead / bytesPerSample][channels];

        int maxAmplitude = 1 << (waveHeader.getBitsPerSample() - 1);
        if (getWaveHeader().getBitsPerSample() == 8) { // one more bit for unsigned value
          maxAmplitude <<= 1;
        }

        // parse and normalize raw data
        for (int f = 0; f < frames.length; f++) {
          for (int c = 0; c < channels; c++) {
            short sample = 0;
            for (int b = 0; b < bytesPerSample / channels; b++) {
              sample |= (short) (rawSamples[f * bytesPerSample + b] & 0xFF) << (b * 8);
            }
            frames[f][c] = (float) sample / maxAmplitude;
          }
        }

        return frames;
      }

      /**
       * Close file stream.
       */
      public void closeDataStream() {
        IOUtils.closeQuietly(inputStream);
      }
    }

    /**
     * Class for encapsulating all information for the wave file
     */
    class WaveHeader {

      public static final String RIFF_HEADER = "RIFF";
      public static final String WAVE_HEADER = "WAVE";
      public static final String FMT_HEADER = "fmt ";
      public static final String DATA_HEADER = "data";
      public static final String LIST_HEADER = "LIST";
      private boolean valid = false;
      private String chunkId = ""; // 4 bytes
      private long chunkSize = 0L; // unsigned 4 bytes, little endian
      private String format = ""; // 4 bytes
      private String subChunk1Id = ""; // 4 bytes
      private long subChunk1Size = 0L; // unsigned 4 bytes, little endian
      private int audioFormat = 0; // unsigned 2 bytes, little endian
      private int channels = 0; // unsigned 2 bytes, little endian
      private long sampleRate = 0L; // unsigned 4 bytes, little endian
      private long byteRate = 0L; // unsigned 4 bytes, little endian
      private int blockAlign = 0; // unsigned 2 bytes, little endian
      private int bitsPerSample = 0; // unsigned 2 bytes, little endian
      private String subChunk2Id = ""; // 4 bytes
      private long subChunk2Size = 0L; // unsigned 4 bytes, little endian

      /**
       * Read the wave file header.
       * Check isValid flag, if the wave file supported.
       * 
       * @param inputStream wave file input stream
       * @throws IOException if an I/O error occurs.
       */
      public WaveHeader(InputStream inputStream) throws IOException {
        valid = loadHeader(inputStream);
      }

      /**
       * Read header data from wav file.
       *
       * @param inputStream wave file input stream
       * @return true if wave file is valid and supported
       * @throws IOException if an I/O error occurs.
       */
      private boolean loadHeader(InputStream inputStream) throws IOException {

        byte[] headerBuffer = null;
        try {

          // get RIFF chunk descriptor
          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          chunkId = new String(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          chunkSize = parseLongLittleEndian(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          format = new String(headerBuffer);

          // get fmt sub-chunk 1
          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          subChunk1Id = new String(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          subChunk1Size = parseLongLittleEndian(headerBuffer);

          headerBuffer = new byte[2];
          inputStream.read(headerBuffer);
          audioFormat = parseIntLittleEndian(headerBuffer);

          headerBuffer = new byte[2];
          inputStream.read(headerBuffer);
          channels = parseIntLittleEndian(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          sampleRate = parseLongLittleEndian(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          byteRate = parseLongLittleEndian(headerBuffer);

          headerBuffer = new byte[2];
          inputStream.read(headerBuffer);
          blockAlign = parseIntLittleEndian(headerBuffer);

          headerBuffer = new byte[2];
          inputStream.read(headerBuffer);
          bitsPerSample = parseIntLittleEndian(headerBuffer);

          if (subChunk1Size > 16) {
            // should be empty on PCM
            headerBuffer = new byte[(int) subChunk1Size - 16];
            inputStream.read(headerBuffer);
          }

          // get data sub-chunk 2
          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          subChunk2Id = new String(headerBuffer);

          headerBuffer = new byte[4];
          inputStream.read(headerBuffer);
          subChunk2Size = parseLongLittleEndian(headerBuffer);

          if (LIST_HEADER.equals(subChunk2Id.toUpperCase())) {
            do {
              // list info header wrap some metadata like encoder software, author, etc.
              // we are not interesting on it but should read them till raw wav data chunk
              byte[] listHeader = new byte[(int)subChunk2Size];
              inputStream.read(listHeader);
              // drop list data chunk

              // read next chunk id and size
              // get data sub-chunk 2
              headerBuffer = new byte[4];
              inputStream.read(headerBuffer);
              subChunk2Id = new String(headerBuffer);

              headerBuffer = new byte[4];
              inputStream.read(headerBuffer);
              subChunk2Size = parseLongLittleEndian(headerBuffer);

            } while (LIST_HEADER.equals(subChunk2Id.toUpperCase()));
          }

        } catch (IllegalArgumentException ex) {
          logger.error("Waveheader parsing failed", ex);
          return false;
        }

        if (bitsPerSample != 8 && bitsPerSample != 16) {
          logger.error("WaveHeader: only supports bitsPerSample 8 or 16");
          return false;
        }

        // check the format is supported
        if (chunkId.toUpperCase().equals(RIFF_HEADER) && format.toUpperCase().equals(WAVE_HEADER)
                && FMT_HEADER.equals(subChunk1Id) && DATA_HEADER.equals(subChunk2Id)
                && audioFormat == 1 && byteRate == sampleRate * channels * bitsPerSample / 8
                && blockAlign == channels * bitsPerSample / 8) {
          return true;
        } else {
          logger.error("WaveHeader: Unsupported header format");
          if (!RIFF_HEADER.equals(chunkId.toUpperCase())) {
            logger.error("chunckId {} is not {}", chunkId.toUpperCase(), RIFF_HEADER);
          }

          if (!WAVE_HEADER.equals(format.toUpperCase())) {
            logger.error("format {} is not {}", format.toUpperCase(), WAVE_HEADER);
          }

          if (!FMT_HEADER.equals(subChunk1Id.toLowerCase())) {
            logger.error("subChunk1Id {} is not {}", subChunk1Id.toLowerCase(), FMT_HEADER);
          }

          if (!DATA_HEADER.equals(subChunk2Id.toLowerCase())) {
            logger.error("subChunk2Id {} is not {}", subChunk2Id.toLowerCase(), DATA_HEADER);
          }

          if (audioFormat != 1) {
            logger.error("audioFormat {} is not 1", audioFormat);
          }

          if (byteRate != sampleRate * channels * bitsPerSample / 8) {
            logger.error("byteRate ({}) != sampleRate ({}) * channels ({}) * bitsPerSample ({}) / 8",
                    new Object[]{byteRate, sampleRate, channels, bitsPerSample});
          }

          if (blockAlign != channels * bitsPerSample / 8) {
            logger.error("blockAlign ({}) != channels({}) * bitsPerSample ({}) / 8",
                    new Object[]{blockAlign, channels, bitsPerSample});
          }
        }

        return false;
      }

      /**
       * Parse an 2 byte (16 bit) signed value from raw byte array (little
       * endian).
       *
       * @param rawData byte array in little endian format
       * @return parsed value
       * @throws IllegalArgumentException array length != 2
       */
      private int parseIntLittleEndian(byte[] rawData) throws IllegalArgumentException {
        if (rawData.length != 2) {
          throw new IllegalArgumentException("rawData schould be 2 byte long");
        }

        return (int) (rawData[0] & 0xFF) | (int) (rawData[1] & 0xFF) << 8;
      }

      /**
       * Parse an 4 byte (32 bit) signed value from raw byte array (little
       * endian).
       *
       * @param rawData byte array in little endian format
       * @return parsed value
       * @throws IllegalArgumentException array length != 4
       */
      private long parseLongLittleEndian(byte[] rawData) throws IllegalArgumentException {
        if (rawData.length != 4) {
          throw new IllegalArgumentException("rawData schould be 4 byte long");
        }

        return (long) (rawData[0] & 0xFF)
                | (long) (rawData[1] & 0xFF) << 8
                | (long) (rawData[2] & 0xFF) << 16
                | (long) (rawData[3] & 0xFF) << 24;
      }

      public boolean isValid() {
        return valid;
      }

      public String getChunkId() {
        return chunkId;
      }

      public long getChunkSize() {
        return chunkSize;
      }

      public String getFormat() {
        return format;
      }

      public String getSubChunk1Id() {
        return subChunk1Id;
      }

      public long getSubChunk1Size() {
        return subChunk1Size;
      }

      public int getAudioFormat() {
        return audioFormat;
      }

      public int getChannels() {
        return channels;
      }

      public int getSampleRate() {
        return (int) sampleRate;
      }

      public int getByteRate() {
        return (int) byteRate;
      }

      public int getBlockAlign() {
        return blockAlign;
      }

      public int getBitsPerSample() {
        return bitsPerSample;
      }

      public String getSubChunk2Id() {
        return subChunk2Id;
      }

      public long getSubChunk2Size() {
        return subChunk2Size;
      }

      public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append("chunkId: " + chunkId);
        sb.append("\n");
        sb.append("chunkSize: " + chunkSize);
        sb.append("\n");
        sb.append("format: " + format);
        sb.append("\n");
        sb.append("subChunk1Id: " + subChunk1Id);
        sb.append("\n");
        sb.append("subChunk1Size: " + subChunk1Size);
        sb.append("\n");
        sb.append("audioFormat: " + audioFormat);
        sb.append("\n");
        sb.append("channels: " + channels);
        sb.append("\n");
        sb.append("sampleRate: " + sampleRate);
        sb.append("\n");
        sb.append("byteRate: " + byteRate);
        sb.append("\n");
        sb.append("blockAlign: " + blockAlign);
        sb.append("\n");
        sb.append("bitsPerSample: " + bitsPerSample);
        sb.append("\n");
        sb.append("subChunk2Id: " + subChunk2Id);
        sb.append("\n");
        sb.append("subChunk2Size: " + subChunk2Size);
        return sb.toString();
      }
    }
  }
}
