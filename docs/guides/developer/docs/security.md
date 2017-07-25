Opencast Security Issue Process
===============================

This document summarizes how issues are reported, who is responsible for what actions, and how things are handled
internally. The instructions here should be considered the *minimum*, and further/faster responses are certainly
possible depending on the severity and type of issue.

----------------------------

For Users and Administrators
----------------------------

What to do if you find a security issue
---------------------------------------

Report it to `security@opencast.org`! Please include a complete description of the issue, including which version(s) it
affects, as well as any steps required to reproduce it. This email will be sent privately to the full list of
committers for the project. You should receive an email acknowledging the issue from the QA coordinator, or release
manager(s) within 3 business days. At this point, depending on the issue, you may be asked for your JIRA and BitBucket
logins. It is enouraged to have one, since internal discussion on JIRA will be restricted to committers and the
reporter. Likewise, all code reviews will be done on in an internal BitBucket repository.

What to do once you have reported your security issue
-----------------------------------------------------

Wait, and/or help fix the issue. The committers will work to find a mitigation for the immediate case, and a proper
solution for the long term. This may take a while, please be patient. It may also require you to test it if the issue
is derived from a complex system that most committers would not necessarily have access to (e.g: your LDAP or LTI
servers).

What happens once the issue has been fixed?
-------------------------------------------
A notice to `security-notices@opencast.org` will be released once the issue has been resolved and proper patches applied
to the codebase. This notice may be accompanied by a release, or instructions on how to patch a live system depending
on the issue.


----------------------------

For Committers
--------------

What to do with a security report
---------------------------------

If no one else has, create a JIRA issue with a 'Committer' security level, and the details in the security report.
Assign the ticket either to yourself (if you intend on working on it) or nobody, and reply on `committers@opencast.org`
with the ticket. Thus far most of our security issues have come with patches attached, however this will not always be
true. Issues which require work beyond application of a provided patch should be treated the same as any other JIRA
issue, aside from the security level. Note that that security level will keep non-committers from viewing the ticket
or comments. Depending on the issue and severity, the QA coordinator and/or release manager(s) may apply for a Common
 Vulnerability and Exposures (CVE) number as well.

Where do we review security patches?
------------------------------------

Minor patches can be reviewed on an adhoc basis but larger patches, especially those requring collaboration in private,
or extensive comment and review, can use the matterhorn-security repository under the opencast-community account. Note
that this repository is private to committers, and reporters. Add the security repository to your remotes with this
command:

```no-highlight
git remote add security git@bitbucket.org:opencast-community/opencast-security.git
```

Then create your branch locally. When the branch is ready to be pushed to the security repo do something like this:

```no-highlight
git push security <your branch name>
```

You can then create your branch like you normally would, pushing it to security rather than your own repository.

Once a security issue has been resolved
---------------------------------------

Once a security issue has been resolved, the QA coordinator, and/or the release manager(s) affected will work together
to ensure that any relevant release(s) are created and available, and then release the security notice. This notice
must contain the CVE (if applicable) and JIRA ticket number, as well as the affected version(s) of Opencast, a
description of the issue, mitigation/upgrade instructions, as well as credit to the reporter(s) of the issue. As an
example, this is a good notice:

```no-highlight
Hello,
this is the official security notice regarding a security issue
recently discovered in Opencast.

Description:

   The Solr index for the search service (back-end e.g. for player and
   media module) in some cases returns results that should not be
   available to the current user.


Affects:

   This issue affects all recent versions of Opencast.


Details:

   Solr in some cases returned results that should not be available to
   the current user. For example, if `UserX` has the role `ROLE_USER`
   and a video should only be available for `ROLE_USER_ADMIN`, `UserX`
   can still access it.

   This may happen only if the second role starts with the complete
   first role. If the rules do not overlap, there should be no problem.


Patching the system:

   Patches for this issue are included in Opencast 2.2.4 and 2.3.0.
   A patch can also be found at
   https://bitbucket.org/opencast-community/opencast/pull-requests/1236


Credits:

  Thanks to Matthias Neugebauer from the University of Münster for
  finding, reporting and fixing the issue.


Best regards,
Lars Kiesow
```
