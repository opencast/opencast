# Opencast 14: Release Notes

### New Admin UI

Opencast 14 brings with it a new version of the Admin UI. The new Admin UI continues with the same look and
feel, but uses new technologies under the hood. It strives to be robust and easy to develop for, while having all the
same functionality users expect from the old UI.

For now, the new Admin UI has not quite reached those goals yet. Therefore, it is not enabled by default and marked
as beta. If you wish to try it out, you can easily do so by going to `https://your-opencast/admin-ui/index.html`
(don't worry if it looks like nothing has changed, the look is *really* similar :)). If you find any issues,
please report them at https://github.com/opencast/opencast-admin-interface/issues.

For notes on how to set the new Admin UI as your default, check [Admin UI](modules/admin-ui.md).

## Opencast 14.0

### Features
- Add new admin UI as beta [[#4695](https://github.com/opencast/opencast/pull/4695)]
- Global 'oc-remember-me' cookie [[#4951](https://github.com/opencast/opencast/pull/4951)]

### Improvements
- Truncate Bundle Info [[#4946](https://github.com/opencast/opencast/pull/4946)]
- Drop orphan statistics database index [[#4945](https://github.com/opencast/opencast/pull/4945)]
- Karaf upgrade 4.4.3 [[#4930](https://github.com/opencast/opencast/pull/4930)]
- Fix rest docs forms [[#4928](https://github.com/opencast/opencast/pull/4928)]
- Update deprecated ACL code [[#4924](https://github.com/opencast/opencast/pull/4924)]
- Fix REST docs login problem [[#4921](https://github.com/opencast/opencast/pull/4921)]
- Run auto-update on main repo only [[#4881](https://github.com/opencast/opencast/pull/4881)]
- Include Amberscript-Transcription Documentation in Module Overview [[#4745](https://github.com/opencast/opencast/pull/4745)]
- Fix documentation syntax error [[#4609](https://github.com/opencast/opencast/pull/4609)]
- Document feature pull request targetting rules [[#4595](https://github.com/opencast/opencast/pull/4595)]
- image preview added [[#4556](https://github.com/opencast/opencast/pull/4556)]
- Fix parent POM version of redirect module in develop [[#4530](https://github.com/opencast/opencast/pull/4530)]
- Remove Twitter and Facebook links [[#4520](https://github.com/opencast/opencast/pull/4520)]
- Make debug output in the JWT filters simpler and more idiomatic [[#4511](https://github.com/opencast/opencast/pull/4511)]
- Update board list in documentation [[#4488](https://github.com/opencast/opencast/pull/4488)]
- Update Issue Template [[#4423](https://github.com/opencast/opencast/pull/4423)]
- Changed rebuild order and added rebuild indicator [[#4206](https://github.com/opencast/opencast/pull/4206)]

### Behavior changes
- Update Paella 7 default theme [[#4943](https://github.com/opencast/opencast/pull/4943)]
- Paella 7 is the new default [[#4875](https://github.com/opencast/opencast/pull/4875)]
- Remove Theodul [[#4315](https://github.com/opencast/opencast/pull/4315)]
- New Default Editor [[#4876](https://github.com/opencast/opencast/pull/4876)]
- Make Composite Ffmpeg Command Configurable [[#4878](https://github.com/opencast/opencast/pull/4878)]


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
