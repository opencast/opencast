Include Workflow Operation
==========================

ID: `include`

Description
-----------

The Include operation can be used to add a workflow definition to the current workflow. This enables re-usable
sequences of operations to be factored out and allows better structuring of complex workflows.

Parameter Table
---------------

|Configuration Key |Example              |Description                                                    |
|------------------|---------------------|---------------------------------------------------------------|
|workflow-id       |`partial-cleanup` |The workflow definition id of the workflow to be included      |

Operation Example
-----------------

```xml
<operation
    id="include"
    description="Remove temporary processing artifacts">
  <configurations>
    <configuration key="workflow-id">partial-cleanup</configuration>
  </configurations>
</operation>
```

```yml
- id: include
  description: Include clean-up workflow
  configurations:
    workflow-id: partial-cleanup
```
