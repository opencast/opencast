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


package org.opencastproject.textextractor.tesseract;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.textextractor.api.TextExtractor;
import org.opencastproject.textextractor.api.TextExtractorException;

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Commandline wrapper around tesseract' <code>tesseract</code> command.
 */
public class TesseractTextExtractor implements TextExtractor, ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TesseractTextExtractor.class);

  /** Default name of the tesseract binary */
  public static final String TESSERACT_BINARY_DEFAULT = "tesseract";

  /** Configuration property that defines the path to the tesseract binary */
  public static final String TESSERACT_BINARY_CONFIG_KEY =
      "org.opencastproject.textanalyzer.tesseract.path";

  /** Configuration property that defines additional tesseract options like the
   * language or the pagesegmode to use. This is just appended to the command
   * line when tesseract is called. */
  public static final String TESSERACT_OPTS_CONFIG_KEY =
      "org.opencastproject.textanalyzer.tesseract.options";

  /** Binary of the tesseract command */
  private String binary;

  /** Additional options for the tesseract command */
  private String addOptions = "";

  /** Tesseract stderr lines not to log */
  private static final List<String> stderrFilter = java.util.Arrays.asList(
          "Page",
          "Tesseract Open Source OCR Engine",
          "Warning: Invalid resolution 0 dpi. Using 70 instead.",
          "Estimating resolution as ");

  /**
   * Creates a new tesseract command wrapper that will be using the default binary.
   */
  public TesseractTextExtractor() {
    this(TESSERACT_BINARY_DEFAULT);
  }

  /**
   * Creates a new tesseract command wrapper that will be using the given binary.
   *
   * @param binary
   *          the tesseract binary
   */
  public TesseractTextExtractor(String binary) {
    this.binary = binary;
  }

  /**
   * Sets additional options for tesseract calls.
   *
   * @param addOptions
   */
  public void setAdditionalOptions(String addOptions) {
    this.addOptions = addOptions;
  }

  /**
   * Returns the additional options for tesseract..
   *
   * @return additional options
   */
  public String getAdditionalOptions() {
    return addOptions;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.textextractor.api.TextExtractor#extract(java.io.File)
   */
  @Override
  public List<String> extract(File image) throws TextExtractorException {
    if (binary == null) {
      throw new IllegalStateException("Binary is not set");
    }

    File outputFile = null;
    File outputFileBase = new File(image.getParentFile(), FilenameUtils.getBaseName(image.getName()));
    // Run tesseract
    List<String> command = getTesseractCommand(image, outputFileBase);
    logger.info("Running Tesseract: {}", command);
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process tesseractProcess = processBuilder.start();

      // listen to output
      try (BufferedReader in = new BufferedReader(new InputStreamReader(tesseractProcess.getInputStream()))) {
        String line;
        while ((line = in.readLine()) != null) {
          final String trimmedLine = line.trim();
          if (stderrFilter.parallelStream().noneMatch(trimmedLine::startsWith)) {
            logger.info(line);
          } else {
            logger.debug(line);
          }
        }
      }

      // wait until the task is finished
      int exitCode = tesseractProcess.waitFor();
      if (exitCode != 0) {
        throw new TextExtractorException("Tesseract exited abnormally with status " + exitCode);
      }

      // Read the tesseract output file
      outputFile = new File(outputFileBase.getAbsolutePath() + ".txt");
      ArrayList<String> output = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile), UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          final String trimmedLine = line.trim();
          if (!trimmedLine.isEmpty()) {
            output.add(trimmedLine);
          }
        }
      }
      return output;
    } catch (IOException | InterruptedException e) {
      throw new TextExtractorException("Error running text extractor " + binary, e);
    } finally {
      FileUtils.deleteQuietly(outputFile);
    }
  }

  /**
   * Generate the command line to run Tesseract
   *
   * @param image
   *          the image file
   * @param outputFile
   *          base name of output file. Tesseract will attach <code>.txt</code>
   * @return the command line to runn Tesseract on the given input file
   */
  private List<String> getTesseractCommand(final File image, final File outputFile) {
    List<String> args = new ArrayList<>();
    args.add(binary);
    args.add(image.getAbsolutePath());
    args.add(outputFile.getAbsolutePath());
    args.addAll(Arrays.asList(StringUtils.split(addOptions)));
    return args;
  }

  @Override
  public void updated(Dictionary properties) {
    String path = (String) properties.get(TESSERACT_BINARY_CONFIG_KEY);
    if (path != null) {
      logger.info("Setting Tesseract path to {}", path);
      this.binary = path;
    }
    /* Set additional options for tesseract (i.e. language to use) */
    String addopts = (String) properties.get(TESSERACT_OPTS_CONFIG_KEY);
    if (addopts != null) {
      logger.info("Setting additional options for Tesseract path to '{}'", addopts);
      this.addOptions = addopts;
    }
  }

  public void activate(ComponentContext cc) {
    // Configure ffmpeg
    String path = cc.getBundleContext().getProperty(TESSERACT_BINARY_CONFIG_KEY);
    if (path == null) {
      logger.debug("DEFAULT " + TESSERACT_BINARY_CONFIG_KEY + ": " + TESSERACT_BINARY_DEFAULT);
    } else {
      this.binary = path;
      logger.info("Setting Tesseract path to binary from config: {}", path);
    }
    /* Set additional options for tesseract (i.e. language to use) */
    String addopts = cc.getBundleContext().getProperty(TESSERACT_OPTS_CONFIG_KEY);
    if (addopts != null) {
      logger.info("Setting additional options for Tesseract to '{}'", addopts);
      this.addOptions = addopts;
    } else {
      logger.info("No additional options for Tesseract");
      this.addOptions = "";
    }
  }

}
