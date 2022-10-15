Configurable AWS/S3 Retract Workflow Operation
==============================================

ID: `retract-configure-aws`


Description
-----------

The configurable AWS/S3 retract operation retracts the published elements from a configured publication.

If the elements have been added to the Publication using "with-published-elements", as in the case with the external
api, they haven't actually been published so it is unnecessary to have a retract-configuration. Adding a retraction
won't cause any errors, it will just skip those elements.


Parameters
----------

These are the keys that can be configured for the worklow operation in the workflow definition.  The `channel-id` is
mandatory.

|Key                    |Description                                          |Example    |Default  |
|-----------------------|-----------------------------------------------------|-----------|---------|
|channel-id             |The id of the channel to retract from                |`internal` |         |


Operation Example
-----------------

Retract from internal channel:

```xml
<operation
    id="retract-configure-aws"
    description="Retract from internal publication channel using AWS S3">
  <configurations>
    <configuration key="channel-id">internal</configuration>
  </configurations>
</operation>
```

Retract from external API:

```xml
<operation
    id="retract-configure-aws"
    description="Retract from external api publication channel using AWS S3">
  <configurations>
    <configuration key="channel-id">api</configuration>
  </configurations>
</operation>
```
