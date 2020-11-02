# ConfigureByDCTermWorkflowOperationHandler

## Description
With the ConfigureByDCTermWorkflowOperationHandler it's possible to create a workflow configuration property according
to whether a Dublin Core term in a catalog has a specific value. So for example it's possible to control a workflow so
that it will publish before editing if a certain Dublin Core term has the specified value.

In combination with [TagByDCTermWorkflowOperationHandler](tag-by-dcterm-woh.md) workflows can be controlled by the
metadata contained within the Dublin Core catalogs.

## Parameter Table
Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|dccatalog         |"episode" or "series"|the type of catalog in which to search for `dcterm`|EMPTY|
|dcterm            |"creator"            |the name of the Dublin Core term which to check|EMPTY|
|match-value       |"Joe Bloggs"         |the Dublin Core term value to check for|EMPTY|
|default-value     |"Anon"               |the implied value if the dubincore term is not present in the catalog|EMPTY|
|*configProperty*  |true / false         |a configuration property and the value it will be given if a match is found|EMPTY|

### dccatalog
The type of Dublin Core catalog in which to look for the `dcterm`. This will usually be `episode` or `series`.

### dcterm
The name of the Dublin Core term to look for in the `dccatalog`. This could be one of the terms set by Opencast or an
additional term adding to the catalog.

### match-value
The value of the `dcterm` which to match against. The comparison is case sensitive.

### default-value
If `default-value` is used when the `dcterm` is not found in the catalog. If not specified the operation will treat the
match as false and not configure anything. If `default-value` is specified the operation will compare the `match-value`
to the `default-value` and set the workflow property if they match. This allows an implied value to be explicitly and
clearly defined. For example if you have mediapackages that were created before additional metadata was added to the
episode catalog you may want to imply that the `audience` term has a value of `all-enrolled`.

### "configProperty"
Specifies as the key the name of a new workflow configuration property and the boolean value to which it will be set if
the Dublin Core term matches the specified value.

Due to the way a workflow evaluates operation `if` conditions as configuration properties are created, only new
configuration properties can be used to modify the execution of subsequent operations. Also since an undefined property
will be evaluated as `false` in practice the only useful value which can set is `true`.  However operation `if`
conditions can be negated though so it is possible to skip subsequent operations on matched `dcterm`  value.

## Operation Example
    <operation
      id="configure-by-dcterm"
      fail-on-error="true"
      description="Configure publication channel by dcterm">
      <configurations>
        <configuration key="dccatalog">episode</configuration>
        <configuration key="dcterm">audience</configuration>
        <configuration key="match-value">private</configuration>
        <configuration key="publishPrivate">true</configuration>
      </configurations>
    </operation>

    ...

    <operation
       id="publish-engage"
       if="${publishPrivate}"
       description="Publish to internal audience only">
       ...
    </operation>


    <operation
       id="publish-youtube"
       if="NOT ${publishPrivate}"
       description="Publish to global audience">
       ...
    </operation>
