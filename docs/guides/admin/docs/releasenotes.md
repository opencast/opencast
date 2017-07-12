Opencast 4: Release Notes
=========================

Opencast 3.0 has been streamlined to better address Adopters' needs when it comes to their processes. This means
the user experience and the efficiency of users performing their tasks have been significantly enhanced.
Other major improvements are new features for the Opencast Player and even more flexibiity when it comes to the
integration of Opencast into external authentication and authorization systems.

New Features and Improvements
-----------------------------


Capture Agent API Changes
-------------------------

- The new scheduler now creates a media package for each event which holds all assets as soon as the schedule is
  created. This makes it unnecessary for any capture agent to download and re-ingest any media package elements unless
  they are modified by the capture agent. Note that re-ingested media package elements will overwrite scheduled
  elements. Hence a capture agent may modify these data. If not required for the inner workings of the capture agent, we
  advise to not download, modify and upload any media package elements to avoid errors.
