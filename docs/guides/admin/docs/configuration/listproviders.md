List Providers
==============

Opencast supports fully configurable key-value lists. To add a new list, simply create a file with the file extension
`.properties` within the folder `etc/listproviders`. The list will be loaded or updated automatically while Opencast
is running.

The Java properties file format is used with the following special keys to configure the list:

Key               | Type    | Description                                        | Mandatory | Default
------------------|---------|----------------------------------------------------|-----------|--------
list.name         | String  | The identifier of the list                         | yes       | n/a
list.default      | String  | The name of the default key                        | no        | n/a
list.translatable | Boolean | Whether the values are supposed to be translatable | no        | false
list.org          | String  | The organisation ID                                | no        | "*"

For the special keys `list.default` and `list.translatable` it is up to the client to handle them.

**IMPORTANT:** Be sure to use unique list names within a single tenant.

Multi-Tenancy
-------------

The special key `list.org` is only relevant for multi-tenant setups. If not specified, its value defaults to `*` which
means that the list is available for all tenants.

In multi-tenant setups, the key can be used to configure lists for specific tenants.

The list provider service will use the following logic to locate a list with a given list name *LISTNAME*:

1. Return the list *LISTNAME* specific to the current tenant
2. If not found, return the list *LISTNAME* available for all tenants
3. If not found, return no list

While the filename of the list does not affect the list itself, we recommend to include the organisation identifier
in the filename.

**Example:**

    /etc/listproviders/mylist-org-a.properties
      list.name=MYLIST
      key=value

    /etc/listproviders/myList-org-b.properties
      list.name=MYLIST
      list.org=org-b
      key-org-b=value-org-b

On *org_b*, the key-value pair for the list *MYLIST* is *["key-org-b", "value-org-b"]*. On *org_a*, the key-value pair for the
list *MYLIST* is *["key", "value"]*.

