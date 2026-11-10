Paella Player 7 - URL Parameters
==============================

The player support different URL parameters to modify the behaviour or the target event.

Parameter      | Example    | Description
---------------|------------|------------
**id**         | `SOME-ID`  | Video Id to play
**time**       | `10m20s`   | Seeks initially automatically to a specified time
**trimming**   | `1m2s;10m` | Apply a soft trimming to the video
**logLevel**   | `DEBUG`    | Configure the logging system to show only the log messages at or above a certain level
**jwt**        | `eyJ...`   | JWT granting access to the event, for embedding the player
**jwtRefresh** | `true`     | Ask the embedding page for new JWTs when needed


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


logLevel
--------
The log level parameter allows developers to configure the logging system to show only the log messages at or above a certain level.

Possible values
* `ERROR`
* `WARN`
* `INFO`
* `DEBUG`
* `VERBOSE`

Default value: defined in `config.json` file


jwt
---
A JWT that grants access to the event. This is used by external applications (e.g. an LMS) that embed the player.
See [Embedding with JWT](jwt.md).


jwtRefresh
----------
If `true`, the player asks the embedding page for new JWTs when needed, e.g. because the one passed via `jwt`
expired. This only works if the player is embedded in an iframe. See [Embedding with JWT](jwt.md#refreshing-jwts).

Default value: `false`


Example
-------
http://YOUR.SERVER/paella/ui/watch.html?id=SOME-ID&time=3m30s


Security Parameters
--------------------

If Stream Security is enabled, there will be additional parameters. For further information, kindly refer to the [stream security section](../../stream-security/stream-security-overview.md).
