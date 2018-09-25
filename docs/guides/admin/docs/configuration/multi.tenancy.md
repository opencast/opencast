Multi Tenancy Configuration
===========================

Introduction
------------

A single Opencast instance can handle mutliple tenants, each of which have their own recordings in the system.
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
intallation that cannot be mapped to any organization.


### Limitations

Multi tenancy in Opencast is working, however it is not fully finished. Certain objects are still shared amongst
organizations, most notably workflow definitions, RSS/Atom feeds and encoding profiles.


Adding A Tenant
---------------

To add a tenant to the installation, two things need to be put in place: a tenant configuration and a set of security
rules. For this example we have a three node install of `admin.opencast.org`, `worker.opencast.org`, and
`presentation.opencast.org`.  Assume that the new tenant is called `tenant1` and should be mapped to
`tenant1-*.opencast.org`.

### Tenant Configuration

Create a file called org.opencastproject.organization-tenant1.cfg in the `etc/` directory of your Opencast
installation, on each of the nodes.  As an example, this is what the admin node looks like:

    id=tenant1
    name=Tenant 1
    server=tenant1-admin.opencast.org,tenant1-presentation.opencast.org
    port=8080
    admin_role=ROLE_ADMIN
    anonymous_role=ROLE_ANONYMOUS

    # Admin and Presentation Server Urls
    prop.org.opencastproject.admin.ui.url=https://tenant1-admin.opencast.org
    prop.org.opencastproject.engage.ui.url=https://tenant1-presentation.opencast.org

    # Default properties for the user interface
    prop.logo_mediamodule=/engage/ui/img/logo/opencast-icon.svg
    prop.logo_player=/engage/ui/img/logo/opencast.svg

There are more options available than in this example. The easiest way of creating that file is probably to create a
copy of the already existing `org.opencastproject.organization-mh_default_org.cfg`.

Note, the default organization file `org.opencastproject.organization-mh_default_org.org` *must* refer to the actual
server names:

    server=admin.opencast.org

This file sets the default organization that is selected.  This is currently required because some Opencast components
do not support multitenancy.

Note that if you are running Apache httpd with mod\_proxy in front of the Opencast installation, the port number will be
-1 in both files.

### Security Configuration

Create a file called tenant1.xml in /etc/security. This file specifies access rules for individual urls that specify
which roles are needed in order to access a given url. In addition, it allows to define the directory services that are
used to authenticate users. The file follows the standard ways on configuring Spring Security and you are free to add
anything that can go into a Spring Security configuration.

The easiest way of creating that file is probably to create a copy of the already existing `mh_default_org.xml`.

### Other Configuration

Two additional files should be copied: `org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common.cfg`
should be copied to `org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common-tenant1.cfg`, and
`org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common.cfg` should be copied to
`org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common-tenant1.cfg`.

In each of the new configuration files, change `organization` key to match the tenant id, and change the
`common-metadata` key to false.  Create a copy of the files for each tenant.  Note: The original `...-common.cfg` files
*must* have their `common-metadata` keys set to true, otherwise metadata will only be available in one tenant and you
will experience a number of odd errors.
