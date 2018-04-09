Logging and Privacy
===================

The Opencast User-Tracking service stores user actions of the Opencast players in the database. This data is used for
the footprint feature of the player and for the optional analytics component.

> *Note that enabling all of the logging options may result in legal problems depending on your country's privacy laws
> and the type of service you are running.*

The settings for logging user data can be found in:

    .../etc/org.opencastproject.usertracking.impl.UserTrackingServiceImpl.cfg

Logging of user data can be controlled on two levels. First, logging can be generally activated or deactivated. Second,
if it is activated, the data being logged can be defined.

`org.opencastproject.usertracking.detailedtrack` defines if the user tracking JavaScript code is loaded and data about
user actions are being sent to and stored by Opencast. Deactivating this will effectively stop all logging. This may
effect features like the footprints in the Opencast player.  Default: `true`.

If logging is still activated, the following keys may be used to define the kind of data that is being logged. The keys
have no effect if logging is turned off.

Key                                           | Data to be logged    | Default value
----------------------------------------------|----------------------|--------------
`org.opencastproject.usertracking.log.ip`     | IP addresses         | `false`
`org.opencastproject.usertracking.log.user`   | login names of users | `false`
`org.opencastproject.usertracking.log.session`| Browser session-IDs  | `false`

If you want to use the footprint feature but do not want to store any user specific data you can turn the logging of IP
addresses, usernames and session-IDs off.
