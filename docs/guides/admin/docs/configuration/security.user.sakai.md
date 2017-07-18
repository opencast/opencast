### What it does

The [Sakai](https://www.sakaiproject.org/) User Provider enriches Opencast users
with a set of roles made up of the user's membership in Sakai sites, of the form 
SITEID_Role. For example, an Opencast user who is also a Sakai user and a member
of the Sakai site `mysiteid` with the Sakai role `Student` will be granted the
Opencast role `mysiteid_Learner`. Note that by default, Sakai site IDs are opaque
GUID values such as `d02f250e-be2d-4b72-009a-161d66ed6df9`.

The mapping of Sakai sites and roles to Opencast roles is consistent with the site
and role mapping used by the [LTI](../modules/ltimodule.md) endpoint. The Sakai
User Provider can therefore be used with LTI or another method of authenticating
users.

The Sakai Role Provider allows Sakai site and role combinations to be used in
Event and Series ACLs. For example, the role `mysiteid_Learner` can be added to a
Series ACL to grant access to the Series to members of the `mysiteid` site in Sakai.

### Requirements

The Sakai User Provider requires Sakai 11.0 or later, and an admin-equivalent 
account on the Sakai instance.

### Step 1

To enable the Sakai User Provider, copy and rename the bundled configuration template from 
`OPENCAST/etc/org.opencastproject.userdirectory.sakai-default.cfg.template` to 
`OPENCAST/etc/org.opencastproject.userdirectory.sakai-default.cfg`

Edit the configuration file to set your Sakai URL, and the username and password of
the admin user on the Sakai system:

```
sakai.url=https://mysakai.my.domain
sakai.user=opencast
sakai.password=CHANGE_ME
```

### Step 2

Verify that the Sakai User Provider starts up with the correct Sakai URL by looking
for a log entry like this:

```
(SakaiUserProviderInstance:154) - Creating new SakaiUserProviderInstance(pid=org.opencastproject.userdirectory.sakai.f1fad141-8cc8-41ee-b514-8dad00984af6, url=https://mysakai.my.domain, cacheSize=1000, cacheExpiration=60)
```

Then login to Opencast using a username which also exists in your Sakai system.
Verify the roles granted to the user by opening the url OPENCAST-URL/info/me.json
in a new browser tab.

If necessary, you can increase the logging detail from the Sakai user provider by
adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j.logger.org.opencastproject.userdirectory.sakai=DEBUG
```

### Step 3

You can grant additional roles to all Sakai users in Opencast by creating a group
with the title 'Sakai'. You can then add additional roles to this group, which will
be inherited by all Sakai users.

You can also use the group role name ROLE_GROUP_SAKAI in Event or Series ACLs.

