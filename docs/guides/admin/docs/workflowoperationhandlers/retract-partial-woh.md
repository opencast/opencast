Partial Retract Engage Workflow Operation
=========================================

ID: `retract-partial`

Description
-----------

The PartialRetractEngageWorkflowOperationHandler retracts a subset of the published elements from the local Opencast Media Module.  This is useful to remove incorrect captions without reprocessing the entire workflow.  Note: the elements selected for retraction match any combination of flavor or tag, and the resulting publication may be unusable if you accidentally retract one or more delivery files.  Use this operation with caution.

Parameter Table
---------------

|configuration keys         |description                                                                                  |
|---------------------------|---------------------------------------------------------------------------------------------|
|retract-flavors            |Which flavor(s) to retract.  Use a comma to separate multiple flavors.                       |
|retract-tags               |Which tags(s) to retract.  Use a comma to separate multiple tags.                            |


Operation Example
-----------------

```xml
<operation
    id="retract-engage"
    fail-on-error="true"
    exception-handler-workflow="partial-error"
    description="Retract recording from Engage">
  <configurations>
    <configuration key="retract-flavors">presentation/*</configuration>
    <configuration key="retract-tags">preview</configuration>
  <configurations>
</operation>
```
