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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderListener;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.mediapackage.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for encoder engines.
 */
public abstract class AbstractEncoderEngine implements EncoderEngine {

  /** The registered encoder listeners */
  protected List<EncoderListener> listeners = new CopyOnWriteArrayList<EncoderListener>();

  /** True if the engine supports multiple jobs at once */
  protected boolean supportsMultithreading = false;

  /** The supported profiles */
  protected Map<String, EncodingProfile> supportedProfiles = null;

  /** the logging facility provided by log4j */
  private static Logger logger = LoggerFactory.getLogger(AbstractEncoderEngine.class.getName());

  /**
   * Creates a new abstract encoder engine with or without support for multiple job submission.
   * 
   * @param supportsMultithreading
   *          <code>true</code> if this engine supports more than one job at a time
   */
  public AbstractEncoderEngine(boolean supportsMultithreading) {
    this.supportsMultithreading = supportsMultithreading;
    this.supportedProfiles = new HashMap<String, EncodingProfile>();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#addEncoderListener(org.opencastproject.composer.api.EncoderListener)
   */
  public void addEncoderListener(EncoderListener listener) {
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#removeEncoderListener(org.opencastproject.composer.api.EncoderListener)
   */
  public void removeEncoderListener(EncoderListener listener) {
    listeners.remove(listener);
  }

  /**
   * Returns the location of the output file.
   * 
   * @param source
   *          the source file
   * @param profile
   *          the encoding profile
   * @return the output file
   */
  protected abstract File getOutputFile(File source, EncodingProfile profile);

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#supportsMultithreading()
   */
  public boolean supportsMultithreading() {
    return supportsMultithreading;
  }

  /**
   * Sets the supported profiles for this encoder engine. The method is called by the corresponding factory that knows
   * about the relation between encoding engine and encoding profiles.
   * 
   * @param profiles
   *          the supported profiles
   */
  void setSupportedProfiles(Map<String, EncodingProfile> profiles) {
    if (profiles == null) {
      profiles = new HashMap<String, EncodingProfile>();
    }
    this.supportedProfiles = profiles;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#supportsProfile(java.lang.String,
   *      org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  @Override
  public boolean supportsProfile(String profile, MediaType type) {
    if (supportedProfiles.containsKey(profile)) {
      EncodingProfile p = supportedProfiles.get(profile);
      return p.isApplicableTo(type);
    }
    return false;
  }

  /**
   * Downloads the track to the <code>java.io.tmpdir</code> directory if does not not already have a <code>file:</code>
   * url associated.
   * 
   * @param track
   *          the track object
   * @return a file reference to the track
   * @throws IOException
   *           if downloading the file fails
   * 
   */
  protected File download(Track track) throws IOException {
    if (track == null || track.getURI() == null) {
      throw new IOException("Caller provided either a null track or a track without a URI");
    }
    if ("file".equals(track.getURI().getScheme()))
      return new File(track.getURI().getPath());

    // The file does not seem to be inside the local filesystem.
    // Let's download it and log a warning, since this shouldn't happen.
    logger.warn("Downloading track " + track + " to temp directory");
    File f = File.createTempFile(track.getURI().toString(), null);
    FileOutputStream fos = new FileOutputStream(f);
    InputStream is = track.getURI().toURL().openStream();
    byte[] bytes = new byte[2048];
    int offset = 0;
    int len = 0;
    while (is.read(bytes, offset, len) >= 0) {
      fos.write(bytes, offset, len);
    }
    fos.flush();
    fos.close();
    is.close();

    return f;
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

}
