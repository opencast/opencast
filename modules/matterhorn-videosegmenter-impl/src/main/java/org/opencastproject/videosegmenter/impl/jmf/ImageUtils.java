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

import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.Buffer;

/**
 * A collection of utility methods used to deal with frame buffers and images.
 */
public final class ImageUtils {

  /** Disallow construction of this utility class */
  private ImageUtils() {
  }

  /**
   * Convers the frame buffer to a <code>BufferedImage</code>. This method returns <code>null</code> if the buffer
   * couldn't be created
   *
   * @param buf
   *          the buffer
   * @return a <code>BufferedImage</code>
   */
  public static BufferedImage createImage(Buffer buf) throws IOException {
    InputStream is = new ByteArrayInputStream((byte[]) buf.getData());
    BufferedImage bufferedImage = ImageIO.read(new MemoryCacheImageInputStream(is));
    return bufferedImage;
  }

  /**
   * Writes the image to disk.
   *
   * @param image
   *          the image
   */
  public static void saveImage(BufferedImage image, File file) throws IOException {
    file.getParentFile().mkdirs();
    FileUtils.deleteQuietly(file);
    ImageIO.write(image, "jpg", file);
  }

}
