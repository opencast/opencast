Plugin Management
=================

Opencast comes with a number of plugins which allow you to turn additional functionality off and on.
Plugins can be enabled at runtime in `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg` by setting the specific
configuration to `on` or `off`.

List of Available Plugins
-------------------------

- opencast-plugin-legacy-annotation
    - Legacy annotation functionality. We do not recommend turning this on. It also requires additional configuration in
      the player. It's kept for a few legacy use-cases.
- opencast-plugin-transcription-services
    - Support for the Amberscript transcription service
    - Support for the Google cloud transcription service
    - Support for the IBM cloud transcription service
    - Support for the Microsoft Azure cloud transcription service
- opencast-plugin-userdirectory-brightspace
    - Module allowing Opencast to look up users in a Brightspace LMS
- opencast-plugin-userdirectory-canvas
    - Module allowing Opencast to look up users in a Canvas LMS
- opencast-plugin-userdirectory-moodle
    - Module allowing Opencast to look up users in a Moodle LMS
- opencast-plugin-userdirectory-sakai
    - Module allowing Opencast to look up users in a Sakai LMS
- opencast-plugin-usertracking
    - Legacy user tracking module. We do not recommend turning this on. It also requires additional configuration in
      the player. It's kept for a few legacy use-cases.
