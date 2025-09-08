Post Media Package Workflow Operation
=====================================

ID: `post-mediapackage`

Description
-----------

This workflow operation can be used to send a POST request containing an XML/JSON representation of the
media package processed by the workflow to an external web service. the service supports HTTP Basic and Digest
authentication.

Parameter Table
---------------

|Configuration Keys |Description                                                                                   |
|-------------------|----------------------------------------------------------------------------------------------|
|url                |The target URL                                                                                |
|format             |The desired export format: `xml` or `json`                                                    |
|debug              |Disable this on a productive system. If enabled, request bodies etc. will be written to log. If disabled, only errors will be logged. |
|mediapackage.type  |Type of Mediapackage to send (possible values: `workflow`, `search`; default: `search`)       |
|auth.enabled       |enable authentication (simple/digest will be detected automatically)                          |
|auth.username      |username for authentication                                                                   |
|auth.password      |password for authentication                                                                   |
|+source_system     |fields with keys beginning with `+` will be added to the message body                         |

Operation Example
-----------------

```yaml
  - id: post-mediapackage
    description: Sending MediaPackage to Lernfunk3
    configurations:
      - url: http://example.com:5000/
      - format: xml
      - debug: no
      - mediapackage.type: search
      - auth.enabled: yes
      - auth.username: exportuser
      - auth.password: secret
      - +source_system: video.example.com
```
