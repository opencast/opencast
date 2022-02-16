Partial Retract Engage Workflow Operation
=========================================

ID: `retract-partial`

Description
-----------

The partial retract engage operation retracts a subset of the published elements from the search service.
This is useful to e.g. remove incorrect captions without reprocessing the entire workflow.

The elements selected for retraction match any combination of flavor or tag,
and the resulting publication may become unusable if you accidentally retract one or more delivery files.
Use this operation with caution.


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
    id="retract-partial"
    description="Retracting elements flavored with presentation and tagged with preview from Engage">
  <configurations>
    <configuration key="retract-flavors">presentation/*</configuration>
    <configuration key="retract-tags">preview</configuration>
  </configurations>
</operation>
```
