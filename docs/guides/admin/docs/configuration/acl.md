Access Control List Configuration
=================================

This document describes how Opencast stores and handles access control settings for series and episodes and what
configuration options related to this are available.

Access Control Lists
--------------------

An access control list (ACL) in the context of Opencast consists of a global deny rule (no one is allowed access) and a
set of roles with rules attached to define access. Hence, it is effectively a white-listing of roles to grant access and
it means that all roles and/or actions not defined in an access control list are denied access.

For example, the following rule defines read access for role 1 and read/write access for role 2:

|role |action|access|
|-----|------|------|
|ROLE1|read  |true  |
|ROLE2|read  |true  |
|     |write |true  |

Opencast can also deny access locally (e.g. deny write access for role 1) which can be interesting if merging of ACLs is
used. But this is not handled in the user interface and using this should therefore be avoided.

System administrators are an exception to these rules. A user with `ROLE_ADMIN` will always be granted access,
regardless of the rule-set attached to an event. Organizational administrators are also granted access in some cases.


Global Rules
------------

In case an event has no custom access control list defined, a global rule set is associated with the event. The global
rules consist only of the general deny rule. Hence, no access is allowed to anyone except administrators..


Series and Episode Rules
------------------------

Access control lists can be specified both on series and on episode level. This means that multiple rule sets can be
attached to an episode which is part of a series. Opencast can handle this in multiple ways.

The handling is specified by the merge mode configured in 
`etc/org.opencastproject.authorization.xacml.XACMLAuthorizationService.cfg`. It
defines the relationship between series and episode access control lists, if both are attached to an event. If only one
list is attached, its rules are always active. If multiple lists are attached, the following modes define Opencast's
behavior:

### Merge Mode “override” (Default)

The episode ACL takes precedence over the series ACL. This means that the series ACL will be completely ignored as soon
as the episode has an ACL, no matter what rules are set in either. This allows users to define general rules for a
series which can be completely redefined on an episode and which are not influenced by changes later made to the series.
This is also a very simple rule and thus easy to understand.

Example:

|         |ROLE1 |       |ROLE2 |       |ROLE3 |       |
|---------|------|-------|------|-------|------|-------|
|         |*read*|*write*|*read*|*write*|*read*|*write*|
|*series* |allow |allow  |allow |allow  |      |       |
|*episode*|      |       |allow |       |allow |       |
|*active* |      |       |allow |       |allow |       |

### Merge Mode “roles”

Series and episode ACLs are merged based on the roles defined within. If both the series and the episode define a rule
for a specific role (user or group), the episode's rule takes precedence. Rules for roles defined in one ACL only are
always part of the resulting active ACL.

Example:

|         |ROLE1 |       |ROLE2 |       |ROLE3 |       |
|---------|------|-------|------|-------|------|-------|
|         |*read*|*write*|*read*|*write*|*read*|*write*|
|*series* |allow |allow  |allow |allow  |      |       |
|*episode*|      |       |allow |       |allow |       |
|*active* |allow |allow  |allow |       |allow |       |

### Merge Mode “actions”

ACLs are merged based on the actions (read, write, …) contained within both ACLs. If a rule is specified for a tuple of
role and action in both ACLs, the rule specified in the episode ACL takes precedence.

Example:

|         |ROLE1 |       |ROLE2 |       |ROLE3 |       |
|---------|------|-------|------|-------|------|-------|
|         |*read*|*write*|*read*|*write*|*read*|*write*|
|*series* |allow |allow  |allow |allow  |      |       |
|*episode*|      |       |allow |       |allow |       |
|*active* |allow |allow  |allow |allow  |allow |       |

### Switching Modes

Switching modes is not necessarily simple since access control lists are cached at several places. Hence, while changing
this value will have an immediate effect on newly processed videos, an index rebuild is inevitable to update cached data
and republications to update old events may be necessary.


Updating Series Permissions
---------------------------

Depending on the admin interface configuration in `etc/org.opencastproject.organization-mh_default_org.cfg`, the admin
interface behaves differently when series access control lists are modified and may also overwrite episode rules of that
series. Possible modes of operation are:

- *always:*
  When modifying series permissions, automatically remove all permission rules specific to single episodes belonging to
  the series. This enforces that every episode has the rules of the series in effect as soon as they are changed.
- *never:*
  Only update the series permissions but never replace permissions set on event level. This may mean that updated rules
  have no effect on already existing events.
- *optional (default):*
  Like `never` but present users with a button in the series permission dialog which allows them to replace the event
  specific permissions easily if they want to.


Templates
---------

Templates of access control lists can be specified for the administrative user interface. They are a convenient way to
apply a defined set of rules all at once instead of applying each rule one after another. Templates stored in `etc/acl/`
are loaded at start-up for all organizations. Templates can also be created and managed in the admin interface.


Additional Actions
------------------

Opencast uses two default actions for access authorization on events:

- `read` allows a role to access (read the value of) objects
- `write` allows a role to modify (write to) objects

More actions can be added but are usually ignored by Opencast. Though they may be handy to specify rules for external
applications.

In case you need other actions, you can configure the admin interface to allow adding additional ones. These are
configured in `etc/listprovides/acl.additional.actions.properties`. For example, this would configure the two actions,
`Upload` and `Download`, to be available in the permission editor of the admin interface:

```properties
list.name=ACL.ACTIONS
# This list provider allows you to configure custom actions that can be added
# to ACLs. The default actions are read and write.
# The pattern for adding them is
# UI_LABEL=actionId
#
Upload=myorg_upload
Download=myorg_downlaod
```

Using a unique prefix for your custom actions like this example did with `myorg_` is recommended to make it unlikely
that later Opencast versions introduce the same action in a different context.
