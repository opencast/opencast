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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Simple test the constructor and get methods for this basic class that represents a capture device.
 */
public class CaptureDeviceTest {

  @Test
  public void testCaptureDevice() {
    ProducerType device = ProducerType.FILE;
    String source = "source";
    String dest = "destination";
    String fname = "TEST";
    CaptureDevice captureDevice = new CaptureDevice(source, device, fname, dest);
    Assert.assertEquals(source, captureDevice.getLocation());
    Assert.assertEquals(dest, captureDevice.getOutputPath());
    Assert.assertEquals(device, captureDevice.getName());
    Assert.assertEquals(fname, captureDevice.getFriendlyName());
    System.out.println(captureDevice.toString());
  }

}
