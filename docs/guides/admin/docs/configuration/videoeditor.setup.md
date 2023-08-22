Video Editor: Setup
===================

Silence Detection Configuration
-------------------------------

The settings regarding the sensitivity of the silence detection can be changed in
`etc/org.opencastproject.silencedetection.impl.SilenceDetectionServiceImpl.cfg`.

1. silence.pre.length
    - Duration of silence that should be included at the beginning of  a new voice segment. This is to avoid that a cut
      seems to sudden.
    - Default: 2000 (2s)
2. silence.threshold.db
    - Silence threshold (e.g. -50dB for loud classrooms, -35dB for silent indoor location).
    - Default: -40dB
3. silence.min.length
    - Minimum duration in milliseconds to detect a sequence as silence.
    - Default: 10000 (10s)
4. voice.min.length
    - Minimum segment duration in milliseconds to start a new voice containing sequence after a silent sequence.
    - Default: 60000 (1min)

Video Editor Configuration
--------------------------

The FFmpeg properties for the Video Editor can be modified in
`etc/org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.cfg`. Usually there should be no reason to touch this
file.
