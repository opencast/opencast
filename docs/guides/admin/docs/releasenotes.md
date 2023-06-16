# Opencast 14: Release Notes

Opencast 14.0
-------------

### Features
- Paella 7 is the new default [[#4875](https://github.com/opencast/opencast/pull/4875)]
- Add new admin UI as beta [[#4695](https://github.com/opencast/opencast/pull/4695)]
- Global 'oc-remember-me' cookie
- New Default Editor [[#4876](https://github.com/opencast/opencast/pull/4876)]

### Improvements
- Truncate Bundle Info [[#4946](https://github.com/opencast/opencast/pull/4946)]
- Drop orphan statistics database index [[#4945](https://github.com/opencast/opencast/pull/4945)]
- Update Paella 7 default theme [[#4943](https://github.com/opencast/opencast/pull/4943)]
- Karaf upgrade 4.4.3 [[#4930](https://github.com/opencast/opencast/pull/4930)]
- Fix rest docs forms [[#4928](https://github.com/opencast/opencast/pull/4928)]
- Update deprecated ACL code [[#4924](https://github.com/opencast/opencast/pull/4924)]
- Fix REST docs login problem [[#4921](https://github.com/opencast/opencast/pull/4921)]
- Run auto-update on main repo only [[#4881](https://github.com/opencast/opencast/pull/4881)]
- Include Amberscript-Transcription Documentation in Module Overview [[#4745](https://github.com/opencast/opencast/pull/4745)]
- Fix documentation syntax error [[#4609](https://github.com/opencast/opencast/pull/4609)]
- Fix parent POM version of redirect module in develop [[#4530](https://github.com/opencast/opencast/pull/4530)]
- Update board list in documentation [[#4488](https://github.com/opencast/opencast/pull/4488)]
- New workflow implementation and migration fixes (OC 13) [[#4456](https://github.com/opencast/opencast/pull/4456)]
- Add index for oc_job_argument table (OC 13) [[#4450](https://github.com/opencast/opencast/pull/4450)]
- Enrich LDAP users with name and mail address [[#4440](https://github.com/opencast/opencast/pull/4440)]
- Update Issue Template [[#4423](https://github.com/opencast/opencast/pull/4423)]
- Add organization properties to mail template data [[#4380](https://github.com/opencast/opencast/pull/4380)]
- Allow use of extended metadata in send-email WOH [[#4376](https://github.com/opencast/opencast/pull/4376)]
- Add basic auth support ingest download [[#4180](https://github.com/opencast/opencast/pull/4180)]
- Common persistence util classes that also implement transaction retries [[#3903](https://github.com/opencast/opencast/pull/3903)]
 
### Behavior changes
- Remove Theodul [[#4315](https://github.com/opencast/opencast/pull/4315)]
- Make Composite Ffmpeg Command Configurable [[#4878](https://github.com/opencast/opencast/pull/4878)]
- Document feature pull request targetting rules [[#4595](https://github.com/opencast/opencast/pull/4595)]
- image preview added [[#4556](https://github.com/opencast/opencast/pull/4556)]
- Remove Twitter and Facebook links [[#4520](https://github.com/opencast/opencast/pull/4520)]
- Make debug output in the JWT filters simpler and more idiomatic [[#4511](https://github.com/opencast/opencast/pull/4511)]
- Publish Captions by Default [[#4415](https://github.com/opencast/opencast/pull/4415)]
- Add support for multipart mails using text and HTML [[#4408](https://github.com/opencast/opencast/pull/4408)]
- Simplify ldap user directory implementation [[#4383](https://github.com/opencast/opencast/pull/4383)]
- Changed rebuild order and added rebuild indicator [[#4206](https://github.com/opencast/opencast/pull/4206)]


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
