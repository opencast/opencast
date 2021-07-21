Trim Segments
=============

This documentation describes how to configure Opencast to trim the start and end of a video as a default.
By default, when trimming a video there are no trim segments at the start and at the end of the video. 
Adding trim segments would help resolve the issue of out of sync audio and video.

The video from a network camera is encoded (e.g. H264) which means when data is captured it could be between key frames 
and therefore video and audio could be out of sync when it gets processed by FFmpeg.

Setting the Configuration
-------------------------

####prop.admin.editor.segment.start_length

* Description: Length of trim segment at the beginning of each video.
* Format: An integer.
* Default: 0
* Measurement: Milliseconds.

####prop.admin.editor.segment.end_length=3000

* Description: Length of trim segment at the end of each video.
* Format: An integer.
* Default: 0
* Measurement: Milliseconds.

####prop.admin.editor.segment.minimum_length=1000

* Description: The minimum length of any one segment.
* Format: An integer.
* Default: 0
* Measurement: Milliseconds.

####prop.admin.editor.previewmode.default=true

* Description: If the preview mode in the video editor is enabled per default.
* Format: Boolean
* Default: true