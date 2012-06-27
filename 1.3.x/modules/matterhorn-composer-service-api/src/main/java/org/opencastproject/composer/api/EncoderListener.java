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

package org.opencastproject.composer.api;

import java.io.File;

/**
 * Interface for encoder listener.
 */
public interface EncoderListener {

  /**
   * Tells the listener that the given file has been encoded into the
   * {@link EncodingProfile} <code>profile</code>.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFiles
   *          the source files being encoded
   * @param profile
   *          the encoding profile
   */
  void fileEncoded(EncoderEngine engine, EncodingProfile profile, File... sourceFiles);

  /**
   * Tells the listener that the given file could not be encoded into
   * {@link EncodingProfile} <code>profile</code>.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFiles
   *          the source files being encoded
   * @param profile
   *          the encoding profile
   * @param cause
   *          the failure reason
   */
  void fileEncodingFailed(EncoderEngine engine, EncodingProfile profile, Throwable cause, File... sourceFiles);

  /**
   * Tells the listener about encoding progress while the file is being encoded into
   * {@link EncodingProfile} <code>profile</code>. The value ranges
   * between <code>0</code> (started) and <code>100</code> (finished).
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFile
   *          the file that was encoded
   * @param profile
   *          the encoding profile
   * @param progress
   *          the encoding progress
   */
  void fileEncodingProgressed(EncoderEngine engine, File sourceFile, EncodingProfile profile, int progress);

}
