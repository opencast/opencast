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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderEngineFactory;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine;

import org.osgi.service.component.ComponentContext;

/**
 * A simple encoder engine factory that always returns an ffmpeg encoder engine, regardless of which profile is
 * specified.
 */
public class EncoderEngineFactoryImpl implements EncoderEngineFactory {
  protected ComponentContext cc;

  protected void activate(ComponentContext cc) {
    this.cc = cc;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.EncoderEngineFactory#newEncoderEngine(org.opencastproject.composer.api.EncodingProfile)
   */
  @Override
  public EncoderEngine newEncoderEngine(EncodingProfile profile) {
    FFmpegEncoderEngine engine = new FFmpegEncoderEngine();
    engine.activate(cc);
    return engine;
  }
}
