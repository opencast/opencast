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

package org.opencastproject.mediapackage;

import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.opencastproject.util.IoSupport.withResource;

/**
 * Utility class used for media package handling.
 */
public final class MediaPackageSupport {

  /** Disable construction of this utility class */
  private MediaPackageSupport() {
  }

  /**
   * Mode used when merging media packages.
   * <p>
   * <ul>
   * <li><code>Merge</code> assigns a new identifier in case of conflicts</li>
   * <li><code>Replace</code> replaces elements in the target media package with matching identifier</li>
   * <li><code>Skip</code> skips elements from the source media package with matching identifer</li>
   * <li><code>Fail</code> fail in case of conflicting identifier</li>
   * </ul>
   */
  enum MergeMode {
    Merge, Replace, Skip, Fail
  };

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageSupport.class.getName());

  /**
   * Merges the contents of media package located at <code>sourceDir</code> into the media package located at
   * <code>targetDir</code>.
   * <p>
   * When choosing to move the media package element into the new place instead of copying them, the source media
   * package folder will be removed afterwards.
   * </p>
   * 
   * @param dest
   *          the target media package directory
   * @param src
   *          the source media package directory
   * @param mode
   *          conflict resolution strategy in case of identical element identifier
   * @throws MediaPackageException
   *           if an error occurs either accessing one of the two media packages or merging them
   */
  public static MediaPackage merge(MediaPackage dest, MediaPackage src, MergeMode mode) throws MediaPackageException {
    try {
      for (MediaPackageElement e : src.elements()) {
        if (dest.getElementById(e.getIdentifier()) == null)
          dest.add(e);
        else {
          if (MergeMode.Replace == mode) {
            logger.debug("Replacing element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            dest.remove(dest.getElementById(e.getIdentifier()));
            dest.add(e);
          } else if (MergeMode.Skip == mode) {
            logger.debug("Skipping element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            continue;
          } else if (MergeMode.Merge == mode) {
            logger.debug("Renaming element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            e.setIdentifier(null);
            dest.add(e);
          } else if (MergeMode.Fail == mode) {
            throw new MediaPackageException("Target media package " + dest + " already contains element with id "
                    + e.getIdentifier());
          }
        }
      }
    } catch (UnsupportedElementException e) {
      throw new MediaPackageException(e);
    }
    return dest;
  }

  /**
   * Creates a unique filename inside the root folder, based on the parameter <code>filename</code>.
   * 
   * @param root
   *          the root folder
   * @param filename
   *          the original filename
   * @return the new and unique filename
   */
  public static File createElementFilename(File root, String filename) {
    String baseName = PathSupport.removeFileExtension(filename);
    String extension = PathSupport.getFileExtension(filename);
    int count = 1;
    StringBuffer name = null;
    File f = new File(root, filename);
    while (f.exists()) {
      name = new StringBuffer(baseName).append("-").append(count).append(".").append(extension);
      f = new File(root, name.toString());
      count++;
    }
    return f;
  }

  /** Immutable modification of a media package. */
  public static MediaPackage modify(MediaPackage mp, Function<MediaPackage, Void> f) {
    final MediaPackage clone = (MediaPackage) mp.clone();
    f.apply(clone);
    return clone;
  }

  /** Create a copy of the given media package. */
  public static MediaPackage copy(MediaPackage mp) {
    return (MediaPackage) mp.clone();
  }

  /** Rewrite the URIs of all media package elements. Modifications are done on a copy of the given package. */
  public static MediaPackage rewriteUris(final MediaPackage mp, final Function<MediaPackageElement, URI> f) {
    return modify(mp, new Effect<MediaPackage>() {
      @Override public void run(MediaPackage mp) {
        for (MediaPackageElement e : mp.getElements()) {
          e.setURI(f.apply(e));
        }
      }
    });
  }

  /** For testing purposes only! Loads a mediapackage from the class path. */
  public static MediaPackage loadMediaPackageFromClassPath(String manifest) {
    final MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    final URL rootUrl = MediaPackageSupport.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    final InputStream in = MediaPackageSupport.class.getResourceAsStream(manifest);
    if (in == null)
      throw new Error(manifest + "can not be found");
    return withResource(in, new Function.X<InputStream, MediaPackage>() {
      @Override public MediaPackage xapply(InputStream is) throws MediaPackageException {
        return mediaPackageBuilder.loadFromXml(is);
      }
    });
  }
}
