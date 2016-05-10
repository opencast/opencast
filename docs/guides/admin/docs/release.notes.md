Opencast 2.2: Release Notes
===========================

Release Schedule
----------------

|Date                              |Phase
|----------------------------------|---------------------------------------------
|April <del>*4th*</del> 6th        |Feature Freeze  *(Cutting of release branch)*
|April <del>*4th*</del> 6th - 24th |Internal QA and bug fixing phase
|&nbsp; *April 11th - 17th*        |Review Test Cases *(Dedicated team)*
|&nbsp; *April 18th - 24th*        |Documentation Review
|April 25th - May 15th             |Public QA phase *(Ask for help)*
|May 15th - June 1st               |Additional bug fixing phase
|&nbsp; *May 25th - June 1st*      |Translation week *(Ask for help)*
|June 2nd - June 12th              |Final QA phase *(Check release readiness)*
|June 15th                         |Release of Opencast 2.2

How to Upgrade
--------------

1. Check out/download Opencast 2.2
2. Stop your current Opencast instance
3. Back up Opencast files and database (optional)
4. Run the appropriate database upgrade script(s)
     - `/docs/upgrade/2.1_to_2.2/`
5. Review the configuration changes and adjust your configuration accordingly
6. Update the third party tools as documented in the [installation docs](installation/index.md).
7. Build Opencast 2.2
8. Delete the "adminui" directory in your Opencast data directory.
9. Start Opencast
10. Log-in to the Karaf console on your node where the search service is running (usually presentation node) and install the opencast-migration feature by entering: `feature:install opencast-migration`
11. Check the logs for errors!
12. Restart Opencast.
13. Reconstruct the Admin UI search index by opening `http://localhost:8080/admin-ng/index/recreateIndex` in your browser or use the REST-docs, open "Admin UI - Index Endpoint" and use the testing form on "/recreateIndex". The resulting page is empty but should return an HTTP status 200 (OK).

For more detailed information on the upgrade check the [upgrade documentation](upgrade/index.md).

Opencast 2.1: Release Notes
===========================

*A feature rich, flexible Opencast*

In the spirit of moving forward from 2.0, Opencast 2.1 provides, amongst other features, a more stable and flexible
backend infrastructure courtesy of Karaf - “the next generation OSGI framework”.

The new version provides a lot of User Interface (UI) improvements and fixes some Admin UI issues. It also provides
additional internationalization support, as well as a Dashboard that provides a quick overview of processing states.

Opencast 2.1 also introduces a way to access the REST-endpoint documentation from within the Admin UI, which paves the
way to allowing us to remove the legacy (1.x-style) Admin UI in upcoming versions. Although the legacy UI is still
usable for some tasks, some areas feel broken and should be removed as the underlying logic has changed (in the
transition from Opencast 1.x to 2.x).

New Features and Improvements
-----------------------------

 - **Switch from Apache Felix to Apache Karaf** - 2.1 sees the move from an OSGI runtime (Felix) to a flexible OSGI
   Environment (Karaf). This is the most prominent feature in 2.1. It ensures that going forward Opencast will have a
   solid, flexible backend infrastructure.

 - **Addition of a new "Assets" tab** - The Event details has a new tab, “Assets”, that gives additional information
   about all the media, meta-data catalogues and publications.

 - **New service health endpoint** - Monitoring tools like Nagios or New Relic can mostly be configured to check the
   health status of the software. This service health endpoint provides the information to indicate to Monitoring tools
   that Opencast works as it is supposed to.

 - **Rewritten workspace** - This is the first step to addressing NFS latency issues. In the past the following scenario
   was observed: “A file has been written to the workspace. The write call returns, then another service tries to access
   the previously written file but gets a "not found" error. Then, some time later the file appears.” This rewrite
   ensures that all workspaces on all nodes are able to see a file after it is written.


 - **A dashboard for the new Admin UI** - The dashboard shows the number of jobs for different filter sets. This only
   works with the events module for now.

 - **i18n : Introduction of Chinese Translation to Opencast** - The introduction of Chinese Traditional as a new
   Translation brings Opencast to the position of being fully translated into 5 languages (English, French, German,
   Spanish and Chinese). The Translation has also been moved to Crowdin which allows a greater Community to help with
   the translation efforts, also enabling people who do not write source-code to contribute to the internationalization
   of Opencast.

Important Administrative Notes
------------------------------

 - **Apache Karaf** : The move from Apache Felix to Apache Karaf resulted in some changes in the way Opencast is built
   and run. The build infrastructure has changed, and the result is a simpler build. When you build opencast you now
   just run maven with much fewer parameters i.e.

        `mvn clean install`

    This creates all the files necessary to run Opencast either in an all-in-one or distributed setup. The outputs of
    the build are now stored in the  `build` folder.

    There you will find `.tar.gz` packages for:

     | filename                                  | installation            |
     | ----------------------------------------- | ----------------------- |
     | `opencast-dist-admin-2.1.1.tar.gz`        | admin node              |
     | `opencast-dist-presentation-2.1.1.tar.gz` | presentation node       |
     | `opencast-dist-worker-2.1.1.tar.gz`       | worker node             |
     | `opencast-dist-allinone-2.1.1.tar.gz`     | all-in-one installation |

    For your convenience the all-in-one installation is automatically extracted to the `build` folder.

 - **New Configuration File Structure** :
    - Main config is now `custom.properties` this contains all the configuration keys that have previously been in
      `config.properties`. There is still a `config.properties` file which is automatically generated during the build
      process and should not be changed.
    - Remember to adjust the bind address for public installations.

- **New start scripts** :
    - `start-opencast` now runs an interactive shell by default.
    - Use `log:tail` to tail the logs.


How to Upgrade
--------------

Note that backing up your Opencast instance before doing a major update is strongly recommended.

1. Check out/download Opencast 2.1
2. Stop your current Opencast instance
3. Back up Opencast files and database (optional)
4. Run the appropriate database upgrade script(s)
     - `docs/upgrade/1.6_2.0.0` -> `docs/upgrade/2.0.1_2.0.2`
5. Review the configuration changes and adjust your configuration accordingly
6. Update the third party tools as documented
7. Build Opencast 2.1
8. Start Opencast


Additional Notes About 2.1.1
----------------------------

Opencast 2.1.1 is a bug fix release that fixes some major issues of Opencast 2.1.0. 
In Opencast 2.1.0 the distributed setup had a problem with the worker-node not starting properly.
This is now fixed. 

---

Opencast 2.0: Release Notes
===========================

*Opencast Matterhorn becomes simply Opencast*

For a long time Matterhorn was the one project of the Opencast community and it was hard to distinguish between the two
names. With the new major release and the move towards Apereo, the Board decided to harmonize the names and
drop the former codename “*Matterhorn*”. Hence, Matterhorn is dead, long live Opencast!

New Features
------------

 - **New administrative user interface** –
   One of the most obvious changes in the new release is the new administrative user interface. It has been completely
   rewritten from scratch, using up-to-date technologies and a cleaner design. For more details, have a look at the
   [Opencast Users Guide](http://docs.opencast.org/r/2.0.x/user).
 - **New Engage player** –
   Opencast 2.0 now offers a HTML5 video player. Its user-interface is accessible: you can control the player with
   keyboard-shortcuts, ARIA profiles support screen-readers and captions are supported.
   The architecture is very modular in design so that new plugins can easily be created. HLS is now supported as a
   streaming protocol but RTMP is still available through a Flash fallback.
    - [Player Architecture](http://docs.opencast.org/r/2.0.x/admin/modules/player.architecture/)
    - [Player Configuration](http://docs.opencast.org/r/2.0.x/admin/modules/player.configuration/)
 - **New media module** –
   The Media Module has been slightly updated. It offers a new tile design which adapts to different screen sizes,
   from mobile devices to regular desktop resolutions.
   Within the Media Module configuration an easy selection of various players has been implemented so that the
   administrator can define the default player to be used.
 - **New FFmpeg-based video editor backend** –
   This change allows us to get rid of the GStreamer dependency.
 - **New video segmenter** –
   Opencast 2.0 comes with a new video segmenter based on the FFmpeg `select` filter. It makes the process much faster
   and (if configured properly) will even allow detection of scene changes in presenter videos.
 - **New silence detector** –
   As with the video segmenter, the silence detector has been replaced with an FFmpeg-based implementation.
 - **New documentation** –
   Until now, the Opencast documentation was confusing because it was split-up into several wikis and people never knew
   where to look for a topic. All official documentation can now be found at
   [http://docs.opencast.org/](http://docs.opencast.org/). The documentation is also included in the source code, so
   that it is connected with the current state of development.  Apart from the official documentation, two wikis still
   exist. These are the [Opencast Adopters Wiki](https://opencast.jira.com/wiki) (meant for users to share their guides)
   and the [Opencast Development Wiki](https://opencast.jira.com/wiki/display/MH) (meant for storing working drafts).

Important Administrative Notes
------------------------------

 - **Apache ActiveMQ** –
   Since Opencast 2, the Apache ActiveMQ message broker is used to enable an asynchronous, fast and reliable data
   exchange between back-end and user interface. It requires, however, to run ActiveMQ as external service, much like
   running a separate database (e.g. MariaDB).
 - **No GStreamer 0.10 dependency** –
   For a long time, Opencast has used GStreamer 0.10 and the Java bindings for that version. This GStreamer version
   has been deprecated for years and is slowly disappearing from all major operating systems. Upgrading GStreamer proved
   nearly impossible since there are no Java bindings for the newer versions. Therefore, we decided to get rid of
   GStreamer, mainly by replacing it with FFmpeg.
 - **Hold State** –
   The new admin UI does not support hold-states anymore.

Removed Components
------------------

 - **Reference capture agent** –
   For a long time, Opencast came with a reference capture agent, providing a free, open source software capture agent.
   In the last years, however, it was mainly replaced by other capture agents. One reason for that was the fact that the
   development of the reference capture agent itself has come to a halt due to lack of interest. That is why
   it was decided to separate the capture agent code from the Opencast core and move it into its own project.
 - **GStreamer service** –
   As outlined before, GStreamer 0.10 has been removed from Opencast 2.0. Many parts have been thus replaced. One
   module that has not been replaced, but simply removed instead, is the GStreamer service, which provided a backend
   for other modules to talk to the deprecated GStreamer version using the deprecated Java bindings.

How to Upgrade
--------------

Note that backing up your Opencast instance before doing a major update is strongly recommended.

1. Check out/download Opencast 2.0
2. Stop your current Opencast instance
3. Back up Opencast files and database (optional)
4. Run the appropriate database upgrade script (`docs/upgrade/1.6_to_2.0`)
5. Review the configuration changes and adjust your configuration accordingly
6. Update the third party tools as documented
7. Rebuild the search indexes
    - Delete (or move) your search indices
        - `${org.opencastproject.storage.dir}/searchindex`
        - `${org.opencastproject.storage.dir}/seriesindex`
        - `${org.opencastproject.storage.dir}/schedulerindex`
    - The indexes will be rebuild automatically when re-starting Opencast. Rebuilding the indices can take quite a while
      depending on the number of recordings in your system.
8. Build Opencast 2.0
9. Start Opencast
