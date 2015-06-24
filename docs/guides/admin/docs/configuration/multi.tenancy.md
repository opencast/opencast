Multi Tenancy Configuration
===========================

Introduction
------------

A single Matterhorn instance can handle mutliple tenants, each of which have their own recordings in the system.
Matterhorn refers to tenants as *organizations*, and an HTTP request to the Matterhorn installation is mapped to an
organization using the server name. Therefore, a Matterhorn instance will usually be set up with multiple DNS names
pointing to the same IP, for example:

 - tenant1.matterhorn.edu
 - tenant2.matterhorn.edu

A tenant configuration thus consists mainly of the DNS name that is mapped to that tenant.


### Default Setup

Out of the box, Matterhorn has one tenant configured, called mh_default_org that is mapped to the server name
localhost:8080. As long as there is one tenant configuration only, Matterhorn will map every request to that tenant
regardless of the server name. As soon as a second tenant configuration is available, requests will be mapped to
organizations using the server name, and a 404 will be returned for requests that hit the Matterhorn intallation that
cannot be mapped to any organization.


### Limitations

Multi tenancy in Matterhorn is working, however it is not fully finished. Certain objects are still shared amongst
organizations, most notably workflow definitions, RSS/Atom feeds and encoding profiles.


Adding A Tenant
---------------

To add a tenant to the installation, two things need to be put in place: a tenant configuration and a set of security
rules. Assume that the new tenant is called `tenant1` and should be mapped to `tenant1.myuniversity.edu`.

### Tenant Configuration

Create a file called org.opencastproject.organization-tenant1.cfg in the /load directory of your matterhorn
installation:

    id=tenant1
    name=Tenant 1
    server=tenant1.myuniversity.edu
    port=8080
    admin_role=ROLE_ADMIN
    anonymous_role=ROLE_ANONYMOUS

    # Admin and Engage Server Urls
    prop.org.opencastproject.admin.ui.url=https://tenant1_admin.myuniversity.edu
    prop.org.opencastproject.engage.ui.url=https://tenant1_engage.myuniversity.edu

    # Default properties for the user interface
    prop.logo_large=/img/MatterhornLogo_large.png
    prop.logo_small=/img/OpencastLogo.png

    # Define which parts of the admin ui should be visible
    prop.adminui.i18n_tab_episode.enable=false
    prop.adminui.i18n_tab_users.enable=false

    # Define which parts of the engage ui should be visible
    prop.engageui.link_download.enable=false
    prop.engageui.link_download.enable=false

Note that if you are running Apache httpd with mod_proxy in front of the Matterhorn installation, the port number will be -1.

### Security Configuration

Create a file called tenant1.xml in /etc/security. This file specifies access rules for individual urls that specify
which roles are needed in order to access a given url. In addition, it allows to define the directory services that are
used to authenticate users. The file follows the standard ways on configuring Spring Security and you are free to add
anything that can go into a Spring Security configuration.

The easiest way of creating that file is probably to create a copy of the already existing mh_default_org.xml.
