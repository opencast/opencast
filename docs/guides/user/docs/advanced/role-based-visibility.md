# Role-Based Visibility

Opencast supports a powerful mechanism that allows to provide users selective access to the administrative user
interface. Using that mechanism, it is possible to configure what parts of the UI (and therefore functionality)
are visibile to a given user based on the user's roles. Hence, we call that mechanism role-based visibility.

## How To Use

The best practise to assign a larger set of roles to a user is to use Opencast's support for user groups. This
way, you can define a group whose roles provide access to the parts of the UI you want to provide access to. Given
such a group, you can just add users to that group and they will get all the roles of the group which includes
roles that allow the users to access specific parts of the UI.

Please consult the [Groups section](../groups.md) for information about adding new groups.

There is a set of so-called user interface roles, each of them providing access to a specific part of the
administrative user interface. Those roles can be easily identified by their name prefix *ROLE_UI*.

**Important** *ROLE_ADMIN* implicitly provides full access to the user interface. When working with role-based
visibility, users (and the groups they belong to) may not have *ROLE_ADMIN* therefore.

## User Interface Roles

This section describes which roles permit access to what parts of the user interface. Note that role-based visibility
is not just hiding graphical elements from users, it does also protect server-side resources from unauthorized access
if necessary.

It is important to understand that the UI roles are not coupled, i.e. having a particular role does not imply any other
permission than the one exactly provided by the role.

Example: Just having the role *ROLE_UI_NAV_CAPTURE_VIEW* won't display the navigation menu. This requires the role
*ROLE_UI_NAV*, too.

The advantage of having independent roles is that it makes role-based visibility even more flexible, For example, it is
possible to not use the navigation menu at all. The drawback of this approach is that it makes configuration more
advanced. But configuring role-based visibility is not a daily tasks.

### General Access

**Important:** *ROLE_ADMIN_UI* is required for accessing the admin ui in general in means of providing access to
some often used resources

|Role                           |User Interface                                                     |
|-------------------------------|-------------------------------------------------------------------|
|ROLE_ADMIN_UI                  |Allow user to access login page as well as commonly used resources |

### Navigation Menu

The navigation menu is the menu on the top-left that allows the user to navigate between subsections of the
administrative user interface.

|Role                           |User Interface                                |
|-------------------------------|----------------------------------------------|
|ROLE_UI_NAV                    |Display navigation menu                       |
|ROLE_UI_NAV_RECORDINGS_VIEW    |Display navigation menu entry *Recordings*    |
|ROLE_UI_NAV_CAPTURE_VIEW       |Display navigation menu entry *Capture*       |
|ROLE_UI_NAV_SYSTEMS_VIEW       |Display navigation menu entry *Systems*       |
|ROLE_UI_NAV_ORGANIZATION_VIEW  |Display navigation menu entry *Organization*  |
|ROLE_UI_NAV_CONFIGURATION_VIEW |Display navigation menu entry *Configuration* |
|ROLE_UI_NAV_STATISTICS_VIEW    |Display navigation menu entry *Statistics*    |

If you want to provide access to the navigation menu, *ROLE_UI_NAV* is needed. Then, for each of the
navigation menu entries, include the respective role if the menu entry should be accessible by the user.

Note that this really just controls the navigation menu and its menu entries. Not less, not more.

### Statistics: Organization

|Role                                 |User Interface              |
|-------------------------------------|----------------------------|
|ROLE_UI_STATISTICS_ORGANIZATION_VIEW |Display *Organization* page |

### Recordings: Events

|Role                         |User Interface                                     |
|-----------------------------|---------------------------------------------------|
|ROLE_UI_EVENTS_VIEW          |Display *Events* page                              |
|ROLE_UI_EVENTS_COUNTERS_VIEW |Display the *Counters* on the *Events* page        |
|ROLE_UI_EVENTS_CREATE        |Display *Add Event* button on *Events* page        |
|ROLE_UI_EVENTS_DELETE        |Display *Delete* action in *Events* table          |
|ROLE_UI_EVENTS_EDITOR_VIEW   |Display *Playback/Editor* action in *Events* table |
|ROLE_UI_EVENTS_DETAILS_VIEW  |Display *Event Details* action in *Events* table   |
|ROLE_UI_TASKS_CREATE         |Display *Actions* on *Events* page                 |

For the *Playback/Editor* tool, further access can be provided:

|Role                        |User Interface                                     |
|----------------------------|---------------------------------------------------|
|ROLE_UI_EVENTS_EDITOR_EDIT  |Allow the user to actually edit videos             |

There are quite a number of roles to provide selective access to the tabs offered by the *Event Details* modal:

|Role                                     |User Interface                           |
|-----------------------------------------|-----------------------------------------|
|ROLE_UI_EVENTS_DETAILS_METADATA_VIEW     |Display tab *Metadata*                   |
|ROLE_UI_EVENTS_DETAILS_ASSETS_VIEW       |Display tab *Assets*                     |
|ROLE_UI_EVENTS_DETAILS_PUBLICATIONS_VIEW |Display tab *Publications*               |
|ROLE_UI_EVENTS_DETAILS_WORKFLOWS_VIEW    |Display tab *Workflows*                  |
|ROLE_UI_EVENTS_DETAILS_SCHEDULING_VIEW   |Display tab *Scheduling*                 |
|ROLE_UI_EVENTS_DETAILS_ACL_VIEW          |Display tab *Access Policy*              |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_VIEW     |Display tab *Comments*                   |
|ROLE_UI_EVENTS_DETAILS_STATISTICS_VIEW   |Display tab *Statistics*                 |

For the individual tabs, it is possible to further provide access:

|Role                                    |User Interface                           |
|----------------------------------------|-----------------------------------------|
|ROLE_UI_EVENTS_DETAILS_METADATA_EDIT    |Allow the user to edit *Metadata*        |
|ROLE_UI_EVENTS_DETAILS_ACL_EDIT         |Allow the user to edit *Access Policy*   |
|ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT   |Allow the user to edit *Workflows*       |
|ROLE_UI_EVENTS_DETAILS_WORKFLOWS_DELETE |Allow the user to delete *Workflows*     |
|ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT  |Allow the user to edit *Scheduling*      |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_CREATE  |Allow the user to create comments        |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE  |Allow the user to delete comments        |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_EDIT    |Allow the user to edit comments          |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_REPLY   |Allow the user to reply to comments      |
|ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE |Allow the user to resolve comments       |

### Recordings: Series

|Role                        |User Interface                                     |
|--------------------------------------|-----------------------------------------|
|ROLE_UI_SERIES_VIEW         |Display *Series* page                              |
|ROLE_UI_SERIES_CREATE       |Display *Add Series* on *Series* page              |
|ROLE_UI_SERIES_DELETE       |Display *Delete* action in *Series* table          |
|ROLE_UI_SERIES_DETAILS_VIEW |Display *Series Details* action in *Series* table  |

There are quite a number of roles to provide selective access to the tabs offered by the *Series Details* modal:

|Role                                   |User Interface                           |
|---------------------------------------|-----------------------------------------|
|ROLE_UI_SERIES_DETAILS_METADATA_VIEW   |Display tab *Metadata*                   |
|ROLE_UI_SERIES_DETAILS_ACL_VIEW        |Display tab *Access Policy*              |
|ROLE_UI_SERIES_DETAILS_THEMES_VIEW     |Display tab *Theme*                      |
|ROLE_UI_SERIES_DETAILS_STATISTICS_VIEW |Display tab *Statistics*                 |

For the individual tabs, it is possible to further provide access:

|Role                                  |User Interface                           |
|--------------------------------------|-----------------------------------------|
|ROLE_UI_SERIES_DETAILS_METADATA_EDIT  |Allow the user to edit *Metadata*        |
|ROLE_UI_SERIES_DETAILS_ACL_EDIT       |Allow the user to edit *Access Policy*   |
|ROLE_UI_SERIES_DETAILS_THEMES_EDIT    |Allow the user to edit *Theme*           |

### Capture: Locations

|Role                                  |User Interface                                         |
|--------------------------------------|-------------------------------------------------------|
|ROLE_UI_LOCATIONS_VIEW                |Display *Locations* page                               |
|ROLE_UI_LOCATIONS_DELETE              |Display *Delete* action in *Locations* table           |
|ROLE_UI_LOCATIONS_DETAILS_VIEW        |Display *Location Details* action in *Locations* table |

There are quite a number of roles to provide selective access to the tabs offered by the *Locations Details* modal:

|Role                                         |User Interface                   |
|---------------------------------------------|---------------------------------|
|ROLE_UI_LOCATIONS_DETAILS_CAPABILITIES_VIEW  |Display tab *Capabilities*       |
|ROLE_UI_LOCATIONS_DETAILS_CONFIGURATION_VIEW |Display tab *Configuration*      |
|ROLE_UI_LOCATIONS_DETAILS_GENERAL_VIEW       |Display tab *General*            |

### Systems: Jobs, Servers and Services

|Role                                  |User Interface                           |
|--------------------------------------|-----------------------------------------|
|ROLE_UI_JOBS_VIEW                     |Display *Jobs* page                      |
|ROLE_UI_SERVERS_VIEW                  |Display *Servers* page                   |
|ROLE_UI_SERVICES_VIEW                 |Display *Services* page                  |

On those pages, it is possible to further provide access:

|Role                                  |User Interface                                |
|--------------------------------------|----------------------------------------------|
|ROLE_UI_SERVERS_MAINTENANCE_EDIT      |Allow the user turn on/off server maintenance |
|ROLE_UI_SERVICES_STATUS_EDIT          |Allow the user to sanitize services           |

### Organization: Users

|Role                                  |User Interface                                 |
|--------------------------------------|-----------------------------------------------|
|ROLE_UI_USERS_VIEW                    |Display *Users* page                           |
|ROLE_UI_USERS_CREATE                  |Display *Add user* on *Users* page             |
|ROLE_UI_USERS_DELETE                  |Display *Delete* action in *Users* table       |
|ROLE_UI_USERS_EDIT                    |Display *User Details* action in *Users* table |

### Organization: Groups

|Role                                  |User Interface                                   |
|--------------------------------------|-------------------------------------------------|
|ROLE_UI_GROUPS_VIEW                   |Display *Groups* page                            |
|ROLE_UI_GROUPS_CREATE                 |Display *Add groups* on *Groups* page            |
|ROLE_UI_GROUPS_DELETE                 |Display *Delete* action in *Groups* table        |
|ROLE_UI_GROUPS_EDIT                   |Display *Group Details* action in *Groups* table |

### Organization: Access Policies

|Role                                  |User Interface                                            |
|--------------------------------------|----------------------------------------------------------|
|ROLE_UI_ACLS_VIEW                     |Display *Access Policies* page                            |
|ROLE_UI_ACLS_CREATE                   |Display *Add access policy* on *Access Policies* page     |
|ROLE_UI_ACLS_DELETE                   |Display *Delete* action in *Access Policies* table        |
|ROLE_UI_ACLS_EDIT                     |Display *Group Details* action in *Access Policies* table |

### Configuration: Themes

|Role                                  |User Interface                                            |
|--------------------------------------|----------------------------------------------------------|
|ROLE_UI_THEMES_VIEW                   |Display *Themes* page                                     |
|ROLE_UI_THEMES_CREATE                 |Display *Add theme* on *Themes* page                      |
|ROLE_UI_THEMES_DELETE                 |Display *Delete* action in *Themes* table                 |
|ROLE_UI_THEMES_EDIT                   |Display *Theme Details* action in *Themes* table          |
