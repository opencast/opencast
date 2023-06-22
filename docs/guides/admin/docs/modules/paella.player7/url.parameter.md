Paella Player 7 - URL Parameters
==============================

Parameter      | Example    | Description
---------------|------------|------------
**id**         | `SOME-ID`  | Video Id to play
**time**       | `10m20s`   | Seeks initially automatically to a specified time
**trimming**   | `1m2s;10m` | Apply a soft trimming to the video


id
----
Video Id to play

time
----
Seeks initially automatically to a specified time.
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
        

trimming
--------
Apply a soft trimming to the video.
The result is that the user will only be able to view the specified video chunk. Useful to put links to parts of the video.

Possible values
* `<start time>;<end time>`

The separator between `<start time>` and `<end time>` must be a semicolon `;`

Time in `XhYmZs` format:
* Hours (with value `X`), minutes (with value `Y`) and seconds (with value `Z`)
    * `XhYmZs`
* Minutes (with value `Y`) and seconds (with value `Z`)
    * `YmZs`
* Minutes (with value `Y`) only
    * `Ym`
* Seconds (with value `Z`) only
    * `Zs`

Default value: `-`


Example
-------
http://YOUR.SERVER/paella/ui/watch.html?id=SOME-ID&time=3m30s
