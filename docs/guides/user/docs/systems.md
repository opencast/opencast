# Locations/Rooms
The Locations page is accessible from the **Main Menu > Capture**. This page lists all capture agents that are connected to Opencast.

# Jobs
The Jobs page is accessible from the **Main Menu > Systems > Jobs**. This page lists all jobs that are currently running on the installation.

# Servers
The Servers page is accessible from the Main **Menu > Systems > Servers**. This page lists all servers that are used by Opencast.
Servers can be set on Maintenance mode by using the toggle in the Actions column. The maintenance mode means that the corresponding server will finish its work on the currently running jobs, and no new job will be accepted until the maintenance mode is removed.

# Services
The Services page is accessible from the **Main Menu > Systems > Services**. This page lists all services that are run by Opencast.
In case of a service failure, the clear icon (⟲) will be displayed in the Actions column. Opencast will put a service into "error" mode when it has suffered from processing failures for two consecutive recordings.


## How to clear service
Click on the icon to reset the service to normal once you are done with the analysis of the problem and have made sure that there is no permanent issue on the corresponding Opencast node that would prevent successful further processing.
