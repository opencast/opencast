Admin Interface
===============

Opencast comes with two admin interfaces. The new Admin UI continues with the same look and
feel, but uses new technologies under the hood. It strives to be robust and easy to work with, while having all the
same functionality users expect from the old UI.


Accessing Old And New Admin Interface
-------------------------------------

You can access the admin interfaces by replacing the `ng` in your address bar with `ui` and vice versa.

- New: `/admin-ui/index.html`
- Old: `/admin-ng/index.html`


Configuring the Default Admin Interface
---------------------------------------

To configure the default admin interface, go to the configuration file located in
`etc/ui-config/{organizationID}/runtime-info-ui/settings.json` and change `adminUIUrl`:

- New: `"adminUIUrl": "/admin-ui/index.html"` (default)
- Old: `"adminUIUrl": "/admin-ng/index.html"`
