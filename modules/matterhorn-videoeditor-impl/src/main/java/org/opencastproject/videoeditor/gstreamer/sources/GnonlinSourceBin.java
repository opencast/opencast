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
package org.opencastproject.videoeditor.gstreamer.sources;

import java.util.concurrent.TimeUnit;
import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.videoeditor.gstreamer.GstreamerElements;
import org.opencastproject.videoeditor.gstreamer.VideoEditorPipeline;
import org.opencastproject.videoeditor.gstreamer.exceptions.PipelineBuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gstreamer source bin factory class using Gnonlin elements.
 */
public class GnonlinSourceBin {
  
  /** Media source types */
  public static enum SourceType {
    Audio, Video
  }
  
  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(VideoEditorPipeline.class);
  
  /** Media source type */
  private final SourceType type;
  /** Gstreamer source bin */
  private final Bin bin;
  /** Gnonlin composition bin, contains Gnonlin source elements (for each segment) */
  private final Bin gnlComposition;
  /** Source media caps */
  private final Caps caps;
  
  /** Bin's max duration in millisecond */
  private long maxLengthMillis = 0L;
  
  /**
   * Creates Gstreamer source bin with Gnonlin composition inside.
   * 
   * @param type source media type
   * @param sourceCaps source media caps
   * @throws UnknownSourceTypeException if mediatype can't be processed
   * @throws PipelineBuildException 
   */
  GnonlinSourceBin(SourceType type, Caps sourceCaps) throws PipelineBuildException {
    this.type = type;
    
    bin = new Bin();
    gnlComposition = (Bin) ElementFactory.make(GstreamerElements.GNL_COMPOSITION, null);
    final Element identity = ElementFactory.make(GstreamerElements.IDENTITY, null);
    final Element queue = ElementFactory.make(GstreamerElements.QUEUE, null);
    final Element converter;
    final Element rate;
    switch(type) {
      case Audio: 
        converter = ElementFactory.make(GstreamerElements.AUDIOCONVERT, null);
        rate = ElementFactory.make(GstreamerElements.AUDIORESAMPLE, null);
        if (sourceCaps != null)
          caps = sourceCaps;
        else 
          caps = Caps.fromString("audio/x-raw-int; audio/x-raw-float");
        
        break;
      case Video: 
        converter = ElementFactory.make(GstreamerElements.FFMPEGCOLORSPACE, null);
        rate = ElementFactory.make(GstreamerElements.VIDEORATE, null);
        if (sourceCaps != null)
          caps = sourceCaps;
        else 
          caps = Caps.fromString("video/x-raw-yuv; video/x-raw-rgb");
        break;
      default:
        // can't pass
        throw new PipelineBuildException();
    }
    
    bin.addMany(gnlComposition, identity, converter, rate, queue);
    if (!Element.linkMany(identity, converter, rate, queue)) {
      throw new PipelineBuildException();
    }
    
    if (type == SourceType.Video)
      identity.set("single-segment", true);
//    identity.set("check-imperfect-timestamp", true);
//    identity.set("check-imperfect-offset", true);
    
    Pad srcPad = queue.getSrcPads().get(0);
    bin.addPad(new GhostPad(srcPad.getName(), srcPad));
    
    gnlComposition.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element source, Pad pad) {
        logger.debug("new pad added {}.{} (caps: {}): ", new String[] {
          source.getName(), pad.getName(), pad.getCaps().toString()
        });

        logger.debug("link {}.{} -> {}.{} with result {}", new String[] {
          source.getName(),
          pad.getName(),
          identity.getName(),
          identity.getSinkPads().get(0).getName(),
          pad.link(identity.getSinkPads().get(0)).toString()
        });
      }
    });
    
    gnlComposition.connect(new Element.NO_MORE_PADS() {

      @Override
      public void noMorePads(Element element) {
        if (!identity.getSinkPads().get(0).isLinked()) {
          logger.error(identity.getName() + " has no peer!");
          getBin().sendEvent(new EOSEvent());
          
        }
      }
    });
  }
  
  /**
   * Add new segment.
   * @param filePath source file
   * @param mediaStartMillis medi start mosition (in milliseconds)
   * @param mediaDurationMillis segment duration (in milliseconds)
   */
  void addFileSource(String filePath, long mediaStartMillis, long mediaDurationMillis) {
    
    Bin gnlsource = (Bin) ElementFactory.make(GstreamerElements.GNL_FILESOURCE, null);
    gnlComposition.add(gnlsource);
        
    gnlsource.set("location", filePath);
    gnlsource.set("caps", caps);
    gnlsource.set("start", TimeUnit.MILLISECONDS.toNanos(maxLengthMillis));
    gnlsource.set("duration", TimeUnit.MILLISECONDS.toNanos(mediaDurationMillis));
    gnlsource.set("media-start", TimeUnit.MILLISECONDS.toNanos(mediaStartMillis));
    gnlsource.set("media-duration", TimeUnit.MILLISECONDS.toNanos(mediaDurationMillis));
    
    maxLengthMillis += mediaDurationMillis;
  }
  
  /**
   * Returns source Pad.
   * @return source pad
   */
  public Pad getSrcPad() {
    return getBin().getSrcPads().get(0);
  }
  
  /**
   * Returns the Gstreamer source bin.
   * @return source bin
   */
  public Bin getBin() {
    return bin;
  }
  
  /**
   * Returns the length of producing media file in milliseconds.
   * @return length of producing media file in milliseconds.
   */
  public long getLengthMilliseconds() {
    return maxLengthMillis;
  }
  
  /**
   * Returns the input source type (audio or video).
   * @return producing source type
   */
  public SourceType getSourceType() {
    return type;
  }
}
