# StartWorkflowWorkflowOperationHandler

## Description

The StartWorkflowWorkflowOperationHandler can be used to start a new workflow for given media package and workflow definition.

## Parameter Table

|Configuration Key         |Example                              |Description                                     |
|--------------------------|-------------------------------------|------------------------------------------------|
|media-package\*           |e72f2265-472a-49ae-bc04-8301d94b4b1a |The ID of the media package that should be used |
|workflow-definition\*     |fast                                 |The workflow definition that should be used     |
|*configProperty*          |abc / false                          |Workflow configuration property                 |

\* mandatory configuration key

## Operation Example

```xml
<operation id="start-workflow">
  <configurations>
    <configuration key="workflow-definition">fast</configuration>
    <configuration key="media-package">e72f2265-472a-49ae-bc04-8301d94b4b1a</configuration>
    <configuration key="key">value</configuration>
    <configuration key="publish">true</configuration>
  </configurations>
</operation>
```
