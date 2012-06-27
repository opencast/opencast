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

import org.opencastproject.capture.pipeline.GStreamerPipeline;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GStreamerCapture is a bridge class that allows the CaptureAgent to use GStreamer to capture from devices or files
 * without knowing about the GStreamer SDK.
 **/
public class GStreamerCaptureFramework implements CaptureFramework {
  
  private static final Logger logger = LoggerFactory.getLogger(GStreamerCaptureFramework.class);
  
  /** The pipeline to use to capture. **/
  private GStreamerPipeline gstreamerPipeline;
  
  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   * 
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext ctx) {
  
  }
  
  /** 
   * Callback from the OSGI container to shutdown this service. 
   */
  public void deactivate() {
    
  }
  
  /**
   * Start capturing using GStreamer for the recording newRec.
   * 
   * @param newRec
   *          The details of the recording that is about to start (e.g. Name, duration etc.)
   * @param captureFailureHandler
   *          Who to call if the capture fails to fire.
   **/
  @Override
  public void start(RecordingImpl newRec, CaptureFailureHandler captureFailureHandler) {
    gstreamerPipeline = new GStreamerPipeline(captureFailureHandler);
    gstreamerPipeline.start(newRec);
  }

  /** Signal the pipeline to stop capturing. **/
  @Override
  public void stop(long timeout) {
    if (gstreamerPipeline != null) {
      gstreamerPipeline.stop(timeout);
    }
    else {
      logger.warn("Pipeline was asked to stop when it is null.");
    }
  }

  /** Check to see if this was a mock capture. **/
  @Override
  public boolean isMockCapture() {
    if (gstreamerPipeline == null) {
      return true;
    }
    return gstreamerPipeline.isPipelineNull();
  } 
}