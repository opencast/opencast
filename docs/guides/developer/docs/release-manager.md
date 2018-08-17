Release Manager Guide
=====================

Role
----

The release manager is ultimately responsible for making sure that the development process is being followed during his
or her release.

The release manager has the following duties:

* To ensure that the release measures up to the quality expected by the community.
* To ensure that the development process is followed and amended if required.
* To ensure that the release is timely and does not languish in process.
* To encourage committers to be involved in testing and fixing of bugs.

It is in the release manager's responsibility to force decisions around the release, help negotiate the acceptance or
rejection of contributions and to provide regular updates about the release on list and during the weekly technical and
monthly adopter meetings. It is important to note that, while the release manager drives the release process, the
committer body is in charge of both the work and the decision making, meaning that votes and successful proposals from
this body take precedence over a release managers decision.

The release manager does not have to be a committer. In case the release manager is not, a committer must agree to be
the proxy release manager for communication on the confidential committers list.

A release manager can abandon the release at any time, giving up their duties and privileges of being the release
manager. The committer list must then either vote within 72 hours on a new release manager, or consider the release
abandoned (the major, minor, and maintenance numbers remain unchanged).

Duties
------

### Setting Release Schedule

Releases should happen twice a year, usually within a time span of 9.5 months between the cut of the previous release
branch and the final release. The release manager should create a release schedule as soon as possible.
For more details about the release schedule, have a look at the [Development Process](development-process.md)

### Creation of Release Branch

According to the set release schedule, at one point a release branch should be cut, effectively marking a feature freeze
for a given release.

Once a release process has started, a branch should be created with the proposed version number of that release. This
branch is created off of `develop` and should be named `r/N.x` (e.g. `r/3.x` for the Opencast 3.0 release branch).
In the following command, we assume that the release branch for 3.0 is to be created. For other versions, please adjust
the version number accordingly.

Create the Branch:

1. Grab the merge ticket for `develop`. You are going to make changes there and it becomes ugly if someone else changes
   something at the same time.

2. Check out `develop` and make sure it has the latest state (replace `<remote>` with your remote name for the community
   repository):

        git checkout develop
        git pull <remote> develop

3. Create and push release branch:

        git checkout -b r/3.x
        git push <remote> r/3.x

4. Make sure you did not modify any files. If you did, stash those changes.

5. That is it for the release branch. Now lets make sure we update the versions in `develop` in preparation for the next
   release:

        git checkout develop
        for i in `find . -name pom.xml`; do \
          sed -i 's/<version>3.0-SNAPSHOT</<version>4.0-SNAPSHOT</' $i; done

6. Have a look at the changes. Make sure no library version we use has the version `3.0-SNAPSHOT` and was accidentally
   changed. Also make sure that nothing else was modified:

        git diff

7. Check again for unwanted changes.

8. If everything looks fine, commit everything and push it to the community repository:

        git commit -as
        git push <remote> develop

9. Finally change the `fixVersion` and `summary` on the merge ticket for 3.0 and create a new one for develop.


At this point, the developer community should then be notified. Consider using the following as a template email:

    To: dev@opencast.org
    Subject: Release Branch for Opencast <VERSION> Cut

    Hi everyone,
    it is my pleasure to announce that the <VERSION> release branch has now been
    cut, available as r/<VERSION>. Pull requests for bug fixes are to be made
    against this branch.

    Remember that the release schedule for this release:

      <RELEASE_SCHEDULE>

    As always, we hope to have a lot of people testing this version, especially
    during the public QA phase.  Please report any bugs or issues you encounter.


### Check Status of Language Translations

On the date when the release branch is cut, the release manager is responsible to check whether there are language
translations that need to be included or excluded for the upcoming release.
Have a look at [Inclusion and Exclusion of Languages](localization.md) for the criteria.

1. Check whether translations not yet included in Opencast meet the inclusion criteria (candidates for includsion)
2. Check whether translations in Opencast meet the exclusion criteria (endangered translations)
3. Publish an announcement on the Opencast Users list that specifies:
    a. Translations that will be included in the upcoming release
    b. Endangered translations

Please create a single post on the Opencast Users list:

    To: users@opencast.org
    Subject: Opencast <VERSION>: Language translation status

    Hi everyone,

    While checking the translation statuses of the languages available on Crowdin (see [1]), we have found
    that the following language translations meet the criteria to be included in Opencast <VERSION>:

    - <LANGUAGE1> (<PERCENTAGE1>)
    - <LANGUAGE2> (<PERCENTAGE2>)
    - ....

    Sincerly,
    Your Opencast <VERSION> Release Manager

    [1] Opencast project on Crowdin, https://crowdin.com/project/opencast-community
    [2] Inclusion and Exclusion of Translations, https://docs.opencast.org/develop/developer/...

In case endangered languages have been identified, this needs to be communicated immediately to the Opencast community.
Please create a post for each endangered translation on the Opencast Users list:

    To: users@opencast.org
    Subject: Opencast <VERSION>: <LANGUAGE> translation is endangered! [HELP NEEDED!]

    Hi everyone,

    While checking the translation status of the <LANGUAGE> translation, we have found that it is
    only <PERCENTAGE> translated.

    This is not enough to justify its inclusion in the upcoming Opencast release (see [1]).

    We hereby declare the <LANGUAGE> translation endangered! This means that it will not be included in
    Opencast <VERSION> unless it is saved by the community.

    To save the <LANGUAGE> translation from removal from the Opencast release, <LANGUAGE> needs to be
    translated at least 90% until <DATE>.

    Sincerly,
    Your Opencast <VERSION> Release Managers

    [1] Inclusion and Exclusion of Translations, https://docs.opencast.org/


### Creating the Merge Ticket

The [Opencast pull request filter](http://pullrequests.opencast.org) links the versions currently in development. The
merge ticket identifier needs to be added to that filter. To do this, create a ticket with a title in the format of
`Merge the result of the current peer review to <VERSION>`. The ticket will be automatically detected by the pull
request filter and will appear shortly.


### Creating the Release Version

The release manager is responsible for creating, or triggering the creation of, the appropriate fix version for the new
release. You may be able to do this yourself by assigning a new fix version to their newly created merge ticket. If you
are unable to do that, please contact one of the JIRA administrators so we can create it for you!


### Release Documentation

The [Opencast release documentation](http://docs.opencast.org) will be built automatically from available release tags.
However, new branches need to be included.  As release manager, please talk to
the [administrator](https://docs.opencast.org/develop/developer/infrastructure/#administrators) of that
tool to ensure the ticket is added.


### Moderation of Peer Reviews

Apart from creating branches and tags, one of the most important things for release managers to do is to ensure that
pull requests for the release are going forward. This does not mean that the release manager has to do the reviews, but
he should talk to developers, remind them about outstanding reviews and help to resolve possible issues.


### Merging Release Branch Into Develop

To not have to merge bug fixes into several branches and create several pull requests, the release manager should merge
the release branch back into `develop` on a regular basis. It is encouraged to do that frequently, to not let the
branches diverge too much and to avoid possible merge conflicts.

To merge the release branch into `develop`. As example, we do that for 3.0. Please adjust the version accordingly:

1. Grab the merge ticket for `develop`. You are not doing any changes to the release branch, so you will not need that
   merge ticket. If someone else holds the merge ticket for a feature pull request, talk to that person that you will
   temporarily steal the ticket.

2. Update release branch:

        git checkout r/3.x
        git pull <remote> r/3.x

3. Update `develop`:

        git checkout develop
        git pull <remote> develop

4. Merge the release branch. Not that if large merge conflicts arise, you may ask for help from the people creating the
   problematic patches:

        git merge r/3.x

5. Push updated branch into the community repository:

        git push <remote> develop

6. Leave a comment in the merge ticket and assign it back to the next pull request in line on `develop`.


### Updating Translations

Updating the [localization translations](localization.md) is easy, and should be done at minimum as part of every
release candidate.


### Beta Versions and Release Candidates

For testing purposes, the release manager should regularly create beta versions. Especially before the public QA phase,
a beta version should exist. Additionally, he should create a release candidate for testing before proposing that a
release be cut.

Create a version/tag. Again, version 3.0 is used as example. Please adjust the version accordingly:

1. Switch to release branch you want to create the beta from:

        git checkout r/3.x

2. Switch to a new branch to create the release (name does not really matter):

        git checkout -b r/3.0-beta1

3. Update the [localization translations](localization.md).

4. Make version changes for release. You can use `sed` to make things easier. Please make sure, the changes are correct:

        for i in `find . -name pom.xml`; do \
          sed -i 's/<version>3.0-SNAPSHOT</<version>3.0-beta1</' $i; done

5. Commit changes and create release tag:

        git commit -asS -m 'Opencast 3.0-beta1'
        git tag -s 3.0-beta1

6. Switch back to release branch and push tags:

        git checkout r/3.x
        git push <remote> 3.0-beta1:3.0-beta1

7. You can remove the new branch afterwards:

        git branch -D r/3.0-beta1

For a release candidate, instead of `A.B-betaX` the tag should be named `A.B-rcX`.

At this point the developer community should then be notified. Consider using the following email template:

    To: dev@opencast.org
    Subject: <VERSION> Available for testing!

    Hi everyone,
    I am pleased to announce that Opencast <VERSION> is now available for
    testing. Please download the source from GitHub [1] or use git to check
    out the tag.

    Issue Count:

      Blockers   <BLOCKER_COUNT>
      Critical   <CRITICAL_ISSUE_COUNT>
      Major      <MAJOR_ISSUE_COUNT>
      Minor      <MINOR_ISSUE_COUNT>

    Please test this release as thoroughly as
    possible.

    [1] https://github.com/opencast/opencast/releases

If the version is a release candidate, you probably want to highlight that there are no *Blockers* left at the moment
and *#propose* this to become the final release.


### Releasing

Once the proposal for a release candidate to become the final release has passed, the release manager should create the
final release. In the following example, again, version 3.0 is used. Please adjust the version accordingly. We also
assume the final release should be based on `3.0-rc2`.

1. Check which commit the release candidate was based on:

        git merge-base 3.0-rc2 r/3.x
          060dfc3e2328518ae402846577fc6e7ce3b650d7

2. Switch to commit you want to create the release from:

        git checkout 060dfc3e2328518ae402846577fc6e7ce3b650d7

3. Switch to a new branch to create the release (name does not really matter):

        git checkout -b r/3.0

4. Add release notes, and update the changelog using the `create-changelog`
   [helper script](https://github.com/opencast/helper-scripts/tree/master/create-changelog) to
   easily format the list, the commit:

        vim docs/guides/admin/docs/releasenotes.md docs/guides/admin/docs/changelog.md
        git commit docs/guides/admin/docs/releasenotes.md docs/guides/admin/docs/changelog.md -sS

5. Update the [localization translations](localization.md).

6. Merge release notes into release branch:

        git checkout r/3.x
        git merge r/3.0
        git checkout r/3.0

7. Make version changes for release. You can use `sed` to make things easier. Please make sure, the changes are correct:

        for i in `find . -name pom.xml`; do \
          sed -i 's/<version>3.0-SNAPSHOT</<version>3.0</' $i; done

8. Commit changes and create release tag:

        git commit -asS -m 'Opencast 3.0'
        git tag -s 3.0

9. Switch back to release branch, push release notes and tags:

        git checkout r/3.x
        git push <remote> 3.0:3.0
        git push <remote> r/3.x

10. You can remove the new branch afterwards:

        git branch -D r/3.0

11. Release the branch in JIRA, and create the next one. Talk to your JIRA administrators to have this done.

12. Push the built artifacts to Maven. Bug the QA Coordinator to do this so that he remembers to set this up from the CI
    servers.

13. Push the built artifacts back to GitHub. Create a new release using the
    [graphical user interface](https://github.com/opencast/opencast/releases) to upload the distribution tarballs
    manually.

14. Write a short paragraph about the changes to the new version, then email it to the QA Coordinator to update the
    Opencast website.


Finally, send a release notice to Opencast's announcement list. Note that posting to this list is restricted to those
who need access to avoid general discussions on that list. In case you do not already have permissions to post on this
list, please ask to be given permission. For the message, you may use the following template:

    To: announcements@opencast.org
    Subject: Opencast <VERSION> Released
    Hi all,
    it is my pleasure to announce that Opencast <VERSION> has been released and
    can be downloaded from GitHub [1] or checked out via git.

    The documentation for this release can be found at [http://docs.opencast.org].

    RPM and Debian packages will be available soon. Watch for announcements on
    the users list.

    To all committers and involved contributors, thank you for all your work.
    This could not have happened without you and I am glad we were able to work
    together and get this release out.

    [1] https://github.com/opencast/opencast/releases


### Appointment of Next Release Manager

#### Identification of a volunteer

After the release branch is cut, all work on `develop` is effectively the preparation for the next release. At this
point, the release manager sends an inquiry to the users, developers and community list to identify a volunteer for the
job of release manager for the next release.

For that, this email template may be used:

    To: dev@opencast.org
    Subject: Opencast <NEXT_RELEASE_VERSION> release manager wanted

    Hi everyone,
    as release manager of Opencast <VERSION> release, it is my duty to announce
    that the Opencast community is looking for a release manager of the upcoming
    <NEXT_RELEASE_VERSION> release between now and <INTENDED RELEASE DATE>
    (release date).

    Note that the release manager's duties do not contain a lot of technical
    work. Instead, they focus on motivation, coordination, and ensuring that the
    development process [1] is being followed. The role of the release manager
    is described in more detail in the Opencast development documentation wiki
    [2].

    The job does not have to be done by one person alone and it has proven good
    practice to have two people fill this job as co-release managers to help
    keep up the process during vacation, sickness and in case of local
    emergencies.

    I am looking forward to your applications on list, please voice your
    interest no later than <TWO WEEKS FROM NOW>.

    [1] http://docs.opencast.org/develop/developer/development/
    [2] http://docs.opencast.org/develop/developer/release-manager/


#### Initiate the Vote

In the case where someone steps up and offers to fill in the role of a release manager for the upcoming release, a vote
is held on the committers list to determine whether the candidate is deemed suitable for the position. Should there be
more than one candidate wanting to become the release manager for the next release, the candidate with the largest
number of votes and no “-1” gets elected.

This email template may be used to initiate the vote:

    To: committers@opencast.org
    Subject: [#proposal] Vote on Release Managers of Opencast <NEXT_RELEASE_VERSION>

    Hi everyone,
    after roughly two weeks of looking for candidates for the position of the
    release manager of the upcoming Opencast <NEXT_RELEASE_VERSION> release, I
    am happy to announce that the following Opencast members have volunteered
    for the position and have expressed the intention of sharing the position as
    release managers:

      <Name, Institition>
      <Name, Institution>

    As the release manager of Opencast <BRANCH_VERSION_FINAL> and in accordance
    with the development process [1], I hereby open the vote on accepting the
    applications of <Name(s)>. The vote will be open for the coming 72h.

    [1] http://docs.opencast.org/develop/developer/development/

#### Announcing Election Results

Votes, once complete, are announced on the developer list by the current QA, or the last release manager.

As an example:

    To: dev@opencast.org
    Subject: Release Managers of Opencast <NEXT_RELEASE_VERSION>

    Hi everyone,

    It is my pleasure to announce that the following person/people have been
    elected to position of Release Manager for the upcoming Opencast <NEXT_RELEASE_VERSION>
    release:

      <Name, Institution>
      <Name, Institution>

    We wish to thank them for volunteering, and hope the release goes smoothly!


Once the vote has been held, the current release manager announces the elected release manager on list, and the newly
elected release manager is expected to start work on the release shortly after.
