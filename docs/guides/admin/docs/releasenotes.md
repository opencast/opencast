Opencast 4: Release Notes
=========================

Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|Sep 22th                     |Feature Freeze
|Sep 25th - Oct 13th          |Internal QA and bug fixing phase
|Oct 16th - Nov 3th           |Public QA phase
|Nov 6th  - Nov 24th          |Additional bug fixing phase
|Nov 20th - Nov 24th          |Translation week
|Nov 27th - Nov 3rd           |Final QA phase
|Dec 7th                      |Release of Opencast 4

New Features and Improvements
-----------------------------


Capture Agent API Changes
-------------------------

- The new scheduler now creates a media package for each event which holds all assets as soon as the schedule is
  created. This makes it unnecessary for any capture agent to download and re-ingest any media package elements unless
  they are modified by the capture agent. Note that re-ingested media package elements will overwrite scheduled
  elements. Hence a capture agent may modify these data. If not required for the inner workings of the capture agent, we
  advise to not download, modify and upload any media package elements to avoid errors.
