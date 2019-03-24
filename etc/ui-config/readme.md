User Interface Configuration
============================

This folder can contain user interface configuration and customization files which are served by Opencast's web server
and can easily be used for frontend configuration. Please expect all these configurations to be public and unprotected.
Therefore, so not use them for any kind of secrets (passwords, …).

Note that the configuration is organization aware. That means that if you use the service, the HTTP path
`/ui/config/<component>/<filename>` will map to the local file `<organization-id>/<component>/<filename>`.
