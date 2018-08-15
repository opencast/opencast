# Advanced

This section describes facilities supposed to be used by advanced users.

### Unprivileged Users

Privileged users are users that own the global administration role (`ROLE_ADMIN`) which grants them access
to all data and all functionality Opencast offers.

Unprivileged users are users that don't own the global administration role. For unprivileged users, Opencast offers
two mechanisms to configure their privileges:

- [Capture Agent Access Control](capture-agent-access.md) can be used to configure the Admin UI to provide selective
  access to capture agents for different users or groups of users
- [Role-Based Visibility](role-based-visibility.md) can be used to configure the Admin UI to provide selective access
  to Opencast's functionality for different users or groups of users