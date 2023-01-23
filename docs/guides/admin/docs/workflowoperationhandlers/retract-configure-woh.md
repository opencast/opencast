Configurable Retract Workflow Operation
=======================================

ID: `retract-configure`

Description
-----------

The configurable retract operation retracts the published elements from a configured publication.

If the elements have been added to the Publication using "with-published-elements", as in the case with the external
api, they haven't actually been published so it is unnecessary to have a retract-configuration. Adding a retraction
won't cause any errors, it will just skip those elements.

Parameters
----------

These are the keys that can be configured for the workflow operation in the workflow definition.  The `channel-id` is
mandatory.

|Key                    |Description                                          |Example    |Default  |
|-----------------------|-----------------------------------------------------|-----------|---------|
|channel-id             |The id of the channel to retract from                |`internal` |         |
|retract-streaming      |Whether to retract streaming elements as well        |`true`     | `false` |

Setting `retract-streaming` to true only makes sense if you've published streaming elements for this channel before.

Operation Example
-----------------

Retract from internal channel:

```xml
<operation
    id="retract-configure"
    description="Retract from internal publication channel">
  <configurations>
    <configuration key="channel-id">internal</configuration>
  </configurations>
</operation>
```

Retract from external API:

```xml
<operation
    id="retract-configure"
    description="Retract from external api publication channel">
  <configurations>
    <configuration key="channel-id">api</configuration>
    <configuration key="retract-streaming">false</configuration>
  </configurations>
</operation>
```
