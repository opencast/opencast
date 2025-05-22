Development Process
===================

[TOC]

This document defines rules and recommendations for Opencast development. In particular, it defines how patches can be
contributed, how they are merged and how releases are done.

*If this document does not answer all of your questions, here is how you can get further help:*

* *Ask on the [Opencast Development List](https://groups.google.com/a/opencast.org/forum/#!forum/dev)*
* *Chat with developers on [Matrix (#opencast-community)](https://app.element.io/#/room/#opencast-community:matrix.org)*
* *Join our weekly technical meeting (see lists or Matrix)*


Contributing Code
-----------------

Opencast sources can be found on [GitHub](https://github.com/opencast). The easiest way to contribute code to the
project is by creating a pull request against the project's official repository. More details about the structure of
this repository are explained later in this guide.

### GitHub

* Opencast uses [GitHub](https://github.com/opencast/opencast/issues) for tracking issues. Each pull request should be
  accompanied by a ticket in GitHub unless it is a very small fix. The issue identifier should also be in the
  description of the pull request, which will automatically close the issue (if any) when the PR is merged.  See
  [here](https://help.github.com/en/articles/closing-issues-using-keywords) for more details. Creating a GitHub issue
  is usually the first step when fixing something.

* Opencast uses [GitHub](https://github.com/opencast) for code hosting. Please
  [fork](https://help.github.com/articles/fork-a-repo/) the [official repository](https://github.com/opencast/opencast)
  on GitHub to [create pull requests](https://help.github.com/articles/creating-a-pull-request/) from your repository
  which will show up on the project's list of open pull requests.


### Acceptance Criteria for Patches in Different Versions

Updates between minor versions should be as smooth as possible and should usually not need manual intervention.
That is why patches may only be accepted into releases branches (`r/?.x`) if they meet the following criteria:

* Patches must not modify existing database tables
* Patches must not modify the indexes or otherwise cause re-indexing
* Patches must not modify existing translation keys
* Patches must work with the same configuration within a major version

Patches which do not meet these criteria should target the branch `develop` to become part of the next major version.

Note: Patches adding features should target the current stable release (`r/{{ opencast_major_version }}.x`), or
`develop`, and are strongly discouraged from targetting the legacy release.  Features going into the legacy release
will need a good reason, and must be highly self contained.

To determine the acceptance of patches, all pull requests will be discussed in the technical meeting.
This protects against inclusion of controversial changes with no broader consent among committers.


### Reviews

Before a patch is merged, it needs to be reviewed. The reviewer tries to make sure that the patch merges without
conflicts, that it works as expected and that it does not break anything else.

If the reviewer discovers any kind of issue, he should comment on the pull request in GitHub, so that the author can
fix the problem.


#### Pull Request Guidelines

When reviewing a pull request, it is always easier if the reviewer knows what the ticket is about, and has a rough idea
of what work has been done. To this end, there are a few expectations for all pull requests:

* The GitHub issue  title should match the pull request title
* The pull request description should contain a summary of the work done, along with reasoning for any major change
    * The GitHub issue should contain the same information
* Pull request should include appropriate documentation
* The pull request should have a clean commit history
* In the case of major user interface changes, it is good practice to include screenshots of the change
* Any actions that would be required for a version upgrade (e.g: from 3.x to 4.x) must be documented in
  `docs/guides/admin/docs/upgrade.md`
* New features require a release note in `docs/guides/admin/releasenotes` of at least one line describing the change
* The commands `./mvnw clean install`, `./mvnw javadoc:javadoc javadoc:aggregate`, and `./mvnw site` should all succeed
* The licenses of any external libraries used in the pull request comply with the [licensing rules](../license.md) both
  in terms of the license itself as well as its listing in NOTICES

Some changes require special attention:

Folder                         | Description
:------------------------------|------------
etc/listproviders              | Changes here might need to be reflected in the static mockup data for the Admin UI facade found in modules/admin-ui/src/test/resources/app/admin-ng/resources
modules/admin-ui/src/main/java | In case the interface of the Admin UI facade changes, those changes need to be also reflected in the static mockup data for the Admin UI facade found in modules/admin-ui/src/test/resources/app.

While a committer may accept a patch even if it does not meet these expectations, it is encouraged that anyone filing
a pull request ensures that they meet these expectations.

#### Merging Pull Requests

After a pull request has received at least one approving review and passes the automated tests, it is ready for merging.
Only a committer can perform a merge, so if a reviewed pull request has not yet received attention from a committer
feel free to contact one.

There are a couple of rules that committers must follow when merging pull requests. These are:

* A pull request requires at least one approving review before merging.
    * More reviews are always welcome.
* A pull request must be approved at the weekly technical meeting before merging (visit https://docs.opencast.org/ for
  the time and place of the technical meeting).
* Reviewing or merging your own pull requests is strongly discouraged, but technically allowed.
    * It is advised to be pragmatic and only do so if necessary.

#### Automatically closing issues when a PR is merged

Our pull request template wants you to "close an accompanying issue."
This can be done as per the [GitHub documentation](https://help.github.com/en/articles/closing-issues-using-keywords)
by using one of several magic keywords in front of a valid issue number,
either in the pull request description, or in any of the commit messages
of the commits you want to merge. For example:

> This PR **fixes #1234**.

A word of caution: due to our [branching model](#git-repository-branching-model)
this might not always work as expected. GitHub only recognizes
the magic words when acting on the default branch of the repository,
which in our case is `develop`. Issues mentioned in descriptions
of PRs targeting `develop` or in any message of a commit that lands
in `develop` will be automatically closed.

Thus, if you are submitting a PR **not** targeting `develop`, and you
want to use this feature, you **have to** mention the magic words
in a commit message. Tne PR description does not work in this case.
And even then, the issue will only be closed, once your merged commits
reach `develop` by our forward merging process.

Mentioning related issues in the PR description **in addition** to
the commit message(-s) might of course still be useful for reviewers!


Reviewing Code
--------------

Reviewing pull requests is as important as creating them, as pull requests cannot be merged without at least one
approving review. Furthermore, the more people review a pull request the more likely it is that potential issues
are found early, saving time and money down the line.

This section intends to give guidelines on what to look for when reviewing a pull request. A review does not need
to cover all of these, do as much as you can and then (at least roughly) write in the review what you looked at. Also if
you found an issue with one of the guidelines, e.g. the description is not making any sense, it is completely okay to
request a change to the description and holding of on reviewing other parts of the PR until the description is fixed.

While a pull requests of course contains changes to the Opencast code base, it also consists of its trappings. Usually
it makes sense to look at those first.

### Trappings

#### Title

Was a sensible title chosen? Does the title serve as good identifier for the pull request? A bad title might be
"LTI bug". A good title might be "Fix a bug where LTI users can not play videos".

#### Description

Was a sensible description chosen?

- Does the description detail the goal of the pull request? A pull request should have one clear goal. If there are
  multiple goals, it is usually best to split these among multiple pull requests.
- Would the stated goal of the pull request improve the Opencast codebase, or should it be rejected outright? Fixing a 
  bug usually improves the code, adding another video player might not.
- Does the description explain how to test the pull request, e.g. is certain configuration necessary?

#### Issues

If the pull request fixes an existing issue on GitHub, is it linked in the pull request? If one or more issues are linked
in the pull request, does the pull request actually address them?

#### Labels

Is the pull request labelled? Is it labelled sensibly?

### Code base changes

#### Hygene

A pull request should have one clear goal. If there are  multiple goals, it is usually best to split these among
multiple pull requests.

Does the pull request have a sane commit tree?

- The tree should be easy to follow, not needlessly complex.
- Commits should have sensible titles and descriptions.
- Merge commits should be avoided, suggest using rebasing instead.

Does the pull request target the right branch? Usually

- Major features go into develop
- Minor features go into stable
- Fixes go into legacy/stable

#### Documentation

- Not necessary for every kind of pull request, but required for new features and major changes to existing behaviour.
- A mention should also be added to the release notes.
- The documentation should be legible and sensible. It should integrate with the surrounding documentation.

#### Functionality

Do the changes actually do what the description promises?

Proofreading: Read through the code and try to think it through.

- What if this line of code throws an exception? What if the function is given bad parameters?
- How computationally expensive is this loop?
- Is the logging constructed so that it will be helpful? Are translations worded well?

Test the code by compiling and running it. Try to test it in ways that won't be caught by the automated tests. Test it
in a distributed Opencast setup if possible.

#### Tests

Does the code require (unit) testing? Are the given tests sensible? Are there any test cases missing?

### Video guides

The GitHub web interface is ever-changing. If you are looking for help with navigating the website, check the
[official GitHub documentation](https://docs.github.com/de/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/reviewing-proposed-changes-in-a-pull-request).

If videos are more your style:

- (English) [Talk from 2021 Opencast Summit](https://video.ethz.ch/events/opencast/2021/graz/0014efff-ff04-414c-876c-7a7eeb66122b.html),
  slide presentation by Greg on how he does things.
- (German) [Talk from 2020 Opencast D/A/CH](https://video.ethz.ch/events/opencast/2020/innsbruck/58a02ce8-1bf6-4b2a-b220-faf65931efbc.html),
  shows how to navigate the Opencast GitHub website for beginners. Starts at minute 30.
- (English) [Talk from 2024 Opencast Summit](https://video.ethz.ch/events/opencast/2024/zaragoza/b12f4344-12db-4d1e-8997-c8dd3832a462.html),
  5 minutes of motivation for non-technical reviewers.


Git Repository Branching Model
------------------------------

While the Opencast repository and branching model is inspired by
[GitFlow](http://nvie.com/posts/a-successful-git-branching-model/), there have been some distinct changes to how release
branches are used and releases are tagged. The purpose of this is mainly to support multiple, simultaneous versions and
maintenance releases.

Swift overview:

* The `develop` branch represents the latest state of development. Features may be merged into this branch and into
  this branch only. Release branches are branched off from `develop`. It is basically the preparation for the next big
  release at all times.
* The release branches are named `r/<a>.x` (e.g. `r/6.x`). They are the latest state of development for a specific
  major version.  All minor releases are created from these branches.  The branches live on as long as there may be
  additional maintenance releases for a given version.
* Git tags in the form of `a.b` are created to indicate official releases.


To get a closer look at the branching model, let us consider a simple example with a single release:

![Git branching model with a single versions](../img/git-branching-model-simple.svg)

As described above, `develop` is the branch used for preparing the next version. At some point marked in the release
schedule, the release branch is cut from `develop`. This action also marks the feature freeze for that version since
features may be merged only into the `develop` branch.

After the release branch is cut, the development on the `develop` branch may continue as before. Features can (and
should) be merged without waiting for the next version to be released. Thus, the creation of a release branch also marks
the beginning of the development for the next version.

In contrast to that, only bug fixes may be merged into the release branch. This branch should be tested with care, so
that bugs can be identified and fixed before the release.

During the whole process the release manager will regularly merge back the release branch into `develop` or, if
existent, the next active release branch.

The releases themselves are not part of the release branch. Instead, the release manager branches off, makes the
necessary changes to the pom files (and possibly the UI) and creates a separately tagged commit.

Finally, after a release is done, more bug fixes may be added to the release branch. The release manager should identify
if there are enough commits to be put into a maintenance release.

Even after an Opencast version has been released, more bugs may be found and fixes for them merged into the release
branch. When the release manager considers that the number or importance of such bug fixes is sufficient, he may decide
to create a new maintenance release. The version `6.1` above is an example of that.

With Opencast supporting two major releases, you may find not one, but up to three active release branches.

![Git branching model with two versions](../img/git-branching-model-two-versions.svg)


Mostly, this is just the same as the simpler model from before. The branches exist separately from each other and only
interact through merges from older to newer versions so that bug fixes from a release branch will automatically become
part of the next Opencast versions (and `develop`), without having to create additional pull requests.

For example, a pull request may be merged into `r/7.x`, `r/7.x` will then be merged into `develop` or, if it already
exists, `r/8.x` and from there into `develop`. That way patches bubble through all newer versions and finally end up in
`develop`.


Release Process
---------------

As indicated above, the release cycle of a new Opencast version starts when a release branch is cut. Patches merged into
`develop` after the cut will be part of the next version, but not the one just cut.

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

|Date                     |Phase
|-------------------------|------------------------------------------
|May 15th                 |Feature Freeze
|May 24th                 |Translation week
|May 31st                 |Public QA phase
|June 15th                |Release of Opencast 7.0


### Release Branch

The release branch is created from `develop`. The release branch is named `r/A.x` (e.g. `r/7.x`) to indicate that it is
the origin of all releases with the major version of `A`. The creation of the release branch marks the feature freeze
for a given version, as no more features can be merged into a release branch.

To ensure that all fixes that go into the release branch will become part of `develop` (and thus part of the next version
of Opencast) with a minimum amount of work, the release manager will merge the release branch into `develop` on a
regular basis. He may request assistance from certain developers in case of merge conflicts. This process continues until
the next release branch is cut.

### Tags

Git tags are used to mark Opencast releases. Here is how a release looks like in the history:

![Opencast version tag in git](../img/git-version-tag.svg)

To create a version based on a given state of the release branch (commit `A`), the release manager will branch off from
this commit, make the necessary version changes to all `pom.xml` files and create a commit which is then finally tagged.
This tag is then pushed to the community repository.

For more details about how to create a release, have a look at the [Release Manager Guide](release-manager.md).

### Maintenance Releases

After a final release, additional issues may show up. These issues may be fixed on the ongoing release branch and at
some point released as maintenance release.

Maintenance releases will be cut monthly for the latest stable release. For legacy releases, it is up to the release
manager to decide when it is worthwhile to make the cut.

Quality Assurance
-----------------

As any piece of software, Opencast may contain bugs. It is the duty of the whole community to identify these bugs,
report them and possibly fix them to improve Opencast as product.

Additionally, before releasing a new version of Opencast, the current release manager and quality assurance manager will
coordinate test phases dedicated to new releases in order to identify possible problems ahead of time. The whole
community will be requested to participate in this testing.

### Reporting Bugs

If you identify any bugs, please [report them on Github](https://github.com/opencast/opencast/issues)!. Please make
sure to  describe in detail how to reproduce the problem, and which version of Opencast you are experiencing the issue
on.

#### Security Issues

Details of how our security issues are handled can be found [here](security-issues.md)

### Unit Tests

All Opencast modules should have built-in unit tests to check that they are actually doing what they are supposed to do
and that code patches do not break the existing functionality. These tests are automatically run whenever the project is
built. If building repeatedly fails due to test failures, then something is most likely wrong. Please report this as a
severe bug.

For a guide on how to write unit tests, see [Writing Unit Tests](https://video.ethz.ch/events/opencast/2023/berlin/b4861f53-738b-40fb-a1ab-ec4726eec6bd.html),
a talk how to write (good!) unit tests.

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
version of Opencast. They are meant for testing. Do not fear to break them. They are meant for testing.

For a list of test servers, take a look at the [infrastructure documentation](infrastructure/index.md).
