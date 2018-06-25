# Advanced

This section describes facilities supposed to be used by advanced users.

### Unprivileged Users

If a user owns the global administration role (`ROLE_ADMIN`), he is a privileged users in means of having access
to all data and all functionality Opencast offers.

Unprivileged users are users that don't own the global administration role. For unprivileged users, Opencast offers
two mechanisms to configure their privileges:

- [Capture Agent Access Control](capture-agent-access.md) can be used to configure the Admin UI to provide selective
  access to capture agents for different users or groups of users
- [Role-Based Visibility](role-based-visibility.md) can be used to configure the Admin UI to provide selective access
  to Opencast's functionality for different users or groups of users