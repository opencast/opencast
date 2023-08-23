Video Editor Frontend
========================

Opencast's video editor frontend (also referred to as "Stand-alone video editor" or just "The editor") provides a tool for
users to cut videos without full access to the admin interface.
It strives to be simple and easy to use while providing enough features for most common use-cases.


Accessing The Editor
--------------------

You can access the editor by providing an event identifier to the web interface like this:

```
/editor-ui/index.html?id=<ID>
```

Stand-alone capabilities
--------------

The editor frontend is also referred to as "stand-alone" since it can be deployed and run outside an Opencast installation.
Should you wish to do so check its github repository: https://github.com/opencast/opencast-editor
However, there is little benefit in doing so, as the editor frontend will not function without a connection to its
backend in Opencast.


Preview Tracks
--------------

Preview tracks for the editor are retrieved from the internal publication, similar to the editor in the admin interface.
But unlike the admin interface, tracks are selected in the following way,
falling back to the next rule if the previous yielded no results:

- select tracks tagged with `preview.tag`
- select tracks with sub-flavor `preview.subtype`
- select all available tracks

More details about the preview track selection can be found and configured in
`etc/org.opencastproject.editor.EditorServiceImpl.cfg`.


Workflow Selection
------------------

The editor offers a workflow selection, allowing users to choose how an event is being processed.
Workflows need to be tagged `editor` to show up in the user interface.
The interface honors title, description and display order of workflows.

If only a single workflow exists, the selection will be skipped.


Replacing the Admin Interface Editor
------------------------------------

It is possible to have the admin interface link to old internal editor instead of the stand-alone editor.
For this, configure the `prop.admin.editor.url` in `etc/org.opencastproject.organization-mh_default_org.cfg`.


Configuration
-------------

The following configuration files allow customization of the editor:

- Backend (track selection, …)
    - `etc/org.opencastproject.editor.EditorServiceImpl.cfg`
- User interface (which tools to show, …))
    - `etc/ui-config/mh_default_org/editor/editor-settings.toml`
- Admin interface integration
    - `etc/org.opencastproject.organization-mh_default_org.cfg`
