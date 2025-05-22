# Opencast Security Issue Process

One of the most sensitive things an Opencast developer can work on is a security issue.  We typically do (almost)
everything in public, with pull requests reviewed publicly as well.  Security issues, on the other hand, must be kept
private until they are fully finished.

---

## For Adopters/Reporters/Users

You have found something you think is security relevant.  Send an email with the details to `security@opencast.org`,
and wait.  Do *not* file an issue in the general project security tracker.  One of a thre things will happen:

 - We will acknowledge the issue, and reploy with a security issue ID.  You may be invited to participate on the issue,
   it will depend on the severity and sensitivity of the issue.
 - You will be asked for more information and details about the issue and your configuration.
 - You will be asked to file the issue in the public issue tracker if we feel it is not a security issue.

At this point, unless you have development experience, you are done and no further action from you will be required.
Security issues are not typically simple to solve, so do not expect an immediate fix, depending on complexity.

---

## For Developers/Committers

### Initial Reports

Many times it will be an Opencast developer who will discover a security issue, but occasionally users or external
security researchers will report the issue first to the `security@opencast.org`.  Regardless of who reported it, reply
 CCing `committers@opencast.org` acknolwedging the issue and request more information if appropriate.  Make sure to
request the reporter's GitHub ID, or other contact information!

At the same time create the draft security advisory in the `Security->View Security Advisories` tab at the same level
as the PRs.  We usually copy the original report into the Description of the advisory and then edit it out later when
the fix is finalized.  Don't stress about being perfect, all fields here are editable.

### The Security Advisory

Once created, the draft security advisory remains private to repository owners and committers.  Now may also be a good
time to add the reporter's GitHub account as a collaborator as well, provided the sensitivity of the issue allows this.
To finalize the release of a security advisory you will need to request a CVE ID.  This is done *after* finalizing the
title, body, and CVE score using the calculator.  The request may require a few days on GitHub's side, so do not leave
this to the last minute, but also do not do it first since some security issues may become more (or less) complex as
you dig into them, or the upstream codebase changes.  If you need help, or want a second set of eyes don't hesitate
to ask.

The security advisory draft also provides a way to get a private repository in which to work on fixing the underlying
issue.  This is done towards the bottom of the page, with the `Start a temporary fork` button.  This will, as stated,
create a private fork of the main repo.  This repository functions exactly the same way as the upstream repo does, so
as you work on the security issue remember to regularly make sure your target branch is up-to-date as well or else you
may end up needing to quickly resolve merge conflicts when it comes time to actually release the fix!

Once you have started work and pushed a branch the draft security advisory will allow you to create a pull request.
This pull request will stay private, and within the private security repository.  It should still maintain the same
rules as the rest of our pull requests.  This PR does *not* get merged the normal way, so don't do that.

### Releasing the Advisory

Once work is complete and the CVE ID has been assigned, it's time to release the security advisory.  This is done on
the draft security notice page, at the bottom in the same spot as requesting the CVE.  This merge should be coordinated
with the current release manager(s) to ensure the fix is merged immediately prior to a new release.  Do not forget to
include the release notes changes, and to remind the release manager(s) to make a public note that there is a security
fix in the newly released version.  Once publicised, the private fork will automatically merge the fix into the target
branch, and then remove itself from GitHub.
