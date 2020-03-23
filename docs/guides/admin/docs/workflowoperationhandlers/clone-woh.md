Clone Workflow Operation
========================

ID: `clone`

Description
-----------

The clone workflow operation can be used to clone media package elements.


Parameter Table
---------------

|Configuration Key         |Example           |Description                                       |
|--------------------------|------------------|--------------------------------------------------|
|source-flavor             |presenter/source  |The source flavor(s) to clone                     |
|source-tags               |archive           |Comma-separated list of source-tags               |
|target-flavor\*           |presenter/target  |The target flavor                                 |

\* mandatory configuration key

Notes:

- *source-flavor* and *source-tags* may be used both together to select media package elements based on both flavors and
  tags If *source-flavor* is not specified, all media package elements matching *source-tags* will be selected
- In case that neither *source-flavor* nor *source-tags* are specified, the operation will be skipped
- In case no media package elements match *source-flavor* and *source-tags*, the operation will be skipped

Source Flavor
-------------

If *source-flavor* is specified as e.g. `*/source`, all matching media package elements will be cloned and have the new
flavor `<original-flavor>/target`.

Target Flavor
-------------

If *target-flavor* is specified as e.g. `*/target`, the target flavors will have the subtype *target* and the type from
the source If *target-flavor* is specified as e.g. `target/*`, the target flavors will have the type *target* and the
subtype from the source.

Operation Example
-----------------

```xml
<operation
    id="clone"
    exception-handler-workflow="partial-error">
  <configurations>
    <configuration key="source-flavor">*/source</configuration>
    <configuration key="source-tags">archive</configuration>
    <configuration key="target-flavor">*/target</configuration>
  </configurations>
</operation>
```
