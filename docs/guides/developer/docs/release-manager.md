Release Manager Guide
=====================

The single most important duty of release managers is to keep an eye on their release, notify the community about
possible problems in a timely manner and encourage community members to help out if needed. While working on Opencast's
code is often done as well during the release process, for release managers this is secondary to the communication and
management role and with few exceptions no requirement for this position.

The community has a number of expectations for release managers, and their handling of the problems which may arise
during the release cycle. The core of these expectations are:

- The development process should be followed or amended if required
- The release should be on time
- The release should not have any critical technical, usability or security issues

This means that release managers may need to force decisions around the release, help negotiate the acceptance or
rejection of contributions and provide regular updates about the release on list and during the technical and adopter
meetings. It is important to note that, while release managers drive the release process, the committer body is in
charge of both the work and the decision making, meaning that votes and successful proposals from this body take
precedence over release manager decisions.


Responsibilities
----------------

While a general rule is certainly just to look out for the release, work together with the community to make the release
work properly and be pragmatic about the process, there are a few tasks which really can only be done by release
managers.

For all of these tasks, it's generally a good idea to look at previous releases and at their solutions for the tasks.
Often, completing the task is simply a matter of repeating or updating previous work (e.g. advance the previous release
schedule by six months).

### Release Notes

It's usually a good idea to create or clean the release notes page early in the release cycle. This allows for a place
to put the release schedule, short descriptions of features or noteworthy configuration changes early on.

### The  Release Schedule

Releases should happen twice a year, usually within a time span of 9.5 months between the cut of the previous release
branch and the final release. The release managers should create a release schedule as soon as possible, announce it on
list and publish it on the release notes page.

### Release Branch

According to the set release schedule, at one point a release branch should be cut, effectively marking a feature freeze
for a given release.  This branch is split off `develop` and should be named `r/N.x` (e.g. `r/6.x` for the Opencast 6
release branch).

Example on how to create the Opencast 7 release branch:


1. Check out `develop` and make sure it has the latest state (replace `<remote>` with your remote name for the community
   repository):

        git checkout develop
        git pull <remote> develop

2. Make sure you did not modify any files. If you did, stash those changes:

        git status   # check for modified files
        git stash    # stash them if necessary

3. Create and push the new release branch:

        git checkout -b r/7.x
        git push <remote> r/7.x

4. That is it for the release branch. Now update the versions in `develop` in preparation for the next release:

        git checkout develop
        for i in `find . -name pom.xml`; do \
          sed -i 's/<version>7-SNAPSHOT</<version>8-SNAPSHOT</' $i; done

5. Have a look at the changes. Make sure no library version we use has the version `6-SNAPSHOT` and was accidentally
   changed. Also make sure that nothing else was modified:

        git diff
        git status | grep modified: | grep -v pom.xml   # this should have no output

6. If everything looks fine, commit the changes and push it to the community repository:

        git add $(git status | grep 'modified:.*pom.xml' | awk '{print $2;}')
        git commit -s -m 'Bumping pom.xml Version Numbers'
        git push <remote> develop


At this point, the developer community should then be notified by writing an email like the following to the developers
list:

```no-highlight
To: dev@opencast.org
Subject: Release Branch for Opencast <version> Cut

Hi everyone,
the Opencast <version> release branch (r/<version>) has been
cut.  Pull requests for bug fixes may still be made against
this branch but, as usual, features should go into develop
instead.

Remember the release schedule for this release:

  <release_schedule>

As always, we hope to have a lot of people testing this
version, especially during the public QA phase. Please
report any bugs or issues you encounter.

For testing, you may use https://stable.opencast.org if you
do not want to set up a test server yourself. The server is
reset on a daily basis and will follow the new release
branch with its next rebuild.

Additionally, look out for announcements regarding container
and package builds for testing on list if you want to run
your own system but do not want to build Opencast from
source.
```


### Status of Translations

After the release branch is cut, the release managers should check if there are languages to be in- or excluded for the
upcoming release as specified by the [criteria in the localization documentation](localization.md) and notify the
community about the status of Opencast's translations if necessary.

Example announcement for included languages:

```no-highlight
To: users@opencast.org
Subject: Opencast <VERSION>: Translation Status

Hi everyone,
while checking the translation statuses of the languages
available on Crowdin[1], we have found that the following
languages meet the criteria to be included in Opencast
<VERSION>:

- <LANGUAGE1> (<PERCENTAGE1>)
- <LANGUAGE2> (<PERCENTAGE2>)
- ....


[1] Opencast on Crowdin
    https://crowdin.com/project/opencast-community
[2] Inclusion and exclusion of translations
    https://docs.opencast.org/develop/developer/localization/#inclusion-and-exclusion-of-translations
```

Example announcement for endangered languages:

```no-highlight
To: users@opencast.org
Subject: Opencast <VERSION>: <LANGUAGE> Translation Endangered

Hi everyone,
while checking the <LANGUAGE> translation status of
Opencast, we have found that it is only <PERCENTAGE>
translated.

This is not enough to justify its inclusion in the upcoming
Opencast release[1], meaning that the <LANGUAGE> translation
is in danger of being removed in Opencast <VERSION> if its
status stays the same.

To save the <LANGUAGE> translation from removal, it needs to
be translated at least 90% before <DATE>.

Sincerely,
Your Opencast <VERSION> Release Managers

[1] Inclusion and Exclusion of Translations
    https://docs.opencast.org/develop/developer/localization/#inclusion-and-exclusion-of-translations
```

A specific translation week may be announced using an email
like this:

```no-highlight
To: users@opencast.org
Subject: Opencast <version>: Translation Week

Hi everyone,
starting on <date> the Opencast <version> translation week
will take place, during which we particular focus on
improving Opencast's translations.


Can I help?
-----------

Everybody that speaks a different language or dialect and
feels confident enough to participate can participate.


How can I help?
---------------

We use Crowdin [1] to manage translations. Please sign up
and request to help with a particular language.


What is the current status?
---------------------------

Fully translated:

- …

Mostly translated (>80% translated):

- …

Endangered translations (≤80% translated):

- …

Note that we can add any additional languages you are
willing to translate.

If you have any additional questions, please do not hesitate
to ask.

[1] https://crowdin.com/project/opencast-community
```


### Moderation of Peer Reviews

Release managers should regularly check open pull requests for possible problems (no reviewers, discussions in need of
moderation, issues that should be raised to community awareness, …) and bring these up in the technical meeting, on the
developer list or wherever appropriate.


### Merging Release Branches

To not have to merge bug fixes into several branches and create several pull requests, the release branch should be
merged down on a regular basis. Assuming, for example, that `r/6.x` is the latest release branch, merges should happen
like this:

    r/5.x → r/6.x → develop

While any committer may do this at any time, it is good practice for release managers to do this for their release
branches on a regular basis.

For example, to merge the latest release branch into `develop`, follow these steps:

1. Update your local repository

        git fetch <remote>

2. Update `develop`:

        git checkout develop
        git merge <remote>/develop   # this should be a fast-forward merge

3. Merge the release branch. Note that if large merge conflicts arise, you may ask for help from the people creating the
   problematic patches:

        git merge <remote>/r/6.x

4. Push the updated branch into the community repository:

        git push <remote> develop


### Updating Translations

Updating the [localization translations](localization.md) is automated for existing translation files. If new files need
to be added, it is something that should happen early during the release process. If files need to be removed, this
needs to be done manually.


### Releasing

The following steps outline the necessary steps for cutting the final release:

1. Switch to and update your release branch and ensure the latest state of the previous release branch is merged:

        git checkout r/6.x
        git fetch <remote>
        git merge <remote>/r/6.x
        git merge <remote>/r/5.x

2. Add the release notes, and update the changelog. The `create-changelog` [helper script
   ](https://github.com/opencast/helper-scripts/tree/master/create-changelog) is a convenient tool for this.

        cd docs/guides/admin/docs/
        vim releasenotes.md
        vim changelog.md
        git commit -S releasenotes.md changelog.md -m 'Updated Release Notes'
        git push <remote> r/6.x

3. Switch to a new branch to create the release (name does not really matter):

        git checkout -b tmp-6.0

4. Make the version changes for the release. You can use `sed` to make things easier but please make sure that the
   changes are correct:

        for i in `find . -name pom.xml`; do \
          sed -i 's/<version>6-SNAPSHOT</<version>6.0</' $i; done

5. Have a look at the changes. Make sure no library version we use had the version `6-SNAPSHOT` and was accidentally
   changed. Also make sure that nothing else was modified:

        git diff
        git status | grep modified: | grep -v pom.xml   # this should yield no output

6. Commit the changes and create a release tag:

        git add $(git status | grep 'modified:.*pom.xml' | awk '{print $2;}')
        git commit -S -m 'Opencast 6.0'
        git tag -s 6.0

7. Push the tag to the community repository (you can remove the branch afterwards):

        git push <remote> 6.0:6.0

8. Push the built artifacts to Maven. Bug the QA Coordinator to do this so that he remembers to set this up from the CI
    servers. If you want to do this yourself please read the [infra documentation](infrastructure/maven-repository.md#pushing-to-maven-central).

9. Create a new release on GitHub using the [graphical user interface](https://github.com/opencast/opencast/releases)
    to upload the distribution tarballs.

Finally, send a release notice to Opencast's announcement list. Note that posting to this list is restricted to those
who need access to avoid general discussions on that list. In case you do not already have permissions to post on this
list, please ask to be given permission. For the message, you may use the following template:

```no-highlight
To: announcements@opencast.org
Subject: Opencast <VERSION> Released

Hi everyone,
it is my pleasure to announce that Opencast <VERSION> has
been released:

  https://github.com/opencast/opencast/releases

The documentation for this release can be found at:

  https://docs.opencast.org/r/<VERSION>/admin/

RPM and Debian packages as well as Docker images will be
available soon. Watch for announcements on the users list.

To all committers and involved contributors, thank you for
all your work. This could not have happened without you and
I am glad we were able to work together and get this release
out.
```


### Appointment of Next Release Manager

After the release branch is cut, all work on `develop` is effectively the preparation for the next release. At this
point, the release managers should send an inquiry to the development list to identify volunteers for the next release.

For that, this email template may be used:

```no-highlight
To: dev@opencast.org
Subject: Opencast <NEXT_RELEASE> release managers wanted

Hi everyone,
the Opencast community is looking for release managers for
the upcoming <NEXT_RELEASE> (Feature freeze around <DATE>,
release around <DATE>).

Note that the release manager's job contains very little
technical work. Instead, they mostly focus on motivation and
coordination of the community during the release phase. The
role of release managers is described in more detail in the
Opencast development documentation:

  https://docs.opencast.org/develop/developer/release-manager/

In the past, it has proven good practice to have two people
fill this job as co-release managers to help keep up the
process during vacation, sickness and in case of local
emergencies.

I am looking forward to your applications on list, please
voice your interest until <DATE>.
```

In the case where someone steps up and offers to fill in the role of a release manager for the upcoming release, a vote
is held on the committers list to determine whether the candidates are deemed suitable for the position.

This email template may be used to initiate the vote:

```no-highlight
To: committers@opencast.org
Subject: [#vote] Vote on Release Managers of Opencast <NEXT_RELEASE>

Hi everyone,
I am happy to announce that the following community members
have volunteered themselves for the position of the Opencast
<NEXT_RELEASE> release manager and have expressed the
intention of sharing the position:

  <NAME, INSTITITION>
  <NAME, INSTITUTION>

I hereby open the vote on accepting them for this position.
The vote will be open for the coming 72h.
```

Once the voting is complete, the result should be announced on the development list:

As an example:

```no-highlight
To: dev@opencast.org
Subject: Release Managers of Opencast <NEXT_RELEASE>

Hi everyone,
it is my pleasure to announce that the following people have
been elected to be the release managers for the upcoming
Opencast <NEXT_RELEASE> release:

  <NAME, INSTITUTION>
  <NAME, INSTITUTION>

We wish to thank them for volunteering, and hope the release
goes smoothly!
```
