# Theodul Pass Player - Events

A Theodul Pass Player plugin can trigger and/or subscribe to events.

An event is defined in the events section of the plugin and looks like this:

    NAME: new Engage.Event("MODULE:NAME", "DESCRIPTION", "OPTION")

The event has the event name "MODULE:NAME", the description DESCRIPTION and one of the options "trigger", "handler" or "both" as OPTION. When the plugin just triggers the event, the option is "trigger", when it just handles the events the option is "handler" and when it does both - trigger and handle it - the option is "both".

An event can be triggered via

    Engage.trigger(plugin.events.NAME.getName(), [parameter(s)]);

and can be subscribed to via

    Engage.on(plugin.events.NAME.getName(), function () {});
 
The following list contains all events of the Core + of all official plugins, sorted alphabetically after "Event name" for version 1.0 of Feb 12, 2015.

Currently official plugins are

 - Controls
 - MHConnection
 - Notifications
 - Usertracking
 - Description
 - Description (Tab)
 - Slide text (Tab)
 - Shortcuts (Tab)
 - Timeline statistics
 - Videodisplay
 
|Name | Event name | Additional parameters|Description|Triggered in|Handled in|
|-----|------------|----------------------|-----------|------------|----------|
|coreInit	|Core:init	 	 	| | |Core| |
|plugin_load_done	|Core:plugin_load_done	 	 	| | |Core|Core, Controls, MHConnection, Notifications, Usertracking, Description, Description (Tab), Slide text (Tab), Shortcuts (Tab), Timeline statistics, Videodisplay|
|timelineplugin_closed	|Engage:timelineplugin_closed	|*Note: No "Engage Event", just use as string, example: Engage.on("Engage:timelineplugin_closed", function() {});*| when the timeline plugin container closed	|Core| | 
|timelineplugin_opened	|Engage:timelineplugin_opened	|*Note: No "Engage Event", just use as string, example: Engage.on("Engage:timelineplugin_opened", function() {});* |when the timeline plugin container opened	|Core|Timeline statistics|
|getMediaInfo	|MhConnection:getMediaInfo|	| |	 	 	|MHConnection|
|getMediaPackage	|MhConnection:getMediaPackage|	| |	 	 	|MHConnection|
|mediaPackageModelError	|MhConnection:mediaPackageModelError	 |	 |	|MHConnection| Core, Controls, Notifications, Usertracking, Description, Description (Tab), Slide text (Tab), Shortcuts (Tab), Timeline statistics, Videodisplay|
|customError	|Notification:customError	|msg: The message to display	|an error occurred	| Core, Controls, Videodisplay| Notifications|
|customNotification	|Notification:customNotification	|msg: The message to display	|a custom message	| Videodisplay| Notifications|
|customOKMessage	|Notification:customOKMessage	| msg: The message to display	| a custom message with an OK button	|Controls | Notifications |
|customSuccess	|Notification:customSuccess	|msg: The message to display	|a custom success message	|Core, Controls| Notifications|
|segmentMouseout	|Segment:mouseOut	|no: Segment number	|the mouse is off a segment	|Controls, Slide text (Tab)| Controls, Slide text (Tab)|
|segmentMouseover	|Segment:mouseOver	|no: Segment number	|the mouse is over a segment	|Controls, Slide text (Tab)| Controls, Slide text (Tab)|
|sliderMousein	|Slider:mouseIn	 |	|the mouse entered the slider	|Controls| | 
|sliderMouseout	|Slider:mouseOut	| 	|the mouse is off the slider	|Controls | |
|sliderMousemove	|Slider:mouseMoved	|timeInMs: The time on the hovered position in ms	|the mouse is moving over the slider	| Controls | |
|sliderStart	|Slider:start	 |	|slider started	|Controls| | 
|sliderStop	|Slider:stop	|time: The time the slider stopped at	| slider stopped	|Controls|Videodisplay|
|aspectRatioSet	|Video:aspectRatioSet	|as: (array) as[0] = width, as[1] = height, as[2] = aspect ratio in %|the aspect ratio has been calculated	|Videodisplay|Controls|
|audioCodecNotSupported	|Video:audioCodecNotSupported	 |	|when the audio codec seems not to be supported by the browser	| Videodisplay |Notifications|
|autoplay	|Video:autoplay	 |	|autoplay the video	|Core| Videodisplay|
|bufferedAndAutoplaying	|Video:bufferedAndAutoplaying	 |	|buffering successful, was playing, autoplaying now	|Videodisplay |Notifications|
|bufferedButNotAutoplaying	|Video:bufferedButNotAutoplaying	 |	|buffering successful, was not playing, not autoplaying now	|Videodisplay |Notifications|
|buffering	|Video:buffering	 |	|video is buffering	|Videodisplay|Notifications|
|ended	|Video:ended	|triggeredByMaster: Whether or not the event has been triggered by master	|video ended	|Videodisplay|Controls|
|fullscreenCancel	|Video:fullscreenCancel	 |	|cancel fullscreen	|Controls, Videodisplay|Videodisplay|
|fullscreenChange	|Video:fullscreenChange	 |	|a fullscreen change happened	|Videodisplay|Controls|
|fullscreenEnable	|Video:fullscreenEnable	 |	|enable fullscreen	|Controls, Core| Controls, Videodisplay|
|isAudioOnly	|Video:isAudioOnly	|audio: true if audio only, false else	|whether it's audio only or not	| Videodisplay|Controls, Notifications|
|initialSeek	|Video:initialSeek	|time: The time to seek to	|Seeks initially after all plugins have been loaded after a short delay	| Core| Videodisplay|
|mute	|Video:mute	 |	|mute	|Videodisplay|Videodisplay|
|muteToggle	|Video:muteToggle	 |	|toggle mute and unmute	| Core|Videodisplay|
|nextChapter	|Video:nextChapter	| | Core | |
|numberOfVideodisplaysSet	|Video:numberOfVideodisplaysSet	|no: Number of videodisplays	|the number of videodisplays has been set	|Videodisplay| |
|pause	|Video:pause	|triggeredByMaster: Whether or not the event has been triggered by master	|pauses the video	
|Core, Controls, Videodisplay| Controls, Videodisplay|
|play	|Video:play	|triggeredByMaster: Whether or not the event has been triggered by master	|plays the video	|Core, Controls, Videodisplay| Controls,Videodisplay |
|playPause	|Video:playPause	 |	 |	|Core|Videodisplay|
|previousChapter	|Video:previousChapter	 	 | | |Core| | 
|playbackRateChanged	|Video:playbackRateChanged	|rate: The video playback rate (0.0-x, default: 1.0)	|The video playback rate changed	|Controls| Controls, Videodisplay|
|playbackRateIncrease	|Video:playbackRateIncrease	 | | |Core|Videodisplay|
|playbackRateDecrease	|Video:playbackRateDecrease	 | | |Core|Videodisplay|
|playerLoaded	|Video:playerLoaded	 | | player loaded successfully	|Videodisplay| |
|ready	|Video:ready	 |	|all videos loaded successfully	|Videodisplay| Controls, Notifications|
|seek	|Video:seek	|time: Current time in seconds	seek video to a given position in seconds	|Core, Controls, Slide text (Tab)|Videodisplay |
|seekLeft	|Video:seekLeft	 |	| 	|Core|Videodisplay|
|seekRight	|Video:seekRight |	| 	|Core|Videodisplay|
|synchronizing	|Video:synchronizing	| 	|synchronizing videos with the master video	|Videodisplay| |
|timeupdate	|Video:timeupdate	|time: Current time in seconds, triggeredByMaster: Whether or not the event has been triggered by master|a timeupdate happened	|Videodisplay| Controls, Usertracking|
|qualitySet	|Video:qualitySet	|quality: the quality that has been set	a video quality has been set	|Controls|Videodisplay|
|unmute	|Video:unmute	 |	|unmute	|Controls|Controls|
|usingFlash	|Video:usingFlash	|flash: true if flash is being used, false else	|flash is being used	|Videodisplay|Controls|
|videoFormatsFound	|Video:videoFormatsFound	|format: array of video formats	if different video formats (qualities) have been found	|Videodisplay|Controls|
|volumechange	|Video:volumechange	|vol: Current volume (0 is off (muted), 1.0 is all the way up, 0.5 is half way)	| a volume change happened	|Videodisplay| |
|volumeDown	|Video:volumeDown	 	| |	|Core|Controls|
|volumeGet	|Video:volumeGet	|callback: A callback function with the current volume as a parameter	|get the volume	 	| Videodisplay|
|volumeSet	|Video:volumeSet	|percentAsDecimal: Volume to set (0 is off (muted), 1.0 is all the way up, 0.5 is half way)	|set the volume	|Controls|Controls, Videodisplay|
|volumeUp	|Video:volumeUp	 	| |	|Core|Controls|
