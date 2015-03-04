[img_videoeditor_preview]: media/img/videoeditor_preview.png

[img_videoeditor_player]: media/img/videoeditor_player.png

[img_videoeditor_toolbar]: media/img/videoeditor_toolbar.png

[img_videoeditor_timeline]: media/img/videoeditor_timeline.png

[img_videoeditor_actions]: media/img/videoeditor_actions.png


[icon_video_preview]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAANCAYAAACkTj4ZAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAWJJREFUeNpsk80rhFEUh19jGlIzOx9FlI0VpSwmaUp2JDULH9mxstIs5y+YndjIglko+VgYmpSNlBQLq5GtIhYshw2lPKd+b525nHp677nn4973nHObNrZ3o0CaYQbmIAsd2n+HWziEU/jxQckgyQjswFD0V3rFLNRgGe5iY8I5TsK1S/IEBRgVBe1F8jHfqfBGY1CBlPRL/d6HO+gGynAC49ACxzBhSe1GGdh3SSx43iVphU3oh3pgSyk2Y4mK0ONOrqqwsQzAClxBm2xVZ7fYoiVaCIr6rK/dYA3OpXfDoKufl8WErhZ2J263dahL+is8aN0XxOxZohK8uM1pzc4n5GBL5LTXLp9YLLaUVAHt9y5UvDQcqGuPqk8sGQ1kWvq3YuvxHNlM5OFLurX3HlY13Vmta7JF8s0rtmGyzzRPZRXV6rAe/S92yJKf7PCJmGE4eGudsr3prR1pKBve2q8AAwBaikzXLZhr0gAAAABJRU5ErkJggg== "Video editor icon"


# Overview
The video editor section contains the tools that allow a producer to visualize and edit videos.
To access the video editor, go to the [Events list](events.md) and press the video editor icon ( ![icon_video_preview][] ) in the Actions column.

[![Playback mode][img_videoeditor_preview]][img_videoeditor_preview]

> Note that if no preview file is available the icon will not be displayed. Make sure that the workflow you are using are generating a preview.


## Video editor
Click on "Editor" below the player controls to activate the editing tools. The editing tool allows for cutting part of a video clip. The main components of the tool are described below.

### The player
[![Video player][img_videoeditor_player]][img_videoeditor_player]


#### Player controls:

* Play / Pause: Start and stop the video playback.
* FF / REW: Will place the marker on the next or previous video frame
* Next / Previous: Will jump to the next or previous segment


#### The toolbar
The toolbar features all the actions that are available in the tool.

[![Video editor toolbar][img_videoeditor_toolbar]][img_videoeditor_toolbar]

* Split: Split the underlying segment at the marker's position on the timeline
* Cut: Marks a segment that should be cut out of the video during processing. The background of the "Cut" segment is red
* Replay segment: Replays the current segment
* Clear segments: Removes all the segments except for one that covers the full length of the video


#### The timeline

The timeline displays the video trak as well as the [waveform](http://en.wikipedia.org/wiki/Waveform) of the audio track. The position marker (red vertical line) can be moved by dragging it with the mouse or by using the Next / Previous buttons in the [player controls](#player-controls).

The zoom control allows for a more precise positioning of the marker.


[![Video editor timeline][img_videoeditor_timeline]][img_videoeditor_timeline]

#### The actions bar
The actions bar allows for saving and starting a processing cycle on a video.

* When no workflow is selected, pressing the Save button will save the segments that have been created and the user will be redirected to the events page.
* When a workflow has been selected, the Save & Process will save the cutting information and immediately start the processing task on the video.

[![Video editor actions][img_videoeditor_actions]][img_videoeditor_actions]


> Note: by pressing close, the user will be redirected to the events page and the segments that have been created will **not** be saved.
