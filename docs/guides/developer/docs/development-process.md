Development Process
===================

This document defines rules and recommendations for Opencast development. In particular, it defines how patches can be
contributed, how they are merged and how releases are done.

*If this document does not answer all of your questions, here is how you can get further help:*

 - *Ask on the [Opencast Development List](https://groups.google.com/a/opencast.org/forum/#!forum/matterhorn)*
 - *Chat with developers on [IRC (#opencast on Freenode)](http://webchat.freenode.net/?channels=opencast)*
 - *Join our weekly technical meeting (see lists or IRC)*


Contributing Code
-----------------

Opencast sources can be found on [BitBucket](http://bitbucket.org/opencast-community). The easiest way to contribute
code to the project is by creating a pull request against the project's official repository. More details about the
structure of this repository are explained later in this guide.

### Jira and BitBucket

 - Opencast uses [Jira](https://opencast.jira.com) for tracking issues. Each pull request should be accompanied by a
   ticket in Jira. The issue identifier should also be used in the title of the pull request and the commits. E.g.:
   `MH-12345, Fixing Something Somewhere`. Creating a Jira ticket is usually the first step when fixing something.

 - Opencast uses [BitBucket](http://bitbucket.org/opencast-community) for code hosting. Please
   [fork](https://confluence.atlassian.com/bitbucket/forking-a-repository-221449527.html) the official
   repository on BitBucket to [create pull
   requests](https://confluence.atlassian.com/bitbucket/work-with-pull-requests-223220593.html) from your repository
   which will show up on the project's list of open pull requests.

 - All open pull requests are listed on the [Opencast Pull Request Filter](http://pullrequests.opencast.org). It might
   take a couple of minutes for a new pull request to show up.

### Bugfix vs Feature

Opencast distinguishes between bug fix and feature pull requests.

 - Features are *only* allowed to be merged into `develop`, which will let them automatically become part of the next
   major/minor release, given that the release branch for the next release has not been cut yet. If possible, please
   name branches containing features according to the pattern `f/MH-XXXXX-short-description`, where MH-XXXXX is the
   relevant Jira ticket.

 - Bug fixes can be merged both into `develop` and into release branches. If possible, please name branches containing
   bug fixes according to the pattern `t/MH-XXXXX-short-description`, where MH-XXXXX is the relevant Jira ticket.

### Reviews

Before a patch is merged, it needs to be reviewed by a committer. The reviewer makes sure that the patch merges
without conflicts, that it works as expected and that it does not break anything else.

If the reviewer discovers any kind of issue, he should comment on the pull request in BitBucket, so that the author can
fix the problem.

For more details about the review and merge process, have a look at [Reviewing, Merging and Declining Pull
Requests](reviewing-and-merging.md).

#### Pull Request Guidelines

When reviewing a pull request, it is always easier if the reviewer knows what the ticket is about, and has a rough idea
of what work has been done.  To this end, there are a few expectations for all pull requests:

 - For each pull request a JIRA ticket should be created
 - The JIRA ticket and JIRA ticket title should be the pull request title
 - The pull request description should contain a summary of the work done, along with reasoning for any major change
   - The JIRA ticket should contain the same information
 - For feature pull requests, accompanying documentation should be included
 - It is encouraged, but not required that the pull request have a clean commit history
 - In the case of major user interface changes, it is good practice to include screenshots of the affect sections of
   the interface
 - If you add or modify any external libraries ensure that those additions and modifications are also applied to the
   NOTICES file
 - Any actions that would be required for a version upgrade (e.g: from 3.x to 4.x) must be documented in
   docs/guides/admin/docs/upgrade.md
 - The commands `mvn clean install`, `mvn javadoc:javadoc javadoc:aggregate`, and `mvn site` should all succeed
 - The licenses of any external libraries used in the pull request comply with the [licensing rules](license.md) both
   in terms of the license itself as well as its listing in NOTICES

While a committer may accept a patch even if it does not meet these expectations, it is encouraged that anyone filing
a pull request ensures that they meet these expectations.


Git Repository Branching Model
------------------------------

While the Opencast repository and branching model is inspired by
[GitFlow](http://nvie.com/posts/a-successful-git-branching-model/), there have been some distinct changes to how release
branches are used and releases are tagged. The purpose of this is mainly to support multiple, simultaneous versions and
maintenance releases.

Swift overview:

 - The `develop` branch represents the latest state of development. Features may be merged into this branch and into
   this branch only. Release branches are branched off from `develop`. It is basically the preparation for the next big
   release at all times.
 - The release branches are named `r/<a>.<b>.x` (e.g. `r/1.6.x`). They are the latest state of development for a
   specific release. Only bug fixes and no features may be added to these branches. All beta versions, release
   candidates and final releases are made from these branches. The branch lives on as long as there may be additional
   maintenance releases for a given version.
 - Git tags are created to indicate official releases. These may be:
    - `x.y.z-betaX` marks a beta release. This is usually a version which may still have bugs but is good enough for
      testing, so that further issues or bugs can be identified before an actual release.
    - `x.y.z-rcX` marks a release candidate. This is a version that seems to be ready to be released as a final
      version. It will become the final version if testing does not reveal any severe issues.
    - `x.y.z` marks a final release.


To get a closer look at the branching model, let us consider a simple example with a single release:


    develop ---*----*----*------*------- ... -----------*-->
                \       /      /                       /
        r/1.6.x  *-----*------*-------------*---------*----- ... ---*-->
                               \             \         \             \
                    1.6.0-beta1 *   1.6.0-rc1 *   1.6.0 *       1.6.1 *

As described above, `develop` is the branch used for preparing the next version. At some point marked in the release
schedule, the release branch is cut from `develop`. This action also marks the feature freeze for such Opencast version,
i.e. the moment when no new features will be included in that specific version. This is because all the new features
must be merged only into the `develop` branch; therefore, the release branches (such as `r/1.6.x` in our example) can
only contain features that were merged before the branch was forked off. Any features merged after the creation of the
release branch can only make it into the next release, but not into this one.

After the release branch is cut, the development on the `develop` branch may continue as before. Features can (and
should) be merged without waiting for the next version to be released. Thus, the creation of a release branch also marks
the beginning of the development for the next version.

In contrast to that, only bug fixes may be merged into the release branch. At this point, the code contained in this
branch should be tested, so that bugs can be identified and fixed. The release manager can tag different beta versions
during the QA process (such as `1.6.0-beta1`) to mark the code evolution as bug fixes are merged. Once the branch status
seems to be stable enough to be released, a release candidate (RC) is tagged and tested (`1.6.0-rc1`). New RCs can be
tagged as long as new issues are found and fixed. When no severe issues are found, the final release is tagged.

During the whole process the release manager will regularly merge back the release branch into `develop` so that bug
fixes from the release branch will automatically become part of `develop` and the next Opencast version, without having
to create an additional pull request. This is continued until the release branch for the next version is cut.

The releases themselves are not part of the release branch. Instead, the release manager branches off, makes the
necessary changes to the pom files (and possibly the UI) and creates a separately tagged commit.

Finally, after a release is done, more bug fixes may be added to the release branch. The release manager should identify
if there are enough commits to be put into a maintenance release.

Even after an Opencast version has been released, more bugs may be found and fixes for them merged into the release
branch. When the release manager considers that the number or importance of such bug fixes is sufficient, he may decide
to create a new maintenance release. The version `1.6.1` above is an example of that.

The branching structure for multiple versions does not look much more complicated:

    develop  -----*---------*----*------------------------*------>
                   \       /    / \                      /
            r/1.5.x *-----*----*------------*--->       /
                           \        \        \         /
                      1.5.0 *        \  1.5.1 *       /
                                      \              /
                               r/1.6.x *------------*----->
                                                     \
                                                1.6.0 *

As you can see, the same principle, the same structure and the same rules apply. The only noteworthy thing is that,
after a new release branch is cut, the old release branch will not be merged back into `develop`. This means that when
a bug fix is relevant to several releases, a different pull request should be created for each of the release branches.


Release Process
---------------

As indicated above, the release cycle of a new Opencast version starts when a release branch is cut. The new features
merged into `develop` afterwards will be part of the next version, but not the one just cut.

This is why the position of release manager for the next Opencast version should be assigned at this point. The current
release manager should therefore ask for volunteers in the mailing lists. For more details about the rights and
responsibilities of a release manager, please have a look at the [Release Manager Guide](release-manager.md).

### Preparations

The first phase of the release consists of adding new features and defining the release schedule. It is the duty of the
release manager to orchestrate this. This does not necessarily mean that release managers merge or review pull requests,
but that they talk to developers and ensure the merge process is driven forward.

#### Release Schedule

Releases should happen twice a year, usually within a time span of 9.5 months between the cut of the previous release
branch and the final release. The release manager should create a release schedule as soon as possible, identifying when
the release branch is cut and when the final release will happen. Additionally, he should coordinate with the QA manager
to identify phases for internal and public testing.

Usually, a release schedule will look like this:

|Date                          |What is happening
|------------------------------|-------------------------------------------------
|April 6th                     |Feature Freeze *(release branch is cut)*
|April 6th - 24th              |Internal QA and bug fixing phase
|&nbsp; *April 11th - 17th*    |Review Test Cases *(handles by a dedicated team)*
|&nbsp; *April 18th - 24th*    |Documentation Review
|April 25th - May 15th         |Public QA phase
|May 15th - June 1st           |Additional bug fixing phase
|&nbsp; *May 25th - June 1st*  |Translation week *(encourage translators to do their work)*
|June 2nd - June 12th          |Final QA phase *(checking release readiness)*
|June 15th                     |Final Release


### Release Branch

The release branch is created from `develop`. The release branch is named `r/A.B.x` (e.g. `r/2.1.x`) to indicate that it
is the origin of all releases with the major and minor version of `A.B`. The creation of the release branch marks the
feature freeze for a given version, as no more features can be merged into a release branch.

To ensure that all fixes that go into the release branch will become part of `develop` (and thus part of the next version
of Opencast) with a minimum amount of work, the release manager will merge the release branch into `develop` on a
regular basis. He may request assistance from certain developers in case of merge conflicts. This process continues until
the next release branch is cut.

### Tags

Git tags are used to mark explicit Opencast versions or releases. Here is how a release should look like in the history:

    r/A.B.x  ------------(A)---->
                           \
               A.B.0-beta1 (B)

To create a version based on a given state of the release branch (commit A), the release manager will branch off from
this commit, make the necessary version changes to all `pom.xml` files and to the UI and create a commit and a *signed*
git tag on this commit. He would then push the commit and the tag (not the branch) to the community repository.

For more details about how to create a release, have a look at the [Release Manager Guide](release-manager.md).

#### Beta Versions/Release Candidates

A beta release (`A.B.C-betaX`) should be cut before the public QA phase. It indicates a specific version of Opencast for
users to test. It is expected to still have bugs. Beta releases should continue until all bugs with a severity of
`Blocker` have been fixed.

If the code seems to be ready for a release, a *release candidate* (`A.B.C-rcX`) should be cut for final testing. The
commit from which a release candidate is created is expected to become the final release if no severe bugs are found.

#### Final Release

Once a release candidate seems to be stable during the QA phase (no issues left marked as `Blocker`), the release manager
will propose this release candidate to become the final release. If the proposal is approved (i.e. no serious bugs are
found before the proposal deadline is met), the final release is then created from the same commit the
release candidate was cut from.

### Maintenance Releases

After a final release, additional issues may show up. These issues may be fixed on the ongoing release branch and at
some point released as maintenance release.

There is usually no release schedule for maintenance releases. It is up to the release manager to decide when it is
worthwhile to create a maintenance release with the fixes for bugs affecting a given release. He would then announce his
intention to create such a release, cut a release candidate and, should no severe issues with this candidate show up, cut
the maintenance release.

Quality Assurance
-----------------

As any piece of software, Opencast may contain bugs. It is the duty of the whole community to identify these bugs,
report them and possibly fix them to improve Opencast as product.

Additionally, before releasing a new version of Opencast, the current release manager and quality assurance manager will
coordinate test phases dedicated to new releases in order to identify possible problems ahead of time. The whole
community will be requested to participate in this testing.

### Reporting Bugs

If you identify any bugs, please report them! To do that, register yourself in the [Opencast
Jira](https://opencast.jira.com) and create a new ticket. Please describe in detail how to reproduce the problem and
especially set the *Affects Version* and "Fix Version", where *Fix Version* should be the next Opencast release.

If in doubt of any items in the ticket, please assign it for review to either the current release manager or to the
quality assurance manager. They will check the issue fields and adjust *fix version*, *severity*, etc. if necessary.

#### Security Issues

If you discover a problem that has severe implications for system security, please do not publish this information on
list. Instead, send a report of the problem to *security@opencast.org*. The message will be forwarded to the private
committers list, where the issue will be discussed. Once a patch for the problem is ready, a security notice will be
released along with it.

### Unit Tests

All Opencast modules should have built-in unit tests to check that they are actually doing what they are supposed to do
and that code patches do not break the existing functionality. These tests are automatically run whenever the project is
built. If building repeatedly fails due to test failures, then something is most likely wrong. Please report this as a
severe bug.

### User Tests

Before each major release, the release and quality assurance managers will ask the whole community to participate in
the execution of a set of manual tests. These tests are designed to check that important functionalities of Opencast
work as expected even if users are in slightly different environments or choose different methods to achieve a certain
goal.

Such a call for participation will usually be raised both on the lists, the technical and the adopters meeting.  If it
is possible for you to participate, please do so. Identifying possible problems early will immensely benefit the release
process.

### Test Server

Some institutions provide public testing infrastructure for Opencast. Use them to try out the most recent development
version of Opencast. They are meant for testing. Do not fear to break them.  If you manage to do it, please contact the
QA manager or send a notice to the list.

Remember that they are usually not running a stable version of Opencast but the latest development release (beta version
or release candidate) instead. If you discover any bugs on these systems, please take a minute to report them.
