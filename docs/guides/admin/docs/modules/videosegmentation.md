Video Segmentation Configuration
================================

What is Video Segmentation
--------------------------

Video segmentation is a way of dividing a movie into meaningful segments. In the context of lecture capture,
segmentation is best applied to captured screen presentation, that the presenter goes through slide after slide.

As a result, the video segmentation returns the exact times of slide changes on the timeline, which allows for
sophisticated ways for the learner to browse the lecture content, as shown in the slides section of the Opencast Player.


How the video segmentation process works
----------------------------------------

For detecting new scenes, Opencast uses the scene detection build into the FFmpeg select filter. The basic idea behind
this filter is to compare to consecutive frames and decide if the second frame belongs to a new scene based on the
difference.


What can be optimized
---------------------

The segmentation does not yield perfect results for all scenarios if always the same parameters for the FFmpeg filter
are used. Especially for presentations that include live handwriting or small videos often way too many segments are
created. In these special cases the difference between two consecutive frames is much higher than for normal
presentation slides and these big differences happen very often, whereby many segments would be found.
To improve the resulting number of segments, different FFmpeg parameters are tried out and to prevent having segments
that are too short to be reasonably clickable, too short segments are filtered out.


How the Optimization works
--------------------------

In general the optimization repeats a cycle of calling the FFmpeg filter, merging too small segments and calculating a
new changes threshold until the segmentation is good enough.

Additional to calling the FFmpeg function there is a filter function that merges small segments to a bigger segment or
splits it to the surrounding segments if the resulting segment would be too small. This can be beneficial for example
if a video is shown in a lecture, so that the video will be only one segment and not many short segments.
The stability threshold is used in the filter method to determine which segments are long enough and which should be
merged.

First the segmentation is run with a stability threshold of 1 second and the initial changes threshold, that can be
changed in the options. If the segmentation or the filtered segmentation doesn't deviate more from the preferred number
of segments than the maximum error allows, the optimization is done. If not, a new changes threshold, that should yield
better results, is calculated and the segmentation is run again until the segmentation is good enough or until the
maximum number of cycles is reached.



Configuration
-------------

The value for the frame difference as well as the minimum length for a segment, the preferred number of segments, the
maximum error and the maximum number of cycles can be configured in
`etc/org.opencastproject.videosegmenter.ffmpeg.VideoSegmenterServiceImpl.cfg`.

The options that can be set are the minimum length of a segment (defaults to 60 sec).

    stabilitythreshold = 60

The percentage of pixels that may change between two frames without considering them different (defaults to 0.025).

    changesthreshold = 0.025

The number of segments that the segmentation optimally should yield (defaults to 30).

    prefNumber = 30

The maximum error for the number of segments in percent. As soon as a segmentation with a lower error is achieved the
optimization will be ended (defaults to 0.25).

    maxError = 0.25

The maximum number of times the optimization will call the FFmpeg select filter (defaults to 3).

    maxCycles = 3

The absolute maximum number of segments. If at the end of the optimization more segments than this are found, instead a
uniform segmentation will be generated (defaults to 150).

    absoluteMax = 150

The absolute minimum number of segments. If at the end of the optimization less segments than this are found, instead a
uniform segmentation will be generated (defaults to 3).

    absoluteMin = 3

This parameter controls whether the options prefNumber, absoluteMax and absoluteMin are interpreted as absolute
segment numbers or relative to track duration. If this is set to true, prefNumber, absoluteMax and absoluteMin will be
interpreted as number of segments per hour. (defaults to false)

    durationDependent = false
