User Interface Configuration
============================

This folder can contain user interface configuration and customization files which are served by Opencast's web server
and can easily be used for frontend configuration.

All configuration files are public.  Do not use them for any kind of secrets (passwords, …).

The configuration is organization aware. That means that if you use the service, the HTTP path
`/ui/config/<component>/<filename>` will map to the local file `<organization-id>/<component>/<filename>`.
A fallback to the default organization (`mh_default_org`) exists.
