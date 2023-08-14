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

package org.opencastproject.silencedetection.impl;

/**
 * SilenceDetectionService properties.
 */
public interface SilenceDetectionProperties {

  /** Timespan in milliseconds before silece cut begin. */
  String SILENCE_PRE_LENGTH = "silence.pre.length";
  /** Minimum length in milliseconds to accept silence sequence. */
  String SILENCE_MIN_LENGTH = "silence.min.length";
  /** Silence threshold in decibel (e.g. -50 for loud classrooms, -35 for very silent indoor location). */
  String SILENCE_THRESHOLD_DB = "silence.threshold.db";
  /* Minimum (voice) segment length in milliseconds. */
  String VOICE_MIN_LENGTH = "voice.min.length";
}
