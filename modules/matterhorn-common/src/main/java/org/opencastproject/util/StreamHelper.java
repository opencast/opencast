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

package org.opencastproject.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Helper class to handle Runtime.exec() output.
 */
public class StreamHelper extends Thread {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StreamHelper.class);

  /** The input stream */
  private InputStream inputStream;

  /** The output stream */
  private OutputStream outputStream;

  /** The content buffer */
  protected StringBuffer contentBuffer = null;

  /** the output writer */
  protected PrintWriter writer = null;

  /** Append messages to this logger */
  protected Logger processLogger = null;

  /** True to keep reading the streams */
  protected boolean keepReading = true;

  /**
   * Creates a new stream helper and immediately starts capturing output from the given stream.
   *
   * @param inputStream
   *          the input stream
   */
  public StreamHelper(InputStream inputStream) {
    this(inputStream, null, null, null);
  }

  /**
   * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
   * to the given buffer.
   *
   * @param inputStream
   *          the input stream to read from
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, StringBuffer contentBuffer) {
    this(inputStream, null, null, contentBuffer);
  }

  /**
   * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
   * to the given buffer.
   *
   * @param inputStream
   *          the input stream to read from
   * @param processLogger
   *          the logger to append to
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, Logger processLogger, StringBuffer contentBuffer) {
    this(inputStream, null, processLogger, contentBuffer);
  }

  /**
   * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
   * to the given buffer and also redirected to the provided output stream.
   *
   * @param inputStream
   *          the input stream to read from
   * @param redirect
   *          a stream to also redirect the captured output to
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, OutputStream redirect, StringBuffer contentBuffer) {
    this(inputStream, redirect, null, contentBuffer);
  }

  /**
   * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
   * to the given buffer and also redirected to the provided output stream.
   *
   * @param inputStream
   *          the input stream to read from
   * @param redirect
   *          a stream to also redirect the captured output to
   * @param processLogger
   *          the logger to append to
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, OutputStream redirect, Logger processLogger, StringBuffer contentBuffer) {
    this.inputStream = inputStream;
    this.outputStream = redirect;
    this.processLogger = processLogger;
    this.contentBuffer = contentBuffer;
    start();
  }

  /**
   * Tells the stream helper to stop reading and exit from the main loop, it then waits for the thread to die.
   *
   * @see Thread#join()
   * @throws InterruptedException
   *           if the thread is interrupted while waiting for the main loop to come to an end
   */
  public void stopReading() throws InterruptedException {
    keepReading = false;
    this.join();
  }

  /**
   * Thread run
   */
  public void run() {

    BufferedReader bufferedReader = null;
    InputStreamReader streamReader = null;

    try {
      if (outputStream != null) {
        writer = new PrintWriter(outputStream);
      }
      streamReader = new InputStreamReader(inputStream);
      bufferedReader = new BufferedReader(streamReader);

      // Whether any content has been read
      boolean foundContent = false;

      // Keep reading either until there is nothing more to read from or we are told to stop waiting
      while (keepReading || foundContent) {
        while (!bufferedReader.ready()) {
          try {
            foundContent = false;
            Thread.sleep(100);
          } catch (InterruptedException e) {
            logger.debug("Closing process stream");
            return;
          }
          if (!keepReading && !bufferedReader.ready())
            return;
        }
        String line = bufferedReader.readLine();
        append(line);
        log(line);
        foundContent = true;
      }
      if (writer != null)
        writer.flush();
    } catch (IOException e) {
      if (keepReading)
        logger.error("Error reading process stream: {}", e.getMessage(), e);
    } catch (Throwable t) {
      logger.debug("Unknown error while reading from process input: {}", t.getMessage());
    } finally {
      IoSupport.closeQuietly(streamReader);
      IoSupport.closeQuietly(bufferedReader);
      IoSupport.closeQuietly(writer);
    }
  }

  /**
   * This method will write any output from the stream to the the content buffer and the logger.
   *
   * @param output
   *          the stream output
   */
  protected void append(String output) {
    // Process stream redirects
    if (writer != null) {
      writer.println(output);
    }

    // Fill the content buffer, if one has been assigned
    if (contentBuffer != null) {
      contentBuffer.append(output.trim());
      contentBuffer.append('\n');
    }

    // Append output to logger?
  }

  /**
   * If a logger has been specified, the output is written to the logger using the defined log level.
   *
   * @param output
   *          the stream output
   */
  protected void log(String output) {
    if (processLogger != null) {
      processLogger.info(output);
    }
  }

}
