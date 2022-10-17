Retract OAI-PMH Workflow Operation
==================================

ID: `retract-oaipmh`

Description
-----------

The retract OAI-PMH operation retracts the published elements from a OAI-PMH repository.

Parameter Table
---------------

|Configuration Keys |Description                                                                                   |
|-------------------|----------------------------------------------------------------------------------------------|
|repository         |The name of the OAI-PMH repository where the media should be retracted from                   |

Operation Example
-----------------

```xml
<operation
    id="retract-oaipmh"
    description="Retract event from the OAI-PMH repository">
  <configurations>
    <configuration key="repository">default</configuration>
  </configurations>
</operation>
```
