### What it does

The [Canvas](https://www.canvaslms.com/) User Provider enriches Opencast users
with a set of roles made up of the user's membership in Canvas courses, of the form
COURSEID_Role. For example, an Opencast user who is also a Canvas user and a member
of the Canvas site `mysiteid` with the Canvas role `Student` will be granted the
Opencast role `mysiteid_Learner`. Note that Canvas site IDs are opaque
numerical values such as `1234`.

The mapping of Canvas sites and roles to Opencast roles is consistent with the course
and role mapping used by the [LTI](../modules/ltimodule.md) endpoint. The Canvas
User Provider can therefore be used with LTI or another method of authenticating
users.

The Canvas Role Provider allows Canvas site and role combinations to be used in
Event and Series ACLs. For example, the role `mysiteid_Learner` can be added to a
Series ACL to grant access to the Series to members of the `mysiteid` site in Canvas.

### Requirements

The Canvas User Provider requires an API access token generated for an admin-level
account on the Canvas instance.

### Step 1

To enable the Canvas User Provider, copy and rename the bundled configuration template from
`OPENCAST/etc/org.opencastproject.userdirectory.canvas-default.cfg.template` to
`OPENCAST/etc/org.opencastproject.userdirectory.canvas-default.cfg`

Edit the configuration file to set your Canvas URL, and the token of your admin-level account
on the Canvas system:

```
canvas.url=https://canvas.my.domain
canvas.token=token_here
```

### Step 2

Verify that the Canvas User Provider starts up with the correct Canvas URL by looking
for a log entry like this:

```
(CanvasUserProviderInstance:159) - Creating new CanvasUserProviderInstance(pid=org.opencastproject.userdirectory.canvas.56f9fbca-71a7-4956-94d8-5f01dbbdc588, url=https://canvas.my.domain, courseIdentifierProperty=id, coursePattern=null, userPattern=null, userNameProperty=short_name, instructorRoles=[teacher, Associate tutor], cacheSize=1000, cacheExpiration=0)
```

Then launch an Opencast LTI tool from within Canvas as a user with course memberships.
Verify the roles granted to the user by opening the url OPENCAST-URL/info/me.json
in a new browser tab.

If necessary, you can increase the logging detail from the Canvas user provider by
adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j2.logger.userdirectory-canvas.name = org.opencastproject.userdirectory.canvas
log4j2.logger.userdirectory-canvas.level = DEBUG
```

### Step 3

You can grant additional roles to all Canvas users in Opencast by creating a group
with the title 'Canvas'. You can then add additional roles to this group, which will
be inherited by all Canvas users.

You can also use the group role name ROLE_GROUP_CANVAS in Event or Series ACLs.

