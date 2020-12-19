Development Process
===================

[TOC]

This document defines rules and recommendations for Opencast development. In particular, it defines how patches can be
contributed, how they are merged and how releases are done.

*If this document does not answer all of your questions, here is how you can get further help:*

* *Ask on the [Opencast Development List](https://groups.google.com/a/opencast.org/forum/#!forum/dev)*
* *Chat with developers on [IRC (#opencast on Freenode)](http://webchat.freenode.net/?channels=opencast)*
* *Join our weekly technical meeting (see lists or IRC)*


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

### Bugfix vs Feature

Opencast distinguishes between bug fix and feature pull requests.

* Features are *only* allowed to be merged into `develop`, which will let them automatically become part of the next
  major/minor release, given that the release branch for the next release has not been cut yet.

* Bug fixes can be merged both into `develop` and into release branches.


### Reviews

Before a patch is merged, it needs to be reviewed. The reviewer tries to make sure that the patch merges without
conflicts, that it works as expected and that it does not break anything else.

If the reviewer discovers any kind of issue, he should comment on the pull request in GitHub, so that the author can
fix the problem.

For more details about the review and merge process, have a look at [Reviewing, Merging and Declining Pull
Requests](reviewing-and-merging.md).

#### Pull Request Guidelines

When reviewing a pull request, it is always easier if the reviewer knows what the ticket is about, and has a rough idea
of what work has been done. To this end, there are a few expectations for all pull requests:

* The GitHub issue  title should match the pull request title
* The pull request description should contain a summary of the work done, along with reasoning for any major change
    * The GitHub issue should contain the same information
* For feature pull requests, accompanying documentation should be included
* The pull request should have a clean commit history
* In the case of major user interface changes, it is good practice to include screenshots of the change
* Any actions that would be required for a version upgrade (e.g: from 3.x to 4.x) must be documented in
  `docs/guides/admin/docs/upgrade.md`
* The commands `mvn clean install`, `mvn javadoc:javadoc javadoc:aggregate`, and `mvn site` should all succeed
* The licenses of any external libraries used in the pull request comply with the [licensing rules](license.md) both
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

* The `develop` branch represents the latest state of development. Features may be merged into this branch and into
  this branch only. Release branches are branched off from `develop`. It is basically the preparation for the next big
  release at all times.
* The release branches are named `r/<a>.x` (e.g. `r/6.x`). They are the latest state of development for a specific
  release. Only bug fixes and no features may be added to these branches. All releases are created from these branches.
  The branches live on as long as there may be additional maintenance releases for a given version.
* Git tags in the form of `a.b` are created to indicate official releases.


To get a closer look at the branching model, let us consider a simple example with a single release:


```graphviz dot branching-simple.png

/**
    develop ---*----*----*------*------- ... -----------*-->
                \       /      /                       /
          r/6.x  *-----*------*-------------*---------*----- ... ---*-->
                               \             \         \             \
                            6.0 *         6.1 *     6.2 *         6.3 *
**/

digraph G {
  rankdir="LR";
  bgcolor="transparent";
  node[width=0.1, height=0.1, shape=point,fontsize=8.0,color=black,fontcolor=black];
  edge[weight=2, arrowhead=none,color=black];
  node[group=develop];
  dbegin -> d1 -> d2 -> d3 -> d4 -> d5 -> d6 -> d7 -> d8 -> d9;
  node[group=releasebranch];
  edge[color=transparent];
  rbegin -> r1;
  edge[color=black];
  r1 -> r2 -> r3 -> r4 -> r5 -> r6;
  d1 -> r1;


  // releases
  node[group=released]
  edge[color=gray];
  release1[shape=point, xlabel="7.0", color=gray]
  r3 -> release1;
  release2[shape=point, xlabel="7.1", color=gray]
  r5 -> release2;

  // end arrows
  edge[arrowhead=normal, color=black];
  dend,rend[shape=none, label=""];
  d9 -> dend;
  r6 -> rend;

  // branch names
  dbegin[shape=plaintext,label=develop];
  rbegin[shape=plain,label="r/7.x"];

  // merge backs
  edge[style=dashed, arrowhead=none, color=black];
  r2 -> d4;
  r5 -> d7;
}
```

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

```graphviz dot branching-two-versions.png

/**
    develop ---*-----*-----*------*-----*- ... -----------*------*------*---->
                \         /      /       \               /             /
                 \       /      /   r/7.x *---*---*-----*----- ... ---*--->
                  \     /      /             /         /
            r/6.x  *---*------*------*------*------*--*----- ... ---*-->
                              \              \         \             \
                           6.0 *          6.1 *     6.2 *         6.3 *
**/

digraph G {
  rankdir="LR";
  bgcolor="transparent";
  node[width=0.1, height=0.1, shape=point,fontsize=8.0,color=black,fontcolor=black];
  edge[weight=2, arrowhead=none,color=black];
  node[group=develop];
  dbegin -> d1 -> d2 -> d3 -> d4 -> d5 -> d6 -> d7 -> d8 -> d9 -> d10 -> d11;
  node[group=releasebranch];
  edge[color=transparent];
  rbegin -> r1;
  edge[color=black];
  r1 -> r2 -> r3 -> r4 -> r5 -> r6;
  d1 -> r1;

  node[group=releasebranch2];
  edge[color=transparent];
  r2begin -> r21;
  edge[color=black];
  r21 -> r22 -> r23 -> r24 -> r25;
  d5 -> r21;


  // releases
  node[group=released]
  edge[color=gray];
  release1[shape=point, xlabel="7.0", color=gray]
  r3 -> release1;
  release2[shape=point, xlabel="7.1", color=gray]
  r5 -> release2;
  release3[shape=point, xlabel="8.0", color=gray]
  r24 -> release3;

  // end arrows
  edge[arrowhead=normal, color=black];
  dend,rend,r2end[shape=none, label=""];
  d11 -> dend;
  r6 -> rend;
  r25 -> r2end;

  // branch names
  dbegin[shape=plaintext,label=develop];
  rbegin[shape=plain,label="r/7.x"];
  r2begin[shape=plain,label="r/8.x"];

  // merge backs
  edge[style=dashed, arrowhead=none, color=black];
  r2 -> d4;
  r5 -> r22;
  r23 -> d9;
  r25 -> d11;
}
```

Mostly, this is just the same as the simpler model from before. The branches exist separately from each other and only
interact through merges from older to newer versions so that bug fixes from a release branch will automatically become
part of the next Opencast versions (and `develop`), without having to create additional pull requests.

For example, a pull request may be merged into `r/7.x`, `r/7.x` will then be merged into `develop` or, if it already
exists, `r/8.x` and from there into `develop`. That way patches bubble through all newer versions and finally end up in
`develop`.


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

|Date                     |Phase
|-------------------------|------------------------------------------
|April 1st                |Feature Freeze
|May 6th - May 12th       |Translation week
|May 13th - May 26th      |Public QA phase
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

```graphviz dot branching-tags.png

/**
    r/7.x  ------------(A)---->
                         \
                     7.0 (B)
**/

digraph G {
  rankdir="LR";
  bgcolor="transparent";
  node[shape=circle, fixedsize=true, width=0.2, fontsize=8.0, fontcolor=black, group=branch, fontname=helvetica];
  edge[weight=2, arrowhead=none, color=black];
  begin -> A;

  // end arrows
  end[shape=none, label=""];
  A -> end [arrowhead=normal, color=black, minlen=3];


  // releases
  B[group=release, xlabel="7.0", color=gray, fontcolor=gray]
  A -> B [color=gray];

  // branch names
  begin[shape=plaintext, label="r/7.x", fixedsize=false];
}
```

To create a version based on a given state of the release branch (commit `A`), the release manager will branch off from
this commit, make the necessary version changes to all `pom.xml` files and create a commit which is then finally tagged.
This tag is then pushed to the community repository.

For more details about how to create a release, have a look at the [Release Manager Guide](release-manager.md).

### Maintenance Releases

After a final release, additional issues may show up. These issues may be fixed on the ongoing release branch and at
some point released as maintenance release.

There is usually no release schedule for maintenance releases. It is up to the release manager to decide when it is
worthwhile to create a maintenance release with the fixes for bugs affecting a given release.

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
version of Opencast. They are meant for testing. Do not fear to break them. They are meant for testing.

For a list of test servers, take a look at the [infrastructure documentation](infrastructure/index.md).
