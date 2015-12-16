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
