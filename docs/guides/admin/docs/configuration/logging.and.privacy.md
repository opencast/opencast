Logging and Privacy
===================

The Matterhorn User-Tracking service stores actions of the users in the engage player within the database. This data is
used for the footprint feature below the player and for the optional Analytics component.

The settings for logging can be changed in the file:

    ${CONF_DIR}/services/org.opencastproject.usertracking.impl.UserTrackingServiceImpl.properties

The following options are available:

 - `org.opencastproject.usertracking.detailedtrack`
   Setting this key to true enables the user tracking javascript, setting it to false prevents the user tracking data
   from being sent. With this set to false now logging will happen at all. The footprints are not available and
   analytics will not work. Default is true.
 - `org.opencastproject.usertracking.log.ip`
   IP-addresses will no longer be logged if this is set to false. Turning this of is needed in some contries, especially
   Germany, if you don't have a permission from the user to store this data. Footprints will still work with this
   feature set to false! Default is true.
 - `org.opencastproject.usertracking.log.user`
   User login names will no longer be tracked if this is set to false. Turning this of is probably needed in most
   countries, espacially if IP-logging is still active too. Footprints will still work if this is set to false! Default
   is true.
 - `org.opencastproject.usertracking.log.session`
   Browser session-IDs will no longer be tracked is this is set to false. This is just for the completness to prevent
   any user related data. So far there is no known reason to turn this of. Footprints will still work if this is set to
   false!  Default is true.

So if you want to use the footprint features but don't want to store any user specific data you can turn the logging of
IP, username and session-ID off, without any problems. Analytics will probably not work then.

