### What it does

The [Moodle](https://moodle.org/) User Provider enriches Opencast users with a
set of roles made up of the user's membership in Moodle courses, of the form
COURSEID_Role. For example, an Opencast user who is also a Moodle user and a
member of the Moodle course `myCourseID` with the Moodle capability
`tool/opencast:learner` will be granted the Opencast role `myCourseID_Learner`.
Analogously, users with the capability `tool/opencast:instructor` will receive
the Opencast role `myCourseID_Instructor`. Note that by default, Moodle course
IDs are opaque ID values such as `10765`. The `ROLE_GROUP_MOODLE` Opencast group
role is granted to all users that also exist in Moodle.

The mapping of Moodle courses and capabilities to Opencast roles is consistent
with the course and role mapping used by the [LTI](../modules/ltimodule.md)
endpoint. The Moodle User Provider can therefore be used with LTI or another
method of authenticating users.

The Moodle Role Provider allows Moodle course and capability combinations to be
used in Event and Series ACLs. For example, the role `myCourseID_Learner` can be
added to a Series ACL to grant access to the Series to members of the
`myCourseID` course in Moodle.

### Requirements

The Moodle User Provider requires the
[moodle-tool_opencast](https://github.com/unirz-tu-ilmenau/moodle-tool_opencast)
plug-in that extends Moodle with the necessary API functions and capabilities.
As this plug-in also provides base settings for additional Moodle plug-ins, the
user is asked to provide Opencast API login information during the installation.
The values can be arbitrary, if only the Moodle User Provider should be
configured.

After the installation, a new user with the capabilities
`webservice/rest:use`, `tool/opencast:externalapi`, `moodle/user:viewalldetails`,
`moodle/user:viewdetails` and `moodle/site:accessallgroups` has to be created.
Then generate a new web service token and add that user to the "Opencast web
service" service.

### Step 1

Edit `etc/org.apache.karaf.features.cfg` and make sure the `opencast-moodle` feature is listed in the `featuresBoot`
option.

### Step 2

To enable the Moodle User Provider, copy and rename the bundled configuration
template from
`OPENCAST/etc/org.opencastproject.userdirectory.moodle-default.cfg.template` to
`OPENCAST/etc/org.opencastproject.userdirectory.moodle-default.cfg`

Edit the configuration file to set your Moodle URL and the web service token of
the Moodle user that should be used for API calls.

```
# The URL and token for the Moodle REST webservice
org.opencastproject.userdirectory.moodle.url=http://localhost/webservice/rest/server.php
org.opencastproject.userdirectory.moodle.token=mytoken1234abcdef
```

### Step 3

Verify that the Moodle User Provider starts up with the correct Moodle URL by looking
for a log entry like this:

```
(MoodleUserProviderInstance:143) - Creating new MoodleUserProviderInstance(pid=org.opencastproject.userdirectory.moodle.378cdff4-825f-4b60-b1ed-33f75aa7f265, url=http://localhost/webservice/rest/server.php, cacheSize=1000, cacheExpiration=60)
```

Then login to Opencast using a username which also exists in your Moodle system.
Verify the roles granted to the user by opening the URL
OPENCAST-URL/info/me.json in a new browser tab, or navigate to the user details
and open the tab "Effective Roles".

If necessary, you can increase the logging detail from the Moodle user provider
by adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j.logger.org.opencastproject.userdirectory.moodle=DEBUG
```

### Step 4

You can grant additional roles to all Moodle users in Opencast by creating a
group with the name 'Moodle'. You can then add additional roles to this group,
which will be inherited by all Moodle users.

You can also use the group role name `ROLE_GROUP_MOODLE` in Event or Series
ACLs.
