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

import org.opencastproject.capture.pipeline.GStreamerPipeline;

import org.gstreamer.Bin;
import org.gstreamer.State;

/**
 * Abstract implementation of {@link EpiphanVGA2USBV4LSubBin}. Creates a empty Bin and implements sub bin state change
 * methods.
 */
public abstract class EpiphanVGA2USBV4LSubAbstractBin implements EpiphanVGA2USBV4LSubBin {

  /** Bin where to store Elements. */
  protected Bin bin;

  /**
   * Constructor. Creates a empty sub bin.
   */
  public EpiphanVGA2USBV4LSubAbstractBin() {
    bin = new Bin();
  }

  /**
   * Constructor. Creates a named empty sub bin.
   * 
   * @param binName
   *          sub bin name
   */
  public EpiphanVGA2USBV4LSubAbstractBin(String binName) {
    bin = new Bin(binName);
  }

  /**
   * Returns the bin.
   * 
   * @return the bin
   */
  public Bin getBin() {
    return bin;
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#start(long)
   */
  @Override
  public boolean start(long time) {
    return setState(State.PLAYING, time);
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#stop()
   */
  @Override
  public void stop() {
    setState(State.NULL, -1);
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#setState(State, long)
   */
  @Override
  public boolean setState(State state, long time) {
    bin.setState(state);
    if (time < 0) {
      return true;
    } else {
      return bin.getState(time * GStreamerPipeline.GST_SECOND) == state;
    }
  }

}
