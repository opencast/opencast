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
package org.opencastproject.capture.impl;

/** Interface used to define a framework to use when capturing from devices / files. **/
public interface CaptureFramework {
  /**
   * Start capturing from devices / files.
   * 
   * @param newRec
   *          The details of the recording to start including its unique ID and which devices to use while capturing.
   * @param captureFailureHandler
   *          The class to call if something goes wrong.
   **/
  void start(RecordingImpl newRec, CaptureFailureHandler captureFailureHandler);

  /** Stop capturing from devices / files. **/
  void stop(long timeout);

  /** Checks to see if this is a mock capture. **/
  boolean isMockCapture();
}
