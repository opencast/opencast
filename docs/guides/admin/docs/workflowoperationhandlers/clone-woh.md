# CloneWorkflowOperationHandler

## Description
The CopyWorkflowOperationHandler can be used to clone media package elements from one flavor to another.

## Parameter Table

|Configuration Key|Example           |Description                                       |
|-----------------|------------------|--------------------------------------------------|
|source-flavor     |presenter/source |The source flavor(s) to clone                     |
|source-tags       |archive          |Comma-separated list of source-tags               |
|target-flavor*    |target           |The target subflavor-name                         |

\* mandatory configuration key

Notes:

* *source-flavor* and *source-tags* may be used both together to select media package elements based on both flavors and tags
* In case that neither *source-flavor* nor *source-tags* are specified, the operation will be skipped
* In case no media package elements match *source-flavor* and *source-tags*, the operation will be skipped

## Source Flavor
If *source-flavor* is specified as e.g. *\*/source*, all matching media package elements will be cloned and have the new flavor *<original-flavor>/target*.

## Operation Example

        <operation
                id="clone"
                fail-on-error="true"
                exception-handler-workflow="ng-partial-error">
                <configurations>
                        <configuration key="source-flavor">*/source</configuration>
                        <configuration key="source-tags">archive</configuration>
                        <configuration key="target-flavor">target</configuration>
                </configurations>
        </operation>


