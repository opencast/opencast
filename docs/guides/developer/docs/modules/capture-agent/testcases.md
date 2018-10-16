Test Cases for a Capture Agent
==============================

These Testcases are used to Test a new Captureagent.
Its for a setup where the Capture agent is firmly installed in a venue.
It runs 24/7 and captures scheduled Lecutres full automated.

|Test      | Expected Result |
| :---     |    :----:       |
|Connection to Opencast Server| CA shows up at Opencast Adminui with status online|
|Start manual recording on CA| Successfull recording|
|Sheduled recoring| Successfull Event recorded and uploaded|
|Shedule muiltiple recordings| Event successfull recorded and uploaded|
|Record 6 hours| Event successfull recorded and uploaded|
|Network loss| Recording starts and stops without network connection|
|Network loss during Recording| Event successfull recorded and uploaded|
|Powerloss| does it start up again|
|Change input signal during recording| Event successfull recorded and uploaded|
|HTTPS| Deives Supports HTTPS connection to Opencast|
