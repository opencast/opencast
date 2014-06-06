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
import org.opencastproject.composer.api.EncoderEngineFactory;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.gstreamer.engine.GStreamerEncoderEngine;

import org.gstreamer.Gst;
import org.osgi.service.component.ComponentContext;

/**
 * Factory that creates GStreamer ComposerEngine instance.
 */
public class GStreamerFactory implements EncoderEngineFactory {

  /**
   * Activates factory and initializes GStreamer.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    Gst.init();
  }

  /**
   * Deactivates and deinitializes GStreamer. NOTE: It seems that deinitialization is done asynchronously and is not
   * necessary completed by the time deinit returns. Calling init right after deinit can lead to unexpected behavior or
   * even seg fault of gstreamer.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    Gst.deinit();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.opencastproject.composer.api.EncoderEngineFactory#newEncoderEngine(org.opencastproject.composer.api.EncodingProfile
   * )
   */
  @Override
  public EncoderEngine newEncoderEngine(EncodingProfile profile) {
    return new GStreamerEncoderEngine();
  }
}
