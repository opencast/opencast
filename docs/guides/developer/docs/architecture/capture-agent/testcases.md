Test Cases for Capture Agents
=============================

This document describes a set of simple test cases which can be used to test new capture agents.
As a vendor, you might want to use these to test new devices before their release.

They are meant for a setup where the capture agent is firmly installed in a venue,
it runs 24/7 and automatically captures scheduled lectures.


|Test                                 |Expected Result                                                 |
|-------------------------------------|----------------------------------------------------------------|
|Connection to Opencast Server        |Agent shows up at Opencast's admin interface with status online |
|Manual recording on agent            |Event successfully recorded and uploaded                        |
|Schedule recording                   |Event successfully recorded and uploaded                        |
|Schedule multiple recordings         |Events successfully recorded and uploaded                       |
|Record 6 hours                       |Event successfully recorded and uploaded                        |
|Network loss before recording starts |Recording starts and stops without network connection           |
|Network loss while recording         |Event successfully recorded and uploaded                        |
|Power loss                           |The capture agent starts up again                               |
|Change input signal during recording |Event successfully recorded and uploaded                        |
|HTTPS                                |Devices supports HTTPS connections to Opencast                  |


Test Servers
------------

You can use Opencast's [official test servers](https://docs.opencast.org/#test-servers) like [stable.opencast.org
](https://stable.opencast.org/), but keep in mind that these servers are reset on a daily basis at about 2am
(Europe/Berlin timezone).

The default credentials for these servers are:

- User interface
    - user: `admin`
    - password: `opencast`
- Capture agent
    - user: `opencast_system_account`
    - password: `CHANGE_ME`
