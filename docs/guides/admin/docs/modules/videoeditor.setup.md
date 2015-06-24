# Video Editor: Setup

## Installation
Since Matterhorn 2.0 the videoeditor modules use ffmpeg for the prcessing. An ffmpeg version > 2.4 is hardly recommended-

## Configuration

### UI Config File Parameters

Currently there are two config file parameters for UI options.

The config file can be found at *etc/load/org.opencastproject.organization-mh_default_org.cfg*

 1. prop.adminui.prePostRoll
   - Change the duration of the pre and post roll (in seconds)
 2. prop.adminui.minSegmentLength
   - Change the minimum required segment length (in seconds)

### Silence Detection Config Parameters
The settings regarding the sensitivity of the silence detection can be changed in *etc/services/org.opencastproject.silencedetection.impl.SilenceDetectionServiceImpl.properties*.

 1. silence.pre.length
  - Duration of silence that should be included at the beginning of  a new voice segment. This is to avoid that a cut seems to sudden.
  - Default: 2000 (2s)
 2. silence.threshold.db
  - Silence threshold in decibel (e.g. -50 for loud classrooms, -35 for silent indoor location).
  - Default: -40
 3. silence.min.length
  - Minimum duration in milliseconds to detect a sequence as silence.
  - Default: 10000 (10s)
 4. voice.min.length
  - Minimum segment duration in milliseconds to start a new voice containing sequence after a silent sequence.
  - Default: 60000 (1min)

### Video Editor Config Parameters
The ffmpeg properties for the Video Editor can be modified in etc/services/org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.properties. Usually there should be no reason to touch this file. 
