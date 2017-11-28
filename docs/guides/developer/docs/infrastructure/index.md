Opencast Infrastructure
=======================

List of Opencast project infrastructure and administrators.  For detailed notes go [here](notes.md)


Test Servers
------------

Institution                | Hostname                      | Admin (Software) | Admin (Hardware)   | Notes
---------------------------|-------------------------------|------------------|--------------------|-----------------
University of Osnabrück    | develop.opencast.org          | Lars Kiesow      | Lars Kiesow        |
ETH Zürich                 | stable.opencast.org           | Lars Kiesow      | Markus Borzechowski|
SWITCH                     | admin.oc-test.switch.ch       | Greg Logan       | Lars Kiesow        | May be unavailable after 2017-07
SWITCH                     | player.oc-test.switch.ch      | Greg Logan       | Lars Kiesow        |
SWITCH                     | ingest.oc-test.switch.ch      | Greg Logan       | Lars Kiesow        |
SWITCH                     | worker1.oc-test.switch.ch     | Greg Logan       | Lars Kiesow        |
SWITCH                     | worker2.oc-test.switch.ch     | Greg Logan       | Lars Kiesow        | inactive
SWITCH                     | worker3.oc-test.switch.ch     | Greg Logan       | Lars Kiesow        | inactive
SWITCH                     | worker4.oc-test.switch.ch     | Greg Logan       | Lars Kiesow        | inactive
SWITCH                     | database.oc-test.switch.ch    | Greg Logan       | Lars Kiesow        |
SWITCH                     | download.oc-test.switch.ch    | Greg Logan       | Lars Kiesow        | message broker
SWITCH                     | streaming.oc-test.switch.ch   | Greg Logan       | Lars Kiesow        | storage/nfs
SWITCH                     | 10.0.207.247 (intern)         | Lars Kiesow      | Lars Kiesow        | capture agent


Maven Repository
----------------

Institution                | Hostname                      | Admin (Software) | Admin (Hardware)    | Notes
---------------------------|-------------------------------|------------------|---------------------|---------------
Harvard DCE                | mvncache.opencast.org         | Lars Kiesow      | DCE Devel group     | Amazon Cloud
University of Osnabrück    | nexus.opencast.org            | Lars Kiesow      | Lars Kiesow         |

Nexus administration:

- Lars Kiesow (uos, dce)
- Michael Stypa (uos)


Other Hosted Services
---------------------

Institution                | Hostname                      | Admin (Software) | Admin (Hardware)
---------------------------|-------------------------------|------------------|-------------------------
University of Osnabrück    | pkg.opencast.org              | Lars Kiesow      | Lars Kiesow
University of Osnabrück    | pullrequests.opencast.org     | Lars Kiesow      | Lars Kiesow
University of Osnabrück    | build.opencast.org            | Greg Logan       | Lars Kiesow
University of Osnabrück    | docs.opencast.org             | Lars Kiesow      | Lars Kiesow
University of Osnabrück    | opencast.org                  | Rüdiger Rolf     | UOS RZ


Administrators
==============

What is an administractor, and how does that differ from a committer?
-----------------------------------------------------------------

An administrator is someone within the Opencast community who has administrative access to one or more of our major
tools.  These tools are

 - JIRA
 - BitBucket
 - Google Groups
 - Crowdin
 - Github

While many of our administrators are committers, an administrator is _not_ a committer by necessity.  Administrators
have important responsibilities within the community, but mainly work behind the scenes.  These responsibilities
include:

 - Adding new committers to the relevant group(s)
 - Removing old committers from the relevant group(s)
 - Contacting support when required for hosted projects (Atlassian, Crowdin, Google)

Adding or removing Committers
-----------------------------

While the committer body manages its own membership, it does not directly have the power to add or remove users
from the appropriate groups across all of our hosted products.  Administrators are required to modify the various
groups in multiple places when a change is necessary.  These changes are

 - Modifying the [JIRA committers group](https://opencast.jira.com/admin/groups/view?groupname=committers-matterhorn)
 - Modifying the [BitBucket committers group](https://bitbucket.org/account/user/opencast-community/groups/opencast-committers/)
 - Modifying the [Google committers group](https://admin.google.com/opencast.org/AdminHome?hl=de&pli=1&fral=1&groupId=committers@opencast.org&chromeless=1#OGX:Group?hl=de)
 - Modifying the [Crowdin commiters group](https://crowdin.com/project/opencast-community/settings#members)
 - Modifying the [GitHub committers group](https://github.com/orgs/opencast/teams/committers/members) upon request

Current Administrators
----------------------

Administrators may not have complete access to all services, however we will coordinate to handle requests in a timely
manner.  If you need to contact an administrator for access to one of the services above, please contact them in this
order:

 - Greg Logan
 - Lars Kiesow
 - Olaf Schulte

Account Management
------------------

The following additional people have administrative access to the following services as well

- opencast.jira.com
    - Stephen Marquard
	 - Global administrators
- bitbucket.org/opencast-community
	 - Global administrators
- github.com/opencast
	 - Global administrators
- twitter.com/openmatter
    - Lars Kiesow
- facebook.com/opencast
    - Rüdiger Rolf
- youtube.com/opencast
    - Rüdiger Rolf
- crowdin.com/project/opencast-community
    - Sven Stauber


Video Conferencing
------------------

- BigBlueButton
    - [Conference rooms](http://opencast.blindsidenetworks.net/opencast/)
        - Password: welcome
    - [Recordings](http://opencast.blindsidenetworks.net/opencast/recordings-5720cd14621.jsp)
    - Flash based
- AppearIn
    - [Conference room](http://appear.in/opencast)
    - WebRTC based; Max. 8 users
