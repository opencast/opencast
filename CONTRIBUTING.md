Contributing to Opencast
========================

This is a short outline of what to pay attention to when contributing to Opencast. If you are interested in a long
document defining rules and recommendations as well as a detailed explanation of Opencast's branching model, take a look
at [the development process documentation](https://docs.opencast.org/develop/developer/development-process/). But this
guide should suffice for most contributions.


Bug Fixes and Feature
---------------------

Opencast distinguishes between bug fix and feature pull requests.

- Features are only allowed to be merged into `develop`, which will let them automatically become part of the next
  major release if they are merged before that releases feature freeze.

- Bug fixes can also go into release branches (named `r/_.x`). Once merged, they will automatically be part of the next
  maintenance release and will be merged down towards `develop` (e.g. `r/4.x` → `r/5.x` → … → `develop`).

Please carefully consider if a patch really needs to be applied to older versions. Making unnecessary changes in
maintenance releases can easily cause problems for adopters. Disruptive changes (database changes, index changes,
breaking configuration) should never go into maintenance releases.


Tests
-----

When building Opencast, a set of unit tests is automatically run on the code. Passing these tests is a hard requirement.
These (and a few more) tests are also run automatically on our CI system. Again, all tests need to pass.

If the CI tests on your pull request fail and you are sure it is not caused by your patch, please complain. Errors
happen and committers can easily trigger a new build. But your patch will not be merged without these tests passing.

Additionally, a reviewer will be assigned to your pull request to ensure that there are no further issues. Once
everything is fine, the reviewer will merge the pull request. The assignment may take some time. This is normal, so do
not be concerned.


Checklist
---------

- [ ] [Closes an accompanying issue](https://help.github.com/en/articles/closing-issues-using-keywords) if one exists
- [ ] Pull request has a proper title and description
- [ ] Appropriate documentation is included
- [ ] Code passes automatic tests
- [ ] The pull request has a clean commit history
- [ ] Commits have a proper commit message (title and body)

There may be additional, special requirements for certain changes (e.g. database changes require update scripts) but you
should be mostly fine if you follow this checklist. If you hit a special case, the reviewer will point that out and
maybe even help to fix that issue.
