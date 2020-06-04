Opencast Infrastructure
=======================

List of Opencast project infrastructure and administrators.  For detailed notes go [here](notes.md)

Infrastructure
--------------

### Test Servers

Institution                    | Hostname                      | Admin (Software) | Admin (Hardware)
-------------------------------|-------------------------------|------------------|------------------
University of Osnabrück        | develop.opencast.org          | Lars Kiesow      | Lars Kiesow
ETH Zürich                     | stable.opencast.org           | Lars Kiesow      | Waldemar Smirnow
Technische Universität Ilmenau | legacy.opencast.org           | Lars Kiesow      | Daniel Konrad


### CI Servers
Institution                | Hostname                             | Admin (Software) | Admin (Hardware)
---------------------------|--------------------------------------|------------------|--------------------
University of Cologne      | oc-com-admin.rrz.uni-koeln.de        | Greg Logan       | Ruth Lang
University of Cologne      | oc-com-worker1.rrz.uni-koeln.de      | Greg Logan       | Ruth Lang
University of Cologne      | oc-com-presentation.rrz.uni-koeln.de | Greg Logan       | Ruth Lang



### Maven Repository

Institution                | Hostname                      | Admin (Software) | Admin (Hardware)  | Notes
---------------------------|-------------------------------|------------------|-------------------|-----------------
Harvard DCE                | mvncache.opencast.org         | Lars Kiesow      | DCE Devel group   | Nginx cache, AWS
University of Osnabrück    | nexus.opencast.org            | Lars Kiesow      | Lars Kiesow       | nexus-oss

Nexus administration:

- Greg Logan
- Lars Kiesow


### Other Hosted Services

Institution                | Hostname                        | Admin (Software) | Admin (Hardware)
---------------------------|---------------------------------|------------------|-------------------------
University of Osnabrück    | pkg.opencast.org                | Lars Kiesow      | Lars Kiesow
University of Cologne      | ci.opencast.org                 | Greg Logan       | Ruth Lang
University of Osnabrück    | docs.opencast.org               | Lars Kiesow      | Lars Kiesow
University of Stuttgart    | testrailoc.tik.uni-stuttgart.de | Release managers | Per Pascal Grube


Administrators
--------------

### What is an administractor, and how does that differ from a committer?

An administrator is someone within the Opencast community who has administrative access to one or more of our major
tools.  These tools are

- GitHub
- Google Groups
- Crowdin

While many of our administrators are committers, an administrator is _not_ a committer by necessity.  Administrators
have important responsibilities within the community, but mainly work behind the scenes.  These responsibilities
include:

- Adding new committers to the relevant group(s)
- Removing old committers from the relevant group(s)
- Contacting support when required for hosted projects (Atlassian, Crowdin, Google)

### Adding or removing Committers

While the committer body manages its own membership, it does not directly have the power to add or remove users
from the appropriate groups across all of our hosted products.  Administrators are required to modify the various
groups in multiple places when a change is necessary.  These changes are

- Modifying the [GitHub committers group](https://github.com/orgs/opencast/teams/committers/members) upon request
- Modifying the [Google committers group](https://admin.google.com/opencast.org/AdminHome?hl=de&pli=1&fral=1&groupId=committers@opencast.org&chromeless=1#OGX:Group?hl=de)
- Modifying the [Crowdin committers group](https://crowdin.com/project/opencast-community/settings#members)
- Modifying the list of committers on the [Opencast website](https://opencast.org/people)

### Current Administrators

Administrators may not have complete access to all services, however we will coordinate to handle requests in a timely
manner. If you need to contact an administrator for access to one of the services above, please contact them in this
order:

- Greg Logan
- Lars Kiesow
- Olaf Schulte

### Video Conferencing

- BigBlueButton
    - [Conference rooms](http://opencast.blindsidenetworks.net/opencast/)
        - Password: welcome
    - [Recordings](http://opencast.blindsidenetworks.net/opencast/recordings-5720cd14621.jsp)

### Other Services

Other services and the primary contact for them:

- Google
    - Greg Logan
- Twitter
    - Lars Kiesow
- Facebook
    - Rüdiger Rolf
