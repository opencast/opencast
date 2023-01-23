Assert Workflow Operation
=========================

ID: `assert`

Description
-----------

The assert operation evaluates workflow configuration variables based on assertions.

The operation fails if an assertion does not meet its criteria.

Parameter Table
---------------

| configuration key | description                                                                        | example                                    |
|-------------------|------------------------------------------------------------------------------------|--------------------------------------------|
| that-**X**        | Assertion to be tested that evaluates to true, same syntax as workflow conditions  | (${presenter_work_resolution_x} &gt; 1600) |
| true-**X**        | Synonym for _that_ assertion                                                       | (${presenter_work_media})                  |
| false-**X**       | Assertion to be tested that evaluates to false, same syntax as workflow conditions | (${presenter_delivery_media})              |

All assertions are sorted as strings and then evaluated in sequence e.g. false-1, that-1, true-1, etc.

Example
-------

Fail workflow if conditions are not met:

```yaml
  - id: assert
    description: "Ensure presenter work media is present and has a matching framerate"
    configurations:
      - that-1: "${presenter_source_framerate} == 30"
      - true-1: "${presenter_source_media}"
```
