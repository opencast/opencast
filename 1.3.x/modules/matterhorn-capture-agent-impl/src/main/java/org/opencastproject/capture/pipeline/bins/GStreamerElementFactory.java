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

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;

public final class GStreamerElementFactory {
  private static GStreamerElementFactory factory;

  /** Singleton factory pattern **/
  public static synchronized GStreamerElementFactory getInstance() {
    if (factory == null) {
      factory = new GStreamerElementFactory();
    }
    return factory;
  }

  /** Private so that we can control how many factories there are. **/
  private GStreamerElementFactory() {
    // Nothing to do here
  }

  /**
   * Creates a GStreamer Element
   * 
   * @param captureDeviceName
   *          Used to create a useful error message for the Exception.
   * @param elementType
   *          Specifies the GStreamer Element type to create.
   * @param elementFriendlyName
   *          Optional friendly name for the Element.
   * @throws UnableToCreateElementException
   *           Thrown if the Element cannot be created because the Element doesn't exist on this machine.
   * **/
  public Element createElement(String captureDeviceName, String elementType, String elementFriendlyName)
          throws UnableToCreateElementException {
    try {
      ElementFactory codecFactory = ElementFactory.find(elementType);
      return codecFactory.create(elementFriendlyName);
    } catch (IllegalArgumentException e) {
      throw new UnableToCreateElementException(captureDeviceName, elementType);
    }
  }

}
