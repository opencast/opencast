### What it does

The [Brighspace](https://www.d2l.com/) User Provider enriches Opencast users with a
set of roles made up of the user's membership in Brightspace courses, of the form
ROLE_courseID. For example, an Opencast user who is also a Brightspace user and a
member of the Brightspace course `myCourseID` will be granted the Opencast role `ROLE_myCourseID`.


### Requirements



### Step 1

To enable the Brightspace User Provider, copy and rename the bundled configuration
template from
`OPENCAST/etc/org.opencastproject.userdirectory.brightspace-default.cfg.template` to
`OPENCAST/etc/org.opencastproject.userdirectory.brightspace-default.cfg`

Edit the configuration file to set your Brightspace URL and the credentials needed for making authenticated API calls.

```
# The organization for this provider
org.opencastproject.userdirectory.brightspace.org=mh_default_org

# The URL for the Brightspace REST webservice
org.opencastproject.userdirectory.brightspace.url=https://brightspace-api

# properties for authentication in brightspace api
org.opencastproject.userdirectory.brightspace.systemuser.id=system-user-id
org.opencastproject.userdirectory.brightspace.systemuser.key=system-user-key
org.opencastproject.userdirectory.brightspace.application.id=application-id
org.opencastproject.userdirectory.brightspace.application.key=application-key


# The maximum number of users to cache
#org.opencastproject.userdirectory.brightspace.cache.size=1000

# The maximum number of minutes to cache a user
#org.opencastproject.userdirectory.brightspace.cache.expiration=60
```

### Step 2

Verify that the Brightspace User Provider starts up with the correct Brightspace URL by looking
for a log entry like this:

```
(BrightspaceUserProviderInstance:143) - Creating new BrightspaceUserProviderInstance(pid=org.opencastproject.userdirectory.brightspace.378cdff4-825f-4b60-b1ed-33f75aa7f265, url= ... , cacheSize=1000, cacheExpiration=60)
```

Then login to Opencast using a username which also exists in your Brightspace system.
Verify the roles granted to the user by opening the url
OPENCAST-URL/info/me.json in a new browser tab, or navigate to the user details
and open the tab "Effective Roles".

If necessary, you can increase the logging detail from the Brightspace user provider
by adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j.logger.org.opencastproject.userdirectory.brightspace=DEBUG
```
