Waveform Service Configuration
==============================

The Waveform service generates waveform images from a audio/video file.
These waveform images are then shown in the Admin-UI video editor.

Service Configuration
---------------------

The Waveform service configuration file `etc/org.opencastproject.waveform.ffmpeg.WaveformServiceImpl.cfg` provides the
following options:

    job.load.waveform = 0.5

With this value you can define the load of waveform jobs, see [job load](../configuration/load)

    waveform.image.width.min = 5000

This will define the minimum width of the waveform image in pixels.

    waveform.image.width.max = 20000

This will define the maximum width of the waveform image in pixels.

    waveform.image.width.ppm = 200

This value defines the width of the waveform image in relation to the length of the audio/video file
(in pixels per minute).

    waveform.image.height = 500

This will define the height of the waveform image in pixels.

    waveform.color = black

This value defines the color of the waveform. The value must be a RGB(A) hex code or one of the predefined values,
see [FFMPEG Colors](https://www.ffmpeg.org/ffmpeg-all.html#Color). You can define one color per audio channel
separated by a whitespace.

    waveform.split.channels = false

This boolean value defines whether multiple audio channels should be mixed in one waveform (if `false`)
or separated one next to each other (if `true`).

    waveform.scale = lin

This value defines the scale of the waveform. You can chose between `lin` for linear or `log` for logarithmic scale.

