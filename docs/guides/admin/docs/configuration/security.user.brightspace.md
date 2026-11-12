> Using this requires you to turn on an Opencast plugin.
> Take a look [at the plugin management documentation](plugin-management.md) to find out how you can do that.

### What it does

The [Brightspace](https://www.d2l.com/) User Provider enriches Opencast users with a
set of roles made up of the user's membership in Brightspace courses, of the form
`ROLE_courseID`. For example, an Opencast user who is also a Brightspace user and a
member of the Brightspace course `myCourseID` will be granted the Opencast role `ROLE_myCourseID`.


### Requirements

The Brightspace User Provider uses the Brightspace REST API (version 1.31 of the Learning Platform API) with the ID-Key
authentication of D2L. The Brightspace URL therefore has to start with `https://`, and you need the following from your
Brightspace administrator:

- **Application ID and key**: These identify Opencast as an application towards Brightspace. They are issued when
  registering an application with D2L.

- **System user ID and key**: The provider makes its requests in the name of a Brightspace user, the system user. The ID
  and key of that user are issued when the user authorizes the application to act on their behalf.

- **Sufficient permissions for the system user**: The role of the system user needs at least the following permissions.
  Otherwise Brightspace rejects requests or leaves out data, and users will lack some of their roles in Opencast.

    - **See the user name of other users** (in the user information privacy settings): Required for looking up a user
      by user name.
    - **View user enrollments** and **Search for {role name}** for all roles your users have in courses: Required for
      getting the courses and roles of a given user. Brightspace only returns the enrollments which the system user
      could also see.

The provider only reads data and never changes anything in Brightspace.

Note: D2L has deprecated ID-Key authentication in favor of OAuth 2.0 and classifies older versions of the Learning
Platform API, which includes version 1.31, as obsolete. D2L may remove access to them from Brightspace. Ask your
Brightspace administrator whether your instance still supports both.


### Step 1: Enable the User Provider

Edit `etc/org.opencastproject.plugin.impl.PluginManagerImpl` and make sure the
`opencast-plugin-userdirectory-brightspace` plugin is enabled.

### Step 2: Configure the User Provider

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

### Step 3: Verify Granted Access

Verify that the Brightspace User Provider starts up with the correct Brightspace URL by looking
for a log entry like this:

```
(BrightspaceUserProviderInstance:143) - Creating new BrightspaceUserProviderInstance(pid=org.opencastproject.userdirectory.brightspace.378cdff4-825f-4b60-b1ed-33f75aa7f265, url= ... , cacheSize=1000, cacheExpiration=60)
```

Then login to Opencast using a username which also exists in your Brightspace system.
Verify the roles granted to the user by opening the URL
OPENCAST-URL/info/me.json in a new browser tab, or navigate to the user details
and open the tab "Effective Roles".

If necessary, you can increase the logging detail from the Brightspace user provider
by adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j.logger.org.opencastproject.userdirectory.brightspace=DEBUG
```
