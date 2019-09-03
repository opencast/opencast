Paella Player - URL Parameters
==============================

Parameter      | Example   | Description
---------------|-----------|------------
**id**         | `SOME-ID` | Video Id to play
**time**       | `10m20s`  | Seeks intially automatically to a specified time
**autoplay**   | `true`    | Automatically starts playing the video


id
----
Video Id to play

time
----
Seeks intially automatically to a specified time.
automatically plays the video from the specified time on

Possible values
* Hours (with value `X`), minutes (with value `Y`) and seconds (with value `Z`)
    * `XhYmZs`
* Minutes (with value `Y`) and seconds (with value `Z`)
    * `YmZs`
* Minutes (with value `Y`) only
    * `Ym`
* Seconds (with value `Z`) only
    * `Zs`

Default value: `-`    
        

autoplay
--------
Automatically starts playing the video after a short delay

Possible values
* `true`
* `false`

Default value: `false`


Example
-------
http://YOUR.SERVER/paella/ui/watch.html?id=SOME-ID&time=3m30s
