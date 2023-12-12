# Opencast 14: Release Notes

## Opencast 14.7

Opencast 14.7 is a minor release. This release includes bug fixes and improvements.

See [changelog](changelog.md#opencast-147) for a comprehensive list of changes.

## Opencast 14.6

Opencast 14.6 is a minor release. This release includes bug fixes and improvements.

See [changelog](changelog.md#opencast-146) for a comprehensive list of changes.

## Opencast 14.5

Opencast 14.5 is a minor release. This release includes bug fixes and improvements.

The analyze-mediapackage WOH was extended so it will also create variables regarding the existence of elements of a
certain flavor _and tag_ (example: `captions_source_hastag_archive`). These new variables are not created by default.

See [changelog](changelog.md#opencast-145) for a comprehensive list of changes.

## Opencast 14.4

Opencast 14.4 is a minor release. This release includes bug fixes and improvements.

See [changelog](changelog.md#opencast-144) for a comprehensive list of changes.

## Opencast 14.3

The off-schedule release of Opencast 14. This release includes fixes from Opencast 13.10 release,
see [release notes of Opencast 13.10](https://docs.opencast.org/r/13.x/admin/#releasenotes/#opencast-1310)

Additionally, the following changes are part of this release.

- Fix admin interface permissions ([#5167](https://github.com/opencast/opencast/pull/5167))
- Fix Admin Interface Redirect ([#5166](https://github.com/opencast/opencast/pull/5166))

See [changelog](changelog.md#opencast-143) for a comprehensive list of changes.

## Opencast 14.2

Opencast 14.2 is a minor release. The release fixes corrupt zip headers in the distributed jar files and elasticsearch
package imports. This was causing Opencast to fail at startup in combination with a recent OpenJDK security update.

The alternative to updating to this release is to run Java with `-Djdk.util.zip.disableZip64ExtraFieldValidation=true`.
Setting this will disable the new security check.

Additionally, the following changes are part of this release.

- Fix changed pax web config keys ([#5124](https://github.com/opencast/opencast/pull/5124))
- Upgrade Crowdin Integration ([#5114](https://github.com/opencast/opencast/pull/5114))

See [changelog](changelog.md#opencast-142) for a comprehensive list of changes.

## Opencast 14.1

Opencast 14.1 is a minor release, containing documentation improvements and Paella Player 7 trimming URL parameter fix.
For more details, make sure to check out the [documentation](configuration/player/paella.player7/url.parameter.md).

See [changelog](changelog.md#opencast-141) for a comprehensive list of changes.

## Opencast 14.0

### New Admin UI

Opencast 14 brings with it a new version of the Admin UI. The new Admin UI continues with the same look and
feel, but uses new technologies under the hood. It strives to be robust and easy to develop for, while having all the
same functionality users expect from the old UI.

For now, the new Admin UI has not quite reached those goals yet. Therefore, it is not enabled by default and marked
as beta. If you wish to try it out, you can easily do so by going to `https://your-opencast/admin-ui/index.html`
(don't worry if it looks like nothing has changed, the look is *really* similar :)). If you find any issues,
please report them at [GitHub project issue board](https://github.com/opencast/opencast-admin-interface/issues).

For notes on how to set the new Admin UI as your default, check [Admin UI](configuration/admin-ui/new-admin-ui.md).

### Features
- The new Admin UI (beta) is now shipped with Opencast [[#4695](https://github.com/opencast/opencast/pull/4695)]
- Global `oc-remember-me` cookie can be configured [[#4951](https://github.com/opencast/opencast/pull/4951)]
- Status of the index rebuild will be shown as an indicator in the Admin UI [[#4206](https://github.com/opencast/opencast/pull/4206)]

### Improvements
- Fixed session IllegalStateException [[#5050](https://github.com/opencast/opencast/pull/5050)]
- Backwards support for old captions/dfxp flavored xml files in Paella 7 [[#5051](https://github.com/opencast/opencast/pull/5051)]
- Enabled dfxp captions support for Paella 7 [[#5049](https://github.com/opencast/opencast/pull/5049)]
- Added missing metadata in Paella 7 [[#5048](https://github.com/opencast/opencast/pull/5048)]
- OSGI bundle info database table will be truncated by the database migration script [[#4946](https://github.com/opencast/opencast/pull/4946)]
- Orphan statistics database index will be dropped by database migration script [[#4945](https://github.com/opencast/opencast/pull/4945)]
- Updated Karaf to version 4.4.3 [[#4930](https://github.com/opencast/opencast/pull/4930)]
- Fixed rest docs forms [[#4928](https://github.com/opencast/opencast/pull/4928)]
- Updateed deprecated ACL code [[#4924](https://github.com/opencast/opencast/pull/4924)]
- Fixed REST docs login problem [[#4921](https://github.com/opencast/opencast/pull/4921)]
- Github actions will run auto-update on main repo only [[#4881](https://github.com/opencast/opencast/pull/4881)]
- Includeed Amberscript-Transcription documentation in module overview [[#4745](https://github.com/opencast/opencast/pull/4745)]
- Fixed documentation syntax error [[#4609](https://github.com/opencast/opencast/pull/4609)]
- Documented feature pull request targetting rules [[#4595](https://github.com/opencast/opencast/pull/4595)]
- Image preview added in the asset details view in the Admin UI [[#4556](https://github.com/opencast/opencast/pull/4556)]
- Fixed parent POM version of redirect module [[#4530](https://github.com/opencast/opencast/pull/4530)]
- Removed Twitter and Facebook links from the readme [[#4520](https://github.com/opencast/opencast/pull/4520)]
- Simplified debug output in the JWT filters and made it more idiomatic [[#4511](https://github.com/opencast/opencast/pull/4511)]
- Updated board list in documentation [[#4488](https://github.com/opencast/opencast/pull/4488)]
- Updated Issue template [[#4423](https://github.com/opencast/opencast/pull/4423)]

### Behavior changes
- Improved Paella 7 default theme [[#4943](https://github.com/opencast/opencast/pull/4943)]
- Made Paella 7 the new default player in Opencast [[#4875](https://github.com/opencast/opencast/pull/4875)]
- Removed Theodul player (plugin) [[#4315](https://github.com/opencast/opencast/pull/4315)]
- Made standalone Editor default videoeditor in Opencast [[#4876](https://github.com/opencast/opencast/pull/4876)]
- Made Composite Ffmpeg command configurable to support GPU encoding [[#4878](https://github.com/opencast/opencast/pull/4878)]

Release Schedule
----------------

| Date          | Phase                      |
|---------------|----------------------------|
| May 15, 2023  | Cutting the release branch |
| May 15, 2023  | Translation week           |
| May 30, 2023  | Public QA phase            |
| June 22, 2023 | Release of Opencast 14.0   |

Release Managers
----------------

- Waldemar Smirnow (ELAN e.V.)
- Stefanos Georgopoulos (FAU Erlangen Nürnberg)
