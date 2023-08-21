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

package org.opencastproject.transcription.microsoftazure;

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BinaryFileReader extends PullAudioInputStreamCallback {
  private static final Logger logger = LoggerFactory.getLogger(BinaryFileReader.class);
  private InputStream stream;

  public BinaryFileReader(String audioFileName) {
    try {
      stream = new FileInputStream(audioFileName);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage());
    }
  }

  @Override
  public int read(byte[] dataBuffer) {
    long ret = 0;

    try {
      ret = stream.read(dataBuffer, 0, dataBuffer.length);
    } catch (Exception ex) {
      logger.error("Read: " + ex);
    }

    return (int)Math.max(0, ret);
  }

  @Override
  public void close() {
    try {
      stream.close();
    } catch (IOException ex) {
      // ignored
    }
  }
}
