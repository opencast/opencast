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

  /** The input stream */
  private InputStream inputStream;

  /** The output stream */
  private OutputStream outputStream;

  /** The content buffer */
  protected StringBuffer contentBuffer = null;

  /** the output writer */
  protected PrintWriter writer = null;

  /** Append messages to this logger */
  protected Logger logger = null;

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
   * @param logger
   *          the logger to append to
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, Logger logger, StringBuffer contentBuffer) {
    this(inputStream, null, logger, contentBuffer);
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
   * @param logger
   *          the logger to append to
   * @param contentBuffer
   *          the buffer to write the captured output to
   */
  public StreamHelper(InputStream inputStream, OutputStream redirect, Logger logger, StringBuffer contentBuffer) {
    this.inputStream = inputStream;
    this.outputStream = redirect;
    this.logger = logger;
    this.contentBuffer = contentBuffer;
    start(); // FIXME This class should either be final, or should not start threads in the constructor.
  }

  /**
   * Tells the stream helper to stop reading and exit from the main loop.
   */
  public void stopReading() {
    keepReading = false;
  }

  /**
   * Thread run
   */
  @Override
  public void run() {
    BufferedReader reader = null;
    InputStreamReader isreader = null;
    try {
      if (outputStream != null) {
        writer = new PrintWriter(outputStream);
      }
      isreader = new InputStreamReader(inputStream);
      reader = new BufferedReader(isreader);
      if (reader.ready()) {
        String line = reader.readLine();
        while (keepReading && reader.ready() && line != null) {
          append(line);
          log(line);
          line = reader.readLine();
        }
        if (writer != null)
          writer.flush();
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      IoSupport.closeQuietly(reader);
      IoSupport.closeQuietly(isreader);
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
    if (logger != null) {
      logger.info(output);
    }
  }

}
