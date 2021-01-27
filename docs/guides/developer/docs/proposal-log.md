Opencast Proposals
==================

All important decisions for Opencast have to be made on list. For more details, please have a look at out [documentation
about decision making](decision-making/index.md).

The following list contains a list of passed proposals for reference.

Passed Proposals
----------------


### Release Process Update
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 24 Dec 2020

```no-highlight
Hi everyone,
based on the discussion on list and in the technical meeting, Lukas and
I have created a proposal for changing our release process and applying
a new set of rules. You might notice that we have deliberately not
included all of what we discussed but hopefully only what can get
consensus for now.

Apart from some minor changes, the main difference in here is that we
transition from making a distinction between feature and bug fix to
defining a set of rules that hopefully ensure smooth minor updates.

If this proposal passes, we can work for there and refine this but we
hope that this is a good starting point.


## Release Schedule

- Major releases happen every half year
- Minor stable releases are cut monthly monthly
- Minor legacy releases are cut on demand
- Urgent minor releases may be cut if necessary
- Feature freeze for a major version should be about a month before the
  release


## Accepting patches for minor releases

- All patches need to be discussed in the technical meeting
    - Minor changes should not take much time
    - Protects against problematic changes in minor releases
    - Exceptions may be discussed if necessary (e.g. version bumps for
      libraries on develop)
- Patches for minor releases must not
    - Modify any existing database tables
    - Modify the indexes or otherwise cause re-indexing
    - Require a different ActiveMQ configuration
    - Modify existing translations
- Patches for minor releases must
    - Work with the same configuration within a major version


## Be Pragmatic

- If everything is broken, don't let rules hold you back…
    - There is always a special case: If in doubt talk to the community
      and find a solution that works.
    - Communication is key: If you think you need to break a rule,
      coordinate that with the community.


## Additional suggestions

- Patches should be applied to the latest release branch if possible
    - Avoid the risk of breaking legacy due to less testing
    - Avoid unnecessary merge conflicts
- Patches should avoid unnecessarily large changes in any release branch


As usual, if no one objects, this proposal passes in three days.

Best regards,
Lars
```


### Relocate build infrastructure to main repository
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 17 Dec 2020

```no-highlight
Hi everyone,
as you are all aware there are multiple community members working on
and maintaining infrastructure for different binary builds. For
example, Greg is doing the Debian packages.

All these are currently maintained in separate repositories… somewhere.
Greg and I would like to bring these back to the main repositories to
not loose track of what's available and where things are living.

But maintaining these sometimes requires quick actions and we cannot
really create a pull request every time a release is cut and then wait
a week or two before it's merged until we can release the packaged
version.

That is why I #propose to allow for maintainers of these areas to work
on these directly without running through our usual pull request and
review rules.

For example, that could mean that I could quickly update the RPM spec
file if necessary while I would not be allowed to just modify the
codebase (e.g. modules/*) on my own.

Initially, I #propose the following areas and maintainers:

  Debian build architecture              Greg Logan
  RPM build architecture                 Lars Kiesow
  docs.opencast.org build architecture   Lars Kiesow

This doesn't really change anything at the moment since we are already
in control of these parts right now. The difference would be that it's
not maintained somewhere any longer but everyone knows where to find
things and (potentially) how contribute.

Of course, we can extend this whenever we need to to include more/less
scripts. Though I suggest that we keep this to what we officially
support.

As usual, this proposal passes unless someone vetos it in the next 72h.

Best regards,
Lars
```


### JDK Support
Proposed by Greg Logan<gregorydlogan@gmail.com>, passed on Wed, 11 Nov 2020

```no-highlight
Hi all,

We have some old rules[1] around which JDK is supported by which version of Opencast, but obviously they are somewhat out of date at this point.  With that in mind, here's what I'm proposing:

Opencast 8: Formal support for JDK 8, and nothing else
Opencast 9: Formal support for JDKs 8 and 11, with a provisional "it should work" for newer versions[2]
Opencast 10: Formal support for JDK 11, with a provisional "it should work" for newer versions

Going forward, I would like to see a given version of Opencast support the latest two LTS JDKs as a general rule.

#proposal passes if no objections are raised by EOD 2020-11-11

G

1: https://docs.opencast.org/develop/developer/#proposal-log/#requiring-java-18-for-30
2: Note that JDK 11 is currently broken, at least until https://github.com/opencast/opencast/pull/2009 gets merged
```


### OSGi Annotations and Configuration
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Wed, 7 Oct 2020

```no-highlight
Hi everyone,
I hereby #propose that for all new code added to opencast

- OSGi annotations should be used instead of XML files for specifying
  components and dependencies.

  Reason: This is far less error prone. Even on today's Review Friday,
  we had problems with the XML files again. No wonder if you write
  these manually.


- @Activate and @Modified should be used for reading and updating
  configuration instead of implementing a Managed Service.

  Reason: Managed services are usually unnecessary and often make
  runtime updates to services problematic (see webinar below)

If there is a reason to go against these rules, it must be clearly
stated on a pull request.

OSGi component annotation example:

  https://vt.uos.de/e5msw


If you want to know more about the service configuration and see
annotations in action, watch the “Opencast OSGI Configuration“ webinar:

  https://video.ethz.ch/events/opencast/webinars/7261ea70-ce36-4e17-8634-963966311028.html

This proposal passes on Wednesday evening if no one objects.

Best regards,
Lars
```


### JPA, PostgreSQL and DDL-Scripts
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Wed, 30 Oct 2019

```no-highlight
Hi everyone,

  tl;dr – we want to replace the manually maintained database set-up
  scripts with JPA's auto-generation capabilities, properly including
  optimizations like indices, constraints, … while re-introducing
  proper support for PostgreSQL.


We recently looked into re-introducing proper PostgreSQL support for
Opencast, updating the driver [1], … – thanks to Beuth University – One
task left now is to properly initialize the database.

While we could now write our own initialization script for PostgreSQL,
much like `docs/scripts/ddl/mysql5.sql `, we would like to spare us as
community the double-effort and instead tackle an alternative route we
have been discussing in the community for a long time now: Making
Opencast generate a proper database schema on its own.

To give you a few more details, if you do not use the DDL scripts, but
just configure a database and start Opencast, you will already see a
database schema being auto-generated and everything (kind of) magically
works.

However, due to a mix of historical reasons and some deliberate
negligence on our side as developers – why do the JPA stuff properly if
we have to write the DDL script anyway – the auto-generated schema is
problematic for production since a lot of optimization is missing.

We did already take a look at this, evaluating the amount of work [2]
and fixing a few of the problems [3] but there is much more work to do.
The first pull request [2] also outlines in much more detail the
current state, what the problems are and what we have to do …if you are
interested.

Now, finally, we would like to continue this work. First, we are
seeking help from others, fixing the issues, but more importantly,
testing the work with your database set-ups.

Additionally, for us, this work only makes sense if we get a proper
PostgreSQL support out of it. Overall, I hope this will make supporting
all databases easier since we do not need to write set-up SQL anymore,
but for now, we do still need to write upgrade SQL statements and
officially re-adding support for PostreSQL, this means that we need to
write upgrade scripts for that as well – we are looking into fixing
that problem as well, but that's a separate project ;-)

Does anyone have objections to this plan?
Does anyone want to help (with development or testing)?

Best regards,
Lars

[1] https://github.com/opencast/opencast/pull/1103
[2] https://github.com/opencast/opencast/pull/1105
[3] https://github.com/opencast/opencast/pull/1133
```


### Pull request reviews
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 7 Feb 2019

```no-highlight
Hi everyone,
right now, we have 43 open pull requests. 23 of these are open for over
a month. 12 are open for over two month. I find this a bit problematic.

That is why I #propose to relax the requirement of reviewers as follows:

  Pull requests with no major modification and no review for over a
  month may be reviewed by developers from the same or related
  organizations.


Reasoning:

I find our review process to be very valuable since it prevents a lot
of bugs from getting into the code base in the first place. The state
of develop is proof that it works great: Most of the time, you can spin
up develop and use it without major issues. This state is completely
different from Opencast develop back in 1.4 days.

Nevertheless, it also puts a burden on the community since it sometimes
makes it very hard to fix problems. Not only do you need to write the
patch, but you also need to find someone completely unrelated to review
this. This can be quite hard in some scenarios. Example: SWITCH tasks
me with fixing a bug. This means that SWITCH is out of the loop for
reviews and ELAN e.V. is out as well. But not only that. We are related
to Osnabrück University so e.g. Rüdiger cannot review my patch and also
e.g. Michael is working for plapadoo and ELAN so he is out as well. In
short: ~80% of the regular reviewers are forbidden from taking the
review.

This example shows that our current rules can be problematic. This is
not generally the case, but this can be the case.

Going back to the original reasoning for this rule, it was an
intentional decision to prevent a specific institution from just
forcing code into Opencast without community involvement by creating a
pull request and having a colleague simply merge it.

Lifting the embargo after a month should have more or less the same
effect. The community has a month to notice and complain about any
given patch. Hence, no institution should be able to just push any code
into Opencast. If no one complained or commented for a month to raise
their concerns, it's unlikely that anyone really objects to the change
and it just becomes a matter of a technical review (does the code
have any problems). For that, any second developer should hopefully
suffice.

Please let me know if you have any objections to this change.
As usual, this proposal will pass in 72h if no one objects.

Best regards,
Lars
```


### Automate translation merges
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Wed, 29 Sept 2018

```no-highlight
Hi everyone,
a few weeks ago we discussed on the technical meeting that it would be
great if we could automatically merge back translations on a regular
basis.

That is why I hereby #propose:

  Updates to translations for existing languages may be pushed into
  Opencast's repository automatically.

Note that adding or removing languages will still need a regular pull
request and a review

If this proposal passes, I can implement the automation.

–Lars
```


### No more merge tickets
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 17 May 2018

```no-highlight
Hi everyone,

  tl;dr
  I hereby #propose to drop the practice of keeping a Jira ticket for
  synchronizing merges

Today we had a short internal discussion about Opencast's merge tickets
where we found that all of us here at Osnabrück think that we should
drop the practice of keeping them.

The original goal for those was to prevent two developers to merge
things simultaneously causing conflicts for each other. Nowadays most
people use Github's merge button anyway which makes this far less
problematic since developers do not need to keep track of the upstream
branch but can just merge which will just magically work as long as
there is no conflict.

Some people still merge via command line but they are usually those who
can handle conflicts anyway ;-)

What also plays into this proposal is that we do not have that many
merges. So this is not that big a problem in the first place. And for
the few occasions where there are many merges (e.g. I remember some
merge sprints before a feature freeze) we always coordinated those
efforts anyway (who is taking which review, what's the progress, …) so
that not having a merge ticket wouldn't be a problem here either.

From experience I can say that even for cutting a release I probably
could have worked without a merge ticket with no problem: I pulled the
version I was cutting into my local branch anyway, so I could work
there and additional merges would not have interfered.

Finally, if there is a rare case where it actually makes sense to block
a branch, I deem us flexible enough to shout out on list, which may even
work better since the message is not drowned by hundreds of similar
messages :)

Best regards,
Lars
```


### Migrate Docker Images to Quay.io
Proposed by Matthias Neugebauer <matthias.neugebauer@uni-muenster.de>, passed on Tue, 1 May 2018

```no-highlight
Hi,

as you might have noticed, there are currently no images available on Docker Hub
for Opencast 4.3. The problem is, that Docker Hub itself uses an old version of
Docker to build new images (version 17.06.1 is from mid 2017). When Opencast 4.3
was released, I prepared new Dockerfiles and also fixed an issue that resulted
in unnecessary large images. Now the problem is, that this requires (only for
building images) a feature that was only added to Docker 17.09 in September
2017. In addition, I found Docker Hub to be really slow and unreliable. Builds
start minutes after triggering and take a long time to complete. And there are
times when nearly all builds simply fail, e.g. because the base image could not
be downloaded or the machine used for building run out of disk space. All things
that should not happen leaving me quite frustrated with Docker Hub.

While Docker Hub is the "official" (more like default) image registry, there
exist multiple alternatives. Quay (https://quay.io/), for example, is another
bigger registry now run by CoreOS (owned by Red Hat). The service is free for
public images and offers some additional features compared to Docker Hub (e.g.
image vulnerability scanning). In my initial tests, I was really pleased. I
don't know what they are doing, but image builds start quick and take under 6
minutes! My local builds take 15-20 minutes :D

To come to the point: I herby #propose to further test out Quay and, if this
service performs well, migrate the Docker images to this registry. For the
tests, I would need to connect the opencast-docker repository to Quay, for which
I don't have the permissions. Also, the migration would leave the already
existing images on Docker Hub, but users would be advised to use the new
repository ("quay.io/opencast/allinone" instead of "opencast/allinone").

Best regards
Matthias
```


### Drop Undocumented Workflow Handler
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Fri, 10 Nov 2017

```no-highlight
Hi,
lately I discovered a couple of undocumented and probably unused
workflow operation handler, for example the 'failing' operation to name
one of them. All of these are not really useful in their current
undocumented form. That is why I hereby #propose to

  drop all workflow operation handler still undocumented at the end of
  the year.

If this proposal passes, I will create and publish a list of all
operations which would be dropped in their current state. If someone
still wants to keep any of them, the only thing they need to do is to
write a short documentation page for those operations. A task easily
done.

That way we may get rid of some unused, unnecessary old operations
while ensuring that all of the actually useful ones are documented and
thus usable without special inner knowledge of Opencast.

Regards,
Lars
```

### Changing Translation Sources
Proposed by Sven Stauber <sven.stauber@switch.ch>, passed on December 20, 2017

```no-highlight
Dear Opencast Developers

I hereby #propose to add an additional rule to our development process as
described on [1]:

Adding or changing translation sources is not allowed in release branches
(implying that pull requests doing so need to be directed to the branch
develop).

Best regards
Sven

[1] https://docs.opencast.org/develop/developer/development-process/
```

### Crowdin Acceptance Policy
Proposed by Greg Logan <gregorydlogan@gmail.com>, passed on November 17, 2017

```no-highlight
Hi all,

Per the discussion in the meeting today, we need to set a policy regarding what
is expected of our Crowdin translators prior to joining the translation team.
My proposal is that they must write a brief, understandable sentence regarding
why they want to help translate Opencast via the Crowdin UI.  This is an
optional field in the workflow where they request to be a translator (ie, no new
tools or fields) which is sometimes filled in, but mostly left blank.  Something
like

'I want to help translate $project into [language]'

would be sufficient.  This filters out the bots, yet is simple enough that
someone with Google translate ought to be able to work something out.  Once this
passes I will update the Crowdin and Opencast docs regarding the requirements,
and then we should be good to go.

Proposal closes EOD 2017-11-17.
```

### Rename Matterhorn Repository To Opencast

Proposed by Lars Kiesow <lkiesow@uos.de>, passed on July 13, 2017

```no-highlight
Hi everyone,
I think we have reached a point where people are wondering what the
hell matterhorn is ;-D

That is why I #propose to rename our official repository from
matterhorn to opencast:

old: https://bitbucket.org/opencast-community/matterhorn/
new: https://bitbucket.org/opencast-community/opencast/

This proposal will end on Thu Jul 13 16:00 CEST 2017

Regards,
Lars
```

### Criteria For Inclusion Of Translations
Proposed by Sven Stauber <sven.stauber@switch.ch>, passed on April 28, 2017

```no-highlight
Dear all,
There are currently no rules about the criteria needed for a translation to be
included or excluded from the official Opencast releases.

I hereby propose the following rules:

1.  A not yet supported translation is included into the next major release if
    it is translated to at least 90% at the time when the release branch is cut.
    The release managers will take the review if no other reviewer can be found.

2.  A not yet supported translation may be included in the current release
    branch anytime if it is translated to 100% and a reviewer is found. It will
    then be part of the next minor release and major release if feasible

3.  An endangered translation is a supported translation that is translated less
    than 80% at the time when the release branch of the next major release is
    cut. The release managers will publish a list of endangered languages if any

4.  An endangered translation will be removed with the next major release if it
    is not saved. The release managers take care of the removal in case no other
    person will

5.  An endangered translation may be saved by reaching at least 90% translated
    until at least two weeks before the release date of the next major release
    and a reviewer is found

6. Considering the percentages of being translated, Crowdin acts as reference

7. Considering the dates of the release cuts of major releases, the respective
   releases schedules act as reference

Best,
Sven
```

### Make Maintenance Releases Easier
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on April 24, 2017

```no-highlight
Hi everyone,
over the last years, I have cut a lot of Opencast maintenance releases.
The process is to announce that a release will be cut, create a release
candidate and wait 72h without veto to actually release.

I have always sent out the voting mail as required by our release
process and I can always count on the usual response: No reply.

This is actually not very surprising since for example now for the
2.3.3 release, people have either already tested the latest state of
r/2.3.3 or are involved in the next big release already.

In short this means that we always have a three day waiting period in
which basically nothing happens. That is why I would like to change the
process for *maintenance releases* in the following way:

  A release manager may cut new maintenance releases at any time
  without prior release candidate.
  He should openly announce the date for a new release a week before
  the release or at any earlier point in time.

Note that this will also allow a release manager to release as fast as
possible if necessary (e.g. security fix) since the announcement is not
strictly required but only a strong advise.

This should lessen the work for the a release managers and will enable
more agile release processes. We also should not really loose any QA
work since everyone knows when releases will happen and people can
always test the latest state of a release branch which will become the
new release.

This proposal will not affect major releases where release candidates
with three days testing period would still be required.

I hope you agree with this change,
Lars
```

### Minor documentation changes do not require JIRA issues or PRs
Proposed by Stephen Marquard <stephen.marquard@uct.ac.za>, passed on June 9, 2017

```no-highlight
To reduce the overhead involved in improving our documentation, I #propose that
minor fixes to documentation may be committed to either maintenance branches or
develop without requiring a JIRA issue or pull request.

Markdown docs can be edited directly on bitbucket (and git should we move to
that), which is a very fast and convenient way for developers to fix
documentation.

Constraints: documentation fixes committed in this way should be minor changes
only; for example fixing typos, layout, formatting, links or small changes to
existing content, but no significant new content (which should continue to go
through the usual review process).
```

### Requiring Java 1.8 for 3.0
Proposed by Greg Logan <gregorydlogan@gmail.com>, passed on June 12, 2017

```no-highlight
Hi folks,

For those following along, James Perrin has identified an issue where 3.0
requires Java 1.8 at runtime.  We haven't formally included that
requirement for 3.0 yet (it's already required for 4.0), but I hereby
propose that we do.  No one seems to have noticed this requirement was
already present in 3.0 (not even me!), even at this late in the release
cycle which speaks, I think, to the already widespread adoption of Java
1.8.  We would also have to go back and redo all of our testing were we to
change the problematic jar to an earlier version, which would be
unfortunate for our release timelines.

This proposal closes EOD 2017-06-12 UTC -6, at which point I should be able
to cut the release.

G
```


### Change version numbers scheme
Proposed by Rüdiger Rolf <rrolf@uni-osnabrueck.de>>, passed on Mar 23, 2017

```no-highlight
Hi all,

as we currently approach a new release, I would like to raise a question
when it comes to our version numbers: du we need a version number that
consists of three parts?

At the moment we have
<main-version-number>.<major-release-number>.<minor-release-number>.

With our current release process, with a new release every 6 month we
would always increase the <major-release-number>. Additional to this we
have the <minor-release-number> for bug-fix-releases, whenever they are
needed.

But we do not have a process for increasing the <main-version-number>.
Okay 2 years ago we were lucky enough that two long running sub-projects
that replaced all UIs in one release were finished. That was an obvious
reason to increase the main version. But will we ever be that lucky
again? Is only replacing all UIs justifying a main version increase?
If I look at the project history we had several milestones that could
have justified a new main version, like a nearly complete refactoring of
the backend in 1.4, the video-editor in 1.6, the Karaf update in 2.1,
the External API in 2.3.

*So my #proposal would be to remove the first part of the version number
for all upcoming releases. So our next release would be 3.0 and the
release at the end of the year it would be 4.0. *

We would follow other projects like Sakai in this change  - although
without the confusing part of going from 2.9.3 to 10.0, where they
removed the first number.

What are your thoughts?

Regards
Rüdiger
```


### Officially declare the Admin UI Facade as internal API for exclusive use by the module matterhorn-adminui-ng
Proposed by Sven Stauber <sven.stauber@switch.ch>, passed on December 16, 2016

```no-highlight
Dear all,
I hereby propose to officially declare the Admin UI Facade as internal API for
exclusive use by the module matterhorn-adminui-ng.

Reason:
The Admin UI Facade is essentially the backend of the Admin UI.  While it would
be technically possible to use this API for other purposes, this would introduce
dependencies to components other than the Admin UI.

Allowing such dependencies to come into existence would cause changes to the
Admin UI Facade to potentially break other (possibly unknown external)
components.  Hence, we would need to announce, coordinate and discuss changes to
this API to not break dependencies to components we potentially don't even know.
This would unnecessarily slow down the future development of the Admin UI.  In
addition, Opencast 2.3 introduces the External API which has been explicitly
designed to meet the requirements of an API used to integrate other components.

Changes needed:
The documentation needs to reflect that the Admin UI Facade is an internal API
that will be changed without prior announcement whenever needed without
respecting dependencies other than the Admin UI itself and therefore people
shall not use this API for integration purposes.

Best,
Sven
```

### Opencast Next: Code Cleanup
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 7 July 2016 15:21:19 UTC

```no-highlight
Hi everyone,
a while ago we discussed on the technical meeting that we would like to
remove some old code from Opencast since these parts do not work
properly (sometimes not at all) or are unused.

Why cleaning up? To name some reasons:

- Less code to run (less memory, faster start-up)
- Less things to compile (faster build)
- Less dependencies
- People do not accidentally stumble upon broken things
- Less work for maintenance

And now here is what I #propose to remove and a reason why I think this
should be removed. I already took the comments people made in the first
draft [1] into account, although I still dared to include the two last
items but this time, hopefully with a convincing reason for why they
should be removed.

1. Old Administrative User Interface (matterhorn-admin-ui)
   The reason for this should be obvious: We got a new one. The old one
   has not been tested for the last three releases, is not linked
   anywhere anymore and is partly buggy due to changes to Opencast. To
   maintain two interfaces for one thing do not make sense.

2. Hold-state Workflow Operations
   These do not work with the new interface any longer and the concept
   has since been replaced by the actions you can perform on archived
   material.

3. CleanSessionsFilter
   Old temporary bug fix. For more details read the thread on our
   developer list.

4. Republish Workflow Operation Handler
   It can be removed since it has been replaced by a flag on
   the publish operation in 2.x.

5. Old workflows + encodings
   We got new ones. These were only left because of the old ui.

6. Old player (Flash in engage ui)
   Flash is dead. We have the new player and Paella.

7. Most of shared_ressources
   Almost everything in here belongs to old user interfaces.

8. matterhorn-engage-player
   This is the old player Flex project. Iam not even sure it can still
   be compiled.


9. matterhorn-test-harness
   Old integration tests

10. matterhorn-mediapackage-ui
    Old UI ressources

11. matterhorn-manager-*
    Old, outdated configuration modification via web ui. This was never
    used and would need a major update to get it working again at all.

12. matterhorn-load-test*
    Some tests. I have never seen them executed by anyone.

13. matterhorn-holdstate-workflowoperation
    Workflow operations requiring a hold state which does not exist
    anymore with the new admin interface.

14. matterhorn-deprecated-workflowoperation
    The name says everything. This includes the download DVD operation.

15. matterhorn-annotation-*
    This should not work with either of the current players anymore.

16. docs/jmeter, docs/scripts/load_testing
    Configuration for a performance testing tool. Not used for a long
    time and not up-to-date.

17. Everything unused from:
    https://data.lkiesow.de/opencast/apidocs/deprecated-list.html
    E.g. FunctionException and ProcessExecutor(Exception)

18. matterhorn-webconsole
    Karaf comes with a web console. We do not use our old implementation
    anymore.

19. matterhorn-mediapackage-manipulator
    Rest endpoint for media package manipulation. It's not used anymore
    except by components to be removed.


20. matterhorn-search-service-feeds
    Broken implementation for RSS/Atom feeds

21. matterhorn-caption-* and embed operation
    Service for converting different subtitle formats and operation to
    embed these subtitles into the media files. This is *not* player
    caption support. If required, FFmpeg can be used for conversion
    between several subtitle formats. Asked on list [2], no one uses
    this.


As indicated before, points 20 and 21 had some comments for leaving them
in which did not convince me to not propose this. “Instead of removing
it, fix it” is an easy thing to say but sadly requires ressources.
Keeping it, announcing it as features and then tell people that it is
not working only afterwards is a bad thing and I would like to avoid
that.

Note that all the code is still in our history so that we loose nothing
if we want the old code back.

Please feel free to indicate if this action is fine for you or if you
want to keep some of the marked code. Please provide a reason if you do.

Best regards,
Lars

[1] http://bit.ly/28YOEZ1
[2] http://bit.ly/28Ztlt8
```

This proposal has passed with these additional corrections:

```no-highlight
Hi,
we discussed this on today's technical meeting and I'm slightly
changing the proposal:

20. Let's remove matterhorn-search-service-feeds only after September
    1st which is a realistic time to get things into the next Opencast
    release. If someone has fixed the issue by them, we will, of
    course, keep it.
    This change takes into account that some people have said they are
    interested into fixing that module, but will make sure that it's
    removed if no one fixes it to not have an advertised but broken
    feature.

21. I will be looking into adding subtitle support in a sensible way
    before removing the matterhorn-caption-* modules or at least
    clarify if they can still be used.

Regards,
Lars
```

```no-highlight
Hi James,
a couple of days, I talked to someone saying that he will soon provide
a patch adding exactly this functionality. The holdstate operations are
definitely broken due to their UI.

My suggestion for a compromise here:
 - Remove them if that patch for archiving the options is released
 - Remove them if no one fixes them in time (September 1st) for 2.3

If you want to bring them back later, we always keep the code in our
history.

Regards,
Lars

> Hi,
> I would like to keep 2 and presumably 13. Both Manchester and AFAIK
> Cape Town have use cases for hold states since there is still no
> mechanism for passing WF configuration options from one WF to another.
> Regards
> James
```

The patch has [already been published](https://bitbucket.org/opencast-community/matterhorn/pull-requests/1104).


### Opencast Community Repository Owners
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Fri, 13 May 2016 18:41:52 UTC

```no-highlight
Hi,
today, in the technical meeting, we shortly discussed how to handle
requests, problems, etc regarding the other repositories we are hosting
under the umbrella of the Opencast community:

  https://bitbucket.org/opencast-community/profile/repositories

While we have people who care about the official Opencast repository as
well as rules about what may be merged, who may merge things, … we do
not have that for other repositories and for some it's very unclear.

That is why I would like to propose that every repository under the
umbrella of the Opencast community needs to have a “project owner”
being responsible for that repository. Usually it should be the one
requesting that repository, but of course it can be someone else known
in the community.

I would also like to propose that if there is no one willing to take up
the responsibility to take care of a repository (ownership) if an old
owner leaves, the repository should either be removed or marked as
deprecated and moved to a separate section if so requested.

Finally, I would like to propose that we use the new “project” feature
of BitBucket to group the repositories into the groups:

- Opencast
- Contrib
- Adopters
- Deprecated (<- to be created if needed)

Currently, all repositories are in one big project.

Regards,
Lars
```

### Rename Opencast Mailing Lists
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 14 Apr 2016 00:00:00 UTC

```no-highlight
Hi everyone,
traditionally, we have the three mailing lists:

 - matterhorn@opencast.org (development list)
 - matterhorn-users@opencast.org (user list)
 - community@opencast.org (more or less announcements)

Recently, though, we have seen especially the last two list being used
for user questions and problems. That is not surprising as we dropped
the name “Matterhorn” and new users do not know what that the list
matterhorn-users is meant for questions about Opencast.

That is why I would like to rename these lists to

 - dev@opencast.org or development@opencast.org (I prefer the short
   name but don't have very strong feelings about that)
 - users@opencast.org
 - announcements@opencast.org

Together with the already existing security-notices list, this gives
these lists a very clear meaning. It would also have the benefit that
users only interested in general announcements could subscribe to one
list only which would likely be a very low-traffic mailing list.

Additionally, this would make it sufficient to send announcements to
one list, instead of sending it to all three lists.

To prevent general questions on the announcements list, I suggest we
grant posting rights to board members, committers or other people who
have or had a role in our community only. I don't think we need to be
too strict here but should make sure that people understand what this
list is for.

Finally, for the sake of our current members, I would suggest that we
forward the mails to the old addresses for at least until the end of
the year, if that is possible.

Best regards,
Lars
```


### Documentation Pull Request Merge Order
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Thu, 25 Feb 2016 20:52:00 UTC

```no-highlight
Hi everyone,
as discussed in this weeks technical meeting, I hereby #propose to
allow out-of-order merges of documentation pull requests in the same way
we have this exception for bug-fixes.

to be precise, I #propose to change the development process docs for
reviewing and merging [1] in the following way:

[old]

 - Pull requests for bug fixes (t/MH-XXXXX-...) may be reviewed and
   merged out of order.

[new]

 - Pull requests for bug fixes or documentation may be reviewed and
   merged out of order.

Regards,
Lars

[1] https://docs.opencast.org/develop/developer/reviewing-and-merging/
```



### Removing instances of print statements with a style rule #proposal
Proposed by Greg Logan <gregorydlogan@gmail.com>, passed on Wed, 12 Feb 2016 12:00:00 UTC

    Hi folks,

    I noticed in a recently review that there are still System.out.println
    statements in use in our codebase.  I was surprised, because thought we had
    previously implemented a checkstyle rule which would have banned those
    statements!  I hereby #propose that we implement the changes outlined in
    https://opencast.jira.com/browse/MH-11222, and remove these statements in
    favour of logger statements.  I also propose that we add this rule to the
    checkstyle ruleset so that we don't have to deal with this again going
    forward.  Proposal closes EOD 2016-02-03.

    G



### How to release a new Opencast version…
Proposed by Lars Kiesow <lkiesow@uos.de>, passed on Fri, 14 Aug 2015 12:54:51 UTC

    Hi everyone,
    serving as co-release manager for two versions of Opencast, I noticed
    that our current release process has some aspects of the release defined
    in a way that is more hindering than helpful and I want to #propose a
    slight change to these recommendations.

    I hereby #propose:

    1. Get rid of the `master` branch, make `develop` the main branch.
    2. Do not use the --no-ff flags for merges
    3. Do not create versions/tags in a release branch. Separate them.


    Reasoning:

    1. The short explanation whould be: When did you explicitely checked
       out `master` last time? People rarely do that. If I want a specific
       version, I use the tag, if not I want the release branch or
       `develop`.
       If you think about it, then the whole reason for `master` in GitFlow
       is to always provide the last stable version to users who just check
       out the repository and do nothing else. The problem with Opencast is,
       that we support multiple versions at the same time. If in a couple
       of weeks 1.6.2 is being released, it is the latest stable. Is it? If
       I check out `master`, however, I will still get 2.0 as we cannot
       merge 1.6.x afterwards. While you can grasp the reasons behind this,
       it is a bit confusing for users and it is much easier to just tell
       them to use the tag to check out a specific version.
       That it, if they do not use the tarballs from BitBucket anyway.

    2. First of all, most people seem to be using BitBucket for
       auto-merging and it does not use --no-ff. So we are not really
       consistent anyway. Being consistent and using  --no-ff would mean to
       forbid the usage of the BitBucket merge.
       Second, have a look at the confusing mess that are the current
       branches (I tried to find something in the visualization a while ago
       but gave up). It would be much cleaner to try using fast-forward
       merges. So instead of using non-fast-forward commits I would argue
       that we should instead try to use as many fast-forward commits as
       possible.

    3. Once we decided to have the tags in our branches like this:

          ---- A ---- B (tagged) ----- C ---- D -->

       A is the commit containing the version that is decided to be
       released. B is the tagged version. It is exactly the same code as A
       except for the pom.xml versions that are modified. Finally C then
       reverts B as the modified version should not be part of the release
       branch, .... After C, the code is basically A again except for the
       history (which we later need to merge which can be problematic). D
       would then be the next “real” commit, meaning the next fix.

       Much easier to handle would be the following structure:

         ---- A ---- D -->
               \
                B (tagged)

       You do not have to revert that commit, you do not need to merge the
       easily conflicting pom.xml changes and in the end, you would anyway
       check out the tag using  git checkout <tag>  if you want that
       specific version


    Branching structure:

    To have a complete overview, this is what the new branching structure
    would look like:


      develop --*--*--*--*--*----*--------*--------*---->
                       \        /                 /
                r/x.y.z *--*--*---*--*--*--*--*--*---->
                               \                  \
                                * x.y.z-beta1      * x.y.z-rc1

    Regards,
    Lars



### Moving away from the 3rd party scripts
Proposed by Greg Logan <gregorydlogan@gmail.com>, passed by Fri, 24 Jul 2015 16:45:40 UTC

    Hi folks,

    As it stands right now we depend on the 3rd party tool script to
    install a great many of our 3rd party dependencies.  These are
    utilities like tesseract, ffmpeg, sox, etc.  This script is maintained
    by Matjaz, in his own time.  I'd like to take a moment to thank him
    for a doing a great job on a particularly annoying aspect of
    supporting our work!  I know it hasn't been easy, especially
    supporting vast number of different OS versions!

    With the release of 2.0 I noticed that our 3rd party tool script is
    becoming both a little out of date, and difficult to maintain.  I took
    a quick look around and it seems like *most* of our dependencies are
    available from normal distribution repositories for Debian based
    systems, and I'm told that there is a similar situation for Redhat
    based systems.  I am unsure of how many of our users are running
    Matterhorn on Mac, but I would hope that our developers who are
    working on Mac would be able to provide instructions and/or binaries
    for those users.  The only dependency where there might be a universal
    sticking point is ffmpeg (due to patent concerns), however ffmpeg
    builds a full static binary with each release, so I assume we can
    either depend on this and/or cache them somewhere.

    What this means is that we can potentially remove the 3rd party script
    from our repository.  I hereby #propose we find a way to do that,
    which would remove the 3rd party script from the repository and
    replace it with a number of new steps in the install documentation.

    G



### Status of youtube in 2.0 and #proposal to change the default workflow
Proposed by Rüdiger Rolf <rrolf@Uni-Osnabrueck.DE>, passed on Sat, 13 Jun 2015 14:15:55 UTC

    Hi list!

    There was some discussion in the DevOps meeting yesterday if the
    Youtube distribution would work or not. I offered to check this.

    The good news first: IT WORKS!
    Just follow this manual and your Matterhorn - ups Opencast -  is ready
    to distribute to Youtube.

      http://docs.opencast.org/r/2.0.x/admin/modules/youtubepublication/

    The bad news: The default workflow definition does not really support
    the publishing on Youtube, as only one video file could be published
    by the current WOH.

      https://opencast.jira.com/browse/MH-10920

    The reason is simple and the fix would be too. But there are some
    options to fix this:

    1. Remove the option to distribute to Youtube from the default workflow
       definition, as the complicated configuration would have to come
       first anyway.
    2. Only let "presenter" or "presentation" be published to Youtube. We
       would need a new youtube tag and add this to the compose operation
       and the youtube operation.
    3. Introduce the composite operation to the workflow definition and
       publish only the resulting single stream to Youtube.
    4. Upgrade the WOH to support publishing of multiple files.

    I would say that option 4 could be 2.1 goal, but not for 2.0.

    I would #propose to go for option 1, as nobody can use Youtube
    out-of-the-box anyway. And the admin could then setup  an appropriate
    Youtube workflow for their needs too.

    Regards
    Rüdiger



### Episode DublinCore Catalog
Proposed by Karen Dolan <kdolan@dce.harvard.edu>, Passed on Sat, 30 May 2015 12:39:05 UTC

    Dear Opencast-ees,

    The following proposal addresses MH-10821[1]. An issue that exposes a
    know long time ambiguity regarding metadata and the ingest service.
    The reason that its a proposal is that it normalizes the handling of
    inbound episode catalog metadata in the ingest service.


    1) A new configuration parameter, boolean, for the Ingest Service. The
       config param identifies if episode metadata precedence is for
       Ingestee (i.e. Opencast system) or the Ingester (i.e. Capture
       Agent).

       For example: at our site, the scheduling entity is the metadata
       authority. All updates are made to the Scheduling endpoint. The
       Capture Agent always has stale episode catalog metadata. At other
       sites, updates are made on the Capture Agent directly. The
       community default can be for priority to the Capture Agent.

    2) All Ingest endpoints perform the same consistent process to ensure
       that an episode catalog will exist, manually or automatically
       provided.

    3) The process performs the following...

    3.1. Gather data

     - Check if inbound media package contain a reference to an Episode
       DublinCore catalog and if that catalog contains a title.
     - Check if the inbound media package contains a title attribute.
     - Check if the Workflow service has a reference to the mediapackage's
       Episode Dublin Core catalog
     - Check if the Scheduler service retained a reference to the event's
       Episode Dublin Core catalog

    3.2. Use config param to prioritize action on acquiring an Episode dc
       catalog for the media package

      If Capture Agent metadata takes precedence:
         - Take the inbound Episode dc catalog, if it exists
         - Take the Episode dc catalog from the workflow service, if it
           exists
         - Take the Episode dc  catalog from the scheduler service, if it
           exists
         - Create an Episode dc catalog from the title in the media
           package,, if it exists
         - Create an Episode dc catalog using a default title (i.e.
           "Recording-1234556XYZ")

     If Opencast metadata takes precedence:
         - Take the Episode dc catalog from the workflow service, if it
           exists
         - Take the Episode dc  catalog from the scheduler service, if it
           exists
         - Take the inbound Episode dc catalog if it exists
         - Create an Episode dc catalog from the title in the media
           package, if it exists
         - Create an Episode dc catalog using a default title (i.e.
           "Recording-1234556XYZ")

    I'll start a pull for the above, and appreciate any thoughts.

    Regards,
    Karen

    [1] https://opencast.jira.com/browse/MH-10821



### Dropping Taglines
Proposed by  Greg Logan <gregorydlogan@gmail.com>, Passed on Fri, 29 May 2015 16:19:09 UTC

    Hi folks,

    I hereby propose that we drop the practice of having taglines.  I
    propose this because we don't have a place in the new admin UI to put
    them, nor have I ever heard any of the adopters make use of it.  I know
    we don't use it as a committing group, which means that *no one* is
    using them.

    G



### Wiki Cleanup
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Fri, 24 May 2015 11:36:49 UTC

    Hi everyone,
    since we partly switched to our new documentation [1] I would like to
    make sure that the old and mostly outdated documentation goes away so
    that no one stumbles upon that. When I had a look at the wikis we
    currently have I noticed that most of our 17(!) wikis have not been
    touched in years and can probably go away.

    Here is a list of our wikis and what I #propose to do with/to them:

    Keep (maybe clean-up a bit):
     - Matterhorn Adopter Guides
     - Matterhorn Developer Wiki
     - Opencast Matterhorn D/A/CH
     - Opencast Matterhorn Español
     - LectureSight

    Export as PDF to archive the contents and then delete:
     - Matterhorn Release Docs - 1.0
     - Matterhorn Release Docs - 1.1
     - Matterhorn Release Docs - 1.2
     - Matterhorn Release Docs - 1.3
     - Matterhorn Release Docs - 1.4
     - Matterhorn Release Docs - 1.5
     - Matterhorn Release Docs - TRUNK

    Keep until 2.1 is out then export as PDF and delete:
     - Matterhorn Release Docs - 1.6

    Just delete:
     - Analytic video annotation
     - Infra
     - Matterhorn Documents
     - Opencast Community


    Please let me know if you agree or disagree with this proposal.

    Regards,
    Lars

    [1] http://documentation.opencast.org



### Jira Clean-Up
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Fri, 8 May 2015 11:52:16 UTC

    Hi everyone,
    as discussed in the technical meeting, I hereby #propose:

      The “Blocker” and “Release Blocker” severity status are more or less
      redundant. As part of cleaning up Jira, let us remove the “Release
      Blocker” severity in favor of “Blocker”.

    As footnote, some statistics: Since the beginning of 2014, 70 Release
    Blockers have been files in Jira while mere *8* Blockers have been
    files.

    Regards,
    Lars



### Opencast Documentation
Proposed by Rüdiger Rolf <rrolf@uni-osnabrueck.de>, Passed on Sat, 02 May 2015 14:43:28 UTC

    Hi all,

    Tobias, Basil, Lars and I discussed status of the current migration of
    the Opencast (Matterhorn) documentation to GIT. We still see some open
    issues that need clarification so we would like to propose the
    following points:

    *1. Formating and Hosting of the Documentation *

    We want to use https://readthedocs.org to or a similar service create
    a more appealing HTML version from the Markdown of the documentation.

    The documentation will be versioned there so that for older versions
    the documentation is still available. By default the "latest" version
    is shown.  The versions of the documenation will be generated based on
    the release branches.

    *2. Structure of the Documentation*

    We see the documentation in*Git *separating into 3 sections:

     - /Administration Guide/: with information about the installation,
       configuration, customization and integration. This will be the part
       of information by an administrator to setup
       Opencast.

     - /Developer Guide/: All information related to implementation
       details of Opencast, so that this will be updated in a pull request
       (API changes, module descriptions, architecture). The development
       process documents should also go here as only committers usually
       should change these.

     - /User Guide/: Documentation of the (new) Admin UI that was already
       started by Entwine and the Engage UI (especially Theodul Player).
       This guide should only describe options available on the UIs.

    Within the *Wiki* we still see the need for 2 sections:

     - /Developer Wiki/: Proposals, working documents and meeting notes
       will be kept here so that anybody can edit these. So information
       not to close to any existing implementation that might still be in
       a process of discussion can be found here.

     - /Adopters Wiki/: This can be the place where adopters share their
       best practises, configurations, hardware recommendations,
       third-party software documentation etc. Again anyone can contribute
       to this wiki.

    The difference between the Wiki and Git is in the first line that the
    Git documentation should become a quality assured ressource for
    Opencast users. The Git documentation should be reviewed within the
    release process and it will be part of the review process of a pull
    request, to make sure that the needed documentation changes have been
    contributed too.

    The Wikis on the other hand should be a more open platform where
    everybody can contribute and users might find cookbooks to enhance
    their system, or they can share ideas.

    So now we would like to get your opinion on this proposal.

    Thank you,
    Rüdiger



### Requirement Specification
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Thu, 16 Apr 2015 15:55:31 UTC


    On list or IRC we often see that people do not really know the current
    requirements for a specific version of Opencast Matterhorn. Of course
    there are the pom.xml files specifying internal dependencies, but there
    is nothing for 3rd-party-tools, ...

    It would be nice to add a file specifying these requirements in a
    format that is easy to parse and can hence be used for automatic
    scripts to generate dependency lists, ...

    That is why I hereby #propose to add a requirements.xml file that
    specifies the requirements for Opencast Matterhorn:
     - Required tools including versions
     - Which modules require which tools
     - Which modules conflict with each other (negative requirement)

    This is mainly what is not specified by the pom.xml files yet.



### Jira Clean-Up (Tags VS Labels)
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Thu, 19. Mar 2015 15:43:20 UTC

    …then hereby I officially #propose removing the labels from Jira.

For more details, have a look at the mail thread at:

    https://groups.google.com/a/opencast.org/forum/#!topic/matterhorn/vIdWQkZmbdQ



### FFmpeg Update
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Sat, 14 Mar 2015 22:12:18 UTC

    Looking at the FFmpeg project for the last two years, you will notice
    that they developed a pretty stable release cycle with a release of a
    new stable version approximately every three month.

    To stop us from having to propose an update again and again, I hereby
    propose the following general rule for our support of FFmpeg:

      A Matterhorn release will oficially support the latest stable
      version of FFmpeg released at the time the release branch is cut and
      all other FFmpeg versions with the same major version number released
      afterwards.

    For example, for Matterhorn 2 this would mean that we will officially
    support FFmpeg 2.5.4 and all later 2.x versions like 2.6 which has
    been released on the 7th of March or a possible 2.7 onece it is
    released. We would, however, not necessarily support an FFmpeg 3 as it
    *might* come with an interface change that *could* break compatibility.

    That obviously does not mean that older versions of FFmpeg just stop
    working. In fact, most parts of the default Matterhorn configuration
    should at the moment still work with FFmpeg 1.x but we will not test or
    fix compatibility problems.


### Proposal Log
Proposed by Lars Kiesow <lkiesow@uos.de>, Passed on Sat, 14 Mar 2015 16:35:08 UTC

    It would be wonderful if we had a central place to look up the proposals
    that have passed.

    That is why I hereby propose that:

     - We create a proposal log in our new documentation containing all
       proposals that have passed on list.

     - A proposal will become effective only after it is written down in
       that log. That should usually be done by the person who sent out
       that proposal.

    This will, of course, not affect the existing decision making rules
    (proposal on list, marked with #proposal, lazy consensus after three
    days, no -1, ...)
