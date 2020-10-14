### What it does

The [Canvas LMS](https://www.instructure.com/canvas/) User Provider enriches Opencast users
with a set of roles made up of the user's membership in Canvas sites, of the form
SITEID_Role. For example, an Opencast user who is also a Canvas user and a member
of the Canvas site `CourseID` with the Canvas role `Student` will be granted the
Opencast role `CourseID_Learner`.

The mapping of Canvas sites and roles to Opencast roles is consistent with the site
and role mapping used by the [LTI](../modules/ltimodule.md) endpoint. The Canvas
User Provider can therefore be used with LTI or another method of authenticating
users.

The Canvas Role Provider allows Canvas site and role combinations to be used in
Event and Series ACLs. For example, the role `CourseID_Learner` can be added to a
Series ACL to grant access to the Series to members of the `CourseID` site in Canvas.

### Requirements

The Canvas User Provider requires a token of a user who has at least the following 
permissions of an account role in a Canvas instance.

- **Users - manage login details**: Required for getting site list of a given user.

- **Users - view list**: Required for getting details of a given user.

### Step 1

Edit `etc/org.apache.karaf.features.cfg` and make sure the `opencast-canvas` feature is listed in the `featuresBoot`
option.

### Step 2

To enable the Canvas User Provider, copy and rename the bundled configuration template from
`OPENCAST/etc/org.opencastproject.userdirectory.canvas.CanvasUserRoleProvider.cfg.template` to
`OPENCAST/etc/org.opencastproject.userdirectory.canvas.CanvasUserRoleProvider.cfg`

Edit the configuration file to set your Canvas URL, and the token of the admin user:

```
# The URL and login token for Canvas LMS
org.opencastproject.userdirectory.canvas.url=https://demo.instructure.com/
org.opencastproject.userdirectory.canvas.token=token_of_a_user_with_sufficient_privilege

```

### Step 3

Verify that the Canvas User Provider starts up with the correct Canvas URL by looking
for a log entry like this:

```
(CanvasUserRoleProvider:116) - Activating CanvasUserRoleProvider(url=https://demo.instructure.com, cacheSize=1000, cacheExpiration=60, instructorRoles=[teacher, ta], ignoredUserNames=[admin, anonymous]
```

Then login to Opencast using a username which also exists in your Canvas system.
Verify the roles granted to the user by opening the URL OPENCAST-URL/info/me.json
in a new browser tab.

If necessary, you can increase the logging detail from the Canvas user provider by
adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j2.logger.canvas.name = org.opencastproject.userdirectory.canvas
log4j2.logger.canvas.level = DEBUG
```

### Step 4

You can use the group role name `ROLE_GROUP_CANVAS` in Event or Series ACLs for all Canvas users
and `ROLE_GROUP_CANVAS_INSTRUCTOR` for all Canvas instructors.
