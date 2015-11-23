Opencast Proposals
==================

All important decisions for Opencast have to be made on list. To do that committers may send proposals (marked with
#proposal) to list on which other committers may then vote. Opencast uses lazy consensus meaning that no response
signals agreement. Apart from that committers may vote with:

 - `+1` yes, agree - also willing to help bring about the proposed action
 - `+0` yes, agree - not willing or able to help bring about the proposed action
 - `-0` no, disagree - but will not oppose the action going forward
 - `-1` veto, disagree - opposes the action going forward and must propose an alternate action to address the issue or a
   justification for not addressing the issue


Passed Proposals
----------------

### How to release a new Opencast version…
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Fri, 14 Aug 2015 12:54:51 UTC

    Hi everyone,
    serving as co-release manager for two versions of Opencast, I noticed
    that our current release process has some aspects of the release defined
    in a way that is more hindering than helpful and I want to #propose a
    slight change to these recommendations.

    I hereby #propose:

    1. Get rid of the `master` branch, make `develop` the main branch.
    2. Do not use the --no-ff flags for merges
    3. Do not create versions/tags in a release branch. Separate them.


    Reasoning:

    1. The short explanation whould be: When did you explicitely checked
       out `master` last time? People rarely do that. If I want a specific
       version, I use the tag, if not I want the release branch or
       `develop`.
       If you think about it, then the whole reason for `master` in GitFlow
       is to always provide the last stable version to users who just check
       out the repository and do nothing else. The problem with Opencast is,
       that we support multiple versions at the same time. If in a couple
       of weeks 1.6.2 is being released, it is the latest stable. Is it? If
       I check out `master`, however, I will still get 2.0 as we cannot
       merge 1.6.x afterwards. While you can grasp the reasons behind this,
       it is a bit confusing for users and it is much easier to just tell
       them to use the tag to check out a specific version.
       That it, if they do not use the tarballs from BitBucket anyway.

    2. First of all, most people seem to be using BitBucket for
       auto-merging and it does not use --no-ff. So we are not really
       consistent anyway. Being consistent and using  --no-ff would mean to
       forbid the usage of the BitBucket merge.
       Second, have a look at the confusing mess that are the current
       branches (I tried to find something in the visualization a while ago
       but gave up). It would be much cleaner to try using fast-forward
       merges. So instead of using non-fast-forward commits I would argue
       that we should instead try to use as many fast-forward commits as
       possible.

    3. Once we decided to have the tags in our branches like this:

          ---- A ---- B (tagged) ----- C ---- D -->

       A is the commit containing the version that is decided to be
       released. B is the tagged version. It is exactly the same code as A
       except for the pom.xml versions that are modified. Finally C then
       reverts B as the modified version should not be part of the release
       branch, .... After C, the code is basically A again except for the
       history (which we later need to merge which can be problematic). D
       would then be the next “real” commit, meaning the next fix.

       Much easier to handle would be the following structure:

         ---- A ---- D -->
               \
                B (tagged)

       You do not have to revert that commit, you do not need to merge the
       easily conflicting pom.xml changes and in the end, you would anyway
       check out the tag using  git checkout <tag>  if you want that
       specific version


    Branching structure:

    To have a complete overview, this is what the new branching structure
    would look like:


      develop --*--*--*--*--*----*--------*--------*---->
                       \        /                 /
                r/x.y.z *--*--*---*--*--*--*--*--*---->
                               \                  \
                                * x.y.z-beta1      * x.y.z-rc1

    Regards,
    Lars



### Moving away from the 3rd party scripts
Proposed by Greg Logan <gregorydlogan@gmail.com>, passed by Fri, 24 Jul 2015 16:45:40 UTC

    Hi folks,

    As it stands right now we depend on the 3rd party tool script to
    install a great many of our 3rd party dependencies.  These are
    utilities like tesseract, ffmpeg, sox, etc.  This script is maintained
    by Matjaz, in his own time.  I'd like to take a moment to thank him
    for a doing a great job on a particularly annoying aspect of
    supporting our work!  I know it hasn't been easy, especially
    supporting vast number of different OS versions!

    With the release of 2.0 I noticed that our 3rd party tool script is
    becoming both a little out of date, and difficult to maintain.  I took
    a quick look around and it seems like *most* of our dependencies are
    available from normal distribution repositories for Debian based
    systems, and I'm told that there is a similar situation for Redhat
    based systems.  I am unsure of how many of our users are running
    Matterhorn on Mac, but I would hope that our developers who are
    working on Mac would be able to provide instructions and/or binaries
    for those users.  The only dependency where there might be a universal
    sticking point is ffmpeg (due to patent concerns), however ffmpeg
    builds a full static binary with each release, so I assume we can
    either depend on this and/or cache them somewhere.

    What this means is that we can potentially remove the 3rd party script
    from our repository.  I hereby #propose we find a way to do that,
    which would remove the 3rd party script from the repository and
    replace it with a number of new steps in the install documentation.

    G



### Status of youtube in 2.0 and #proposal to change the default workflow
Proposed by Rüdiger Rolf <rrolf@Uni-Osnabrueck.DE>, passed on Sat, 13 Jun 2015 14:15:55 UTC

    Hi list!

    There was some discussion in the DevOps meeting yesterday if the
    Youtube distribution would work or not. I offered to check this.

    The good news first: IT WORKS!
    Just follow this manual and your Matterhorn - ups Opencast -  is ready
    to distribute to Youtube.

      http://docs.opencast.org/r/2.0.x/admin/modules/youtubepublication/

    The bad news: The default workflow definition does not really support
    the publishing on Youtube, as only one video file could be published
    by the current WOH.

      https://opencast.jira.com/browse/MH-10920

    The reason is simple and the fix would be too. But there are some
    options to fix this:

    1. Remove the option to distribute to Youtube from the default workflow
       definition, as the complicated configuration would have to come
       first anyway.
    2. Only let "presenter" or "presentation" be published to Youtube. We
       would need a new youtube tag and add this to the compose operation
       and the youtube operation.
    3. Introduce the composite operation to the workflow definition and
       publish only the resulting single stream to Youtube.
    4. Upgrade the WOH to support publishing of multiple files.

    I would say that option 4 could be 2.1 goal, but not for 2.0.

    I would #propose to go for option 1, as nobody can use Youtube
    out-of-the-box anyway. And the admin could then setup  an appropriate
    Youtube workflow for their needs too.

    Regards
    Rüdiger



### Episode DublinCore Catalog
Proposed by Karen Dolan <kdolan@dce.harvard.edu>, Passed on Sat, 30 May 2015 12:39:05 UTC

    Dear Opencast-ees,

    The following proposal addresses MH-10821[1]. An issue that exposes a
    know long time ambiguity regarding metadata and the ingest service.
    The reason that its a proposal is that it normalizes the handling of
    inbound episode catalog metadata in the ingest service.


    1) A new configuration parameter, boolean, for the Ingest Service. The
       config param identifies if episode metadata precedence is for
       Ingestee (i.e. Opencast system) or the Ingester (i.e. Capture
       Agent).

       For example: at our site, the scheduling entity is the metadata
       authority. All updates are made to the Scheduling endpoint. The
       Capture Agent always has stale episode catalog metadata. At other
       sites, updates are made on the Capture Agent directly. The
       community default can be for priority to the Capture Agent.

    2) All Ingest endpoints perform the same consistent process to ensure
       that an episode catalog will exist, manually or automatically
       provided.

    3) The process performs the following...

    3.1. Gather data

     - Check if inbound media package contain a reference to an Episode
       DublinCore catalog and if that catalog contains a title.
     - Check if the inbound media package contains a title attribute.
     - Check if the Workflow service has a reference to the mediapackage's
       Episode Dublin Core catalog
     - Check if the Scheduler service retained a reference to the event's
       Episode Dublin Core catalog

    3.2. Use config param to prioritize action on acquiring an Episode dc
       catalog for the media package

      If Capture Agent metadata takes precedence:
         - Take the inbound Episode dc catalog, if it exists
         - Take the Episode dc catalog from the workflow service, if it
           exists
         - Take the Episode dc  catalog from the scheduler service, if it
           exists
         - Create an Episode dc catalog from the title in the media
           package,, if it exists
         - Create an Episode dc catalog using a default title (i.e.
           "Recording-1234556XYZ")

     If Opencast metadata takes precedence:
         - Take the Episode dc catalog from the workflow service, if it
           exists
         - Take the Episode dc  catalog from the scheduler service, if it
           exists
         - Take the inbound Episode dc catalog if it exists
         - Create an Episode dc catalog from the title in the media
           package, if it exists
         - Create an Episode dc catalog using a default title (i.e.
           "Recording-1234556XYZ")

    I'll start a pull for the above, and appreciate any thoughts.

    Regards,
    Karen

    [1] https://opencast.jira.com/browse/MH-10821



### Dropping Taglines
Proposed by  Greg Logan <gregorydlogan@gmail.com>, Passed on Fri, 29 May 2015 16:19:09 UTC

    Hi folks,

    I hereby propose that we drop the practice of having taglines.  I
    propose this because we don't have a place in the new admin UI to put
    them, nor have I ever heard any of the adopters make use of it.  I know
    we don't use it as a committing group, which means that *no one* is
    using them.

    G



### Wiki Cleanup
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Fri, 24 May 2015 11:36:49 UTC

    Hi everyone,
    since we partly switched to our new documentation [1] I would like to
    make sure that the old and mostly outdated documentation goes away so
    that no one stumbles upon that. When I had a look at the wikis we
    currently have I noticed that most of our 17(!) wikis have not been
    touched in years and can probably go away.

    Here is a list of our wikis and what I #propose to do with/to them:

    Keep (maybe clean-up a bit):
     - Matterhorn Adopter Guides
     - Matterhorn Developer Wiki
     - Opencast Matterhorn D/A/CH
     - Opencast Matterhorn Español
     - LectureSight

    Export as PDF to archive the contents and then delete:
     - Matterhorn Release Docs - 1.0
     - Matterhorn Release Docs - 1.1
     - Matterhorn Release Docs - 1.2
     - Matterhorn Release Docs - 1.3
     - Matterhorn Release Docs - 1.4
     - Matterhorn Release Docs - 1.5
     - Matterhorn Release Docs - TRUNK

    Keep until 2.1 is out then export as PDF and delete:
     - Matterhorn Release Docs - 1.6

    Just delete:
     - Analytic video annotation
     - Infra
     - Matterhorn Documents
     - Opencast Community


    Please let me know if you agree or disagree with this proposal.

    Regards,
    Lars

    [1] http://documentation.opencast.org



### Jira Clean-Up
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Fri, 8 May 2015 11:52:16 UTC

    Hi everyone,
    as discussed in the technical meeting, I hereby #propose:

      The “Blocker” and “Release Blocker” severity status are more or less
      redundant. As part of cleaning up Jira, let us remove the “Release
      Blocker” severity in favor of “Blocker”.

    As footnote, some statistics: Since the beginning of 2014, 70 Release
    Blockers have been files in Jira while mere *8* Blockers have been
    files.

    Regards,
    Lars



### Opencast Documentation
Proposed by Rüdiger Rolf <rrolf@uni-osnabrueck.de>, Passed on Sat, 02 May 2015 14:43:28 UTC

    Hi all,

    Tobias, Basil, Lars and I discussed status of the current migration of
    the Opencast (Matterhorn) documentation to GIT. We still see some open
    issues that need clarification so we would like to propose the
    following points:

    *1. Formating and Hosting of the Documentation *

    We want to use https://readthedocs.org to or a similar service create
    a more appealing HTML version from the Markdown of the documentation.

    The documentation will be versioned there so that for older versions
    the documentation is still available. By default the "latest" version
    is shown.  The versions of the documenation will be generated based on
    the release branches.

    *2. Structure of the Documentation*

    We see the documentation in*Git *separating into 3 sections:

     - /Administration Guide/: with information about the installation,
       configuration, customization and integration. This will be the part
       of information by an administrator to setup
       Opencast.

     - /Developer Guide/: All information related to implementation
       details of Opencast, so that this will be updated in a pull request
       (API changes, module descriptions, architecture). The development
       process documents should also go here as only committers usually
       should change these.

     - /User Guide/: Documentation of the (new) Admin UI that was already
       started by Entwine and the Engage UI (especially Theodul Player).
       This guide should only describe options available on the UIs.

    Within the *Wiki* we still see the need for 2 sections:

     - /Developer Wiki/: Proposals, working documents and meeting notes
       will be kept here so that anybody can edit these. So information
       not to close to any existing implementation that might still be in
       a process of discussion can be found here.

     - /Adopters Wiki/: This can be the place where adopters share their
       best practises, configurations, hardware recommendations,
       third-party software documentation etc. Again anyone can contribute
       to this wiki.

    The difference between the Wiki and Git is in the first line that the
    Git documentation should become a quality assured ressource for
    Opencast users. The Git documentation should be reviewed within the
    release process and it will be part of the review process of a pull
    request, to make sure that the needed documentation changes have been
    contributed too.

    The Wikis on the other hand should be a more open platform where
    everybody can contribute and users might find cookbooks to enhance
    their system, or they can share ideas.

    So now we would like to get your opinion on this proposal.

    Thank you,
    Rüdiger



### Requirement Specification
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Thu, 16 Apr 2015 15:55:31 UTC


    On list or IRC we often see that people do not really know the current
    requirements for a specific version of Opencast Matterhorn. Of course
    there are the pom.xml files specifying internal dependencies, but there
    is nothing for 3rd-party-tools, ...

    It would be nice to add a file specifying these requirements in a
    format that is easy to parse and can hence be used for automatic
    scripts to generate dependency lists, ...

    That is why I hereby #propose to add a requirements.xml file that
    specifies the requirements for Opencast Matterhorn:
     - Required tools including versions
     - Which modules require which tools
     - Which modules conflict with each other (negative requirement)

    This is mainly what is not specified by the pom.xml files yet.



### Jira Clean-Up (Tags VS Labels)
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Thu, 19. Mar 2015 15:43:20 UTC

    …then hereby I officially #propose removing the labels from Jira.

For more details, have a look at the mail thread at:

    https://groups.google.com/a/opencast.org/forum/#!topic/matterhorn/vIdWQkZmbdQ



### FFmpeg Update
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Sat, 14 Mar 2015 22:12:18 UTC

    Looking at the FFmpeg project for the last two years, you will notice
    that they developed a pretty stable release cycle with a release of a
    new stable version approximately every three month.

    To stop us from having to propose an update again and again, I hereby
    propose the following general rule for our support of FFmpeg:

      A Matterhorn release will oficially support the latest stable
      version of FFmpeg released at the time the release branch is cut and
      all other FFmpeg versions with the same major version number released
      afterwards.

    For example, for Matterhorn 2 this would mean that we will officially
    support FFmpeg 2.5.4 and all later 2.x versions like 2.6 which has
    been released on the 7th of March or a possible 2.7 onece it is
    released. We would, however, not necessarily support an FFmpeg 3 as it
    *might* come with an interface change that *could* break compatibility.

    That obviously does not mean that older versions of FFmpeg just stop
    working. In fact, most parts of the default Matterhorn configuration
    should at the moment still work with FFmpeg 1.x but we will not test or
    fix compatibility problems.


### Proposal Log
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Sat, 14 Mar 2015 16:35:08 UTC

    It would be wonderful if we had a central place to look up the proposals
    that have passed.

    That is why I hereby propose that:

     - We create a proposal log in our new documentation containing all
       proposals that have passed on list.

     - A proposal will become effective only after it is written down in
       that log. That should usually be done by the person who sent out
       that proposal.

    This will, of course, not affect the existing decision making rules
    (proposal on list, marked with #proposal, lazy consensus after three
    days, no -1, ...)
