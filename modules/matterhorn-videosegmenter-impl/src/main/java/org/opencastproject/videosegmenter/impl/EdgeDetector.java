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
package org.opencastproject.videosegmenter.impl;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * This is an implementation of Canny's edge detection algorithm.
 * <p>
 * Original version of the code released to the public domain at
 * {@link "http://www.tomgibara.com/computer-vision/canny-edge-detector"}.
 */
public class EdgeDetector {

  // statics

  private static final float GAUSSIAN_CUT_OFF = 0.005f;
  private static final float MAGNITUDE_SCALE = 100F;
  private static final float MAGNITUDE_LIMIT = 1000F;
  private static final int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);

  // fields

  private int height;
  private int width;
  private int picsize;
  private int[] data;
  private int[] magnitude;
  private BufferedImage edgesImage;

  private float gaussianKernelRadius;
  private float lowThreshold;
  private float highThreshold;
  private int gaussianKernelWidth;
  private boolean contrastNormalized;

  private float[] xConv;
  private float[] yConv;
  private float[] xGradient;
  private float[] yGradient;

  /**
   * Constructs a new detector with default parameters.
   */
  public EdgeDetector() {
    lowThreshold = 2.5f;
    highThreshold = 7.5f;
    gaussianKernelRadius = 2f;
    gaussianKernelWidth = 16;
    contrastNormalized = false;
  }

  /**
   * Obtains an image containing the edges detected during the last call to the process method. The buffered image is an
   * opaque image of type BufferedImage.TYPE_INT_ARGB in which edge pixels are white and all other pixels are black.
   *
   * @return an image containing the detected edges, or null if the process method has not yet been called.
   */
  public BufferedImage getEdgesImage() {
    return edgesImage;
  }

  /**
   * Sets the edges image. Calling this method will not change the operation of the edge detector in any way. It is
   * intended to provide a means by which the memory referenced by the detector object may be reduced.
   *
   * @param edgesImage
   *          expected (though not required) to be null
   */
  public void setEdgesImage(BufferedImage edgesImage) {
    this.edgesImage = edgesImage;
  }

  /**
   * The low threshold for hysteresis. The default value is 2.5.
   *
   * @return the low hysteresis threshold
   */
  public float getLowThreshold() {
    return lowThreshold;
  }

  /**
   * Sets the low threshold for hysteresis. Suitable values for this parameter must be determined experimentally for
   * each application. It is nonsensical (though not prohibited) for this value to exceed the high threshold value.
   *
   * @param threshold
   *          a low hysteresis threshold
   */
  public void setLowThreshold(float threshold) {
    if (threshold < 0)
      throw new IllegalArgumentException();
    lowThreshold = threshold;
  }

  /**
   * The high threshold for hysteresis. The default value is 7.5.
   *
   * @return the high hysteresis threshold
   */
  public float getHighThreshold() {
    return highThreshold;
  }

  /**
   * Sets the high threshold for hysteresis. Suitable values for this parameter must be determined experimentally for
   * each application. It is nonsensical (though not prohibited) for this value to be less than the low threshold value.
   *
   * @param threshold
   *          a high hysteresis threshold
   */
  public void setHighThreshold(float threshold) {
    if (threshold < 0)
      throw new IllegalArgumentException();
    highThreshold = threshold;
  }

  /**
   * The number of pixels across which the Gaussian kernel is applied. The default value is 16.
   *
   * @return the radius of the convolution operation in pixels
   */
  public int getGaussianKernelWidth() {
    return gaussianKernelWidth;
  }

  /**
   * The number of pixels across which the Gaussian kernel is applied. This implementation will reduce the radius if the
   * contribution of pixel values is deemed negligable, so this is actually a maximum radius.
   *
   * @param gaussianKernelWidth
   *          a radius for the convolution operation in pixels, at least 2.
   */
  public void setGaussianKernelWidth(int gaussianKernelWidth) {
    if (gaussianKernelWidth < 2)
      throw new IllegalArgumentException();
    this.gaussianKernelWidth = gaussianKernelWidth;
  }

  /**
   * The radius of the Gaussian convolution kernel used to smooth the source image prior to gradient calculation. The
   * default value is 16.
   *
   * @return the Gaussian kernel radius in pixels
   */
  public float getGaussianKernelRadius() {
    return gaussianKernelRadius;
  }

  /**
   * Sets the radius of the Gaussian convolution kernel used to smooth the source image prior to gradient calculation.
   */
  public void setGaussianKernelRadius(float gaussianKernelRadius) {
    if (gaussianKernelRadius < 0.1f)
      throw new IllegalArgumentException();
    this.gaussianKernelRadius = gaussianKernelRadius;
  }

  /**
   * Whether the luminance data extracted from the source image is normalized by linearizing its histogram prior to edge
   * extraction. The default value is false.
   *
   * @return whether the contrast is normalized
   */
  public boolean isContrastNormalized() {
    return contrastNormalized;
  }

  /**
   * Sets whether the contrast is normalized
   *
   * @param contrastNormalized
   *          true if the contrast should be normalized, false otherwise
   */
  public void setContrastNormalized(boolean contrastNormalized) {
    this.contrastNormalized = contrastNormalized;
  }

  /**
   * Processes the input image and returns the resulting edge image.
   */
  public BufferedImage process(BufferedImage sourceImage) {
    if (sourceImage == null)
      throw new IllegalStateException("No source image has been set");

    width = sourceImage.getWidth();
    height = sourceImage.getHeight();
    picsize = width * height;

    // Prepare data structures
    data = new int[picsize];
    magnitude = new int[picsize];
    xConv = new float[picsize];
    yConv = new float[picsize];
    xGradient = new float[picsize];
    yGradient = new float[picsize];

    // Create the luminance values
    readLuminance(sourceImage);

    // Adjust contrast if needed
    if (contrastNormalized)
      normalizeContrast();

    computeGradients(gaussianKernelRadius, gaussianKernelWidth);
    int low = Math.round(lowThreshold * MAGNITUDE_SCALE);
    int high = Math.round(highThreshold * MAGNITUDE_SCALE);
    performHysteresis(low, high);
    thresholdEdges();

    BufferedImage edgesImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    edgesImage.getWritableTile(0, 0).setDataElements(0, 0, width, height, data);

    return edgesImage;
  }

  // NOTE: The elements of the method below (specifically the technique for
  // non-maximal suppression and the technique for gradient computation)
  // are derived from an implementation posted in the following forum (with the
  // clear intent of others using the code):
  // http://forum.java.sun.com/thread.jspa?threadID=546211&start=45&tstart=0
  // My code effectively mimics the algorithm exhibited above.
  // Since I don't know the providence of the code that was posted it is a
  // possibility (though I think a very remote one) that this code violates
  // someone's intellectual property rights. If this concerns you feel free to
  // contact me for an alternative, though less efficient, implementation.

  private void computeGradients(float kernelRadius, int kernelWidth) {

    // generate the gaussian convolution masks
    float[] kernel = new float[kernelWidth];
    float[] diffKernel = new float[kernelWidth];
    int kwidth;
    for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
      float g1 = gaussian(kwidth, kernelRadius);
      if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2)
        break;
      float g2 = gaussian(kwidth - 0.5f, kernelRadius);
      float g3 = gaussian(kwidth + 0.5f, kernelRadius);
      kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
      diffKernel[kwidth] = g3 - g2;
    }

    int initX = kwidth - 1;
    int maxX = width - (kwidth - 1);
    int initY = width * (kwidth - 1);
    int maxY = width * (height - (kwidth - 1));

    // perform convolution in x and y directions
    for (int x = initX; x < maxX; x++) {
      for (int y = initY; y < maxY; y += width) {
        int index = x + y;
        float sumX = data[index] * kernel[0];
        float sumY = sumX;
        int xOffset = 1;
        int yOffset = width;
        for (; xOffset < kwidth;) {
          sumY += kernel[xOffset] * (data[index - yOffset] + data[index + yOffset]);
          sumX += kernel[xOffset] * (data[index - xOffset] + data[index + xOffset]);
          yOffset += width;
          xOffset++;
        }

        yConv[index] = sumY;
        xConv[index] = sumX;
      }

    }

    for (int x = initX; x < maxX; x++) {
      for (int y = initY; y < maxY; y += width) {
        float sum = 0f;
        int index = x + y;
        for (int i = 1; i < kwidth; i++)
          sum += diffKernel[i] * (yConv[index - i] - yConv[index + i]);

        xGradient[index] = sum;
      }

    }

    for (int x = kwidth; x < width - kwidth; x++) {
      for (int y = initY; y < maxY; y += width) {
        float sum = 0.0f;
        int index = x + y;
        int yOffset = width;
        for (int i = 1; i < kwidth; i++) {
          sum += diffKernel[i] * (xConv[index - yOffset] - xConv[index + yOffset]);
          yOffset += width;
        }

        yGradient[index] = sum;
      }

    }

    initX = kwidth;
    maxX = width - kwidth;
    initY = width * kwidth;
    maxY = width * (height - kwidth);
    for (int x = initX; x < maxX; x++) {
      for (int y = initY; y < maxY; y += width) {
        int index = x + y;
        int indexN = index - width;
        int indexS = index + width;
        int indexW = index - 1;
        int indexE = index + 1;
        int indexNW = indexN - 1;
        int indexNE = indexN + 1;
        int indexSW = indexS - 1;
        int indexSE = indexS + 1;

        float xGrad = xGradient[index];
        float yGrad = yGradient[index];
        float gradMag = hypot(xGrad, yGrad);

        // perform non-maximal supression
        float nMag = hypot(xGradient[indexN], yGradient[indexN]);
        float sMag = hypot(xGradient[indexS], yGradient[indexS]);
        float wMag = hypot(xGradient[indexW], yGradient[indexW]);
        float eMag = hypot(xGradient[indexE], yGradient[indexE]);
        float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
        float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
        float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
        float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
        float tmp;
        /*
         * An explanation of what's happening here, for those who want to understand the source: This performs the
         * "non-maximal supression" phase of the Canny edge detection in which we need to compare the gradient magnitude
         * to that in the direction of the gradient; only if the value is a local maximum do we consider the point as an
         * edge candidate.
         *
         * We need to break the comparison into a number of different cases depending on the gradient direction so that
         * the appropriate values can be used. To avoid computing the gradient direction, we use two simple comparisons:
         * first we check that the partial derivatives have the same sign (1) and then we check which is larger (2). As
         * a consequence, we have reduced the problem to one of four identical cases that each test the central gradient
         * magnitude against the values at two points with 'identical support'; what this means is that the geometry
         * required to accurately interpolate the magnitude of gradient function at those points has an identical
         * geometry (upto right-angled-rotation/reflection).
         *
         * When comparing the central gradient to the two interpolated values, we avoid performing any divisions by
         * multiplying both sides of each inequality by the greater of the two partial derivatives. The common comparand
         * is stored in a temporary variable (3) and reused in the mirror case (4).
         */
        // CHECKSTYLE:OFF
        if (xGrad * yGrad <= (float) 0 /* (1) */
        ? Math.abs(xGrad) >= Math.abs(yGrad) /* (2) */
        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /* (3) */
                && tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /* (4) */
        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /* (3) */
                && tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /* (4) */
        : Math.abs(xGrad) >= Math.abs(yGrad) /* (2) */
        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /* (3) */
                && tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /* (4) */
        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /* (3) */
                && tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /* (4) */
        // CHECKSTYLE:ON
        ) {
          magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
          // NOTE: The orientation of the edge is not employed by this
          // implementation. It is a simple matter to compute it at
          // this point as: Math.atan2(yGrad, xGrad);
        } else {
          magnitude[index] = 0;
        }
      }
    }
  }

  // NOTE: It is quite feasible to replace the implementation of this method
  // with one which only loosely approximates the hypot function. I've tested
  // simple approximations such as Math.abs(x) + Math.abs(y) and they work fine.
  private float hypot(float x, float y) {
    return (float) Math.hypot(x, y);
  }

  private float gaussian(float x, float sigma) {
    return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
  }

  private void performHysteresis(int low, int high) {
    // NOTE: this implementation reuses the data array to store both
    // luminance data from the image, and edge intensity from the processing.
    // This is done for memory efficiency, other implementations may wish
    // to separate these functions.
    Arrays.fill(data, 0);

    int offset = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (data[offset] == 0 && magnitude[offset] >= high) {
          follow(x, y, offset, low);
        }
        offset++;
      }
    }
  }

  private void follow(int x1, int y1, int i1, int threshold) {
    int x0 = x1 == 0 ? x1 : x1 - 1;
    int x2 = x1 == width - 1 ? x1 : x1 + 1;
    int y0 = y1 == 0 ? y1 : y1 - 1;
    int y2 = y1 == height - 1 ? y1 : y1 + 1;

    data[i1] = magnitude[i1];
    for (int x = x0; x <= x2; x++) {
      for (int y = y0; y <= y2; y++) {
        int i2 = x + y * width;
        if ((y != y1 || x != x1) && data[i2] == 0 && magnitude[i2] >= threshold) {
          follow(x, y, i2, threshold);
          return;
        }
      }
    }
  }

  private void thresholdEdges() {
    for (int i = 0; i < picsize; i++) {
      data[i] = data[i] > 0 ? -1 : 0xff000000;
    }
  }

  private int luminance(float r, float g, float b) {
    return Math.round(0.299f * r + 0.587f * g + 0.114f * b);
  }

  /**
   * Reads the luminance values from the image.
   *
   * @param sourceImage
   *          the source image
   */
  private void readLuminance(BufferedImage sourceImage) {
    int type = sourceImage.getType();
    if (type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB) {
      int[] pixels = (int[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
      for (int i = 0; i < picsize; i++) {
        int p = pixels[i];
        int r = (p & 0xff0000) >> 16;
        int g = (p & 0xff00) >> 8;
        int b = p & 0xff;
        data[i] = luminance(r, g, b);
      }
    } else if (type == BufferedImage.TYPE_BYTE_GRAY) {
      byte[] pixels = (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
      for (int i = 0; i < picsize; i++) {
        data[i] = (pixels[i] & 0xff);
      }
    } else if (type == BufferedImage.TYPE_USHORT_GRAY) {
      short[] pixels = (short[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
      for (int i = 0; i < picsize; i++) {
        data[i] = (pixels[i] & 0xffff) / 256;
      }
    } else if (type == BufferedImage.TYPE_3BYTE_BGR) {
      byte[] pixels = (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
      int offset = 0;
      for (int i = 0; i < picsize; i++) {
        int b = pixels[offset++] & 0xff;
        int g = pixels[offset++] & 0xff;
        int r = pixels[offset++] & 0xff;
        data[i] = luminance(r, g, b);
      }
    } else {
      throw new IllegalArgumentException("Unsupported image type: " + type);
    }
  }

  private void normalizeContrast() {
    int[] histogram = new int[256];
    for (int i = 0; i < data.length; i++) {
      histogram[data[i]]++;
    }
    int[] remap = new int[256];
    int sum = 0;
    int j = 0;
    for (int i = 0; i < histogram.length; i++) {
      sum += histogram[i];
      int target = sum * 255 / picsize;
      for (int k = j + 1; k <= target; k++) {
        remap[k] = i;
      }
      j = target;
    }

    for (int i = 0; i < data.length; i++) {
      data[i] = remap[data[i]];
    }
  }

}
