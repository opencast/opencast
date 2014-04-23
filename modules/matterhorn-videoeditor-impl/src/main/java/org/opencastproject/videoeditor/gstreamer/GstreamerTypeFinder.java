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

package org.opencastproject.videoeditor.gstreamer;

import java.util.LinkedList;
import java.util.List;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.DecodeBin2;
import org.gstreamer.lowlevel.MainLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input media type detection service using Gstreamer.
 */
public class GstreamerTypeFinder {
  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(GstreamerTypeFinder.class);

  private final Pipeline pipeline;
  private final MainLoop mainLoop = new MainLoop();

  private final List<Caps> capsFound = new LinkedList<Caps>();
  private Caps audioCaps = null;
  private Caps videoCaps = null;

  /**
   * Run media type detection on source file.
   *
   * @param filePath source file to determine media type
   */
  public GstreamerTypeFinder(String filePath) {
    pipeline = new Pipeline();

    Element filesrc = ElementFactory.make(GstreamerElements.FILESRC, null);
    DecodeBin2 decodebin = new DecodeBin2("dec");

    pipeline.addMany(filesrc, decodebin);
    filesrc.link(decodebin);

    filesrc.set("location", filePath);
    decodebin.set("expose-all-streams", true);
    decodebin.connect(new DecodeBin2.AUTOPLUG_CONTINUE() {

      @Override
      public boolean autoplugContinue(DecodeBin2 element, Pad pad, Caps caps) {
        logger.debug("found caps: " + caps.toString());
        capsFound.add(caps);
        return true;
      }
    });
    decodebin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element element, Pad pad) {
        if (pad.getCaps().isAlwaysCompatible(Caps.fromString("audio/x-raw-int; audio/x-raw-float")))
          audioCaps = pad.getCaps();
        else if (pad.getCaps().isAlwaysCompatible(Caps.fromString("video/x-raw-rgb; video/x-raw-yuv")))
          videoCaps = pad.getCaps();
        else capsFound.add(pad.getCaps());

        logger.debug("found final caps: " + pad.getCaps());
      }
    });
    decodebin.connect(new DecodeBin2.UNKNOWN_TYPE() {

      @Override
      public void unknownType(DecodeBin2 element, Pad pad, Caps caps) {
        logger.debug("unknown type event: pad {}, caps {}", new String[] {
          pad.getName(), caps.toString()
        });
        mainLoop.quit();
      }
    });
    decodebin.connect(new Element.NO_MORE_PADS() {

      @Override
      public void noMorePads(Element element) {
        mainLoop.quit();
      }
    });

    pipeline.getBus().connect(new Bus.EOS() {

      @Override
      public void endOfStream(GstObject source) {
        logger.info("EOS from " + source.getName());
        mainLoop.quit();
      }
    });

    pipeline.getBus().connect(new Bus.ERROR() {

      @Override
      public void errorMessage(GstObject source, int code, String message) {
        mainLoop.quit();
      }
    });

    filesrc.disown();
    decodebin.disown();

    pipeline.play();
    mainLoop.run();
    pipeline.stop();
    logger.debug("type find job done");
  }

  /**
   * Returns found Gstreamer caps.
   * @return found caps
   */
  public List<Caps> getFoundCaps() {
    return capsFound;
  }

  /**
   * Returns true if source file has an audio stream.
   * @return true if source file has an audio stream, false otherwise
   */
  public boolean isAudioFile() {
    return audioCaps != null;
  }

  /**
   * Returns true if source file has an video stream.
   * @return true if source file has an video stream, false otherwise
   */
  public boolean isVideoFile() {
    return videoCaps != null;
  }

  /**
   * Returns found audio caps.
   * @return found audio caps
   */
  public Caps getAudioCaps() {
    Caps audioCaps = this.audioCaps;
    for (Caps c : capsFound) {
      if (c.getStructure(0).getName().startsWith("audio/")
              && !isContainerCaps(c)) {
//        audioCaps = c;
        return c;
      }
    }

    return audioCaps;
  }

  /**
   * Returns raw (final) audio caps.
   * @return raw (final) audio caps
   */
  public Caps getRawAudioCaps() {
    return audioCaps;
  }

  /**
   * Returns found video caps.
   * @return found video caps
   */
  public Caps getVideoCaps() {
    Caps videoCaps = this.videoCaps;
    for (Caps c : capsFound) {
      if (c.getStructure(0).getName().startsWith("video/")
              && !isContainerCaps(c)) {
//        videoCaps = c;
        return c;
      }
    }

    return videoCaps;
  }

  /**
   * Returns raw (final) video caps.
   * @return raw (final) video caps
   */
  public Caps getRawVideoCaps() {
    return videoCaps;
  }

  /**
   * Returns true, if caps describe an container format.
   * @param caps caps to detect container or not
   * @return true if caps describe an container format
   */
  private boolean isContainerCaps(Caps caps) {

    String capsName = caps.getStructure(0).getName();
    if ("video/x-ms-asf".equals(capsName)
            || "video/x-msvideo".equals(capsName)
            || "video/x-dv".equals(capsName)
            || "video/x-matroska".equals(capsName)
            || "application/ogg".equals(capsName)
            || "application/ogg".equals(capsName)
            || "video/quicktime".equals(capsName)
            || "application/vnd.rn-realmedia".equals(capsName)
            || "audio/x-wav".equals(capsName))
      return true;

    if ("video/mpeg".equals(capsName)
            && caps.getStructure(0).hasField("systemstream")
            && caps.getStructure(0).getBoolean("systemstream"))
      return true;

    if ("video/x-dv".equals(capsName)
            && caps.getStructure(0).hasField("systemstream")
            && caps.getStructure(0).getBoolean("systemstream"))
      return true;

    return false;
  }
}
