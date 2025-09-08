Start-Workflow Workflow Operation
=================================

ID: `start-workflow`

Description
-----------

The `start-workflow` operationHandler can be used to start a new workflow for given media package and workflow definition.

Parameter Table
---------------

|Configuration Key         |Example                              |Description                                     |
|--------------------------|-------------------------------------|------------------------------------------------|
|media-packages\*          |e72f2265-472a-49ae-bc04-8301d94b4b1a, a32e2265-472a-49ae-bc04-8351d94b4b1c |comma separated list of the media package ids that should be used |
|workflow-definition\*     |fast                                 |The workflow definition that should be used     |
|configProperty            |abc / false                          |Workflow configuration property                 |

\* mandatory configuration key

Operation Example
-----------------

```yaml
  - id: start-workflow
    configurations:
      - workflow-definition: fast
      - media-packages: ${duplicate_media_package_ids}
      - key: value
      - publish: true
```
