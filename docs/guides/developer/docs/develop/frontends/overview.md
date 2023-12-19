Frontends
---------

Opencast boasts multiple graphical user interfaces for different purposes, living in different modules or even projects.
The following is a brief overview.

### Admin UI
- Lives in the `admin-ui-frontend` module.
- The main frontend for managing Opencast.
- Style guide can be found [here](./admin-ui/development.md).

### New Admin UI
- Lives in its [own GitHub repository](https://github.com/opencast/opencast-admin-interface). Builds end up in the
  `admin-ui-interface` module.
- Will eventually become and replace the (old) Admin UI.

### Media Module
- Lives in the `engage-ui` module.
- A simple video portal for end users.

### LTI Tools
- Lives in the `lti` module.
- Provides simple ui for LTI tool consumers.

### Opencast Editor
- Lives in its [own GitHub repository](https://github.com/opencast/opencast-editor). Builds end up in the
  `editor` module.
- A simple video editor for cutting down videos.

### Rest docs
- Lives in the `runtime-info` module. The individual pages are generated from annotations in their respective
  `{Name}RestService.java` files.
- Documentation and graphical endpoint testing.
