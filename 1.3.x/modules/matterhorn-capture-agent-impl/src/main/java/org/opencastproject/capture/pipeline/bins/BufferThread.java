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
package org.opencastproject.capture.pipeline.bins;

import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.Message;
import org.gstreamer.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferThread implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(BufferThread.class);
  private static final int MILLISECONDS_BETWEEN_CHECKS = 60000;
  private Element queue = null;
  private boolean run = true;

  /**
   * A Quick and dirty logging class. This will only be created when the logging level is set to TRACE. It's sole
   * purpose is to output the three limits on the buffer for each device
   * 
   * @param newQueue
   *          The GStreamer Element queue that we will be logging.
   */
  public BufferThread(Element newQueue) {
    log.info("Buffer monitoring thread started for device " + newQueue.getName());
    queue = newQueue;

    queue.getBus().connect(new Bus.MESSAGE() {
      @Override
      public void busMessage(Bus arg0, Message arg1) {
        if (arg1.getType().equals(MessageType.EOS)) {
          log.info("Shutting down buffer monitor thread for {}.", queue.getName());
          shutdown();
        }
      }
    });

  }

  /** Checks the buffer, bytes and time on the queue at every tick. **/
  public void run() {
    while (run) {
      log.trace(queue.getName() + "," + queue.get("current-level-buffers") + "," + queue.get("current-level-bytes")
              + "," + queue.get("current-level-time"));
      try {
        Thread.sleep(MILLISECONDS_BETWEEN_CHECKS);
      } catch (InterruptedException e) {
        log.trace(queue.getName() + "'s buffer monitor thread caught an InterruptedException but is continuing.");
      }
    }
    log.trace(queue.getName() + "'s buffer monitor thread hit the end of the run() function.");
  }

  public void shutdown() {
    run = false;
  }
}
