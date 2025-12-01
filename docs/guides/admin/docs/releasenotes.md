# Opencast 18: Release Notes

## Opencast 18.4

Among other minor fixes, this minor release includes a bug fix related to the direct publication of live events. [[#7118](https://github.com/opencast/opencast/pull/7118)]

This version also contains new releases of both the admin ui
([18.x-2025-11-28](https://github.com/opencast/opencast-admin-interface/releases/tag/18.x-2025-11-28)) and the editor
([18.x-2025-11-28](https://github.com/opencast/opencast-editor/releases/tag/18.x-2025-11-28)).

## Opencast 18.3

This minor release includes a fix for the silence detection with newer FFmpeg versions. [[#7069](https://github.com/opencast/opencast/pull/7069)]

This version also contains new releases of both the admin ui
([18.x-2025-10-24](https://github.com/opencast/opencast-admin-interface/releases/tag/18.x-2025-10-24)) and the editor
([18.x-2025-10-24](https://github.com/opencast/opencast-editor/releases/tag/18.x-2025-10-24)).

## Opencast 18.2

This version contains two **security fixes**!
One of them is a cross-site-scripting vulnerability in the
Paella Player (see [GHSA-m2vg-rmq6-p62r](https://github.com/opencast/opencast/security/advisories/GHSA-m2vg-rmq6-p62r)).
The other allowed accidental publishing in the Opencast Editor (see
[GHSA-x6vw-p693-jjhv](https://github.com/opencast/opencast/security/advisories/GHSA-x6vw-p693-jjhv))
It is advised to update to this version in a timely fashion

This version also contains new releases of both the admin ui
([18.x-2025-09-29](https://github.com/opencast/opencast-admin-interface/releases/tag/18.x-2025-09-29)) and the editor
([18.x-2025-09-29](https://github.com/opencast/opencast-editor/releases/tag/18.x-2025-09-29)).

## Opencast 18.1

Among other bug fixes, this version contains a **security fix**. Please see
[#6979](https://github.com/opencast/opencast/pull/6979) and
[GHSA-hq8m-v68g-8cf8](https://github.com/opencast/opencast/security/advisories/GHSA-hq8m-v68g-8cf8)
(once published) for details.

As a minor feature, it is now possible to pass additional arguments to whisper.cpp
([#6966](https://github.com/opencast/opencast/pull/6966)).

This version also contains new releases of both the admin ui
([18.x-2025-08-29](https://github.com/opencast/opencast-admin-interface/releases/tag/18.x-2025-08-29)) and the editor
([18.x-2025-08-29](https://github.com/opencast/opencast-editor/releases/tag/18.x-2025-08-29)).

## Opencast 18.0

### Features
- JWT support has been improved by standardizing the JWT schema. This is the groundwork for further support
  of JWT to ease the connection of external systems to Opencast. [[#6177](https://github.com/opencast/opencast/pull/6177)]
- Added the ability to configure S3 buckets for each organization. [[#6253](https://github.com/opencast/opencast/pull/6253)]
- Opencast now supports plugins with different modules on different distributions. [[#6303](https://github.com/opencast/opencast/pull/6303)]
- Various improvements to full-text search [[#6498](https://github.com/opencast/opencast/pull/6498)]
- Added support for audio-only uploads [[#5856](https://github.com/opencast/opencast/pull/5856)]
- Added the mux WOH which is able to encode multiple tracks into one.
  [[#6894](https://github.com/opencast/opencast/pull/6894)]

### Breaking Changes
- The animate service and workflow operation have been removed. Make sure to update your custom workflows, if you used
  this operation. [[#6518](https://github.com/opencast/opencast/pull/6518)]
- All SoX related modules have been removed. Any workflows depending on this should switch to ffmpeg based normalization.
  [[#6682](https://github.com/opencast/opencast/pull/6682)]
- The crop service is now a plugin and disabled by default. If you have used it in previous versions, you need to enable
  it in `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`. [[#6303](https://github.com/opencast/opencast/pull/6303)]
- The required Java version is now 21. [[#6472](https://github.com/opencast/opencast/pull/6472)]
- We switched to using the new default binary names used by Whisper.cpp. The default model directory is now
  /usr/share/whisper.cpp/models [[#6794](https://github.com/opencast/opencast/pull/6794)]

### Configuration Changes
- Series, Editor and Login-Buttons in the PaellaPlayer are now disabled by default.
  [[#6479](https://github.com/opencast/opencast/pull/6479)] [[#6480](https://github.com/opencast/opencast/pull/6480)]
  [[#6481](https://github.com/opencast/opencast/pull/6481)]
- The feature "Episode ID Roles" is now a core feature: enabled by default with no option to disable.
  [[#6696](https://github.com/opencast/opencast/pull/6696)]
  If you have not used this feature before, you'll need to perform an index rebuild. See [upgrade guide](upgrade.md) for
  details.
- The default workflows partial-publish and partial-preview have been modified to enable audio-only processing.
  If you want to use this feature and use your own workflows, have a look at the required changes in the PR.
  [[#5856](https://github.com/opencast/opencast/pull/5856)]

For more details, please take a look at the [full changelog](changelog/opencast-18.md). If you want to update Opencast
from a previous version, you should also read the [upgrade guide](upgrade.md).

## Release Schedule

| Date             | Phase                    |
|------------------|--------------------------|
| June 19, 2025    | Release Branch Cut       |
| July 24, 2025    | Release of Opencast 18.0 |


## Release Managers

- Dennis Benz (Osnabrück University)
- Jonas Dühring (elan e.V.)
