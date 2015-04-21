Opencast Proposals
==================

All important decisions for Opencast Matterhorn have to be made on list. To do that committers may send proposals
(marked with #proposal) to list on which other committers may then vote. Opencast uses lazy consensus meaning that no
response signals agreement. Apart from that committers may vote with:

 - `+1` yes, agree - also willing to help bring about the proposed action
 - `+0` yes, agree - not willing or able to help bring about the proposed action
 - `-0` no, disagree - but will not oppose the action going forward
 - `-1` veto, disagree - opposes the action going forward and must propose an alternate action to address the issue or a
   justification for not addressing the issue


Passed Proposals
----------------

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
