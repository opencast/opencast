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

package org.opencastproject.util;

import static com.entwinemedia.fn.Prelude.chuck;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.Unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

/**
 * A StreamConsumer helps to asynchronously consume a text input stream line by line.
 * The consumer guarantees the closing of the stream.
 */
public class StreamConsumer implements Runnable {
  private final CountDownLatch running = new CountDownLatch(1);
  private final CountDownLatch ready = new CountDownLatch(1);
  private final CountDownLatch finished = new CountDownLatch(1);

  private final Fn<String, Boolean> consumer;

  private boolean stopped = false;
  private InputStream stream;
  private BufferedReader reader;

  /**
   * Create a new stream consumer.
   *
   * @param consumer
   *         a predicate function that may stop reading further lines by returning <code>false</code>
   */
  public StreamConsumer(Fn<String, Boolean> consumer) {
    this.consumer = consumer;
  }

  @Override public void run() {
    try {
      running.countDown();
      ready.await();
      // also save a reference to the reader to able to close it in stopReading
      // otherwise the read loop may continue reading from the buffer
      reader = new BufferedReader(new InputStreamReader(stream));
      IoSupport.withResource(reader, consumeBuffered);
      finished.countDown();
    } catch (InterruptedException e) {
      chuck(e);
    }
  }

  /** Wait for the executing thread to run. */
  public void waitUntilRunning() {
    try {
      running.await();
    } catch (InterruptedException e) {
      chuck(e);
    }
  }

  /** Wait until the stream has been fully consumed. */
  public void waitUntilFinished() {
    try {
      finished.await();
    } catch (InterruptedException e) {
      chuck(e);
    }
  }

  /** Forcibly stop consuming the stream. */
  public void stopConsuming() {
    if (stream != null) {
      stopped = true;
      IoSupport.closeQuietly(stream);
      IoSupport.closeQuietly(reader);
    }
  }

  /** Start consuming <code>stream</code>. It is guaranteed that the stream gets closed. */
  public void consume(InputStream stream) {
    waitUntilRunning();
    this.stream = stream;
    ready.countDown();
  }

  private final Fn<BufferedReader, Unit> consumeBuffered = new Fx<BufferedReader>() {
    @Override public void ap(BufferedReader reader) {
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          if (!consumer.ap(line)) {
            stopConsuming();
          }
        }
      } catch (IOException e) {
        if (!stopped) {
          chuck(e);
        }
      }
    }
  }.toFn();
}
