Opencast Studio
===============

Studio is a small web application that runs in the browser and allows the user to record webcam video, the user's display and microphone audio. Afterwards, the user can easily upload their recording to an Opencast instance.

Studio uses the recording capabilities built into modern browsers to record audio and video streams. The recording happens completely in the user's browser: no server is involved in that part. Network access is only needed to initially load the application and to (optionally) upload the videos to an Opencast instance.

This module includes Studio directly into Opencast and pre-configures it accordingly. It is available at `https://yourserver/studio`. Note: Studio is developed [outside of the main repository](https://github.com/elan-ev/opencast-studio): you can find additional documentation in that repository. Please also report bugs and feature requests for Studio to that repository, unless it's a bug related to the integration in Opencast.


## Giving users access to Studio

The path `/studio` is accessible by users with the role `ROLE_ADMIN` or `ROLE_STUDIO`. The APIs used by Studio (`/ingest/*` and `info/me.json`) are also accessible if the user has `ROLE_STUDIO`.

The preferred way to let your users access Studio is via LTI. Remember to configure your LTI users to have the role `ROLE_STUDIO` so that they can access Studio and all APIs used by Studio.

This can be done, for example, by configuring the `LtiLaunchAuthenticationHandler` in `etc/org.opencastproject.security.lti.LtiLaunchAuthenticationHandler.cfg`. At the bottom of this file you see the two options `lti.custom_role_name` and `lti.custom_roles`. The former should be set to an LTI role that is supposed to have access to Studio, and the latter is a comma-separated list of Opencast roles that users with this LTI role should get. This list has to at least include `ROLE_STUDIO`.

A minimal configuration giving LTI-`Instructor`-s access to Opencast Studio could look like this:

```
lti.custom_role_name=Instructor
lti.custom_roles=ROLE_STUDIO
```

You will also have to enable persisting LTI users in the same file to avoid access management related failures during ingests initiated by them:

```
lti.create_jpa_user_reference=true
```

Note that with this configuration, every LTI user is persisted in the database with their ID and all the roles assigned to them! They are stored in the user references table (`oc_user_ref`) as opposed to the "normal" users table (`oc_user`), though.


## Configuring Studio

Studio is pre-configured via `etc/ui-config/mh_default_org/studio/settings.json`. You can modify that file to change the configuration, but note that you probably don't want to touch `opencast.serverUrl` and `opencast.loginProvided`. For information on possible configuration values, please see [the Studio repository](https://github.com/elan-ev/opencast-studio).

If you want to pre-configure the ACL that is sent by studio, place a file `acl.xml` in `etc/ui-config/mh_default_org/studio/` and set `upload.acl` in `settings.json` to `/ui/config/studio/acl.xml`.
