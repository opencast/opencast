Release Manager Guide
=====================

The single most important duty of release managers is to keep an eye on their release, notify the community about
possible problems in a timely manner and encourage community members to help out if needed. While working on Opencast's
code is often done as well during the release process, for release managers this is secondary to the communication and
management role.  With few exceptions, a release manager should not *need* to be able to code.

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

Recommended practices for release managers
------------------------------

This is a recommendation of best practices to help to organize the duties of the release managers after they have been
chosen.

### After the election of the release manager
- Create a draft pull request for the release notes early, and keep it up to date as development progresses.
- Create a draft article about the release in the opencast.org Wordpress instance.
- Create draft slides for presentation in summits and adopters meetings.

### After the end or begining of each month
- Check the merged pull requests
    - Update the release notes (If necessary)
    - Add any missing tag from each pull request
- Update the slides (If necessary)

### After the end of translation week
- Add/Remove languages acording the translation criteria


Responsibilities
----------------

While a general rule is certainly to look out for the release, work together with the community to make the release
work properly, and be pragmatic about the process, there are a few tasks which really can only be done by release
managers.

For all of these tasks, it's generally a good idea to look at previous releases and at their solutions for the tasks.
Often, completing the task is simply a matter of repeating or updating previous work (e.g. advance the previous release
schedule by six months).

### Release Notes

It's usually a good idea to create or clean the release notes page early in the release cycle. This allows for a place
to put the release schedule, short descriptions of features or noteworthy configuration changes early on.  It also
gives developers a clean slate to work from, otherwise there will be constant conflicts as one PR alters the notes file
underneath a second PR.

Also, once you have processed the release notes text files, please remove them from the history so that future RMs do
not need to parse out which notes are for their new branch, and which belong to the old one.

### Upgrade script

In general, everyone assumes someone else is testing the upgrade guides and scripts.  That means it's your job to take
a quick peek every one in a while and make sure that they work.  Things to think about:

- Do we need Elasticsearch index rebuilds?
- Is there any database migration necessary?
- Are there any leftover steps in the upgrade docs from previous Opencast versions?
- Is the table of content in the guide correct?

### The  Release Schedule

Releases should happen twice a year, usually within a time span of 6 months between the cut of the previous release
branch and the final release. The release managers should create a release schedule as soon as possible, announce it on
list and publish it on the release notes page.

### Release Branch

According to the set release schedule, at one point a release branch should be cut, effectively marking a feature freeze
for a given release.  This branch is split off `develop` and should be named `r/N.x` (e.g.
`r/{{ opencast_major_version() }}.x` for the Opencast {{ opencast_major_version() }} release branch).

Example on how to create the Opencast {{ opencast_major_version() }} release branch:

0. Ensure that you have applied a replacement rule set up for github

        git config --global url."git@github.com:opencast/".insteadOf "https://github.com/opencast/"

1. Check out `develop` and make sure it has the latest state (replace `<remote>` with your remote name for the community
   repository):

        git checkout develop
        git pull <remote> develop

2. Make sure you did not modify any files. If you did, stash those changes:

        git status   # check for modified files
        git stash    # stash them if necessary

3. Create and push the new release branch:

        git checkout -b r/{{ opencast_major_version() }}.x
        git submodule foreach git checkout -b r/{{ opencast_major_version() }}.x
        sed -i 's#branch = develop#branch = r/{{ opencast_major_version() }}.x#g' .gitmodules
        git add .gitmodules
        git commit -m "Updating .gitmodules to point at the r/{{ opencast_major_version() }}.x"
        git push <remote> r/{{ opencast_major_version() }}.x
        git submodule foreach git push <remote> r/{{ opencast_major_version() }}.x

4. That is it for the release branch. Now update the versions in `develop` in preparation for the next release:

        git checkout develop
        ./mvnw versions:set -DnewVersion={{ opencast_major_version() + 1 }}-SNAPSHOT versions:commit

5. Have a look at the changes. Make sure that nothing else was modified:

        git diff
        git status | grep modified: | grep -v pom.xml | grep -v "modified content"  # this should have no output

6. If everything looks fine, commit the changes and push it to the community repository:

        git submodule foreach git add pom.xml
        git submodule foreach git commit -s -m 'Bumping pom.xml Version Nnumbers'
        git add $(git status | grep 'modified:.*pom.xml' | awk '{print $2;}')
        git add $(git submodule | cut -f 2 -d " ")
        git commit -s -m 'Bumping pom.xml Version Numbers'
        git push <remote> develop
        git submodule foreach git push origin develop

7. File a PR against the infra repo updating version numbers:

        git clone -b master git@github.com:opencast/opencast-project-infrastructure.git
        [ Update ansible-demo-machines/deploy.yml ]
        git commit -m "Updating demo machine versions due to {{ opencast_major_version() + 1 }} branch cut"
        [ Create pr ]
        [ Bug QA manager on Matrix/email ]


If you are unable to create the branches in the last three repositories, please make noise in the Matrix channel so we
can fix your permissions!


### Status of Translations

After the release branch is cut, the release managers should check if there are languages to be in- or excluded for the
upcoming release as specified by the [criteria in the localization documentation](localization.md) and notify the
community about the status of Opencast's translations if necessary.

Example discussion template for included languages:

```no-highlight
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
    https://docs.opencast.org/develop/developer/#develop/localization/#inclusion-and-exclusion-of-translations
```

Example discussion template for endangered languages:

```no-highlight
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
    https://docs.opencast.org/develop/developer/#develop/localization/#inclusion-and-exclusion-of-translations
```

A specific translation week may be announced using a discussion post
like this:

```no-highlight
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
merged down on a regular basis. Assuming, for example, that `r/{{ opencast_major_version() }}.x` is the latest release
branch, merges should happen like this:

    r/{{ opencast_major_version() - 1 }}.x → r/{{ opencast_major_version() }}.x → develop

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

        git merge <remote>/r/{{ opencast_major_version() }}.x

4. Push the updated branch into the community repository:

        git push <remote> develop


### Updating Translations

Updating the [localization translations](localization.md) is automated for existing translation files. If new files need
to be added, it is something that should happen early during the release process. If files need to be removed, this
needs to be done manually.


### Releasing

The following steps outline the necessary steps for cutting the final release, using {{ opencast_major_version() }}.0
as an example:

0. Ensure that you have applied a replacement rule set up for github

        git config --global url."git@github.com:opencast/".insteadOf "https://github.com/opencast/"

1. Switch to and update your local release branch, ensuring your local branch is up to date with the main repo.

2. Add the release notes, and update the changelog. The `create-changelog` [helper script
   ](https://github.com/opencast/helper-scripts/tree/master/release-management/create-changelog) is a convenient tool
   for this.  The script can be called a few different ways, please read the documentation and figure our yours.

        cd docs/guides/admin/docs/
        vim releasenotes.md
        python3 helper-scripts/release-management/create-changelog.py [args] >> changelog/opencast-{{ opencast_major_version() }}.md
        [ manual check that the doc looks correct ]
        git add changelog/opencast-{{ opencast_major_version() }}.md

    - Check that PRs tagged as features are all listed in the release notes, since sometimes committers miss adding the
    text files.

    - Ensure you check across the admin, editor, and studio repositories, since PRs in those repositories may not get
    picked up by the script above!

    - The release notes for a major release (x.0) should be built from the various text files in the 
   `docs/guides/admin/docs/releasenotes` directory.  the release notes for a minor release (x.y) should be a rough
   summary of the development activity between x.y and x.y-1

3. Switch to a new branch to create the release (name does not really matter):

        git checkout -b tmp-{{ opencast_major_version() }}.0
        git submodule foreach git checkout -b tmp-{{ opencast_major_version() }}.0

4. Make the version changes for the release:

        ./mvnw versions:set -DnewVersion={{ opencast_major_version() }}.0 versions:commit

5. Have a look at the changes. Make sure that nothing else was modified:

        git diff
        # The following command should yield no output:
        git status | grep modified: | grep -v pom.xml | grep -v "modified content"

6. Commit the changes and create a release tag:

        git submodule foreach git add pom.xml
        git submodule foreach git commit -S -m 'Opencast {{ opencast_major_version() }}.0'
        git add $(git status | grep 'modified:.*pom.xml' | awk '{print $2;}')
        git add $(git submodule | cut -f 2 -d " ")
        git commit -S -m 'Opencast {{ opencast_major_version() }}.0'

7. Build and test the distributions.  Start each one and make sure they boot successfully.

8. Push the tag to the community repository (you can remove the branch afterwards):

        git submodule foreach git tag -s {{ opencast_major_version() }}.0 -m 'Opencast {{ opencast_major_version() }}.0'
        git tag -s {{ opencast_major_version() }}.0 -m 'Opencast {{ opencast_major_version() }}.0'
        git submodule foreach git push origin {{ opencast_major_version() }}.0
        git push <remote> {{ opencast_major_version() }}.0
        git branch -D tmp-{{ opencast_major_version() }}.0
        git submodule foreach git branch -D tmp-{{ opencast_major_version() }}.0

9. Check the “Create new release” GitHub Actions workflow.
   It will automatically build and upload the release tarballs and create a new release draft.
   Once it is finished, review the draft, adjust the description and publish the release.

   If the workflow fails, investigate what was going wrong and either restart the workflow or create the release
   manually in the GitHub user interface.

10. In the case a x.0 release, post a release notification on [opencast.org](https://opencast.org).  You will need to
    ensure you have the appropriate permissions - talk to the QA Coordinator, or the board if you do not know how or
    have the rights.  Typically we reuse a previous major version's message, altering the version numbers, but the
    actual content is up to the release manager.


11. Check that the release is published on [Maven Central](https://repo1.maven.org/maven2/org/opencastproject/opencast-common/).
    This can take some time, and is done via [Buildbot](http://ci.opencast.org).  If in doubt, ask the QA Coordinator to
    check.  If you need to do this yourself please read the [infra documentation](infrastructure/maven-repository.md#pushing-to-maven-central).

### Appointment of Next Release Manager

After the release branch is cut, all work on `develop` is effectively the preparation for the next release. At this
point, the release managers should send an inquiry to the general development discussion group to identify volunteers
for the next release.

For that, this post template may be used:

```no-highlight
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

  https://docs.opencast.org/develop/developer/#participate/release-manager/

In the past, it has proven good practice to have two people
fill this job as co-release managers to help keep up the
process during vacation, sickness and in case of local
emergencies.

I am looking forward to your applications on list, please
voice your interest until <DATE_ROUGHLY_2_WEEKS_IN_THE_FUTURE>.
```

In many cases only a single pair of users will step forward to fill these roles.  In this case, barring any objection
from the community, these two will be selected to be release managers automatically.

In the case where more than one pair steps up, the usual process is that the second pair becomes the release managers
for the NEXT_NEXT_RELEASE.  If no agreement between the parties can be reached, a formal vote can be called using the
same mechanism used for board votes.  In this case, contact the board directly for instructions.

Once the release managers are selected the result should be announced on in the same development discussion:

As an example:

```no-highlight
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
