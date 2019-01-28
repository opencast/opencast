List Providers
==============

Opencast supports fully configurable key-value lists. To add a new list, simply create a file with the extension
`.properties` in `etc/listproviders`. The list will be loaded or updated automatically.

The Java properties file format is used with the following special keys to configure the list:

Key               | Type    | Description                                        | Mandatory | Default
------------------|---------|----------------------------------------------------|-----------|--------
list.name         | String  | The list's unique identifier within a tenant       | yes       | n/a
list.default      | String  | The name of the default key                        | no        | n/a
list.translatable | Boolean | Whether the values are supposed to be translatable | no        | false
list.org          | String  | The organisation ID                                | no        | "\*"

Note that it is up to the client to handle the keys `list.default` and `list.translatable`.


Multi-Tenancy
-------------

The key `list.org` can be used to configure lists for specific tenants in multi-tenant setups. It defaults to `*` which
means that the list is available for all tenants.


The following logic is used to locate a list with a given list name *LISTNAME*:

1. Return the list *LISTNAME* specific to the current tenant
2. If not found, return the list *LISTNAME* available for all tenants
3. If not found, return no list

While the filename of the list does not affect the list itself, we recommend to include the organisation identifier
in the filename.

### Example

    /etc/listproviders/mylist-org-a.properties
      list.name=MYLIST
      key=value

    /etc/listproviders/mylist-org-b.properties
      list.name=MYLIST
      list.org=org-b
      key-org-b=value-org-b

On *org-b*, the key-value pair for the list *MYLIST* is *["key-org-b", "value-org-b"]* due to the tenant specific
configuration. On *org-a*, the key-value pair for the list *MYLIST* is *["key", "value"]*. Since there is no tenant
specific configuration for *org-a*, the defaults are used.
