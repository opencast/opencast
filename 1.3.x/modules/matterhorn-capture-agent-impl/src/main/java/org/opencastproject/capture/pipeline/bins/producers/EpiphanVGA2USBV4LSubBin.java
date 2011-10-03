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
package org.opencastproject.capture.pipeline.bins.producers;

import org.gstreamer.State;
import org.gstreamer.elements.AppSink;

/**
 * Interface of sub bins to use in {@link EpiphanVGA2USBV4LProducer}.
 */
interface EpiphanVGA2USBV4LSubBin {

  /**
   * Returns AppSink Element to get buffer from.
   * 
   * @return AppSink, the last Element in a bin.
   */
  AppSink getSink();

  /**
   * Start bin.
   * 
   * @param time
   *          time to check, if bin is starting, -1 skip checks.
   * @return true, if the bin started.
   */
  boolean start(long time);

  /**
   * Stop bin.
   */
  void stop();

  /**
   * Set bin to specified State.
   * 
   * @param state
   *          state to set.
   * @param time
   *          time to check state, -1 skip checks.
   * @return true, if bin is in specified state.
   */
  boolean setState(State state, long time);
  
  /** Sends an End of Stream signal to the source of this bin so that the media file can be closed properly. **/
  void shutdown();
}
