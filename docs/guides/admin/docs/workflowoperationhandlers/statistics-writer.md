Statistics Writer Workflow Operation
====================================

ID: `statistics-writer`

Description
-----------

The statistics writer operation can be used to publish statistics about a video to a statistics backend such as
InfluxDB. Currently, it only writes the length of the video in seconds to the data base. It can be configured to
write the negative length and can thus be used for retract workflows, too.


Parameter Table
---------------

|configuration keys            |required|description                                                        |
|------------------------------|--------|-------------------------------------------------------------------|
|flavor                        |yes     |The flavor of the track you want to publish statistics about       |
|measurement-name              |yes     |Measurement name in the statistics DB                              |
|organization-resource-id-name |yes     |Resource ID name for the organization                              |
|length-field-name             |yes     |Field name for the length of the video                             |
|temporal-resolution           |yes     |Temporal resolution to store the length in \*                      |
|retention-policy              |no      |Retention policy to use for the statistics DB                      |
|retract                       |no      |Whether to publish positive or negative numbers (default: `false`) |

\* Possible values are `milliseconds`, `seconds`, `minutes`, `hours`, `days`

Operation Examples
------------------

```yaml
  - id: statistics-writer
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Collect video statistics
    configurations:
      - flavor: presenter/video
      - retract: false
      - retention-policy: infinite
      - measurement-name: publishedhours
      - organization-resource-id-name: organizationId
      - length-field-name: hours
      - temporal-resolution: hours
```
