Reviewing, Merging and Declining Pull Requests
==============================================

Before a patch is merged into an official branch, it needs to be reviewed by a committer. During a
review, the reviewer tries to make sure that the patch merges without conflicts, that it works as expected and that
it does not break anything else.

If the reviewer discovers any kind of issue, he should leave a comment in the pull request view of GitHub and let the
contributor fix the problem. Reviewer and contributor should work together to fix any problem with the pull requests
before it is ready to go into the codebase.

Reviewing Rules
-----

* Reviews and merges need to be done by committers.
* Reviewers should come from a different institution than the contributor.
* Pull requests for bug fixes or documentation may be reviewed and merged out of order.
* Feature pull requests have to be merged in order unless their contributor or reviewer decide to temporarily skip it.
  Such a decision has to be justified by the existence of unresolved issues in that particular pull request.
* There is a so-called “merge ticket” in Jira for `develop` and for each release branch. Committers must not merge code
  into these branches unless they are the assignee of the corresponding ticket.
* By default, the merge ticket for `develop` should be always assigned to the reviewer of the next feature pull request
  to be merged.
* The merge ticket for `develop`, can be temporarily taken by anyone for merging a bug fix after coordinating this with
  the current assignee of the ticket
* No features can be merged in the release branches, so the merge tickets corresponding to a release branch will be
  unassigned by default. Reviewers merging bug fixes must assign the ticket to themselves before doing so, and then
  leave it unassigned after finishing.
* The release ticket should not be unassigned for longer than 72 hours in which case the release manager should assign
  it to the reviewer of the next pull request in line.
* After taking up a review (or being assigned to one), a basic review has to be done within the following 14 days for a
  bug fix or 21 days for a feature. A basic review means that either some issues should be pointed out or the pull
  request should be approved.
* No pull request shall be without a reviewer for more than 14 days. If no committer is willing to take up the review
  on their own, the review will be assigned.
* Any committer may merge an arbitrary set of *approved* pull requests, even if he is not the official reviewer, given
  that:
    * The committer sends an announcement to the development list 24 hours in advance, including a list of the pull
      requests to be merged and no other committer objects to that.
    * The committer checks that all the patches are working (which basically means to review the patch set as a whole).
* A reviewer may decline a pull request if issues were pointed out but were neither fixed nor discussed for more than
  14 days. It is generally suggested to try to contact the contributor before declining the pull request and to
  additionally bring the problem up at the technical meeting.



Reviewing a Pull Request
------------------------

There is no list of things you are required to do as reviewer of a pull request. Our primary goals in doing reviews for
all pull requests are to ensure that:

* There are no bugs in the pull request
    * The feature works as advertised
    * It does not break any other features
* There are no obvious legal problems
    * [Licenses seem to be fine](license.md)
    * Licenses are added to the `NOTICES` file
* Feature documentation is in place
* An upgrade path exists (usually relevant for database changes)


The exact review process heavily depends on the type, size and purpose of the pull request. For example, it does not
make sense to run the unit or integration tests for pull requests containing documentation only. Instead, reviewers
should run `mkdocs` to build the documentation.

Here are some things a reviewer should usually do:

* Check if code looks sensible (e.g. no nonsensical files, no obvious formatting errors, no large binary files, …)
* Perform the merge locally to make sure there are no conflicts
* Build Opencast with the `-P dev,dist` flag (build all modules) and tests enabled (default). If you have some more time
  (e.g. lunch break), clear your local Maven repository (usually `~/.m2`) before starting the build process
* Locally run Opencast to make sure it still works after the merge
* Make sure the issue has been fixed (as described in the pull request and/or the Jira ticket) or the feature does
  what it is supposed to
* Check and build the documentation
