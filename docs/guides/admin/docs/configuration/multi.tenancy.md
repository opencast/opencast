Multi Tenancy Configuration
===========================

Introduction
------------

A single Opencast instance can handle multiple tenants, each of which have their own recordings in the system.
Opencast refers to tenants as *organizations*, and an HTTP request to the Opencast installation is mapped to an
organization using the server name. Therefore, a Opencast instance will usually be set up with multiple DNS names
pointing to the same IP, for example:

- admin.example.org
- tenant1-admin.example.org
- tenant2-admin.example.org

should all resolve to the same IP.

A tenant configuration thus consists mainly of the DNS name that is mapped to that tenant.


### Default Setup

Out of the box, Opencast has one tenant configured, called `mh_default_org` that is mapped to the server name
`localhost:8080`. As long as there is one tenant configuration only, Opencast will map every request to that tenant
regardless of the server name. As soon as a second tenant configuration is available, requests will be mapped to
organizations using the server name, and an HTTP status code 404 will be returned for requests that hit the Opencast
installation that cannot be mapped to any organization.


### Limitations

Multi tenancy in Opencast is working, however it is not fully finished. Certain objects are still shared amongst
organizations.


Adding A Tenant
---------------

To add a tenant to the installation, two things need to be put in place: a tenant configuration and a set of security
rules. For this example we have a three node install of `admin.example.org`, `worker.example.org`, and
`presentation.example.org`.  Assume that the new tenant is called `tenant1` and should be mapped to
`tenant1-*.example.org`.

### Step 1: Tenant Configuration

Create a file called org.opencastproject.organization-tenant1.cfg in the `etc/` directory of your Opencast
installation, on each of the nodes.  As an example, this is what the admin node looks like:

    id=tenant1
    name=Tenant 1
    admin_role=ROLE_TENANT1_ADMIN
    anonymous_role=ROLE_ANONYMOUS

    # Hostname URL mapping
    prop.org.opencastproject.host.admin.example.org=https://tenant1-admin.example.org
    prop.org.opencastproject.host.presentation.example.org=https://tenant1-presentation.example.org
    prop.org.opencastproject.host.worker.example.org=https://tenant1-worker.example.org:8443

    # Admin and Presentation Server Urls
    prop.org.opencastproject.admin.ui.url=https://tenant1-admin.example.org
    prop.org.opencastproject.engage.ui.url=https://tenant1-presentation.example.org

    # Default properties for the user interface
    prop.logo_mediamodule=/engage/ui/img/logo/opencast-icon.svg
    prop.logo_player=/engage/ui/img/logo/opencast.svg

Give each tenant its own administrator role instead of reusing `ROLE_ADMIN`. `ROLE_ADMIN` is Opencast's *global*
administrator role and grants administrative access across all tenants. Setting `admin_role` to a tenant-specific
value such as `ROLE_TENANT1_ADMIN` keeps that tenant's administrators scoped to their own tenant. Note that the
default organization (`mh_default_org`) intentionally keeps `admin_role=ROLE_ADMIN`, because its administrator is the
system-wide administrator. When you pick a tenant-specific admin role here, make sure the tenant's security
configuration grants it access (see Step 2).

There are more options available than in this example. The easiest way of creating that file is probably to create a
copy of the already existing `org.opencastproject.organization-mh_default_org.cfg`.

Note, the default organization file `org.opencastproject.organization-mh_default_org.org` *must* refer to the actual
server names:

    prop.org.opencastproject.host.admin.example.org=https://admin.example.org
    prop.org.opencastproject.host.presentation.example.org=https://presentation.example.org
    prop.org.opencastproject.host.worker.example.org=https://worker.example.org:8443

This file sets the default organization that is selected.  This is currently required because some Opencast components
do not support multitenancy.

Hosts can have different schemas (http / https) and port numbers, e.g. when you run behind proxy server. Note, that
the combination of hostname and port number is uniquely mapped to an organization.

It is not supported for Opencast tenants to be hosted in a subpath.
Opencast tenants needs to be served from the root path element.
The RFC 3986 URI path component needs to be empty.

### Step 2: Security Configuration

Create a file called tenant1.xml in /etc/security. This file specifies access rules for individual URLs that specify
which roles are needed in order to access a given URL. In addition, it allows to define the directory services that are
used to authenticate users. The file follows the standard ways on configuring Spring Security and you are free to add
anything that can go into a Spring Security configuration.

The easiest way of creating that file is probably to create a copy of the already existing `mh_default_org.xml`.

The access rules in `mh_default_org.xml` grant access using the global administrator role `ROLE_ADMIN`. If you gave the
tenant its own administrator role in Step 1, that role needs to be granted access here as well; otherwise its
administrators cannot use the parts of the Admin UI protected by these rules. Add the tenant-specific role alongside
`ROLE_ADMIN` in the access rules, for example by replacing every `'ROLE_ADMIN'` with
`'ROLE_ADMIN', 'ROLE_TENANT1_ADMIN'`. Keeping `ROLE_ADMIN` leaves access for the system-wide administrator intact. Take
care not to alter unrelated roles that merely start with the same text, such as `ROLE_ADMIN_UI`.

### Step 3: Other Configuration

Two additional files should be copied: `org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common.cfg`
should be copied to `org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common-tenant1.cfg`, and
`org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common.cfg` should be copied to
`org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common-tenant1.cfg`.

In each of the new configuration files, change `organization` key to match the tenant id, and change the
`common-metadata` key to false.  Create a copy of the files for each tenant.  Note: The original `...-common.cfg` files
*must* have their `common-metadata` keys set to true, otherwise metadata will only be available in one tenant and you
will experience a number of odd errors.
