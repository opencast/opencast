Video Segmentation Configuration
================================

What is Video Segmentation
--------------------------

Video segmentation is a way of dividing a movie into meaningful segments. In the context of lecture capture,
segmentation is best applied to captured screen presentation, that the presenter goes through slide after slide.

As a result, video segmentation returns the exact timepoints of slide changes on the timeline, which allows for
sophisticated ways for the learner to browse the lecture content, as shown in the slides section of the Matterhorn Media
Player.


How the video segmentation process works
----------------------------------------

For detecting new scenes, Matterhorn uses the scene detection build into the FFmpeg select filter. The basic idea behind
this filter is to compare to consecutive frames and decide if the second frame belongs to a new scene based on the
difference.


Configuration
-------------

The value for the frame difference as well as the minimum length for a segment can be configured in
`etc/services/org.opencastproject.videosegmenter.ffmpeg.VideoSegmenterServiceImpl.properties`.

The two options that can be set are the minimum length of a segment (defaults to 5 sec).

    stabilitythreshold = 5

The percentage of pixels that may change between tow frames without considering them different (defaults to 0.05).

    changesthreshold = 0.05
