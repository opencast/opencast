Opencast Studio
===============

Studio is a small web application that runs in the browser and allows the user to record webcam video, the user's display and microphone audio. Afterwards, the user can easily upload their recording to an Opencast instance.

Studio uses the recording capabilities built into modern browsers to record audio and video streams. The recording happens completely in the user's browser: no server is involved in that part. Network access is only needed to initially load the application and to (optionally) upload the videos to an Opencast instance.

This module includes Studio directly into Opencast and pre-configures it accordingly. It is available at `https://yourserver/studio`. Note: Studio is developed [outside of the main repository](https://github.com/elan-ev/opencast-studio): you can find additional documentation in that repository. Please also report bugs and feature requests for Studio to that repository, unless it's a bug related to the integration in Opencast.


## Giving users access to Studio

The path `/studio` is accessible by users with the role `ROLE_ADMIN` or `ROLE_STUDIO`. The APIs used by Studio (`/ingest/*` and `info/me.json`) are also accessible if the user has `ROLE_STUDIO`.

The preferred way to let your users access Studio is via LTI. Remember to configure your LTI users to have the role `ROLE_STUDIO` so that they can access Studio and all APIs used by Studio.


## Configuring Studio

Studio is pre-configured via `etc/ui-config/mh_default_org/studio/settings.json`. You can modify that file to change the configuration, but note that you probably don't want to touch `opencast.serverUrl` and `opencast.loginProvided`. For information on possible configuration values, please see [`CONFIGURATION.md` in the Studio repository](https://github.com/elan-ev/opencast-studio/blob/master/CONFIGURATION.md).

If you want to pre-configure the ACL that is sent by studio, place a file `acl.xml` in `etc/ui-config/mh_default_org/studio/` and set `upload.acl` in `settings.json` to `/ui/config/studio/acl.xml`.

## Workflow requirements

The videos produced by the built-in recording capabilities of browsers are often quite exotic, to put it mildly. The Opencast workflow responsible for processing uploads from Studio needs to be able to handle the following things:

- **Variable framerate**. If your workflow produces 1000fps videos, it's probably because it can't handle variable framerate. The standard solution would be to re-encode at a fixed and common framerate.
- **Missing duration and seeking data**. Videos produced by Chrome and Edge do not store the video duration in the container. Furthermore, they don't contain any seeking cues, so that some operations can't be done quickly.
- **Variable video dimensions**. This can happen when someone records from a phone and rotates it by 90°, making width and height of the video swap. Another common case is browsers capturing a single window/application on the user's desktop. When that window is resized, the video dimensions change, too.
- **Very long frames**. If the user's device is very slow and can't keep up with encoding, the frame rate might be *very* low. A more extreme example is the "capture tab" feature of Chrome. In that case, Chrome records all the frames produced by its rendering engine – if the website displayed in the selected tab is static and the user does not scroll, no new frames are created, potentially for minutes!

Additionally, for the video cutting in Studio to work, your workflow needs to respect the SMIL catalog Studio includes in the ingest. The default Studio workflow can handle all those things.

Finally, here are some other oddities and details of videos produced by browsers:

- Most browsers will default to VP8 as video codec, OPUS as audio codec and WEBM (or the superset MKV) as container.
- Desktop capture seem to happen with 30fps in Chrome and Firefox (tested on a 60hz monitor).
- In some cases, Firefox encodes all frames as key-frames, which – given the fixed bitrate – oftne results in a fairly low quality video.
