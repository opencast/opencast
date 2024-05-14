Timeline Previews Configuration
===============================

The Timeline previews service generates timeline preview images from a given video.
These are shown above the timeline of the video in the engage player.

The generation of these images is done using FFmpeg and all preview images are stored in one image file. To achieve
this, several FFmpeg video filters are combined:
- **fps**: to specify how many preview images should be generated. As the length of track is known and the desired
  number of preview images is configured in the workflow operation handler, it can be calculated how many seconds each
  preview image should represent. If 1 divided by that duration in seconds is set as fps value, FFmpeg will generate the
  desired amount of preview images.
- **scale**: to achieve the desired output image resolution. This filter will scale the input video, so that the
  generated images will have that resolution. If one of the values is set to -1, the filter will use a value that
  maintains the aspect ratio of the input image.
- **tile**: to tile all remaining frames together. This filter will put the preview images into one image file. The grid
  size can be set and is currently always quadratic, which means the number of lines and the number of columns are both
  the rounded up square root of the desired number of images.


Configuration
-------------

The resolution, the output format and the mimetype can be configured in
`etc/org.opencastproject.timelinepreviews.ffmpeg.TimelinePreviewsServiceImpl.cfg`.

Width of the resolution of a single preview image in pixels (defaults to 160).

    resolutionX = 160

Height of the resolution of a single preview image in pixels (defaults to -1).
If not set or set to -1, it will be set automatically to preserve the original aspect ratio.

    resolutionY = -1

Output file format for the timeline previews image file (defaults to ".png").

    outputFormat = ".png"

Mimetype for the output image (defaults to "image/png").

    mimetype = "image/png"
