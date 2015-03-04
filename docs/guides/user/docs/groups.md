<!-- Hamburger Icon -->
[icon_hamburger]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAPCAYAAAAGRPQsAAAARklEQVQ4y2Ow6L3SCsQ/gfg/BRikv5WBCgbB8GcGKrnsF9hlIwSQEGY/CYYLiYH/mVouG1ExRqUwIxy7FGalz9RyGUbsAgCNXmeVduHT9gAAAABJRU5ErkJggg== "Edit Icon"

<!-- Delete icon -->
[icon_delete]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABEAAAARCAYAAAA7bUf6AAABEklEQVR42q2Uuw4BURRFVYpLoSCYL2PQTTU0vsBXeY14TIyan/Ao6ChQcE6yJTs37phCsTLZ++x7cp+Te9TrNkWhL6yEi/DCdwW/aI+xG/jCUQe6QN13NRlQcCM0hRpqNegNZQZ2Ex+Fp9CF56KrOeTb6n324AQzUDMDAfJnoaRGD0ZCobUwFgx0QYiELWUSjOupiCCaFBjCWwoVIYaeUKYBL1Kxh6hSwAhz+DdqaCjjwT+ouEPkGMzgqjV8y1Y9j9pdxeHLTAqfJVCjRdpMIogGBaa0hLI2gB592ZOZ63R2aGSgDTY7dpzOf+6J0qIbG/5oENKN7aS9nQRr9nAKHnTiejtMK+MrbvO4tP9JnPV/8gansczJeXp0AgAAAABJRU5ErkJggg== "Delete icon"

# Overview
By linking Roles and Users, Groups allow for managing permissions and access to resources and certain parts of the UI. See the [About Roles section](groups.md#about-roles) for more information about Roles in the UI.


## How to add Groups
Groups can be added using the **Add group** button. Roles and users can be selected upon creation of the group.

Note that every group defines a role, and each group member is assigned this role `ROLE_GROUP_<group name>`, which can then be used in Access Policies to grant a certain action to all members of that group.


## How to edit Groups
Groups can be edited using the edit icon ( ![icon_hamburger][] ). Roles and users can be selected upon creation of the group. Once selected, all aspects of a group can be edited and updated.

## How to delete Groups
Use the delete icon ( ![icon_delete][] ) in the Actions column to delete groups.

## About Roles
Roles allow an administrator to define permissions on a per-user or per-group basis. Here is a list of some of the role and what they are used for:

* `ROLE_ADMIN`: This role overrides any other role and provides full access to the users that possess it.

* `ROLE_UI_*`: Are bound to UI elements and endpoint security. They allow/deny access to certain parts of the UI.

* `ROLE_API_*`: These are similar to the ROLES_UI however they protect the external API of Opencast.

* `ROLE_GROUP_*`: A role that is defined by each group and that is assigned to every member of that group.
