Test Cases for a Capture Agent
==============================

These test cases can be used to test new capture agents.
As a vendor, you might want to use these to test new devices before their release.

They are meant for a setup where the capture agent is firmly installed in a venue,
it runs 24/7 and captures scheduled lectures fully automated.

|Test      | Expected Result |
| :---     |    :----:       |
|Connection to Opencast Server| Agent shows up at Opencast's admin interface with status online|
|Manual recording on agent| Successfully recording|
|Schedule recoring| Event successfully recorded and uploaded|
|Schedule multiple recordings| Events successfully recorded and uploaded|
|Record 6 hours| Event successfully recorded and uploaded|
|Network loss before recording starts| Recording starts and stops without network connection|
|Network loss while recording| Event successfully recorded and uploaded|
|Power loss| Does it start up again|
|Change input signal during recording| Event successfully recorded and uploaded|
|HTTPS| Devices supports HTTPS connections to Opencast|
