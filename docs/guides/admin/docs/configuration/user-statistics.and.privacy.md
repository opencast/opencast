User Statistics and Privacy
===========================

*There exists a newer and more complete tracking service using Matomo included [as a module](../modules/player.matomo.tracking.md).*

The Opencast User-Tracking service stores user actions of the Opencast players in the database. This data is used for
the footprint feature of the player and for the optional analytics component.

> *Note that enabling all of the tracking options may result in legal problems depending on your country's privacy laws
> and the type of service you are running.*

The settings for tracking user data can be found in:

    .../etc/org.opencastproject.usertracking.impl.UserTrackingServiceImpl.cfg

Tracking of user data can be controlled on two levels. First, tracking can be generally activated or deactivated. Second,
if it is activated, the data being tracked can be defined.

`org.opencastproject.usertracking.detailedtrack` defines if the user tracking JavaScript code is loaded and data about
user actions are being sent to and stored by Opencast. Deactivating this will effectively stop all tracking. This may
effect features like the footprints in the Opencast player.  Default: `true`.

If tracking is still activated, the following keys may be used to define the kind of data that is being tracked. The keys
have no effect if tracking is turned off.

Key                                           | Data to be tracked    | Default value
----------------------------------------------|----------------------|--------------
`org.opencastproject.usertracking.log.ip`     | IP addresses         | `false`
`org.opencastproject.usertracking.log.user`   | login names of users | `false`
`org.opencastproject.usertracking.log.session`| Browser session-IDs  | `false`

If you want to use the footprint feature but do not want to store any user specific data you can turn the tracking of IP
addresses, usernames and session-IDs off.
