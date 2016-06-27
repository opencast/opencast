Access Control List Configuration
=================================

This document describes configuration options considering the access control lists (ACL) used by
Opencast for authorization.

## ACL Templates

On startup, Opencast loads all ACL templates found in `/opt/opencast/etc/acl/`.

Notes:

* ACL templates can also be created and managed directly in the Admin UI

## Additional ACL Actions

Opencast uses to ACL actions to authorize roles to perform specific actions on a given object:

* `read` allows the role to access to object
* `write` allows the role to modify the object

Those built-in actions are known to Opencast.

In case you need other ACL actions, you can configure additional ACL actions in
`/opt/opencast/etc/listprovides/acl.additional.actions.properties`. Those additional ACL actions are not affecting the way
Opencast treats objects but are simply just forwarded to publication channels so that third-party applications
(expecting those specific ACL actions) can implement the respective authorization logic.

Example:

    list.name=ACL.ACTIONS
    # This list provider allows you to configure custom actions that can be added
    # to ACLs. The default actions are read and write.
    # The pattern for adding them is
    # UI_LABEL=actionId
    #
    Upload=myorg_upload
    Download=myorg_downlaod

In the example above, the two additional ACL actions `Upload` and `Download` have been configured.
The ACL editor of the Admin UI will allow the user to set those actions.

Notes:

* To ensure compatibility with future Opencast versions, it is highly recommended to use a prefix for your
customized additional actions in case later Opencast versions would introduce an action with the same name