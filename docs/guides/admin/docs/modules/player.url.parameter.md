# Theodul Pass Player - URL Parameters

## URL Parameters

 - **time**
  - Possible values
    - Minutes (with value X) and seconds (with value Y)
      - XmYs
      - YsXm
      - XmY
    - Minutes (with value X) only
      - Xm
    - Seconds (with value Y) only
      - Ys
      - Y
  - Default value
    - -
  - Description
     - Seeks intially automatically to a specified time
     - automatically plays the video from the specified time on
 - **autoplay**
  - Possible values
    - true
    - false
  - Default value
    - false
  - Description
    - Automatically starts playing the video after a short delay
 - **quality**
  - Possible values
    - low
    - medium
    - high
  - Default value
    - medium
  - Description
    - Sets a video quality if the video has been encoded in multiple qualities
 - **mode**
  - Possible values
    - desktop
    - embed
    - mobile
  - Default value
    - desktop
  - Description
    - Sets the player mode manually
 - **browser**
  - Possible values
    - all
    - default
  - Default value
    - default
  - Description
    - if your browser is not supported, try the new player with this flag activated 
    - overwrites filtering for supported browsers with parameter set to "all"
    - resets the setting with "default"
    - the value is permanently stored for the browser in the local storage

### Example
http://YOUR.SERVER:8080/engage/theodul/ui/core.html?id=SOME-ID&time=3m30s

## Developer URL Parameters

 - **debug**
  - Possible values
    - true
    - false
  - Default value
    - false
  - Description
    - prints debug output to the developer console
 - **debugEvents**
  - Possible values
    - true
    - false
  - Default value
    - false
  - Description
    - prints debug output to the developer console when an event occurs
 - **format**
  - Possible Values
    - *hls*: Apple HTTP Live Streaming
    - *dash*: MPEG DASH
    - *rtmp*: Adobe RTMP (Flash)
    - *mp4*: MP4 videos (no streaming)
    - *webm*: WebM videos (no streaming)
    - *audio*: audio only (no streaming)
    - *default*: reset to defaults
  - Default value
    - default
  - Description 
    - sets the preferred (streaming) format
    - if not available, the defaults will be selected
    - the value is permanently stored for the browser in the local storage

### Example

http://YOUR.SERVER:8080/engage/theodul/ui/core.html?id=SOME-ID&debug=true&debugEvents=true
