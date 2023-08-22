New Admin UI
========================

<div class=warn>
The new Admin UI is still <b>beta</b>.
</div>

Opencast comes with two Admin UIs, the classic one and a new one. The new Admin UI continues with the same look and
feel, but uses new technologies under the hood. It strives to be robust and easy to work with, while having all the
same functionality users expect from the old UI.


Accessing The New Admin UI
--------------------

You can access the new Admin UI by replacing the `ng` in your address bar with `ui`

```
/admin-ui/index.html
```


Replacing the old Admin UI
------------------------------------

To use the new Admin UI per default, go to the configuration file located in
`etc/ui-config/{organizationID}/runtime-info-ui/settings.json` and change

```
"adminUIUrl": "/admin-ng/index.html"
```

to

```
"adminUIUrl": "/admin-ui/index.html"
```
