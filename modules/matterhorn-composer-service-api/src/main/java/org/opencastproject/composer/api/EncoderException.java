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


package org.opencastproject.composer.api;

/**
 * This exception may be thrown by an encoder.
 */
public class EncoderException extends Exception {

  /** serial version uid */
  private static final long serialVersionUID = -8883994454091884800L;

  /** The engine that threw the exception */
  private EncoderEngine engine = null;

  /** Exit code of external processes */
  private int exitCode = -1;

  /**
   * Creates a new encoder exception with the given error message.
   *
   * @param message
   *          the error message
   */
  public EncoderException(String message) {
    super(message);
  }

  /**
   * Creates a new encoder exception with the given error message, caused by the given exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the error cause
   */
  public EncoderException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new encoder exception, caused by the given exception.
   *
   * @param cause
   *          the error cause
   */
  public EncoderException(Throwable cause) {
    super(cause);
  }

  /**
   * @param engine
   *          the compression engine
   * @param message
   *          the error message
   */
  public EncoderException(EncoderEngine engine, String message) {
    super(message);
    this.engine = engine;
  }

  /**
   * @param engine
   *          the compression engine
   * @param cause
   *          the original cause
   */
  public EncoderException(EncoderEngine engine, Throwable cause) {
    super(cause);
    this.engine = engine;
  }

  /**
   * @param engine
   *          the compression engine
   * @param message
   *          the error message
   * @param cause
   *          the original cause
   */
  public EncoderException(EncoderEngine engine, String message, Throwable cause) {
    super(message, cause);
    this.engine = engine;
  }

  /**
   * Returns the compression engine that threw the exception.
   *
   * @return the engine
   */
  public EncoderEngine getEngine() {
    return engine;
  }

  /**
   * Returns the exit code of the process if it was not 0. If the exception wasn't caused by an exit code unequal to 0,
   * -1 is returned.
   *
   * @return the exit code
   */
  public int getExitCode() {
    return exitCode;
  }

}
