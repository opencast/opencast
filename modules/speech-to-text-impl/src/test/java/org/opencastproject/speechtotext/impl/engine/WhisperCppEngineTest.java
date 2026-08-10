/*
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

package org.opencastproject.speechtotext.impl.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class WhisperCppEngineTest {

  private Pattern outputFailurePattern() throws Exception {
    Field field = WhisperCppEngine.class.getDeclaredField("OUTPUT_FAILURE_PATTERN");
    field.setAccessible(true);
    return (Pattern) field.get(null);
  }

  /**
   * WhisperC++ reports an unwritable output file on stdout and still exits with status 0.
   * These are the lines from the log in issue #5747.
   */
  @Test
  public void testDetectsFailureToWriteOutputFiles() throws Exception {
    Pattern pattern = outputFailurePattern();

    assertTrue(pattern.matcher(
        "output_vtt: failed to open '/srv/opencast/workspace/collection/subtitles/job-12146265/foo.vtt' "
            + "for writing").find());
    assertTrue(pattern.matcher(
        "output_json: failed to open '/srv/opencast/workspace/collection/subtitles/job-12146265/foo.json' "
            + "for writing").find());
    assertTrue(pattern.matcher("output_srt: failed to open '/tmp/foo.srt' for writing").find());
  }

  /**
   * Ordinary transcription output must not be mistaken for a write failure, or every job would
   * fail. The second line matters most: transcribed speech can contain arbitrary text, including
   * text quoting an error message.
   */
  @Test
  public void testIgnoresRegularOutput() throws Exception {
    Pattern pattern = outputFailurePattern();

    assertFalse(pattern.matcher("whisper_model_load: loading model").find());
    assertFalse(pattern.matcher(
        "[00:00:12.000 --> 00:00:15.000]  output_vtt: failed to open 'x' for writing").find());
    assertFalse(pattern.matcher("output_vtt: saving output to '/tmp/foo.vtt'").find());
    assertFalse(pattern.matcher("").find());
  }
}
