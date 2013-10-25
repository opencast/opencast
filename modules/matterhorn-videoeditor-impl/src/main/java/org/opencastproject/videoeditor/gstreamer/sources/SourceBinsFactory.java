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

import java.io.FileNotFoundException;
import org.gstreamer.Bin;
import org.opencastproject.videoeditor.gstreamer.GstreamerTypeFinder;
import org.opencastproject.videoeditor.gstreamer.exceptions.InputSourceTypeException;
import org.opencastproject.videoeditor.gstreamer.exceptions.PipelineBuildException;

/**
 * Source bins factory class.
 */
public class SourceBinsFactory {

  private String outputFilePath = null;
  private GnonlinSourceBin audioSourceBin = null;
  private GnonlinSourceBin videoSourceBin = null;

  public SourceBinsFactory(String outputFilePath) {
    this.outputFilePath = outputFilePath;
  }

  /**
   * Create Gnonlin source bins for each media type from source file.
   * 
   * @param inputFilePath source file
   * @param mediaStartMillis sequence start position
   * @param durationMillis sequence duration
   * @throws FileNotFoundException source file not found
   * @throws UnknownSourceTypeException can not determine source type
   * @throws PipelineBuildException can not build source bin
   * @throws InputSourceTypeException if input file does not match source media type
   */
  public void addFileSource(String inputFilePath, long mediaStartMillis, long durationMillis)
          throws FileNotFoundException, PipelineBuildException, InputSourceTypeException {

    GstreamerTypeFinder typeFinder;
    typeFinder = new GstreamerTypeFinder(inputFilePath);

    if (typeFinder.isAudioFile()) {
      if (audioSourceBin == null) {
        audioSourceBin = new GnonlinSourceBin(GnonlinSourceBin.SourceType.Audio, typeFinder.getRawAudioCaps());
      }
      audioSourceBin.addFileSource(inputFilePath, mediaStartMillis, durationMillis);
    }
    
    if (typeFinder.isVideoFile()) {
      if (videoSourceBin == null) {
        videoSourceBin = new GnonlinSourceBin(GnonlinSourceBin.SourceType.Video, typeFinder.getRawVideoCaps());
      }
      videoSourceBin.addFileSource(inputFilePath, mediaStartMillis, durationMillis);
    }
  }

  /**
   * Returns true, if input sources has an audio stream.
   * @return true if produces audio
   */
  public boolean hasAudioSource() {
    return audioSourceBin != null;
  }

  /**
   * Returns true, if input sources has a video stream.
   * @return true if produces video
   */
  public boolean hasVideoSource() {
    return videoSourceBin != null;
  }
  
  /**
   * Returns the output file path.
   * @return output file path
   */
  public String getOutputFilePath() {
    return outputFilePath;
  }
  
  /**
   * Returns audio source bin.
   * @return audio source bin
   */
  public Bin getAudioSourceBin() {
    return audioSourceBin.getBin();
  }
  
  /**
   * Returns video source bin.
   * @return video source bin
   */
  public Bin getVideoSourceBin() {
    return videoSourceBin.getBin();
  }
}
