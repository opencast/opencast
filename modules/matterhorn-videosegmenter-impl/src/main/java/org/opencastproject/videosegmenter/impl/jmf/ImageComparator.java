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
package org.opencastproject.videosegmenter.impl.jmf;

import org.opencastproject.videosegmenter.impl.EdgeDetector;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.imageio.ImageIO;

/**
 * Utility class used to compare to images.
 */
public class ImageComparator {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageComparator.class);

  /** The changes threshold in percent */
  private float changesThreshold = 0.01f; // 1%

  /** Number format for percentages */
  private NumberFormat percentageNf = null;

  /** Flag to indicate whether statistics should be collected */
  private boolean collectStatistics = false;

  /** Number of image comparisons */
  private long comparisons = 0;

  /** Number of total changes in % */
  private float totalChanges = 0.0f;

  /** The temporary image repository */
  private File tempDir = null;

  /**
   * Creates a new image comparator that will use <code>changesThreshold</code> as the threshold to decide whether two
   * images are different.
   * <p>
   * <b>Note:</b> turning on statistics will make the algorithm slightly less efficient.
   *
   * @param changesThreshold
   *          the number of changes in percent that will make two images different
   */
  public ImageComparator(float changesThreshold) {
    this.changesThreshold = changesThreshold;
    percentageNf = NumberFormat.getPercentInstance();
    percentageNf.setMaximumFractionDigits(2);
  }

  /**
   * Turns collecting statistical values on or off. Turning on statistics will make the algorithm slightly less
   * efficient.
   *
   * @param statistics
   *          <code>true</code> to collect statistics.
   */
  public void setStatistics(boolean statistics) {
    this.collectStatistics = statistics;
  }

  /**
   * Returns <code>true</code> if collecting statistics was turned on.
   *
   * @return <code>true</code> if statistics have been collected
   */
  public boolean hasStatistics() {
    return this.collectStatistics;
  }

  /**
   * Creates a directory where the temporary files will be put. If no directory is passed, images will not be written to
   * disk, thus making the comparator essentially a black box.
   *
   * @param directory
   *          directory for the temporary images
   */
  public void saveImagesTo(File directory) {
    this.tempDir = directory;
    if (directory != null)
      logger.info("Saving intermediary images to {}", directory);
  }

  /**
   * Returns the directory containing the intermediary images.
   *
   * @return the images
   */
  public File getSavedImagesDirectory() {
    return tempDir;
  }

  /**
   * Returns the average change in %. Note that this method will throw an <code>IllegalStateException</code> if
   * statistics have not been turned on in advance.
   *
   * @return the average change
   */
  public float getAvgChange() {
    if (!collectStatistics)
      throw new IllegalStateException("Need to turn on statistics first");
    return totalChanges / comparisons;
  }

  /**
   * Returns the number of comparisons made by this comparator.
   *
   * @return the number of comparisons
   */
  public long getComparisons() {
    return comparisons;
  }

  /**
   * Returns <code>true</code> if <code>image</code> differs from <code>currentImage</code>. In order to be treated a
   * different image, the <code>rgb</code> values of at least <code>changesThreshold</code> pixels must have changed.
   * <p>
   * Note that <code>image</code> might contain an altered version of the image, which will facilitate in the comparison
   * next time when <code>image</code> is <code>currentImage</code>.
   *
   * @param previousImage
   *          the previous image
   * @param image
   *          the new image
   * @param timestamp
   *          the image timestamp
   *
   * @return <code>true</code> if the two images are different
   */
  public boolean isDifferent(BufferedImage previousImage, BufferedImage image, long timestamp) {
    boolean differsFromCurrentScene = false;
    BufferedImage edgeImage = getEdgedImage(image);

    if (previousImage == null) {
      differsFromCurrentScene = true;
      logger.debug("First segment started");
    } else if (previousImage.getWidth() != image.getWidth() || previousImage.getHeight() != image.getHeight()) {
      differsFromCurrentScene = true;
      String currentResolution = previousImage.getWidth() + "x" + previousImage.getHeight();
      String newResolution = image.getWidth() + "x" + image.getHeight();
      logger.warn("Resolution change detected ({} -> {})", currentResolution, newResolution);
    } else {
      int changes = 0;
      long pixels = image.getWidth() * image.getHeight();
      long changesThresholdPixels = (long) (pixels * changesThreshold);

      imagecomparison: for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          if (edgeImage.getRGB(x, y) != previousImage.getRGB(x, y)) {
            changes++;
            if (changes > changesThresholdPixels) {
              differsFromCurrentScene = true;
              if (!collectStatistics)
                break imagecomparison;
            }
          }
        }
      }

      float percentage = ((float) changes) / ((float) pixels);
      if (differsFromCurrentScene)
        logger.debug("Differences found at {} s ({} change to previous image)", timestamp, percentageNf.format(percentage));
      else
        logger.debug("Found {} changes at {} s to the previous frame", percentageNf.format(percentage), timestamp);

      comparisons++;
      totalChanges += percentage;
    }

    // Write the images to disk for debugging and verification purposes
    if (tempDir != null) {
      try {
        FileUtils.forceMkdir(tempDir);
        ImageIO.write(image, "jpg", new File(tempDir, "image-" + timestamp + ".jpg"));
        ImageIO.write(edgeImage, "jpg", new File(tempDir, "image-" + timestamp + "-edged.jpg"));
      } catch (IOException e) {
        logger.warn("Error writing intermediary images to {}" + tempDir);
        e.printStackTrace();
      }
    }

    // Copy the resulting image for further reference to the original
    image.getRaster().setRect(edgeImage.getData());

    return differsFromCurrentScene;
  }

  /**
   * Returns an image that is reduced to it's edges.
   *
   * @param image
   *          the image
   * @return the edged image
   */
  private BufferedImage getEdgedImage(BufferedImage image) {
    // Configure the edge detector
    EdgeDetector edgeDetector = new EdgeDetector();
    edgeDetector.setLowThreshold(2.5f);
    edgeDetector.setHighThreshold(7.5f);
    edgeDetector.setGaussianKernelRadius(2.0f);
    edgeDetector.setGaussianKernelWidth(16);
    // edgeDetector.setContrastNormalized(true);
    return edgeDetector.process(image);
  }

}
