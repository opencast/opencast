# Capture Agent Access Control

Opencast allows restrictions considering what a user or group of users can do with capture agents to be configured.

There are three kinds of access restriction levels:

- Unrestricted Access: Full access to all capture agents
- Restricted Access: No access to any capture agent
- Selective Access: Selected access to a subset of capture agents

### Unrestricted Access

Users that have the global administration role (`ROLE_ADMIN`) or the organization administration role
(value depends on configuration) always have access to all capture agents.

### Restricted Access

Unprivileged users by default have no permission to modify anything that is relevant to control capture agents.
Those users cannot:

- Edit scheduling information of scheduled events (Events->Event Details->Scheduling is read-only)
- Edit workflow configuration of scheduled events (Events->Event Details->Processing is read-only)
- Schedule new events (Events->Add Event->Source will only allow upload)
- Delete scheduled events (Events->Actions->Delete and Events->Action->Delete)
- Remove capture agents (Locations->Locations->Action->Remove)

### Selective Access

It is possible to selectively allow specific users or groups of users to access subsets of capture agents.

Each capture agent has a role which is derived from its id by removing all non-alphanumerical characters and
prepending the prefix `ROLE_CAPTURE_AGENT_`.
If a user with restricted access owns the roles of a set of given capture agents, the user has selective access
to those capture agents.

**Example**

- Capture agent ID: `Opencast's Capture Agent`
- Derived role: `ROLE_CAPTURE_AGENT_OPENCASTSCAPTUREAGENT`

If the user Bob with restricted access has the role `ROLE_CAPTURE_AGENT_OPENCASTSCAPTUREAGENT`, Bob is allowed
to control the capture agent with the id `Opencast's Capture Agent`.
