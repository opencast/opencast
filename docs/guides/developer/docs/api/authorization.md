# Introduction

The Application API can be accessed in two different ways: Either using a single dedicated user with access to everything (“super user”) or by implementing more fine grained access through user and role switching upon every request (“user switching” or “sudo” execution mode), where the request is executed in the name and using the roles of the specified user.

The first method is ideal for scenarios where the end users of the external application are not managed in Opencast. The downside of this approach is a potential security risk as well as the inability to audit and track changes made by the external applications back to the actual user who actually triggered the changes. The second method is more cumbersome to implement but leads a much improved control and assessment of security.

# Delegation of Authorization

In situations where the provider of the API offers a super user who is allowed “sudo” requests that are executed on behalf of another user, the API is actually delegating authorization to the client application. In this cause authorization is performed upon login of the super user, but then the super user can switch to any other user or any set of roles (with a few exceptions for security reasons).

Note that in order to allow for user switching, a specific role needs to be assigned to the super user, and that role cannot be obtained by manipulating the role set (see [Role switching](#role-switching)).

## User switching

When working with a super user, it is considered a best practice to specify a dedicated execution user upon each request whenever possible and reasonable. This way, creation or modification of resources can later be audited and mapped back to that user if needed.

The execution user can be specified by setting the `X-RUN-AS-USER` request header with the user name as its value, as seen in this sample request:

Request           | Headers
:-----------------|:------------------------------------------------------------
`GET /api`        | `X-RUN-AS-USER: john.doe`



Switching to another user can potentially fail for various reasons: The user might not exist or may not be allowed to switch to due to potential privilege escalation, or the current user might not be allowed to switch users at all.

If the request to switch to another user fails, the following response codes are returned:

Response code             | Comment
:-------------------------|:---------------------------------------------------
`403 (FORBIDDEN)`         | The current user is not allowed to switch users
`403 (FORBIDDEN)`         | The user cannot be switched to due to potential escalation of privileges
`412 Precondition failed` | The user specified in the X-RUN-AS-USER header does not exist


## Role switching

Rather than specifying an execution user, the client might choose to specify a set of roles that should be used when executing the request. This technique is recommended in cases where the users are not managed by the API. By specifying a set of roles, the corresponding request will be executed using the API’s anonymous user but equipped with the specified set of roles.

The execution user’s roles can be specified by setting the `X-RUN-WITH-ROLES` request header with the set of roles as its value and with individual roles separated by comma, as seen in this sample request:

Request           | Headers
:-----------------|:------------------------------------------------------------
`GET /api`        | `X-RUN-WITH-ROLES: ROLE_A,ROLE_B`

Switching to a set of roles can potentially fail for various reasons: The role may not be granted to due to potential privilege escalation, or the current user might not be allowed to switch roles at all.
If the request to apply a set of roles fails, the following response codes are returned:

Response code             | Comment
:-------------------------|:---------------------------------------------------
`403 (FORBIDDEN)`         | The current user is not allowed to switch roles
`403 (FORBIDDEN)`         | The roles cannot be granted to due to potential escalation of privileges


# Best practice

## One user per external application

As a best practice, the API provider should create one super user per external application and tenant, so that access through that super user can be controlled, limited and turned off individually for each external application and tenant.


## Preference for user and role switching

Client implementations accessing the API through a super user are urged to implement and enforce user and role switching as much as possible, since it allows for auditing of user activity on the API and introduces less risk by running requests with a limited set of privileges.

Obviously, if all requests are executed using the super user directly, it is not possible to track which user initiated a given action.

# Access Control

Most events in Opencast come with an access control list (ACL), containing entries that map actions to roles, either allowing or denying that action. Opencast currently only supports the ability to explicitly allow an action and consider everything else to be denied.

## Roles

When a user authenticates against Opencast, it is assigned its set of roles that determine the user's access to Opencast data entities. There are multiple ways to associate roles with a user:

1. Explicit assignment directly to the user
2. Directly through membership in groups (ROLE_GROUP_&lt;group name&gt;)
3. Indirectly through membership in groups (whatever roles have been assigned to the group)

In addition, a special role is assigned that uniquely identifies a user ("user role"). The user role can be determined by
evaluating the `userrole` attribute in the Base API's call to [/info/me](base-api/#get-apiinfome).
